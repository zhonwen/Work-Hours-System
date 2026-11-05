<template>
  <div class="page-root">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">我的工时</h2>
      </div>
      <div class="header-right">
        <el-button :icon="Plus" @click="handleCreateLeave">请假</el-button>
        <el-button type="primary" :icon="Plus" @click="handleCreate">工时填报</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon today">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-label">今日工时</div>
              <div class="stats-value">{{ statistics.todayHours || 0 }} 小时</div>
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
              <div class="stats-value">{{ statistics.weekHours || 0 }} 小时</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon month">
              <el-icon><DataLine /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-label">本月工时</div>
              <div class="stats-value">{{ statistics.monthHours || 0 }} 小时</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon total">
              <el-icon><Histogram /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-label">总计工时</div>
              <div class="stats-value">{{ statistics.totalHours || 0 }} 小时</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索筛选 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item v-if="canViewAllWorkHours" label="查看范围">
          <el-select
            v-model="queryForm.viewScope"
            style="width: 140px"
            @change="handleViewScopeChange"
          >
            <el-option label="仅自己" value="SELF" />
            <el-option label="全部" value="ALL" />
            <el-option label="指定人员" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="canViewAllWorkHours && queryForm.viewScope === 'USER'" label="人员">
          <el-select
            v-model="queryForm.userId"
            placeholder="选择人员"
            filterable
            clearable
            style="width: 180px"
            @change="handleQuery"
          >
            <el-option
              v-for="u in userOptions"
              :key="u.id"
              :label="u.nickname || u.username"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="项目">
          <el-select v-model="queryForm.projectId" placeholder="请选择项目" clearable filterable style="width: 200px" @change="handleQuery">
            <el-option
              v-for="project in projectList"
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
            value-format="YYYY-MM-DD"
            @change="handleDateRangeChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 工时记录列表 -->
    <el-card class="table-card">
      <el-table :data="workHourList" stripe v-loading="loading" border>
        <el-table-column
          type="index"
          label="序号"
          width="64"
          fixed="left"
          :index="indexMethod"
        />
        <el-table-column
          v-if="showUserColumn"
          label="人员"
          min-width="120"
          fixed="left"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.userName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="工作日期" width="120">
          <template #default="{ row }">
            {{ row.workDate }}
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.recordType === 'LEAVE' ? 'warning' : 'success'" size="small">
              {{ row.recordType === 'LEAVE' ? '请假' : '工时' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="项目/请假类型" min-width="160" :fixed="showUserColumn ? undefined : 'left'" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.recordType === 'LEAVE' ? (row.leaveTypeName || '-') : (row.projectName || '-') }}
          </template>
        </el-table-column>
        <el-table-column label="任务" width="160">
          <template #default="{ row }">
            {{ row.recordType === 'LEAVE' ? '-' : (row.taskName || '-') }}
          </template>
        </el-table-column>
        <el-table-column prop="hours" label="时长" width="100">
          <template #default="{ row }">
            {{ row.hours }} 小时
          </template>
        </el-table-column>
        <el-table-column prop="content" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="日期" prop="workDate">
          <el-date-picker
            v-model="formData.workDate"
            name="workDate"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <template v-if="formData.recordType === 'LEAVE'">
          <el-form-item label="请假类型" prop="leaveTypeId">
            <el-select v-model="formData.leaveTypeId" placeholder="请选择请假类型" style="width: 100%">
              <el-option
                v-for="lt in leaveTypeList"
                :key="lt.id"
                :label="lt.name"
                :value="lt.id"
              />
            </el-select>
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="项目" prop="projectId">
            <el-select v-model="formData.projectId" placeholder="请选择项目" filterable style="width: 100%" @change="handleProjectChange">
              <el-option
                v-for="project in projectList"
                :key="project.id"
                :label="project.name"
                :value="project.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="任务">
            <el-select v-model="formData.taskId" placeholder="请选择任务（可选）" clearable filterable style="width: 100%">
              <el-option
                v-for="task in taskList"
                :key="task.id"
                :label="task.title"
                :value="task.id"
              />
            </el-select>
          </el-form-item>
        </template>
        <el-form-item :label="formData.recordType === 'LEAVE' ? '时长' : '工时数'" prop="hours">
          <el-input-number v-model="formData.hours" :min="0.5" :max="24" :step="0.5" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="formData.recordType === 'LEAVE' ? '备注' : '工作内容'" :prop="formData.recordType === 'LEAVE' ? undefined : 'content'">
          <el-input
            v-model="formData.content"
            name="content"
            type="textarea"
            :rows="3"
            :placeholder="formData.recordType === 'LEAVE' ? '选填' : '请详细描述工作内容'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Edit, Delete, Clock, Calendar, DataLine, Histogram } from '@element-plus/icons-vue'
