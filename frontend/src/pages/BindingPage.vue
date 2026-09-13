<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Route, Bracket, Binding } from '@/types'
import { getRoutes } from '@/api/route'
import { getBrackets } from '@/api/bracket'
import { createBinding, deleteBinding, getBindingsByRoute, checkMatch } from '@/api/binding'

const routes = ref<Route[]>([])
const brackets = ref<Bracket[]>([])
const bindings = ref<Binding[]>([])
const loading = ref(false)
const selectedRoute = ref<number | null>(null)
const selectedBracket = ref<number | null>(null)

const columns = [
  { prop: 'bracketCode', label: '支架编号' },
  { prop: 'bracketName', label: '支架名称' },
  { prop: 'maxLoad', label: '最大承重(kg)' },
  { prop: 'bracketMinWind', label: '支架最小风力' },
  { prop: 'bracketMaxWind', label: '支架最大风力' },
  { prop: 'matchLevel', label: '匹配等级', width: 100 },
  { prop: 'action', label: '操作', width: 100 }
]

const currentRoute = computed(() => {
  return routes.value.find(r => r.id === selectedRoute.value)
})

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
  } catch (error) {
    ElMessage.error('加载绑定关系失败')
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
    const result = await checkMatch(selectedRoute.value, selectedBracket.value)
    if (!result.matched) {
      await ElMessageBox.confirm(`该支架风力区间与航线不匹配：${result.message}，是否继续绑定？`, '提示', {
        type: 'warning'
      })
    }

    await createBinding(selectedRoute.value, selectedBracket.value)
    ElMessage.success('绑定成功')
    selectedBracket.value = null
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
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('解绑失败')
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

onMounted(() => {
  loadRoutes()
  loadBrackets()
})
</script>

<template>
  <div class="page-container">
    <div class="selection-bar">
      <el-form :model="{ route: selectedRoute }" inline>
        <el-form-item label="选择航线" required>
          <el-select v-model="selectedRoute" placeholder="请选择航线" @change="handleRouteChange" style="width: 250px">
            <el-option v-for="route in routes" :key="route.id" 
              :label="`${route.routeCode} - ${route.routeName}`" :value="route.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择支架">
          <el-select v-model="selectedBracket" placeholder="请选择支架" style="width: 250px">
            <el-option v-for="bracket in brackets" :key="bracket.id" 
              :label="`${bracket.bracketCode} - ${bracket.bracketName}`" :value="bracket.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleBind" :disabled="!selectedRoute || !selectedBracket">绑定</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="currentRoute" class="route-info">
      <el-card>
        <div class="info-row">
          <span class="label">航线名称：</span>
          <span>{{ currentRoute.routeName }}</span>
          <span class="label" style="margin-left: 30px">风力范围：</span>
          <span>{{ currentRoute.minWindSpeed }} - {{ currentRoute.maxWindSpeed }} m/s</span>
        </div>
      </el-card>
    </div>

    <div class="table-container">
      <el-table :data="bindings" :loading="loading" border style="width: 100%">
        <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label">
          <template #default="{ row }">
            <template v-if="col.prop === 'matchLevel'">
              <span :style="{ color: getMatchLevelColor(row.matchLevel) }">
                {{ getMatchLevelText(row.matchLevel) }}
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
      <div v-if="bindings.length === 0 && selectedRoute" class="empty-tip">
        暂无绑定的支架，可选择上方支架进行绑定
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  padding: 20px;
}

.selection-bar {
  margin-bottom: 20px;
  padding: 15px;
  background: #fafafa;
  border-radius: 8px;
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
