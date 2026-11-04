<template>
  <div class="page-root">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">工时日历</h2>
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
              <el-icon><List /></el-icon>
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
              <el-icon><Folder /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-label">总计工时</div>
              <div class="stats-value">{{ statistics.totalHours || 0 }} 小时</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 工具栏（仅浏览，无填报） -->
    <div class="calendar-toolbar">
      <div class="toolbar-left">
        <el-button-group>
          <el-button :icon="ArrowLeft" @click="handlePrevMonth">上个月</el-button>
          <el-button @click="handleToday">今天</el-button>
          <el-button :icon="ArrowRight" @click="handleNextMonth">下个月</el-button>
        </el-button-group>
        <div class="current-month">{{ currentMonthText }}</div>
      </div>
    </div>

    <!-- 日历网格 -->
    <div class="month-view" v-loading="loading">
      <div class="weekdays">
        <div v-for="day in weekdays" :key="day" class="weekday">{{ day }}</div>
      </div>
      <div class="calendar-grid">
        <div
          v-for="cell in calendarCells"
          :key="cell.date"
          class="calendar-cell"
          :class="{
            today: cell.isToday,
            weekend: cell.isWeekend,
            'other-month': cell.isOtherMonth,
            'has-data': cell.hasData
          }"
          @click="handleCellClick(cell)"
        >
          <div class="cell-header">
            <span class="date-number">{{ cell.dayNumber }}</span>
            <span v-if="cell.totalHours > 0" class="total-hours">
              <el-icon><Clock /></el-icon>
              {{ cell.totalHours }}h
            </span>
          </div>
          <div class="cell-body">
            <div v-if="cell.workHours.length === 0" class="empty-state">
              {{ cell.isOtherMonth ? '' : '无工时' }}
            </div>
            <div v-else class="work-hours-list">
              <div
                v-for="wh in cell.workHours.slice(0, 3)"
                :key="wh.id"
                class="work-hour-item"
              >
                <div class="wh-hours">{{ wh.hours }}h {{ wh.taskName || wh.projectName || '' }}</div>
              </div>
              <div v-if="cell.workHours.length > 3" class="more-hint">
                +{{ cell.workHours.length - 3 }} 条
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 当日工时只读明细 -->
    <el-dialog
      v-model="detailVisible"
      :title="`工时明细 — ${detailDate}`"
      width="640px"
      destroy-on-close
    >
      <div v-if="detailItems.length === 0" class="detail-empty">
        <el-empty description="当日无工时记录" :image-size="80" />
      </div>
      <template v-else>
        <div class="detail-summary">
          共 <strong>{{ detailItems.length }}</strong> 条，合计
          <strong>{{ detailTotalHours }}</strong> 小时
        </div>
        <el-table :data="detailItems" border stripe size="small">
          <el-table-column prop="projectName" label="项目" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.projectName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="taskName" label="任务" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.taskName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="content" label="工作内容" min-width="160" show-overflow-tooltip />
          <el-table-column prop="hours" label="工时" width="80" align="right">
            <template #default="{ row }">
              {{ row.hours }}h
            </template>
          </el-table-column>
        </el-table>
      </template>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { Clock, Calendar, List, Folder, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { getMonthRange, getCurrentYearMonth, getMonthText, formatDate, isToday, isWeekend } from '@/utils/dateUtils'
import { getWorkHourList, getUserWorkHourStatistics, type WorkHourInfo } from '@/api/workhour'
import { useAuthStore } from '@/stores/auth'
import { message } from '@/utils/message'

const authStore = useAuthStore()

const detailVisible = ref(false)
const detailDate = ref('')
const detailItems = ref<WorkHourInfo[]>([])
const detailTotalHours = computed(() =>
  detailItems.value.reduce((sum, item) => sum + (item.hours || 0), 0)
)

const { year: initYear, month: initMonth } = getCurrentYearMonth()
const currentYear = ref(initYear)
const currentMonth = ref(initMonth)
const currentMonthText = computed(() => getMonthText(currentYear.value, currentMonth.value))
const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const loading = ref(false)

interface CalendarCell {
  date: string
  dayNumber: number
  isToday: boolean
  isWeekend: boolean
  isOtherMonth: boolean
  totalHours: number
  hasData: boolean
  workHours: WorkHourInfo[]
}

interface DayWorkHours {
  date: string
  totalHours: number
  workHours: WorkHourInfo[]
}

const monthData = ref<Map<string, DayWorkHours>>(new Map())

const calendarCells = computed<CalendarCell[]>(() => {
  const cells: CalendarCell[] = []
  const year = currentYear.value
  const month = currentMonth.value

  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)

  let dayOfWeek = firstDay.getDay()
  dayOfWeek = dayOfWeek === 0 ? 7 : dayOfWeek

  const prevMonthLastDay = new Date(year, month - 1, 0)
  for (let i = dayOfWeek - 1; i > 0; i--) {
    const date = new Date(prevMonthLastDay)
    date.setDate(prevMonthLastDay.getDate() - i + 1)
    const dateStr = formatDate(date)
    cells.push({
      date: dateStr,
      dayNumber: date.getDate(),
      isToday: isToday(dateStr),
      isWeekend: isWeekend(dateStr),
      isOtherMonth: true,
      totalHours: 0,
      hasData: false,
      workHours: []
    })
  }

  for (let day = 1; day <= lastDay.getDate(); day++) {
    const date = new Date(year, month - 1, day)
    const dateStr = formatDate(date)
    const dayData = monthData.value.get(dateStr)
    cells.push({
      date: dateStr,
      dayNumber: day,
      isToday: isToday(dateStr),
      isWeekend: isWeekend(dateStr),
      isOtherMonth: false,
      totalHours: dayData?.totalHours || 0,
      hasData: !!dayData?.workHours?.length,
      workHours: dayData?.workHours || []
    })
  }

  const remainder = cells.length % 7
  if (remainder !== 0) {
    const remaining = 7 - remainder
    for (let i = 1; i <= remaining; i++) {
      const date = new Date(year, month, i)
      const dateStr = formatDate(date)
      cells.push({
        date: dateStr,
        dayNumber: i,
        isToday: isToday(dateStr),
        isWeekend: isWeekend(dateStr),
        isOtherMonth: true,
        totalHours: 0,
        hasData: false,
        workHours: []
      })
    }
  }

  return cells
})

