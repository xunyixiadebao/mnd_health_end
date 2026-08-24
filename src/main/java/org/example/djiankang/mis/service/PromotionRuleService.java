package org.example.djiankang.mis.service;

import java.util.List;
import java.util.Map;

public interface PromotionRuleService {
    /**
     * 查询所有促销规则
     *
     * @return 规则列表
     */
    List<Map<String, Object>> listAll();
}
