package com.kun.oj.manage.oss;

import cn.hutool.core.io.FileUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;

@Slf4j
@RequiredArgsConstructor
public class OssTemplate {

    private final OSS ossClient;
    private final OssProperties properties;

    /**
     * 上传本地文件（例如测试用例 zipFile），返回 PutObjectResult（包含 ETag、RequestId 等）
     *
     * @param objectName OSS 对象存储路径（如 "judgeCase/1/testcase.zip"）
     * @param localFile  本地文件对象
     * @return PutObjectResult 上传结果
     */
    public PutObjectResult uploadFile(String objectName, File localFile) {
        try {
            PutObjectResult result = ossClient.putObject(properties.getBucketName(), objectName, localFile);
            log.info("OSS 本地文件上传成功: {} -> {}", localFile.getAbsolutePath(), objectName);
            return result;
        } catch (Exception e) {
            log.error("OSS 上传本地文件失败: {}", objectName, e);
            throw new RuntimeException("OSS 上传文件失败", e);
        }
    }

    /**
     * 上传输入流并返回文件的外链访问 URL（适用于头像、图片、文件流）
     *
     * @param objectName  OSS 对象存储路径
     * @param inputStream 输入流
     * @return 文件的可访问 URL 地址
     */
    public String uploadFile(String objectName, InputStream inputStream) {
        try {
            ossClient.putObject(properties.getBucketName(), objectName, inputStream);
            log.info("OSS 流式文件上传成功: {}", objectName);
            return getFileUrl(objectName);
        } catch (Exception e) {
            log.error("OSS 上传流文件失败: {}", objectName, e);
            throw new RuntimeException("OSS 上传文件失败", e);
        }
    }

    /**
     * 上传 Spring Web 的 MultipartFile，返回文件外链 URL
     *
     * @param objectName    OSS 对象存储路径
     * @param multipartFile 前端表单上传的文件
     * @return 文件的可访问 URL 地址
     */
    public String uploadFile(String objectName, MultipartFile multipartFile) {
        try {
            return uploadFile(objectName, multipartFile.getInputStream());
        } catch (Exception e) {
            log.error("OSS 上传 MultipartFile 失败: {}", objectName, e);
            throw new RuntimeException("OSS 上传文件失败", e);
        }
    }

    /**
     * 获取文件外链访问 URL
     */
    public String getFileUrl(String objectName) {
        return properties.getDomain() != null
                ? properties.getDomain() + "/" + objectName
                : "https://" + properties.getBucketName() + "." + properties.getEndpoint() + "/" + objectName;
    }

    /**
     * 下载 OSS 文件到本地目录
     */
    public void downloadFile(String objectName, File localFile) {
        try {
            // 关键防呆：确保本地父级目录存在，否则会报 FileNotFoundException
            FileUtil.mkParentDirs(localFile);
            // 调用 OSS SDK 下载并写入本地文件
            GetObjectRequest getObjectRequest = new GetObjectRequest(properties.getBucketName(), objectName);
            ossClient.getObject(getObjectRequest, localFile);
            log.info("OSS 文件下载成功: {} -> {}", objectName, localFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("OSS 下载文件到本地失败: {}", objectName, e);
            throw new RuntimeException("下载文件失败", e);
        }
    }


    /**
     * 删除文件
     */
    public void deleteFile(String objectName) {
        try {
            ossClient.deleteObject(properties.getBucketName(), objectName);
            log.info("OSS 文件删除成功: {}", objectName);
        } catch (Exception e) {
            log.error("OSS 文件删除失败: {}", objectName, e);
            throw new RuntimeException("OSS 文件删除失败", e);
        }
    }
}
