package com.gsms.gsms.dto.workhour;

import com.gsms.gsms.dto.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 工时记录查询请求
 */
@Schema(description = "工时记录查询请求")
public class WorkHourQueryReq extends BasePageQuery {

    /**
     * 查看范围：SELF=仅自己（默认），ALL=全部，USER=指定用户。
     * ALL / USER 需具备 WORKHOUR_VIEW_ALL；日历与「我的工时」默认 SELF。
     */
    @Schema(description = "查看范围 SELF/ALL/USER，默认 SELF", example = "SELF")
    private String viewScope;
    
    @Schema(description = "用户ID（viewScope=USER 时必填；SELF 时忽略）", example = "1")
    private Long userId;
    
    @Schema(description = "项目ID", example = "1")
    private Long projectId;
    
    @Schema(description = "任务ID", example = "1")
    private Long taskId;
    
    @Schema(description = "开始日期（格式：yyyy-MM-dd）", example = "2025-01-01")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "结束日期（格式：yyyy-MM-dd）", example = "2025-01-31")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    // Getter and Setter
    public String getViewScope() {
        return viewScope;
    }

    public void setViewScope(String viewScope) {
        this.viewScope = viewScope;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}