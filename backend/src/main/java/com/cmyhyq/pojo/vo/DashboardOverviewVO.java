package com.cmyhyq.pojo.vo;


import lombok.Data;
import java.util.List;

@Data
public class DashboardOverviewVO {
    
    /**
     * 服务人次
     */
    private Long visitorCount;
    
    /**
     * 对话次数
     */
    private Long chatCount;
    
    /**
     * 热门问题 TOP10
     */
    private List<HotQuestionVO> topQuestions;
    
    /**
     * 热门景点 TOP5
     */
    private List<HotScenicVO> topScenics;

}
