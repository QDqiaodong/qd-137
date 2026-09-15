<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Route, DailyFlightSummary } from '@/types'
import {
  getRoutes,
  createRoute,
  updateRoute,
  deleteRoute,
  getRouteGroups,
  getRoutesByGroup,
  rematchBracketsForRoute
} from '@/api/route'
import { getDailyFlightsByDate, recordDailyFlight } from '@/api/dailyFlight'
import { verifyIdentity } from '@/api/auth'
import { identity, setIdentity, isDispatcher } from '@/stores/identity'

const routes = ref<Route[]>([])
const groups = ref<string[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const selectedGroup = ref('')

// 调度按日期查看各航线当天合计
const todayStr = () => {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}
const selectedDate = ref(todayStr())
const summaries = ref<DailyFlightSummary[]>([])
const summaryLoading = ref(false)

// 登记飞行（仅调度）
const recordDialogVisible = ref(false)
const recordSubmitting = ref(false)
const recordRouteId = ref(0)
const recordForm = ref({ flightCount: 1, durationMinutes: 30 })
// 调度身份核对（登记前）
const identityCode = ref('')
const identityPassword = ref('')
const verifying = ref(false)

const form = ref({
  id: 0,
  routeCode: '',
  routeName: '',
  groupName: '',
  minWindSpeed: 0,
  maxWindSpeed: 0,
  distance: 0,
  duration: 0,
  difficultyLevel: '',
  status: 'ACTIVE',
  description: ''
})

const columns = [
  { prop: 'routeCode', label: '航线编号' },
  { prop: 'routeName', label: '航线名称' },
  { prop: 'groupName', label: '分组' },
  { prop: 'minWindSpeed', label: '最小风力(m/s)' },
  { prop: 'maxWindSpeed', label: '最大风力(m/s)' },
  { prop: 'distance', label: '距离(km)' },
  { prop: 'duration', label: '参考时长(分钟)' },
  { prop: 'flightCount', label: `当天趟次` },
  { prop: 'totalDuration', label: '当天总时长(分钟)' },
  { prop: 'difficultyLevel', label: '难度等级' },
  { prop: 'status', label: '状态', formatter: (row: Route) => row.status === 'ACTIVE' ? '启用' : '停用' },
  { prop: 'action', label: '操作', width: 260 }
]

const filteredRoutes = computed(() => {
  if (!selectedGroup.value) return routes.value
  return routes.value.filter(r => r.groupName === selectedGroup.value)
})

// 以当前分组后的航线为基准，补上当天合计；没记过的日期显示“还没有趟次”
const tableRows = computed(() => {
  const summaryMap = new Map<number, DailyFlightSummary>(
    summaries.value.map(s => [s.routeId, s])
  )
  return filteredRoutes.value.map(r => {
    const s = summaryMap.get(r.id)
    return {
      ...r,
      flightCount: s?.recorded ? s.flightCount : null,
      totalDuration: s?.recorded ? s.totalDuration : null
    }
  })
})

const hasAnyRecord = computed(() => summaries.value.some(s => s.recorded))

const getSummary = (routeId: number) => summaries.value.find(s => s.routeId === routeId)

const loadRoutes = async () => {
  loading.value = true
  try {
    routes.value = await getRoutes()
  } catch (error) {
    ElMessage.error('加载航线失败')
  } finally {
    loading.value = false
  }
}

const loadGroups = async () => {
  try {
    groups.value = await getRouteGroups()
  } catch (error) {
    console.error('加载分组失败', error)
  }
}

const loadSummaries = async () => {
  if (!selectedDate.value) {
    summaries.value = []
    return
  }
  summaryLoading.value = true
  try {
    summaries.value = await getDailyFlightsByDate(selectedDate.value)
  } catch (error) {
    ElMessage.error('加载当日飞行合计失败')
    summaries.value = []
  } finally {
    summaryLoading.value = false
  }
}

const handleDateChange = () => loadSummaries()

const handleGroupChange = async (group: string) => {
  if (!group) {
    await loadRoutes()
    return
  }
  loading.value = true
  try {
    routes.value = await getRoutesByGroup(group)
  } catch (error) {
    ElMessage.error('加载分组航线失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  form.value = {
    id: 0,
    routeCode: '',
    routeName: '',
    groupName: '',
    minWindSpeed: 0,
    maxWindSpeed: 0,
    distance: 0,
    duration: 0,
    difficultyLevel: '',
    status: 'ACTIVE',
    description: ''
  }
  dialogVisible.value = true
}

const handleEdit = (row: Route) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (row: Route) => {
  try {
    await ElMessageBox.confirm(`确定删除航线 ${row.routeName} 吗？`, '提示', {
      type: 'warning'
    })
    await deleteRoute(row.id)
    ElMessage.success('删除成功')
    await loadRoutes()
    await loadSummaries()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleRematch = async (row: Route) => {
  try {
    const result = await rematchBracketsForRoute(row.id)
    ElMessage.success(`重新匹配完成：${result.message}`)
  } catch (error) {
    ElMessage.error('重新匹配失败')
  }
}

// 打开登记窗口；仅已完成身份核对的调度可登记
const openRecordDialog = (row: Route) => {
  if (!identity.operator) {
    ElMessage.warning('请先在页面上方完成调度身份核对')
    return
  }
  if (!isDispatcher()) {
    ElMessage.error('只有调度可以登记飞行趟次')
    return
  }
  recordRouteId.value = row.id
  recordForm.value = { flightCount: 1, durationMinutes: row.duration || 30 }
  recordDialogVisible.value = true
}

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
    if (operator.role !== 'DISPATCHER') {
      ElMessage.warning('登记飞行趟次仅调度可操作')
      return
    }
    ElMessage.success(`身份核对通过：${operator.operatorName}（调度）`)
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '身份核对失败')
  } finally {
    verifying.value = false
  }
}

const recordRoute = computed(() => routes.value.find(r => r.id === recordRouteId.value))

const handleRecordSubmit = async () => {
  if (!recordRouteId.value || !selectedDate.value) return
  if (!recordForm.value.flightCount || recordForm.value.flightCount < 1) {
    ElMessage.warning('趟次至少为1')
    return
  }
  if (!recordForm.value.durationMinutes || recordForm.value.durationMinutes < 1) {
    ElMessage.warning('飞行时长至少为1分钟')
    return
  }
  recordSubmitting.value = true
  try {
    const updated = await recordDailyFlight({
      routeId: recordRouteId.value,
      flightDate: selectedDate.value,
      flightCount: recordForm.value.flightCount,
      durationMinutes: recordForm.value.durationMinutes
    })
    ElMessage.success(
      `已叠加到 ${selectedDate.value} ${updated.routeName}：合计 ${updated.flightCount} 趟 / ${updated.totalDuration} 分钟`
    )
    recordDialogVisible.value = false
    await loadSummaries()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '登记失败')
  } finally {
    recordSubmitting.value = false
  }
}

