<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type {
  ReleaseCertificate,
  CertificateRow,
  BatchRejectedDetail,
  OperatorInfo
} from '@/types'
import {
  listCertificates,
  listCertificateOperators,
  submitCertificateBatch,
  renewCertificate
} from '@/api/certificate'
import { verifyIdentity } from '@/api/auth'
import { identity, setIdentity, clearIdentity, isDispatcher } from '@/stores/identity'

// 身份核对
const identityCode = ref('')
const identityPassword = ref('')
const verifying = ref(false)
const verified = computed(() => identity.operator != null)

const loading = ref(false)
const submitting = ref(false)
const modeTab = ref('batch')

// 台账
const certificates = ref<ReleaseCertificate[]>([])
const operators = ref<OperatorInfo[]>([])

// 批量补录/换证：可粘贴多行文本，每行「工号,证号,发证日期,到期日期」
const batchText = ref(
  'OP-001,CERT-OP001-02,2025-04-01,2026-03-31\nOP-002,CERT-OP002-02,2026-07-01,2027-06-30'
)
// 解析后的批量行（表格里可直接改）
const batchRows = ref<CertificateRow[]>([])
// 最近一次整批被拦的说明
const rejected = ref<BatchRejectedDetail | null>(null)
const lastResult = ref('')

// 单条换证
const single = ref<CertificateRow>({ operatorCode: '', certificateNo: '', issueDate: '', expireDate: '' })

const roleText = computed(() => (isDispatcher() ? '调度' : '放飞员'))

const roleTagType = (role: string) =>
  role === 'DISPATCHER' ? 'danger' : role === 'LAUNCH_OPERATOR' ? 'success' : 'info'
const roleName = (role: string) =>
  role === 'DISPATCHER' ? '调度' : role === 'LAUNCH_OPERATOR' ? '放飞员' : role === 'GROUND_CREW' ? '地勤' : role

/** 每个人当前手里最晚到期日，供换证参考 */
const latestExpireByOperator = computed(() => {
  const map: Record<string, { date: string; certNo: string }> = {}
  for (const c of certificates.value) {
    const cur = map[c.operatorCode]
    if (!cur || c.expireDate > cur.date) {
      map[c.operatorCode] = { date: c.expireDate, certNo: c.certificateNo }
    }
  }
  return map
})

const selectedOperator = computed(() =>
  operators.value.find(o => o.operatorCode === single.value.operatorCode) || null
)

const selectedOperatorLatest = computed(() => latestExpireByOperator.value[single.value.operatorCode])

const handleVerify = async () => {
  if (!identityCode.value.trim() || !identityPassword.value) {
    ElMessage.warning('请输入工号和口令')
    return
  }
  verifying.value = true
  try {
    const operator = await verifyIdentity(identityCode.value.trim(), identityPassword.value)
    setIdentity(operator, identityPassword.value)
    identityPassword.value = ''
    ElMessage.success(`身份核对通过：${operator.operatorName}（${roleText.value}）`)
    await initData()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '身份核对失败')
  } finally {
    verifying.value = false
  }
}

const handleLogout = () => {
  clearIdentity()
  identityCode.value = ''
  identityPassword.value = ''
  certificates.value = []
  operators.value = []
  rejected.value = null
  lastResult.value = ''
}

const loadCertificates = async () => {
  loading.value = true
  try {
    certificates.value = await listCertificates()
  } catch (error: any) {
    if (error.response?.status === 401) {
      ElMessage.error(error.response?.data?.message || '身份核对已失效，请重新核对')
      handleLogout()
    } else {
      ElMessage.error('加载证照台账失败')
    }
  } finally {
    loading.value = false
  }
}

const loadOperators = async () => {
  try {
    operators.value = await listCertificateOperators()
  } catch {
    // 仅调度补录/换证强依赖，查询失败不阻塞台账展示
    operators.value = []
  }
}

const initData = async () => {
  await Promise.all([loadCertificates(), isDispatcher() ? loadOperators() : Promise.resolve()])
  parseBatchText(false)
}

