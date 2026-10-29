package com.cmyhyq.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class OssService {
    @Value("${app.alioss.endpoint}")
    private String endpoint;
    @Value("${app.alioss.access-key-id}")
    private String accessKeyId;
    @Value("${app.alioss.access-key-secret}")
    private String accessKeySecret;
    @Value("${app.alioss.bucket-name}")
    private String bucketName;

    /**
     * 通用上传方法（支持 InputStream）
     * 供 WebSocket 异步上传 Base64 图片使用
     */
    public String uploadFile(InputStream inputStream, String fileName) {
        try {
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            String objectName = "chat-images/" + fileName;
            ossClient.putObject(bucketName, objectName, inputStream);
            ossClient.shutdown();
            return "https://" + bucketName + "." + endpoint + "/" + objectName;
        } catch (Exception e) {
            throw new RuntimeException("OSS 上传失败", e);
        }
    }

    /**
     * 图片上传
     * @param file
     * @return
     */
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String fileName = UUID.randomUUID().toString() + extension;

            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            String objectName = "avatar-images/" + fileName;
            ossClient.putObject(bucketName, objectName, file.getInputStream());
            ossClient.shutdown();
            return "https://" + bucketName + "." + endpoint + "/" + objectName;
        } catch (IOException e) {
            throw new RuntimeException("OSS 上传失败", e);
        }
    }


}
