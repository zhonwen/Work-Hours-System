package com.gsms.gsms.dto.leavetype;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Schema(description = "更新请假类型")
public class LeaveTypeUpdateReq {

    @NotNull(message = "ID不能为空")
    private Long id;

    @Schema(description = "类型名称")
    private String name;

    @Pattern(regexp = "^[A-Z_]+$", message = "编码只能包含大写字母和下划线")
    @Schema(description = "类型编码")
    private String code;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态 1启用 0禁用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

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
}
