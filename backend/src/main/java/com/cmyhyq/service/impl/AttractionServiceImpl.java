package com.cmyhyq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmyhyq.mapper.AttractionMapper;
import com.cmyhyq.mapper.HotScenicMapper;
import com.cmyhyq.pojo.entity.Attraction;
import com.cmyhyq.service.AttractionService;
import com.cmyhyq.ai.rag.SyncService.HotDataSyncService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class AttractionServiceImpl extends ServiceImpl<AttractionMapper, Attraction> implements AttractionService {

    private final HotScenicMapper hotScenicMapper; // 加上 final 保证注入生效
    private final HotDataSyncService hotDataSyncService; // 注入联动服务
    private List<String> allAttractionNames;

    /**
     * 启动时加载一次，所有景点名称存入内存
     */
    @PostConstruct
    public void init() {
        // 启动时加载一次，存入内存
        this.allAttractionNames = this.list(new LambdaQueryWrapper<Attraction>()
                        .select(Attraction::getAttrName))
                .stream()
                .map(Attraction::getAttrName)
                .collect(Collectors.toList());
        log.info("景点名称字典已加载，共 {} 个", allAttractionNames.size());
    }

    /**
     * 获取所有景点
     * @return
     */
    @Override
    public List<String> getAllAttractionNames() {
        return allAttractionNames;
    }


    /**
     * 分页查询景点
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param keyword 关键词（景点名称）
     * @param isHot 是否热门（1=是，0=否，null=全部）
     * @return
     */
    public IPage<Attraction> pageForAdmin(Integer pageNum, Integer pageSize, String keyword, Integer isHot) {
        Page<Attraction> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Attraction> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(Attraction::getAttrName, keyword);
        }
        if (isHot != null) {
            queryWrapper.eq(Attraction::getIsHot, isHot);
        }
        queryWrapper.orderByDesc(Attraction::getAttrId);

        return this.page(page, queryWrapper);
    }

    /**
     * 增加景点
     * @param attraction
     */
    public void addAttraction(Attraction attraction) {
        if (!StringUtils.hasText(attraction.getAttrId())) {
            throw new IllegalArgumentException("景点ID不能为空");
        }
        if (!StringUtils.hasText(attraction.getAttrName())) {
            throw new IllegalArgumentException("景点名称不能为空");
        }

        Attraction exist = this.getById(attraction.getAttrId());
        if (exist != null) {
            throw new IllegalArgumentException("景点ID已存在");
        }

        if (attraction.getIsHot() == null) {
            attraction.setIsHot(0);
        }

        this.save(attraction);
        refreshAttractionNames();
        log.info("新增景点成功: attrId={}, attrName={}", attraction.getAttrId(), attraction.getAttrName());
    }

    /**
     * 更新景点
     * @param attraction
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAttraction(Attraction attraction) {
        if (!StringUtils.hasText(attraction.getAttrId())) {
            throw new IllegalArgumentException("景点ID不能为空");
        }

        Attraction exist = this.getById(attraction.getAttrId());
        if (exist == null) {
            throw new IllegalArgumentException("景点不存在");
        }



        // 更新非空字段（保护已有数据）
        if (StringUtils.hasText(attraction.getAttrName())) exist.setAttrName(attraction.getAttrName());
        if (StringUtils.hasText(attraction.getAttrType())) exist.setAttrType(attraction.getAttrType());
        if (StringUtils.hasText(attraction.getLocation())) exist.setLocation(attraction.getLocation());
        if (StringUtils.hasText(attraction.getHighlights())) exist.setHighlights(attraction.getHighlights());
        if (StringUtils.hasText(attraction.getBuildingInfo())) exist.setBuildingInfo(attraction.getBuildingInfo());
        if (StringUtils.hasText(attraction.getFeatures())) exist.setFeatures(attraction.getFeatures());
        if (StringUtils.hasText(attraction.getCulture())) exist.setCulture(attraction.getCulture());
        if (StringUtils.hasText(attraction.getDetail())) exist.setDetail(attraction.getDetail());
        if (StringUtils.hasText(attraction.getPerformanceInfo())) exist.setPerformanceInfo(attraction.getPerformanceInfo());
        if (StringUtils.hasText(attraction.getRemark())) exist.setRemark(attraction.getRemark());
        if (attraction.getIsHot() != null) exist.setIsHot(attraction.getIsHot());

        this.updateById(exist);

        // 调用联动服务：清理热门问答（MySQL+向量库）+ 热门景点统计
        hotDataSyncService.onAttractionChanged(attraction.getAttrId());


        refreshAttractionNames();
        log.info("编辑景点成功: attrId={}, newName={}", attraction.getAttrId(), exist.getAttrName());
    }
    /**
     * 删除景点
     * @param attrId
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttraction(String attrId) {
        if (!StringUtils.hasText(attrId)) {
            throw new IllegalArgumentException("景点ID不能为空");
        }

        Attraction exist = this.getById(attrId);
        if (exist == null) {
            throw new IllegalArgumentException("景点不存在");
        }

        // 删除景点实体
        this.removeById(attrId);

        // 调用联动服务：清理热门问答（MySQL+向量库）+ 热门景点统计
        hotDataSyncService.onAttractionChanged( exist.getAttrId());

        refreshAttractionNames();
        log.info("删除景点成功: attrId={}", attrId);
    }



    /**
     * 刷新内存中的景点名称列表
     */
    private void refreshAttractionNames() {
        allAttractionNames = this.list(new LambdaQueryWrapper<Attraction>()
                        .select(Attraction::getAttrName))
                .stream()
                .map(Attraction::getAttrName)
                .collect(Collectors.toList());
        log.info("景点名称字典已刷新，共 {} 个", allAttractionNames.size());
    }
}

