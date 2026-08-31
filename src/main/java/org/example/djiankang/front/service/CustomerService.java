package org.example.djiankang.front.service;

import java.util.Map;

public interface CustomerService {
    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @return true表示发送成功
     */
    boolean sendSmsCode(String phone);
    /**
     * 用户登录
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 登录结果
     */
    Map<String, Object> login(String phone, String code);

    Map<String, Object> getSummaryById(Integer id);

    void update(Map<String, Object> param);
}