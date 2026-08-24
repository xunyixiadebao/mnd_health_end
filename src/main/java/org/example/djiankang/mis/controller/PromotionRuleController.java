package org.example.djiankang.mis.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.djiankang.common.R;
import org.example.djiankang.mis.service.PromotionRuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController("misRuleController")
@RequestMapping("/mis/rule")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "促销规则管理")
public class PromotionRuleController {
    private final PromotionRuleService promotionRuleService;

    @GetMapping("/list")
    @SaCheckPermission(value = {"ROOT", "RULE:SELECT"}, mode = SaMode.OR)
    @Operation(summary = "查看促销规则列表")
    public R list() {
        List<Map<String, Object>> ruleList = promotionRuleService.listAll();
        return R.ok().put("ruleList", ruleList);
    }
}