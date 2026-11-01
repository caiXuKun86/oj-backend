package com.kun.oj.manage.oss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    private String region;
    private String endpoint;
    private String bucketName;
    private String domain;
}
