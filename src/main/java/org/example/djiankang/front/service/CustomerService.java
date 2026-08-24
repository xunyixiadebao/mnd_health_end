package org.example.djiankang.front.service;

public interface CustomerService {
    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @return true表示发送成功
     */
    boolean sendSmsCode(String phone);
}