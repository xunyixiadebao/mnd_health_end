package org.example.djiankang.front.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.djiankang.db.entity.CrmCustomerEntity;
import org.example.djiankang.db.mapper.CrmCustomerMapper;
import org.example.djiankang.db.mapper.TradeOrderMapper;
import org.example.djiankang.exception.HisException;
import org.example.djiankang.front.service.CustomerService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final StringRedisTemplate redisTemplate;
    private final CrmCustomerMapper crmCustomerMapper;
    private final TradeOrderMapper tradeOrderMapper;

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

    @Override
    public Map<String, Object> login(String phone, String code) {
        Map<String, Object> map = new HashMap<>();

        // 校验验证码
        String key = "sms_code_" + phone;
        String refreshKey = "sms_code_refresh_" + phone;

        if (!redisTemplate.hasKey(key)) {
            map.put("result", false);
            map.put("msg", "短信验证码已过期");
            return map;
        }

        String smsCode = redisTemplate.opsForValue().get(key).toString();
        if (!smsCode.equals(code)) {
            map.put("result", false);
            map.put("msg", "短信验证码错误");
            return map;
        }
        // 验证通过，清除验证码（防止重复使用）
        redisTemplate.delete(key);
        redisTemplate.delete(refreshKey);

        // 4. 查询或注册用户
        Integer id = crmCustomerMapper.findIdByPhone(phone);
        if (id == null) {
            CrmCustomerEntity customer = new CrmCustomerEntity();
            customer.setPhone(phone);
            int rows = crmCustomerMapper.insert(customer);
            if(rows != 1){
                map.put("result", false);
                map.put("msg", "用户注册失败，稍后再试");
                return map;
            }
            id = customer.getId();
        }

        // 5. 返回结果
        map.put("result", true);
        map.put("id", id);
        map.put("msg", "登录成功");
        return map;
    }

    @Override
    public Map<String, Object> getSummaryById(Integer id) {
        Map<String, Object> map = crmCustomerMapper.selectById(id);
        if (map == null) {
            throw new HisException("客户不存在", 404);
        }
        map.putAll(tradeOrderMapper.selectOrderStatistics(id));
        return map;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Map<String, Object> param) {
        int rows = crmCustomerMapper.update(param);
        if (rows != 1) {
            throw new HisException("客户信息更新失败", 500);
        }
        log.info("客户信息更新成功：id={}", MapUtil.getInt(param, "id"));
    }
}