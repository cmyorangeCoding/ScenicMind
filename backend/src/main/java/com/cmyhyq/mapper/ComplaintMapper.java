package com.cmyhyq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmyhyq.pojo.entity.Complaint;
import com.cmyhyq.pojo.vo.ComplaintStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ComplaintMapper extends BaseMapper<Complaint> {

    /**
     * 根据时间范围，按【投诉类型】分组统计投诉数据
     * @param startTime
     * @param endTime
     * @return
     */
    List<ComplaintStatsVO> selectComplaintStats(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

}