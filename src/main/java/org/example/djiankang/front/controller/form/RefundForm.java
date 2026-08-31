package org.example.djiankang.front.controller.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "退款请求参数")
public class RefundForm {

    @NotNull(message = "订单ID不能为空")
    @Min(value = 1, message = "订单ID不能小于1")
    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Integer orderId;
}