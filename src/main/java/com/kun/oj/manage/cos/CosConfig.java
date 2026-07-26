package com.kun.oj.manage.cos;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@ConditionalOnProperty(name = "cos.secretKey")
public class CosConfig {
    @Resource
    private CosProperties cosProperties;

    @Bean
    public COSClient cosClient(){
        String secretKey = cosProperties.getSecretKey();
        String secretId = cosProperties.getSecretId();
        String regionStr = cosProperties.getRegion();
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        Region region = new Region(regionStr);
        ClientConfig clientConfig = new ClientConfig(region);

        clientConfig.setHttpProtocol(HttpProtocol.https);
        return new COSClient(cred, clientConfig);
    }

}

