package com.recruit.agent.candidate.application;

import com.recruit.agent.candidate.domain.ResumeDocument;
import com.recruit.agent.candidate.domain.ResumeParseStatus;
import com.recruit.agent.candidate.infrastructure.InMemoryResumeRepository;
import com.recruit.agent.common.document.PlainTextDocumentTextExtractor;
import com.recruit.agent.common.storage.FileUpload;
import com.recruit.agent.common.storage.LocalFileStorage;
import com.recruit.agent.common.storage.StoredFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateResumeTextExtractionServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void extractsTextAndMarksResumeParsed() {
        InMemoryResumeRepository resumeRepository = new InMemoryResumeRepository();
        LocalFileStorage storage = new LocalFileStorage(tempDir, java.time.Clock.systemUTC());
        StoredFile storedFile = storage.store(new FileUpload(
                "resume.txt",
                "text/plain",
                16,
                new ByteArrayInputStream("Java Spring Boot".getBytes(StandardCharsets.UTF_8))
        ));
        ResumeDocument resume = resumeRepository.save(new ResumeDocument(
                resumeRepository.nextResumeId(),
                2001L,
                "resume.txt",
                "text/plain",
                16,
                storedFile.objectName(),
                ResumeParseStatus.UPLOADED,
                null,
                Instant.parse("2026-07-11T00:00:00Z")
        ));
        CandidateResumeTextExtractionService service = new CandidateResumeTextExtractionService(
                resumeRepository,
                storage,
                new PlainTextDocumentTextExtractor()
        );

        ResumeDocument parsed = service.extractText(resume.id());

        assertThat(parsed.parseStatus()).isEqualTo(ResumeParseStatus.PARSED);
        assertThat(parsed.rawText()).isEqualTo("Java Spring Boot");
        assertThat(resumeRepository.findById(resume.id()).orElseThrow().parseStatus())
                .isEqualTo(ResumeParseStatus.PARSED);
    }
}
