<template>
  <div class="workhour-stats">
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
                  <el-avatar :size="20">{{
                    member.nickname?.charAt(0)
                  }}</el-avatar>
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
                <div class="ranking-number" :class="`rank-${index + 1}`">
                  {{ index + 1 }}
                </div>
                <el-avatar :size="36">{{ item.nickname?.charAt(0) }}</el-avatar>
                <div class="member-info">
                  <div class="member-name">{{ item.nickname }}</div>
                  <div class="member-stats">
                    <span>{{ item.taskCount }} 个任务</span>
                    <span>{{ item.workDays }} 天工作</span>
                  </div>
                </div>
                <div class="member-hours">
                  <div class="hours-value hours-accent">
                    {{ item.totalHours }}
                  </div>
                  <div class="hours-unit">小时</div>
                </div>
                <div class="hours-bar">
                  <div
                    class="hours-bar-fill"
                    :style="{ width: (item.totalHours / maxHours) * 100 + '%' }"
                  ></div>
                </div>
              </div>
              <el-empty
                v-if="memberRanking.length === 0"
                description="暂无工时数据"
                :image-size="80"
              />
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
                    :style="{
                      height: (item.hours / maxDailyHours) * 100 + '%',
                    }"
                    :title="`${item.date}: ${item.hours}h`"
                  ></div>
                </div>
                <div class="trend-label">{{ formatTrendDate(item.date) }}</div>
                <div class="trend-value hours-accent">{{ item.hours }}h</div>
              </div>
              <el-empty
                v-if="dailyTrend.length === 0"
                description="暂无趋势数据"
                :image-size="80"
              />
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 任务工时分布：父子同卡可展开；有工时→小时；有预估→预估Xh；已完成→完成；否则需评估 -->
      <div class="section-card section-card--auto">
        <h3 class="section-title">任务工时分布</h3>
        <div v-if="taskDistribution.length" class="wh-task-dist">
          <div
            v-for="(item, index) in taskDistribution"
            :key="item.taskId"
            class="wh-task-dist__card"
            :class="`is-${item.labelType}`"
          >
            <div
              class="wh-task-dist__row"
              @click="item.children?.length && toggleDistExpand(item.taskId)"
            >
              <div class="wh-task-dist__info">
                <button
                  v-if="item.children?.length"
                  type="button"
                  class="wh-task-dist__expand"
                  :aria-expanded="isDistExpanded(item.taskId)"
                  @click.stop="toggleDistExpand(item.taskId)"
                >
                  <el-icon>
                    <ArrowDown v-if="isDistExpanded(item.taskId)" />
                    <ArrowRight v-else />
                  </el-icon>
                </button>
                <span v-else class="wh-task-dist__expand-placeholder" />
                <span class="wh-task-dist__name" :title="item.taskName">{{
                  item.taskName
                }}</span>
                <span
                  v-if="item.children?.length"
                  class="wh-task-dist__child-count"
                >
                  {{ item.children.length }} 子任务
                </span>
                <span
                  class="wh-task-dist__hours"
                  :class="`label-${item.labelType}`"
                >
                  <el-icon
                    v-if="item.labelType === 'done'"
                    class="wh-task-dist__done-icon"
                  >
                    <CircleCheck />
                  </el-icon>
                  {{ item.hoursLabel }}
                </span>
              </div>
              <div class="wh-task-dist__track">
                <div
                  v-if="item.labelType === 'hours'"
                  class="wh-task-dist__fill"
                  :style="{
                    width:
                      Math.max((item.totalHours / maxTaskHours) * 100, 4) + '%',
                    backgroundColor: getChartColor(index),
                  }"
                ></div>
                <div
                  v-else-if="item.labelType === 'done'"
                  class="wh-task-dist__fill wh-task-dist__fill--done"
                ></div>
              </div>
            </div>

            <!-- 子任务：同卡内展开，评估/工时各自展示 -->
            <div
              v-if="item.children?.length && isDistExpanded(item.taskId)"
              class="wh-task-dist__children"
            >
              <div
                v-for="(child, cIndex) in item.children"
                :key="child.taskId"
                class="wh-task-dist__child"
                :class="`is-${child.labelType}`"
              >
                <div class="wh-task-dist__info">
                  <span class="wh-task-dist__branch" />
                  <span class="wh-task-dist__name" :title="child.taskName">{{
                    child.taskName
                  }}</span>
                  <span
                    class="wh-task-dist__hours"
                    :class="`label-${child.labelType}`"
                  >
                    <el-icon
                      v-if="child.labelType === 'done'"
                      class="wh-task-dist__done-icon"
                    >
                      <CircleCheck />
                    </el-icon>
                    {{ child.hoursLabel }}
                  </span>
                </div>
                <div class="wh-task-dist__track">
                  <div
                    v-if="child.labelType === 'hours'"
                    class="wh-task-dist__fill"
                    :style="{
                      width:
                        Math.max((child.totalHours / maxTaskHours) * 100, 4) +
                        '%',
                      backgroundColor: getChartColor(index + cIndex + 1),
                    }"
                  ></div>
                  <div
                    v-else-if="child.labelType === 'done'"
                    class="wh-task-dist__fill wh-task-dist__fill--done"
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="项目暂无任务" :image-size="72" />
      </div>

      <!-- 工时明细 -->
      <div class="section-card section-card--auto hour-details">
        <div class="hour-details__header">
          <h3 class="section-title">工时明细</h3>
          <div style="width: 150px">
            <el-date-picker
              v-model="detailDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              clearable
              class="hour-details__date-picker"
              @change="handleDetailDateChange"
            />
          </div>
        </div>
        <el-table :data="pagedWorkHours" stripe border style="width: 100%">
          <el-table-column prop="workDate" label="日期" width="120" />
          <el-table-column label="成员" width="140">
            <template #default="{ row }">
              <div class="user-cell">
                <el-avatar :size="24">{{
                  (row.userName || "U")?.charAt(0)
                }}</el-avatar>
                <span>{{ row.userName || "未知用户" }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="taskName" label="关联任务" min-width="200">
            <template #default="{ row }">
              <span class="task-link">{{ row.taskName || "无关联任务" }}</span>
            </template>
          </el-table-column>
          <el-table-column
            prop="hours"
            label="工时数"
            width="100"
            align="center"
          >
            <template #default="{ row }">
              <span class="hours-accent">{{ row.hours }}h</span>
            </template>
          </el-table-column>
          <el-table-column
            prop="content"
            label="工作内容"
            min-width="220"
            show-overflow-tooltip
          />
        </el-table>
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="detailWorkHours.length"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handlePageChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue";
import { ElMessage } from "element-plus";
import {
  Clock,
  TrendCharts,
  User,
  Calendar,
  CircleCheck,
  ArrowRight,
  ArrowDown,
} from "@element-plus/icons-vue";
import { getProjectMembers } from "@/api/project";
import { getWorkHoursByProjectId, type WorkHourInfo } from "@/api/workhour";
import { getTasksByProjectId, type TaskInfo } from "@/api/task";

type DistLabelType = "hours" | "estimate" | "assess" | "done";

type DistItem = {
  taskId: number;
  taskName: string;
  /** 展示用工时：父节点含子孙合计 */
  totalHours: number;
  hoursLabel: string;
  labelType: DistLabelType;
  children?: DistItem[];
};

const props = defineProps<{
  projectId: number;
}>();

const loading = ref(false);
const members = ref<
  Array<{ userId: number; nickname?: string; roleType?: number }>
>([]);
/** 扁平任务（统计成员任务数等） */
const projectTasks = ref<TaskInfo[]>([]);
/** 根任务树（工时分布父子同卡） */
const projectTaskRoots = ref<TaskInfo[]>([]);
/** 本项目全部工时（项目成员可见，非仅本人） */
const allProjectHours = ref<WorkHourInfo[]>([]);
const pageNum = ref(1);
const pageSize = ref(20);

const dateRange = ref<[string, string] | null>(null);
const detailDateRange = ref<[string, string] | null>(null);
const selectedMemberId = ref<number | undefined>(undefined);
/** 展开的父任务 id */
const expandedDistIds = ref<Set<number>>(new Set());

const summary = reactive({
  totalHours: 0,
  avgHours: 0,
  activeMembers: 0,
  workDays: 0,
});

const memberRanking = ref<any[]>([]);
const dailyTrend = ref<any[]>([]);
const taskDistribution = ref<DistItem[]>([]);

const chartColors = [
  "#1890ff",
  "#52c41a",
  "#faad14",
  "#f5222d",
  "#722ed1",
  "#eb2f96",
  "#13c2c2",
  "#2f54eb",
  "#fa8c16",
  "#a0d911",
];

/** 顶部筛选：影响概览 / 排行 / 趋势 / 分布 */
const filteredWorkHours = computed(() => {
  let list = allProjectHours.value.filter(
    (item) => item.recordType !== "LEAVE" && item.recordType !== 2
  );
  const [start, end] = dateRange.value || [];
  if (start) {
    list = list.filter((item) => item.workDate >= start);
  }
  if (end) {
    list = list.filter((item) => item.workDate <= end);
  }
  if (selectedMemberId.value != null) {
    list = list.filter(
      (item) => Number(item.userId) === Number(selectedMemberId.value)
    );
  }
  return list;
});

/** 明细表：成员沿用顶部筛选，日期用明细独立筛选 */
const detailWorkHours = computed(() => {
  let list = allProjectHours.value.filter(
    (item) => item.recordType !== "LEAVE" && item.recordType !== 2
  );
  if (selectedMemberId.value != null) {
    list = list.filter(
      (item) => Number(item.userId) === Number(selectedMemberId.value)
    );
  }
  const [start, end] = detailDateRange.value || [];
  if (start) {
    list = list.filter((item) => item.workDate >= start);
  }
  if (end) {
    list = list.filter((item) => item.workDate <= end);
  }
  return list;
});

const pagedWorkHours = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value;
  return detailWorkHours.value.slice(start, start + pageSize.value);
});

