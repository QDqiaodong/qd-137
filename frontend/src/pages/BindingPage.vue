<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Route, Bracket, Binding } from '@/types'
import { getRoutes } from '@/api/route'
import { getBrackets } from '@/api/bracket'
import { createBinding, deleteBinding, getBindingsByRoute, checkMatch } from '@/api/binding'
import { verifyIdentity } from '@/api/auth'
import { identity, setIdentity, clearIdentity, isDispatcher } from '@/stores/identity'

const routes = ref<Route[]>([])
const brackets = ref<Bracket[]>([])
const bindings = ref<Binding[]>([])
const loading = ref(false)
const selectedRoute = ref<number | null>(null)
const selectedBracket = ref<number | null>(null)
const launchWindSpeed = ref<number>()

const FILTER_STORAGE_KEY = 'binding-wind-status-filter'
const ROUTE_STORAGE_KEY = 'binding-selected-route'

type WindStatusFilter = 'ALL' | 'MATCHED' | 'MISMATCH'
const windStatusFilter = ref<WindStatusFilter>('ALL')

// 身份核对表单
const identityCode = ref('')
const identityPassword = ref('')
const verifying = ref(false)

const columns = [
  { prop: 'bracketCode', label: '支架编号' },
  { prop: 'bracketName', label: '支架名称' },
  { prop: 'maxLoad', label: '最大承重(kg)' },
  { prop: 'bracketMinWind', label: '支架最小风力' },
  { prop: 'bracketMaxWind', label: '支架最大风力' },
  { prop: 'launchWindSpeed', label: '当日放飞风速', width: 120 },
  { prop: 'matchLevel', label: '匹配等级', width: 100 },
  { prop: 'windStatus', label: '区间状态', width: 100 },
  { prop: 'action', label: '操作', width: 100 }
]

const verified = computed(() => identity.operator != null)

// 放飞员只读：不展示改挂操作列
const visibleColumns = computed(() => {
  if (isDispatcher()) return columns
  return columns.filter(c => c.prop !== 'action')
})

// 调度可见全部航线；放飞员仅可见自己当班航线
const selectableRoutes = computed(() => {
  if (isDispatcher()) return routes.value
  const dutyIds = identity.operator?.dutyRouteIds ?? []
  return routes.value.filter(r => dutyIds.includes(r.id))
})

const roleText = computed(() => (isDispatcher() ? '调度' : '放飞员'))

const currentRoute = computed(() => {
  return routes.value.find(r => r.id === selectedRoute.value)
})

const currentBracket = computed(() => {
  return brackets.value.find(b => b.id === selectedBracket.value)
})

const effectiveWindStatus = (binding: Binding) => binding.windStatus || 'MATCHED'

const filteredBindings = computed(() => {
  if (windStatusFilter.value === 'ALL') return bindings.value
  return bindings.value.filter(b => effectiveWindStatus(b) === windStatusFilter.value)
})

watch(windStatusFilter, (val) => {
  localStorage.setItem(FILTER_STORAGE_KEY, val)
})

watch(selectedRoute, (val) => {
  if (val == null) {
    localStorage.removeItem(ROUTE_STORAGE_KEY)
  } else {
    localStorage.setItem(ROUTE_STORAGE_KEY, String(val))
  }
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
    ElMessage.success(`身份核对通过：${operator.operatorName}（${operator.role === 'DISPATCHER' ? '调度' : '放飞员'}）`)
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
  routes.value = []
  brackets.value = []
  bindings.value = []
  selectedRoute.value = null
  selectedBracket.value = null
  launchWindSpeed.value = undefined
}

const loadRoutes = async () => {
  try {
    routes.value = await getRoutes()
  } catch (error) {
    ElMessage.error('加载航线失败')
  }
}

const loadBrackets = async () => {
  try {
    brackets.value = await getBrackets()
  } catch (error) {
    ElMessage.error('加载支架失败')
  }
}

const loadBindings = async () => {
  if (!selectedRoute.value) {
    bindings.value = []
    return
  }
  loading.value = true
  try {
    bindings.value = await getBindingsByRoute(selectedRoute.value)
  } catch (error: any) {
    const status = error.response?.status
    if (status === 401) {
      ElMessage.error(error.response?.data?.message || '身份核对已失效，请重新核对')
      handleLogout()
    } else if (status === 403) {
      ElMessage.error(error.response?.data?.message || '无权查看该航线')
      bindings.value = []
    } else {
      ElMessage.error('加载绑定关系失败')
    }
  } finally {
    loading.value = false
  }
}

const handleRouteChange = async (routeId: number | null) => {
  selectedRoute.value = routeId
  selectedBracket.value = null
  await loadBindings()
}

const handleBind = async () => {
  if (!selectedRoute.value || !selectedBracket.value) {
    ElMessage.warning('请选择航线和支架')
    return
  }

  try {
    const check = await checkMatch(selectedRoute.value, selectedBracket.value, launchWindSpeed.value ?? null)
    if (!check.matched) {
      await ElMessageBox.confirm(
        '该支架适飞窗口与航线适飞窗口无重叠，提交后将标记为区间错位且无法完成绑定，是否继续？',
        '提示',
        { type: 'warning' }
      )
    }

    const result = await createBinding(selectedRoute.value, selectedBracket.value, launchWindSpeed.value ?? null)
    if (result.windStatus === 'MISMATCH') {
      ElMessage.error(result.message || '区间错位，不能完成绑定')
    } else {
      ElMessage.success('绑定成功')
      selectedBracket.value = null
      launchWindSpeed.value = undefined
    }
    await loadBindings()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '绑定失败')
    }
  }
}

