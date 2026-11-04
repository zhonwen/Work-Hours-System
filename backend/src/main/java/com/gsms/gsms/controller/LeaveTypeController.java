package com.gsms.gsms.controller;

import com.gsms.gsms.dto.leavetype.LeaveTypeCreateReq;
import com.gsms.gsms.dto.leavetype.LeaveTypeInfoResp;
import com.gsms.gsms.dto.leavetype.LeaveTypeUpdateReq;
import com.gsms.gsms.infra.common.PageResult;
import com.gsms.gsms.infra.common.Result;
import com.gsms.gsms.service.LeaveTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "请假类型", description = "请假类型字典管理")
@RestController
@RequestMapping("/api/leave-types")
@Validated
public class LeaveTypeController {

    private static final Logger logger = LoggerFactory.getLogger(LeaveTypeController.class);

    private final LeaveTypeService leaveTypeService;

    public LeaveTypeController(LeaveTypeService leaveTypeService) {
        this.leaveTypeService = leaveTypeService;
    }

    @Operation(summary = "分页查询请假类型")
    @GetMapping
    public PageResult<LeaveTypeInfoResp> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return leaveTypeService.findAll(name, status, pageNum, pageSize);
    }

    @Operation(summary = "查询启用中的请假类型（填报下拉）")
    @GetMapping("/enabled")
    public Result<List<LeaveTypeInfoResp>> listEnabled() {
        return Result.success(leaveTypeService.listEnabled());
    }

    @Operation(summary = "根据ID查询")
    @GetMapping("/{id}")
    public Result<LeaveTypeInfoResp> getById(@PathVariable Long id) {
        return Result.success(leaveTypeService.getById(id));
    }

    @Operation(summary = "创建请假类型")
    @PostMapping
    public Result<LeaveTypeInfoResp> create(@Valid @RequestBody LeaveTypeCreateReq req) {
        logger.info("创建请假类型: {}", req.getName());
        return Result.success(leaveTypeService.create(req));
    }

    @Operation(summary = "更新请假类型")
    @PutMapping
    public Result<LeaveTypeInfoResp> update(@Valid @RequestBody LeaveTypeUpdateReq req) {
        logger.info("更新请假类型: {}", req.getId());
        return Result.success(leaveTypeService.update(req));
    }

    @Operation(summary = "删除请假类型")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        logger.info("删除请假类型: {}", id);
        leaveTypeService.delete(id);
        return Result.success();
    }
}
