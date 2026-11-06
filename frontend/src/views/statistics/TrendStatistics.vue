<template>
  <div class="page-root">
    <div class="page-header">
      <h2 class="page-title">工时趋势分析</h2>
    </div>

    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="项目">
          <el-select
            v-model="searchForm.projectId"
            placeholder="全部项目"
            clearable
            style="width: 200px"
          >
            <el-option
              v-for="project in projects"
              :key="project.id"
              :label="project.name"
              :value="project.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="用户">
          <el-select
            v-model="searchForm.userId"
            placeholder="全部用户"
            clearable
            filterable
            style="width: 200px"
          >
            <el-option
              v-for="user in users"
              :key="user.id"
              :label="user.username"
              :value="user.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 趋势总览：三卡紧凑布局，与项目统计页一致 -->
    <el-row v-if="statisticsData" :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="8">
        <el-card class="stats-card" shadow="never">
          <div class="stats-content">
            <div class="stats-icon today">
              <el-icon><Calendar /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-label">统计周期</div>
              <div class="stats-value stats-value--sm">
                {{ statisticsData.startDate }} ~ {{ statisticsData.endDate }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stats-card" shadow="never">
          <div class="stats-content">
            <div class="stats-icon month">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-label">总工时</div>
              <div class="stats-value">{{ formatHours(statisticsData.totalHours) }} 小时</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stats-card" shadow="never">
          <div class="stats-content">
            <div class="stats-icon active">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-label">平均每日工时</div>
              <div class="stats-value">{{ formatHours(averageHoursPerDay) }} 小时</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card" v-if="statisticsData && trendData.length > 0">
      <template #header>
        <div class="card-header">
          <div class="card-title-wrap">
            <span class="card-title">工时趋势图</span>
            <el-tooltip
              placement="bottom-start"
              :show-after="200"
              effect="dark"
              popper-class="trend-rule-tooltip"
            >
              <template #content>
                <div class="rule-tooltip">
                  <div class="rule-title">计算规则说明</div>
                  <div class="rule-item">
                    <span class="rule-label">工时数：</span>
                    当日实际填报工时合计（不含请假），单位小时。
                  </div>
                  <div class="rule-item">
                    <span class="rule-label">进度条占比：</span>
                    当日工时 ÷ 本周期内单日最高工时 × 100%，用于对比每日相对强度（最高日为 100%）。
                  </div>
                  <div class="rule-item">
                    <span class="rule-label">占比：</span>
                    当日工时 ÷ 本周期总工时 × 100%，表示该日在整段周期中的贡献比例。
                  </div>
                  <div class="rule-item">
                    <span class="rule-label">平均每日工时：</span>
                    周期总工时 ÷ 有数据的天数。
                  </div>
                </div>
              </template>
              <el-icon class="title-help-icon" tabindex="0">
                <QuestionFilled />
              </el-icon>
            </el-tooltip>
          </div>
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button label="table">表格视图</el-radio-button>
            <el-radio-button label="chart">图表视图</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table v-if="viewMode === 'table'" :data="trendData" stripe border>
        <el-table-column type="index" label="序号" width="64" />
        <el-table-column prop="date" label="日期" min-width="140" />
        <el-table-column label="工时" min-width="220">
          <template #default="{ row }">
            <el-progress
              :percentage="calcBarPercent(row.hours, maxHours)"
              :format="formatPercent"
              :color="getProgressColor(row.hours, maxHours)"
            />
          </template>
        </el-table-column>
        <el-table-column label="工时数（小时）" width="140" align="right">
          <template #default="{ row }">
            {{ formatHours(row.hours) }}
          </template>
        </el-table-column>
        <el-table-column label="占比" width="100" align="right">
          <template #default="{ row }">
            {{ formatPercent(calcPercent(row.hours, statisticsData.totalHours)) }}
          </template>
        </el-table-column>
      </el-table>

      <div v-else class="chart-container">
        <div class="chart-bars">
          <div
            v-for="(item, index) in trendData"
            :key="index"
            class="chart-bar-item"
          >
            <div class="bar-wrapper">
              <div
                class="bar"
                :style="{
                  height: calcBarPercent(item.hours, maxHours) + '%',
                  backgroundColor: getBarColor(item.hours)
                }"
              >
                <span class="bar-label">{{ formatHours(item.hours) }}h</span>
              </div>
            </div>
            <div class="bar-date">{{ formatShortDate(item.date) }}</div>
          </div>
        </div>
      </div>
    </el-card>

    <el-empty v-if="!loading && !statisticsData" description="请选择日期范围查看趋势数据" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, Clock, TrendCharts, QuestionFilled } from '@element-plus/icons-vue'
import { getWorkHourTrendStatistics } from '@/api/statistics'
import { getProjectList } from '@/api/project'
import { getUserList } from '@/api/user'

interface TrendDataItem {
  date: string
  hours: number
}

interface StatisticsData {
  totalHours: number
  startDate: string
  endDate: string
  trendData: TrendDataItem[]
}

const loading = ref(false)
const statisticsData = ref<StatisticsData | null>(null)
const trendData = ref<TrendDataItem[]>([])
const projects = ref<any[]>([])
const users = ref<any[]>([])
const viewMode = ref<'table' | 'chart'>('table')

