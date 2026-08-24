package org.example.djiankang.front.controller.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "发送短信验证码请求参数")
public class SendSmsCodeForm {
    @NotBlank(message = "phone不能为空")
    @Pattern(regexp = "^1[1-9]\\d{9}$", message = "phone内容错误")
    @Schema(description = "手机号码", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000", pattern = "^1[1-9]\\d{9}$")
    private String phone;
}