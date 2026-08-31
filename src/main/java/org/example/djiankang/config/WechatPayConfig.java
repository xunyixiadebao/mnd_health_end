package org.example.djiankang.config;


import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class WechatPayConfig {

    @Value("${wechat.pay.v3.mnd-health.mch-id}")
    private String mchId;

    @Value("${wechat.pay.v3.mnd-health.merchant-serial-no}")
    private String merchantSerialNo;

    @Value("${wechat.pay.v3.mnd-health.private-key-path}")
    private String privateKeyPath;

    @Value("${wechat.pay.v3.mnd-health.app-v3-secret}")
    private String apiV3Key;

    /**
     * 创建微信支付核心配置
     * RSAAutoCertificateConfig 会自动下载并更新微信平台证书
     */
    @Bean
    public Config wechatPayConfigBean() throws Exception {
        log.info("初始化微信支付配置，商户号：{}", mchId);

        return new RSAAutoCertificateConfig.Builder()
                .merchantId(mchId)
                .privateKeyFromPath(privateKeyPath)      // 加载 PEM 格式私钥
                .merchantSerialNumber(merchantSerialNo)
                .apiV3Key(apiV3Key).build();
    }
}