const handleUnbind = async (binding: Binding) => {
  try {
    await ElMessageBox.confirm(`确定解绑支架 ${binding.bracketName} 吗？`, '提示', {
      type: 'warning'
    })
    await deleteBinding(binding.routeId, binding.bracketId)
    ElMessage.success('解绑成功')
    await loadBindings()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '解绑失败')
    }
  }
}

const getMatchLevelText = (level: string) => {
  const map: Record<string, string> = {
    'PERFECT': '完全匹配',
    'PARTIAL': '部分匹配',
    'NONE': '不匹配'
  }
  return map[level] || level
}

const getMatchLevelColor = (level: string) => {
  const map: Record<string, string> = {
    'PERFECT': '#67c23a',
    'PARTIAL': '#e6a23c',
    'NONE': '#f56c6c'
  }
  return map[level] || '#909399'
}

const initData = async () => {
  await Promise.all([loadRoutes(), loadBrackets()])
  const savedRouteId = Number(localStorage.getItem(ROUTE_STORAGE_KEY))
  if (savedRouteId && selectableRoutes.value.some(r => r.id === savedRouteId)) {
    selectedRoute.value = savedRouteId
    await loadBindings()
  }
}

onMounted(async () => {
  const savedFilter = localStorage.getItem(FILTER_STORAGE_KEY)
  if (savedFilter === 'ALL' || savedFilter === 'MATCHED' || savedFilter === 'MISMATCH') {
    windStatusFilter.value = savedFilter
  }
  // 已核对身份（会话内）才加载数据；否则先完成身份核对
  if (verified.value) {
    await initData()
  }
})
</script>

