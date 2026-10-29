package com.cmyhyq.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cmyhyq.common.Result;
import com.cmyhyq.pojo.vo.AdminComplaintVO;
import com.cmyhyq.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/complaint-manage")
@RequiredArgsConstructor
@Slf4j
public class ComplaintManageController {

    private final ComplaintService complaintService;

    /**
     * 分页查询用户投诉列表
     * @param pageNum
     * @param pageSize
     * @param status
     * @param startTime
     * @param endTime
     * @return
     */
    @GetMapping("/page")
    public Result<IPage<AdminComplaintVO>> pageComplaints(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            IPage<AdminComplaintVO> page = complaintService.pageComplaintsForAdmin(pageNum, pageSize, status, startTime, endTime);
            return Result.success(page);
        } catch (Exception e) {
            log.error("查询投诉列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 处理用户投诉
     * @param id
     * @param replyContent
     * @param status
     * @return
     */
    @PutMapping("/handle")
    public Result<String> handleComplaint(
            @RequestParam Long id,
            @RequestParam(required = false) String replyContent,
            @RequestParam(required = false) Integer status) {
        try {
            complaintService.handleComplaint(id, replyContent, status);
            return Result.success("处理成功");
        } catch (Exception e) {
            log.error("处理投诉工单失败", e);
            return Result.error(e.getMessage());
        }
    }
}