const maxHours = computed(() => {
  return Math.max(...memberRanking.value.map((m) => m.totalHours), 1);
});

const maxDailyHours = computed(() => {
  return Math.max(...dailyTrend.value.map((d) => d.hours), 1);
});

const maxTaskHours = computed(() => {
  const collect = (nodes: DistItem[]): number[] =>
    nodes.flatMap((n) => [
      n.totalHours,
      ...(n.children ? collect(n.children) : []),
    ]);
  return Math.max(...collect(taskDistribution.value), 1);
});

const isDistExpanded = (taskId: number) => expandedDistIds.value.has(taskId);

const toggleDistExpand = (taskId: number) => {
  const next = new Set(expandedDistIds.value);
  if (next.has(taskId)) next.delete(taskId);
  else next.add(taskId);
  expandedDistIds.value = next;
};

const fetchMembers = async () => {
  try {
    const res = await getProjectMembers(props.projectId);
    members.value = res || [];
  } catch (error) {
    console.error("获取成员列表失败:", error);
  }
};

const fetchTasks = async () => {
  try {
    const res = await getTasksByProjectId(props.projectId, 1, 1000);
    const roots = res?.list || [];
    projectTaskRoots.value = roots;
    const flat: TaskInfo[] = [];
    const walk = (list: TaskInfo[]) => {
      list.forEach((t) => {
        flat.push(t);
        if (t.subtasks?.length) walk(t.subtasks);
      });
    };
    walk(roots);
    projectTasks.value = flat;
  } catch (error) {
    console.error("获取任务列表失败:", error);
    projectTaskRoots.value = [];
    projectTasks.value = [];
  }
};