<template>
  <div class="page-container">
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
          航线支架挂接已按角色收窄：先做身份核对再操作。调度可改挂任意航线；放飞员仅可查看自己当班航线已挂接的支架。
        </div>
      </template>
      <template v-else>
        <div class="identity-info">
          <el-tag :type="isDispatcher() ? 'danger' : 'success'" effect="dark">{{ roleText }}</el-tag>
          <span class="operator-name">{{ identity.operator?.operatorName }}（{{ identity.operator?.operatorCode }}）</span>
          <span v-if="!isDispatcher()" class="duty-tip">
            当班航线 {{ selectableRoutes.length }} 条，只读权限，不能改挂
          </span>
          <span v-else class="duty-tip">可改挂任意航线的支架挂接</span>
          <el-button size="small" style="margin-left: auto" @click="handleLogout">退出核对</el-button>
        </div>
      </template>
    </div>

    <template v-if="verified">
      <div class="selection-bar">
        <el-form :model="{ route: selectedRoute }" inline>
          <el-form-item label="选择航线" required>
            <el-select v-model="selectedRoute" placeholder="请选择航线" @change="handleRouteChange" style="width: 250px">
              <el-option v-for="route in selectableRoutes" :key="route.id"
                :label="`${route.routeCode} - ${route.routeName}`" :value="route.id" />
            </el-select>
          </el-form-item>
          <template v-if="isDispatcher()">
            <el-form-item label="选择支架">
              <el-select v-model="selectedBracket" placeholder="请选择支架" style="width: 250px">
                <el-option v-for="bracket in brackets" :key="bracket.id"
                  :label="`${bracket.bracketCode} - ${bracket.bracketName}`" :value="bracket.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="当日放飞风速(m/s)">
              <el-input-number v-model="launchWindSpeed" :min="0" :max="30" :step="0.5" :precision="1"
                controls-position="right" placeholder="实测风速" style="width: 150px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleBind" :disabled="!selectedRoute || !selectedBracket">绑定</el-button>
            </el-form-item>
          </template>
        </el-form>
        <div v-if="isDispatcher()" class="wind-hint">
          放飞员须当场核对：当日放飞风速需同时落在航线与支架各自的适飞窗口内；未填写风速或不在双方窗口内将标记为区间错位，不能完成绑定。
        </div>
        <div v-else class="wind-hint">
          放飞员只读视图：仅可查看自己当班航线已经挂上的支架；改挂请联系调度。
        </div>
      </div>

      <div v-if="currentRoute" class="route-info">
        <el-card>
          <div class="info-row">
            <span class="label">航线名称：</span>
            <span>{{ currentRoute.routeName }}</span>
            <span class="label" style="margin-left: 30px">航线适飞窗口：</span>
            <span>{{ currentRoute.minWindSpeed }} - {{ currentRoute.maxWindSpeed }} m/s</span>
            <template v-if="isDispatcher() && currentBracket">
              <span class="label" style="margin-left: 30px">支架适飞窗口：</span>
              <span>{{ currentBracket.minWindSpeed }} - {{ currentBracket.maxWindSpeed }} m/s</span>
            </template>
          </div>
        </el-card>
      </div>

      <div v-if="selectedRoute" class="filter-bar">
        <span class="filter-label">区间状态筛选：</span>
        <el-radio-group v-model="windStatusFilter">
          <el-radio-button value="ALL">全部</el-radio-button>
          <el-radio-button value="MATCHED">区间匹配</el-radio-button>
          <el-radio-button value="MISMATCH">区间错位</el-radio-button>
        </el-radio-group>
      </div>

      <div class="table-container">
        <el-table :data="filteredBindings" :loading="loading" border style="width: 100%">
          <el-table-column v-for="col in visibleColumns" :key="col.prop" :prop="col.prop" :label="col.label">
            <template #default="{ row }">
              <template v-if="col.prop === 'matchLevel'">
                <span :style="{ color: getMatchLevelColor(row.matchLevel) }">
                  {{ getMatchLevelText(row.matchLevel) }}
                </span>
              </template>
              <template v-else-if="col.prop === 'launchWindSpeed'">
                {{ row.launchWindSpeed != null ? row.launchWindSpeed + ' m/s' : '未填写' }}
              </template>
              <template v-else-if="col.prop === 'windStatus'">
                <span :style="{ color: effectiveWindStatus(row) === 'MISMATCH' ? '#f56c6c' : '#67c23a', fontWeight: 'bold' }">
                  {{ effectiveWindStatus(row) === 'MISMATCH' ? '区间错位' : '区间匹配' }}
                </span>
              </template>
              <template v-else-if="col.prop === 'action'">
                <el-button size="small" type="danger" @click="handleUnbind(row)">解绑</el-button>
              </template>
              <template v-else>
                {{ row[col.prop] }}
              </template>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="filteredBindings.length === 0 && selectedRoute" class="empty-tip">
          {{ bindings.length === 0
            ? (isDispatcher() ? '暂无绑定的支架，可选择上方支架进行绑定' : '该航线暂无已挂接的支架')
            : '当前筛选条件下暂无记录' }}
        </div>
        <div v-if="!selectedRoute && selectableRoutes.length === 0 && !isDispatcher()" class="empty-tip">
          当前账号没有当班航线，请联系调度排班
        </div>
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

.selection-bar {
  margin-bottom: 20px;
  padding: 15px;
  background: #fafafa;
  border-radius: 8px;
}

.wind-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.route-info {
  margin-bottom: 20px;
}

.info-row {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.label {
  font-weight: 500;
  color: #606266;
}

.filter-bar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  margin-right: 10px;
}

.table-container {
  background: white;
  border-radius: 8px;
  padding: 10px;
}

.empty-tip {
  text-align: center;
  padding: 40px;
  color: #909399;
}
</style>
