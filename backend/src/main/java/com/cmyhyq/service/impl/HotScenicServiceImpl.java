package com.cmyhyq.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmyhyq.mapper.HotScenicMapper;
import com.cmyhyq.pojo.entity.HotScenic;
import com.cmyhyq.service.HotScenicService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class HotScenicServiceImpl extends ServiceImpl<HotScenicMapper, HotScenic> implements HotScenicService {

    /**
     * 更新热门景点频次
     * @param scenicId
     * @param scenicName
     * @param statDate
     */
    public void touch(String scenicId, String scenicName, LocalDate statDate) {
        baseMapper.upsertInc(scenicId, scenicName, statDate);
    }
}
