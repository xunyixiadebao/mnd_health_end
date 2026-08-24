package org.example.djiankang.front.service.impl;

import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.djiankang.front.service.CustomerService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean sendSmsCode(String phone) {
        // 生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 将验证码输出到控制台


        // 判断缓存中有没有 sms_code_refresh_电话号码  的key，如果还有这个key，表示用户还得再等等，等完1分钟之后才能再次发送短信验证码。
        if (redisTemplate.hasKey("sms_code_refresh_" + phone)) {
            return false;
        }

        // 程序能执行到这里，说明没有这个key，代表用户可以再次发送短信验证码
        // 把 sms_code_refresh_电话号码  这个数据放到redis缓存中
        redisTemplate.opsForValue().set("sms_code_refresh_" + phone, code);
        // 设置缓存有效期为1分钟
        redisTemplate.expire("sms_code_refresh_" + phone, 1, TimeUnit.MINUTES);

        // 将验证码缓存到redis中
        redisTemplate.opsForValue().set("sms_code_" + phone, code);
        // 设置缓存的有效期为5分钟
        redisTemplate.expire("sms_code_" + phone, 5, TimeUnit.MINUTES);
        // 调用短信运营商的API接口，让运营商发送短信
        return true;
    }
}