package org.example.djiankang.db.mapper;

import java.util.List;
import java.util.Map;

/**
* @author zl
* @description 针对表【promotion_rule(促销规则表)】的数据库操作Mapper
* @createDate 2026-08-17 20:34:37
* @Entity org.example.djiankang.db.entity.PromotionRuleEntity
*/
public interface PromotionRuleMapper {

    /**
     * 查询所有的促销规则
     * @return 规则列表
     */
    List<Map<String, Object>> selectAll();
}




