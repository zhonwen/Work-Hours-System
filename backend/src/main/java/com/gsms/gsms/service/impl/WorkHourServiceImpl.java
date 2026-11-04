package com.gsms.gsms.service.impl;

import com.gsms.gsms.model.entity.LeaveType;
import com.gsms.gsms.model.entity.WorkHour;
import com.gsms.gsms.infra.exception.CommonErrorCode;
import com.gsms.gsms.model.enums.WorkHourRecordType;
import com.gsms.gsms.model.errorcode.LeaveTypeErrorCode;
import com.gsms.gsms.model.errorcode.WorkHourErrorCode;
import com.gsms.gsms.infra.exception.BusinessException;
import com.gsms.gsms.infra.utils.UserContext;
import com.gsms.gsms.repository.LeaveTypeMapper;
import com.gsms.gsms.repository.WorkHourMapper;
import com.gsms.gsms.service.AuthService;
import com.gsms.gsms.service.WorkHourService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 工时记录服务实现类
 */
@Service
public class WorkHourServiceImpl implements WorkHourService {

    private final WorkHourMapper workHourMapper;
    private final LeaveTypeMapper leaveTypeMapper;
    private final AuthService authService;

    public WorkHourServiceImpl(WorkHourMapper workHourMapper, LeaveTypeMapper leaveTypeMapper, AuthService authService) {
        this.workHourMapper = workHourMapper;
        this.leaveTypeMapper = leaveTypeMapper;
        this.authService = authService;
    }

    private void validateAndNormalize(WorkHour workHour) {
        WorkHourRecordType recordType = workHour.getRecordType() != null
                ? workHour.getRecordType()
                : WorkHourRecordType.NORMAL;
        workHour.setRecordType(recordType);

        if (recordType.isLeave()) {
            if (workHour.getLeaveTypeId() == null) {
                throw new BusinessException(WorkHourErrorCode.WORKHOUR_LEAVE_TYPE_REQUIRED);
            }
            LeaveType leaveType = leaveTypeMapper.selectById(workHour.getLeaveTypeId());
            if (leaveType == null) {
                throw new BusinessException(LeaveTypeErrorCode.LEAVE_TYPE_NOT_FOUND);
            }
            if (leaveType.getStatus() == null || leaveType.getStatus() != 1) {
                throw new BusinessException(LeaveTypeErrorCode.LEAVE_TYPE_DISABLED);
            }
            workHour.setProjectId(null);
            workHour.setTaskId(null);
        } else {
            if (workHour.getProjectId() == null) {
                throw new BusinessException(WorkHourErrorCode.WORKHOUR_PROJECT_REQUIRED);
            }
            workHour.setLeaveTypeId(null);
        }
    }

    @Override
    public WorkHour getWorkHourById(Long id) {
        // 先查询工时记录
        WorkHour workHour = workHourMapper.selectById(id);
        if (workHour == null) {
            throw new BusinessException(WorkHourErrorCode.WORKHOUR_NOT_FOUND);
        }

        // 鉴权
        Long currentUserId = UserContext.getCurrentUserId();
        authService.checkWorkHourAccess(currentUserId, workHour.getUserId());

        return workHour;
    }

