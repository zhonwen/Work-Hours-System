package com.gsms.gsms.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.AssertTrue;

/**
 * 任务创建请求（继承公共字段）
 */
@Schema(description = "任务创建请求")
public class TaskCreateReq extends TaskBaseReq {

    @AssertTrue(message = "项目ID不能为空")
    @Schema(hidden = true)
    public boolean isProjectIdPresent() {
        return getProjectId() != null;
    }
}
