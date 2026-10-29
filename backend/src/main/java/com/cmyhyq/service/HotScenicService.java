package com.cmyhyq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cmyhyq.pojo.entity.HotScenic;

import java.time.LocalDate;

public interface HotScenicService extends IService<HotScenic> {

    void touch(String scenicId, String scenicName, LocalDate statDate);
}
