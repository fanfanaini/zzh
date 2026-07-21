package com.recruit.agent.common.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void storesFileUnderDateDirectory() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(
                tempDir,
                Clock.fixed(Instant.parse("2026-07-11T00:00:00Z"), ZoneOffset.UTC)
        );

        StoredFile storedFile = storage.store(new FileUpload(
                "../resume.pdf",
                "application/pdf",
                13,
                new ByteArrayInputStream("resume-content".getBytes(StandardCharsets.UTF_8))
        ));

        assertThat(storedFile.objectName()).startsWith("2026/07/11/");
        assertThat(storedFile.objectName()).endsWith("-resume.pdf");
        assertThat(Files.readString(tempDir.resolve(storedFile.objectName()), StandardCharsets.UTF_8))
                .isEqualTo("resume-content");
    }

    @Test
    void opensStoredFileContent() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(
                tempDir,
                Clock.fixed(Instant.parse("2026-07-11T00:00:00Z"), ZoneOffset.UTC)
        );
        StoredFile storedFile = storage.store(new FileUpload(
                "resume.txt",
                "text/plain",
                13,
                new ByteArrayInputStream("resume-content".getBytes(StandardCharsets.UTF_8))
        ));

        try (StoredFileContent content = storage.open(storedFile.objectName())) {
            assertThat(new String(content.inputStream().readAllBytes(), StandardCharsets.UTF_8))
                    .isEqualTo("resume-content");
            assertThat(content.objectName()).isEqualTo(storedFile.objectName());
        }
    }
}