const handleSubmit = async () => {
  try {
    if (isEdit.value) {
      await updateRoute(form.value.id, form.value)
      ElMessage.success('更新成功，已自动重新匹配支架')
    } else {
      await createRoute(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadRoutes()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

const getStatusColor = (status: string) => {
  return status === 'ACTIVE' ? '#67c23a' : '#f56c6c'
}

onMounted(() => {
  loadRoutes()
  loadGroups()
  loadSummaries()
})
</script>

<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="{ group: selectedGroup }" inline>
        <el-form-item label="航线分组">
          <el-select v-model="selectedGroup" placeholder="全部" @change="handleGroupChange">
            <el-option label="全部" value="" />
            <el-option v-for="group in groups" :key="group" :label="group" :value="group" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="selectedDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            :clearable="false"
            @change="handleDateChange"
          />
        </el-form-item>
      </el-form>
      <el-button type="primary" @click="handleAdd" style="margin-left: auto">新增航线</el-button>
    </div>

    <!-- 调度身份核对（仅登记飞行趟次时需要） -->
    <div v-if="!identity.operator" class="identity-bar">
      <span class="identity-tip">登记飞行趟次需先完成调度身份核对：</span>
      <el-input v-model="identityCode" placeholder="调度工号，如 DISP-001" style="width: 200px" />
      <el-input v-model="identityPassword" type="password" placeholder="口令" show-password style="width: 180px" @keyup.enter="handleVerify" />
      <el-button type="primary" :loading="verifying" @click="handleVerify">身份核对</el-button>
    </div>
    <div v-else-if="!isDispatcher()" class="identity-bar">
      <span class="identity-tip">当前为放飞员身份，飞行趟次登记仅调度可操作；如需登记请在「航线支架绑定」页切换调度身份。</span>
    </div>

    <el-alert
      v-if="!summaryLoading && !hasAnyRecord"
      :title="`${selectedDate} 还没有趟次记录`"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 12px"
    />

    <el-table v-loading="loading || summaryLoading" :data="tableRows" border style="width: 100%">
      <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.prop === 'flightCount' ? `${selectedDate} 趟次` : col.prop === 'totalDuration' ? `${selectedDate} 总时长(分钟)` : col.label" :width="col.width">
        <template #default="{ row }">
          <template v-if="col.prop === 'status'">
            <span :style="{ color: getStatusColor(row.status) }">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</span>
          </template>
          <template v-else-if="col.prop === 'flightCount'">
            <el-tag v-if="getSummary(row.id)?.recorded" type="success">
              {{ getSummary(row.id)?.flightCount }} 趟
            </el-tag>
            <span v-else class="empty-text">还没有趟次</span>
          </template>
          <template v-else-if="col.prop === 'totalDuration'">
            <span v-if="getSummary(row.id)?.recorded">{{ getSummary(row.id)?.totalDuration }}</span>
            <span v-else class="empty-text">—</span>
          </template>
          <template v-else-if="col.prop === 'action'">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="success" @click="openRecordDialog(row)">登记飞行</el-button>
            <el-button size="small" type="warning" @click="handleRematch(row)">重新匹配</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
          <template v-else>
            {{ row[col.prop] }}
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- 登记飞行趟次：同日同航线重复登记会叠加到原合计行 -->
    <el-dialog title="登记飞行" :visible="recordDialogVisible" @close="recordDialogVisible = false">
      <el-form label-width="120px">
        <el-form-item label="航线">
          <span>{{ recordRoute?.routeCode }} / {{ recordRoute?.routeName }}</span>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="selectedDate"
            type="date"
            value-format="YYYY-MM-DD"
            :clearable="false"
            disabled
          />
        </el-form-item>
        <el-form-item label="本笔记几趟">
          <el-input-number v-model="recordForm.flightCount" :min="1" :step="1" />
        </el-form-item>
        <el-form-item label="本笔时长(分钟)">
          <el-input-number v-model="recordForm.durationMinutes" :min="1" :step="5" />
        </el-form-item>
        <el-alert
          v-if="getSummary(recordRouteId)?.recorded"
          :title="`当天已有 ${getSummary(recordRouteId)?.flightCount} 趟 / ${getSummary(recordRouteId)?.totalDuration} 分钟，本笔将叠加到原合计行，不另开新行`"
          type="warning"
          :closable="false"
          show-icon
        />
        <el-alert
          v-else
          title="当天还没有趟次，本笔将创建该航线当天的合计行"
          type="info"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="recordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="recordSubmitting" @click="handleRecordSubmit">确定登记</el-button>
      </template>
    </el-dialog>

    <el-dialog :title="isEdit ? '编辑航线' : '新增航线'" :visible="dialogVisible" @close="dialogVisible = false">
      <el-form :model="form" label-width="120px">
        <el-form-item label="航线编号" required>
          <el-input v-model="form.routeCode" />
        </el-form-item>
        <el-form-item label="航线名称" required>
          <el-input v-model="form.routeName" />
        </el-form-item>
        <el-form-item label="航线分组">
          <el-select v-model="form.groupName">
            <el-option v-for="group in groups" :key="group" :label="group" :value="group" />
            <el-option label="观光航线" value="观光航线" />
            <el-option label="竞赛航线" value="竞赛航线" />
            <el-option label="培训航线" value="培训航线" />
          </el-select>
        </el-form-item>
        <el-form-item label="最小风力(m/s)" required>
          <el-input v-model.number="form.minWindSpeed" type="number" />
        </el-form-item>
        <el-form-item label="最大风力(m/s)" required>
          <el-input v-model.number="form.maxWindSpeed" type="number" />
        </el-form-item>
        <el-form-item label="距离(km)">
          <el-input v-model.number="form.distance" type="number" />
        </el-form-item>
        <el-form-item label="参考时长(分钟)">
          <el-input v-model.number="form.duration" type="number" />
        </el-form-item>
        <el-form-item label="难度等级">
          <el-select v-model="form.difficultyLevel">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="DELETED" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  padding: 20px;
}

.search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 15px;
  background: #fafafa;
  border-radius: 8px;
}

.identity-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding: 12px 15px;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 8px;
}

.identity-tip {
  color: #ad6800;
  font-size: 13px;
}

.empty-text {
  color: #909399;
  font-size: 13px;
}
</style>
