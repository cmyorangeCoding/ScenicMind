package com.cmyhyq.controller.admin;

import com.cmyhyq.common.Result;
import com.cmyhyq.pojo.vo.HotQuestionVO;
import com.cmyhyq.service.HotQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/hot-question")
@RequiredArgsConstructor
public class HotQuestionManageController {

    private final HotQuestionService hotQuestionService;

    /**
     * 获取热门问题列表
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 热门问题列表
     */
    @GetMapping("/list")
    public Result<List<HotQuestionVO>> list(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        List<HotQuestionVO> list;
        if (startDate != null && endDate != null) {
            list = hotQuestionService.getTopHotQuestionsByPeriod(startDate, endDate);
        } else {
            list = hotQuestionService.getTopHotQuestions();
        }
        return Result.success(list);
    }

    /**
     * 获取待审核的缓存候选列表（从PGVector查询）
     * @return 待审核的缓存列表
     */
    @GetMapping("/pending")
    public Result<List<HotQuestionVO>> getPendingCaches() {
        List<HotQuestionVO> list = hotQuestionService.getPendingCaches();
        return Result.success(list);
    }

    /**
     * 审核通过缓存（将PGVector中的status改为approved）
     * @param question 问题内容（作为唯一标识）
     * @return 操作结果
     */
    @PostMapping("/approve")
    public Result<Void> approve(@RequestParam("question") String question) {
        log.info("收到同意请求，question={}", question);
        hotQuestionService.approveCache(question);
        return Result.success(null);
    }

    /**
     * 审核拒绝缓存（删除PGVector中的记录）
     * @param question 问题内容（作为唯一标识）
     * @return 操作结果
     */
    @PostMapping("/reject")
    public Result<Void> reject(@RequestParam("question") String question) {
        hotQuestionService.rejectCache(question);
        return Result.success(null);
    }

    /**
     * 删除热门问题记录（仅删除MySQL统计，不清理PGVector）
     * @param question 问题内容
     * @return 操作结果
     */
    @DeleteMapping
    public Result delete(@RequestParam("question") String question) {
        hotQuestionService.deleteByQuestion(question);
        return Result.success(null);
    }

}
