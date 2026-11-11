package com.gsms.gsms.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 当前用户个人资料响应
 */
@Schema(description = "个人资料响应")
public class UserProfileResp {

    @Schema(description = "用户名（只读）")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "个人资料部门（自由文本）")
    private String departmentDisplay;

    @Schema(description = "手机")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像标识：preset:userN 或 /uploads/...")
    private String avatarUrl;

    public UserProfileResp() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getDepartmentDisplay() {
        return departmentDisplay;
    }

    public void setDepartmentDisplay(String departmentDisplay) {
        this.departmentDisplay = departmentDisplay;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
