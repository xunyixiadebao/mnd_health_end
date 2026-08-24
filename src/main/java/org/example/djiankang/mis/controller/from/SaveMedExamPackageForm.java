package org.example.djiankang.mis.controller.from;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;
import java.util.ArrayList;

@Data
@Schema(description = "新增体检套餐请求表单")
public class SaveMedExamPackageForm {

    @Schema(description = "套餐编号，6-20位字母或数字", example = "PKG2024001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "packageCode不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$", message = "packageCode内容不正确")
    private String packageCode;

    @Schema(description = "套餐名称，2-50位字母、数字或中文", example = "尊享体检套餐", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "packageName不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]{2,50}$", message = "packageName内容不正确")
    private String packageName;

    @Schema(description = "套餐简介信息，最多200字符", example = "本套餐包含全面体检项目，适合中青年人群", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "description不能为空")
    @Length(max = 200, message = "description不能超过200个字符")
    private String description;

    @Schema(description = "科室检查项目列表")
    @Valid
    private ArrayList<ExamForm> departmentExam;

    @Schema(description = "实验室检查项目列表")
    @Valid
    private ArrayList<ExamForm> labExam;

    @Schema(description = "医技检查项目列表")
    @Valid
    private ArrayList<ExamForm> medicalExam;

    @Schema(description = "其他检查项目列表")
    @Valid
    private ArrayList<ExamForm> otherExam;

    @Schema(description = "封面图片路径", example = "front/goods/abc123.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "coverImage不能为空")
    @Pattern(regexp = "^[0-9a-zA-Z/\\.]{1,200}$", message = "coverImage内容不正确")
    private String coverImage;

    @Schema(description = "套餐原价（元）", example = "1999.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "originalPrice不能为空")
    @Min(value = 0, message = "originalPrice不能小于0")
    private BigDecimal originalPrice;

    @Schema(description = "套餐现价（元）", example = "1599.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "currentPrice不能为空")
    @Min(value = 0, message = "currentPrice不能小于0")
    private BigDecimal currentPrice;

    @Schema(description = "套餐类型", example = "职场白领", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"父母体检", "入职体检", "职场白领", "个人高端", "中青年体检"})
    @NotBlank(message = "packageType不能为空")
    @Pattern(regexp = "^父母体检$|^入职体检$|^职场白领$|^个人高端$|^中青年体检$")
    private String packageType;

    @Schema(description = "特征标签数组", example = "[\"热门\", \"优惠\", \"推荐\"]")
    private String[] tags;

    @Schema(description = "展示区ID，范围1-5", example = "1")
    @Range(min = 1, max = 5, message = "categoryId范围不正确")
    private Integer categoryId;

    @Schema(description = "促销活动ID", example = "1")
    @Min(value = 1, message = "promotionId不能小于1")
    private Integer promotionId;
}