/** 解析粘贴文本：支持逗号/制表符/中文逗号分隔，跳过空行 */
const parseBatchText = (notify = true) => {
  rejected.value = null
  const rows: CertificateRow[] = batchText.value
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(line => line.length > 0)
    .map(line => {
      const parts = line.split(/[,，\t]/).map(p => p.trim())
      return {
        operatorCode: parts[0] || '',
        certificateNo: parts[1] || '',
        issueDate: parts[2] || '',
        expireDate: parts[3] || ''
      }
    })
  batchRows.value = rows
  if (notify) {
    ElMessage.success(rows.length ? `已解析出 ${rows.length} 条，可在表格中核对后整批提交` : '未解析出任何行')
  }
}

/** 把表格改动写回批量文本（提交前不必调用，提交直接用 batchRows） */
const syncRowsToText = () => {
  batchText.value = batchRows.value
    .map(r => [r.operatorCode, r.certificateNo, r.issueDate, r.expireDate].join(','))
    .join('\n')
}

const addRow = () => {
  batchRows.value.push({ operatorCode: '', certificateNo: '', issueDate: '', expireDate: '' })
}

const removeRow = (index: number) => {
  batchRows.value.splice(index, 1)
}

/** 统一处理写台账失败：422 整批被拦 / 409 有人在写 / 403 越权 */
const handleSubmitError = (error: any) => {
  const status = error.response?.status
  if (status === 422) {
    const detail = error.response?.data?.detail as BatchRejectedDetail | undefined
    rejected.value = detail || null
    lastResult.value = ''
    ElMessage.error(detail?.reason || error.response?.data?.message || '整批未通过校验，已全部回滚')
  } else if (status === 409) {
    rejected.value = null
    ElMessage.warning(error.response?.data?.message || '已经有人在写台账，本批未写入，请稍后重试')
  } else if (status === 403) {
    ElMessage.error(error.response?.data?.message || '仅调度可登记/换发放飞证')
  } else if (status === 401) {
    ElMessage.error(error.response?.data?.message || '身份核对已失效，请重新核对')
    handleLogout()
  } else {
    ElMessage.error(error.response?.data?.message || '提交失败，本批未写入')
  }
}

