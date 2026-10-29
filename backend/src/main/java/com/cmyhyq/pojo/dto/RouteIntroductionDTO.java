package com.cmyhyq.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class RouteIntroductionDTO {

    private String routeName;

    private List<String> attractions;

    private String totalDuration;



}

