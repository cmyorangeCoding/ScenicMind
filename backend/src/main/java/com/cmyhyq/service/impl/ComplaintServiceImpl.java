package com.cmyhyq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmyhyq.mapper.ComplaintMapper;
import com.cmyhyq.mapper.UserMapper;
import com.cmyhyq.pojo.dto.ComplaintSubmitDTO;
import com.cmyhyq.pojo.entity.Complaint;
import com.cmyhyq.pojo.entity.User;
import com.cmyhyq.pojo.vo.AdminComplaintVO;
import com.cmyhyq.pojo.vo.ComplaintVO;
import com.cmyhyq.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl extends ServiceImpl<ComplaintMapper, Complaint> implements ComplaintService {

    private final UserMapper userMapper;

    private static final Map<Integer, String> STATUS_TEXT_MAP = new HashMap<>() {{
        put(0, "待处理");
        put(1, "处理中");
        put(2, "已处理");
        put(3, "已关闭");
    }};


    @Override
    public Long submitComplaint(ComplaintSubmitDTO dto) {
        // 参数校验
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (dto.getComplaintType() == null || dto.getComplaintType().isBlank()) {
            throw new IllegalArgumentException("投诉类型不能为空");
        }
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new IllegalArgumentException("投诉内容不能为空");
        }

        // 构建投诉对象
        Complaint complaint = new Complaint();
        complaint.setUserId(dto.getUserId());
        complaint.setComplaintType(dto.getComplaintType());
        complaint.setContent(dto.getContent());
        complaint.setStatus(0); // 默认待处理
        complaint.setCreateTime(LocalDateTime.now());
        complaint.setUpdateTime(LocalDateTime.now());

        // 保存
        save(complaint);
        
        log.info("用户 {} 提交投诉成功，投诉ID: {}", dto.getUserId(), complaint.getId());
        return complaint.getId();
    }

    @Override
    public List<ComplaintVO> getUserComplaints(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        // 查询该用户的所有投诉，按创建时间倒序
        List<Complaint> complaints = lambdaQuery()
                .eq(Complaint::getUserId, userId)
                .orderByDesc(Complaint::getCreateTime)
                .list();

        // 转换为 VO
        return complaints.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private ComplaintVO convertToVO(Complaint complaint) {
        ComplaintVO vo = new ComplaintVO();
        BeanUtils.copyProperties(complaint, vo);
        return vo;
    }

    /**
     * 管理端：分页查询投诉列表
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param status 状态筛选（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return
     */
    public IPage<AdminComplaintVO> pageComplaintsForAdmin(Integer pageNum, Integer pageSize, Integer status, String startTime, String endTime) {
        Page<Complaint> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Complaint> queryWrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            queryWrapper.eq(Complaint::getStatus, status);
        }
        if (StringUtils.hasText(startTime)) {
            queryWrapper.ge(Complaint::getCreateTime, LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (StringUtils.hasText(endTime)) {
            queryWrapper.le(Complaint::getCreateTime, LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        queryWrapper.orderByDesc(Complaint::getCreateTime);

        IPage<Complaint> complaintPage = this.page(page, queryWrapper);

        IPage<AdminComplaintVO> voPage = new Page<>(complaintPage.getCurrent(), complaintPage.getSize(), complaintPage.getTotal());
        voPage.setRecords(complaintPage.getRecords().stream().map(complaint -> {
            AdminComplaintVO vo = new AdminComplaintVO();
            BeanUtils.copyProperties(complaint, vo);
            vo.setStatusText(STATUS_TEXT_MAP.getOrDefault(complaint.getStatus(), "未知"));

            User user = userMapper.selectById(complaint.getUserId());
            if (user != null) {
                vo.setUserPhone(user.getPhone());
            }

            return vo;
        }).toList());

        return voPage;
    }

    /**
     * 管理端：处理投诉工单
     * @param id 工单ID
     * @param replyContent 回复内容
     * @param status 新状态
     */
    public void handleComplaint(Long id, String replyContent, Integer status) {
        if (id == null) {
            throw new IllegalArgumentException("工单ID不能为空");
        }

        Complaint complaint = this.getById(id);
        if (complaint == null) {
            throw new IllegalArgumentException("工单不存在");
        }

        if (StringUtils.hasText(replyContent)) {
            complaint.setReplyContent(replyContent);
        }

        if (status != null) {
            complaint.setStatus(status);
        }

        complaint.setUpdateTime(LocalDateTime.now());
        this.updateById(complaint);

        log.info("管理员处理投诉工单成功，工单ID: {}, 新状态: {}", id, complaint.getStatus());
    }
}


