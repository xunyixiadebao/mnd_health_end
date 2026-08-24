package org.example.djiankang.mis.controller.from;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
@Schema(description = "体检套餐分页查询请求参数")
public class ExamPackagePageQueryForm {

    @Schema(description = "套餐名称，支持字母、数字、中文，长度1-50", example = "尊享体检套餐")
    @Pattern(regexp = "^$|^[a-zA-Z0-9\\u4e00-\\u9fa5]{1,50}$", message = "packageName内容不正确")
    private String packageName;

    @Schema(description = "套餐编码，6-20位字母或数字", example = "PKG2024001")
    @Pattern(regexp = "^$|^[a-zA-Z0-9]{6,20}$", message = "packageCode内容不正确")
    private String packageCode;

    @Schema(description = "套餐类型", example = "职场白领", allowableValues = {"父母体检", "入职体检", "职场白领", "个人高端", "中青年体检"})
    @Pattern(regexp = "^父母体检$|^入职体检$|^职场白领$|^个人高端$|^中青年体检$", message = "packageType内容不正确")
    private String packageType;

    @Schema(description = "分类ID，范围1-5", example = "2")
    @Range(min = 1, max = 5, message = "categoryId范围不正确")
    private Byte categoryId;

    @Schema(description = "状态：true-启用，false-禁用", example = "true")
    private Boolean status;

    @Schema(description = "当前页码，从1开始，默认1", example = "1", defaultValue = "1")
    @Min(value = 1, message = "pageNo不能小于1")
    private Integer pageNo = 1;

    @Schema(description = "每页记录数，范围10-50，默认10", example = "10", defaultValue = "10")
    @Range(min = 10, max = 50, message = "pageSize必须为10~50之间")
    private Integer pageSize = 10;
}