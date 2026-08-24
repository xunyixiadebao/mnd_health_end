package org.example.djiankang.front.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.djiankang.common.R;
import org.example.djiankang.front.service.GoodsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController("frontGoodsController")
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/front/goods")
@Tag(name = "商品管理")
public class GoodsController {
    private final GoodsService goodsService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询商品", description = "根据商品ID获取商品详细信息")
    public R getById(
            @Parameter(description = "商品id", required = true, example = "1")
            @NotNull(message = "id不能为空")
            @Min(value = 1, message = "id不能小于1")
            @PathVariable
            Integer id) {
        Map<String, Object> goods = goodsService.getById(id);
        return R.ok().put("goods", goods);
    }
}
