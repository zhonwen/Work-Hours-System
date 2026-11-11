<template>
  <div class="page-root">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">{{ pageTitle }}</h2>
    </div>

    <!-- 统计条：轻卡细线 -->
    <div class="stats-row stats-row-5">
      <div class="stats-card">
        <div class="stats-label">今日工时</div>
        <div class="stats-value">{{ statistics.todayHours || 0 }}<span class="unit">小时</span></div>
      </div>
      <div class="stats-card">
        <div class="stats-label">本周工时</div>
        <div class="stats-value">{{ statistics.weekHours || 0 }}<span class="unit">小时</span></div>
      </div>
      <div class="stats-card">
        <div class="stats-label">本月工时</div>
        <div class="stats-value">{{ statistics.monthHours || 0 }}<span class="unit">小时</span></div>
      </div>
      <div class="stats-card stats-card-leave">
        <div class="stats-label">
          <span>本月请假</span>
          <span class="leave-detail-btn" @click.stop="openLeaveDetail">明细</span>
        </div>
        <div class="stats-value">{{ statistics.monthLeaveHours || 0 }}<span class="unit">小时</span></div>
      </div>
      <div class="stats-card">
        <div class="stats-label">累计工时</div>
        <div class="stats-value">{{ statistics.totalHours || 0 }}<span class="unit">小时</span></div>
      </div>
    </div>

    <!-- 日历面板：工具栏 + 网格 -->
    <div class="calendar-panel" v-loading="loading">
      <div class="calendar-toolbar">
        <div class="toolbar-left">
          <el-button size="small" :icon="ArrowLeft" @click="handlePrevMonth" />
          <el-button size="small" class="month-btn">{{ currentMonthText }}</el-button>
          <el-button size="small" :icon="ArrowRight" @click="handleNextMonth" />
          <el-button size="small" @click="handleToday">今天</el-button>
        </div>
        <div v-if="canViewAllWorkHours" class="toolbar-right">
          <el-select
            v-model="selectedUserId"
            filterable
            size="small"
            class="user-filter-select"
            @change="handleUserChange"
          >
            <el-option label="查看：我的工时" :value="0" />
            <el-option
              v-for="u in userOptions"
              :key="u.id"
              :label="`查看：${u.nickname || u.username}`"
              :value="u.id"
            />
          </el-select>
        </div>
      </div>

      <div class="month-view">
        <div class="calendar-weekdays">
          <div v-for="day in weekdays" :key="day" class="calendar-weekday">{{ day }}</div>
        </div>
        <div class="calendar-grid">
          <div
            v-for="cell in calendarCells"
            :key="cell.date"
            class="calendar-cell"
            :class="{
              'is-today': cell.isToday,
              'is-out': cell.isOtherMonth
            }"
            @click="handleCellClick(cell)"
          >
            <span class="calendar-date">{{ cell.dayNumber }}</span>
            <div
              v-for="wh in cell.workHours.slice(0, 3)"
              :key="wh.id"
              class="calendar-log"
              :class="{ leave: wh.recordType === 'LEAVE' }"
            >
              <span class="log-text">
                {{
                  wh.recordType === 'LEAVE'
                    ? (wh.leaveTypeName || '请假')
                    : (wh.taskName || wh.projectName || '工时')
                }}
              </span>
              <span class="hours">{{ wh.hours }}h</span>
            </div>
            <div v-if="cell.workHours.length > 3" class="more-hint">
              +{{ cell.workHours.length - 3 }} 条
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
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { getMonthRange, getCurrentYearMonth, getMonthText, formatDate, isToday, isWeekend } from '@/utils/dateUtils'
import {
  getWorkHourList,
  isLeaveRecord,
  sumWorkHours,
  type WorkHourInfo,
  type WorkHourViewScope
} from '@/api/workhour'
import { getAllUsers, type UserInfo } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { message } from '@/utils/message'

const authStore = useAuthStore()
const canViewAllWorkHours = computed(() => authStore.hasPermission('WORKHOUR_VIEW_ALL'))
/** 0 = 我自己；>0 = 一次只看该人 */
const selectedUserId = ref(0)
const userOptions = ref<UserInfo[]>([])

const viewingQuery = computed(() => {
  if (canViewAllWorkHours.value && selectedUserId.value > 0) {
    return { viewScope: 'USER' as WorkHourViewScope, userId: selectedUserId.value }
  }
  return { viewScope: 'SELF' as WorkHourViewScope }
})

