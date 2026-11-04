package com.gsms.gsms.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 工时记录类型：正常工时 / 请假
 */
public enum WorkHourRecordType {
    NORMAL(1, "正常工时"),
    LEAVE(2, "请假");

    @EnumValue
    private final Integer code;
    private final String desc;

    WorkHourRecordType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    @JsonValue
    @Override
    public String toString() {
        return this.name();
    }

    public static WorkHourRecordType fromCode(Integer code) {
        if (code == null) {
            return NORMAL;
        }
        for (WorkHourRecordType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("无效的工时记录类型: " + code);
    }

    public boolean isLeave() {
        return this == LEAVE;
    }
}
