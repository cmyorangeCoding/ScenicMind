package com.cmyhyq.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("attraction")
public class Attraction {


    @TableId
    private String attrId;
    private String attrName;
    private String attrType;
    private String location;
    private String highlights;
    private String buildingInfo;
    private String features;
    private String culture;
    private String detail;
    private String performanceInfo;
    private String remark;
    //是否热门景点 1=是、0=否
    private Integer isHot;
}