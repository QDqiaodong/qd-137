<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Route } from '@/types'
import {
  getRoutes,
  createRoute,
  updateRoute,
  deleteRoute,
  getRouteGroups,
  getRoutesByGroup,
  rematchBracketsForRoute
} from '@/api/route'

const routes = ref<Route[]>([])
const groups = ref<string[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const selectedGroup = ref('')

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
  { prop: 'duration', label: '时长(分钟)' },
  { prop: 'difficultyLevel', label: '难度等级' },
  { prop: 'status', label: '状态', formatter: (row: Route) => row.status === 'ACTIVE' ? '启用' : '停用' },
  { prop: 'action', label: '操作', width: 200 }
]

const filteredRoutes = computed(() => {
  if (!selectedGroup.value) return routes.value
  return routes.value.filter(r => r.groupName === selectedGroup.value)
})

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
      </el-form>
      <el-button type="primary" @click="handleAdd" style="margin-left: auto">新增航线</el-button>
    </div>

    <el-table :data="filteredRoutes" :loading="loading" border style="width: 100%">
      <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label">
        <template #default="{ row }">
          <template v-if="col.prop === 'status'">
            <span :style="{ color: getStatusColor(row.status) }">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</span>
          </template>
          <template v-else-if="col.prop === 'action'">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="handleRematch(row)">重新匹配</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
          <template v-else>
            {{ row[col.prop] }}
          </template>
        </template>
      </el-table-column>
    </el-table>

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
        <el-form-item label="时长(分钟)">
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
</style>
