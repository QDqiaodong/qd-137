<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Bracket, DutyShift, ReturnItem } from '@/types'
import { getBrackets } from '@/api/bracket'
import {
  getHandoffStatus,
  startShift,
  closeShift,
  getShiftItems,
  addReturnItem,
  markItemStatus
} from '@/api/returnHandoff'
import { verifyIdentity } from '@/api/auth'
import { identity, setIdentity, clearIdentity } from '@/stores/identity'

// 身份核对表单
const identityCode = ref('')
const identityPassword = ref('')
const verifying = ref(false)
const verified = computed(() => identity.operator != null)

const loading = ref(false)
const busy = ref(false)

// 交接状态
const activeShift = ref<DutyShift | null>(null)
const pendingShift = ref<DutyShift | null>(null)
const pendingItems = ref<ReturnItem[]>([])
const canStartShift = ref(false)
const statusMessage = ref('')

// 本班清单
const activeItems = ref<ReturnItem[]>([])

// 登记支架
const brackets = ref<Bracket[]>([])
const selectedBracketId = ref<number | null>(null)

const isHandoff = computed(() => activeShift.value == null)
const activeOnSiteCount = computed(() =>
  activeItems.value.filter(i => i.returnStatus === 'ON_SITE').length
)

/** 本班还没登记过的支架才出现在下拉里，同一支架不重复登记 */
const selectableBrackets = computed(() => {
  const addedIds = new Set(activeItems.value.map(i => i.bracketId))
  return brackets.value.filter(b => !addedIds.has(b.id))
})

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
    ElMessage.success(`身份核对通过：${operator.operatorName}`)
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
  activeShift.value = null
  pendingShift.value = null
  pendingItems.value = []
  activeItems.value = []
}

const loadBrackets = async () => {
  try {
    brackets.value = await getBrackets()
  } catch (error) {
    ElMessage.error('加载支架失败')
  }
}

/** 拉取交接状态；本班进行中时顺带拉本班完整清单 */
const refresh = async () => {
  loading.value = true
  try {
    const status = await getHandoffStatus()
    activeShift.value = status.activeShift
    pendingShift.value = status.pendingShift
    pendingItems.value = status.pendingItems
    canStartShift.value = status.canStartShift
    statusMessage.value = status.message

    if (status.activeShift) {
      activeItems.value = await getShiftItems(status.activeShift.id)
    } else {
      activeItems.value = []
    }
  } catch (error: any) {
    if (error.response?.status === 401) {
      ElMessage.error(error.response?.data?.message || '身份核对已失效，请重新核对')
      handleLogout()
    } else {
      ElMessage.error('加载归位状态失败')
    }
  } finally {
    loading.value = false
  }
}

const initData = async () => {
  await Promise.all([loadBrackets(), refresh()])
}

/** 开始本班：上一班还有未收回支架时后端会拦截（403） */
const handleStartShift = async () => {
  busy.value = true
  try {
    const shift = await startShift()
    ElMessage.success(`本班已开始：${shift.shiftCode}`)
    selectedBracketId.value = null
    await refresh()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '开始本班失败')
    await refresh()
  } finally {
    busy.value = false
  }
}

/** 下班交接 */
const handleCloseShift = async () => {
  const left = activeOnSiteCount.value
  try {
    await ElMessageBox.confirm(
      left > 0
        ? `本班还有 ${left} 个支架停在场地，交接后下一班必须先收回才能开始本班，确认交接？`
        : '本班支架已全部收回停放区，确认交接下班？',
      '下班交接',
      { type: 'warning', confirmButtonText: '确认交接', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  busy.value = true
  try {
    await closeShift()
    ElMessage.success(left > 0 ? '已交接，遗留支架已交给下一班处理' : '已交接下班')
    await refresh()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '交接失败')
  } finally {
    busy.value = false
  }
}

/** 登记一个当天动过的支架（默认还停在场地） */
const handleAddItem = async () => {
  if (selectedBracketId.value == null) {
    ElMessage.warning('请选择当天动过的支架')
    return
  }
  busy.value = true
  try {
    await addReturnItem(selectedBracketId.value)
    ElMessage.success('已登记，当前标记为还停在场地')
    selectedBracketId.value = null
    await refresh()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '登记失败')
  } finally {
    busy.value = false
  }
}

/** 切换一条清单的归位状态 */
const handleToggleStatus = async (item: ReturnItem) => {
  const next = item.returnStatus === 'ON_SITE' ? 'RETURNED' : 'ON_SITE'
  busy.value = true
  try {
    await markItemStatus(item.id, next)
    ElMessage.success(next === 'RETURNED' ? '已标记为收回停放区' : '已改回还停在场地')
    await refresh()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '状态更新失败')
  } finally {
    busy.value = false
  }
}

