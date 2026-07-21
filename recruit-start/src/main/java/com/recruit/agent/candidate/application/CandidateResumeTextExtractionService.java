package com.recruit.agent.candidate.application;

import com.recruit.agent.candidate.domain.ResumeDocument;
import com.recruit.agent.candidate.domain.ResumeRepository;
import com.recruit.agent.common.document.DocumentContent;
import com.recruit.agent.common.document.DocumentTextExtractor;
import com.recruit.agent.common.document.ExtractedText;
import com.recruit.agent.common.storage.FileStorage;
import com.recruit.agent.common.storage.StoredFileContent;

import java.util.NoSuchElementException;

public class CandidateResumeTextExtractionService {

    private final ResumeRepository resumeRepository;
    private final FileStorage fileStorage;
    private final DocumentTextExtractor textExtractor;

    public CandidateResumeTextExtractionService(
            ResumeRepository resumeRepository,
            FileStorage fileStorage,
            DocumentTextExtractor textExtractor
    ) {
        this.resumeRepository = resumeRepository;
        this.fileStorage = fileStorage;
        this.textExtractor = textExtractor;
    }

    public ResumeDocument extractText(Long resumeId) {
        ResumeDocument resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NoSuchElementException("简历不存在: " + resumeId));
        resumeRepository.save(resume.markParsing());
        try (StoredFileContent content = fileStorage.open(resume.storageObjectName())) {
            ExtractedText extractedText = textExtractor.extract(new DocumentContent(
                    resume.originalFilename(),
                    resume.contentType(),
                    content.inputStream()
            ));
            ResumeDocument parsedResume = resume.markParsed(extractedText.rawText());
            return resumeRepository.save(parsedResume);
        } catch (RuntimeException exception) {
            resumeRepository.save(resume.markFailed(exception.getMessage()));
            throw exception;
        } catch (LinkageError error) {
            resumeRepository.save(resume.markFailed(error.getMessage()));
            throw error;
        } catch (Exception exception) {
            resumeRepository.save(resume.markFailed(exception.getMessage()));
            throw new IllegalStateException("简历文本提取失败", exception);
        }
    }
}
