package com.cmyhyq.pojo.vo;



import lombok.Data;

@Data
public class HotScenicVO {
    
    /**
     * 景点ID
     */
    private String scenicId;
    
    /**
     * 景点名称
     */
    private String scenicName;
    
    /**
     * 咨询次数
     */
    private Integer count;
}
