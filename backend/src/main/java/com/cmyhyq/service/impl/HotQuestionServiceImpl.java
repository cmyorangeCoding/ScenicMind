package com.cmyhyq.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmyhyq.ai.rag.HotQuestionMatch;
import com.cmyhyq.ai.rag.HotQuestionVectorService;
import com.cmyhyq.mapper.HotQuestionMapper;
import com.cmyhyq.pojo.entity.HotQuestion;
import com.cmyhyq.pojo.vo.HotQuestionVO;
import com.cmyhyq.service.HotQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class HotQuestionServiceImpl extends ServiceImpl<HotQuestionMapper, HotQuestion> implements HotQuestionService {

    private final HotQuestionVectorService hotQuestionVectorService;

    /**
     * 记录热门问题（仅MySQL统计，不写入PGVector）
     */
    @Override
    public void recordHotTopic(String question, LocalDate date) {
        if (question == null || question.isBlank()) {
            return;
        }
        baseMapper.upsertHotQuestion(question, date);
        log.info("热门问题已统计入库: {}", question);
    }

    /**
     * 添加缓存候选（写入PGVector，status=pending）
     */
    @Override
    public void addCacheCandidate(String question, String answer, String sourceType, String sourceId) {
        if (question == null || question.isBlank() || answer == null || answer.isBlank()) {
            return;
        }
        // 写入PGVector，status=pending
        hotQuestionVectorService.upsert(question, answer, sourceType, sourceId);
        log.info("缓存候选已写入PGVector: {}", question);
    }


    /**
     * 获取待审核的缓存候选列表（从PGVector查询）
     */
    @Override
    public List<HotQuestionVO> getPendingCaches() {
        List<HotQuestionMatch> matches = hotQuestionVectorService.getPendingCaches();
        return matches.stream()
                .map(match -> {
                    HotQuestionVO vo = new HotQuestionVO();
                    vo.setQuestion(match.standardQuestion());
                    vo.setAnswer(match.standardAnswer());
                    vo.setTotalCount(null); // 待审核缓存没有频次统计
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 审核通过缓存候选（将PGVector中的status改为approved）
     */
    @Override
    public void approveCache(String question) {
        hotQuestionVectorService.approve(question);
    }

    /**
     * 审核拒绝缓存候选（删除PGVector中的记录）
     */
    @Override
    public void rejectCache(String question) {
        hotQuestionVectorService.reject(question);
    }


    /**
     * 尝试从向量库缓存中命中热门问答
     */
    @Override
    public HotQuestionVO tryHitCache(String message) {
        HotQuestionMatch match = hotQuestionVectorService.tryMatch(message);
        if (match == null) {
            return null;
        }
        HotQuestionVO vo = new HotQuestionVO();
        vo.setQuestion(match.standardQuestion());
        vo.setAnswer(match.standardAnswer());
        return vo;
    }

    /**
     * 用户端：获取历史总榜 Top 10 热门问题
     */
    @Override
    public List<HotQuestionVO> getTopHotQuestions() {
        return baseMapper.selectTop10AllTime();
    }

    /**
     * 管理端：获取指定时间段的热门问题
     */
    @Override
    public List<HotQuestionVO> getTopHotQuestionsByPeriod(LocalDate startDate, LocalDate endDate) {
        return baseMapper.selectTop10ByDateRange(startDate, endDate);
    }

    /**
     * 缓存命中后记录频次（仅MySQL统计）
     */
    @Override
    public void recordCacheHit(String question) {
        if (question == null || question.isBlank()) {
            return;
        }
        baseMapper.upsertHotQuestion(question, LocalDate.now());
    }

    /**
     * 兼容旧调用：保持接口兼容
     * @deprecated 使用 recordHotTopic 和 addCacheCandidate 代替
     */
    @Deprecated
    @Override
    public void touch(String question, String answer, LocalDate date, String sourceType, String sourceId) {
        // 只统计，不写入PGVector
        recordHotTopic(question, date);
    }

    /**
     * 根据问题内容删除所有相关统计记录
     */
    @Override
    public void deleteByQuestion(String question) {
        if (question == null || question.isBlank()) {
            return;
        }
        baseMapper.deleteByQuestion(question);
        log.info("已删除热门问题统计记录: {}", question);
    }

}
