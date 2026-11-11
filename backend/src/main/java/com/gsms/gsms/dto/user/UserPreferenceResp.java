package com.gsms.gsms.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户偏好响应
 */
@Schema(description = "用户偏好响应")
public class UserPreferenceResp {

    @Schema(description = "菜单主题ID")
    private String themeId;

    public UserPreferenceResp() {
    }

    public UserPreferenceResp(String themeId) {
        this.themeId = themeId;
    }

    public String getThemeId() {
        return themeId;
    }

    public void setThemeId(String themeId) {
        this.themeId = themeId;
    }
}
