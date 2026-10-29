package com.cmyhyq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmyhyq.pojo.entity.HotScenic;
import com.cmyhyq.pojo.vo.HotScenicVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HotScenicMapper extends BaseMapper<HotScenic> {

    int upsertInc(@Param("scenicId") String scenicId, @Param("scenicName") String scenicName, @Param("statDate") LocalDate statDate);

    /**
     * 管理端：按时间范围查询热门景点
     */
    List<HotScenicVO> selectTopScenicsByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("limit") int limit);

    /**
     * 用户端/管理端总榜：查询历史总榜 TopN（不考虑时间）
     */
    List<HotScenicVO> selectTopScenicsAllTime(@Param("limit") int limit);

    /**
     * 物理删除该景点的所有历史统计（用于景点被删除时）
     * @param scenicId 景点ID
     */
    void deleteByScenicId(@Param("scenicId") String scenicId);

    /**
     * 同步更新热门景点名称（用于景点改名时）
     * @param scenicId 景点ID
     * @param scenicName 新名称
     */
    void updateScenicName(@Param("scenicId") String scenicId, @Param("scenicName") String scenicName);

}