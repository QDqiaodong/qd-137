<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { Bracket } from '@/types'
import { getBrackets, getSuitableBrackets } from '@/api/bracket'

const allBrackets = ref<Bracket[]>([])
const loading = ref(false)
const searchForm = ref({
  minWindSpeed: '',
  maxWindSpeed: ''
})

const columns = [
  { prop: 'bracketCode', label: '支架编号' },
  { prop: 'bracketName', label: '支架名称' },
  { prop: 'maxLoad', label: '最大承重(kg)' },
  { prop: 'minWindSpeed', label: '支架最小风力' },
  { prop: 'maxWindSpeed', label: '支架最大风力' },
  { prop: 'bracketType', label: '支架类型' },
  { prop: 'matchStatus', label: '匹配状态', width: 120 }
]

const suitableBrackets = ref<Bracket[]>([])
const showResult = ref(false)

/** 支架区间是否完全盖住当日风速区间 */
const isPerfectMatch = (bracket: Bracket) => {
  const min = Number(searchForm.value.minWindSpeed)
  const max = Number(searchForm.value.maxWindSpeed)
  return bracket.minWindSpeed <= min && bracket.maxWindSpeed >= max
}

/** 是否存在能完全盖住当日风速区间的支架；一个都盖不住时页上必须明确告知 */
const hasPerfectMatch = computed(() => suitableBrackets.value.some(isPerfectMatch))

const windRangeDesc = computed(() => {
  if (!searchForm.value.minWindSpeed || !searchForm.value.maxWindSpeed) return ''
  return `${searchForm.value.minWindSpeed} - ${searchForm.value.maxWindSpeed} m/s`
})

const loadBrackets = async () => {
  try {
    allBrackets.value = await getBrackets()
  } catch (error) {
    ElMessage.error('加载支架失败')
  }
}

const handleSearch = async () => {
  if (!searchForm.value.minWindSpeed || !searchForm.value.maxWindSpeed) {
    ElMessage.warning('请输入风力区间范围')
    return
  }

  loading.value = true
  try {
    suitableBrackets.value = await getSuitableBrackets(
      Number(searchForm.value.minWindSpeed),
      Number(searchForm.value.maxWindSpeed)
    )
    showResult.value = true
    if (hasPerfectMatch.value) {
      ElMessage.success(`共找到 ${suitableBrackets.value.length} 个适配支架`)
    } else {
      ElMessage.warning('今天没有完全匹配的支架')
    }
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

const getBracketTypeText = (type: string) => {
  const map: Record<string, string> = {
    'FIXED': '地面固定支架',
    'COUNTERWEIGHT': '配重支架',
    'HIGH_WIND': '大风专用支架'
  }
  return map[type] || type
}

const getMatchStatus = (bracket: Bracket) => {
  const min = Number(searchForm.value.minWindSpeed)
  const max = Number(searchForm.value.maxWindSpeed)

  if (isPerfectMatch(bracket)) {
    return { text: '完全匹配', color: '#67c23a' }
  }
  if (bracket.maxWindSpeed >= min && bracket.minWindSpeed <= max) {
    return { text: '部分匹配', color: '#e6a23c' }
  }
  return { text: '不匹配', color: '#f56c6c' }
}

onMounted(() => {
  loadBrackets()
})
</script>

<template>
  <div class="page-container">
    <div class="search-section">
      <el-card title="风力区间筛选">
        <el-form :model="searchForm" label-width="120px">
          <el-form-item label="目标最小风力(m/s)" required>
            <el-input v-model.number="searchForm.minWindSpeed" type="number" placeholder="请输入最小风力" />
          </el-form-item>
          <el-form-item label="目标最大风力(m/s)" required>
            <el-input v-model.number="searchForm.maxWindSpeed" type="number" placeholder="请输入最大风力" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">筛选适配支架</el-button>
            <el-button @click="searchForm = { minWindSpeed: '', maxWindSpeed: '' }; showResult = false">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <div v-if="showResult" class="result-section">
      <el-card :title="`筛选结果 - 风力区间: ${windRangeDesc}`">
        <el-alert
          v-if="suitableBrackets.length > 0 && !hasPerfectMatch"
          type="warning"
          :closable="false"
          show-icon
          class="no-perfect-alert"
          title="今天没有完全匹配的支架"
          description="以下支架只能部分覆盖当日风速区间，没有能完全盖住的，请勿当作完全适配使用"
        />
        <el-table :data="suitableBrackets" :loading="loading" border style="width: 100%">
          <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label">
            <template #default="{ row }">
              <template v-if="col.prop === 'bracketType'">
                {{ getBracketTypeText(row.bracketType) }}
              </template>
              <template v-else-if="col.prop === 'matchStatus'">
                <span :style="{ color: getMatchStatus(row).color, fontWeight: 'bold' }">
                  {{ getMatchStatus(row).text }}
                </span>
              </template>
              <template v-else>
                {{ row[col.prop] }}
              </template>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="suitableBrackets.length === 0" class="empty-tip">
          <el-icon size="48" color="#909399">
            <Search />
          </el-icon>
          <p>今天没有完全匹配的支架</p>
          <p class="hint">建议检查风力范围或增加支架配置</p>
        </div>
      </el-card>
    </div>

    <div class="info-section">
      <el-card title="风力匹配规则说明">
        <div class="rule-list">
          <div class="rule-item">
            <span class="rule-badge perfect">完全匹配</span>
            <span>支架风力区间完全覆盖目标风力区间</span>
          </div>
          <div class="rule-item">
            <span class="rule-badge partial">部分匹配</span>
            <span>支架风力区间与目标风力区间有重叠</span>
          </div>
          <div class="rule-item">
            <span class="rule-badge none">不匹配</span>
            <span>支架风力区间无法覆盖目标风力区间</span>
          </div>
        </div>
        <div class="warning-box">
          <el-icon color="#e6a23c">
            <Warning />
          </el-icon>
          <span>低承重支架不允许绑定大风航线，请确保使用大风专用支架</span>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  padding: 20px;
}

.search-section {
  margin-bottom: 20px;
}

.result-section {
  margin-bottom: 20px;
}

.no-perfect-alert {
  margin-bottom: 16px;
}

.empty-tip {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.empty-tip p {
  margin: 10px 0;
}

.hint {
  font-size: 12px;
}

.info-section {
  margin-top: 20px;
}

.rule-list {
  margin-bottom: 20px;
}

.rule-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.rule-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  margin-right: 12px;
}

.rule-badge.perfect {
  background: #e8f5e9;
  color: #4caf50;
}

.rule-badge.partial {
  background: #fff8e1;
  color: #ff9800;
}

.rule-badge.none {
  background: #ffebee;
  color: #f44336;
}

.warning-box {
  display: flex;
  align-items: center;
  padding: 15px;
  background: #fefce8;
  border-left: 4px solid #eab308;
  border-radius: 0 4px 4px 0;
}

.warning-box span {
  margin-left: 10px;
  color: #854d0e;
}
</style>
