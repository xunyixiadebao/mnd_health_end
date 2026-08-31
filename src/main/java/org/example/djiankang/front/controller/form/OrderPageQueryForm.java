package org.example.djiankang.front.controller.form;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
@Schema(description = "订单分页查询请求参数")
public class OrderPageQueryForm {

    @Schema(description = "搜索关键字（支持字母、数字、中文，长度1-50）", example = "订单号001", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Pattern(regexp = "^$|^[a-zA-Z0-9\\u4e00-\\u9fa5]{1,50}$", message = "keyword内容不正确")
    private String keyword;

    @Schema(description = "订单状态（1-待支付，3-已完成）", allowableValues = {"1", "3"}, example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Pattern(regexp = "^1$|^3$", message = "orderStatus内容不正确")
    private String orderStatus;

    @Schema(description = "当前页码（从1开始）", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "pageNo不能为空")
    @Min(value = 1, message = "pageNo不能小于1")
    private Integer pageNo;

    @Schema(description = "每页记录数（范围10-50）", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "pageSize不能为空")
    @Range(min = 10, max = 50, message = "pageSize必须为10~50之间")
    private Integer pageSize;
}