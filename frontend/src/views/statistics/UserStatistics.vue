<template>
  <div class="page-root">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">用户工时统计</h2>
    </div>

    <!-- 筛选条件 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户">
          <el-select
            v-model="searchForm.userId"
            placeholder="请选择用户"
            filterable
            style="width: 200px"
            @change="handleSearch"
          >
            <el-option
              v-for="user in users"
              :key="user.id"
              :label="getUserDisplayName(user)"
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
            @change="handleDateChange"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row" v-if="statisticsData">
      <el-col :span="8">
        <el-card class="stats-card" shadow="never">
          <div class="stats-content">
            <div class="stats-icon today">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-label">总工时</div>
              <div class="stats-value">{{ formatHours(statisticsData.totalHours) }} 小时</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stats-card" shadow="never">
          <div class="stats-content">
            <div class="stats-icon month">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-label">工时记录数</div>
              <div class="stats-value">{{ statisticsData.totalRecords || 0 }} 条</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stats-card" shadow="never">
          <div class="stats-content">
            <div class="stats-icon project">
              <el-icon><Folder /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-label">参与项目</div>
              <div class="stats-value">{{ statisticsData.projectCount || 0 }} 个</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 项目工时分布：样式对齐项目工时统计（border + 固定列） -->
    <el-card class="table-card" v-if="statisticsData">
      <template #header>
        <div class="card-header">
          <div class="card-title-wrap">
            <span class="card-title">项目工时分布</span>
            <el-tooltip
              placement="bottom-start"
              :show-after="200"
              effect="dark"
              popper-class="stats-rule-tooltip"
            >
              <template #content>
                <div class="rule-tooltip">
                  <div class="rule-title">计算规则说明</div>
                  <div class="rule-item">
                    <span class="rule-label">工时数：</span>
                    该用户在筛选条件下，于该项目实际填报工时合计（不含请假），单位小时。
                  </div>
                  <div class="rule-item">
                    <span class="rule-label">进度条 / 占比：</span>
                    该项目工时 ÷ 用户总工时 × 100%，表示各项目对该用户总投入的贡献比例。
                  </div>
                  <div class="rule-item">
                    <span class="rule-label">排名：</span>
                    按工时数降序排列。
                  </div>
                </div>
              </template>
              <el-icon class="title-help-icon" tabindex="0">
                <QuestionFilled />
              </el-icon>
            </el-tooltip>
          </div>
        </div>
      </template>

      <el-table
        v-if="projectDistribution.length > 0"
        :data="projectDistribution"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column type="index" label="排名" width="64" align="center" fixed="left" />
        <el-table-column
          prop="projectName"
          label="项目名称"
          min-width="160"
          fixed="left"
          show-overflow-tooltip
        />
        <el-table-column label="工时" min-width="200">
          <template #default="{ row }">
            <el-progress
              :percentage="calcPercent(row.hours, statisticsData.totalHours)"
              :format="formatPercent"
              :color="getProgressColor(row.hours, statisticsData.totalHours)"
            />
          </template>
        </el-table-column>
        <el-table-column label="工时数（小时）" width="140">
          <template #default="{ row }">
            {{ formatHours(row.hours) }}
          </template>
        </el-table-column>
        <el-table-column label="占比" width="100">
          <template #default="{ row }">
            {{ formatPercent(calcPercent(row.hours, statisticsData.totalHours)) }}
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无工时" :image-size="80" />
    </el-card>

    <!-- 暂无数据提示 -->
    <el-empty v-if="!loading && !statisticsData" description="请选择用户查看统计数据" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Clock, Document, Folder, QuestionFilled } from '@element-plus/icons-vue'
import { getUserWorkHourStatistics } from '@/api/statistics'
import { getUserList, type UserInfo } from '@/api/user'
import { getProjectList } from '@/api/project'
import { useAuthStore } from '@/stores/auth'

interface StatisticsData {
  totalHours: number
  totalRecords: number
  projectCount: number
  projectHoursDistribution: Record<number, number>
  startDate?: string
  endDate?: string
}

interface ProjectDistributionItem {
  projectId: number
  projectName: string
  hours: number
}

const authStore = useAuthStore()
const loading = ref(false)
const statisticsData = ref<StatisticsData | null>(null)
const projectDistribution = ref<ProjectDistributionItem[]>([])
const users = ref<UserInfo[]>([])
const projects = ref<Array<{ id: number; name: string }>>([])

