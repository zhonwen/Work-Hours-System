package com.gsms.gsms.controller;

import com.gsms.gsms.model.entity.User;
import com.gsms.gsms.dto.user.UserInfoResp;
import com.gsms.gsms.dto.user.UserLoginReq;
import com.gsms.gsms.dto.user.UserQueryReq;
import com.gsms.gsms.dto.user.UserRegisterReq;
import com.gsms.gsms.dto.user.UserCreateReq;
import com.gsms.gsms.dto.user.UserUpdateReq;
import com.gsms.gsms.dto.user.PasswordChangeReq;
import com.gsms.gsms.dto.user.PasswordResetReq;
import com.gsms.gsms.dto.user.UserPreferenceResp;
import com.gsms.gsms.dto.user.UserPreferenceUpdateReq;
import com.gsms.gsms.dto.user.UserProfileResp;
import com.gsms.gsms.dto.user.UserProfileUpdateReq;
import com.gsms.gsms.infra.common.PageResult;
import com.gsms.gsms.infra.common.Result;
import com.gsms.gsms.infra.exception.BusinessException;
import com.gsms.gsms.infra.utils.JwtUtil;
import com.gsms.gsms.infra.utils.RsaUtil;
import com.gsms.gsms.infra.utils.UserContext;
import com.gsms.gsms.model.enums.errorcode.UserErrorCode;
import com.gsms.gsms.service.CaptchaService;
import com.gsms.gsms.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理接口", description = "用户相关的API接口")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RsaUtil rsaUtil;
    private final CaptchaService captchaService;

    public UserController(UserService userService, JwtUtil jwtUtil,
                          RsaUtil rsaUtil, CaptchaService captchaService) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.rsaUtil = rsaUtil;
        this.captchaService = captchaService;
    }

    /**
     * 获取当前用户偏好（菜单主题等）
     */
    @GetMapping("/preference")
    @Operation(summary = "获取当前用户偏好")
    public Result<UserPreferenceResp> getPreference() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(userService.getPreference(userId));
    }

    /**
     * 更新当前用户偏好
     */
    @PutMapping("/preference")
    @Operation(summary = "更新当前用户偏好")
    public Result<UserPreferenceResp> updatePreference(@Valid @RequestBody UserPreferenceUpdateReq req) {
        Long userId = UserContext.getCurrentUserId();
        logger.info("更新用户偏好: userId={}, themeId={}", userId, req.getThemeId());
        return Result.success(userService.updatePreference(userId, req));
    }

    /**
     * 获取当前用户个人资料
     */
    @GetMapping("/profile")
    @Operation(summary = "获取当前用户个人资料")
    public Result<UserProfileResp> getProfile() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(userService.getProfile(userId));
    }

    /**
     * 更新当前用户个人资料
     */
    @PutMapping("/profile")
    @Operation(summary = "更新当前用户个人资料")
    public Result<UserProfileResp> updateProfile(@Valid @RequestBody UserProfileUpdateReq req) {
        Long userId = UserContext.getCurrentUserId();
        logger.info("更新个人资料: userId={}", userId);
        return Result.success(userService.updateProfile(userId, req));
    }

    /**
     * 上传当前用户头像
     */
    @PostMapping("/profile/avatar")
    @Operation(summary = "上传当前用户头像")
    public Result<UserProfileResp> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getCurrentUserId();
        logger.info("上传头像: userId={}, size={}", userId, file == null ? 0 : file.getSize());
        return Result.success(userService.uploadAvatar(userId, file));
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id:\\d+}")
    @Operation(summary = "根据ID查询用户")
    public Result<UserInfoResp> getById(@PathVariable Long id) {
        logger.info("查询用户: {}", id);
        return Result.success(userService.getById(id));
    }

    /**
     * 分页查询用户列表
     */
    @GetMapping
    @Operation(summary = "分页查询用户列表")
    public PageResult<UserInfoResp> findAll(UserQueryReq userQueryReq) {
        logger.info("查询用户列表: {}", userQueryReq);
        return userService.findAll(userQueryReq);
    }

    /**
     * 创建用户
     */
    @PostMapping
    @Operation(summary = "创建用户")
    public Result<UserInfoResp> create(@Valid @RequestBody UserCreateReq createReq) {
        logger.info("创建用户: {}", createReq.getUsername());
        UserInfoResp resp = userService.create(createReq);
        return Result.success(resp);
    }

    /**
     * 更新用户
     */
    @PutMapping
    @Operation(summary = "更新用户")
    public Result<UserInfoResp> update(@Valid @RequestBody UserUpdateReq updateReq) {
        logger.info("更新用户: {}", updateReq.getId());
        UserInfoResp resp = userService.update(updateReq);
        return Result.success(resp);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "删除用户")
    public Result<String> delete(@PathVariable Long id) {
        logger.info("删除用户: {}", id);
        userService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 用户登录：password 为 RSA 密文（明文格式：密码|cid）
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<String> login(@Valid @RequestBody UserLoginReq req) {
        logger.info("用户登录: {}", req.getUsername());
        String plainPassword = rsaUtil.decryptPasswordBoundToCid(req.getPassword(), req.getCid());
        captchaService.verifyAndConsume(req.getCid(), req.getCaptchaCode());

        User user = userService.login(req.getUsername(), plainPassword);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        logger.info("用户登录成功: {}", user.getUsername());
        return Result.success("登录成功", token);
    }

    /**
     * 用户注册：password 为 RSA 密文（明文格式：密码|cid）
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<UserInfoResp> register(@Valid @RequestBody UserRegisterReq req) {
        logger.info("用户注册: {}", req.getUsername());
        String plainPassword = rsaUtil.decryptPasswordBoundToCid(req.getPassword(), req.getCid());
        assertPasswordLength(plainPassword);
        captchaService.verifyAndConsume(req.getCid(), req.getCaptchaCode());

        UserCreateReq createReq = new UserCreateReq();
        createReq.setUsername(req.getUsername());
        createReq.setPassword(plainPassword);
        createReq.setNickname(req.getNickname());
        createReq.setEmail(req.getEmail());
        createReq.setPhone(req.getPhone());

        UserInfoResp resp = userService.create(createReq);
        logger.info("用户注册成功: {}", resp.getUsername());
        return Result.success(resp);
    }

    /**
     * 修改密码：old/new 均为 RSA 密文（明文格式：密码|cid）
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public Result<String> changePassword(@Valid @RequestBody PasswordChangeReq req) {
        logger.info("用户修改密码");
        String oldPlain = rsaUtil.decryptPasswordBoundToCid(req.getOldPassword(), req.getCid());
        String newPlain = rsaUtil.decryptPasswordBoundToCid(req.getNewPassword(), req.getCid());
        assertPasswordLength(newPlain);
        captchaService.verifyAndConsume(req.getCid(), req.getCaptchaCode());

        req.setOldPassword(oldPlain);
        req.setNewPassword(newPlain);
        userService.changePassword(req);
        return Result.success("密码修改成功，请重新登录");
    }

    /**
     * 重置密码（管理员）：newPassword 为 RSA 密文（明文格式：密码|cid）
     */
    @PutMapping("/password/reset")
    @Operation(summary = "重置密码（管理员）")
    public Result<String> resetPassword(@Valid @RequestBody PasswordResetReq req) {
        logger.info("管理员重置密码: userId={}", req.getUserId());
        String newPlain = rsaUtil.decryptPasswordBoundToCid(req.getNewPassword(), req.getCid());
        assertPasswordLength(newPlain);
        captchaService.verifyAndConsume(req.getCid(), req.getCaptchaCode());

        req.setNewPassword(newPlain);
        userService.resetPassword(req);
        return Result.success("密码重置成功");
    }

    private void assertPasswordLength(String plainPassword) {
        if (plainPassword == null || plainPassword.length() < 6 || plainPassword.length() > 20) {
            throw new BusinessException(UserErrorCode.PASSWORD_LENGTH_INVALID);
        }
    }

    /**
     * 查询用户的角色ID列表
     */
    @GetMapping("/{id}/roles")
    @Operation(summary = "查询用户的角色列表")
    public Result<java.util.List<Long>> getRoles(@PathVariable Long id) {
        logger.info("查询用户角色列表: userId={}", id);
        return Result.success(userService.getRoleIds(id));
    }

    /**
     * 为用户分配角色
     */
    @PostMapping("/{id}/roles")
    @Operation(summary = "为用户分配角色")
    public Result<String> assignRoles(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> payload) {
        logger.info("为用户分配角色: userId={}", id);
        @SuppressWarnings("unchecked")
        java.util.List<Long> roleIds = (java.util.List<Long>) payload.get("roleIds");
        userService.assignRoles(id, roleIds);
        return Result.success("角色分配成功");
    }

    /**
     * 移除用户角色
     */
    @DeleteMapping("/{userId}/roles/{roleId}")
    @Operation(summary = "移除用户角色")
    public Result<String> removeRole(
            @PathVariable Long userId,
            @PathVariable Long roleId) {
        logger.info("移除用户角色: userId={}, roleId={}", userId, roleId);
        userService.removeRole(userId, roleId);
        return Result.success("角色移除成功");
    }

    /**
     * 查询用户的权限编码列表
     */
    @GetMapping("/{id}/permissions")
    @Operation(summary = "查询用户的权限编码列表")
    public Result<java.util.List<String>> getPermissions(@PathVariable Long id) {
        logger.info("查询用户权限编码列表: userId={}", id);
        return Result.success(userService.getPermissionCodes(id));
    }
}
