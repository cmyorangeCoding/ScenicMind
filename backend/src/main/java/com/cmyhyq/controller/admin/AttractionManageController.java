package com.cmyhyq.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cmyhyq.common.Result;
import com.cmyhyq.pojo.entity.Attraction;
import com.cmyhyq.service.AttractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/attraction")
@RequiredArgsConstructor
@Slf4j
public class AttractionManageController {

    private final AttractionService attractionService;

    /**
     * 分页查询景点（支持关键词、热门筛选）
     */
    @GetMapping("/page")
    public Result<IPage<Attraction>> pageAttraction(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer isHot) {
        try {
            IPage<Attraction> page = attractionService.pageForAdmin(pageNum, pageSize, keyword, isHot);
            return Result.success(page);
        } catch (Exception e) {
            log.error("查询景点列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增景点
     */
    @PostMapping
    public Result<String> addAttraction(@RequestBody Attraction attraction) {
        try {
            attractionService.addAttraction(attraction);
            return Result.success("新增成功");
        } catch (Exception e) {
            log.error("新增景点失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 编辑景点
     */
    @PutMapping
    public Result<String> updateAttraction(@RequestBody Attraction attraction) {
        try {
            attractionService.updateAttraction(attraction);
            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("编辑景点失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除景点
     */
    @DeleteMapping("/{attrId}")
    public Result<String> deleteAttraction(@PathVariable String attrId) {
        try {
            attractionService.deleteAttraction(attrId);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除景点失败", e);
            return Result.error(e.getMessage());
        }
    }
    
}