const searchForm = reactive({
  userId: undefined as number | undefined,
  startDate: '',
  endDate: ''
})

const dateRange = ref<string[]>([])

const getUserDisplayName = (user: UserInfo | undefined | null, fallbackId?: number) => {
  if (!user) return fallbackId != null ? `用户${fallbackId}` : '未知用户'
  return user.nickname || user.username || `用户${user.id}`
}

const getProjectName = (projectId: number) => {
  const project = projects.value.find(p => Number(p.id) === Number(projectId))
  return project?.name || `项目${projectId}`
}

/** 默认选中当前登录用户（不在列表中则取首个） */
const resolveDefaultUserId = (): number | undefined => {
  if (!users.value.length) return undefined
  const selfId = authStore.getCurrentUserId()
  if (selfId && users.value.some(u => Number(u.id) === Number(selfId))) {
    return selfId
  }
  return users.value[0]?.id
}

const formatHours = (hours: number | string | undefined | null): string => {
  const n = Number(hours || 0)
  if (Number.isNaN(n)) return '0'
  return (Math.round(n * 10) / 10).toString()
}

const calcPercent = (value: number, total: number): number => {
  const t = Number(total || 0)
  const v = Number(value || 0)
  if (!t) return 0
  return Math.round((v / t) * 1000) / 10
}

const formatPercent = (percentage: number): string => `${percentage}%`

const getProgressColor = (value: number, total: number): string => {
  const percentage = calcPercent(value, total)
  if (percentage >= 50) return '#409eff'
  if (percentage >= 30) return '#67c23a'
  if (percentage >= 10) return '#e6a23c'
  return '#f56c6c'
}

const fetchUsers = async () => {
  try {
    const res = await getUserList({ pageNum: 1, pageSize: 1000 })
    users.value = res.list || []
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

const fetchProjects = async () => {
  try {
    const res = await getProjectList({ pageNum: 1, pageSize: 1000 })
    projects.value = res.list || []
  } catch (error) {
    console.error('获取项目列表失败:', error)
  }
}

const fetchStatistics = async () => {
  if (!searchForm.userId) {
    statisticsData.value = null
    projectDistribution.value = []
    return
  }

  loading.value = true
  try {
    const data = await getUserWorkHourStatistics(
      searchForm.userId,
      searchForm.startDate || undefined,
      searchForm.endDate || undefined
    )

    statisticsData.value = {
      ...data,
      totalHours: Number(data.totalHours || 0)
    }

    if (data.projectHoursDistribution && typeof data.projectHoursDistribution === 'object') {
      const distribution: ProjectDistributionItem[] = Object.entries(data.projectHoursDistribution).map(
        ([projectId, hours]) => ({
          projectId: Number(projectId),
          projectName: getProjectName(Number(projectId)),
          hours: Number(hours || 0)
        })
      )
      distribution.sort((a, b) => b.hours - a.hours)
      projectDistribution.value = distribution
    } else {
      projectDistribution.value = []
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  fetchStatistics()
}

const handleReset = () => {
  searchForm.startDate = ''
  searchForm.endDate = ''
  dateRange.value = []
  searchForm.userId = resolveDefaultUserId()
  if (searchForm.userId) {
    fetchStatistics()
  } else {
    statisticsData.value = null
    projectDistribution.value = []
  }
}

const handleDateChange = (value: string[] | null) => {
  if (value && value.length === 2) {
    searchForm.startDate = value[0]
    searchForm.endDate = value[1]
  } else {
    searchForm.startDate = ''
    searchForm.endDate = ''
  }
  if (searchForm.userId) {
    fetchStatistics()
  }
}

onMounted(async () => {
  await Promise.all([fetchUsers(), fetchProjects()])
  searchForm.userId = resolveDefaultUserId()
  if (searchForm.userId) {
    await fetchStatistics()
  }
})
</script>

<style scoped>
.card-header {
  display: flex;
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
</style>

<style>
.stats-rule-tooltip {
  max-width: 380px !important;
}

.stats-rule-tooltip .rule-tooltip {
  line-height: 1.65;
  font-size: 13px;
}

.stats-rule-tooltip .rule-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.stats-rule-tooltip .rule-item + .rule-item {
  margin-top: 6px;
}

.stats-rule-tooltip .rule-label {
  font-weight: 600;
}
</style>
