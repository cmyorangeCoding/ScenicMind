package com.cmyhyq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cmyhyq.pojo.dto.TravelNoteGenerateDTO;
import com.cmyhyq.pojo.entity.TravelNote;
import com.cmyhyq.pojo.vo.TravelNoteSummaryVO;
import com.cmyhyq.pojo.vo.TravelNoteVO;

import java.util.List;

public interface TravelNoteService extends IService<TravelNote> {

    /**
     * 生成游记
     * @param dto 游览信息
     * @return 游记ID
     */
    Long generateTravelNote(TravelNoteGenerateDTO dto);

    /**
     * 查询用户游记列表
     * @param userId 用户ID
     * @return 游记摘要列表
     */
    List<TravelNoteSummaryVO> getUserTravelNotes(Long userId);

    /**
     * 查询游记详情
     * @param userId 用户ID
     * @param noteId 游记ID
     * @return 游记详情
     */
    TravelNoteVO getTravelNoteDetail(Long userId, Long noteId);
}
