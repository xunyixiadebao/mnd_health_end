package org.example.djiankang.front.controller.form;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

@Data
@Schema(description = "商品分页查询表单")
public class PageQueryGoodsForm {

    @Schema(description = "关键字（套餐名称/编号）", example = "入职体检")
    @Length(min = 1, max = 50, message = "keyword字数超出范围")
    private String keyword;

    @Schema(description = "套餐类型（父母体检/入职体检/职场白领/个人高端/中青年体检）", example = "入职体检")
    @Pattern(regexp = "^父母体检$|^入职体检$|^职场白领$|^个人高端$|^中青年体检$", message = "packageType内容不正确")
    private String packageType;

    @Schema(description = "性别（男性/女性）", example = "男性")
    @Pattern(regexp = "^男性$|^女性$", message = "sex内容不正确")
    private String sex;

    @Schema(description = "价格类型（1-0-100，2-100-500，3-500-1000，4-1000以上）", example = "1")
    @Range(min = 1, max = 4, message = "priceType范围不正确")
    private Integer priceType;

    @Schema(description = "排序类型（1-最新，2-销量，3-价格升序，4-价格降序）", example = "1")
    @Range(min = 1, max = 4, message = "orderType范围不正确")
    private Integer orderType;

    @Schema(description = "当前页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "page不能为空")
    @Min(value = 1, message = "pageNo不能小于1")
    private Integer pageNo;

    @Schema(description = "每页条数（10-50之间）", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "pageSize不能为空")
    @Range(min = 10, max = 50, message = "length必须为10~50之间")
    private Integer pageSize;
}