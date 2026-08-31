package org.example.djiankang.front.service;

import org.example.djiankang.common.PageResult;

import java.util.List;
import java.util.Map;

public interface GoodsService {
    /**
     * 根据商品id查询商品信息
     *
     * @param id 商品id
     * @return 商品信息
     */
    Map<String, Object> getById(Integer id);
    /**
     * 获取每个分区销量排名前4的套餐数据
     *
     * @param categoryIds 分区id数组
     * @return 套餐列表
     */
    Map<Integer, List<Map<String, Object>>> getTop4ByCategoryIds(Integer[] categoryIds);
    /**
     * 业务端：分页查询套餐信息
     *
     * @param param 查询条件
     * @return 分页对象
     */
    PageResult<Map<String, Object>> queryPage(Map<String, Object> param);
}