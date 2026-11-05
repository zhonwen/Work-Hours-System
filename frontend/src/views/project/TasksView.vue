<template>
  <div class="tasks-view">
    <!-- 传统列表：已移除左侧迭代树、看板/树视图切换 -->
    <div class="tasks-layout">
      <div class="work-items-column">
        <div class="toolbar search-form">
          <div class="toolbar-left">
            <el-form-item class="toolbar-field">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索工作项..."
                :prefix-icon="Search"
                clearable
                style="width: 240px"
                @input="handleSearch"
              />
            </el-form-item>
            <el-form-item class="toolbar-field">
              <el-select
                v-model="selectedTaskType"
                placeholder="类型"
                clearable
                style="width: 120px"
                @change="handleFilterChange"
              >
                <el-option label="全部" :value="undefined" />
                <el-option label="任务" value="TASK" />
                <el-option label="需求" value="REQUIREMENT" />
                <el-option label="缺陷" value="BUG" />
              </el-select>
            </el-form-item>
          </div>
          <div class="toolbar-right">
            <el-button type="primary" :icon="Plus" @click="handleCreateTask">
              新建工作项
            </el-button>
          </div>
        </div>

        <div class="list-view">
          <el-table
            :data="treeTableData"
            class="tasks-table"
            stripe
            border
            highlight-current-row
            :current-row-key="selectedTaskId"
            row-key="id"
            :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
            :row-class-name="tableRowClassName"
            @row-click="handleSelectTask"
          >
            <!-- 展开箭头独占首列，对齐首页待办树表 -->
            <el-table-column width="44" class-name="expand-col" />
            <el-table-column label="序号" width="64" class-name="index-col" align="center">
              <template #default="{ row }">
                <span class="task-index" :class="{ 'is-subtask': row.isSubtask }">{{ row.indexLabel }}</span>
              </template>
            </el-table-column>
            <el-table-column label="标题" min-width="240" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="task-title-cell" :class="{ 'is-subtask': row.isSubtask }">
                  <span v-if="row.isSubtask" class="subtask-branch" aria-hidden="true" />
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
            <el-table-column prop="type" label="类型" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="getTaskTypeTag(row.type)" size="small">
                  {{ getTaskTypeText(row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="预估工时" width="100" align="center">
              <template #default="{ row }">
                <span :class="{ 'estimate-empty': row.estimateHours == null }">
                  {{ formatEstimateHours(row.estimateHours) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="计划时间" width="200" align="center">
              <template #default="{ row }">
                <span v-if="row.planStartDate || row.planEndDate" class="plan-date-text">
                  {{ formatPlanRange(row.planStartDate, row.planEndDate) }}
                </span>
                <span v-else class="estimate-empty">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getStatusTag(row.status)" size="small">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="assigneeName" label="负责人" width="120" show-overflow-tooltip />
            <el-table-column prop="priority" label="优先级" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="getPriorityTag(row.priority)" size="small">
                  {{ getPriorityText(row.priority) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="handleEditTask(row)">编辑</el-button>
                <el-button link type="danger" @click.stop="handleDeleteTask(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 右栏：详情面板（含父子级信息，分割线区分） -->
      <div class="right-column detail-panel-column">
        <div v-if="selectedTask" class="detail-panel">
          <div class="panel-header">
            <div class="task-number">{{ selectedTask.id }}</div>
            <el-button text :icon="Close" @click="clearSelection" />
          </div>

          <div class="panel-body">
            <div class="detail-header">
              <h2 class="task-title">{{ selectedTask.title }}</h2>
              <el-dropdown trigger="click" @command="handleStatusChange">
                <button
                  type="button"
                  class="status-trigger"
                  :class="`status-${(selectedTask.status || 'TODO').toLowerCase()}`"
                >
                  <span class="status-dot" />
                  <span>{{ getStatusText(selectedTask.status) }}</span>
                  <el-icon class="status-arrow"><ArrowDown /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="TODO">待办</el-dropdown-item>
                    <el-dropdown-item command="IN_PROGRESS">进行中</el-dropdown-item>
                    <el-dropdown-item command="DONE">已完成</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <div class="detail-section assignee-section">
              <div class="section-label">负责人</div>
              <div class="assignee-chip" :class="{ empty: !selectedTask.assigneeName }">
                <el-avatar :size="28" class="assignee-avatar" v-if="selectedTask.assigneeName">
                  {{ selectedTask.assigneeName?.charAt(0) }}
                </el-avatar>
                <el-avatar :size="28" class="assignee-avatar empty-avatar" v-else>?</el-avatar>
                <span class="assignee-name">{{ selectedTask.assigneeName || '未分配' }}</span>
              </div>
            </div>

            <div class="detail-section">
              <div class="section-label">描述</div>
              <div class="section-value description">
                {{ selectedTask.description || '暂无描述' }}
              </div>
            </div>

            <div class="detail-section">
              <div class="section-label">工时</div>
              <div class="section-value">
                <div class="hours-info">
                  <span>预估：{{ formatEstimateHours(selectedTask.estimateHours) }}小时</span>
                  <span>实际：{{ actualHours }}小时</span>
                </div>
              </div>
            </div>

            <div class="detail-section">
              <div class="section-label">计划时间</div>
              <div class="section-value">
                {{ formatPlanRange(selectedTask.planStartDate, selectedTask.planEndDate) }}
              </div>
            </div>

            <div class="detail-section">
              <div class="section-label">元数据</div>
              <div class="section-value metadata">
                <div>创建时间：{{ selectedTask.createTime }}</div>
                <div>更新时间：{{ selectedTask.updateTime }}</div>
              </div>
            </div>

            <!-- 父级任务 -->
            <template v-if="selectedParentTask">
              <el-divider content-position="left">父级任务</el-divider>
              <div
                class="relation-card"
                @click="handleSelectTask(selectedParentTask)"
              >
                <div class="relation-title">{{ selectedParentTask.title }}</div>
                <div class="relation-meta">
                  <el-tag :type="getStatusTag(selectedParentTask.status)" size="small">
                    {{ getStatusText(selectedParentTask.status) }}
                  </el-tag>
                  <el-tag :type="getTaskTypeTag(selectedParentTask.type)" size="small">
                    {{ getTaskTypeText(selectedParentTask.type) }}
                  </el-tag>
                  <span>{{ selectedParentTask.assigneeName || '未分配' }}</span>
                </div>
              </div>
            </template>

            <!-- 子任务列表 -->
            <template v-if="selectedChildTasks.length">
              <el-divider content-position="left">
                子任务（{{ selectedChildTasks.length }}）
              </el-divider>
              <div
                v-for="child in selectedChildTasks"
                :key="child.id"
                class="relation-card"
                @click="handleSelectTask(child)"
              >
                <div class="relation-title">{{ child.title }}</div>
                <div class="relation-meta">
                  <el-tag :type="getStatusTag(child.status)" size="small">
                    {{ getStatusText(child.status) }}
                  </el-tag>
                  <el-tag :type="getPriorityTag(child.priority)" size="small">
                    {{ getPriorityText(child.priority) }}
                  </el-tag>
                  <span>{{ child.assigneeName || '未分配' }}</span>
                  <span v-if="child.estimateHours != null">{{ child.estimateHours }}h</span>
                </div>
              </div>
            </template>
          </div>
        </div>
        <div v-else class="empty-detail">
          <el-empty description="选择一个工作项查看详情" :image-size="80" />
        </div>
      </div>
    </div>

    <!-- 新建工作项对话框 -->
    <el-dialog
      v-model="taskDialogVisible"
      :title="taskDialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="taskFormRef" :model="taskFormData" :rules="taskFormRules" label-width="100px">
        <el-form-item label="工作项类型" prop="type">
          <el-radio-group v-model="taskFormData.type">
            <el-radio value="REQUIREMENT">需求</el-radio>
            <el-radio value="TASK">任务</el-radio>
            <el-radio value="BUG">缺陷</el-radio>
          </el-radio-group>
        </el-form-item>
        <!-- TODO[迭代]: 所属迭代选择 — 后续开放 -->
        <el-form-item label="父级任务">
          <el-select
            v-model="taskFormData.parentId"
            placeholder="选填：选择父级任务"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="task in parentTaskOptions"
              :key="task.id"
              :label="`${task.title}（${getTaskTypeText(task.type)}）`"
              :value="task.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="taskFormData.title"
            placeholder="请输入标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="taskFormData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
          />
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="taskFormData.priority">
            <el-radio value="LOW">低</el-radio>
            <el-radio value="MEDIUM">中</el-radio>
            <el-radio value="HIGH">高</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="taskFormData.status">
            <el-radio value="TODO">待办</el-radio>
            <el-radio value="IN_PROGRESS">进行中</el-radio>
            <el-radio value="DONE">已完成</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="负责人">
          <el-select
            v-model="taskFormData.assigneeId"
            placeholder="请选择负责人"
            filterable
            clearable
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
        <el-form-item label="预估工时">
          <el-input-number
            v-model="taskFormData.estimateHours"
            :min="0"
            :max="999"
            :precision="1"
            placeholder="请输入预估工时"
            style="width: 100%"
          />
          <span style="margin-left: 10px; color: #999">小时</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTaskSubmit" :loading="taskSubmitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- TODO[迭代]: 新建迭代对话框 — 后续开放
    <el-dialog
      v-model="iterationDialogVisible"
      title="新建迭代"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="iterationFormRef" :model="iterationFormData" :rules="iterationFormRules" label-width="100px">
        <el-form-item label="迭代名称" prop="name">
          <el-input
            v-model="iterationFormData.name"
            placeholder="请输入迭代名称"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="迭代描述">
          <el-input
            v-model="iterationFormData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入迭代描述"
          />
        </el-form-item>
        <el-form-item label="计划开始时间">
          <el-date-picker
            v-model="iterationFormData.planStartDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="计划结束时间">
          <el-date-picker
            v-model="iterationFormData.planEndDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="iterationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleIterationSubmit" :loading="iterationSubmitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>
    -->
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Plus,
  Search,
  List,
  Grid,
  Share,
  Close,
  ArrowRight,
  ArrowDown
} from '@element-plus/icons-vue'
import { getTasksByProjectId, createTask, updateTask, deleteTask, updateTaskStatus, updateTaskIterationId, type TaskInfo, type TaskCreateReq } from '@/api/task'
import { getIterationList, createIteration, type IterationInfo } from '@/api/iteration'
import { getAllUsers, type UserInfo } from '@/api/user'
import { getProjectMembers } from '@/api/project'

const props = defineProps<{
  projectId: number
}>()

const route = useRoute()
const projectId = computed(() => props.projectId || Number(route.params.id))

// 数据定义
const tasks = ref<TaskInfo[]>([])
const iterations = ref<IterationInfo[]>([])
const availableUsers = ref<UserInfo[]>([])

// 视图模式
const viewMode = ref<'list' | 'kanban' | 'tree'>('list')

// 搜索和筛选
const searchKeyword = ref('')
const selectedTaskType = ref<string>()
const selectedIterationId = ref<number | null>(null)
const selectedRequirementId = ref<number | null>(null)
const selectedTaskId = ref<number>()
const selectedTask = ref<TaskInfo | null>(null)
const showStatusTags = ref(true) // 显示状态标签

// 拖拽状态
const draggedTask = ref<TaskInfo | null>(null)

// 新建工作项对话框
const taskDialogVisible = ref(false)
const taskDialogTitle = computed(() => '新建工作项')
const taskSubmitLoading = ref(false)
const taskFormRef = ref<FormInstance>()
const taskFormData = reactive<TaskCreateReq>({
  projectId: projectId.value,
  title: '',
  description: '',
  type: 'TASK',
  priority: 'MEDIUM',
  status: 'TODO',
  assigneeId: undefined,
  iterationId: undefined,
  parentId: undefined,
  estimateHours: undefined
})
const taskFormRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
}

// 新建迭代对话框
const iterationDialogVisible = ref(false)
const iterationSubmitLoading = ref(false)
const iterationFormRef = ref<FormInstance>()
const iterationFormData = reactive({
  projectId: projectId.value,
  name: '',
  description: '',
  planStartDate: '',
  planEndDate: ''
})
const iterationFormRules: FormRules = {
  name: [{ required: true, message: '请输入迭代名称', trigger: 'blur' }]
}

// 迭代列表数据结构（包含需求）
interface IterationNode {
  id: number
  name: string
  status: string
  taskCount: number
  expanded: boolean
  requirements: {
    id: number
    title: string
    taskCount: number
  }[]
}

// 迭代列表（包含需求）— 基于扁平任务，避免漏掉嵌套子任务
const iterationList = computed<IterationNode[]>(() => {
  const flat = flattenTasks(tasks.value)
  return iterations.value.map(iteration => {
    const requirements = flat
      .filter(t => t.iterationId === iteration.id && t.type === 'REQUIREMENT')
      .map(req => ({
        id: req.id,
        title: req.title,
        taskCount: flat.filter(t => t.parentId === req.id || t.id === req.id).length
      }))

    const iterationTasks = flat.filter(t => t.iterationId === iteration.id && !t.parentId)

    return {
      id: iteration.id,
      name: iteration.name,
      status: iteration.status,
      taskCount: iterationTasks.length,
      expanded: false,
      requirements
    }
  })
})

// 未分配的任务（没有迭代）
const unassignedTasks = computed(() => {
  return flattenTasks(tasks.value).filter(t => !t.iterationId)
})

/** 展平树形任务（含子任务） */
const flattenTasks = (list: TaskInfo[]): TaskInfo[] => {
  const result: TaskInfo[] = []
  for (const task of list) {
    result.push(task)
    if (task.subtasks?.length) {
      result.push(...flattenTasks(task.subtasks))
    }
  }
  return result
}

/** 全量扁平任务（含嵌套子任务） */
const allTasksFlat = computed(() => flattenTasks(tasks.value))

/** 过滤后的根任务（保留子任务树，对齐首页待办） */
const filteredRootTasks = computed(() => {
  let result = tasks.value

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(t => {
      const selfMatch =
        t.title.toLowerCase().includes(keyword) || t.id.toString().includes(keyword)
      const childMatch = (t.subtasks || []).some(
        c => c.title.toLowerCase().includes(keyword) || c.id.toString().includes(keyword)
      )
      return selfMatch || childMatch
    })
  }

  if (selectedTaskType.value) {
    result = result.filter(t => {
      if (t.type === selectedTaskType.value) return true
      return (t.subtasks || []).some(c => c.type === selectedTaskType.value)
    })
  }

  return result
})

/** 表格树行：序号 1 / 1.1，children 供 el-table 展开 */
interface TaskTreeRow extends TaskInfo {
  indexLabel: string
  isSubtask: boolean
  subtaskCount?: number
  children?: TaskTreeRow[]
}

const buildTreeTableRows = (roots: TaskInfo[]): TaskTreeRow[] => {
  return roots.map((task, parentIdx) => {
    const parentIndex = parentIdx + 1
    const subtasks = task.subtasks || []
    const children = subtasks.length
      ? subtasks.map((child, childIdx) => ({
          ...child,
          isSubtask: true,
          indexLabel: `${parentIndex}.${childIdx + 1}`,
          children: undefined
        }))
      : undefined

    return {
      ...task,
      isSubtask: false,
      subtaskCount: subtasks.length || undefined,
      indexLabel: String(parentIndex),
      children
    }
  })
}

const treeTableData = computed(() => buildTreeTableRows(filteredRootTasks.value))

/** 当前编辑中的任务 ID（新建为空） */
const editingTaskId = ref<number | undefined>()

/** 新建/编辑时可选父级任务：顶级任务，编辑时排除自身 */
const parentTaskOptions = computed(() => {
  return allTasksFlat.value.filter(task => {
    if (editingTaskId.value && task.id === editingTaskId.value) return false
    return !task.parentId
  })
})

const tableRowClassName = ({ row }: { row: TaskInfo & { isSubtask?: boolean } }) => {
  const classes: string[] = []
  if (row.id === selectedTaskId.value) classes.push('is-selected-row')
  if (row.isSubtask) classes.push('is-subtask-row')
  return classes.join(' ')
}

/** 预估工时展示（兼容 number / string） */
const formatEstimateHours = (hours: number | string | null | undefined) => {
  if (hours == null || hours === '') return '-'
  const n = Number(hours)
  if (Number.isNaN(n)) return '-'
  return (Math.round(n * 10) / 10).toString()
}

/** 计划时间区间 */
const formatPlanRange = (start?: string | null, end?: string | null) => {
  if (!start && !end) return '-'
  const s = start || '?'
  const e = end || '?'
  return s === e ? s : `${s} ~ ${e}`
}

/** 详情：父级任务 */
const selectedParentTask = computed(() => {
  if (!selectedTask.value?.parentId) return null
  return allTasksFlat.value.find(t => t.id === selectedTask.value!.parentId) || null
})

/** 详情：子任务列表 */
const selectedChildTasks = computed(() => {
  if (!selectedTask.value) return []
  if (selectedTask.value.subtasks?.length) return selectedTask.value.subtasks
  const root = tasks.value.find(t => t.id === selectedTask.value!.id)
  return root?.subtasks || []
})

/** 默认选中第一条（或保持当前选中仍在列表中） */
const ensureDefaultSelection = () => {
  const list = flattenTasks(filteredRootTasks.value)
  if (!list.length) {
    selectedTask.value = null
    selectedTaskId.value = undefined
    return
  }
  const stillExists =
    selectedTaskId.value != null && list.some(t => t.id === selectedTaskId.value)
  if (stillExists) {
    selectedTask.value = list.find(t => t.id === selectedTaskId.value) || list[0]
    return
  }
  // 默认选中第一个根任务
  handleSelectTask(filteredRootTasks.value[0] || list[0])
}

// 看板列定义
const kanbanColumns = computed(() => {
  return [
    {
      key: 'TODO',
      label: '待办',
      color: '#faad14',
      tasks: filteredRootTasks.value.filter(t => t.status === 'TODO')
    },
    {
      key: 'IN_PROGRESS',
      label: '进行中',
      color: '#1890ff',
      tasks: filteredRootTasks.value.filter(t => t.status === 'IN_PROGRESS')
    },
    {
      key: 'DONE',
      label: '已完成',
      color: '#52c41a',
      tasks: filteredRootTasks.value.filter(t => t.status === 'DONE')
    }
  ]
})

// 树形视图数据
const treeTasks = computed(() => {
  const requirements = allTasksFlat.value.filter(t => t.type === 'REQUIREMENT')
  return requirements.map(req => ({
    ...req,
    subtasks: allTasksFlat.value.filter(t => t.parentId === req.id)
  }))
})

// 实际工时（从工时记录计算）
const actualHours = computed(() => {
  if (!selectedTask.value?.estimateHours) return '-'
  // TODO: 从工时记录表获取实际工时
  const remaining = selectedTask.value.estimateHours - 0
  return remaining.toFixed(1)
})

// 获取数据（后端 /tasks/search 已返回带 subtasks 的树）
const fetchTasks = async () => {
  try {
    const res = await getTasksByProjectId(projectId.value, 1, 1000)
    tasks.value = res.list || []
    ensureDefaultSelection()
  } catch (error) {
    console.error('获取任务列表失败:', error)
  }
}

const fetchIterations = async () => {
  try {
    const res = await getIterationList({ projectId: projectId.value, pageNum: 1, pageSize: 100 })
    iterations.value = res.list || []
  } catch (error) {
    console.error('获取迭代列表失败:', error)
  }
}

const fetchAvailableUsers = async () => {
  try {
    const members = await getProjectMembers(projectId.value)
    if (members && members.length > 0) {
      availableUsers.value = members.map(m => ({
        id: m.userId,
        nickname: m.nickname,
        username: m.username || ''
      }))
    } else {
      const res = await getAllUsers()
      availableUsers.value = res.list || []
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

// 事件处理
const handleSearch = () => {
  // 搜索后校正选中项
  ensureDefaultSelection()
}

const handleFilterChange = () => {
  // 筛选后若当前选中不在结果中，回落到第一条
  ensureDefaultSelection()
}

// 选择未分配的任务
const handleSelectUnassigned = () => {
  selectedIterationId.value = null
  selectedRequirementId.value = null
}

// 选择迭代
const handleSelectIteration = (iterationId: number) => {
  selectedIterationId.value = iterationId
  selectedRequirementId.value = null
}

// 选择需求
const handleSelectRequirement = (requirement: { id: number; title: string }) => {
  selectedRequirementId.value = requirement.id
  // 自动展开所属迭代
  const iteration = iterationList.value.find(iter =>
    iter.requirements.some(req => req.id === requirement.id)
  )
  if (iteration) {
    selectedIterationId.value = iteration.id
  }
}

// 展开/折叠迭代
const toggleIterationExpand = (iteration: IterationNode) => {
  iteration.expanded = !iteration.expanded
}

// 全部展开
const expandAll = () => {
  iterationList.value.forEach(iter => {
    iter.expanded = true
  })
}

// 全部折叠
const collapseAll = () => {
  iterationList.value.forEach(iter => {
    iter.expanded = false
  })
}

// 获取迭代状态图标
const getIterationStatusIcon = (status: string) => {
  const icons: Record<string, any> = {
    'NOT_STARTED': Calendar,
    'IN_PROGRESS': VideoPlay,
    'COMPLETED': CircleCheck
  }
  return icons[status] || Calendar
}

// 获取迭代状态标签类型
const getIterationStatusTagType = (status: string) => {
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

const resolveTask = (task: TaskInfo): TaskInfo => {
  return allTasksFlat.value.find(t => t.id === task.id) || task
}

const handleSelectTask = (task: TaskInfo) => {
  const full = resolveTask(task)
  selectedTaskId.value = full.id
  selectedTask.value = full
}

const clearSelection = () => {
  selectedTask.value = null
  selectedTaskId.value = undefined
}

const handleCreateTask = () => {
  // 必须清空编辑态，否则提交会走 update 覆盖当前选中行
  editingTaskId.value = undefined
  Object.assign(taskFormData, {
    projectId: projectId.value,
    title: '',
    description: '',
    type: 'TASK',
    priority: 'MEDIUM',
    status: 'TODO',
    assigneeId: undefined,
    iterationId: undefined,
    parentId: undefined,
    estimateHours: undefined
  })
  taskDialogVisible.value = true
}

const handleEditTask = (task: TaskInfo) => {
  editingTaskId.value = task.id
  selectedTask.value = task
  selectedTaskId.value = task.id
  Object.assign(taskFormData, {
    projectId: projectId.value,
    title: task.title,
    description: task.description,
    type: task.type,
    priority: task.priority,
    status: task.status,
    assigneeId: task.assigneeId,
    iterationId: task.iterationId,
    parentId: task.parentId,
    estimateHours: task.estimateHours
  })
  taskDialogVisible.value = true
}

const handleDeleteTask = (task: TaskInfo) => {
  ElMessageBox.confirm(`确定要删除工作项 "${task.title}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await deleteTask(task.id)
        ElMessage.success('删除成功')
        fetchTasks()
      } catch (error) {
        console.error('删除失败:', error)
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

const handleTaskSubmit = async () => {
  if (!taskFormRef.value) return

  await taskFormRef.value.validate(async (valid) => {
    if (!valid) return

    taskSubmitLoading.value = true
    try {
      // 用 editingTaskId 区分新建/编辑，勿用 selectedTask（列表默认选中会导致新建变成覆盖更新）
      if (editingTaskId.value) {
        await updateTask({
          id: editingTaskId.value,
          ...taskFormData
        })
        ElMessage.success('更新成功')
      } else {
        await createTask({ ...taskFormData })
        ElMessage.success('创建成功')
      }
      taskDialogVisible.value = false
      editingTaskId.value = undefined
      await fetchTasks()
    } catch (error) {
      console.error('操作失败:', error)
      ElMessage.error('操作失败')
    } finally {
      taskSubmitLoading.value = false
    }
  })
}

const handleStatusChange = async (status: string) => {
  if (!selectedTask.value) return

  try {
    await updateTaskStatus({
      id: selectedTask.value.id,
      status
    })
    ElMessage.success('状态更新成功')
    fetchTasks()
    // 更新选中任务的状态
    if (selectedTask.value) {
      selectedTask.value.status = status
    }
  } catch (error) {
    console.error('状态更新失败:', error)
    ElMessage.error('状态更新失败')
  }
}

const handleCreateIteration = () => {
  Object.assign(iterationFormData, {
    projectId: projectId.value,
    name: '',
    description: '',
    planStartDate: '',
    planEndDate: ''
  })
  iterationDialogVisible.value = true
}

const handleIterationSubmit = async () => {
  if (!iterationFormRef.value) return

  await iterationFormRef.value.validate(async (valid) => {
    if (!valid) return

    iterationSubmitLoading.value = true
    try {
      await createIteration(iterationFormData)
      ElMessage.success('创建成功')
      iterationDialogVisible.value = false
      fetchIterations()
    } catch (error) {
      console.error('创建失败:', error)
      ElMessage.error('创建失败')
    } finally {
      iterationSubmitLoading.value = false
    }
  })
}

// 拖拽处理
const handleDragStart = (task: TaskInfo, event: DragEvent) => {
  draggedTask.value = task
  if (event.dataTransfer) {
    event.dataTransfer.setData('text/plain', task.id.toString())
    event.dataTransfer.effectAllowed = 'move'
  }
}

const handleDragOver = (event: DragEvent) => {
  event.preventDefault()
  ;(event.currentTarget as HTMLElement).classList.add('drag-over')
}

const handleDragLeave = (event: DragEvent) => {
  ;(event.currentTarget as HTMLElement).classList.remove('drag-over')
}

const handleDropToStatus = async (event: DragEvent, status: string) => {
  event.preventDefault()
  ;(event.currentTarget as HTMLElement).classList.remove('drag-over')

  if (draggedTask.value && draggedTask.value.status !== status) {
    try {
      await updateTaskStatus({
        id: draggedTask.value.id,
        status
      })
      ElMessage.success('状态更新成功')
      fetchTasks()
    } catch (error) {
      console.error('状态更新失败:', error)
      ElMessage.error('状态更新失败')
    }
  }

  draggedTask.value = null
}

const handleDropToIteration = async (event: DragEvent, iterationId: number) => {
  event.preventDefault()

  if (draggedTask.value) {
    try {
      await updateTaskIterationId(draggedTask.value.id, iterationId === 0 ? undefined : iterationId)
      ElMessage.success('迭代更新成功')
      fetchTasks()
    } catch (error) {
      console.error('迭代更新失败:', error)
      ElMessage.error('迭代更新失败')
    }
  }

  draggedTask.value = null
}

// 工具函数
const getTaskTypeTag = (type: string) => {
  const tags: Record<string, string> = {
    'REQUIREMENT': 'success',
    'TASK': 'primary',
    'BUG': 'danger'
  }
  return tags[type] || 'info'
}

const getTaskTypeText = (type: string) => {
  const texts: Record<string, string> = {
    'REQUIREMENT': '需求',
    'TASK': '任务',
    'BUG': '缺陷'
  }
  return texts[type] || '未知'
}

const getStatusTag = (status: string) => {
  const tags: Record<string, string> = {
    'TODO': 'info',
    'IN_PROGRESS': 'primary',
    'DONE': 'success'
  }
  return tags[status] || 'info'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    'TODO': '待办',
    'IN_PROGRESS': '进行中',
    'DONE': '已完成'
  }
  return texts[status] || '未知'
}

const getPriorityTag = (priority: string) => {
  const tags: Record<string, string> = {
    'LOW': 'info',
    'MEDIUM': '',
    'HIGH': 'warning'
  }
  return tags[priority] || 'info'
}

const getPriorityText = (priority: string) => {
  const texts: Record<string, string> = {
    'LOW': '低',
    'MEDIUM': '中',
    'HIGH': '高'
  }
  return texts[priority] || '未知'
}

const handleViewParent = (parentId: number) => {
  const parent = allTasksFlat.value.find(t => t.id === parentId)
  if (parent) {
    handleSelectTask(parent)
  }
}

// 快捷键支持
const handleKeydown = (event: KeyboardEvent) => {
  // N - 新建
  if (event.key === 'n' || event.key === 'N') {
    if (!taskDialogVisible.value) {
      handleCreateTask()
    }
  }
  // F - 搜索
  else if (event.key === 'f' || event.key === 'F') {
    if (!taskDialogVisible.value) {
      const searchInput = document.querySelector('.toolbar-left .el-input__inner') as HTMLInputElement
      searchInput?.focus()
    }
  }
  // Esc - 关闭详情面板
  else if (event.key === 'Escape') {
    if (selectedTask.value) {
      clearSelection()
    }
  }
}

// 生命周期
onMounted(() => {
  fetchTasks()
  fetchIterations()
  fetchAvailableUsers()
  document.addEventListener('keydown', handleKeydown)
})

// 清理事件监听
import { onUnmounted } from 'vue'
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.tasks-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 传统列表布局（已去掉左侧迭代树） */
.tasks-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
  gap: 0;
}

.work-items-column {
  flex: 1;
  min-width: 0;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 左栏：导航树（已废弃，保留样式避免历史引用报错） */
.left-column {
  display: none;
}

/* 树形工具栏 */
.tree-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}

.tree-toolbar h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.toolbar-actions {
  display: flex;
  gap: 4px;
}

/* 导航树容器 */
.navigation-tree {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

/* 迭代节点状态样式 */
.iteration-node.status-in-progress {
  border-left: 3px solid #1890ff;
}

.iteration-node.status-completed {
  border-left: 3px solid #52c41a;
}

.iteration-node.status-not-started {
  border-left: 3px solid #d9d9d9;
}

.iteration-node {
  padding: 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-bottom: 4px;
  border-left: 3px solid transparent;
}

.iteration-node:hover {
  background: #f5f5f5;
}

.iteration-node.active {
  background: #e6f7ff;
}

/* 状态图标 */
.status-icon {
  font-size: 16px;
  margin-right: 4px;
}

.status-in-progress .status-icon {
  color: #1890ff;
}

.status-completed .status-icon {
  color: #52c41a;
}

.status-not-started .status-icon {
  color: #d9d9d9;
}

/* 迭代头部 */
.iteration-header {
  display: flex;
  align-items: center;
  gap: 6px;
}

.iteration-name {
  flex: 1;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-tag {
  font-size: 11px;
  margin-right: 4px;
}

.task-badge {
  margin-left: auto;
}

/* 迭代内容（需求列表） */
.iteration-body {
  margin-top: 8px;
  padding-left: 20px;
}

/* 需求节点 */
.requirement-node {
  padding: 6px 8px;
  margin-bottom: 4px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.requirement-node:hover {
  background: #f5f5f5;
}

.requirement-node.active {
  background: #e6f7ff;
  color: #1890ff;
}

.requirement-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.requirement-icon {
  color: #52c41a;
  font-size: 14px;
}

.requirement-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #666;
}

.requirement-node.active .requirement-title {
  color: #1890ff;
}

/* 空需求提示 */
.empty-requirements {
  padding: 8px;
  text-align: center;
}

.empty-text {
  font-size: 12px;
  color: #999;
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

/* 中栏：工作项列表 */
.middle-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 0;
}

.toolbar.search-form {
  flex-wrap: nowrap;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-field {
  margin-right: 0 !important;
  margin-bottom: 0 !important;
}

.list-view,
.kanban-view,
.tree-view {
  flex: 1;
  overflow: auto;
  padding: 16px;
}

.tasks-table {
  width: 100%;
}

.tasks-table :deep(.expand-col .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.tasks-table :deep(.expand-col .el-table__expand-icon) {
  margin: 0;
  height: 20px;
  line-height: 20px;
}

/* 树缩进落在首列时压缩占位，标题列自行缩进展示层级 */
.tasks-table :deep(.expand-col .el-table__indent) {
  padding-left: 0 !important;
}

.tasks-table :deep(.expand-col .el-table__placeholder) {
  width: 0 !important;
}

.tasks-table :deep(.index-col .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-left: 4px;
  padding-right: 4px;
}

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

/* 子任务：缩进 + 层级引导线 */
.task-title-cell.is-subtask {
  color: #606266;
  padding-left: 8px;
}

.subtask-branch {
  flex-shrink: 0;
  width: 12px;
  height: 12px;
  margin-right: 2px;
  border-left: 2px solid #c0c4cc;
  border-bottom: 2px solid #c0c4cc;
  border-radius: 0 0 0 2px;
  box-sizing: border-box;
}

:deep(.tasks-table .is-subtask-row > td.el-table__cell) {
  background-color: #fafbfc;
}

:deep(.tasks-table .is-selected-row > td.el-table__cell) {
  background-color: #ecf5ff !important;
}

.estimate-empty {
  color: #c0c4cc;
}

.plan-date-text {
  font-size: 13px;
  color: #606266;
}

.relation-card {
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s;
}

.relation-card:hover {
  background: #f5f9ff;
  border-color: #c6e2ff;
}

.relation-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 8px;
  line-height: 1.4;
}

.relation-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #909399;
}

.task-number {
  color: #1890ff;
  font-family: 'Monaco', 'Consolas', monospace;
  font-weight: 500;
}

/* 看板视图 */
.kanban-column {
  background: #f5f5f5;
  border-radius: 8px;
  overflow: hidden;
  min-height: 400px;
  transition: all 0.3s;
}

.kanban-column.drag-over {
  background: #e6f7ff;
  box-shadow: 0 0 0 2px #1890ff inset;
}

.column-header {
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.column-title {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.column-badge {
  margin-left: auto;
}

.column-body {
  padding: 8px;
  min-height: 300px;
}

.task-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.task-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.task-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.task-card-title {
  font-size: 14px;
  color: #333;
  line-height: 1.4;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.task-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 右栏：详情面板 */
.right-column {
  width: 400px;
  min-width: 400px;
  background: #fff;
  border-left: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.detail-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.task-title {
  flex: 1;
  min-width: 0;
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f1f1f;
  line-height: 1.45;
  word-break: break-word;
}

/* 状态切换：更清晰的胶囊按钮 */
.status-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  height: 28px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  cursor: pointer;
  outline: none;
  transition: background-color 0.2s, border-color 0.2s;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-arrow {
  font-size: 12px;
  opacity: 0.7;
}

.status-todo {
  color: #595959;
  background: #f5f5f5;
  border-color: #e8e8e8;
}

.status-todo:hover {
  background: #eeeeee;
}

.status-in_progress {
  color: #1677ff;
  background: #e6f4ff;
  border-color: #91caff;
}

.status-in_progress:hover {
  background: #d6ebff;
}

.status-done {
  color: #389e0d;
  background: #f6ffed;
  border-color: #b7eb8f;
}

.status-done:hover {
  background: #edf9e4;
}

.detail-section {
  margin-bottom: 18px;
}

.section-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
  font-weight: 500;
  letter-spacing: 0.02em;
}

.section-value {
  font-size: 14px;
  color: #262626;
}

.section-value.description {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #595959;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 8px;
}

.section-value.metadata {
  font-size: 12px;
  color: #8c8c8c;
}

.section-value.metadata > div {
  margin-bottom: 4px;
}

.hours-info {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}

/* 负责人：紧凑芯片布局 */
.assignee-section .section-label {
  margin-bottom: 6px;
}

.assignee-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  padding: 6px 10px 6px 6px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 999px;
}

.assignee-chip.empty {
  color: #8c8c8c;
}

.assignee-avatar {
  flex-shrink: 0;
  background: #e6f4ff;
  color: #1677ff;
  font-size: 12px;
  font-weight: 600;
}

.assignee-avatar.empty-avatar {
  background: #f0f0f0;
  color: #8c8c8c;
}

.assignee-name {
  font-size: 13px;
  color: #262626;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.assignee-chip.empty .assignee-name {
  color: #8c8c8c;
}

.empty-text {
  color: #8c8c8c;
}

.empty-detail {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}

/* 滚动条样式 */
.iteration-tree::-webkit-scrollbar,
.column-body::-webkit-scrollbar,
.panel-body::-webkit-scrollbar {
  width: 6px;
}

.iteration-tree::-webkit-scrollbar-thumb,
.column-body::-webkit-scrollbar-thumb,
.panel-body::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 3px;
}

.iteration-tree::-webkit-scrollbar-thumb:hover,
.column-body::-webkit-scrollbar-thumb:hover,
.panel-body::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>
