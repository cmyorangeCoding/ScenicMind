package com.cmyhyq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cmyhyq.pojo.entity.HotQuestion;
import com.cmyhyq.pojo.vo.HotQuestionVO;

import java.time.LocalDate;
import java.util.List;

public interface HotQuestionService extends IService<HotQuestion> {

    /**
     * 记录热门问题（仅MySQL统计，不写入PGVector）
     * @param standardQuestion 标准问题
     * @param statDate 统计日期
     */
    void recordHotTopic(String standardQuestion, LocalDate statDate);

    /**
     * 添加缓存候选（写入PGVector，status=pending）
     * @param question 问题
     * @param answer 答案
     * @param sourceType 来源类型（knowledge_doc/attraction）
     * @param sourceId 来源ID
     */
    void addCacheCandidate(String question, String answer, String sourceType, String sourceId);


    /**
     * 获取待审核的缓存候选列表（从PGVector查询）
     * @return 待审核的缓存列表
     */
    List<HotQuestionVO> getPendingCaches();


    /**
     * 审核通过缓存候选（将PGVector中的status改为approved）
     * @param question 问题内容
     */
    void approveCache(String question);

    /**
     * 审核拒绝缓存候选（删除PGVector中的记录）
     * @param question 问题内容
     */
    void rejectCache(String question);


    /**
     * 尝试从缓存命中热门问答
     * @param message 用户提问
     * @return 命中返回 VO，未命中返回 null
     */
    HotQuestionVO tryHitCache(String message);

    /**
     * 用户端：获取历史总榜 Top 10 热门问答
     * @return Top 10 热门问答
     */
    List<HotQuestionVO> getTopHotQuestions();

    /**
     * 管理端：获取指定时间段的热门问题
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return Top 10 热门问答
     */
    List<HotQuestionVO> getTopHotQuestionsByPeriod(LocalDate startDate, LocalDate endDate);

    /**
     * 缓存命中后记录频次（仅MySQL统计）
     * @param question 标准问题
     */
    void recordCacheHit(String question);

    /**
     * 兼容旧调用：保持接口兼容
     * @deprecated 使用 recordHotTopic 和 addCacheCandidate 代替
     */
    @Deprecated
    void touch(String standardQuestion, String standardAnswer, LocalDate statDate, String sourceType, String sourceId);

    /**
     * 根据问题内容删除所有相关统计记录
     * @param question 问题内容
     */
    void deleteByQuestion(String question);

}
