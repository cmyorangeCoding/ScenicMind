package com.cmyhyq.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("avatar")
public class Avatar {

    @TableId(type = IdType.INPUT)
    private String avatarId;
    
    private String avatarName;
    
    private String avatarImage;
    
    private String vcn;
    
    private Integer speed;
    
    private Integer pitch;
    
    private Integer volume;
    
    private Integer status;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
