<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Bracket } from '@/types'
import {
  getBrackets,
  createBracket,
  updateBracket,
  deleteBracket,
  getBracketTypes,
  searchBrackets
} from '@/api/bracket'

const brackets = ref<Bracket[]>([])
const types = ref<string[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const searchForm = ref({
  minWindSpeed: '',
  maxWindSpeed: ''
})

const form = ref({
  id: 0,
  bracketCode: '',
  bracketName: '',
  maxLoad: 0,
  minWindSpeed: 0,
  maxWindSpeed: 0,
  bracketType: '',
  status: 'ACTIVE',
  description: ''
})

const columns = [
  { prop: 'bracketCode', label: '支架编号' },
  { prop: 'bracketName', label: '支架名称' },
  { prop: 'maxLoad', label: '最大承重(kg)' },
  { prop: 'minWindSpeed', label: '最小风力(m/s)' },
  { prop: 'maxWindSpeed', label: '最大风力(m/s)' },
  { prop: 'bracketType', label: '支架类型' },
  { prop: 'status', label: '状态', formatter: (row: Bracket) => row.status === 'ACTIVE' ? '启用' : '停用' },
  { prop: 'description', label: '描述' },
  { prop: 'action', label: '操作', width: 150 }
]

const loadBrackets = async () => {
  loading.value = true
  try {
    brackets.value = await getBrackets()
  } catch (error) {
    ElMessage.error('加载支架失败')
  } finally {
    loading.value = false
  }
}

const loadTypes = async () => {
  try {
    types.value = await getBracketTypes()
  } catch (error) {
    console.error('加载类型失败', error)
  }
}

const handleSearch = async () => {
  loading.value = true
  try {
    if (searchForm.value.minWindSpeed && searchForm.value.maxWindSpeed) {
      brackets.value = await searchBrackets(
        Number(searchForm.value.minWindSpeed),
        Number(searchForm.value.maxWindSpeed)
      )
    } else {
      await loadBrackets()
    }
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  form.value = {
    id: 0,
    bracketCode: '',
    bracketName: '',
    maxLoad: 0,
    minWindSpeed: 0,
    maxWindSpeed: 0,
    bracketType: '',
    status: 'ACTIVE',
    description: ''
  }
  dialogVisible.value = true
}

const handleEdit = (row: Bracket) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (row: Bracket) => {
  try {
    await ElMessageBox.confirm(`确定删除支架 ${row.bracketName} 吗？`, '提示', {
      type: 'warning'
    })
    await deleteBracket(row.id)
    ElMessage.success('删除成功')
    await loadBrackets()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  try {
    if (isEdit.value) {
      await updateBracket(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createBracket(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadBrackets()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

const getStatusColor = (status: string) => {
  return status === 'ACTIVE' ? '#67c23a' : '#f56c6c'
}

onMounted(() => {
  loadBrackets()
  loadTypes()
})
</script>

<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="searchForm" inline>
        <el-form-item label="最小风力">
          <el-input v-model="searchForm.minWindSpeed" type="number" placeholder="最小风力" style="width: 120px" />
        </el-form-item>
        <el-form-item label="最大风力">
          <el-input v-model="searchForm.maxWindSpeed" type="number" placeholder="最大风力" style="width: 120px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="searchForm = { minWindSpeed: '', maxWindSpeed: '' }; loadBrackets()">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" @click="handleAdd" style="margin-left: auto">新增支架</el-button>
    </div>

    <el-table :data="brackets" :loading="loading" border style="width: 100%">
      <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label">
        <template #default="{ row }">
          <template v-if="col.prop === 'status'">
            <span :style="{ color: getStatusColor(row.status) }">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</span>
          </template>
          <template v-else-if="col.prop === 'action'">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
          <template v-else>
            {{ row[col.prop] }}
          </template>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="isEdit ? '编辑支架' : '新增支架'" :visible="dialogVisible" @close="dialogVisible = false">
      <el-form :model="form" label-width="120px">
        <el-form-item label="支架编号" required>
          <el-input v-model="form.bracketCode" />
        </el-form-item>
        <el-form-item label="支架名称" required>
          <el-input v-model="form.bracketName" />
        </el-form-item>
        <el-form-item label="最大承重(kg)" required>
          <el-input v-model.number="form.maxLoad" type="number" />
        </el-form-item>
        <el-form-item label="最小风力(m/s)" required>
          <el-input v-model.number="form.minWindSpeed" type="number" />
        </el-form-item>
        <el-form-item label="最大风力(m/s)" required>
          <el-input v-model.number="form.maxWindSpeed" type="number" />
        </el-form-item>
        <el-form-item label="支架类型" required>
          <el-select v-model="form.bracketType">
            <el-option v-for="type in types" :key="type" :label="type" :value="type" />
            <el-option label="地面固定支架" value="FIXED" />
            <el-option label="配重支架" value="COUNTERWEIGHT" />
            <el-option label="大风专用支架" value="HIGH_WIND" />
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
