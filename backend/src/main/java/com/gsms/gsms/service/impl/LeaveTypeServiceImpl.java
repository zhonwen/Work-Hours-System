package com.gsms.gsms.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.gsms.gsms.dto.leavetype.LeaveTypeCreateReq;
import com.gsms.gsms.dto.leavetype.LeaveTypeInfoResp;
import com.gsms.gsms.dto.leavetype.LeaveTypeUpdateReq;
import com.gsms.gsms.infra.common.PageResult;
import com.gsms.gsms.infra.exception.BusinessException;
import com.gsms.gsms.infra.utils.UserContext;
import com.gsms.gsms.model.entity.LeaveType;
import com.gsms.gsms.model.errorcode.LeaveTypeErrorCode;
import com.gsms.gsms.repository.LeaveTypeMapper;
import com.gsms.gsms.service.LeaveTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveTypeServiceImpl implements LeaveTypeService {

    private final LeaveTypeMapper leaveTypeMapper;

    public LeaveTypeServiceImpl(LeaveTypeMapper leaveTypeMapper) {
        this.leaveTypeMapper = leaveTypeMapper;
    }

    @Override
    public LeaveTypeInfoResp getById(Long id) {
        LeaveType entity = leaveTypeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(LeaveTypeErrorCode.LEAVE_TYPE_NOT_FOUND);
        }
        return LeaveTypeInfoResp.from(entity);
    }

    @Override
    public PageResult<LeaveTypeInfoResp> findAll(String name, Integer status, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        List<LeaveType> list = leaveTypeMapper.selectByCondition(name, status);
        PageInfo<LeaveType> pageInfo = new PageInfo<>(list);
        List<LeaveTypeInfoResp> respList = list.stream().map(LeaveTypeInfoResp::from).collect(Collectors.toList());
        return PageResult.success(respList, pageInfo.getTotal(), pageInfo.getPageNum(), pageInfo.getPageSize());
    }

    @Override
    public List<LeaveTypeInfoResp> listEnabled() {
        return leaveTypeMapper.selectEnabled().stream()
                .map(LeaveTypeInfoResp::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveTypeInfoResp create(LeaveTypeCreateReq req) {
        if (leaveTypeMapper.selectByCode(req.getCode()) != null) {
            throw new BusinessException(LeaveTypeErrorCode.LEAVE_TYPE_CODE_EXISTS);
        }
        Long userId = UserContext.getCurrentUserId();
        LeaveType entity = new LeaveType();
        entity.setName(req.getName());
        entity.setCode(req.getCode());
        entity.setSort(req.getSort() == null ? 0 : req.getSort());
        entity.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        entity.setRemark(req.getRemark());
        entity.setCreateUserId(userId);
        entity.setUpdateUserId(userId);
        if (leaveTypeMapper.insert(entity) <= 0) {
            throw new BusinessException(LeaveTypeErrorCode.LEAVE_TYPE_CREATE_FAILED);
        }
        return LeaveTypeInfoResp.from(leaveTypeMapper.selectById(entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveTypeInfoResp update(LeaveTypeUpdateReq req) {
        LeaveType exist = leaveTypeMapper.selectById(req.getId());
        if (exist == null) {
            throw new BusinessException(LeaveTypeErrorCode.LEAVE_TYPE_NOT_FOUND);
        }
        if (req.getCode() != null && !req.getCode().isEmpty()) {
            LeaveType byCode = leaveTypeMapper.selectByCode(req.getCode());
            if (byCode != null && !byCode.getId().equals(req.getId())) {
                throw new BusinessException(LeaveTypeErrorCode.LEAVE_TYPE_CODE_EXISTS);
            }
        }
        LeaveType entity = new LeaveType();
        entity.setId(req.getId());
        entity.setName(req.getName());
        entity.setCode(req.getCode());
        entity.setSort(req.getSort());
        entity.setStatus(req.getStatus());
        entity.setRemark(req.getRemark());
        entity.setUpdateUserId(UserContext.getCurrentUserId());
        if (leaveTypeMapper.update(entity) <= 0) {
            throw new BusinessException(LeaveTypeErrorCode.LEAVE_TYPE_UPDATE_FAILED);
        }
        return LeaveTypeInfoResp.from(leaveTypeMapper.selectById(req.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        LeaveType exist = leaveTypeMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(LeaveTypeErrorCode.LEAVE_TYPE_NOT_FOUND);
        }
        if (leaveTypeMapper.countUsageByLeaveTypeId(id) > 0) {
            throw new BusinessException(LeaveTypeErrorCode.LEAVE_TYPE_IN_USE);
        }
        if (leaveTypeMapper.deleteById(id) <= 0) {
            throw new BusinessException(LeaveTypeErrorCode.LEAVE_TYPE_DELETE_FAILED);
        }
    }
}
