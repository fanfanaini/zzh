package com.recruit.agent.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@Profile("!minio")
public class LocalFileStorage implements FileStorage {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final Path rootDirectory;
    private final Clock clock;

    @Autowired
    public LocalFileStorage(@Value("${recruit.storage.local-root:./data/resumes}") String rootDirectory) {
        this(Path.of(rootDirectory), Clock.systemDefaultZone());
    }

    public LocalFileStorage(Path rootDirectory, Clock clock) {
        this.rootDirectory = rootDirectory.toAbsolutePath().normalize();
        this.clock = clock;
    }

    @Override
    public StoredFile store(FileUpload upload) {
        String safeFilename = safeFilename(upload.originalFilename());
        String datePath = LocalDate.now(clock).format(DATE_PATH);
        String objectName = datePath + "/" + UUID.randomUUID() + "-" + safeFilename;
        Path target = resolveObjectPath(objectName);

        try {
            Files.createDirectories(target.getParent());
            Files.copy(upload.inputStream(), target);
            return new StoredFile(upload.originalFilename(), upload.contentType(), upload.size(), objectName);
        } catch (IOException exception) {
            throw new FileStorageException("文件存储失败", exception);
        }
    }

    @Override
    public StoredFileContent open(String objectName) {
        Path target = resolveObjectPath(objectName);
        try {
            long size = Files.size(target);
            return new StoredFileContent(
                    objectName,
                    target.getFileName().toString(),
                    Files.probeContentType(target),
                    size,
                    Files.newInputStream(target)
            );
        } catch (IOException exception) {
            throw new FileStorageException("File read failed: " + target, exception);
        }
    }

    @Override
    public void delete(String objectName) {
        Path target = resolveObjectPath(objectName);
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new FileStorageException("文件删除失败", exception);
        }
    }

    private String safeFilename(String originalFilename) {
        String filename = Path.of(originalFilename).getFileName().toString();
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Path resolveObjectPath(String objectName) {
        Path target = rootDirectory.resolve(objectName).normalize();
        if (!target.startsWith(rootDirectory)) {
            throw new IllegalArgumentException("文件存储路径非法");
        }
        return target;
    }
}
