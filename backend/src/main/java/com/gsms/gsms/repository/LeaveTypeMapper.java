package com.gsms.gsms.repository;

import com.gsms.gsms.model.entity.LeaveType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LeaveTypeMapper {

    LeaveType selectById(@Param("id") Long id);

    LeaveType selectByCode(@Param("code") String code);

    List<LeaveType> selectByCondition(@Param("name") String name,
                                      @Param("status") Integer status);

    List<LeaveType> selectEnabled();

    int insert(LeaveType leaveType);

    int update(LeaveType leaveType);

    int deleteById(@Param("id") Long id);

    int countUsageByLeaveTypeId(@Param("leaveTypeId") Long leaveTypeId);
}
