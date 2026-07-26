package com.kun.oj.manage.cos;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cos")
public class CosProperties {
    private String secretId;
    private String secretKey;
    private String region;
    private String bucket;
}

