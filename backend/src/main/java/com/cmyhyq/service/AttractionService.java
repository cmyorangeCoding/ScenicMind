package com.cmyhyq.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cmyhyq.pojo.entity.Attraction;
import com.cmyhyq.pojo.vo.SpotGuideVO;

import java.util.List;

public interface AttractionService extends IService<Attraction> {



    /**
     * 获取所有景点名称
     * @return
     */
    List<String> getAllAttractionNames();


    /**
     * 管理端：分页查询景点
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param keyword 关键词（景点名称）
     * @param isHot 是否热门（1=是，0=否，null=全部）
     * @return 分页结果
     */
    IPage<Attraction> pageForAdmin(Integer pageNum, Integer pageSize, String keyword, Integer isHot);

    /**
     * 管理端：新增景点
     */
    void addAttraction(Attraction attraction);

    /**
     * 管理端：编辑景点
     */
    void updateAttraction(Attraction attraction);

    /**
     * 管理端：删除景点
     */
    void deleteAttraction(String attrId);



}
