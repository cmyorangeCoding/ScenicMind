package com.cmyhyq.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cmyhyq.pojo.dto.ComplaintSubmitDTO;
import com.cmyhyq.pojo.entity.Complaint;
import com.cmyhyq.pojo.vo.AdminComplaintVO;
import com.cmyhyq.pojo.vo.ComplaintVO;

import java.util.List;

public interface ComplaintService extends IService<Complaint> {

    /**
     * 提交投诉
     * @param dto 投诉信息
     * @return 投诉ID
     */
    Long submitComplaint(ComplaintSubmitDTO dto);

    /**
     * 查询用户的投诉列表
     * @param userId 用户ID
     * @return 投诉列表
     */
    List<ComplaintVO> getUserComplaints(Long userId);

    /**
     * 管理端：分页查询投诉列表
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param status 状态筛选（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 投诉列表分页
     */
    IPage<AdminComplaintVO> pageComplaintsForAdmin(Integer pageNum, Integer pageSize, Integer status, String startTime, String endTime);

    /**
     * 管理端：处理投诉工单
     * @param id 工单ID
     * @param replyContent 回复内容
     * @param status 新状态
     */
    void handleComplaint(Long id, String replyContent, Integer status);
}



