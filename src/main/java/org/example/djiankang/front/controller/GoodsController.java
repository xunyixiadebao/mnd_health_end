package org.example.djiankang.front.controller;

import cn.hutool.core.bean.BeanUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.djiankang.common.PageResult;
import org.example.djiankang.common.R;
import org.example.djiankang.front.controller.form.GetTop4ByCategoryIdsForm;
import org.example.djiankang.front.controller.form.PageQueryGoodsForm;
import org.example.djiankang.front.service.GoodsService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    // 虽然请求是获取数据，应该接受GET请求。但由于需要传数组，POST请求更方便，因此实际开发中这种情况发POST请求的也很多。
    @PostMapping("/top4")
    @Operation(summary = "获取各分类Top4套餐", description = "根据分类ID数组，返回每个分类下销量前4的套餐")
    public R getTop4(@RequestBody @Valid GetTop4ByCategoryIdsForm form) {
        Map<Integer, List<Map<String, Object>>> top4 = goodsService.getTop4ByCategoryIds(form.getCategoryIds());
        return R.ok().put("top4", top4);
    }
    @GetMapping("/list")
    @Operation(summary = "分页查询", description = "根据查询条件分页查询商品")
    public R list(@ParameterObject @Valid PageQueryGoodsForm form) {
        Integer pageNo = form.getPageNo();
        Integer pageSize = form.getPageSize();
        Integer startIndex = (pageNo - 1) * pageSize;
        Map<String, Object> params = BeanUtil.beanToMap(form);
        params.put("startIndex", startIndex);
        PageResult<Map<String, Object>> pageResult = goodsService.queryPage(params);
        return R.ok().put("pageResult", pageResult);
    }
}
