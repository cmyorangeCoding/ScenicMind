package com.cmyhyq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmyhyq.pojo.entity.ChatHistory;
import com.cmyhyq.pojo.vo.SatisfactionTrendVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

    Long countDistinctUsers(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    Long countChats(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    List<SatisfactionTrendVO> selectSatisfactionTrend(@Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);

    List<Map<String, Object>> selectEmotionStats(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);
}