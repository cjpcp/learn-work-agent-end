package com.example.learnworkagent.infrastructure.external.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * OSS 服务类 - 用于文件上传到阿里云OSS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssService {

    @Value("${oss.endpoint}")
    private String endpoint;

    @Value("${oss.accessKeyId}")
    private String accessKeyId;

    @Value("${oss.accessKeySecret}")
    private String accessKeySecret;

    @Value("${oss.bucketName}")
    private String bucketName;

    @Value("${oss.domain}")
    private String domain;

    /**
     * 上传文件到OSS
     *
     * @param file   上传的文件
     * @param folder 存储文件夹
     * @return 文件在OSS上的URL
     */
    public String uploadFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上传文件不能为空");
        }

        try {
            // 生成唯一文件名
            String fileExtension = getFileExtension(file);
            String fileName = folder + "/" + UUID.randomUUID() + fileExtension;

            // 创建OSS客户端
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

            try {
                // 检查bucket是否存在
                if (!ossClient.doesBucketExist(bucketName)) {
                    ossClient.createBucket(bucketName);
                }

                // 设置文件元数据
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(file.getSize());
                metadata.setContentType(file.getContentType());

                // 上传文件
                try (InputStream inputStream = file.getInputStream()) {
                    PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, inputStream, metadata);
                    ossClient.putObject(putObjectRequest);
                }

                // 生成文件URL
                String fileUrl = domain + "/" + fileName;
                log.info("文件上传成功，URL: {}", fileUrl);
                return fileUrl;
            } finally {
                // 关闭OSS客户端
                ossClient.shutdown();
            }
        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("OSS上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "OSS上传失败: " + e.getMessage());
        }
    }

    private static @NotNull String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String fileExtension;
        if (originalFilename != null && originalFilename.lastIndexOf(".") >= 0) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            // 无扩展名时从 ContentType 推断
            String contentType = file.getContentType();
            if (contentType != null) {
                if (contentType.contains("webm")) fileExtension = ".webm";
                else if (contentType.contains("ogg")) fileExtension = ".ogg";
                else if (contentType.contains("wav")) fileExtension = ".wav";
                else if (contentType.contains("mp3") || contentType.contains("mpeg")) fileExtension = ".mp3";
                else if (contentType.contains("jpeg") || contentType.contains("jpg")) fileExtension = ".jpg";
                else if (contentType.contains("png")) fileExtension = ".png";
                else if (contentType.contains("gif")) fileExtension = ".gif";
                else if (contentType.contains("pdf")) fileExtension = ".pdf";
                else fileExtension = "";
            } else {
                fileExtension = "";
            }
        }
        return fileExtension;
    }

    /**
     * 上传咨询相关文件（图片或语音）
     *
     * @param file     上传的文件
     * @param userId   用户ID
     * @param fileType 文件类型（image或voice）
     * @return 文件在OSS上的URL
     */
    public String uploadConsultationFile(MultipartFile file, Long userId, String fileType) {
        return uploadFile(file, "consultation/" + fileType + "/" + userId);
    }
}
