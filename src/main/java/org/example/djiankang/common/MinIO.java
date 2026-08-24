package org.example.djiankang.common;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.djiankang.exception.HisException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Component
@Slf4j
public class MinIO {
    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucket;

    private MinioClient client;

    @PostConstruct
    public void init() {
        this.client = new MinioClient.Builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * 上传图片到MinIO对象存储
     *
     * @param path 文件在MinIO中的存储路径（包含文件名）
     * @param file Spring MultipartFile文件对象
     * @throws HisException 当文件上传失败时抛出业务异常
     */
    public void uploadImage(String path, MultipartFile file) {
        // 参数校验
        if (path == null || path.trim().isEmpty()) {
            throw new HisException("文件路径不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new HisException("上传文件不能为空");
        }

        try (InputStream inputStream = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket) // 指定存储桶
                    .object(path) // 指定存储的对象名：包括完整路径+文件名
                    // 指定文件内容（输入流）、文件大小、分片大小
                    .stream(inputStream, file.getSize(), 5 * 1024 * 1024)
                    // contentType 告诉浏览器如何解析和渲染该文件
                    .contentType("image/jpeg")
                    .build());
            log.debug("文件上传成功: {}", path);
        } catch (Exception e) {
            log.error("文件上传失败: {}", path, e);
            throw new HisException("文件上传失败");
        }
    }
    /**
     * 上传Excel文件到MinIO对象存储
     *
     * @param path 文件存储路径（包含文件名）
     * @param file 上传的Excel文件
     * @throws HisException 上传失败时抛出
     */
    public void uploadExcel(String path, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            // 根据文件扩展名设置MIME类型
            String filename = file.getOriginalFilename();
            String mime = filename != null && filename.endsWith(".xlsx")
                    ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    : "application/vnd.ms-excel";

            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .stream(inputStream, file.getSize(), 20 * 1024 * 1024)
                    .contentType(mime)
                    .build());

            log.info("Excel文件上传成功: path={}, size={}KB", path, file.getSize() / 1024);

        } catch (Exception e) {
            log.error("Excel文件上传失败: path={}", path, e);
            throw new HisException("Excel文件上传失败: " + e.getMessage());
        }
    }
    /**
     * 从MinIO下载文件输入流
     *
     * @param path 文件在MinIO中的存储路径
     * @return 文件输入流（调用方需自行关闭流）
     * @throws HisException 参数非法或下载失败时抛出
     */
    public InputStream downloadFile(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new HisException("下载失败：文件路径不能为空");
        }
        try {
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(path.trim())
                    .build();
            return client.getObject(args);
        } catch (Exception e) {
            log.error("文件下载失败，路径：{}", path, e);
            throw new HisException("文件下载失败: " + e.getMessage());
        }
    }
    /**
     * 从MinIO对象存储中删除指定路径的文件
     *
     * @param path 文件在MinIO中的存储路径（对象键），如 "user/avatar/12345.jpg"
     * @throws HisException 当参数非法或文件删除失败时抛出业务异常
     */
    public void deleteFile(String path) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(path.trim())
                    .build());
            log.info("文件删除成功: {}", path);
        } catch (Exception e) {
            log.error("文件删除失败，文件路径：{}", path, e);
            throw new HisException("文件删除失败：" + e.getMessage());
        }
    }
}