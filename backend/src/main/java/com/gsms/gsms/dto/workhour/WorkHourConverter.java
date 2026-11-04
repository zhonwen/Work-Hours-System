package com.gsms.gsms.dto.workhour;

import com.gsms.gsms.model.entity.WorkHour;
import com.gsms.gsms.model.enums.WorkHourRecordType;
import com.gsms.gsms.model.enums.WorkHourStatus;

public class WorkHourConverter {

    public static WorkHour toWorkHour(WorkHourCreateReq req) {
        if (req == null) {
            return null;
        }
        WorkHour workHour = new WorkHour();
        WorkHourRecordType recordType = req.getRecordType() != null ? req.getRecordType() : WorkHourRecordType.NORMAL;
        workHour.setRecordType(recordType);
        if (recordType.isLeave()) {
            workHour.setProjectId(null);
            workHour.setTaskId(null);
            workHour.setLeaveTypeId(req.getLeaveTypeId());
        } else {
            workHour.setProjectId(req.getProjectId());
            workHour.setTaskId(req.getTaskId());
            workHour.setLeaveTypeId(null);
        }
        workHour.setWorkDate(req.getWorkDate());
        workHour.setHours(req.getHours());
        workHour.setContent(req.getContent());
        workHour.setStatus(req.getStatus() != null ? req.getStatus() : WorkHourStatus.SAVED);
        return workHour;
    }

    public static WorkHour toWorkHour(WorkHourUpdateReq req) {
        if (req == null) {
            return null;
        }
        WorkHour workHour = new WorkHour();
        workHour.setId(req.getId());
        WorkHourRecordType recordType = req.getRecordType() != null ? req.getRecordType() : WorkHourRecordType.NORMAL;
        workHour.setRecordType(recordType);
        if (recordType.isLeave()) {
            workHour.setProjectId(null);
            workHour.setTaskId(null);
            workHour.setLeaveTypeId(req.getLeaveTypeId());
        } else {
            workHour.setProjectId(req.getProjectId());
            workHour.setTaskId(req.getTaskId());
            workHour.setLeaveTypeId(null);
        }
        workHour.setWorkDate(req.getWorkDate());
        workHour.setHours(req.getHours());
        workHour.setContent(req.getContent());
        workHour.setStatus(req.getStatus());
        return workHour;
    }
}
