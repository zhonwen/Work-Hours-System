<template>
  <div class="project-gantt">
    <!-- 工具栏 -->
    <div class="gantt-toolbar">
      <div class="toolbar-left">
        <el-radio-group v-model="viewMode" @change="handleViewModeChange">
          <el-radio-button value="day">日视图</el-radio-button>
          <el-radio-button value="week">周视图</el-radio-button>
          <el-radio-button value="month">月视图</el-radio-button>
        </el-radio-group>
      </div>
      <div class="toolbar-right">
        <el-button @click="handleRefresh" type="primary" :icon="Refresh">刷新</el-button>
        <!-- <el-button @click="handleExport" :icon="Download">导出</el-button> -->
      </div>
    </div>

    <!-- 甘特图容器 -->
    <div ref="ganttContainer" class="gantt-container"></div>

    <!-- 加载状态 -->
    <div v-if="loading" class="gantt-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Download, Loading } from '@element-plus/icons-vue'
import { gantt } from 'dhtmlx-gantt'
import 'dhtmlx-gantt/codebase/dhtmlxgantt.css'
import { getProjectGanttData, updateTaskDates, updateTaskParent, type GanttTask } from '@/api/gantt'
import {
  getTaskStatusInfo,
  getProjectStatusInfo,
  getIterationStatusInfo
} from '@/utils/statusMapping'

interface Props {
  projectId: number
}

const props = defineProps<Props>()
const router = useRouter()

// 视图模式
const viewMode = ref('day')
const loading = ref(false)
const ganttContainer = ref<HTMLElement>()

/** 类型底色（对齐设计稿语义主色） */
const TYPE_BASE_COLOR: Record<string, string> = {
  project: '#3370fe',   // 主色蓝
  iteration: '#8b5cf6', // 迭代紫
  task: '#2ea44f'       // 任务绿
}

/** 语义色 */
const SEMANTIC = {
  primary: '#3370fe',
  success: '#2ea44f',
  warning: '#d4790c',
  danger: '#d83931',
  muted: '#8f959e'
}

/**
 * 条颜色：类型定底色 → 优先级覆盖 → 状态再覆盖（终态优先）
 */
const getBarColor = (task: { type?: string; priority?: string; status?: string }): string => {
  const type = task.type || 'task'
  let color = TYPE_BASE_COLOR[type] || TYPE_BASE_COLOR.task

  if (type !== 'task') return color

  // 优先级语义覆盖
  if (task.priority === 'HIGH') color = SEMANTIC.danger
  else if (task.priority === 'MEDIUM') color = SEMANTIC.primary
  else if (task.priority === 'LOW') color = SEMANTIC.success

  // 状态语义覆盖（终态/过程态优先于优先级）
  const status = task.status || ''
  if (status === 'DONE' || status === 'COMPLETED' || status === 'CLOSED') {
    color = SEMANTIC.success
  } else if (status === 'CANCELLED') {
    color = SEMANTIC.danger
  } else if (status === 'TESTING' || status === 'REOPENED') {
    color = SEMANTIC.warning
  } else if (status === 'IN_PROGRESS') {
    color = SEMANTIC.primary
  } else if (status === 'TODO') {
    // 待办：保留优先级色；无优先级时用中性灰
    if (!task.priority) color = SEMANTIC.muted
  }

  return color
}

/** 状态列中文文案 */
const getStatusLabel = (task: { type?: string; status?: string }): string => {
  const status = task.status
  if (!status) return '-'
  if (task.type === 'project') return getProjectStatusInfo(status).text
  if (task.type === 'iteration') return getIterationStatusInfo(status).text
  return getTaskStatusInfo(status).text
}

