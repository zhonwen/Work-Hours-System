package com.gsms.gsms.model.errorcode;

import com.gsms.gsms.infra.exception.ErrorCode;

/**
 * 请假类型错误码 56xx
 */
public enum LeaveTypeErrorCode implements ErrorCode {
    LEAVE_TYPE_NOT_FOUND(5601, "请假类型不存在"),
    LEAVE_TYPE_CODE_EXISTS(5602, "请假类型编码已存在"),
    LEAVE_TYPE_DISABLED(5603, "请假类型已禁用"),
    LEAVE_TYPE_IN_USE(5604, "请假类型已被使用，无法删除"),
    LEAVE_TYPE_CREATE_FAILED(5691, "请假类型创建失败"),
    LEAVE_TYPE_UPDATE_FAILED(5692, "请假类型更新失败"),
    LEAVE_TYPE_DELETE_FAILED(5693, "请假类型删除失败");

    private final int code;
    private final String message;

    LeaveTypeErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