const statistics = reactive({
  todayHours: 0,
  weekHours: 0,
  monthHours: 0,
  totalHours: 0
})

const fetchMonthData = async () => {
  loading.value = true
  try {
    const { start, end } = getMonthRange(currentYear.value, currentMonth.value)
    const res = await getWorkHourList({
      startDate: start,
      endDate: end,
      pageNum: 1,
      pageSize: 1000
    })

    const map = new Map<string, DayWorkHours>()
    const workHours = res?.list || []

    workHours.forEach((wh: WorkHourInfo) => {
      if (!map.has(wh.workDate)) {
        map.set(wh.workDate, {
          date: wh.workDate,
          totalHours: 0,
          workHours: []
        })
      }
      const day = map.get(wh.workDate)!
      day.totalHours += wh.hours
      day.workHours.push(wh)
    })

    monthData.value = map
    await updateStatistics(workHours)
  } catch (error) {
    console.error('获取工时数据失败:', error)
    message.error('获取工时数据失败')
  } finally {
    loading.value = false
  }
}

const updateStatistics = async (allWorkHours: WorkHourInfo[]) => {
  try {
    const today = new Date()
    const todayStr = formatDate(today)

    statistics.todayHours = allWorkHours
      .filter(wh => wh.workDate === todayStr)
      .reduce((sum, wh) => sum + wh.hours, 0)

    const weekStart = new Date(today)
    const day = today.getDay()
    weekStart.setDate(today.getDate() - (day === 0 ? 6 : day - 1))
    const weekStartStr = formatDate(weekStart)
    statistics.weekHours = allWorkHours
      .filter(wh => wh.workDate >= weekStartStr && wh.workDate <= todayStr)
      .reduce((sum, wh) => sum + wh.hours, 0)

    const monthStartStr = formatDate(new Date(today.getFullYear(), today.getMonth(), 1))
    statistics.monthHours = allWorkHours
      .filter(wh => wh.workDate >= monthStartStr && wh.workDate <= todayStr)
      .reduce((sum, wh) => sum + wh.hours, 0)

    const currentUserId = authStore.getCurrentUserId()
    if (currentUserId) {
      const totalRes = await getUserWorkHourStatistics(currentUserId)
      statistics.totalHours = (totalRes as { totalHours?: number })?.totalHours || 0
    } else {
      statistics.totalHours = allWorkHours.reduce((sum, wh) => sum + wh.hours, 0)
    }
  } catch (error) {
    console.error('更新统计数据失败:', error)
    statistics.totalHours = allWorkHours.reduce((sum, wh) => sum + wh.hours, 0)
  }
}

