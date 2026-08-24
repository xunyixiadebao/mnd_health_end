package org.example.djiankang.mis.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.djiankang.db.mapper.PromotionRuleMapper;
import org.example.djiankang.mis.service.PromotionRuleService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
@Service
@RequiredArgsConstructor
public class PromotionRuleServiceImpl implements PromotionRuleService {

    private final PromotionRuleMapper promotionRuleMapper;

    @Override
    public List<Map<String, Object>> listAll() {
        return promotionRuleMapper.selectAll();
    }
}
