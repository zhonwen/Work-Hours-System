<template>
  <div class="workhour-stats page-root">
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="6" animated />
    </div>

    <div v-else class="workhour-stats-body">
      <!-- 筛选：对齐全局 search-form -->
      <el-card class="search-card">
        <el-form :inline="true" class="search-form">
          <el-form-item>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 300px"
              @change="handleFilterChange"
            />
          </el-form-item>
          <el-form-item>
            <el-select
              v-model="selectedMemberId"
              placeholder="全部成员"
              clearable
              style="width: 180px"
              @change="handleFilterChange"
            >
              <el-option label="全部成员" :value="undefined" />
              <el-option
                v-for="member in members"
                :key="member.userId"
                :label="member.nickname"
                :value="member.userId"
              >
                <div class="member-option">
                  <el-avatar :size="20">{{ member.nickname?.charAt(0) }}</el-avatar>
                  <span>{{ member.nickname }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 概览卡片：与首页 / 我的工时统一 -->
      <el-row :gutter="16" class="stats-row">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stats-card" shadow="never">
            <div class="stats-content">
              <div class="stats-icon today">
                <el-icon><Clock /></el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-label">总工时</div>
                <div class="stats-value">{{ summary.totalHours }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stats-card" shadow="never">
            <div class="stats-content">
              <div class="stats-icon week">
                <el-icon><TrendCharts /></el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-label">人均工时</div>
                <div class="stats-value">{{ summary.avgHours }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stats-card" shadow="never">
            <div class="stats-content">
              <div class="stats-icon active">
                <el-icon><User /></el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-label">活跃成员</div>
                <div class="stats-value">{{ summary.activeMembers }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stats-card" shadow="never">
            <div class="stats-content">
              <div class="stats-icon days">
                <el-icon><Calendar /></el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-label">工作日数</div>
                <div class="stats-value">{{ summary.workDays }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 排行 + 趋势：双列铺满 -->
      <el-row :gutter="16" class="charts-row">
        <el-col :xs="24" :lg="14">
          <div class="section-card">
            <h3 class="section-title">成员贡献度排行</h3>
            <div class="ranking-list">
              <div
                v-for="(item, index) in memberRanking"
                :key="item.userId"
                class="ranking-item"
              >
                <div class="ranking-number" :class="`rank-${index + 1}`">{{ index + 1 }}</div>
                <el-avatar :size="36">{{ item.nickname?.charAt(0) }}</el-avatar>
                <div class="member-info">
                  <div class="member-name">{{ item.nickname }}</div>
                  <div class="member-stats">
                    <span>{{ item.taskCount }} 个任务</span>
                    <span>{{ item.workDays }} 天工作</span>
                  </div>
                </div>
                <div class="member-hours">
                  <div class="hours-value hours-accent">{{ item.totalHours }}</div>
                  <div class="hours-unit">小时</div>
                </div>
                <div class="hours-bar">
                  <div
                    class="hours-bar-fill"
                    :style="{ width: (item.totalHours / maxHours * 100) + '%' }"
                  ></div>
                </div>
              </div>
              <el-empty v-if="memberRanking.length === 0" description="暂无工时数据" :image-size="80" />
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :lg="10">
          <div class="section-card">
            <h3 class="section-title">工时趋势（最近30天）</h3>
            <div class="trend-chart">
              <div
                v-for="item in dailyTrend"
                :key="item.date"
                class="trend-bar-wrapper"
              >
                <div class="trend-bar-container">
                  <div
                    class="trend-bar"
                    :style="{ height: (item.hours / maxDailyHours * 100) + '%' }"
                    :title="`${item.date}: ${item.hours}h`"
                  ></div>
                </div>
                <div class="trend-label">{{ formatTrendDate(item.date) }}</div>
                <div class="trend-value hours-accent">{{ item.hours }}h</div>
              </div>
              <el-empty v-if="dailyTrend.length === 0" description="暂无趋势数据" :image-size="80" />
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 任务工时分布 -->
      <div class="section-card">
        <h3 class="section-title">任务工时分布</h3>
        <div class="distribution-chart">
          <div
            v-for="(item, index) in taskDistribution"
            :key="item.taskId"
            class="distribution-item"
          >
            <div class="distribution-info">
              <span class="task-name">{{ item.taskName }}</span>
              <span class="task-hours hours-accent">{{ item.totalHours }}h</span>
            </div>
            <div class="distribution-bar">
              <div
                class="distribution-bar-fill"
                :style="{
                  width: (item.totalHours / maxTaskHours * 100) + '%',
                  backgroundColor: getChartColor(index)
                }"
              ></div>
            </div>
          </div>
          <el-empty v-if="taskDistribution.length === 0" description="暂无任务分布数据" :image-size="80" />
        </div>
      </div>

      <!-- 工时明细 -->
      <div class="section-card hour-details">
        <h3 class="section-title">工时明细</h3>
        <el-table :data="workHourList" stripe border style="width: 100%">
          <el-table-column prop="workDate" label="日期" width="120" />
          <el-table-column label="成员" width="140">
            <template #default="{ row }">
              <div class="user-cell">
                <el-avatar :size="24">{{ (row.userName || 'U')?.charAt(0) }}</el-avatar>
                <span>{{ row.userName || '未知用户' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="taskName" label="关联任务" min-width="200">
            <template #default="{ row }">
              <span class="task-link">{{ row.taskName || '无关联任务' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="hours" label="工时数" width="100" align="center">
            <template #default="{ row }">
              <span class="hours-accent">{{ row.hours }}h</span>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="工作内容" min-width="220" show-overflow-tooltip />
        </el-table>
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchWorkHours"
            @current-change="fetchWorkHours"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Clock,
  TrendCharts,
  User,
  Calendar
} from '@element-plus/icons-vue'
import { getProjectMembers, type ProjectMember } from '@/api/project'
import { getWorkHourList, type WorkHourInfo } from '@/api/workhour'

const props = defineProps<{
  projectId: number
}>()

const loading = ref(false)
const members = ref<ProjectMember[]>([])
const workHourList = ref<WorkHourInfo[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

const dateRange = ref<[string, string]>([])
const selectedMemberId = ref<number | undefined>(undefined)

const summary = reactive({
  totalHours: 0,
  avgHours: 0,
  activeMembers: 0,
  workDays: 0
})

const memberRanking = ref<any[]>([])
const dailyTrend = ref<any[]>([])
const taskDistribution = ref<any[]>([])

const chartColors = [
  '#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1',
  '#eb2f96', '#13c2c2', '#2f54eb', '#fa8c16', '#a0d911'
]

const maxHours = computed(() => {
  return Math.max(...memberRanking.value.map(m => m.totalHours), 1)
})

const maxDailyHours = computed(() => {
  return Math.max(...dailyTrend.value.map(d => d.hours), 1)
})

const maxTaskHours = computed(() => {
  return Math.max(...taskDistribution.value.map(t => t.totalHours), 1)
})

const fetchMembers = async () => {
  try {
    const res = await getProjectMembers(props.projectId)
    members.value = res || []
  } catch (error) {
    console.error('获取成员列表失败:', error)
  }
}

const fetchWorkHours = async () => {
  loading.value = true
  try {
    const res = await getWorkHourList({
      projectId: props.projectId,
      userId: selectedMemberId.value,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    workHourList.value = res?.list || []
    total.value = res?.total || 0
    calculateStats()
  } catch (error) {
    console.error('获取工时数据失败:', error)
    ElMessage.error('获取工时数据失败')
  } finally {
    loading.value = false
  }
}

const calculateStats = () => {
  const list = workHourList.value
  const workList = list.filter(item => item.recordType !== 'LEAVE' && item.recordType !== 2)
  summary.totalHours = workList.reduce((sum, item) => sum + Number(item.hours || 0), 0)

  const uniqueUsers = new Set(workList.map(item => item.userId))
  summary.activeMembers = uniqueUsers.size

  const uniqueDates = new Set(workList.map(item => item.workDate))
  summary.workDays = uniqueDates.size

  summary.avgHours = summary.activeMembers > 0
    ? Math.round(summary.totalHours / summary.activeMembers)
    : 0

  const memberMap = new Map<number, any>()
  workList.forEach(item => {
    if (!memberMap.has(item.userId)) {
      memberMap.set(item.userId, {
        userId: item.userId,
        nickname: item.userName,
        totalHours: 0,
        taskCount: 0,
        workDays: new Set<string>()
      })
    }
    const member = memberMap.get(item.userId)!
    member.totalHours += Number(item.hours || 0)
    member.workDays.add(item.workDate)
  })

  const taskSet = new Map<number, Set<number>>()
  workList.forEach(item => {
    if (!item.taskId) return
    if (!taskSet.has(item.userId)) {
      taskSet.set(item.userId, new Set())
    }
    taskSet.get(item.userId)!.add(item.taskId)
  })

  memberRanking.value = Array.from(memberMap.values())
    .map(member => ({
      ...member,
      taskCount: taskSet.get(member.userId)?.size || 0,
      workDays: member.workDays.size
    }))
    .sort((a, b) => b.totalHours - a.totalHours)

  const dateMap = new Map<string, number>()
  workList.forEach(item => {
    const hours = dateMap.get(item.workDate) || 0
    dateMap.set(item.workDate, hours + Number(item.hours || 0))
  })

  dailyTrend.value = Array.from(dateMap.entries())
    .map(([date, hours]) => ({ date, hours }))
    .sort((a, b) => a.date.localeCompare(b.date))
    .slice(-30)

  const taskMap = new Map<number, any>()
  workList.forEach(item => {
    if (!item.taskId) return
    if (!taskMap.has(item.taskId)) {
      taskMap.set(item.taskId, {
        taskId: item.taskId,
        taskName: item.taskName || `任务 #${item.taskId}`,
        totalHours: 0
      })
    }
    taskMap.get(item.taskId)!.totalHours += Number(item.hours || 0)
  })

  taskDistribution.value = Array.from(taskMap.values())
    .sort((a, b) => b.totalHours - a.totalHours)
    .slice(0, 10)
}

const handleFilterChange = () => {
  pageNum.value = 1
  fetchWorkHours()
}

const formatTrendDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const getChartColor = (index: number) => {
  return chartColors[index % chartColors.length]
}

onMounted(async () => {
  await fetchMembers()
  await fetchWorkHours()
})
</script>

<style scoped>
.workhour-stats {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  padding: 16px 20px 24px;
  box-sizing: border-box;
  background: #f5f7fa;
}

.workhour-stats-body {
  width: 100%;
  max-width: 100%;
}

.loading-container {
  padding: 24px;
  background: #fff;
  border-radius: 4px;
}

.member-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.charts-row {
  margin-bottom: 0;
}

.charts-row .el-col {
  margin-bottom: 16px;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #f7f8fa;
  border-radius: 4px;
  position: relative;
  overflow: hidden;
}

.ranking-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  background: #d9d9d9;
  color: #666;
  flex-shrink: 0;
}

.ranking-number.rank-1 {
  background: #ffd700;
  color: #fff;
}

.ranking-number.rank-2 {
  background: #c0c0c0;
  color: #fff;
}

.ranking-number.rank-3 {
  background: #cd7f32;
  color: #fff;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.member-stats {
  font-size: 12px;
  color: #999;
  display: flex;
  gap: 12px;
}

.member-hours {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  margin-right: 4px;
}

.hours-value {
  font-size: 22px;
  line-height: 1.2;
}

.hours-unit {
  font-size: 12px;
  color: #999;
}

.hours-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: #f0f0f0;
}

.hours-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #1890ff 0%, #52c41a 100%);
  transition: width 0.5s;
}

.trend-chart {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  min-height: 200px;
  padding-top: 12px;
  overflow-x: auto;
}

.trend-bar-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 40px;
  height: 200px;
}

.trend-bar-container {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
}

.trend-bar {
  width: 100%;
  background: linear-gradient(180deg, #1890ff 0%, #69c0ff 100%);
  border-radius: 4px 4px 0 0;
  transition: height 0.3s;
  min-height: 4px;
}

.trend-label {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
}

.trend-value {
  margin-top: 2px;
  font-size: 12px;
}

.distribution-chart {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.distribution-item {
  padding: 12px;
  background: #f7f8fa;
  border-radius: 4px;
}

.distribution-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  gap: 12px;
}

.task-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-hours {
  font-size: 14px;
  flex-shrink: 0;
}

.distribution-bar {
  width: 100%;
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.distribution-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-link {
  color: #1890ff;
}

.hour-details {
  margin-bottom: 0;
}
</style>