const handleCellClick = (cell: CalendarCell) => {
  if (cell.isOtherMonth) return
  detailDate.value = cell.date
  detailItems.value = [...cell.workHours]
  detailVisible.value = true
}

const handleToday = () => {
  const { year, month } = getCurrentYearMonth()
  currentYear.value = year
  currentMonth.value = month
}

const handlePrevMonth = () => {
  if (currentMonth.value === 1) {
    currentYear.value--
    currentMonth.value = 12
  } else {
    currentMonth.value--
  }
}

const handleNextMonth = () => {
  if (currentMonth.value === 12) {
    currentYear.value++
    currentMonth.value = 1
  } else {
    currentMonth.value++
  }
}

watch([currentYear, currentMonth], () => {
  fetchMonthData()
})

onMounted(() => {
  fetchMonthData()
})
</script>

<style scoped>
.calendar-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.current-month {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  min-width: 100px;
}

.month-view {
  background: #fff;
  padding: 16px;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 1px;
  margin-bottom: 8px;
}

.weekday {
  text-align: center;
  padding: 12px;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  background: #f5f5f5;
  border-radius: 4px;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.calendar-cell {
  min-height: 120px;
  padding: 8px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  cursor: pointer;
  transition: transform 0.2s, opacity 0.2s, border-color 0.2s, background-color 0.2s;
}

.calendar-cell:hover:not(.other-month) {
  box-shadow: 0 0 0 2px #1890ff inset;
}

.calendar-cell.weekend:not(.other-month) {
  background: #e6f7ff;
}

.calendar-cell.other-month {
  background: #f5f5f5;
  opacity: 0.6;
  cursor: default;
}

.calendar-cell.other-month.weekend {
  background: #f5f5f5;
  opacity: 0.6;
}

.calendar-cell.today {
  border: 2px solid #1890ff;
  box-shadow: 0 0 0 1px #1890ff;
}

.calendar-cell.has-data:not(.other-month):not(.weekend) {
  background: #f6ffed;
}

.calendar-cell.has-data.weekend:not(.other-month) {
  background: #d9f7be;
}

.cell-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.date-number {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.total-hours {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #1890ff;
  font-weight: 500;
}

.cell-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
  color: #c0c4cc;
  font-size: 12px;
}

.work-hours-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.work-hour-item {
  padding: 4px 6px;
  background: #f5f5f5;
  border-radius: 3px;
  font-size: 12px;
}

.wh-hours {
  color: #666;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.more-hint {
  font-size: 11px;
  color: #909399;
  padding-left: 4px;
}

.detail-summary {
  margin-bottom: 12px;
  font-size: 14px;
  color: #606266;
}

.detail-empty {
  padding: 12px 0;
}
</style>
