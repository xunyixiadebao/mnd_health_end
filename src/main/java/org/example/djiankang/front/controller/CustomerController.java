package org.example.djiankang.front.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.djiankang.common.R;
import org.example.djiankang.front.controller.form.SendSmsCodeForm;
import org.example.djiankang.front.service.CustomerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/front/customer")
@RequiredArgsConstructor
@Tag(name = "客户管理", description = "客户相关接口")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/sms-codes")
    @Operation(summary = "发送短信验证码", description = "向指定手机号发送短信验证码，用于登录或注册验证")
    public R sendSmsCode(@Valid @RequestBody SendSmsCodeForm form) {
        boolean success = customerService.sendSmsCode(form.getPhone());
        String msg = success ? "短信验证码发送成功" : "短信验证码发送失败";
        return R.ok(msg).put("result", success);
    }
}