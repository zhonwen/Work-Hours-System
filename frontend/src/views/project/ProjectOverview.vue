<template>
  <div class="project-overview">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 空状态 -->
    <div v-else-if="!project" class="empty-container">
      <el-empty description="项目信息加载失败" />
    </div>

    <!-- 项目概览内容 -->
    <div v-else class="overview-content">
      <!-- 项目基本信息卡片 -->
      <div class="info-card">
        <div class="info-header">
          <div class="header-left">
            <h2 class="project-title">{{ project.name }}</h2>
            <el-tag :type="getStatusType(project.status)" size="large">
              {{ getStatusText(project.status) }}
            </el-tag>
          </div>
          <div class="header-right">
            <span class="project-code">{{ project.code }}</span>
          </div>
        </div>
        <p class="project-description">{{ project.description || '暂无描述' }}</p>
        <div class="project-meta">
          <div class="meta-item">
            <el-icon><Calendar /></el-icon>
            <span>创建时间：{{ formatDate(project.createTime) }}</span>
          </div>
          <div class="meta-item">
            <el-icon><User /></el-icon>
            <span>项目经理：{{ managerName }}</span>
          </div>
        </div>
      </div>

      <!-- 关键指标卡片 -->
      <div class="metrics-grid">
        <div class="metric-card">
          <div class="metric-icon" style="background: #e6f7ff; color: #1890ff;">
            <el-icon :size="24"><List /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ metrics.totalTasks }}</div>
            <div class="metric-label">总任务数</div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon" style="background: #f6ffed; color: #52c41a;">
            <el-icon :size="24"><CircleCheck /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ metrics.completedTasks }}</div>
            <div class="metric-label">已完成</div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon" style="background: #fff7e6; color: #faad14;">
            <el-icon :size="24"><Clock /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ metrics.remainingTasks }}</div>
            <div class="metric-label">待处理</div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon" style="background: #fff0f6; color: #eb2f96;">
            <el-icon :size="24"><TrendCharts /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ metrics.completionRate }}%</div>
            <div class="metric-label">完成率</div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon" style="background: #f9f0ff; color: #722ed1;">
            <el-icon :size="24"><Timer /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ metrics.totalHours }}</div>
            <div class="metric-label">项目工时(h)</div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon" style="background: #fff1f0; color: #f5222d;">
            <el-icon :size="24"><Warning /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ metrics.overdueTasks }}</div>
            <div class="metric-label">超期任务</div>
          </div>
        </div>
      </div>

      <!-- 进度可视化 -->
      <div class="progress-section">
        <h3 class="section-title">项目进度</h3>
        <div class="progress-bar-container">
          <el-progress
            :percentage="metrics.completionRate"
            :color="progressColors"
            :stroke-width="20"
            text-inside
          />
        </div>
        <div class="progress-stats">
          <div class="stat-item">
            <span class="stat-label">待办</span>
            <span class="stat-value">{{ metrics.todoTasks }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">进行中</span>
            <span class="stat-value">{{ metrics.inProgressTasks }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">已完成</span>
            <span class="stat-value">{{ metrics.completedTasks }}</span>
          </div>
        </div>
      </div>

      <!-- 团队概览：负责人固定左侧，其余成员横向滚动 -->
      <div class="team-section">
        <div class="section-header">
          <h3 class="section-title">团队成员 ({{ memberCount }})</h3>
          <div class="section-actions">
            <el-button type="primary" size="small" :icon="Plus" @click="handleAddMember">
              添加成员
            </el-button>
            <el-button size="small" @click="handleViewMoreMembers">更多</el-button>
          </div>
        </div>
        <div class="member-row">
          <!-- 负责人（managerId）固定左侧 -->
          <div class="owner-slot">
            <div v-if="ownerMember" class="member-card owner-card">
              <el-avatar :size="40">{{ ownerMember.nickname?.charAt(0) || '?' }}</el-avatar>
              <div class="member-info">
                <div class="member-name">{{ ownerMember.nickname || '未命名' }}</div>
                <div class="member-role-row">
                  <el-tag type="warning" size="small" effect="plain">负责人</el-tag>
                  <el-tag
                    v-if="ownerMember.roleType === 1"
                    type="primary"
                    size="small"
                    effect="plain"
                  >
                    项目经理
                  </el-tag>
                </div>
              </div>
            </div>
            <div v-else class="member-card owner-card owner-empty">
              <el-avatar :size="40">?</el-avatar>
              <div class="member-info">
                <div class="member-name">未指定负责人</div>
                <div class="member-role">请在项目设置中指定</div>
              </div>
            </div>
          </div>

          <div class="member-divider" />

          <!-- 其他成员可滚动 -->
          <div class="member-scroll">
            <div
              v-for="member in scrollableMembers"
              :key="member.userId"
              class="member-card"
            >
              <el-avatar :size="36">{{ member.nickname?.charAt(0) }}</el-avatar>
              <div class="member-info">
                <div class="member-name">{{ member.nickname }}</div>
                <div class="member-role-row">
                  <el-tag
                    v-if="member.roleType === 1"
                    type="primary"
                    size="small"
                    effect="plain"
                  >
                    项目经理
                  </el-tag>
                  <span v-else class="member-role">{{ getRoleText(member.roleType) }}</span>
                </div>
              </div>
              <el-button
                link
                type="danger"
                size="small"
                @click="handleRemoveMember(member)"
              >
                移除
              </el-button>
            </div>
            <div v-if="scrollableMembers.length === 0" class="member-scroll-empty">
              暂无其他成员
            </div>
          </div>
        </div>
      </div>

      <!-- 迭代概览 -->
      <div class="iteration-section">
        <h3 class="section-title">迭代概览</h3>
        <div v-if="iterations.length > 0" class="iteration-list">
          <div
            v-for="iteration in iterations.slice(0, 5)"
            :key="iteration.id"
            class="iteration-item"
            @click="handleViewIteration(iteration)"
          >
            <div class="iteration-header">
              <span class="iteration-name">{{ iteration.name }}</span>
              <el-tag :type="getIterationStatusType(iteration.status)" size="small">
                {{ getIterationStatusText(iteration.status) }}
              </el-tag>
            </div>
            <div class="iteration-meta">
              <span v-if="iteration.planStartDate" class="iteration-date">
                {{ formatDate(iteration.planStartDate) }} - {{ formatDate(iteration.planEndDate) }}
              </span>
              <span v-else class="iteration-date">时间未定</span>
            </div>
            <div class="iteration-progress">
              <el-progress
                :percentage="calculateIterationProgress(iteration)"
                :stroke-width="6"
                :show-text="false"
              />
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无迭代数据" :image-size="80" />
      </div>

      <!-- 最近活动 -->
      <div class="activity-section">
        <h3 class="section-title">最近活动</h3>
        <el-timeline v-if="recentActivities.length > 0">
          <el-timeline-item
            v-for="activity in recentActivities.slice(0, 10)"
            :key="activity.id"
            :timestamp="formatRelativeTime(activity.timestamp)"
            placement="top"
          >
            <div class="activity-item">
              <span class="activity-user">{{ activity.user }}</span>
              <span class="activity-action">{{ activity.action }}</span>
              <span class="activity-target">{{ activity.target }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无最近活动" :image-size="80" />
      </div>
    </div>

    <!-- 添加成员对话框 -->
    <el-dialog
      v-model="addMemberDialogVisible"
      title="添加项目成员"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="memberFormData" label-width="100px">
        <el-form-item label="选择用户">
          <el-select
            v-model="memberFormData.userId"
            placeholder="请选择用户"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="user in availableUsers"
              :key="user.id"
              :label="user.nickname"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="角色类型">
          <el-radio-group v-model="memberFormData.roleType">
            <el-radio :value="1">项目管理员</el-radio>
            <el-radio :value="2">普通成员</el-radio>
            <el-radio :value="3">只读访客</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addMemberDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddMemberSubmit" :loading="addMemberLoading">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Calendar,
  User,
  List,
  CircleCheck,
  Clock,
  TrendCharts,
  Timer,
  Warning,
  Plus
} from '@element-plus/icons-vue'
import { getProjectDetail, getProjectMembers, addProjectMember, removeProjectMember } from '@/api/project'
import { getAllUsers, type UserInfo } from '@/api/user'
import { getTasksByProjectId, type TaskInfo } from '@/api/task'
import { getIterationList, type IterationInfo } from '@/api/iteration'
import { getWorkHoursByProjectId, type WorkHourInfo } from '@/api/workhour'

// Props
const props = defineProps<{
  projectId: number
}>()

const emit = defineEmits<{
  (e: 'navigate-members'): void
}>()

interface OverviewMember {
  userId: number
  nickname?: string
  roleType?: number
}

// 数据定义
const project = ref<any>(null)
const members = ref<OverviewMember[]>([])
const tasks = ref<TaskInfo[]>([])
const projectWorkHours = ref<WorkHourInfo[]>([])
const iterations = ref<IterationInfo[]>([])
const availableUsers = ref<UserInfo[]>([])
const loading = ref(false)
const router = useRouter()

// 添加成员对话框
const addMemberDialogVisible = ref(false)
const addMemberLoading = ref(false)
const memberFormData = ref({
  userId: undefined as number | undefined,
  roleType: 2 // 默认为普通成员
})

// 关键指标
const metrics = reactive({
  totalTasks: 0,
  completedTasks: 0,
  remainingTasks: 0,
  todoTasks: 0,
  inProgressTasks: 0,
  overdueTasks: 0,
  totalHours: 0,
  completionRate: 0
})

// 最近活动
const recentActivities = ref<any[]>([])

// 进度条颜色
const progressColors = [
  { color: '#f5222d', percentage: 20 },
  { color: '#faad14', percentage: 40 },
  { color: '#1890ff', percentage: 60 },
  { color: '#52c41a', percentage: 80 },
  { color: '#52c41a', percentage: 100 }
]

// 计算属性
const memberCount = computed(() => members.value.length)

/** 负责人：以项目 managerId 为准（唯一） */
const ownerMember = computed<OverviewMember | null>(() => {
  const managerId = project.value?.managerId
  if (!managerId) return null
  const fromMembers = members.value.find(m => Number(m.userId) === Number(managerId))
  if (fromMembers) return fromMembers
  const fromUsers = availableUsers.value.find(u => Number(u.id) === Number(managerId))
  if (fromUsers) {
    return {
      userId: Number(fromUsers.id),
      nickname: fromUsers.nickname,
      roleType: 1
    }
  }
  return { userId: Number(managerId), nickname: `用户${managerId}`, roleType: 1 }
})

/** 可滚动成员：排除负责人；多名「项目经理」角色在此以徽章展示 */
const scrollableMembers = computed(() => {
  const managerId = project.value?.managerId
  if (!managerId) return members.value
  return members.value.filter(m => Number(m.userId) !== Number(managerId))
})

const managerName = computed(() => ownerMember.value?.nickname || '未指定')

// 获取项目详情
const fetchProject = async () => {
  try {
    const res = await getProjectDetail(props.projectId)
    project.value = res
  } catch (error) {
    console.error('获取项目详情失败:', error)
    ElMessage.error('获取项目详情失败')
  }
}

// 获取项目成员
const fetchMembers = async () => {
  try {
    const res = await getProjectMembers(props.projectId)
    members.value = res || []
  } catch (error) {
    console.error('获取项目成员失败:', error)
  }
}

// 获取可用用户列表
const fetchAvailableUsers = async () => {
  try {
    const res = await getAllUsers()
    availableUsers.value = res.list || []
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

// 获取任务列表
const fetchTasks = async () => {
  try {
    const res = await getTasksByProjectId(props.projectId, 1, 1000)
    tasks.value = res.list || []
    calculateMetrics()
  } catch (error) {
    console.error('获取任务列表失败:', error)
  }
}

// 获取项目实际填报工时（业务项目只统计本项目真实投入）
const fetchProjectWorkHours = async () => {
  try {
    const res = await getWorkHoursByProjectId(props.projectId)
    projectWorkHours.value = (Array.isArray(res) ? res : []) as WorkHourInfo[]
    calculateMetrics()
  } catch (error) {
    console.error('获取项目工时失败:', error)
    projectWorkHours.value = []
  }
}

// 获取迭代列表
const fetchIterations = async () => {
  try {
    const res = await getIterationList({ projectId: props.projectId, pageNum: 1, pageSize: 100 })
    iterations.value = res.list || []
  } catch (error) {
    console.error('获取迭代列表失败:', error)
  }
}

// 计算关键指标
const calculateMetrics = () => {
  const taskList = tasks.value
  metrics.totalTasks = taskList.length
  metrics.completedTasks = taskList.filter(t => t.status === 'DONE').length
  metrics.todoTasks = taskList.filter(t => t.status === 'TODO').length
  metrics.inProgressTasks = taskList.filter(t => t.status === 'IN_PROGRESS').length
  metrics.remainingTasks = metrics.todoTasks + metrics.inProgressTasks

  // 计算超期任务（计划结束时间已过但未完成的任务）
  const now = new Date()
  metrics.overdueTasks = taskList.filter(t => {
    if (t.status === 'DONE' || !t.planEndDate) return false
    const endDate = new Date(t.planEndDate)
    return endDate < now
  }).length

  // 项目工时：仅正常投入（不含请假）
  metrics.totalHours = Math.round(
    projectWorkHours.value
      .filter(wh => wh.recordType !== 'LEAVE' && wh.recordType !== 2)
      .reduce((sum, wh) => sum + Number(wh.hours || 0), 0) * 10
  ) / 10

  // 计算完成率
  metrics.completionRate = metrics.totalTasks > 0
    ? Math.round((metrics.completedTasks / metrics.totalTasks) * 100)
    : 0
}

// 获取项目状态类型
const getStatusType = (status: string) => {
  const types: Record<string, string> = {
    'NOT_STARTED': 'info',
    'IN_PROGRESS': 'primary',
    'SUSPENDED': 'warning',
    'ARCHIVED': 'success'
  }
  return types[status] || 'info'
}

// 获取项目状态文本
const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    'NOT_STARTED': '未开始',
    'IN_PROGRESS': '进行中',
    'SUSPENDED': '已暂停',
    'ARCHIVED': '已归档'
  }
  return texts[status] || '未知'
}

// 获取角色文本
const getRoleText = (roleType: number) => {
  const texts: Record<number, string> = {
    1: '管理员',
    2: '成员',
    3: '访客'
  }
  return texts[roleType] || '未知'
}

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

// 获取迭代状态类型
const getIterationStatusType = (status: string) => {
  const types: Record<string, string> = {
    'NOT_STARTED': 'info',
    'IN_PROGRESS': 'primary',
    'COMPLETED': 'success'
  }
  return types[status] || 'info'
}

// 获取迭代状态文本
const getIterationStatusText = (status: string) => {
  const texts: Record<string, string> = {
    'NOT_STARTED': '未开始',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成'
  }
  return texts[status] || '未知'
}

// 计算迭代进度
const calculateIterationProgress = (iteration: IterationInfo) => {
  const iterationTasks = tasks.value.filter(t => t.iterationId === iteration.id)
  if (iterationTasks.length === 0) return 0
  const completedTasks = iterationTasks.filter(t => t.status === 'DONE').length
  return Math.round((completedTasks / iterationTasks.length) * 100)
}

// 查看迭代详情
const handleViewIteration = (iteration: IterationInfo) => {
  router.push(`/projects/${props.projectId}/iterations/${iteration.id}`)
}

// 格式化相对时间
const formatRelativeTime = (timestamp: string) => {
  const now = new Date()
  const time = new Date(timestamp)
  const diff = now.getTime() - time.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return formatDate(timestamp)
}

// 生命周期
onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([
      fetchProject(),
      fetchMembers(),
      fetchAvailableUsers(),
      fetchTasks(),
      fetchProjectWorkHours(),
      fetchIterations()
    ])
  } finally {
    loading.value = false
  }
})