    @Override
    public List<WorkHour> getWorkHoursByUserId(Long userId) {
        Long currentUserId = UserContext.getCurrentUserId();
        // 具备全局工时查看权限的用户可以查看任意用户工时
        if (authService.canViewAllWorkHours(currentUserId)) {
            return workHourMapper.selectByUserId(userId);
        }
        // 普通用户只能查看自己的工时记录
        if (!currentUserId.equals(userId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        return workHourMapper.selectByUserId(currentUserId);
    }

    @Override
    public List<WorkHour> getWorkHoursByProjectId(Long projectId) {
        Long currentUserId = UserContext.getCurrentUserId();
        // 具备全局工时查看权限的用户可以查看任意项目工时
        if (authService.canViewAllWorkHours(currentUserId)) {
            return workHourMapper.selectByProjectId(projectId);
        }
        // 普通用户只能查看自己参与项目范围内的工时
        authService.checkProjectAccess(currentUserId, projectId);
        return workHourMapper.selectByProjectId(projectId);
    }

    @Override
    public List<WorkHour> getWorkHoursByCondition(Long userId, Long projectId, Long taskId, LocalDate startDate, LocalDate endDate) {
        Long currentUserId = UserContext.getCurrentUserId();
        // 具备全局工时查看权限的用户可以按任意条件查询
        if (authService.canViewAllWorkHours(currentUserId)) {
            return workHourMapper.selectByCondition(userId, projectId, taskId, startDate, endDate);
        }
        // 普通用户：
        // 1）只能查询自己的工时，因此强制 userId = 当前用户；
        // 2）如果传入 projectId，则必须在可访问项目范围内
        Long queryUserId = currentUserId;
        Long queryProjectId = projectId;
        if (projectId != null) {
            authService.checkProjectAccess(currentUserId, projectId);
        }
        return workHourMapper.selectByCondition(queryUserId, queryProjectId, taskId, startDate, endDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkHour createWorkHour(WorkHour workHour) {
        Long currentUserId = UserContext.getCurrentUserId();
        validateAndNormalize(workHour);
        if (!workHour.getRecordType().isLeave()) {
            authService.checkProjectAccess(currentUserId, workHour.getProjectId());
        }

        workHour.setUserId(currentUserId);
        workHour.setCreateUserId(currentUserId);
        workHour.setUpdateUserId(currentUserId);

        int result = workHourMapper.insert(workHour);
        if (result <= 0) {
            throw new BusinessException(WorkHourErrorCode.WORKHOUR_CREATE_FAILED);
        }

        return workHourMapper.selectById(workHour.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<WorkHour> createWorkHoursBatch(List<WorkHour> workHours) {
        Long currentUserId = UserContext.getCurrentUserId();

        for (WorkHour workHour : workHours) {
            validateAndNormalize(workHour);
            if (!workHour.getRecordType().isLeave()) {
                authService.checkProjectAccess(currentUserId, workHour.getProjectId());
            }
            workHour.setUserId(currentUserId);
            workHour.setCreateUserId(currentUserId);
            workHour.setUpdateUserId(currentUserId);
        }

        workHours.forEach(workHour -> workHourMapper.insert(workHour));
        return workHours;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkHour updateWorkHour(WorkHour workHour) {
        WorkHour existWorkHour = workHourMapper.selectById(workHour.getId());
        if (existWorkHour == null) {
            throw new BusinessException(WorkHourErrorCode.WORKHOUR_NOT_FOUND);
        }

        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        authService.checkWorkHourAccess(currentUserId, existWorkHour.getUserId());

        validateAndNormalize(workHour);
        if (!workHour.getRecordType().isLeave()) {
            authService.checkProjectAccess(currentUserId, workHour.getProjectId());
        }
        workHour.setUpdateUserId(currentUserId);

        int result = workHourMapper.update(workHour);
        if (result <= 0) {
            throw new BusinessException(WorkHourErrorCode.WORKHOUR_UPDATE_FAILED);
        }

        return workHourMapper.selectById(workHour.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkHour(Long id) {
        // 检查工时记录是否存在
        WorkHour existWorkHour = workHourMapper.selectById(id);
        if (existWorkHour == null) {
            throw new BusinessException(WorkHourErrorCode.WORKHOUR_NOT_FOUND);
        }

        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        // 鉴权 - 检查工时访问权限
        authService.checkWorkHourAccess(currentUserId, existWorkHour.getUserId());

        int result = workHourMapper.deleteById(id);
        if (result <= 0) {
            throw new BusinessException(WorkHourErrorCode.WORKHOUR_DELETE_FAILED);
        }
    }

    @Override
    public java.math.BigDecimal getTotalHoursByTaskId(Long taskId) {
        List<WorkHour> workHours = workHourMapper.selectByTaskId(taskId);
        return workHours.stream()
                .map(WorkHour::getHours)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }
}