// 初始化甘特图
const initGantt = () => {
  // 设置中文语言包
  gantt.i18n.setLocale('cn')

  // 配置日期格式
  gantt.config.date_format = '%Y-%m-%d'
  gantt.config.xml_date = '%Y-%m-%d'

  // 启用树形结构，默认展开
  gantt.config.open_tree_initially = true
  gantt.config.order_branch = true
  gantt.config.order_branch_free = true
  gantt.config.show_grid = true
  gantt.config.show_links = false

  // 配置左侧列（类似 MS Project）
  gantt.config.columns = [
    {
      name: 'text',
      label: '任务名称',
      tree: true,
      width: 280,
      resize: true,
      template: (obj: any) => {
        // 所有类型都不显示图标，只显示文本
        return obj.text || ''
      }
    },
    { name: 'start_date', label: '开始日期', align: 'center', width: 100 },
    { name: 'duration', label: '工期(天)', align: 'center', width: 80 },
    { name: 'owner', label: '负责人', align: 'center', width: 90, template: (obj: any) => obj.owner || '-' },
    {
      name: 'status',
      label: '状态',
      align: 'center',
      width: 100,
      template: (obj: any) => getStatusLabel(obj)
    }
  ]

  // 启用拖拽编辑
  gantt.config.drag_links = false // 暂不启用依赖关系连线
  gantt.config.drag_progress = false
  gantt.config.drag_resize = true
  gantt.config.drag_move = true

  // 配置时间刻度（中文格式）；加宽日期列避免重叠
  gantt.config.scale_unit = 'day'
  gantt.config.date_scale = '%Y年%m月%d日'
  gantt.config.subscales = [
    { unit: 'day', step: 1, date: '%m月%d日 周%D' }
  ]
  gantt.config.min_column_width = 120
  gantt.config.scale_height = 56

  // 任务条 class：类型 + 优先级 + 状态（便于 CSS 兜底）
  gantt.templates.task_class = (_start: Date, _end: Date, task: any) => {
    const type = task.type || 'task'
    const parts = [`gantt_task_${type}`]
    if (task.priority) parts.push(`gantt_priority_${task.priority}`)
    if (task.status) parts.push(`gantt_status_${task.status}`)
    return parts.join(' ')
  }

  // 右侧时间轴任务条不显示文字（已在左侧显示）
  gantt.templates.task_text = () => ''

  gantt.templates.grid_row_class = (_start: Date, _end: Date, task: any) => {
    return `gantt_row_${task.type || 'task'}`
  }

  gantt.templates.task_row_class = (_start: Date, _end: Date, task: any) => {
    return `gantt_row_${task.type || 'task'}`
  }

  // 监听拖拽事件 - 更新任务时间
  gantt.attachEvent('onAfterTaskDrag', async (id: string, mode: string, e: Event) => {
    const task = gantt.getTask(id)
    const startDate = formatDate(task.start_date)
    const endDate = formatDate(task.end_date)
    const duration = task.duration

    // 计算结束日期（dhtmlx-gantt的end_date是任务最后一天的下一天）
    const actualEndDate = new Date(task.end_date)
    actualEndDate.setDate(actualEndDate.getDate() - 1)
    const formattedEndDate = formatDate(actualEndDate)

    try {
      await updateTaskDates(Number(id), {
        planStartDate: startDate,
        planEndDate: formattedEndDate
      })
      ElMessage.success('任务时间已更新')
    } catch (error: any) {
      ElMessage.error(error.message || '更新失败')
      // 恢复原位置
      loadGanttData()
    }
  })

  // 监听任务拖拽到新父节点
  gantt.attachEvent('onAfterTaskMove', async (id: string, parent: string, oldParent: string) => {
    if (parent === oldParent) return

    try {
      await updateTaskParent(Number(id), {
        parentId: parent === '0' || parent === null ? null : Number(parent)
      })
      ElMessage.success('任务层级已更新')
      loadGanttData()
    } catch (error: any) {
      ElMessage.error(error.message || '更新失败')
      loadGanttData()
    }
  })

  // 监听任务双击事件 - 使用 businessId 跳转（双击避免与拖拽冲突）
  gantt.attachEvent('onTaskDblClick', (id: string) => {
    const task = gantt.getTask(id)
    const businessId = task.businessId

    if (task.type === 'task') {
      router.push(`/tasks/${businessId}`)
    } else if (task.type === 'iteration') {
      router.push(`/iterations/${businessId}`)
    } else if (task.type === 'project') {
      router.push(`/projects/${businessId}`)
    }
    return true
  })

  // 初始化
  if (ganttContainer.value) {
    gantt.init(ganttContainer.value)
  }
}

