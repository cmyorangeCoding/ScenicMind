package com.cmyhyq.controller.user;

import com.cmyhyq.common.Result;
import com.cmyhyq.pojo.dto.TravelNoteGenerateDTO;
import com.cmyhyq.pojo.vo.TravelNoteSummaryVO;
import com.cmyhyq.pojo.vo.TravelNoteVO;
import com.cmyhyq.service.TravelNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class TravelNoteController {

    private final TravelNoteService travelNoteService;

    /**
     * 生成游记
     * @param dto 游览信息
     * @return 游记ID
     */
    @PostMapping("/travel-note/generate")
    public Result<Long> generateTravelNote(@RequestBody TravelNoteGenerateDTO dto) {
        try {
            Long noteId = travelNoteService.generateTravelNote(dto);
            return Result.success(noteId);
        } catch (Exception e) {
            return Result.error(e.getMessage() == null ? "生成游记失败" : e.getMessage());
        }
    }

    /**
     * 查询用户游记列表
     * @param userId 用户ID
     * @return 游记摘要列表
     */
    @GetMapping("/travel-note/list")
    public Result<List<TravelNoteSummaryVO>> getUserTravelNotes(@RequestParam Long userId) {
        try {
            List<TravelNoteSummaryVO> notes = travelNoteService.getUserTravelNotes(userId);
            return Result.success(notes);
        } catch (Exception e) {
            return Result.error(e.getMessage() == null ? "查询游记列表失败" : e.getMessage());
        }
    }

    /**
     * 查询游记详情
     * @param userId 用户ID
     * @param noteId 游记ID
     * @return 游记详情
     */
    @GetMapping("/travel-note/{noteId}")
    public Result<TravelNoteVO> getTravelNoteDetail(
            @PathVariable Long noteId, 
            @RequestParam Long userId) {
        try {
            TravelNoteVO detail = travelNoteService.getTravelNoteDetail(userId, noteId);
            if (detail == null) {
                return Result.error("游记不存在或无权限查看");
            }
            return Result.success(detail);
        } catch (Exception e) {
            return Result.error(e.getMessage() == null ? "查询游记详情失败" : e.getMessage());
        }
    }
}
