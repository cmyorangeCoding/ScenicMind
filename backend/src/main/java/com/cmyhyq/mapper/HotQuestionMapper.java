package com.cmyhyq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmyhyq.pojo.entity.HotQuestion;
import com.cmyhyq.pojo.vo.HotQuestionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HotQuestionMapper extends BaseMapper<HotQuestion> {

    /**
     * 热门问题统计（仅MySQL，仅记录问题和频次）
     * 只插入3个字段：standard_question, count, stat_date
     */
    int upsertHotQuestion(@Param("q") String q, @Param("date") LocalDate date);

    /**
     * 管理端：按时间范围查询热门问题
     * 只返回问题和总频次，不返回答案
     */
    List<HotQuestionVO> selectTop10ByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 用户端/管理端总榜：查询历史总榜 Top10（不考虑时间，累加所有频次）
     * 只返回问题和总频次，不返回答案
     */
    List<HotQuestionVO> selectTop10AllTime();

    /**
     * 根据问题内容删除所有相关统计记录
     * @param question 问题内容
     */
    int deleteByQuestion(@Param("question") String question);

}