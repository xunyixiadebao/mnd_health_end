package org.example.djiankang.front.controller.form;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "更新客户信息表单")
public class UpdateCustomerForm {

    @Schema(description = "客户姓名", example = "张三")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{2,10}$", message = "customerName内容不正确")
    private String customerName;

    @Schema(description = "性别（男/女）", example = "男", allowableValues = {"男", "女"})
    @Pattern(regexp = "^男$|^女$", message = "gender内容不正确")
    private String gender;

    @Schema(description = "手机号", example = "13812345678")
    @NotBlank(message = "phone不能为空")
    @Pattern(regexp = "^1[1-9]\\d{9}$", message = "phone内容错误")
    private String phone;
}