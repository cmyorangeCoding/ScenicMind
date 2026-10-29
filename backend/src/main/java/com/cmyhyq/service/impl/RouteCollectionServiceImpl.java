package com.cmyhyq.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmyhyq.mapper.RouteCollectionMapper;
import com.cmyhyq.pojo.entity.RouteCollection;
import com.cmyhyq.service.RouteCollectionService;
import org.springframework.stereotype.Service;

@Service
public class RouteCollectionServiceImpl extends ServiceImpl<RouteCollectionMapper, RouteCollection> implements RouteCollectionService {
}

