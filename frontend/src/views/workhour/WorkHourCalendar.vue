<template>
  <div class="page-root">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">工时日历</h2>
    </div>

    <!-- 统计卡片：工时不含请假；本月请假单独展示 -->
    <div class="stats-row stats-row-5">
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
      <el-card class="stats-card stats-card-leave">
        <div class="stats-content">
          <div class="stats-icon leave">
            <el-icon><Calendar /></el-icon>
          </div>
          <div class="stats-info">
            <div class="stats-label">
              <span>本月请假</span>
              <div class="leave-detail-btn"  @click="openLeaveDetail">明细</div>
            </div>
            <div class="stats-value">{{ statistics.monthLeaveHours || 0 }} 小时</div>
          </div>
        </div>
      </el-card>
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
    </div>

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
                :class="{ leave: wh.recordType === 'LEAVE' }"
              >
                <div class="wh-hours">
                  {{ wh.hours }}h
                  {{
                    wh.recordType === 'LEAVE'
                      ? (wh.leaveTypeName || '请假')
                      : (wh.taskName || wh.projectName || '')
                  }}
                </div>
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
          共 <strong>{{ detailItems.length }}</strong> 条，工时
          <strong>{{ detailTotalHours }}</strong> 小时
          <template v-if="detailLeaveHours > 0">
            ，请假 <strong>{{ detailLeaveHours }}</strong> 小时
          </template>
        </div>
        <el-table :data="detailItems" border stripe size="small">
          <el-table-column label="类型" width="80">
            <template #default="{ row }">
              <el-tag :type="row.recordType === 'LEAVE' ? 'warning' : 'success'" size="small">
                {{ row.recordType === 'LEAVE' ? '请假' : '工时' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="项目/请假" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">
              {{
                row.recordType === 'LEAVE'
                  ? (row.leaveTypeName || '-')
                  : (row.projectName || '-')
              }}
            </template>
          </el-table-column>
          <el-table-column label="任务" min-width="100" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.recordType === 'LEAVE' ? '-' : (row.taskName || '-') }}
            </template>
          </el-table-column>
          <el-table-column prop="content" label="说明" min-width="140" show-overflow-tooltip />
          <el-table-column prop="hours" label="时长" width="80" align="right">
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

    <!-- 本月请假明细 -->
    <el-dialog
      v-model="leaveDetailVisible"
      :title="`本月请假明细 — ${currentMonthText}`"
      width="640px"
      destroy-on-close
    >
      <div v-if="monthLeaveItems.length === 0" class="detail-empty">
        <el-empty description="本月暂无请假记录" :image-size="80" />
      </div>
      <template v-else>
        <div class="detail-summary">
          共 <strong>{{ monthLeaveItems.length }}</strong> 条，合计
          <strong>{{ statistics.monthLeaveHours }}</strong> 小时
        </div>
        <el-table :data="monthLeaveItems" border stripe size="small">
          <el-table-column prop="workDate" label="日期" width="120" />
          <el-table-column label="请假类型" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.leaveTypeName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="content" label="备注" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.content || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="hours" label="时长" width="90" align="right">
            <template #default="{ row }">
              {{ row.hours }}h
            </template>
          </el-table-column>
        </el-table>
      </template>
      <template #footer>
        <el-button type="primary" @click="leaveDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { Clock, Calendar, List, Folder, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { getMonthRange, getCurrentYearMonth, getMonthText, formatDate, isToday, isWeekend } from '@/utils/dateUtils'
import { getWorkHourList, getUserWorkHourStatistics, isLeaveRecord, sumWorkHours, type WorkHourInfo } from '@/api/workhour'
import { useAuthStore } from '@/stores/auth'
import { message } from '@/utils/message'

const authStore = useAuthStore()

const detailVisible = ref(false)
const detailDate = ref('')
const detailItems = ref<WorkHourInfo[]>([])
const detailTotalHours = computed(() => sumWorkHours(detailItems.value))
const detailLeaveHours = computed(() =>
  detailItems.value
    .filter(item => isLeaveRecord(item))
    .reduce((sum, item) => sum + Number(item.hours || 0), 0)
)

const leaveDetailVisible = ref(false)
const monthLeaveItems = ref<WorkHourInfo[]>([])

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
  monthLeaveHours: 0,
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
      // 格子右上角合计仅统计正常工时
      if (!isLeaveRecord(wh)) {
        day.totalHours += Number(wh.hours || 0)
      }
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
    const workOnly = allWorkHours.filter(wh => !isLeaveRecord(wh))
    const leaveOnly = allWorkHours.filter(wh => isLeaveRecord(wh))

    // 本月请假：当前日历月份内全部请假（不计入工时卡）
    monthLeaveItems.value = [...leaveOnly].sort((a, b) =>
      a.workDate === b.workDate
        ? Number(b.hours || 0) - Number(a.hours || 0)
        : a.workDate.localeCompare(b.workDate)
    )
    statistics.monthLeaveHours = leaveOnly.reduce(
      (sum, wh) => sum + Number(wh.hours || 0),
      0
    )

    statistics.todayHours = sumWorkHours(workOnly.filter(wh => wh.workDate === todayStr))

    const weekStart = new Date(today)
    const day = today.getDay()
    weekStart.setDate(today.getDate() - (day === 0 ? 6 : day - 1))
    const weekStartStr = formatDate(weekStart)
    statistics.weekHours = sumWorkHours(
      workOnly.filter(wh => wh.workDate >= weekStartStr && wh.workDate <= todayStr)
    )

    // 本月工时：仅正常工时（当前视图月内、截至今天；翻月时按该月数据）
    const isCurrentMonth =
      currentYear.value === today.getFullYear() && currentMonth.value === today.getMonth() + 1
    statistics.monthHours = isCurrentMonth
      ? sumWorkHours(workOnly.filter(wh => wh.workDate <= todayStr))
      : sumWorkHours(workOnly)

    const currentUserId = authStore.getCurrentUserId()
    if (currentUserId) {
      const totalRes = await getUserWorkHourStatistics(currentUserId)
      statistics.totalHours = Number((totalRes as { totalHours?: number })?.totalHours || 0)
    } else {
      statistics.totalHours = sumWorkHours(workOnly)
    }
  } catch (error) {
    console.error('更新统计数据失败:', error)
    statistics.totalHours = sumWorkHours(allWorkHours)
  }
}

const openLeaveDetail = () => {
  leaveDetailVisible.value = true
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
.stats-row-5 {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

@media (max-width: 1200px) {
  .stats-row-5 {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .stats-row-5 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

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

.stats-icon.leave {
  background: linear-gradient(135deg, #ffb347 0%, #fa8c16 100%);
  color: #fff;
}

/* 请假卡：结构同其他卡，文字用橙色区分 */
.stats-card-leave .stats-label {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  /* color: #d46b08; */
}

.stats-card-leave .stats-value {
  color: red;
  opacity: 0.6;
}

.leave-detail-btn {
  padding: 0;
  height: auto;
  font-size: 12px;
  color: #fa8c16;
  margin-top: 2px;
  cursor: pointer;
}

.leave-detail-btn:hover {
  color: #d46b08;
}

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

.work-hour-item.leave {
  background: #fff7e6;
  border-left: 2px solid #fa8c16;
}

.work-hour-item.leave .wh-hours {
  color: #d46b08;
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
