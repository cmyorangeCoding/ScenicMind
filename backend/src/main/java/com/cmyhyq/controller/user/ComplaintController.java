package com.cmyhyq.controller.user;

import com.cmyhyq.common.Result;
import com.cmyhyq.pojo.dto.ComplaintSubmitDTO;
import com.cmyhyq.pojo.vo.ComplaintVO;
import com.cmyhyq.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    /**
     * 提交投诉
     * @param dto 投诉信息
     * @return 投诉ID
     */
    @PostMapping("/complaint/submit")
    public Result<Long> submitComplaint(@RequestBody ComplaintSubmitDTO dto) {
        try {
            Long complaintId = complaintService.submitComplaint(dto);
            return Result.success(complaintId);
        } catch (Exception e) {
            return Result.error(e.getMessage() == null ? "提交投诉失败" : e.getMessage());
        }
    }

    /**
     * 查询用户的投诉列表
     * @param userId 用户ID
     * @return 投诉列表
     */
    @GetMapping("/complaint/list")
    public Result<List<ComplaintVO>> getUserComplaints(@RequestParam Long userId) {
        try {
            List<ComplaintVO> complaints = complaintService.getUserComplaints(userId);
            return Result.success(complaints);
        } catch (Exception e) {
            return Result.error(e.getMessage() == null ? "查询投诉列表失败" : e.getMessage());
        }
    }
}
