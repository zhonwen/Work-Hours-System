package com.gsms.gsms.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Size;

/**
 * 更新项目公告请求
 */
@Schema(description = "更新项目公告请求")
public class ProjectAnnouncementUpdateReq {

    @Schema(description = "公告Markdown内容，最多5000字；空字符串表示清空")
    @Size(max = 5000, message = "公告内容不能超过5000字")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
