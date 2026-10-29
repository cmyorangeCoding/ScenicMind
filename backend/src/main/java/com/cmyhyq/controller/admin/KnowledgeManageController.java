package com.cmyhyq.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cmyhyq.common.Result;
import com.cmyhyq.pojo.dto.KnowledgeDocDTO;
import com.cmyhyq.pojo.vo.KnowledgeDocVO;
import com.cmyhyq.service.KnowledgeDocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/knowledge")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeManageController {

    private final KnowledgeDocService knowledgeDocService;

    /**
     * 分页查询知识库（支持分类、标签、关键词筛选）
     */
    @GetMapping("/page")
    public Result<IPage<KnowledgeDocVO>> pageKnowledge(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword) {
        try {
            IPage<KnowledgeDocVO> page = knowledgeDocService.pageForAdmin(pageNum, pageSize, category, tag, keyword);
            return Result.success(page);
        } catch (Exception e) {
            log.error("查询知识库失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增知识
     */
    @PostMapping
    public Result<String> addKnowledge(@RequestBody KnowledgeDocDTO dto) {
        try {
            knowledgeDocService.addDoc(dto);
            return Result.success("新增成功");
        } catch (Exception e) {
            log.error("新增知识失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 编辑知识
     */
    @PutMapping
    public Result<String> updateKnowledge(@RequestBody KnowledgeDocDTO dto) {
        try {
            knowledgeDocService.updateDoc(dto);
            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("编辑知识失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除知识
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteKnowledge(@PathVariable Long id) {
        try {
            knowledgeDocService.deleteDoc(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除知识失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量删除知识
     */
    @PostMapping("/batch-delete")
    public Result<String> batchDelete(@RequestBody List<Long> ids) {
        try {
            knowledgeDocService.batchDelete(ids);
            return Result.success("批量删除成功");
        } catch (Exception e) {
            log.error("批量删除知识失败", e);
            return Result.error(e.getMessage());
        }
    }
}
