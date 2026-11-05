<template>
  <div class="page-root">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">项目工时统计</h2>
    </div>

    <!-- 筛选条件 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="项目">
          <el-select
            v-model="searchForm.projectId"
            placeholder="请选择项目"
            filterable
            style="width: 200px"
            @change="handleProjectChange"
          >
            <el-option
              v-for="project in projects"
              :key="project.id"
              :label="project.name"
              :value="project.id"
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
            <div class="stats-icon active">
              <el-icon><User /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-label">参与人数</div>
              <div class="stats-value">{{ statisticsData.userCount || 0 }} 人</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 用户工时分布 -->
    <el-card class="table-card" v-if="statisticsData && userDistribution.length > 0">
      <template #header>
        <div class="card-header">
          <span class="card-title">用户工时分布</span>
        </div>
      </template>

      <el-table :data="userDistribution" stripe border>
        <el-table-column type="index" label="排名" width="64" fixed="left" />
        <el-table-column prop="userName" label="用户名称" min-width="140" fixed="left" show-overflow-tooltip />
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
    </el-card>

    <!-- 暂无数据提示 -->
    <el-empty v-if="!loading && !statisticsData" description="请选择项目查看统计数据" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Clock, Document, User } from '@element-plus/icons-vue'
import { getProjectWorkHourStatistics } from '@/api/statistics'
import { getProjectList } from '@/api/project'
import { getAllUsers, type UserInfo } from '@/api/user'

const PROJECT_STORAGE_KEY = 'gsms.projectStatistics.projectId'

interface StatisticsData {
  totalHours: number
  totalRecords: number
  userCount: number
  userHoursDistribution: Record<number, number>
  startDate?: string
  endDate?: string
}

interface UserDistributionItem {
  userId: number
  userName: string
  hours: number
}

const loading = ref(false)
const statisticsData = ref<StatisticsData | null>(null)
const userDistribution = ref<UserDistributionItem[]>([])
const projects = ref<any[]>([])
const users = ref<UserInfo[]>([])

const searchForm = reactive({
  projectId: undefined as number | undefined,
  startDate: '',
  endDate: ''
})

const dateRange = ref<string[]>([])

const persistProjectId = (projectId?: number) => {
  if (projectId != null) {
    localStorage.setItem(PROJECT_STORAGE_KEY, String(projectId))
  } else {
    localStorage.removeItem(PROJECT_STORAGE_KEY)
  }
}

const resolveDefaultProjectId = (): number | undefined => {
  if (!projects.value.length) return undefined
  const saved = Number(localStorage.getItem(PROJECT_STORAGE_KEY))
  if (saved && projects.value.some(p => p.id === saved)) {
    return saved
  }
  return projects.value[0].id
}

const getUserDisplayName = (userId: number): string => {
  const user = users.value.find(u => u.id === userId)
  if (!user) return `用户${userId}`
  return user.nickname || user.username || `用户${userId}`
}

/** 工时保留最多 1 位小数 */
const formatHours = (hours: number | string | undefined | null): string => {
  const n = Number(hours || 0)
  if (Number.isNaN(n)) return '0'
  return (Math.round(n * 10) / 10).toString()
}

/** 占比保留 1 位小数（0-100） */
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
    const res = await getAllUsers()
    users.value = res.list || []
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

const fetchStatistics = async () => {
  if (!searchForm.projectId) {
    statisticsData.value = null
    userDistribution.value = []
    return
  }

  loading.value = true
  try {
    const data = await getProjectWorkHourStatistics(
      searchForm.projectId,
      searchForm.startDate || undefined,
      searchForm.endDate || undefined
    )

    statisticsData.value = {
      ...data,
      totalHours: Number(data.totalHours || 0)
    }

    if (data.userHoursDistribution && typeof data.userHoursDistribution === 'object') {
      const distribution: UserDistributionItem[] = Object.entries(data.userHoursDistribution).map(
        ([userId, hours]) => ({
          userId: Number(userId),
          userName: getUserDisplayName(Number(userId)),
          hours: Number(hours || 0)
        })
      )
      distribution.sort((a, b) => b.hours - a.hours)
      userDistribution.value = distribution
    } else {
      userDistribution.value = []
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败')
  } finally {
    loading.value = false
  }
}

const handleProjectChange = (projectId: number | undefined) => {
  persistProjectId(projectId)
  fetchStatistics()
}

const handleSearch = () => {
  persistProjectId(searchForm.projectId)
  fetchStatistics()
}

const handleReset = () => {
  searchForm.startDate = ''
  searchForm.endDate = ''
  dateRange.value = []
  // 重置后回到默认/首个项目，并保留到 storage
  searchForm.projectId = resolveDefaultProjectId()
  persistProjectId(searchForm.projectId)
  if (searchForm.projectId) {
    fetchStatistics()
  } else {
    statisticsData.value = null
    userDistribution.value = []
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
  if (searchForm.projectId) {
    fetchStatistics()
  }
}

onMounted(async () => {
  await Promise.all([fetchProjects(), fetchUsers()])
  searchForm.projectId = resolveDefaultProjectId()
  persistProjectId(searchForm.projectId)
  if (searchForm.projectId) {
    await fetchStatistics()
  }
})
</script>

<style scoped>
.card-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}
</style>
