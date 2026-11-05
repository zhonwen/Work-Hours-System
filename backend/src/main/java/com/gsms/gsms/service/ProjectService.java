package com.gsms.gsms.service;

import com.gsms.gsms.dto.project.ProjectAnnouncementUpdateReq;
import com.gsms.gsms.dto.project.ProjectQueryReq;
import com.gsms.gsms.dto.project.ProjectCreateReq;
import com.gsms.gsms.dto.project.ProjectUpdateReq;
import com.gsms.gsms.dto.project.ProjectInfoResp;
import com.gsms.gsms.infra.common.PageResult;

/**
 * 项目服务接口
 */
public interface ProjectService {
    /**
     * 根据ID查询项目
     * @param id 项目ID
     * @return 项目响应DTO
     */
    ProjectInfoResp getById(Long id);

    /**
     * 根据条件分页查询项目
     * @param req 查询条件
     * @return 分页结果
     */
    PageResult<ProjectInfoResp> findAll(ProjectQueryReq req);

    /**
     * 创建项目
     * @param createReq 创建请求DTO
     * @return 创建成功的项目响应DTO
     */
    ProjectInfoResp create(ProjectCreateReq createReq);

    /**
     * 更新项目
     * @param updateReq 更新请求DTO
     * @return 更新后的项目响应DTO
     */
    ProjectInfoResp update(ProjectUpdateReq updateReq);

    /**
     * 更新项目公告 / 规划板
     * @param projectId 项目ID
     * @param req 公告内容
     * @return 更新后的项目详情
     */
    ProjectInfoResp updateAnnouncement(Long projectId, ProjectAnnouncementUpdateReq req);

    /**
     * 删除项目
     * @param id 项目ID
     */
    void delete(Long id);
}