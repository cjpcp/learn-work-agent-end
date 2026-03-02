package com.example.learnworkagent.infrastructure.external.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * @param file 上传的文件
     * @param folder 存储文件夹
     * @return 文件在OSS上的URL
     */
    public String uploadFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上传文件不能为空");
        }

        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".unknown";
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

    /**
     * 上传奖助申请相关文件
     *
     * @param file 上传的文件
     * @param userId 用户ID
     * @return 文件在OSS上的URL
     */
    public String uploadAwardFile(MultipartFile file, Long userId) {
        return uploadFile(file, "award-applications/" + userId);
    }

    /**
     * 上传咨询相关文件（图片或语音）
     *
     * @param file 上传的文件
     * @param userId 用户ID
     * @param fileType 文件类型（image或voice）
     * @return 文件在OSS上的URL
     */
    public String uploadConsultationFile(MultipartFile file, Long userId, String fileType) {
        return uploadFile(file, "consultation/" + fileType + "/" + userId);
    }
}
