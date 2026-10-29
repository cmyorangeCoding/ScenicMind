package com.cmyhyq.pojo.dto;



import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class AvatarDTO {
    
    private String avatarId;
    
    private String avatarName;
    
    private MultipartFile avatarImage;
    
    private String vcn;
    
    private Integer speed;
    
    private Integer pitch;
    
    private Integer volume;
    
    private Integer status;
}