// 加载甘特图数据
const loadGanttData = async () => {
  loading.value = true
  try {
    const data = await getProjectGanttData(props.projectId)

    // 转换任务数据格式 - 扁平化嵌套结构
    const ganttTasks: any[] = []
    const flattenTasks = (tasks: GanttTask[], parentId: number | null = null) => {
      tasks.forEach((task: GanttTask) => {
        const ganttTask = {
          id: task.ganttId,  // 使用 ganttId 给 dhtmlx-gantt 渲染
          businessId: task.id,  // 保留原始 id 用于显示和跳转
          text: task.text,
          type: task.type || 'task',
          start_date: task.startDate ? new Date(task.startDate) : null,
          duration: task.duration || 1,
          progress: task.progress || 0,
          parent: parentId || 0, // 使用父节点的 ganttId
          owner: task.owner,
          ownerId: task.ownerId,
          status: task.status,
          priority: task.priority,
          color: getBarColor(task),
          $virtual: false
        }
        ganttTasks.push(ganttTask)

        // 递归处理子任务，传递 ganttId 作为父 ID
        if (task.subtasks && task.subtasks.length > 0) {
          flattenTasks(task.subtasks, task.ganttId)
        }
      })
    }

    flattenTasks(data.data || [])

    const ganttLinks = (data.links || []).map((link: any) => ({
      id: link.id,
      source: link.source,
      target: link.target,
      type: link.type || '0'
    }))

    // 解析数据
    gantt.clearAll()
    gantt.parse({ data: ganttTasks, links: ganttLinks })

    // 自动调整时间范围
    adjustTimeScale()
  } catch (error: any) {
    ElMessage.error(error.message || '加载甘特图数据失败')
  } finally {
    loading.value = false
  }
}

// 格式化日期
const formatDate = (date: Date | string): string => {
  const d = typeof date === 'string' ? new Date(date) : date
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 调整时间刻度
const adjustTimeScale = () => {
  const task = gantt.getTaskByTime()
  if (!task || task.length === 0) return

  // 根据任务时间范围自动调整刻度
  const dates = gantt.getSubtaskDates()
  const minDate = new Date(dates.start_date)
  const maxDate = new Date(dates.end_date)

  // 添加前后缓冲
  minDate.setDate(minDate.getDate() - 7)
  maxDate.setDate(maxDate.getDate() + 7)

  gantt.config.start_date = minDate
  gantt.config.end_date = maxDate
  gantt.render()
}

// 视图模式切换
const handleViewModeChange = () => {
  switch (viewMode.value) {
    case 'day':
      gantt.config.scale_unit = 'day'
      gantt.config.date_scale = '%Y年%m月%d日'
      gantt.config.subscales = [
        { unit: 'day', step: 1, date: '%m月%d日 周%D' }
      ]
      gantt.config.min_column_width = 120
      gantt.config.scale_height = 56
      break
    case 'week':
      gantt.config.scale_unit = 'week'
      gantt.config.date_scale = '%Y年 第%W周'
      gantt.config.subscales = [
        { unit: 'day', step: 1, date: '%m/%d 周%D' }
      ]
      gantt.config.min_column_width = 72
      gantt.config.scale_height = 56
      break
    case 'month':
      gantt.config.scale_unit = 'month'
      gantt.config.date_scale = '%Y年 %m月'
      gantt.config.subscales = [
        { unit: 'week', step: 1, date: '第%W周' },
        { unit: 'day', step: 1, date: '%d日' }
      ]
      gantt.config.min_column_width = 48
      gantt.config.scale_height = 56
      break
  }
  gantt.render()
}

// 刷新
const handleRefresh = () => {
  loadGanttData()
}

// 导出（导出为图片）
const handleExport = () => {
  // 生成 PNG 图片
  gantt.exportToPNG({
    name: `gantt-${props.projectId}-${Date.now()}.png`,
    header: `<style>
      .gantt_task_line { border: 1px solid #000; }
    </style>`
  })
}

// 生命周期
onMounted(() => {
  initGantt()
  loadGanttData()
})

onBeforeUnmount(() => {
  // 清理甘特图实例
  if (ganttContainer.value) {
    gantt.clearAll()
  }
})
</script>

<style scoped>
.project-gantt {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.gantt-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color);
}

.toolbar-left,
.toolbar-right {
  display: flex;
  gap: 12px;
}

.gantt-container {
  flex: 1;
  width: 100%;
  min-height: 400px;
  position: relative;
}

