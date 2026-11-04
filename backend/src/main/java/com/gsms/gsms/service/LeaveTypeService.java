package com.gsms.gsms.service;

import com.gsms.gsms.dto.leavetype.LeaveTypeCreateReq;
import com.gsms.gsms.dto.leavetype.LeaveTypeInfoResp;
import com.gsms.gsms.dto.leavetype.LeaveTypeUpdateReq;
import com.gsms.gsms.infra.common.PageResult;

import java.util.List;

public interface LeaveTypeService {

    LeaveTypeInfoResp getById(Long id);

    PageResult<LeaveTypeInfoResp> findAll(String name, Integer status, Integer pageNum, Integer pageSize);

    List<LeaveTypeInfoResp> listEnabled();

    LeaveTypeInfoResp create(LeaveTypeCreateReq req);

    LeaveTypeInfoResp update(LeaveTypeUpdateReq req);

    void delete(Long id);
}
