package org.example.djiankang.front.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.djiankang.common.R;
import org.example.djiankang.config.satoken.StpCustomerUtil;
import org.example.djiankang.front.controller.form.CustomerLoginForm;
import org.example.djiankang.front.controller.form.SendSmsCodeForm;
import org.example.djiankang.front.controller.form.UpdateCustomerForm;
import org.example.djiankang.front.service.CustomerService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    @PostMapping("/login")
    @Operation(summary = "手机号登录", description = "通过手机号和短信验证码进行登录认证，登录成功后返回访问令牌")
    public R login(@RequestBody @Valid CustomerLoginForm form) {
        // 获取用户提交的手机号和验证码
        String phone = form.getPhone();
        String code = form.getCode();

        // 调用service验证
        Map<String, Object> map = customerService.login(phone, code);

        Boolean result = MapUtil.getBool(map, "result");
        String msg = MapUtil.getStr(map, "msg");
        R r = R.ok(msg).put("result", result);

        if (result) {
            // 生成令牌
            int id = MapUtil.getInt(map, "id");
            // 注意：业务端的认证体系和mis端的认证体系一定是不一样的。这里千万不能用StpUtil工具类。
            StpCustomerUtil.login(id, "PC");
            String token = StpCustomerUtil.getTokenValue();
            r.put("token", token);
        }

        return r;
    }
    @GetMapping("/auth/status")
    @Operation(summary = "检查用户登录状态")
    public R checkLogin() {
        boolean isLogin = StpCustomerUtil.isLogin();
        return R.ok().put("result", isLogin);
    }
    @DeleteMapping("/auth/session")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    @Operation(summary = "用户登出", description = "退出当前登录会话，清除服务端 Session 和客户端 Token")
    public R logout() {
        int loginId = StpCustomerUtil.getLoginIdAsInt();
        StpCustomerUtil.logout(loginId, "PC");
        return R.ok();
    }
    @GetMapping("/summary")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    @Operation(summary = "获取用户信息摘要", description = "根据当前登录用户的ID获取个人概要信息")
    public R getSummary() {
        int id = StpCustomerUtil.getLoginIdAsInt();
        Map<String, Object> summary = customerService.getSummaryById(id);
        return R.ok().put("result", summary);
    }
    @PutMapping
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    @Operation(summary = "修改客户信息", description = "更新当前登录用户的基本信息（姓名、性别、手机号）")
    public R update(@RequestBody @Valid UpdateCustomerForm form) {
        int id = StpCustomerUtil.getLoginIdAsInt();
        Map<String, Object> param = BeanUtil.beanToMap(form);
        param.put("id", id);
        customerService.update(param);
        return R.ok("客户信息更新成功");
    }
}