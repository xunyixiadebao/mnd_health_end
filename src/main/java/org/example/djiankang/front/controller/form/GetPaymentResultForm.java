package org.example.djiankang.front.controller.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "查询支付结果请求参数")
public class GetPaymentResultForm {

    @Schema(description = "商户订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "5F8E9A2B3C4D5E6F7G8H9I0J1K2L3M4N")
    @NotBlank(message = "outTradeNo不能为空")
    @Pattern(regexp = "^[0-9A-Z]{32}$", message = "outTradeNo内容不正确")
    private String outTradeNo;
}