import {
  getWorkHourList,
  createWorkHour,
  updateWorkHour,
  deleteWorkHour,
  sumWorkHours,
  type WorkHourInfo,
  type WorkHourRecordType,
  type WorkHourViewScope
} from '@/api/workhour'
import { getProjectList } from '@/api/project'
import { getTaskList } from '@/api/task'
import { getAllUsers, type UserInfo } from '@/api/user'
import { getEnabledLeaveTypes, type LeaveTypeInfo } from '@/api/leaveType'
import { useAuthStore } from '@/stores/auth'
import { getWorkHourStatusInfo } from '@/utils/statusMapping'

const authStore = useAuthStore()
const canViewAllWorkHours = computed(() => authStore.hasPermission('WORKHOUR_VIEW_ALL'))

// 查询表单（默认仅自己）
const queryForm = reactive({
  viewScope: 'SELF' as WorkHourViewScope,
  userId: undefined as number | undefined,
  projectId: undefined as number | undefined,
  startDate: undefined as string | undefined,
  endDate: undefined as string | undefined
})

/** 查看全部或指定他人时显示「人员」列，避免缺主语 */
const showUserColumn = computed(
  () => canViewAllWorkHours.value && queryForm.viewScope !== 'SELF'
)

// 日期范围
const dateRange = ref<[string, string] | null>(null)

// 项目列表
const projectList = ref<any[]>([])
const userOptions = ref<UserInfo[]>([])

// 任务列表
const taskList = ref<any[]>([])

/** 列表/统计查询参数（与查看范围一致） */
const buildListQuery = (extra: Record<string, unknown> = {}) => {
  const scope = canViewAllWorkHours.value ? queryForm.viewScope : 'SELF'
  return {
    viewScope: scope,
    userId: scope === 'USER' ? queryForm.userId : undefined,
    projectId: queryForm.projectId,
    startDate: queryForm.startDate,
    endDate: queryForm.endDate,
    ...extra
  }
}

// 工时列表
const workHourList = ref<WorkHourInfo[]>([])
const loading = ref(false)

// 统计数据
const statistics = reactive({
  todayHours: 0,
  weekHours: 0,
  monthHours: 0,
  totalHours: 0
})

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

/** 表格序号（跨页连续） */
const indexMethod = (index: number) =>
  (pagination.pageNum - 1) * pagination.pageSize + index + 1

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const leaveTypeList = ref<LeaveTypeInfo[]>([])
const formData = reactive({
  id: undefined as number | undefined,
  workDate: '',
  projectId: undefined as number | undefined,
  taskId: undefined as number | undefined,
  hours: 8,
  content: '',
  recordType: 'NORMAL' as WorkHourRecordType,
  leaveTypeId: undefined as number | undefined
})

const formRules = computed<FormRules>(() => {
  const rules: FormRules = {
    workDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
    hours: [{ required: true, message: '请输入时长', trigger: 'blur' }]
  }
  if (formData.recordType === 'LEAVE') {
    rules.leaveTypeId = [{ required: true, message: '请选择请假类型', trigger: 'change' }]
  } else {
    rules.projectId = [{ required: true, message: '请选择项目', trigger: 'change' }]
    rules.content = [{ required: true, message: '请输入工作内容', trigger: 'blur' }]
  }
  return rules
})