const searchForm = reactive({
  projectId: undefined as number | undefined,
  userId: undefined as number | undefined,
  startDate: '',
  endDate: ''
})

const dateRange = ref<string[]>([])

const averageHoursPerDay = computed(() => {
  if (!statisticsData.value || !trendData.value.length) return 0
  return statisticsData.value.totalHours / trendData.value.length
})

const maxHours = computed(() => {
  if (!trendData.value.length) return 0
  return Math.max(...trendData.value.map(item => item.hours))
})

/** 工时保留最多 1 位小数 */
const formatHours = (hours: number | string | undefined | null): string => {
  const n = Number(hours || 0)
  if (Number.isNaN(n)) return '0'
  return (Math.round(n * 10) / 10).toString()
}

/** 占总工时占比，保留 1 位小数（0-100） */
const calcPercent = (value: number, total: number): number => {
  const t = Number(total || 0)
  const v = Number(value || 0)
  if (!t) return 0
  return Math.round((v / t) * 1000) / 10
}

/** 进度条相对最大值占比，整数 0-100（避免 88.888...%） */
const calcBarPercent = (value: number, max: number): number => {
  const m = Number(max || 0)
  const v = Number(value || 0)
  if (!m) return 0
  return Math.min(100, Math.round((v / m) * 100))
}

const formatPercent = (percentage: number): string => `${percentage}%`

const getProgressColor = (value: number, max: number): string => {
  const percentage = calcBarPercent(value, max)
  if (percentage >= 80) return '#409eff'
  if (percentage >= 50) return '#67c23a'
  if (percentage >= 25) return '#e6a23c'
  return '#f56c6c'
}

const getBarColor = (hours: number): string => {
  if (hours >= 8) return '#67c23a'
  if (hours >= 4) return '#409eff'
  if (hours >= 2) return '#e6a23c'
  return '#f56c6c'
}

const formatShortDate = (dateStr: string): string => {
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return dateStr
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const fetchProjects = async () => {
  try {
    const res = await getProjectList({ pageNum: 1, pageSize: 1000 })
    projects.value = res.list || []
  } catch (error) {
    console.error('获取项目列表失败:', error)
  }
}

const fetchUsers = async () => {
  try {
    const res = await getUserList({ pageNum: 1, pageSize: 1000 })
    users.value = res.list || []
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

const fetchStatistics = async () => {
  if (!searchForm.startDate || !searchForm.endDate) {
    ElMessage.warning('请选择日期范围')
    return
  }

  loading.value = true
  try {
    const data = await getWorkHourTrendStatistics({
      projectId: searchForm.projectId,
      userId: searchForm.userId,
      startDate: searchForm.startDate,
      endDate: searchForm.endDate
    })

    statisticsData.value = data
    trendData.value = data.trendData || []
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择日期范围')
    return
  }

  searchForm.startDate = dateRange.value[0]
  searchForm.endDate = dateRange.value[1]
  fetchStatistics()
}

const handleReset = () => {
  searchForm.projectId = undefined
  searchForm.userId = undefined
  searchForm.startDate = ''
  searchForm.endDate = ''
  dateRange.value = []
  statisticsData.value = null
  trendData.value = []
}

onMounted(() => {
  fetchProjects()
  fetchUsers()

  const endDate = new Date()
  const startDate = new Date()
  startDate.setDate(endDate.getDate() - 6)

  const toDateStr = (date: Date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  dateRange.value = [toDateStr(startDate), toDateStr(endDate)]
  searchForm.startDate = dateRange.value[0]
  searchForm.endDate = dateRange.value[1]
  fetchStatistics()
})
</script>

<style scoped>
.stats-value--sm {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.35;
  word-break: break-all;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title-wrap {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.card-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.title-help-icon {
  font-size: 16px;
  color: #909399;
  cursor: help;
  outline: none;
  transition: color 0.2s;
}

.title-help-icon:hover,
.title-help-icon:focus {
  color: #409eff;
}

.chart-container {
  padding: 12px 0 4px;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
  height: 300px;
}

.chart-bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.bar-wrapper {
  width: 100%;
  height: 250px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 4px;
}

.bar {
  width: 100%;
  min-height: 2px;
  border-radius: 4px 4px 0 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 4px;
  transition: opacity 0.2s, transform 0.2s;
}

.bar:hover {
  opacity: 0.85;
  transform: scaleY(1.02);
  transform-origin: bottom;
}

.bar-label {
  font-size: 12px;
  color: #fff;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.bar-date {
  font-size: 12px;
  color: #666;
  text-align: center;
}
</style>

<!-- tooltip 挂载到 body，需非 scoped -->
<style>
.trend-rule-tooltip {
  max-width: 380px !important;
}

.trend-rule-tooltip .rule-tooltip {
  line-height: 1.65;
  font-size: 13px;
}

.trend-rule-tooltip .rule-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.trend-rule-tooltip .rule-item + .rule-item {
  margin-top: 6px;
}

.trend-rule-tooltip .rule-label {
  font-weight: 600;
}
</style>