const pageTitle = computed(() => {
  if (selectedUserId.value <= 0) return '我的工时日历'
  const u = userOptions.value.find(item => item.id === selectedUserId.value)
  const name = u?.nickname || u?.username || '该成员'
  return `${name}的工时日历`
})

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
    // 默认 SELF；有权限且选中某人时 USER（一次一人，不叠全员）
    const res = await getWorkHourList({
      ...viewingQuery.value,
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

    // 本月请假：当前日历月份内（当前查看对象）
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

    const isCurrentMonth =
      currentYear.value === today.getFullYear() && currentMonth.value === today.getMonth() + 1
    statistics.monthHours = isCurrentMonth
      ? sumWorkHours(workOnly.filter(wh => wh.workDate <= todayStr))
      : sumWorkHours(workOnly)

    // 总计：与当前查看对象一致（本人或所选一人）
    const totalRes = await getWorkHourList({
      ...viewingQuery.value,
      pageNum: 1,
      pageSize: 10000
    })
    statistics.totalHours = sumWorkHours(totalRes?.list || [])
  } catch (error) {
    console.error('更新统计数据失败:', error)
    statistics.totalHours = sumWorkHours(allWorkHours.filter(wh => !isLeaveRecord(wh)))
  }
}

const fetchUsers = async () => {
  if (!canViewAllWorkHours.value) return
  try {
    const res: any = await getAllUsers()
    const list: UserInfo[] = res?.list || res || []
    const myId = authStore.getCurrentUserId()
    // 下拉不含自己（清空即回「我自己」）
    userOptions.value = list.filter(u => u.id !== myId)
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

const handleUserChange = () => {
  fetchMonthData()
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
  fetchUsers()
  fetchMonthData()
})
</script>

<style scoped>
.stats-row-5 {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--page-gap, 16px);
  margin-bottom: var(--page-gap, 16px);
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

.stats-card {
  background: var(--surface, #fff);
  border: 1px solid var(--line-1, #e5e6eb);
  border-radius: var(--radius-md, 6px);
  padding: 14px 16px;
  box-shadow: none;
}

.stats-label {
  font-size: 12px;
  color: var(--text-3, #8f959e);
}

.stats-value {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-1, #1f2329);
  margin-top: 6px;
  line-height: 1.2;
}

.stats-value .unit {
  font-size: 12px;
  font-weight: 400;
  color: var(--text-3, #8f959e);
  margin-left: 2px;
}

.stats-card-leave .stats-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stats-card-leave .stats-value {
  color: var(--warning, #d4790c);
}

.leave-detail-btn {
  font-size: 12px;
  color: var(--primary, #3370fe);
  cursor: pointer;
}

.leave-detail-btn:hover {
  color: var(--primary-hover, #245bdb);
  text-decoration: underline;
}

.calendar-panel {
  background: var(--surface, #fff);
  border: 1px solid var(--line-1, #e5e6eb);
  border-radius: var(--radius-md, 6px);
  overflow: hidden;
}

.calendar-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--line-1, #e5e6eb);
  background: var(--surface, #fff);
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-right {
  margin-left: auto;
}

.month-btn {
  min-width: 110px;
}

.user-filter-select {
  width: 180px;
  --el-component-size: var(--control-height, 32px);
}

.month-view {
  background: var(--surface, #fff);
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  border-top: 1px solid var(--line-1, #e5e6eb);
}

.calendar-weekday {
  padding: 8px 12px;
  font-size: 12px;
  color: var(--text-2, #646a73);
  background: var(--fill-1, #f7f8fa);
  border-bottom: 1px solid var(--line-1, #e5e6eb);
  border-right: 1px solid var(--line-1, #e5e6eb);
}

.calendar-weekday:last-child {
  border-right: none;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}

.calendar-cell {
  min-height: 96px;
  padding: 8px;
  border-bottom: 1px solid var(--line-1, #e5e6eb);
  border-right: 1px solid var(--line-1, #e5e6eb);
  font-size: 13px;
  cursor: pointer;
  background: var(--surface, #fff);
  transition: background-color 0.15s;
}

.calendar-cell:nth-child(7n) {
  border-right: none;
}

.calendar-cell:hover:not(.is-out) {
  background: var(--fill-1, #f7f8fa);
}

.calendar-cell.is-out {
  background: var(--fill-1, #f7f8fa);
  cursor: default;
}

.calendar-cell.is-today {
  background: var(--primary-tint, #eef3ff);
}

.calendar-date {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text-2, #646a73);
  min-width: 20px;
  height: 20px;
}

.calendar-cell.is-today .calendar-date {
  background: var(--primary, #3370fe);
  color: #fff;
  border-radius: 50%;
  width: 20px;
  font-weight: 500;
}

.calendar-log {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-1, #1f2329);
  background: var(--fill-1, #f7f8fa);
  border-radius: 3px;
  padding: 3px 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.calendar-log:hover {
  background: var(--primary-tint, #eef3ff);
  color: var(--primary, #3370fe);
}

.calendar-log .log-text {
  overflow: hidden;
}

.calendar-log .hours {
  float: right;
  color: var(--text-2, #646a73);
  margin-left: 6px;
}

.calendar-log.leave {
  background: var(--warning-tint, #fdf3e7);
  color: var(--warning, #d4790c);
}

.calendar-log.leave .hours {
  color: var(--warning, #d4790c);
}

.more-hint {
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-3, #8f959e);
  padding-left: 2px;
}

.detail-summary {
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--text-2, #646a73);
}

.detail-empty {
  padding: 12px 0;
}
</style>
