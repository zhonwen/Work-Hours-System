package com.gsms.gsms.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 更新当前用户个人资料
 */
@Schema(description = "个人资料更新请求")
public class UserProfileUpdateReq {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过50")
    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Size(max = 50, message = "部门长度不能超过50")
    @Schema(description = "个人资料部门（自由文本）")
    private String departmentDisplay;

    @Schema(description = "手机（选填）", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱（选填）", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "头像：仅允许 preset:user1～user4；自定义请走上传接口")
    private String avatarUrl;

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
