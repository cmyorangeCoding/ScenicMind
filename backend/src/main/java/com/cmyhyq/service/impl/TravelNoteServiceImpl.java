package com.cmyhyq.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmyhyq.ai.agent.AnalyzeAgent;
import com.cmyhyq.mapper.TravelNoteMapper;
import com.cmyhyq.pojo.dto.TravelNoteGenerateDTO;
import com.cmyhyq.pojo.dto.VisitedSpotDTO;
import com.cmyhyq.pojo.entity.Attraction;
import com.cmyhyq.pojo.entity.TravelNote;
import com.cmyhyq.pojo.entity.UserTag;
import com.cmyhyq.pojo.vo.*;
import com.cmyhyq.service.AttractionService;
import com.cmyhyq.service.TravelNoteService;
import com.cmyhyq.ai.agent.ProfileAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelNoteServiceImpl extends ServiceImpl<TravelNoteMapper, TravelNote> implements TravelNoteService {

    private final AnalyzeAgent analyzeAgent;
    private final ProfileAgent profileAgent;
    private final AttractionService attractionService;

    /**
     * 生成游记（异步方式）
     * 先保存游记记录，后台异步生成内容
     * @param dto 游览信息
     * @return 游记ID
     */
    public Long generateTravelNote(TravelNoteGenerateDTO dto) {
        // 1. 参数校验
        if (dto.getUserId() == null || dto.getVisitedSpots() == null || dto.getVisitedSpots().isEmpty()) {
            throw new IllegalArgumentException("用户ID和景点列表不能为空");
        }

        // 2. 获取用户画像标签
        List<UserTag> userTags = profileAgent.getUserTags(dto.getUserId());
        String userTagsStr = userTags.stream()
                .map(UserTag::getTag)
                .collect(Collectors.joining(", "));

        // 3. 从 MySQL 获取景点详细信息（亮点、文化等）
        List<String> spotNames = dto.getVisitedSpots().stream()
                .map(VisitedSpotDTO::getSpotName)
                .collect(Collectors.toList());
        
        List<Attraction> attractions = attractionService.lambdaQuery()
                .in(Attraction::getAttrName, spotNames)
                .list();

        // 4. 组装景点信息字符串传给 AI
        StringBuilder spotsInfo = new StringBuilder();
        for (Attraction attr : attractions) {
            spotsInfo.append(String.format("""
                    景点名称: %s
                    游玩亮点: %s
                    文化内涵: %s
                    
                    """, 
                    attr.getAttrName(),
                    attr.getHighlights(),
                    attr.getCulture()
            ));
        }

        // 5. 计算游览时长
        Duration duration = Duration.between(dto.getStartTime(), dto.getEndTime());
        double durationHours = duration.toMinutes() / 60.0;

        // 6. 先保存游记记录（状态为生成中）
        String visitedSpotsJson = JSON.toJSONString(dto.getVisitedSpots());
        TravelNote note = new TravelNote();
        note.setUserId(dto.getUserId());
        note.setTitle("生成中...");
        note.setContent("");
        note.setStartTime(dto.getStartTime());
        note.setEndTime(dto.getEndTime());
        note.setDurationHours(durationHours);
        note.setVisitedSpotsJson(visitedSpotsJson);
        note.setCreateTime(LocalDateTime.now());
        note.setStatus("PENDING");
        this.save(note);
        
        Long noteId = note.getId();
        log.info("游记记录已保存，ID: {}", noteId);
        
        // 7. 异步生成游记内容
        asyncGenerateTravelNote(noteId, userTagsStr, spotsInfo.toString(), visitedSpotsJson, 
                dto.getStartTime().toString(), dto.getEndTime().toString());
        
        return noteId;
    }
    
    /**
     * 异步生成游记内容
     */
    @Async
    public void asyncGenerateTravelNote(Long noteId, String userTagsStr, String spotsInfo, 
            String visitedSpotsJson, String startTime, String endTime) {
        // 解析实际景点数量（从JSON中获取）
        int spotCount = 0;
        try {
            JSONArray jsonArray = JSON.parseArray(visitedSpotsJson);
            spotCount = jsonArray.size();
        } catch (Exception e) {
            spotCount = 0;
        }
        log.info("开始异步生成游记，ID: {}, 用户标签长度: {}, 实际景点数量: {}", 
                noteId, userTagsStr != null ? userTagsStr.length() : 0, spotCount);
        
        try {
            // 调用 AnalyzeAgent 生成游记
            AnalyzeAgent.TravelNoteContent generated = analyzeAgent.generateTravelNote(
                    userTagsStr,
                    spotsInfo,
                    visitedSpotsJson,
                    startTime,
                    endTime
            );

            // 更新游记内容
            TravelNote note = this.getById(noteId);
            if (note != null) {
                note.setTitle(generated.getTitle());
                note.setContent(generated.getContent());
                note.setStatus("COMPLETED");
                this.updateById(note);
                log.info("游记生成完成，ID: {}", noteId);
            }
            
        } catch (Exception e) {
            log.error("异步生成游记失败，ID: {}", noteId, e);
            
            // 更新状态为失败
            TravelNote note = this.getById(noteId);
            if (note != null) {
                note.setTitle("生成失败");
                note.setContent("很抱歉，游记生成失败，请稍后重试。");
                note.setStatus("FAILED");
                this.updateById(note);
            }
        }
    }
    


    /**
     * 获取该用户游记列表
     * @param userId 用户ID
     * @return
     */
    public List<TravelNoteSummaryVO> getUserTravelNotes(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        List<TravelNote> notes = lambdaQuery()
                .eq(TravelNote::getUserId, userId)
                .orderByDesc(TravelNote::getCreateTime)
                .list();

        return notes.stream()
                .map(note -> {
                    TravelNoteSummaryVO vo = new TravelNoteSummaryVO();
                    BeanUtils.copyProperties(note, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取游记详情
     * @param userId 用户ID
     * @param noteId 游记ID
     * @return
     */
    public TravelNoteVO getTravelNoteDetail(Long userId, Long noteId) {
        if (userId == null || noteId == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        TravelNote note = lambdaQuery()
                .eq(TravelNote::getId, noteId)
                .eq(TravelNote::getUserId, userId)  // 权限校验
                .one();

        if (note == null) {
            return null;
        }

        TravelNoteVO vo = new TravelNoteVO();
        BeanUtils.copyProperties(note, vo);
        
        // 解析景点列表
        List<VisitedSpotVO> spots = JSON.parseArray(note.getVisitedSpotsJson(), VisitedSpotVO.class);
        vo.setVisitedSpots(spots);

        return vo;
    }
}
