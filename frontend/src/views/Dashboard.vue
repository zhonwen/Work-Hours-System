<template>
  <div class="page-root">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">首页看板</h2>
        <div class="welcome-text">欢迎回来，{{ authStore.username }}</div>
      </div>
      <div class="header-right">
        <el-button :icon="Refresh" :loading="loading" @click="handleRefresh">
          刷新
        </el-button>
        <el-button type="primary" :icon="Plus" @click="handleBatchCreateWorkHour">
          工时填报
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon project">
              <el-icon><Folder /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-label">项目数</div>
              <div class="stats-value">{{ dashboardData.projectCount || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon task">
              <el-icon><List /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-label">待办任务</div>
              <div class="stats-value">{{ dashboardData.pendingTaskCount || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon today">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-label">今日工时</div>
              <div class="stats-value">{{ dashboardData.todayHours || 0 }} 小时</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon week">
              <el-icon><Calendar /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-label">本周工时</div>
              <div class="stats-value">{{ dashboardData.weekHours || 0 }} 小时</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 工时统计 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="12">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon month">
              <el-icon><DataLine /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-label">本月工时</div>
              <div class="stats-value">{{ dashboardData.monthHours || 0 }} 小时</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon total">
              <el-icon><Plus /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-label">总计工时</div>
              <div class="stats-value">{{ dashboardData.totalHours || 0 }} 小时</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Tab内容区域 -->
    <el-card class="content-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="我的待办任务" name="tasks">
          <template #label>
            <span class="tab-label">
              我的待办任务
              <el-badge v-if="dashboardData.pendingTasks?.length" :value="dashboardData.pendingTasks.length" class="tab-badge" />
            </span>
          </template>
          <div v-if="dashboardData.pendingTasks?.length" class="pending-task-table-wrap">
            <div class="pending-toolbar">
              <el-select
                v-model="pendingProjectFilter"
                clearable
                placeholder="全部项目"
                style="width: 220px"
              >
                <el-option
                  v-for="item in pendingProjectOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </div>
            <el-table
              v-if="treePendingTasks.length"
              class="pending-task-table"
              :data="treePendingTasks"
              stripe
              row-key="id"
              :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
            >
              <!-- 展开箭头独占首列，避免与序号重叠错位 -->
              <el-table-column width="44" class-name="expand-col" />
              <el-table-column label="序号" width="64" class-name="index-col">
                <template #default="{ row }">
                  <span class="task-index" :class="{ 'is-subtask': row.isSubtask }">{{ row.indexLabel }}</span>
                </template>
              </el-table-column>
              <el-table-column label="任务标题" min-width="220">
                <template #default="{ row }">
                  <div class="task-title-cell" :class="{ 'is-subtask': row.isSubtask }">
                    <span class="task-title-text" :title="row.title">{{ row.title }}</span>
                    <el-tag v-if="row.isSubtask" type="info" size="small" class="subtask-count-tag">子任务</el-tag>
                    <el-tag
                      v-else-if="row.subtaskCount"
                      type="info"
                      size="small"
                      class="subtask-count-tag"
                    >
                      {{ row.subtaskCount }} 子任务
                    </el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="任务内容" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ row.description || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="所属项目" width="140" show-overflow-tooltip>
                <template #default="{ row }">
                  <el-link type="primary" @click="goToProject(row.projectId)">
                    {{ getProjectName(row.projectId) }}
                  </el-link>
                </template>
              </el-table-column>
              <el-table-column label="优先级" width="90">
                <template #default="{ row }">
                  <el-tag :type="getPriorityType(row.priority)" size="small">
                    {{ getPriorityText(row.priority) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)" size="small">
                    {{ getStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="截止日期" width="110">
                <template #default="{ row }">
                  <span :class="getDueDateClass(row.planEndDate)">
                    {{ formatDate(row.planEndDate) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="72" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="goToTask(row.id)">查看</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="当前项目下暂无待办任务" :image-size="80" />
            <div class="tab-footer">
              <el-button link type="primary" @click="goToTasks('TODO')">查看全部待办任务 →</el-button>
            </div>
          </div>
          <el-empty v-else description="暂无待办任务" />
        </el-tab-pane>

        <el-tab-pane label="我的已完成" name="completed">
          <template #label>
            <span class="tab-label">
              我的已完成
              <el-badge
                v-if="dashboardData.completedTasks?.length"
                :value="dashboardData.completedTasks.length"
                class="tab-badge"
              />
            </span>
          </template>
          <div v-if="dashboardData.completedTasks?.length" class="pending-task-table-wrap">
            <div class="pending-toolbar">
              <el-select
                v-model="completedProjectFilter"
                clearable
                placeholder="全部项目"
                style="width: 220px"
              >
                <el-option
                  v-for="item in completedProjectOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </div>
            <el-table
              v-if="treeCompletedTasks.length"
              class="pending-task-table"
              :data="treeCompletedTasks"
              stripe
              row-key="id"
              :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
            >
              <el-table-column width="44" class-name="expand-col" />
              <el-table-column label="序号" width="64" class-name="index-col">
                <template #default="{ row }">
                  <span class="task-index" :class="{ 'is-subtask': row.isSubtask }">{{ row.indexLabel }}</span>
                </template>
              </el-table-column>
              <el-table-column label="任务标题" min-width="220">
                <template #default="{ row }">
                  <div class="task-title-cell" :class="{ 'is-subtask': row.isSubtask }">
                    <span class="task-title-text" :title="row.title">{{ row.title }}</span>
                    <el-tag v-if="row.isSubtask" type="info" size="small" class="subtask-count-tag">子任务</el-tag>
                    <el-tag
                      v-else-if="row.subtaskCount"
                      type="info"
                      size="small"
                      class="subtask-count-tag"
                    >
                      {{ row.subtaskCount }} 子任务
                    </el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="预估" width="88">
                <template #default="{ row }">
                  {{ formatEstimateHours(row.estimateHours) }}
                </template>
              </el-table-column>
              <el-table-column label="实际工时" width="110">
                <template #default="{ row }">
                  <span :class="{ 'quick-fix-label': isQuickFixTask(row) }">
                    {{ formatActualHours(row) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="任务内容" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ row.description || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="所属项目" width="140" show-overflow-tooltip>
                <template #default="{ row }">
                  <el-link type="primary" @click="goToProject(row.projectId)">
                    {{ getProjectName(row.projectId) }}
                  </el-link>
                </template>
              </el-table-column>
              <el-table-column label="优先级" width="90">
                <template #default="{ row }">
                  <el-tag :type="getPriorityType(row.priority)" size="small">
                    {{ getPriorityText(row.priority) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)" size="small">
                    {{ getStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="完成日期" width="110">
                <template #default="{ row }">
                  {{ formatCompletedDate(row.actualEndDate || row.planEndDate) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="72" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="goToTask(row.id)">查看</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="当前项目下暂无已完成任务" :image-size="80" />
            <div class="tab-footer">
              <el-button link type="primary" @click="goToTasks('DONE')">查看全部已完成任务 →</el-button>
            </div>
          </div>
          <el-empty v-else description="暂无已完成任务" />
        </el-tab-pane>

        <el-tab-pane label="我的项目" name="projects">
          <template #label>
            <span class="tab-label">
              我的项目
              <el-badge v-if="dashboardData.projects?.length" :value="dashboardData.projects.length" class="tab-badge" />
            </span>
          </template>
          <div v-if="dashboardData.projects?.length">
            <el-table :data="dashboardData.projects" stripe>
              <el-table-column label="序号" type="index" width="64" fixed="left" />
              <el-table-column prop="name" label="项目名称" min-width="200" fixed="left" show-overflow-tooltip />
              <el-table-column prop="code" label="项目编码" width="150" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getProjectStatusType(row.status)">
                    {{ getProjectStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="goToProject(row.id)">查看</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="tab-footer">
              <el-button link type="primary" @click="$router.push('/projects')">查看全部项目 →</el-button>
            </div>
          </div>
          <el-empty v-else description="暂无项目" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 工时填报对话框 -->
    <WorkHourBatchDialog
      v-model:visible="batchDialogVisible"
      :date="selectedDate"
      @success="fetchDashboardData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Folder, List, Clock, Calendar, DataLine, Plus, Refresh } from '@element-plus/icons-vue'
import { getTaskStatusInfo, getTaskPriorityInfo, getProjectStatusInfo } from '@/utils/statusMapping'
import { useAuthStore } from '@/stores/auth'
import { getDashboardData } from '@/api/statistics'
import WorkHourBatchDialog from '@/components/WorkHourBatchDialog.vue'

const router = useRouter()
const authStore = useAuthStore()

// 工时填报对话框
const batchDialogVisible = ref(false)
const selectedDate = ref<string>()
const activeTab = ref('tasks') // 默认显示待办任务tab

// 打开工时填报对话框
const handleBatchCreateWorkHour = () => {
  selectedDate.value = undefined // 不传日期，使用今天
  batchDialogVisible.value = true
}

// 打开工时填报对话框（指定日期）
const handleBatchCreateWorkHourWithDate = (date: string) => {
  selectedDate.value = date
  batchDialogVisible.value = true
}

type DashboardTaskNode = {
  id: number
  title: string
  description?: string
  status: string
  priority: string
  projectId: number
  planEndDate?: string
  actualEndDate?: string
  parentId?: number | null
  estimateHours?: number | null
  actualHours?: number
  subtasks?: DashboardTaskNode[]
}

interface DashboardData {
  projectCount: number
  pendingTaskCount: number
  completedTaskCount: number
  todayHours: number
  weekHours: number
  monthHours: number
  totalHours: number
  pendingTasks: DashboardTaskNode[]
  completedTasks: DashboardTaskNode[]
  projects: Array<{
    id: number
    name: string
    code: string
    status: string
  }>
}

const dashboardData = reactive<DashboardData>({
  projectCount: 0,
  pendingTaskCount: 0,
  completedTaskCount: 0,
  todayHours: 0,
  weekHours: 0,
  monthHours: 0,
  totalHours: 0,
  pendingTasks: [],
  completedTasks: [],
  projects: []
})

// 项目筛选（空=全部）
const pendingProjectFilter = ref<number | undefined>(undefined)
const completedProjectFilter = ref<number | undefined>(undefined)

const buildProjectOptions = (tasks: DashboardTaskNode[]) => {
  const map = new Map<number, string>()
  for (const task of tasks) {
    if (!map.has(task.projectId)) {
      map.set(task.projectId, getProjectName(task.projectId))
    }
    for (const child of task.subtasks || []) {
      if (!map.has(child.projectId)) {
        map.set(child.projectId, getProjectName(child.projectId))
      }
    }
  }
  return Array.from(map.entries()).map(([id, name]) => ({ id, name }))
}

const buildTreeTasks = (tasks: DashboardTaskNode[], projectFilter?: number) => {
  const filtered = projectFilter
    ? tasks.filter(task => task.projectId === projectFilter)
    : tasks

  return filtered.map((task, parentIdx) => {
    const parentIndex = parentIdx + 1
    const subtasks = task.subtasks || []
    const children = subtasks.length
      ? subtasks.map((child, childIdx) => ({
          id: child.id,
          title: child.title,
          description: child.description,
          status: child.status,
          priority: child.priority,
          projectId: child.projectId,
          planEndDate: child.planEndDate,
          actualEndDate: child.actualEndDate,
          estimateHours: child.estimateHours,
          actualHours: child.actualHours,
          isSubtask: true,
          indexLabel: `${parentIndex}.${childIdx + 1}`
        }))
      : undefined

    return {
      id: task.id,
      title: task.title,
      description: task.description,
      status: task.status,
      priority: task.priority,
      projectId: task.projectId,
      planEndDate: task.planEndDate,
      actualEndDate: task.actualEndDate,
      estimateHours: task.estimateHours,
      actualHours: task.actualHours,
      isSubtask: false,
      subtaskCount: subtasks.length || undefined,
      indexLabel: String(parentIndex),
      children
    }
  })
}

const pendingProjectOptions = computed(() => buildProjectOptions(dashboardData.pendingTasks))
const completedProjectOptions = computed(() => buildProjectOptions(dashboardData.completedTasks))

// 树形列表：默认收起；父任务 1、2…，展开后子任务 1.1、1.2…
const treePendingTasks = computed(() =>
  buildTreeTasks(dashboardData.pendingTasks, pendingProjectFilter.value)
)
const treeCompletedTasks = computed(() =>
  buildTreeTasks(dashboardData.completedTasks, completedProjectFilter.value)
)

const loading = ref(false)

// 获取状态映射
const getStatusType = (status: string) => getTaskStatusInfo(status).type
const getStatusText = (status: string) => getTaskStatusInfo(status).text
const getPriorityType = (priority: string) => getTaskPriorityInfo(priority).type
const getPriorityText = (priority: string) => getTaskPriorityInfo(priority).text
const getProjectStatusType = (status: string) => getProjectStatusInfo(status).type
const getProjectStatusText = (status: string) => getProjectStatusInfo(status).text

// 获取看板数据
const fetchDashboardData = async (showSuccess = false) => {
  loading.value = true
  try {
    const data = await getDashboardData()
    Object.assign(dashboardData, data)
    if (showSuccess) {
      ElMessage.success('看板已刷新')
    }
  } catch (error) {
    console.error('获取看板数据失败:', error)
    ElMessage.error('获取看板数据失败')
  } finally {
    loading.value = false
  }
}

const handleRefresh = () => {
  fetchDashboardData(true)
}

// 跳转到任务详情
const goToTask = (taskId: number) => {
  router.push(`/tasks/${taskId}`)
}

// 跳转到任务列表（带状态过滤）
const goToTasks = (statusFilter?: string) => {
  if (statusFilter) {
    router.push({
      path: '/tasks',
      query: { status: statusFilter }
    })
  } else {
    router.push('/tasks')
  }
}

// 跳转到项目详情
const goToProject = (projectId: number) => {
  router.push(`/projects/${projectId}`)
}

// 获取项目名称
const getProjectName = (projectId: number): string => {
  const project = dashboardData.projects.find(p => p.id === projectId)
  return project?.name || `项目${projectId}`
}

// 格式化日期（待办：相对截止日期）
const formatDate = (dateStr?: string): string => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const now = new Date()
  const diffTime = date.getTime() - now.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  if (diffDays < 0) return '已逾期'
  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '明天'
  if (diffDays <= 7) return `${diffDays}天后`

  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}/${day}`
}

// 已完成日期：固定展示月/日
const formatCompletedDate = (dateStr?: string): string => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return '-'
  return `${date.getMonth() + 1}/${date.getDate()}`
}

/** 预估工时：任务创建时填写的 estimateHours，未填显示 -- */
const formatEstimateHours = (estimateHours?: number | string | null) => {
  if (estimateHours == null || estimateHours === '') return '--'
  const hours = Number(estimateHours)
  if (!Number.isFinite(hours) || hours <= 0) return '--'
  return Number.isInteger(hours) ? `${hours} 小时` : `${hours} 小时`
}

/** 实际工时：关联工时记录合计；已完成且未填则标为快速修复 */
const getActualHours = (row: { actualHours?: number | string | null }) => {
  const hours = Number(row.actualHours ?? 0)
  return Number.isFinite(hours) ? hours : 0
}

const isQuickFixTask = (row: { actualHours?: number | string | null; status?: string }) => {
  return getActualHours(row) <= 0 && row.status === 'DONE'
}

const formatActualHours = (row: { actualHours?: number | string | null; status?: string }) => {
  const hours = getActualHours(row)
  if (hours > 0) {
    return Number.isInteger(hours) ? `${hours} 小时` : `${hours} 小时`
  }
  if (row.status === 'DONE') return '快速修复'
  return '--'
}

// 获取截止日期样式类
const getDueDateClass = (dateStr?: string): string => {
  if (!dateStr) return ''

  const date = new Date(dateStr)
  const now = new Date()
  const diffTime = date.getTime() - now.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  if (diffDays < 0) return 'due-date-overdue'
  if (diffDays <= 2) return 'due-date-soon'
  return ''
}

onMounted(() => {
  fetchDashboardData()
})
</script>

<style scoped>
/* ========== 首页特定样式 ========== */

/* 欢迎文本 */
.welcome-text {
  font-size: 14px;
  color: #666;
}

/* 统计图标渐变色 */
.stats-icon.project {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.stats-icon.task {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: #fff;
}

.stats-icon.today {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: #fff;
}

.stats-icon.week {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  color: #fff;
}

.stats-icon.month {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  color: #fff;
}

.stats-icon.total {
  background: linear-gradient(135deg, #30cfd0 0%, #330867 100%);
  color: #fff;
}

/* Tab 相关 */
.tab-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-badge {
  margin-left: 4px;
}

.tab-footer {
  margin-top: 16px;
  text-align: right;
}

/* 截止日期样式 */
.due-date-overdue {
  color: #f56c6c;
  font-weight: 500;
}

.due-date-soon {
  color: #e6a23c;
  font-weight: 500;
}

/* 待办树表：展开列与序号对齐 */
.pending-task-table-wrap {
  width: 100%;
  overflow-x: auto;
}

.pending-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.pending-task-table :deep(.expand-col .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.pending-task-table :deep(.expand-col .el-table__expand-icon) {
  margin: 0;
  height: 20px;
  line-height: 20px;
}

.pending-task-table :deep(.index-col .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-left: 4px;
  padding-right: 4px;
}

/* 序号：父任务深色，子任务浅色 */
.task-index {
  display: inline-block;
  min-width: 28px;
  text-align: center;
  font-weight: 600;
  color: #303133;
  line-height: 20px;
}

.task-index.is-subtask {
  font-weight: 500;
  color: #909399;
}

/* 待办标题与子任务 */
.task-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.task-title-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.subtask-count-tag {
  flex-shrink: 0;
}

.task-title-cell.is-subtask {
  color: #606266;
}

.quick-fix-label {
  color: #e6a23c;
}
</style>
