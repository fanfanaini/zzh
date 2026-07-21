package com.recruit.agent.common.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@Profile("minio")
public class MinioFileStorage implements FileStorage {
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private final MinioClient client;
    private final String bucket;

    public MinioFileStorage(@Value("${recruit.storage.minio.endpoint}") String endpoint,
                            @Value("${recruit.storage.minio.access-key}") String accessKey,
                            @Value("${recruit.storage.minio.secret-key}") String secretKey,
                            @Value("${recruit.storage.minio.bucket:recruit-resumes}") String bucket) {
        this.client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        this.bucket = bucket;
        initializeBucket();
    }

    @Override
    public StoredFile store(FileUpload upload) {
        String filename = safeFilename(upload.originalFilename());
        String objectName = LocalDate.now().format(DATE_PATH) + "/" + UUID.randomUUID() + "-" + filename;
        try {
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(objectName)
                    .stream(upload.inputStream(), upload.size(), -1)
                    .contentType(upload.contentType()).build());
            return new StoredFile(upload.originalFilename(), upload.contentType(), upload.size(), objectName);
        } catch (Exception exception) {
            throw new FileStorageException("MinIO 文件存储失败", exception);
        }
    }

    @Override
    public StoredFileContent open(String objectName) {
        try {
            StatObjectResponse metadata = client.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(objectName).build());
            GetObjectResponse stream = client.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(objectName).build());
            return new StoredFileContent(objectName, filename(objectName), metadata.contentType(), metadata.size(), stream);
        } catch (Exception exception) {
            throw new FileStorageException("MinIO 文件读取失败", exception);
        }
    }

    @Override
    public void delete(String objectName) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
        } catch (Exception exception) {
            throw new FileStorageException("MinIO 文件删除失败", exception);
        }
    }

    private void initializeBucket() {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception exception) {
            throw new FileStorageException("MinIO Bucket 初始化失败", exception);
        }
    }

    private String safeFilename(String originalFilename) {
        String value = originalFilename == null ? "file" : filename(originalFilename);
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    private String filename(String path) {
        int separator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return separator < 0 ? path : path.substring(separator + 1);
    }
}
