package org.example.djiankang.front.controller.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建支付请求参数")
public class CreatePaymentForm {

    @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "goodsId不能为空")
    @Min(value = 1, message = "goodsId不能小于1")
    private Integer goodsId;

    @Schema(description = "购买数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2", minimum = "1")
    @NotNull(message = "buyCount不能为空")
    @Min(value = 1, message = "buyCount不能小于1")
    private Integer buyCount;
}