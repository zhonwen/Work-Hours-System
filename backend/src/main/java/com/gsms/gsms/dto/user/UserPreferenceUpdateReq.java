package com.gsms.gsms.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;

/**
 * 用户偏好更新请求
 */
@Schema(description = "用户偏好更新请求")
public class UserPreferenceUpdateReq {

    @NotBlank(message = "主题ID不能为空")
    @Schema(description = "菜单主题ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String themeId;

    public String getThemeId() {
        return themeId;
    }

    public void setThemeId(String themeId) {
        this.themeId = themeId;
    }
}