/** 拉取本项目全部成员工时（非 SELF 范围） */
const fetchWorkHours = async () => {
  try {
    const res = await getWorkHoursByProjectId(props.projectId);
    allProjectHours.value = res?.list || [];
    calculateStats();
  } catch (error) {
    console.error("获取工时数据失败:", error);
    ElMessage.error("获取工时数据失败");
  }
};

const calculateStats = () => {
  const workList = filteredWorkHours.value;
  summary.totalHours =
    Math.round(
      workList.reduce((sum, item) => sum + Number(item.hours || 0), 0) * 10
    ) / 10;

  const uniqueUsers = new Set(workList.map((item) => item.userId));
  summary.activeMembers = uniqueUsers.size;

  const uniqueDates = new Set(workList.map((item) => item.workDate));
  summary.workDays = uniqueDates.size;

  const memberDenom =
    selectedMemberId.value != null
      ? 1
      : Math.max(members.value.length, summary.activeMembers, 1);
  summary.avgHours = Math.round((summary.totalHours / memberDenom) * 10) / 10;

  // 成员负责任务数（本项目任务 assignee）
  const assignedTaskCount = new Map<number, number>();
  projectTasks.value.forEach((t) => {
    if (t.assigneeId == null) return;
    assignedTaskCount.set(
      t.assigneeId,
      (assignedTaskCount.get(t.assigneeId) || 0) + 1
    );
  });

  const hoursByUser = new Map<
    number,
    { totalHours: number; workDays: Set<string> }
  >();
  workList.forEach((item) => {
    if (!hoursByUser.has(item.userId)) {
      hoursByUser.set(item.userId, { totalHours: 0, workDays: new Set() });
    }
    const row = hoursByUser.get(item.userId)!;
    row.totalHours += Number(item.hours || 0);
    row.workDays.add(item.workDate);
  });

  const rankingSource =
    selectedMemberId.value != null
      ? members.value.filter(
          (m) => Number(m.userId) === Number(selectedMemberId.value)
        )
      : members.value;

  memberRanking.value = rankingSource
    .map((m) => {
      const agg = hoursByUser.get(m.userId);
      return {
        userId: m.userId,
        nickname: m.nickname || `用户${m.userId}`,
        totalHours: Math.round((agg?.totalHours || 0) * 10) / 10,
        taskCount: assignedTaskCount.get(m.userId) || 0,
        workDays: agg?.workDays.size || 0,
      };
    })
    .sort((a, b) => b.totalHours - a.totalHours);

  hoursByUser.forEach((agg, userId) => {
    if (memberRanking.value.some((m) => Number(m.userId) === Number(userId)))
      return;
    if (
      selectedMemberId.value != null &&
      Number(userId) !== Number(selectedMemberId.value)
    )
      return;
    const sample = workList.find((w) => Number(w.userId) === Number(userId));
    memberRanking.value.push({
      userId,
      nickname: sample?.userName || `用户${userId}`,
      totalHours: Math.round(agg.totalHours * 10) / 10,
      taskCount: assignedTaskCount.get(userId) || 0,
      workDays: agg.workDays.size,
    });
  });
  memberRanking.value.sort((a, b) => b.totalHours - a.totalHours);

  const dateMap = new Map<string, number>();
  workList.forEach((item) => {
    const hours = dateMap.get(item.workDate) || 0;
    dateMap.set(item.workDate, hours + Number(item.hours || 0));
  });

  dailyTrend.value = Array.from(dateMap.entries())
    .map(([date, hours]) => ({ date, hours: Math.round(hours * 10) / 10 }))
    .sort((a, b) => a.date.localeCompare(b.date))
    .slice(-30);

  // 任务工时分布：父子同卡；0h 按状态/预估标注
  const hoursByTask = new Map<number, number>();
  workList.forEach((item) => {
    if (!item.taskId) return;
    hoursByTask.set(
      item.taskId,
      (hoursByTask.get(item.taskId) || 0) + Number(item.hours || 0)
    );
  });

  const formatEstimate = (hours: number | string | null | undefined) => {
    if (hours == null || hours === "") return "";
    const n = Number(hours);
    if (Number.isNaN(n) || n <= 0) return "";
    return `${Math.round(n * 10) / 10}h`;
  };

  const buildLeafLabel = (
    t: TaskInfo,
    selfHours: number
  ): Pick<DistItem, "hoursLabel" | "labelType" | "totalHours"> => {
    if (selfHours > 0) {
      return {
        totalHours: selfHours,
        hoursLabel: `${selfHours}h`,
        labelType: "hours",
      };
    }
    const done = t.status === "DONE" || t.status === "CLOSED";
    if (done) {
      return { totalHours: 0, hoursLabel: "完成", labelType: "done" };
    }
    const estimateText = formatEstimate(t.estimateHours);
    if (estimateText) {
      return {
        totalHours: 0,
        hoursLabel: `预估${estimateText}`,
        labelType: "estimate",
      };
    }
    return { totalHours: 0, hoursLabel: "需评估", labelType: "assess" };
  };

  /** 父节点汇总标签：有实际工时优先；否则父子评估状态拼在一起 */
  const buildParentLabel = (
    t: TaskInfo,
    selfHours: number,
    children: DistItem[]
  ): Pick<DistItem, "hoursLabel" | "labelType" | "totalHours"> => {
    const childSum = children.reduce((s, c) => s + c.totalHours, 0);
    const total = Math.round((selfHours + childSum) * 10) / 10;
    if (total > 0) {
      return { totalHours: total, hoursLabel: `${total}h`, labelType: "hours" };
    }

    const self = buildLeafLabel(t, 0);
    const parts: string[] = [self.hoursLabel];
    children.forEach((c) => {
      if (!parts.includes(c.hoursLabel)) parts.push(c.hoursLabel);
    });
    const joined = parts.join(" / ");

    const allDone =
      self.labelType === "done" &&
      children.every((c) => c.labelType === "done");
    if (allDone) {
      return { totalHours: 0, hoursLabel: "完成", labelType: "done" };
    }
    const hasEstimate =
      self.labelType === "estimate" ||
      children.some((c) => c.labelType === "estimate");
    const hasAssess =
      self.labelType === "assess" ||
      children.some((c) => c.labelType === "assess");
    if (hasEstimate && !hasAssess) {
      return { totalHours: 0, hoursLabel: joined, labelType: "estimate" };
    }
    if (hasEstimate) {
      // 预估与需评估同卡展示在一起
      return { totalHours: 0, hoursLabel: joined, labelType: "estimate" };
    }
    if (hasAssess) {
      return { totalHours: 0, hoursLabel: joined, labelType: "assess" };
    }
    return { totalHours: 0, hoursLabel: joined, labelType: self.labelType };
  };

  const buildNode = (t: TaskInfo): DistItem => {
    const children = (t.subtasks || []).map(buildNode);
    const selfHours = Math.round((hoursByTask.get(t.id) || 0) * 10) / 10;
    if (children.length) {
      const label = buildParentLabel(t, selfHours, children);
      return {
        taskId: t.id,
        taskName: t.title,
        children,
        ...label,
      };
    }
    const label = buildLeafLabel(t, selfHours);
    return {
      taskId: t.id,
      taskName: t.title,
      ...label,
    };
  };

  const dist: DistItem[] = projectTaskRoots.value.map(buildNode);
  const coveredIds = new Set<number>();
  const mark = (nodes: DistItem[]) => {
    nodes.forEach((n) => {
      coveredIds.add(n.taskId);
      if (n.children?.length) mark(n.children);
    });
  };
  mark(dist);

  // 工时指向已不在树中的任务时补齐为独立卡片
  hoursByTask.forEach((hours, taskId) => {
    if (coveredIds.has(Number(taskId))) return;
    const total = Math.round(hours * 10) / 10;
    if (total <= 0) return;
    const sample = workList.find((w) => Number(w.taskId) === Number(taskId));
    dist.push({
      taskId,
      taskName: sample?.taskName || `任务 #${taskId}`,
      totalHours: total,
      hoursLabel: `${total}h`,
      labelType: "hours",
    });
  });

  const typeRank = (t: DistLabelType) => {
    if (t === "hours") return 0;
    if (t === "estimate") return 1;
    if (t === "assess") return 2;
    return 3;
  };
  taskDistribution.value = dist
    .sort(
      (a, b) =>
        b.totalHours - a.totalHours ||
        typeRank(a.labelType) - typeRank(b.labelType) ||
        a.taskName.localeCompare(b.taskName, "zh-CN")
    )
    .slice(0, 30);

  // 默认展开带预估/需评估混合的父子卡，便于一眼看到评估时间
  const nextExpanded = new Set<number>();
  taskDistribution.value.forEach((item) => {
    if (!item.children?.length) return;
    const mixed = item.hoursLabel.includes("/");
    if (
      mixed ||
      item.children.some(
        (c) => c.labelType === "estimate" || c.labelType === "assess"
      )
    ) {
      nextExpanded.add(item.taskId);
    }
  });
  expandedDistIds.value = nextExpanded;
};