// 获取工时状态信息
const getStatusType = (status: string) => getWorkHourStatusInfo(status).type
const getStatusText = (status: string) => getWorkHourStatusInfo(status).text

// 获取工时列表
const fetchWorkHours = async () => {
  if (queryForm.viewScope === 'USER' && !queryForm.userId) {
    workHourList.value = []
    pagination.total = 0
    return
  }
  loading.value = true
  try {
    const res = await getWorkHourList(
      buildListQuery({
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      })
    )
    workHourList.value = res.list || []
    pagination.total = res.total || 0
  } catch (error) {
    console.error('获取工时列表失败:', error)
    ElMessage.error('获取工时列表失败')
  } finally {
    loading.value = false
  }
}

// 顶部统计与当前查看范围一致（不含请假）；默认仅本人
const fetchStatistics = async () => {
  try {
    if (queryForm.viewScope === 'USER' && !queryForm.userId) {
      statistics.todayHours = 0
      statistics.weekHours = 0
      statistics.monthHours = 0
      statistics.totalHours = 0
      return
    }
    const today = new Date()
    const todayStr = today.toISOString().split('T')[0]
    const weekStart = new Date(today)
    const day = today.getDay()
    weekStart.setDate(today.getDate() - (day === 0 ? 6 : day - 1))
    const weekStartStr = weekStart.toISOString().split('T')[0]
    const monthStartStr = new Date(today.getFullYear(), today.getMonth(), 1)
      .toISOString()
      .split('T')[0]

    const res = await getWorkHourList(
      buildListQuery({
        projectId: undefined,
        startDate: undefined,
        endDate: undefined,
        pageNum: 1,
        pageSize: 10000
      })
    )
    const list = res?.list || []

    statistics.todayHours = sumWorkHours(list.filter(wh => wh.workDate === todayStr))
    statistics.weekHours = sumWorkHours(
      list.filter(wh => wh.workDate >= weekStartStr && wh.workDate <= todayStr)
    )
    statistics.monthHours = sumWorkHours(
      list.filter(wh => wh.workDate >= monthStartStr && wh.workDate <= todayStr)
    )
    statistics.totalHours = sumWorkHours(list)
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const fetchUsers = async () => {
  if (!canViewAllWorkHours.value) return
  try {
    const res: any = await getAllUsers()
    userOptions.value = res?.list || res || []
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

const handleViewScopeChange = () => {
  if (queryForm.viewScope !== 'USER') {
    queryForm.userId = undefined
  }
  handleQuery()
  fetchStatistics()
}

// 获取项目列表
const fetchProjects = async () => {
  try {
    const res = await getProjectList({
      pageNum: 1,
      pageSize: 1000
    })
    projectList.value = res.list || []
  } catch (error) {
    console.error('获取项目列表失败:', error)
  }
}

// 获取任务列表
const fetchTasks = async () => {
  if (!formData.projectId) {
    taskList.value = []
    return
  }
  try {
    const res = await getTaskList({
      projectId: formData.projectId,
      pageNum: 1,
      pageSize: 1000
    })
    taskList.value = res.list || []
  } catch (error) {
    console.error('获取任务列表失败:', error)
  }
}

// 日期范围变化
const handleDateRangeChange = (dates: [string, string] | null) => {
  if (dates && dates.length === 2) {
    queryForm.startDate = dates[0]
    queryForm.endDate = dates[1]
  } else {
    queryForm.startDate = undefined
    queryForm.endDate = undefined
  }
  handleQuery()
}

// 查询
const handleQuery = () => {
  pagination.pageNum = 1
  fetchWorkHours()
  fetchStatistics()
}

// 重置
const handleReset = () => {
  queryForm.viewScope = 'SELF'
  queryForm.userId = undefined
  queryForm.projectId = undefined
  queryForm.startDate = undefined
  queryForm.endDate = undefined
  dateRange.value = null
  pagination.pageNum = 1
  fetchWorkHours()
  fetchStatistics()
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  fetchWorkHours()
}

// 当前页变化
const handleCurrentChange = (page: number) => {
  pagination.pageNum = page
  fetchWorkHours()
}

// 创建
const resetFormBase = () => {
  const today = new Date().toISOString().split('T')[0]
  formData.id = undefined
  formData.workDate = today
  formData.projectId = undefined
  formData.taskId = undefined
  formData.hours = 8
  formData.content = ''
  formData.leaveTypeId = undefined
  taskList.value = []
}

const fetchLeaveTypes = async () => {
  try {
    leaveTypeList.value = (await getEnabledLeaveTypes()) || []
  } catch (error) {
    console.error('获取请假类型失败:', error)
  }
}

// 新建正常工时
const handleCreate = () => {
  dialogTitle.value = '工时填报'
  resetFormBase()
  formData.recordType = 'NORMAL'
  dialogVisible.value = true
}

// 新建请假
const handleCreateLeave = async () => {
  dialogTitle.value = '请假登记'
  resetFormBase()
  formData.recordType = 'LEAVE'
  formData.hours = 8
  if (leaveTypeList.value.length === 0) {
    await fetchLeaveTypes()
  }
  dialogVisible.value = true
}

// 编辑
const handleEdit = async (row: WorkHourInfo) => {
  const isLeave = row.recordType === 'LEAVE'
  dialogTitle.value = isLeave ? '编辑请假' : '编辑工时'
  formData.id = row.id
  formData.workDate = row.workDate
  formData.projectId = row.projectId
  formData.taskId = row.taskId
  formData.hours = row.hours
  formData.content = row.content || ''
  formData.recordType = isLeave ? 'LEAVE' : 'NORMAL'
  formData.leaveTypeId = row.leaveTypeId
  if (isLeave) {
    if (leaveTypeList.value.length === 0) await fetchLeaveTypes()
  } else {
    fetchTasks()
  }
  dialogVisible.value = true
}

// 项目变化时加载任务
const handleProjectChange = () => {
  formData.taskId = undefined
  fetchTasks()
}

// 提交
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (formData.id) {
          const payload = {
            id: formData.id,
            workDate: formData.workDate,
            hours: formData.hours,
            content: formData.content,
            recordType: formData.recordType,
            projectId: formData.recordType === 'LEAVE' ? undefined : formData.projectId,
            taskId: formData.recordType === 'LEAVE' ? undefined : formData.taskId,
            leaveTypeId: formData.recordType === 'LEAVE' ? formData.leaveTypeId : undefined
          }
          await updateWorkHour(payload)
          ElMessage.success('更新成功')
        } else {
          await createWorkHour({
            workDate: formData.workDate,
            hours: formData.hours,
            content: formData.content,
            recordType: formData.recordType,
            projectId: formData.recordType === 'LEAVE' ? undefined : formData.projectId,
            taskId: formData.recordType === 'LEAVE' ? undefined : formData.taskId,
            leaveTypeId: formData.recordType === 'LEAVE' ? formData.leaveTypeId : undefined
          })
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        fetchWorkHours()
        fetchStatistics()
      } catch (error) {
        console.error('操作失败:', error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 删除
const handleDelete = (row: WorkHourInfo) => {
  ElMessageBox.confirm(`确定要删除 ${row.workDate} 的工时记录吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await deleteWorkHour(row.id)
        ElMessage.success('删除成功')
        fetchWorkHours()
        fetchStatistics()
      } catch (error) {
        console.error('删除失败:', error)
      }
    })
    .catch(() => {})
}

onMounted(() => {
  fetchProjects()
  fetchUsers()
  fetchWorkHours()
  fetchStatistics()
  fetchLeaveTypes()
})
</script>

<style scoped>
/* ========== 工时页面特定样式 ========== */

/* 统计图标渐变色 */
.stats-icon.today {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.stats-icon.week {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: #fff;
}

.stats-icon.month {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: #fff;
}

.stats-icon.total {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  color: #fff;
}

/* 数字列等宽字体 */
:deep(.el-table .number-column) {
  font-variant-numeric: tabular-nums;
}
</style>
