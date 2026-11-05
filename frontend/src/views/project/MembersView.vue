<template>
  <div class="members-view">
    <!-- 搜索筛选：对齐全局 search-form -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item>
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索成员姓名"
            :prefix-icon="Search"
            clearable
            style="width: 220px"
            @input="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-select
            v-model="searchForm.roleType"
            placeholder="按角色筛选"
            clearable
            style="width: 148px"
            @change="handleSearch"
          >
            <el-option label="项目管理员" :value="1" />
            <el-option label="普通成员" :value="2" />
            <el-option label="只读访客" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select
            v-model="searchForm.sortBy"
            placeholder="排序方式"
            clearable
            style="width: 148px"
            @change="handleSort"
          >
            <el-option label="默认排序" value="" />
            <el-option label="任务数量" value="taskCount" />
            <el-option label="工时总数" value="totalHours" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-form-actions">
          <el-button type="primary" :icon="Plus" @click="handleAddMember">
            添加成员
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 成员网格 -->
    <div v-loading="loading" class="members-grid">
      <div
        v-for="member in filteredMembers"
        :key="member.id"
        class="member-card"
        @click="handleViewMember(member)"
      >
        <!-- 成员头像和基本信息 -->
        <div class="member-header">
          <el-avatar :size="56" class="member-avatar">
            {{ (member.nickname || 'U').charAt(0) }}
          </el-avatar>
          <div class="member-basic">
            <div class="member-name">{{ member.nickname }}</div>
            <div class="member-email">{{ member.email || '无邮箱' }}</div>
          </div>
          <div class="member-tags">
            <el-tag v-if="isProjectOwner(member)" type="warning" size="small" effect="plain">
              负责人
            </el-tag>
            <el-tag :type="getRoleType(member.roleType)" size="small">
              {{ member.roleName }}
            </el-tag>
          </div>
        </div>

        <!-- 统计数据 -->
        <div class="member-stats">
          <div class="stat-item">
            <div class="stat-label">任务数量</div>
            <div class="stat-value">{{ getMemberTaskCount(member.userId) }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">总工时</div>
            <div class="stat-value">{{ getMemberTotalHours(member.userId) }}h</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">本周工时</div>
            <div class="stat-value">{{ getMemberWeekHours(member.userId) }}h</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">本月工时</div>
            <div class="stat-value">{{ getMemberMonthHours(member.userId) }}h</div>
          </div>
        </div>

        <!-- 贡献度 -->
        <div class="member-contribution">
          <div class="contribution-header">
            <div class="contribution-label">贡献度</div>
            <div class="contribution-score">{{ getContributionScore(member.userId) }}</div>
          </div>
          <div class="contribution-progress">
            <el-progress
              :percentage="getContributionPercentage(member.userId)"
              :stroke-width="8"
              :show-text="false"
              :color="getContributionColor(member.userId)"
            />
          </div>
          <div class="contribution-stars">
            <span
              v-for="i in 5"
              :key="i"
              class="star"
              :class="{ active: i <= getContributionLevel(member.userId) }"
            >★</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="member-actions" @click.stop>
          <el-button link type="primary" size="small" @click="handleViewMember(member)">
            查看详情
          </el-button>
          <el-button link type="primary" size="small" @click="handleAssignTask(member)">
            分配任务
          </el-button>
          <el-dropdown trigger="click" @command="(cmd) => handleMoreAction(cmd, member)">
            <el-button link type="primary" size="small">
              更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="role">更改角色</el-dropdown-item>
                <el-dropdown-item command="remove" divided>移除成员</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty
        v-if="filteredMembers.length === 0 && !loading"
        description="暂无成员"
        :image-size="100"
      />
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

    <!-- 成员详情对话框 -->
    <el-dialog
      v-model="memberDetailDialogVisible"
      :title="`${currentMember?.nickname} 的详细信息`"
      width="800px"
      :close-on-click-modal="false"
    >
      <div v-if="currentMember" class="member-detail">
        <!-- 基本信息 -->
        <div class="detail-section">
          <h4 class="section-title">基本信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="label">头像：</span>
              <el-avatar :size="48">{{ (currentMember.nickname || 'U').charAt(0) }}</el-avatar>
            </div>
            <div class="detail-item">
              <span class="label">姓名：</span>
              <span class="value">{{ currentMember.nickname }}</span>
            </div>
            <div class="detail-item">
              <span class="label">角色：</span>
              <el-tag :type="getRoleType(currentMember.roleType)" size="small">
                {{ currentMember.roleName }}
              </el-tag>
            </div>
            <div class="detail-item">
              <span class="label">邮箱：</span>
              <span class="value">{{ currentMember.email || '无邮箱' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">用户名：</span>
              <span class="value">{{ currentMember.username || '-' }}</span>
            </div>
          </div>
        </div>

        <!-- 工时统计 -->
        <div class="detail-section">
          <h4 class="section-title">工时统计</h4>
          <div class="stats-cards">
            <div class="stat-card">
              <div class="stat-label">总工时</div>
              <div class="stat-value primary">{{ getMemberTotalHours(currentMember.userId) }}h</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">本周工时</div>
              <div class="stat-value success">{{ getMemberWeekHours(currentMember.userId) }}h</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">本月工时</div>
              <div class="stat-value warning">{{ getMemberMonthHours(currentMember.userId) }}h</div>
            </div>
          </div>
        </div>

        <!-- 任务列表 -->
        <div class="detail-section">
          <h4 class="section-title">参与的任务 ({{ getMemberTasks(currentMember.userId).length }})</h4>
          <div class="task-list">
            <div
              v-for="task in getMemberTasks(currentMember.userId)"
              :key="task.id"
              class="task-item"
            >
              <el-tag :type="getTaskStatusType(task.status)" size="small">
                {{ getTaskStatusText(task.status) }}
              </el-tag>
              <span class="task-title">{{ task.title }}</span>
              <span class="task-hours">{{ task.estimateHours || 0 }}h</span>
            </div>
            <el-empty v-if="getMemberTasks(currentMember.userId).length === 0" description="暂无任务" />
          </div>
        </div>

        <!-- 贡献度分析 -->
        <div class="detail-section">
          <h4 class="section-title">贡献度分析</h4>
          <div class="contribution-analysis">
            <div class="contribution-item">
              <span class="label">完成任务数：</span>
              <span class="value">{{ getCompletedTaskCount(currentMember.userId) }}</span>
            </div>
            <div class="contribution-item">
              <span class="label">贡献度等级：</span>
              <div class="stars">
                <span
                  v-for="i in 5"
                  :key="i"
                  class="star"
                  :class="{ active: i <= getContributionLevel(currentMember.userId) }"
                >★</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 更改角色对话框 -->
    <el-dialog
      v-model="changeRoleDialogVisible"
      title="更改成员角色"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form :model="roleFormData" label-width="100px">
        <el-form-item label="当前角色">
          <el-tag :type="getRoleType(currentMember?.roleType)" size="small">
            {{ currentMember?.roleName }}
          </el-tag>
        </el-form-item>
        <el-form-item label="新角色">
          <el-radio-group v-model="roleFormData.roleType">
            <el-radio :value="1">项目管理员</el-radio>
            <el-radio :value="2">普通成员</el-radio>
            <el-radio :value="3">只读访客</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeRoleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangeRoleSubmit" :loading="changeRoleLoading">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Plus,
  ArrowDown
} from '@element-plus/icons-vue'
import {
  getProjectMembers,
  addProjectMember,
  removeProjectMember,
  getProjectMemberStats
} from '@/api/project'
import { getTasksByProjectId, type TaskInfo } from '@/api/task'
import { getAllUsers, type UserInfo } from '@/api/user'

// Props
const props = defineProps<{
  projectId: number
  managerId?: number
}>()

// Emits
const emit = defineEmits<{
  'refresh': []
}>()

// 数据定义
const loading = ref(false)
const members = ref<any[]>([])
const tasks = ref<TaskInfo[]>([])
const availableUsers = ref<UserInfo[]>([])
const memberStats = ref<Record<number, {
  totalHours: number
  weekHours: number
  monthHours: number
  taskCount: number
  completedCount: number
}>>({})

// 搜索和筛选
const searchForm = reactive({
  keyword: '',
  roleType: undefined as number | undefined,
  sortBy: ''
})

// 添加成员对话框
const addMemberDialogVisible = ref(false)
const addMemberLoading = ref(false)
const memberFormData = ref({
  userId: undefined as number | undefined,
  roleType: 2
})

// 成员详情对话框
const memberDetailDialogVisible = ref(false)
const currentMember = ref<any>(null)

// 更改角色对话框
const changeRoleDialogVisible = ref(false)
const changeRoleLoading = ref(false)
const roleFormData = ref({
  roleType: 2
})

const isProjectOwner = (member: { userId: number }) => {
  return props.managerId != null && Number(member.userId) === Number(props.managerId)
}

/** 负责人始终靠前，其余再按所选规则排序 */
const compareMembers = (a: any, b: any) => {
  const aOwner = isProjectOwner(a) ? 0 : 1
  const bOwner = isProjectOwner(b) ? 0 : 1
  if (aOwner !== bOwner) return aOwner - bOwner

  if (searchForm.sortBy === 'taskCount') {
    return getMemberTaskCount(b.userId) - getMemberTaskCount(a.userId)
  }
  if (searchForm.sortBy === 'totalHours') {
    return getMemberTotalHours(b.userId) - getMemberTotalHours(a.userId)
  }
  // 默认：负责人 → 项目管理员 → 普通成员 → 访客，同角色按姓名
  const roleOrder = (roleType?: number) => {
    if (roleType === 1) return 0
    if (roleType === 2) return 1
    if (roleType === 3) return 2
    return 9
  }
  const roleDiff = roleOrder(a.roleType) - roleOrder(b.roleType)
  if (roleDiff !== 0) return roleDiff
  return String(a.nickname || '').localeCompare(String(b.nickname || ''), 'zh-CN')
}

// 过滤后的成员列表（负责人固定首位）
const filteredMembers = computed(() => {
  let result = [...members.value]

  if (searchForm.keyword) {
    const keyword = searchForm.keyword.toLowerCase()
    result = result.filter(m =>
      (m.nickname || '').toLowerCase().includes(keyword) ||
      (m.email || '').toLowerCase().includes(keyword)
    )
  }

  if (searchForm.roleType !== undefined) {
    result = result.filter(m => m.roleType === searchForm.roleType)
  }

  result.sort(compareMembers)
  return result
})

// 获取项目成员
const fetchMembers = async () => {
  try {
    const res = await getProjectMembers(props.projectId)
    members.value = res || []
  } catch (error) {
    console.error('获取项目成员失败:', error)
  }
}

// 获取任务列表
const fetchTasks = async () => {
  try {
    const res = await getTasksByProjectId(props.projectId, 1, 1000)
    tasks.value = res.list || []
  } catch (error) {
    console.error('获取任务列表失败:', error)
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

// 计算成员统计数据（工时仅统计本项目）
const calculateMemberStats = async () => {
  const stats: Record<number, any> = {}

  for (const member of members.value) {
    const userId = member.userId
    const memberTasks = tasks.value.filter(t => t.assigneeId === userId)
    const completedTasks = memberTasks.filter(t => t.status === 'DONE')

    stats[userId] = {
      taskCount: memberTasks.length,
      completedCount: completedTasks.length,
      totalHours: 0,
      weekHours: 0,
      monthHours: 0
    }

    try {
      const res = await getProjectMemberStats(props.projectId, userId)
      stats[userId].totalHours = Number(res?.totalHours || 0)
      stats[userId].weekHours = Number(res?.weekHours || 0)
      stats[userId].monthHours = Number(res?.monthHours || 0)
      // 后端已按项目过滤任务数，优先采用
      if (res?.totalTasks != null) stats[userId].taskCount = res.totalTasks
      if (res?.completedTasks != null) stats[userId].completedCount = res.completedTasks
    } catch (error) {
      console.error(`获取成员 ${userId} 本项目工时统计失败:`, error)
    }
  }

  memberStats.value = stats
}

// 获取成员任务数量
const getMemberTaskCount = (userId: number) => {
  return tasks.value.filter(t => t.assigneeId === userId).length
}

// 获取成员任务列表
const getMemberTasks = (userId: number) => {
  return tasks.value.filter(t => t.assigneeId === userId)
}

// 获取成员完成任务数
const getCompletedTaskCount = (userId: number) => {
  return tasks.value.filter(t => t.assigneeId === userId && t.status === 'DONE').length
}

// 获取成员总工时
const getMemberTotalHours = (userId: number) => {
  return memberStats.value[userId]?.totalHours || 0
}

// 获取成员本周工时
const getMemberWeekHours = (userId: number) => {
  return memberStats.value[userId]?.weekHours || 0
}

// 获取成员本月工时
const getMemberMonthHours = (userId: number) => {
  return memberStats.value[userId]?.monthHours || 0
}

// 计算贡献度等级（基于完成任务数）
const getContributionLevel = (userId: number) => {
  const completedCount = getCompletedTaskCount(userId)
  if (completedCount >= 20) return 5
  if (completedCount >= 15) return 4
  if (completedCount >= 10) return 3
  if (completedCount >= 5) return 2
  if (completedCount >= 1) return 1
  return 0
}

// 计算贡献度分数（综合任务数和工时）
const getContributionScore = (userId: number) => {
  const taskScore = getCompletedTaskCount(userId) * 10
  const hourScore = Math.floor(getMemberTotalHours(userId) * 2)
  const totalScore = taskScore + hourScore

  if (totalScore >= 500) return 'S'
  if (totalScore >= 300) return 'A'
  if (totalScore >= 150) return 'B'
  if (totalScore >= 50) return 'C'
  return 'D'
}

// 计算贡献度百分比
const getContributionPercentage = (userId: number) => {
  const maxScore = Math.max(...members.value.map(m => {
    const taskScore = getCompletedTaskCount(m.userId) * 10
    const hourScore = Math.floor(getMemberTotalHours(m.userId) * 2)
    return taskScore + hourScore
  }), 1)

  const taskScore = getCompletedTaskCount(userId) * 10
  const hourScore = Math.floor(getMemberTotalHours(userId) * 2)
  const totalScore = taskScore + hourScore

  return Math.min(Math.round((totalScore / maxScore) * 100), 100)
}

// 获取贡献度颜色
const getContributionColor = (userId: number) => {
  const percentage = getContributionPercentage(userId)
  if (percentage >= 80) return '#52c41a'
  if (percentage >= 60) return '#1890ff'
  if (percentage >= 40) return '#faad14'
  return '#d9d9d9'
}

// 获取角色类型
const getRoleType = (roleType: number) => {
  const types: Record<number, string> = {
    1: 'danger',
    2: '',
    3: 'info'
  }
  return types[roleType] || 'info'
}

// 获取任务状态类型
const getTaskStatusType = (status: string) => {
  const types: Record<string, string> = {
    'TODO': 'info',
    'IN_PROGRESS': 'primary',
    'DONE': 'success'
  }
  return types[status] || 'info'
}

// 获取任务状态文本
const getTaskStatusText = (status: string) => {
  const texts: Record<string, string> = {
    'TODO': '待办',
    'IN_PROGRESS': '进行中',
    'DONE': '已完成'
  }
  return texts[status] || '未知'
}

// 搜索处理
const handleSearch = () => {
  // 搜索逻辑由 computed 自动处理
}

// 排序处理
const handleSort = () => {
  // 排序逻辑由 computed 自动处理
}

// 添加成员
const handleAddMember = () => {
  memberFormData.value = {
    userId: undefined,
    roleType: 2
  }
  addMemberDialogVisible.value = true
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
    await refresh()
  } catch (error) {
    console.error('添加成员失败:', error)
    ElMessage.error('添加失败')
  } finally {
    addMemberLoading.value = false
  }
}

// 查看成员详情
const handleViewMember = (member: any) => {
  currentMember.value = member
  memberDetailDialogVisible.value = true
}

// 分配任务
const handleAssignTask = (member: any) => {
  ElMessage.info(`为 ${member.nickname} 分配任务功能待实现`)
  // TODO: 打开任务分配对话框
}

// 更多操作
const handleMoreAction = (command: string, member: any) => {
  if (command === 'role') {
    handleChangeRole(member)
  } else if (command === 'remove') {
    handleRemoveMember(member)
  }
}

// 更改角色
const handleChangeRole = (member: any) => {
  currentMember.value = member
  roleFormData.value.roleType = member.roleType
  changeRoleDialogVisible.value = true
}

// 提交更改角色
const handleChangeRoleSubmit = async () => {
  if (!currentMember.value) return

  changeRoleLoading.value = true
  try {
    // 先移除成员，再以新角色添加
    await removeProjectMember(props.projectId, currentMember.value.userId)
    await addProjectMember(props.projectId, currentMember.value.userId, roleFormData.value.roleType)
    ElMessage.success('角色更改成功')
    changeRoleDialogVisible.value = false
    await refresh()
  } catch (error) {
    console.error('更改角色失败:', error)
    ElMessage.error('更改失败')
  } finally {
    changeRoleLoading.value = false
  }
}

// 移除成员
const handleRemoveMember = (member: any) => {
  ElMessageBox.confirm(
    `确定要移除成员 "${member.nickname}" 吗？移除后将无法访问此项目。`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      try {
        await removeProjectMember(props.projectId, member.userId)
        ElMessage.success('移除成功')
        await refresh()
      } catch (error) {
        console.error('移除成员失败:', error)
        ElMessage.error('移除失败')
      }
    })
    .catch(() => {})
}

// 刷新数据
const refresh = async () => {
  await fetchMembers()
  await calculateMemberStats()
  emit('refresh')
}

// 初始化
onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([
      fetchMembers(),
      fetchTasks(),
      fetchAvailableUsers()
    ])
    await calculateMemberStats()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.members-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: transparent;
  padding: 16px 20px 24px;
  box-sizing: border-box;
}

.members-view .search-card {
  margin-bottom: 16px;
}

.members-view .search-form {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.members-view .search-form-actions {
  margin-left: auto;
  margin-right: 0 !important;
}

.member-tags {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}

/* 成员网格：顶对齐，避免矮卡片被撑出底部空白 */
.members-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  align-content: start;
  align-items: start;
  overflow-y: auto;
  padding: 2px;
  min-height: 0;
}

.members-grid:empty {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 成员卡片 */
.member-card {
  width: 100%;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 16px;
  box-shadow: none;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.member-card:hover {
  border-color: #d6e4ff;
  box-shadow: 0 4px 14px rgba(24, 144, 255, 0.08);
  transform: none;
}

.member-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f5f5f5;
}

.member-avatar {
  flex-shrink: 0;
  background: #e6f4ff;
  color: #1677ff;
  font-weight: 600;
}

.member-basic {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f1f1f;
  margin-bottom: 2px;
  line-height: 1.4;
}

.member-email {
  font-size: 12px;
  color: #8c8c8c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 统计数据 */
.member-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.stat-item {
  text-align: center;
  padding: 10px 8px;
  background: #fafafa;
  border-radius: 8px;
}

.stat-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
  line-height: 1.2;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
  line-height: 1.2;
}

/* 贡献度 */
.member-contribution {
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 8px;
}

.contribution-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.contribution-label {
  font-size: 12px;
  color: #8c8c8c;
}

.contribution-score {
  font-size: 15px;
  font-weight: 600;
  color: #1677ff;
}

.contribution-progress {
  margin-bottom: 6px;
}

.contribution-stars {
  display: flex;
  gap: 2px;
}

.star {
  font-size: 14px;
  color: #d9d9d9;
  line-height: 1;
}

.star.active {
  color: #fadb14;
}

/* 操作按钮 */
.member-actions {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px 8px;
  padding-top: 10px;
  border-top: 1px solid #f5f5f5;
}

/* 成员详情对话框 */
.member-detail {
  max-height: 600px;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-section:last-child {
  border-bottom: none;
}

.section-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-item .label {
  font-size: 14px;
  color: #666;
  flex-shrink: 0;
}

.detail-item .value {
  font-size: 14px;
  color: #333;
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stat-card {
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
  text-align: center;
}

.stat-card .stat-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.stat-card .stat-value {
  font-size: 28px;
  font-weight: 600;
}

.stat-card .stat-value.primary {
  color: #1890ff;
}

.stat-card .stat-value.success {
  color: #52c41a;
}

.stat-card .stat-value.warning {
  color: #faad14;
}

/* 任务列表 */
.task-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #f9f9f9;
  border-radius: 4px;
}

.task-title {
  flex: 1;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-hours {
  font-size: 13px;
  color: #666;
  flex-shrink: 0;
}

/* 贡献度分析 */
.contribution-analysis {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.contribution-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.contribution-item .label {
  font-size: 14px;
  color: #666;
}

.contribution-item .value {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.contribution-item .stars {
  display: flex;
  gap: 2px;
}

/* 滚动条样式 */
.members-grid::-webkit-scrollbar,
.task-list::-webkit-scrollbar,
.member-detail::-webkit-scrollbar {
  width: 6px;
}

.members-grid::-webkit-scrollbar-thumb,
.task-list::-webkit-scrollbar-thumb,
.member-detail::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 3px;
}

.members-grid::-webkit-scrollbar-thumb:hover,
.task-list::-webkit-scrollbar-thumb:hover,
.member-detail::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>