const handleFilterChange = () => {
  pageNum.value = 1;
  calculateStats();
};

const handleDetailDateChange = () => {
  pageNum.value = 1;
};

const handlePageChange = () => {
  // 客户端分页，仅依赖 pageNum/pageSize
};

const formatTrendDate = (dateStr: string) => {
  const date = new Date(dateStr);
  return `${date.getMonth() + 1}/${date.getDate()}`;
};

const getChartColor = (index: number) => {
  return chartColors[index % chartColors.length];
};

onMounted(async () => {
  loading.value = true;
  try {
    await Promise.all([fetchMembers(), fetchTasks(), fetchWorkHours()]);
    calculateStats();
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.workhour-stats {
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 16px 20px 56px;
  box-sizing: border-box;
  background: #f5f7fa;
}

.workhour-stats-body {
  width: 100%;
  max-width: 100%;
  padding-bottom: 24px;
  box-sizing: border-box;
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

/* 覆盖全局 section-card height:100%，避免卡片被撑满视口产生大块空白 */
.workhour-stats :deep(.section-card) {
  height: auto;
}

.section-card--auto {
  height: auto !important;
}

.charts-row {
  margin-bottom: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: stretch;
}

.charts-row .el-col {
  margin-bottom: 16px;
  display: flex;
}

.charts-row .el-col > .section-card {
  width: 100%;
  height: 100% !important;
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

.wh-task-dist {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 360px;
  overflow-y: auto;
  padding-right: 4px;
}

.wh-task-dist__card {
  padding: 12px 14px;
  background: #f7f8fa;
  border: 1px solid #eef0f3;
  border-radius: 6px;
  box-sizing: border-box;
  flex-shrink: 0;
}

.wh-task-dist__row {
  cursor: default;
}

.wh-task-dist__card:has(.wh-task-dist__expand) .wh-task-dist__row {
  cursor: pointer;
}

.wh-task-dist__info {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  gap: 8px;
}

.wh-task-dist__expand,
.wh-task-dist__expand-placeholder {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  background: transparent;
  color: #909399;
  cursor: pointer;
  border-radius: 4px;
}

.wh-task-dist__expand:hover {
  background: #e8eaed;
  color: #303133;
}

.wh-task-dist__name {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wh-task-dist__child-count {
  flex-shrink: 0;
  font-size: 12px;
  color: #909399;
  background: #eef0f3;
  border-radius: 10px;
  padding: 1px 8px;
}

.wh-task-dist__hours {
  flex-shrink: 0;
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 600;
  color: #1890ff;
  max-width: 46%;
  text-align: right;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.wh-task-dist__hours.label-estimate {
  color: #303133;
}

.wh-task-dist__hours.label-assess {
  color: #e6a23c;
}

.wh-task-dist__hours.label-done {
  color: #67c23a;
}

.wh-task-dist__done-icon {
  font-size: 16px;
}

.wh-task-dist__track {
  width: 100%;
  height: 8px;
  background: #e8eaed;
  border-radius: 4px;
  overflow: hidden;
}

.wh-task-dist__fill {
  height: 100%;
  min-width: 4px;
  border-radius: 4px;
  transition: width 0.35s ease;
}

.wh-task-dist__fill--done {
  width: 100%;
  min-width: 100%;
  background: linear-gradient(90deg, #95d475 0%, #67c23a 100%);
}

.wh-task-dist__card.is-assess .wh-task-dist__track,
.wh-task-dist__card.is-estimate > .wh-task-dist__row .wh-task-dist__track,
.wh-task-dist__child.is-assess .wh-task-dist__track,
.wh-task-dist__child.is-estimate .wh-task-dist__track {
  background: #f0f2f5;
}

.wh-task-dist__children {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #e4e7ed;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.wh-task-dist__child {
  padding-left: 4px;
}

.wh-task-dist__branch {
  flex-shrink: 0;
  width: 14px;
  height: 14px;
  margin-left: 4px;
  border-left: 2px solid #dcdfe6;
  border-bottom: 2px solid #dcdfe6;
  border-radius: 0 0 0 3px;
  align-self: flex-start;
  margin-top: 4px;
}

.wh-task-dist__child .wh-task-dist__name {
  font-weight: 400;
  color: #606266;
}

.wh-task-dist__child .wh-task-dist__info {
  margin-bottom: 8px;
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
  margin-bottom: 8px;
}

.hour-details__header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.hour-details__header .section-title {
  margin: 0;
}

/* daterange 默认约 350px；父级 flex 下还需压掉 min-width:auto，width 才会生效 */
.hour-details__date-picker {
  --el-date-editor-daterange-width: 150px;
  width: 150px;
  min-width: 150px;
  flex-shrink: 0;
}

.workhour-stats-body > .section-card:last-child {
  margin-bottom: 0;
}
</style>
