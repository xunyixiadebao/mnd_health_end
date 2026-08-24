package org.example.djiankang.front.service;

import java.util.Map;

public interface GoodsService {
    /**
     * 根据商品id查询商品信息
     *
     * @param id 商品id
     * @return 商品信息
     */
    Map<String, Object> getById(Integer id);
}