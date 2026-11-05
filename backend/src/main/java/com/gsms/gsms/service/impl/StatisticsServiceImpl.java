package com.gsms.gsms.service.impl;

import com.gsms.gsms.infra.utils.UserContext;
import com.gsms.gsms.model.entity.Project;
import com.gsms.gsms.model.entity.Task;
import com.gsms.gsms.model.entity.User;
import com.gsms.gsms.model.entity.WorkHour;
import com.gsms.gsms.model.enums.TaskStatus;
import com.gsms.gsms.repository.ProjectMapper;
import com.gsms.gsms.repository.TaskMapper;
import com.gsms.gsms.repository.UserMapper;
import com.gsms.gsms.repository.WorkHourMapper;
import com.gsms.gsms.service.StatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据统计服务实现
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsServiceImpl.class);

    private final WorkHourMapper workHourMapper;
    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;

    public StatisticsServiceImpl(WorkHourMapper workHourMapper, TaskMapper taskMapper, UserMapper userMapper, ProjectMapper projectMapper) {
        this.workHourMapper = workHourMapper;
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
        this.projectMapper = projectMapper;
    }

    /** 仅正常工时计入「工时」统计，请假不计入 */
    private static boolean isWorkRecord(WorkHour wh) {
        return wh != null && (wh.getRecordType() == null || !wh.getRecordType().isLeave());
    }

    private static List<WorkHour> onlyWorkRecords(List<WorkHour> workHours) {
        if (workHours == null || workHours.isEmpty()) {
            return Collections.emptyList();
        }
        return workHours.stream().filter(StatisticsServiceImpl::isWorkRecord).collect(Collectors.toList());
    }

    private static BigDecimal sumHours(List<WorkHour> workHours) {
        return workHours.stream()
                .map(WorkHour::getHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Map<String, Object> getProjectWorkHourStatistics(Long projectId, LocalDate startDate, LocalDate endDate) {
        logger.debug("统计项目工时: projectId={}, startDate={}, endDate={}", projectId, startDate, endDate);

        Map<String, Object> result = new HashMap<>();

        // selectByCondition(projectId) 已限定 record_type=1，再过滤一次兜底
        List<WorkHour> workHours = onlyWorkRecords(
                workHourMapper.selectByCondition(null, projectId, null, startDate, endDate));

        BigDecimal totalHours = sumHours(workHours);

        Map<Long, BigDecimal> userHoursMap = workHours.stream()
                .collect(Collectors.groupingBy(
                        WorkHour::getUserId,
                        Collectors.reducing(BigDecimal.ZERO, WorkHour::getHours, BigDecimal::add)
                ));

        result.put("projectId", projectId);
        result.put("totalHours", totalHours);
        result.put("totalRecords", workHours.size());
        result.put("userCount", userHoursMap.size());
        result.put("userHoursDistribution", userHoursMap);
        result.put("startDate", startDate);
        result.put("endDate", endDate);

        logger.info("项目工时统计完成: projectId={}, totalHours={}, recordCount={}", projectId, totalHours, workHours.size());
        return result;
    }

    @Override
    public Map<String, Object> getUserWorkHourStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        logger.debug("统计用户工时: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

        Map<String, Object> result = new HashMap<>();

        List<WorkHour> workHours = onlyWorkRecords(
                workHourMapper.selectByCondition(userId, null, null, startDate, endDate));

        BigDecimal totalHours = sumHours(workHours);

        Map<Long, BigDecimal> projectHoursMap = workHours.stream()
                .filter(wh -> wh.getProjectId() != null)
                .collect(Collectors.groupingBy(
                        WorkHour::getProjectId,
                        Collectors.reducing(BigDecimal.ZERO, WorkHour::getHours, BigDecimal::add)
                ));

        result.put("userId", userId);
        result.put("totalHours", totalHours);
        result.put("totalRecords", workHours.size());
        result.put("projectCount", projectHoursMap.size());
        result.put("projectHoursDistribution", projectHoursMap);
        result.put("startDate", startDate);
        result.put("endDate", endDate);

        logger.info("用户工时统计完成: userId={}, totalHours={}, recordCount={}", userId, totalHours, workHours.size());
        return result;
    }

    @Override
    public Map<String, Object> getDepartmentWorkHourStatistics(Long departmentId, LocalDate startDate, LocalDate endDate) {
        logger.debug("统计部门工时: departmentId={}, startDate={}, endDate={}", departmentId, startDate, endDate);

        Map<String, Object> result = new HashMap<>();

        List<User> departmentUsers = userMapper.selectByDepartmentId(departmentId);
        List<Long> userIds = departmentUsers.stream().map(User::getId).collect(Collectors.toList());

        if (userIds.isEmpty()) {
            result.put("departmentId", departmentId);
            result.put("totalHours", BigDecimal.ZERO);
            result.put("totalRecords", 0);
            result.put("userCount", 0);
            logger.info("部门工时统计完成: departmentId={}, totalHours=0, userCount=0", departmentId);
            return result;
        }

        List<WorkHour> workHours = new ArrayList<>();
        for (Long userId : userIds) {
            workHours.addAll(onlyWorkRecords(
                    workHourMapper.selectByCondition(userId, null, null, startDate, endDate)));
        }

        BigDecimal totalHours = sumHours(workHours);

        Map<Long, BigDecimal> userHoursMap = workHours.stream()
                .collect(Collectors.groupingBy(
                        WorkHour::getUserId,
                        Collectors.reducing(BigDecimal.ZERO, WorkHour::getHours, BigDecimal::add)
                ));

        result.put("departmentId", departmentId);
        result.put("totalHours", totalHours);
        result.put("totalRecords", workHours.size());
        result.put("userCount", userHoursMap.size());
        result.put("userHoursDistribution", userHoursMap);
        result.put("startDate", startDate);
        result.put("endDate", endDate);

        logger.info("部门工时统计完成: departmentId={}, totalHours={}, recordCount={}", departmentId, totalHours, workHours.size());
        return result;
    }

    @Override
    public Map<String, Object> getTaskWorkHourStatistics(Long taskId) {
        logger.debug("统计任务工时: taskId={}", taskId);

        Map<String, Object> result = new HashMap<>();

        List<WorkHour> workHours = onlyWorkRecords(workHourMapper.selectByTaskId(taskId));
        Task task = taskMapper.selectById(taskId);

        BigDecimal totalHours = sumHours(workHours);

        BigDecimal estimateHours = task != null ? task.getEstimateHours() : BigDecimal.ZERO;
        BigDecimal variance = totalHours.subtract(estimateHours);

        result.put("taskId", taskId);
        result.put("totalHours", totalHours);
        result.put("estimateHours", estimateHours);
        result.put("variance", variance);
        result.put("totalRecords", workHours.size());

        logger.info("任务工时统计完成: taskId={}, totalHours={}, estimateHours={}", taskId, totalHours, estimateHours);
        return result;
    }

    @Override
    public Map<String, Object> getProjectTaskCompletionStatistics(Long projectId) {
        logger.debug("统计项目任务完成度: projectId={}", projectId);

        Map<String, Object> result = new HashMap<>();

        List<Task> tasks = taskMapper.selectByProjectId(projectId);

        long totalTasks = tasks.size();
        long todoTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
        long inProgressTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long doneTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();

        double completionRate = totalTasks > 0 ? (double) doneTasks / totalTasks * 100 : 0;

        result.put("projectId", projectId);
        result.put("totalTasks", totalTasks);
        result.put("todoTasks", todoTasks);
        result.put("inProgressTasks", inProgressTasks);
        result.put("doneTasks", doneTasks);
        result.put("completionRate", String.format("%.2f%%", completionRate));

        logger.info("项目任务完成度统计完成: projectId={}, totalTasks={}, completionRate={}", projectId, totalTasks, result.get("completionRate"));
        return result;
    }

    @Override
    public Map<String, Object> getWorkHourTrendStatistics(Long projectId, Long userId, LocalDate startDate, LocalDate endDate) {
        logger.debug("统计工时趋势: projectId={}, userId={}, startDate={}, endDate={}", projectId, userId, startDate, endDate);

        Map<String, Object> result = new HashMap<>();

        List<WorkHour> workHours = onlyWorkRecords(
                workHourMapper.selectByCondition(userId, projectId, null, startDate, endDate));

        Map<LocalDate, BigDecimal> dailyHoursMap = workHours.stream()
                .collect(Collectors.groupingBy(
                        WorkHour::getWorkDate,
                        Collectors.reducing(BigDecimal.ZERO, WorkHour::getHours, BigDecimal::add)
                ));

        List<Map<String, Object>> trendData = dailyHoursMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> dataPoint = new HashMap<>();
                    dataPoint.put("date", entry.getKey());
                    dataPoint.put("hours", entry.getValue());
                    return dataPoint;
                })
                .collect(Collectors.toList());

        BigDecimal totalHours = sumHours(workHours);

        result.put("projectId", projectId);
        result.put("userId", userId);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("totalHours", totalHours);
        result.put("trendData", trendData);

        logger.info("工时趋势统计完成: totalHours={}, dataPoints={}", totalHours, trendData.size());
        return result;
    }

    @Override
    public Map<String, Object> getDashboardData() {
        logger.debug("获取首页看板数据");

        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("用户未登录");
        }

        Map<String, Object> result = new HashMap<>();
        LocalDate today = LocalDate.now();

        // 获取用户可访问的项目列表
        List<Project> allProjects = projectMapper.selectAccessibleProjects(userId);
        result.put("projectCount", allProjects.size());

        // 获取项目列表（限制5个）
        List<Map<String, Object>> projectList = allProjects.stream()
                .limit(5)
                .map(project -> {
                    Map<String, Object> projectData = new HashMap<>();
                    projectData.put("id", project.getId());
                    projectData.put("name", project.getName());
                    projectData.put("code", project.getCode());
                    projectData.put("status", project.getStatus());
                    return projectData;
                })
                .collect(Collectors.toList());
        result.put("projects", projectList);

        // 获取用户任务：待办（TODO/IN_PROGRESS）与已完成（DONE）
        List<Task> allTodoTasks = new ArrayList<>();
        List<Task> allDoneTasks = new ArrayList<>();
        List<Task> allTasks = taskMapper.selectAccessibleTasksByCondition(userId, null, userId, null);
        for (Task task : allTasks) {
            if (task.getStatus() == TaskStatus.TODO || task.getStatus() == TaskStatus.IN_PROGRESS) {
                allTodoTasks.add(task);
            } else if (task.getStatus() == TaskStatus.DONE) {
                allDoneTasks.add(task);
            }
        }

        result.put("pendingTaskCount", allTodoTasks.size());
        result.put("completedTaskCount", allDoneTasks.size());

        // 按父任务归类：子任务挂在父任务下，不与父任务平级展示
        result.put("pendingTasks", buildDashboardTaskTree(allTodoTasks));
        result.put("completedTasks", buildDashboardTaskTree(allDoneTasks));

        // 计算今日/本周/本月/总工时（不含请假）
        List<WorkHour> todayWorkHours = onlyWorkRecords(
                workHourMapper.selectByCondition(userId, null, null, today, today));
        result.put("todayHours", sumHours(todayWorkHours));

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);
        List<WorkHour> weekWorkHours = onlyWorkRecords(
                workHourMapper.selectByCondition(userId, null, null, weekStart, weekEnd));
        result.put("weekHours", sumHours(weekWorkHours));

        LocalDate monthStart = LocalDate.of(today.getYear(), today.getMonth(), 1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        List<WorkHour> monthWorkHours = onlyWorkRecords(
                workHourMapper.selectByCondition(userId, null, null, monthStart, monthEnd));
        result.put("monthHours", sumHours(monthWorkHours));

        List<WorkHour> allWorkHours = onlyWorkRecords(
                workHourMapper.selectByCondition(userId, null, null, null, null));
        BigDecimal totalHours = sumHours(allWorkHours);
        result.put("totalHours", totalHours);

        logger.info("首页看板数据获取完成: userId={}, projectCount={}, pendingTaskCount={}, todayHours={}",
                userId, allProjects.size(), allTodoTasks.size(), result.get("todayHours"));
        return result;
    }

    /**
     * 将任务按父任务归类：仅顶层父任务进列表，子任务放入 subtasks。
     * 若仅有子任务命中而父任务未命中，仍拉取父任务作为分组容器。
     */
    private List<Map<String, Object>> buildDashboardTaskTree(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Task> taskById = new HashMap<>();
        Map<Long, List<Task>> subtasksByParent = new HashMap<>();
        Set<Long> parentIds = new HashSet<>();

        for (Task task : tasks) {
            taskById.put(task.getId(), task);
            if (task.getParentId() == null) {
                parentIds.add(task.getId());
            } else {
                parentIds.add(task.getParentId());
                subtasksByParent.computeIfAbsent(task.getParentId(), k -> new ArrayList<>()).add(task);
            }
        }

        // 补齐未出现在结果集中的父任务（例如仅子任务命中筛选）
        List<Long> missingParentIds = parentIds.stream()
                .filter(id -> !taskById.containsKey(id))
                .collect(Collectors.toList());
        Map<Long, Task> parentLookup = new HashMap<>(taskById);
        if (!missingParentIds.isEmpty()) {
            List<Task> missingParents = taskMapper.selectTasksByIds(missingParentIds);
            if (missingParents != null) {
                for (Task parent : missingParents) {
                    parentLookup.put(parent.getId(), parent);
                }
            }
        }

        List<Task> roots = parentIds.stream()
                .map(parentLookup::get)
                .filter(Objects::nonNull)
                .sorted((t1, t2) -> t2.getId().compareTo(t1.getId()))
                .limit(5)
                .collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Task parent : roots) {
            Map<String, Object> parentData = toDashboardTaskMap(parent);
            List<Task> children = subtasksByParent.getOrDefault(parent.getId(), Collections.emptyList());
            List<Map<String, Object>> childMaps = children.stream()
                    .sorted((t1, t2) -> t2.getId().compareTo(t1.getId()))
                    .map(this::toDashboardTaskMap)
                    .collect(Collectors.toList());
            parentData.put("subtasks", childMaps);
            result.add(parentData);
        }
        return result;
    }

    private Map<String, Object> toDashboardTaskMap(Task task) {
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("id", task.getId());
        taskData.put("title", task.getTitle());
        taskData.put("description", task.getDescription());
        taskData.put("status", task.getStatus());
        taskData.put("priority", task.getPriority());
        taskData.put("projectId", task.getProjectId());
        taskData.put("planEndDate", task.getPlanEndDate());
        taskData.put("actualEndDate", task.getActualEndDate());
        taskData.put("parentId", task.getParentId());
        return taskData;
    }
}
