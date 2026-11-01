package com.kun.oj.manage.oss;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OssConfiguration {

    /**
     * 1. 根据配置创建阿里云 OSS 客户端 Bean
     */
    @Bean(destroyMethod = "shutdown") // 容器销毁时优雅关闭连接
    @ConditionalOnMissingBean
    public OSS ossClient(OssProperties properties) {
        // 从环境变量获取访问凭证
        String accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
        String accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");

        // 设置OSS地域和Endpoint
        String region = properties.getRegion();
        String endpoint = properties.getEndpoint();

        // 创建凭证提供者
        DefaultCredentialProvider provider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);
        // 配置客户端参数
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        // 显式声明使用V4签名算法
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

        // 初始化OSS客户端
        return OSSClientBuilder.create()
                .credentialsProvider(provider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .endpoint(endpoint)
                .build();
    }

    /**
     * 2. 把创建好的 OSS 客户端与 properties 注入给 OssTemplate
     */
    @Bean
    @ConditionalOnMissingBean
    public OssTemplate ossTemplate(OSS ossClient, OssProperties properties) {
        return new OssTemplate(ossClient, properties);
    }
}