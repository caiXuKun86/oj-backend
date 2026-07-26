package com.kun.oj.manage.cos;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

@ConditionalOnProperty(name = "cos.secretKey")
@Component
public class CosManager {
    @Resource
    private COSClient cosClient;
    @Resource
    private CosProperties cosProperties;

    public String getHost(String key) {
        return String.format("https://%s.cos.%s.myqcloud.com/%s", cosProperties.getBucket(), cosProperties.getRegion(), key);
    }

    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosProperties.getBucket(), key, file);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
        return putObjectResult;
    }

    public ObjectListing listObjects(String prefix) {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.setBucketName(cosProperties.getBucket());
        listObjectsRequest.setMaxKeys(1000);
        listObjectsRequest.setPrefix(prefix);
        return cosClient.listObjects(listObjectsRequest);
    }

    public void getObject(String key, File localFile) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosProperties.getBucket(), key);
        cosClient.getObject(getObjectRequest, localFile);
    }
    public void deleteObject(String key) {
        DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(cosProperties.getBucket(), key);
        cosClient.deleteObject(deleteObjectRequest);
    }


}