.gantt-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-secondary);
}

/* dhtmlx-gantt 自定义样式 */
:deep(.gantt_task_line) {
  border-radius: 4px;
}

/* 左侧表头 + 右侧时间轴表头统一，上下左右居中 */
:deep(.gantt_grid_scale),
:deep(.gantt_task_scale),
:deep(.gantt_scale_line) {
  background: var(--fill-1, #f7f8fa) !important;
  border-bottom: 1px solid var(--line-1, #e5e6eb) !important;
  color: var(--text-2, #646a73);
  font-size: 12px;
  font-weight: 500;
}

:deep(.gantt_grid_head_cell),
:deep(.gantt_scale_cell) {
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  background: var(--fill-1, #f7f8fa) !important;
  color: var(--text-2, #646a73) !important;
  border-right: 1px solid var(--line-1, #e5e6eb) !important;
  border-bottom: 1px solid var(--line-1, #e5e6eb) !important;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.2;
  text-align: center !important;
  vertical-align: middle;
  box-sizing: border-box;
  padding: 0 8px !important;
}

:deep(.gantt_grid_scale .gantt_grid_head_cell) {
  height: 100% !important;
}

:deep(.gantt_scale_line) {
  display: flex;
  align-items: stretch;
}

:deep(.gantt_scale_line .gantt_scale_cell) {
  height: 100% !important;
}

/* 确保折叠/展开按钮可见 */
:deep(.gantt_tree_icon) {
  width: 20px;
  height: 20px;
  display: inline-block;
  vertical-align: middle;
  cursor: pointer;
  margin-right: 4px;
}

:deep(.gantt_tree_icon.gantt_open) {
  background-position: 0 0;
}

:deep(.gantt_tree_icon.gantt_closed) {
  background-position: 0 -20px;
}

/* 确保任务名称文本垂直居中对齐 */
:deep(.gantt_tree_content) {
  display: flex;
  align-items: center;
  line-height: 1;
}

:deep(.gantt_cell) {
  display: flex;
  align-items: center;
}

/* 类型底色（task.color 未生效时的 CSS 兜底） */
:deep(.gantt_task_project) {
  background-color: #3370fe;
  border-color: #245bdb;
}

:deep(.gantt_task_iteration) {
  background-color: #8b5cf6;
  border-color: #7c3aed;
}

:deep(.gantt_task_task) {
  background-color: #2ea44f;
  border-color: #22863a;
}

/* 优先级语义覆盖 */
:deep(.gantt_priority_HIGH) {
  background-color: #d83931 !important;
  border-color: #b42318 !important;
}

:deep(.gantt_priority_MEDIUM) {
  background-color: #3370fe !important;
  border-color: #245bdb !important;
}

:deep(.gantt_priority_LOW) {
  background-color: #2ea44f !important;
  border-color: #22863a !important;
}

/* 状态语义覆盖（高于优先级） */
:deep(.gantt_status_DONE),
:deep(.gantt_status_COMPLETED),
:deep(.gantt_status_CLOSED) {
  background-color: #2ea44f !important;
  border-color: #22863a !important;
}

:deep(.gantt_status_CANCELLED) {
  background-color: #d83931 !important;
  border-color: #b42318 !important;
}

:deep(.gantt_status_TESTING),
:deep(.gantt_status_REOPENED) {
  background-color: #d4790c !important;
  border-color: #b45309 !important;
}

:deep(.gantt_status_IN_PROGRESS) {
  background-color: #3370fe !important;
  border-color: #245bdb !important;
}

:deep(.gantt_row_project) {
  background-color: var(--fill-1, #f7f8fa);
  font-weight: 600;
}

:deep(.gantt_row_iteration) {
  background-color: var(--fill-1, #f7f8fa);
}

:deep(.gantt_row_task) {
  background-color: var(--surface, #ffffff);
}

/* 行 hover */
:deep(.gantt_grid_data .gantt_row.odd:hover),
:deep(.gantt_grid_data .gantt_row:hover),
:deep(.gantt_task_row:hover),
:deep(.gantt_row:hover) {
  background-color: #eaf3ff !important;
}

:deep(.gantt_task_row.gantt_selected),
:deep(.gantt_row.gantt_selected) {
  background-color: #eaf3ff !important;
}
</style>
