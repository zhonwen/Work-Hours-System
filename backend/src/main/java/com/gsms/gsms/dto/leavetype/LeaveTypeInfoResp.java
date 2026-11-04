package com.gsms.gsms.dto.leavetype;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gsms.gsms.model.entity.LeaveType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "请假类型信息")
public class LeaveTypeInfoResp {

    private Long id;
    private String name;
    private String code;
    private Integer sort;
    private Integer status;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public static LeaveTypeInfoResp from(LeaveType entity) {
        if (entity == null) {
            return null;
        }
        LeaveTypeInfoResp resp = new LeaveTypeInfoResp();
        resp.setId(entity.getId());
        resp.setName(entity.getName());
        resp.setCode(entity.getCode());
        resp.setSort(entity.getSort());
        resp.setStatus(entity.getStatus());
        resp.setRemark(entity.getRemark());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