const formatTime = (value?: string | null) => {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(async () => {
  if (verified.value) {
    await initData()
  }
})
</script>

<template>
  <div class="page-container" v-loading="loading">
    <!-- 身份核对 -->
    <div class="identity-bar">
      <template v-if="!verified">
        <el-form inline @submit.prevent>
          <el-form-item label="工号" required>
            <el-input v-model="identityCode" placeholder="请输入工号" style="width: 180px"
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
          地勤归位交接：下班前把当天动过的地面固定支架逐一登记，标明已收回停放区还是还停在场地；
          下一班打开本页必须先收回上一班遗留的支架，全部收回后才能开始本班。
        </div>
      </template>
      <template v-else>
        <div class="identity-info">
          <el-tag type="warning" effect="dark">地勤交接</el-tag>
          <span class="operator-name">{{ identity.operator?.operatorName }}（{{ identity.operator?.operatorCode }}）</span>
          <el-button size="small" style="margin-left: auto" @click="handleLogout">退出核对</el-button>
        </div>
      </template>
    </div>

    <template v-if="verified">
      <!-- 交接间隙：上一班遗留 + 开始本班 -->
      <template v-if="isHandoff">
        <!-- 未收回清单永远排在最前面 -->
        <el-alert
          :title="statusMessage"
          :type="canStartShift ? 'success' : 'error'"
          :closable="false"
          show-icon
          class="top-alert"
        />

        <div v-if="pendingShift" class="section">
          <div class="section-title">
            上一班遗留：{{ pendingShift.shiftCode }}
            <el-tag size="small" type="info" effect="plain">
              交接于 {{ formatTime(pendingShift.closedAt) }}
            </el-tag>
            <el-tag size="small" :type="pendingItems.length ? 'danger' : 'success'">
              未收回 {{ pendingItems.length }} 个
            </el-tag>
          </div>

          <el-table :data="pendingItems" border style="width: 100%" v-if="pendingItems.length">
            <el-table-column prop="bracketCode" label="支架编号" width="140" />
            <el-table-column prop="bracketName" label="支架名称" />
            <el-table-column label="当前状态" width="140">
              <template #default>
                <el-tag type="danger" effect="dark">还停在场地</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button size="small" type="success" :loading="busy" @click="handleToggleStatus(row)">
                  标记已收回停放区
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-else description="上一班支架已全部收回停放区" :image-size="80" />
        </div>

        <!-- 开始本班：有遗留未收回时禁用 -->
        <div class="start-bar">
          <el-button
            type="primary"
            size="large"
            :disabled="!canStartShift"
            :loading="busy"
            @click="handleStartShift">
            开始本班
          </el-button>
          <span v-if="!canStartShift" class="start-tip">
            还有 {{ pendingItems.length }} 个支架停在场地，全部收回后才能开始本班
          </span>
          <span v-else class="start-tip ok">本班可以开始作业</span>
        </div>
      </template>

      <!-- 本班进行中：登记 + 归位清单 -->
      <template v-else>
        <div class="shift-bar">
          <div class="section-title">
            本班进行中：{{ activeShift?.shiftCode }}
            <el-tag size="small" type="success" effect="dark">ACTIVE</el-tag>
            <el-tag size="small" type="info" effect="plain">
              开始于 {{ formatTime(activeShift?.startedAt) }}
            </el-tag>
          </div>
          <el-button type="warning" :loading="busy" @click="handleCloseShift">下班交接</el-button>
        </div>

        <!-- 登记当天动过的支架 -->
        <div class="register-bar">
          <el-form inline @submit.prevent>
            <el-form-item label="当天动过的支架">
              <el-select v-model="selectedBracketId" placeholder="选择支架编号"
                filterable style="width: 320px">
                <el-option v-for="b in selectableBrackets" :key="b.id"
                  :label="`${b.bracketCode} - ${b.bracketName}`" :value="b.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :disabled="selectedBracketId == null"
                :loading="busy" @click="handleAddItem">
                加入归位清单
              </el-button>
            </el-form-item>
          </el-form>
          <div class="register-hint">
            新加入的支架默认「还停在场地」，收回后在下方清单中逐条标记为「已收回停放区」。
          </div>
        </div>

        <!-- 本班归位清单：未收回排前面 -->
        <div class="section">
          <div class="section-title">
            本班归位清单
            <el-tag size="small" :type="activeOnSiteCount ? 'danger' : 'success'">
              未收回 {{ activeOnSiteCount }} / 共 {{ activeItems.length }}
            </el-tag>
          </div>

          <el-table :data="[...activeItems].sort((a, b) => {
              if (a.returnStatus === b.returnStatus) return 0
              return a.returnStatus === 'ON_SITE' ? -1 : 1
            })" border style="width: 100%">
            <el-table-column prop="bracketCode" label="支架编号" width="140" />
            <el-table-column prop="bracketName" label="支架名称" />
            <el-table-column label="归位状态" width="150">
              <template #default="{ row }">
                <el-tag :type="row.returnStatus === 'RETURNED' ? 'success' : 'danger'" effect="dark">
                  {{ row.returnStatus === 'RETURNED' ? '已收回停放区' : '还停在场地' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="登记时间" width="160">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="收回时间" width="160">
              <template #default="{ row }">{{ formatTime(row.returnedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button v-if="row.returnStatus === 'ON_SITE'" size="small" type="success"
                  :loading="busy" @click="handleToggleStatus(row)">
                  标记已收回
                </el-button>
                <el-button v-else size="small" :loading="busy" @click="handleToggleStatus(row)">
                  改回未收回
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="activeItems.length === 0" description="本班还没有登记动过的支架" :image-size="80" />
        </div>
      </template>
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
  background: #fdf6ec;
  border: 1px solid #faecd8;
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

.top-alert {
  margin-bottom: 20px;
}

.section {
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.start-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
}

.start-tip {
  font-size: 13px;
  color: #f56c6c;
  font-weight: 600;
}

.start-tip.ok {
  color: #67c23a;
}

.shift-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.register-bar {
  margin-bottom: 20px;
  padding: 15px;
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 8px;
}

.register-hint {
  font-size: 12px;
  color: #909399;
}
</style>