const handleSubmitBatch = async () => {
  syncRowsToText()
  if (batchRows.value.length === 0) {
    ElMessage.warning('请先粘贴或添加至少一条证照')
    return
  }
  if (batchRows.value.some(r => !r.operatorCode || !r.certificateNo || !r.issueDate || !r.expireDate)) {
    ElMessage.warning('存在字段未填的行，请补全后再整批提交（后端也会逐条拦截）')
    return
  }
  try {
    await ElMessageBox.confirm(
      `将整批提交 ${batchRows.value.length} 条放飞证照。任一条不合格整批回滚、一条不落，是否继续？`,
      '整批提交确认',
      { type: 'warning', confirmButtonText: '整批提交', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    const result = await submitCertificateBatch(batchRows.value.map(r => ({ ...r })))
    rejected.value = null
    lastResult.value = result.message
    ElMessage.success(result.message)
    await loadCertificates()
  } catch (error: any) {
    handleSubmitError(error)
  } finally {
    submitting.value = false
  }
}

const onSingleOperatorChange = (code: string) => {
  single.value.operatorCode = code
  // 换证默认建议下一张证号，仍可改
  const op = operators.value.find(o => o.operatorCode === code)
  if (op) {
    const latest = latestExpireByOperator.value[code]
    if (latest) {
      const m = latest.certNo.match(/^(.*?)(\d+)$/)
      single.value.certificateNo = m
        ? m[1] + String(Number(m[2]) + 1).padStart(m[2].length, '0')
        : latest.certNo + '-NEW'
    }
  }
}

const handleRenewOne = async () => {
  if (!single.value.operatorCode || !single.value.certificateNo || !single.value.issueDate || !single.value.expireDate) {
    ElMessage.warning('请补全持人、新证号、发证日期和到期日期')
    return
  }
  submitting.value = true
  try {
    const result = await renewCertificate({ ...single.value })
    ElMessage.success(result.message)
    single.value = { operatorCode: '', certificateNo: '', issueDate: '', expireDate: '' }
    await loadCertificates()
  } catch (error: any) {
    handleSubmitError(error)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (verified.value) {
    initData()
  }
})
</script>

<template>
  <div class="page-container">
    <!-- 身份核对栏 -->
    <div class="identity-bar">
      <template v-if="!verified">
        <el-form inline @submit.prevent>
          <el-form-item label="工号" required>
            <el-input v-model="identityCode" placeholder="调度/放飞员工号" style="width: 180px"
              @keyup.enter="handleVerify" />
          </el-form-item>
          <el-form-item label="口令" required>
            <el-input v-model="identityPassword" type="password" placeholder="请输入口令" show-password
              style="width: 180px" @keyup.enter="handleVerify" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="verifying" @click="handleVerify">身份核对</el-button>
          </el-form-item>
        </el-form>
        <div class="identity-hint">
          放飞证照台账仅调度可登记/换发；放飞员身份核对后为只读，不能改。先做身份核对再查看。
        </div>
      </template>
      <template v-else>
        <div class="identity-info">
          <el-tag :type="isDispatcher() ? 'danger' : 'success'" effect="dark">{{ roleText }}</el-tag>
          <span class="operator-name">{{ identity.operator?.operatorName }}（{{ identity.operator?.operatorCode }}）</span>
          <span v-if="isDispatcher()" class="duty-tip">可把历年散落证照整批补进台账，并为在册人员换新证</span>
          <span v-else class="duty-tip">放飞员只读视图：可查看证照台账，登记/换发请联系调度</span>
          <el-button size="small" style="margin-left: auto" @click="handleLogout">退出核对</el-button>
        </div>
      </template>
    </div>

    <template v-if="verified">
      <!-- 调度：整批补录 / 单条换证 -->
      <template v-if="isDispatcher()">
        <el-tabs v-model="modeTab" type="card">
          <!-- 整批补录/换证 -->
          <el-tab-pane label="整批补录 / 换证" name="batch">
            <div class="entry-card">
              <div class="entry-title">批量录入（每行一条：工号,证号,发证日期,到期日期）</div>
              <el-input
                v-model="batchText"
                type="textarea"
                :rows="6"
                spellcheck="false"
                placeholder="OP-001,CERT-OP001-02,2025-04-01,2026-03-31"
              />
              <div class="row-actions">
                <el-button @click="parseBatchText(true)">解析到表格</el-button>
                <el-button @click="addRow">加一行</el-button>
                <el-button type="primary" :loading="submitting" @click="handleSubmitBatch">
                  整批提交（任一不合格全部回滚）
                </el-button>
                <span class="entry-hint">日期格式 yyyy-MM-dd；到期日必须晚于发证日，换证新证到期日必须严格晚于该人现有最晚到期日。</span>
              </div>

              <el-table :data="batchRows" border size="small" class="batch-table">
                <el-table-column type="index" label="#" width="50" />
                <el-table-column label="工号" min-width="130">
                  <template #default="{ row }">
                    <el-input v-model="row.operatorCode" size="small" placeholder="工号" />
                  </template>
                </el-table-column>
                <el-table-column label="证号" min-width="160">
                  <template #default="{ row }">
                    <el-input v-model="row.certificateNo" size="small" placeholder="证号" />
                  </template>
                </el-table-column>
                <el-table-column label="发证日期" min-width="150">
                  <template #default="{ row }">
                    <el-input v-model="row.issueDate" size="small" placeholder="yyyy-MM-dd" />
                  </template>
                </el-table-column>
                <el-table-column label="到期日期" min-width="150">
                  <template #default="{ row }">
                    <el-input v-model="row.expireDate" size="small" placeholder="yyyy-MM-dd" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80">
                  <template #default="{ $index }">
                    <el-button link type="danger" size="small" @click="removeRow($index)">删</el-button>
                  </template>
                </el-table-column>
              </el-table>

              <el-alert
                v-if="rejected"
                class="result-alert"
                type="error"
                :closable="true"
                show-icon
                @close="rejected = null"
              >
                <template #title>整批未落库，已全部回滚</template>
                <div class="reject-detail">
                  <div><b>卡在第 {{ rejected.rowNumber }} 条</b>：{{ rejected.reason }}</div>
                  <div v-if="rejected.currentHolderCode" class="holder-line">
                    该证号当前占用人：{{ rejected.currentHolderName }}（工号 {{ rejected.currentHolderCode }}）
                  </div>
                  <div class="reject-row">
                    该行内容：{{ rejected.row?.operatorCode }} ｜ {{ rejected.row?.certificateNo }} ｜
                    {{ rejected.row?.issueDate }} ｜ {{ rejected.row?.expireDate }}
                  </div>
                </div>
              </el-alert>
              <el-alert
                v-else-if="lastResult"
                class="result-alert"
                type="success"
                :closable="true"
                show-icon
                :title="lastResult"
                @close="lastResult = ''"
              />
            </div>
          </el-tab-pane>

          <!-- 单条换证 -->
          <el-tab-pane label="单条换证" name="single">
            <div class="entry-card">
              <div class="entry-title">为在册人员换新证（与批量同一套到期日次序，不合规照样拦下）</div>
              <el-form :model="single" label-width="110px" class="single-form">
                <el-form-item label="持人" required>
                  <el-select
                    :model-value="single.operatorCode"
                    placeholder="选择在册人员"
                    filterable
                    style="width: 320px"
                    @change="onSingleOperatorChange"
                  >
                    <el-option
                      v-for="op in operators"
                      :key="op.operatorCode"
                      :label="`${op.operatorCode} - ${op.operatorName}（${roleName(op.role)}）`"
                      :value="op.operatorCode"
                    />
                  </el-select>
                  <el-tag
                    v-if="selectedOperator"
                    :type="roleTagType(selectedOperator.role)"
                    size="small"
                    style="margin-left: 10px"
                  >
                    {{ roleName(selectedOperator.role) }}
                  </el-tag>
                </el-form-item>
                <el-form-item v-if="selectedOperatorLatest" label="现有最晚到期">
                  <span class="latest-tip">
                    {{ selectedOperatorLatest.date }}（证号 {{ selectedOperatorLatest.certNo }}）
                    —— 新证到期日必须严格晚于此日期
                  </span>
                </el-form-item>
                <el-form-item label="新证号" required>
                  <el-input v-model="single.certificateNo" placeholder="新证号，不能与库里或本批撞号" style="width: 320px" />
                </el-form-item>
                <el-form-item label="发证日期" required>
                  <el-date-picker
                    v-model="single.issueDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    format="YYYY-MM-DD"
                    placeholder="选择发证日期"
                    style="width: 320px"
                  />
                </el-form-item>
                <el-form-item label="到期日期" required>
                  <el-date-picker
                    v-model="single.expireDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    format="YYYY-MM-DD"
                    placeholder="选择到期日期"
                    style="width: 320px"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="submitting" @click="handleRenewOne">提交换证</el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>

      <!-- 放飞员只读提示 -->
      <el-alert
        v-else
        class="readonly-alert"
        type="info"
        :closable="false"
        show-icon
        title="放飞员为只读权限：可查看放飞证照台账，补录与换发仅调度可操作。"
      />

      <!-- 台账 -->
      <div class="ledger-card">
        <div class="ledger-header">
          <span class="entry-title">放飞证照台账（共 {{ certificates.length }} 张）</span>
          <el-button size="small" :loading="loading" @click="loadCertificates">刷新</el-button>
        </div>
        <el-table :data="certificates" v-loading="loading" border size="small">
          <el-table-column type="index" label="#" width="55" />
          <el-table-column prop="operatorCode" label="工号" width="110" />
          <el-table-column prop="operatorName" label="姓名" min-width="120" />
          <el-table-column prop="certificateNo" label="证号" min-width="150" />
          <el-table-column prop="issueDate" label="发证日期" width="120" />
          <el-table-column prop="expireDate" label="到期日期" width="120" />
          <el-table-column prop="batchNo" label="批次号" min-width="190" />
          <el-table-column prop="createdOperatorCode" label="经办调度" width="110" />
        </el-table>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page-container {
  padding: 20px;
}

.identity-bar {
  margin-bottom: 20px;
  padding: 15px;
  background: #f0f5ff;
  border: 1px solid #d6e4ff;
  border-radius: 8px;
}

.identity-hint {
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
}

.identity-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.operator-name {
  font-weight: 600;
  color: #303133;
}

.duty-tip {
  font-size: 12px;
  color: #909399;
}

.entry-card,
.ledger-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 18px;
}

.entry-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 12px 0;
  flex-wrap: wrap;
}

.entry-hint {
  font-size: 12px;
  color: #909399;
}

.batch-table {
  margin-top: 8px;
}

.result-alert {
  margin-top: 14px;
}

.reject-detail {
  line-height: 1.8;
  font-size: 13px;
}

.holder-line {
  color: #b88230;
}

.reject-row {
  color: #909399;
}

.single-form {
  max-width: 640px;
}

.latest-tip {
  color: #e6a23c;
  font-size: 13px;
}

.readonly-alert {
  margin-bottom: 18px;
}

.ledger-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
</style>
