package com.cmyhyq.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class PageVO<T> {
    private Long total;
    private List<T> list;
}