// 打开添加成员对话框
const handleAddMember = () => {
  memberFormData.value = {
    userId: undefined,
    roleType: 2
  }
  addMemberDialogVisible.value = true
}

// 更多 → 跳转团队协作页签
const handleViewMoreMembers = () => {
  emit('navigate-members')
}

// 提交添加成员
const handleAddMemberSubmit = async () => {
  if (!memberFormData.value.userId) {
    ElMessage.warning('请选择用户')
    return
  }

  addMemberLoading.value = true
  try {
    await addProjectMember(props.projectId, memberFormData.value.userId, memberFormData.value.roleType)
    ElMessage.success('添加成功')
    addMemberDialogVisible.value = false
    await fetchMembers()
  } catch (error) {
    console.error('添加成员失败:', error)
    ElMessage.error('添加失败')
  } finally {
    addMemberLoading.value = false
  }
}

// 移除成员（负责人不可在概览移除）
const handleRemoveMember = async (member: OverviewMember) => {
  if (project.value?.managerId && Number(member.userId) === Number(project.value.managerId)) {
    ElMessage.warning('项目负责人不可在此移除，请先更换负责人')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要移除成员 "${member.nickname}" 吗？移除后将无法访问此项目。`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await removeProjectMember(props.projectId, member.userId)
    ElMessage.success('移除成功')
    await fetchMembers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移除成员失败:', error)
      ElMessage.error('移除失败')
    }
  }
}
</script>

<style scoped>
.project-overview {
  height: 100%;
  overflow-y: auto;
  padding: 24px;
  background: #f5f5f5;
}

.loading-container,
.empty-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.overview-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 项目基本信息卡片 */
.info-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.project-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.project-code {
  font-size: 14px;
  color: #999;
  font-family: 'Monaco', 'Consolas', monospace;
}

.project-description {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.project-meta {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #666;
}

/* 关键指标卡片 */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.metric-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.metric-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.metric-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.metric-content {
  flex: 1;
}

.metric-value {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  line-height: 1.2;
}

.metric-label {
  font-size: 14px;
  color: #999;
  margin-top: 4px;
}

/* 进度可视化 */
.progress-section {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.section-title {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.progress-bar-container {
  margin-bottom: 16px;
}

.progress-stats {
  display: flex;
  gap: 32px;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 4px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

/* 团队概览 */
.team-section {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.member-row {
  display: flex;
  align-items: stretch;
  gap: 12px;
  min-height: 72px;
}

.owner-slot {
  flex: 0 0 auto;
  width: 220px;
}

.member-divider {
  width: 1px;
  background: #e8e8e8;
  flex-shrink: 0;
  align-self: stretch;
}

.member-scroll {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: thin;
}

.member-scroll::-webkit-scrollbar {
  height: 6px;
}

.member-scroll::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 3px;
}

.member-scroll-empty {
  color: #999;
  font-size: 13px;
  padding: 0 8px;
  white-space: nowrap;
}

.member-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #f5f5f5;
  border-radius: 6px;
  transition: background 0.2s;
  flex: 0 0 auto;
  min-width: 180px;
  max-width: 240px;
}

.member-card:hover {
  background: #f0f0f0;
}

.owner-card {
  width: 100%;
  max-width: none;
  min-width: 0;
  background: #fff7e6;
  border: 1px solid #ffd591;
}

.owner-card:hover {
  background: #fff1b8;
}

.owner-empty {
  opacity: 0.85;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-role {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.member-role-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
  flex-wrap: wrap;
}

/* 迭代概览 */
.iteration-section {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.iteration-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.iteration-item {
  padding: 16px;
  background: #f5f5f5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.iteration-item:hover {
  background: #f0f0f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.iteration-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.iteration-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.iteration-meta {
  margin-bottom: 12px;
}

.iteration-date {
  font-size: 13px;
  color: #666;
}

.iteration-progress {
  margin-top: 8px;
}

/* 最近活动 */
.activity-section {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.activity-item {
  font-size: 14px;
  line-height: 1.6;
}

.activity-user {
  font-weight: 500;
  color: #333;
}

.activity-action {
  color: #666;
  margin: 0 4px;
}

.activity-target {
  color: #1890ff;
  cursor: pointer;
}

.activity-target:hover {
  text-decoration: underline;
}
</style>
