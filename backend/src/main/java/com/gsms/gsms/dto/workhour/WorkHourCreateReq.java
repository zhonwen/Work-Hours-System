package com.gsms.gsms.dto.workhour;

import com.gsms.gsms.model.enums.WorkHourRecordType;
import com.gsms.gsms.model.enums.WorkHourStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "工时记录创建请求")
public class WorkHourCreateReq {

    @Schema(description = "项目ID（正常工时必填，请假不填）")
    private Long projectId;

    @Schema(description = "任务ID（可选）")
    private Long taskId;

    @NotNull(message = "工作日期不能为空")
    @Schema(description = "工作日期（格式：yyyy-MM-dd）", required = true)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate workDate;

    @NotNull(message = "工时数不能为空")
    @Schema(description = "工时数")
    private BigDecimal hours;

    @Schema(description = "工作内容/请假备注")
    private String content;

    @Schema(description = "记录类型 NORMAL/LEAVE，默认 NORMAL")
    private WorkHourRecordType recordType;

    @Schema(description = "请假类型ID（请假必填）")
    private Long leaveTypeId;

    @Schema(description = "状态")
    private WorkHourStatus status;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public BigDecimal getHours() { return hours; }
    public void setHours(BigDecimal hours) { this.hours = hours; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public WorkHourRecordType getRecordType() { return recordType; }
    public void setRecordType(WorkHourRecordType recordType) { this.recordType = recordType; }
    public Long getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(Long leaveTypeId) { this.leaveTypeId = leaveTypeId; }
    public WorkHourStatus getStatus() { return status; }
    public void setStatus(WorkHourStatus status) { this.status = status; }
}
