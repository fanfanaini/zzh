package com.recruit.agent.candidate.api;

import com.recruit.agent.candidate.application.CandidateProfileExtractionService;
import com.recruit.agent.candidate.application.CandidateResumeTextExtractionService;
import com.recruit.agent.candidate.application.CandidateApplicationService;
import com.recruit.agent.candidate.application.AddResumeCommand;
import com.recruit.agent.candidate.application.ResumeParsingTaskService;
import com.recruit.agent.common.storage.FileStorage;
import com.recruit.agent.common.storage.FileUpload;
import com.recruit.agent.common.storage.StoredFile;
import com.recruit.agent.common.storage.StoredFileContent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.InputStreamResource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.recruit.agent.task.api.AgentTaskResponse;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final CandidateResumeTextExtractionService textExtractionService;
    private final CandidateProfileExtractionService profileExtractionService;
    private final CandidateApplicationService candidateService;
    private final FileStorage fileStorage;
    private final ObjectMapper objectMapper;
    private final ResumeParsingTaskService parsingTaskService;

    public ResumeController(
            CandidateResumeTextExtractionService textExtractionService,
            CandidateProfileExtractionService profileExtractionService,
            ObjectProvider<CandidateApplicationService> candidateService,
            ObjectProvider<FileStorage> fileStorage,
            ObjectMapper objectMapper,
            ObjectProvider<ResumeParsingTaskService> parsingTaskService
    ) {
        this.textExtractionService = textExtractionService;
        this.profileExtractionService = profileExtractionService;
        this.candidateService = candidateService.getIfAvailable();
        this.fileStorage = fileStorage.getIfAvailable();
        this.objectMapper = objectMapper;
        this.parsingTaskService = parsingTaskService.getIfAvailable();
    }

    @PostMapping("/{id}/extract-text")
    @ResponseStatus(HttpStatus.OK)
    public ResumeResponse extractText(@PathVariable("id") Long id) {
        return ResumeResponse.from(textExtractionService.extractText(id));
    }

    @GetMapping("/{id}")
    public ResumeResponse get(@PathVariable("id") Long id) {
        if (candidateService == null) throw new IllegalStateException("简历服务未启用");
        return ResumeResponse.from(candidateService.getResume(id));
    }

    @GetMapping("/{id}/detail")
    public ResumeDetailResponse detail(@PathVariable("id") Long id) {
        if (candidateService == null) throw new IllegalStateException("简历服务未启用");
        var resume = candidateService.getResume(id);
        return new ResumeDetailResponse(ResumeResponse.from(resume), readStructuredDetail(resume.structuredDetailJson()));
    }

    @PutMapping("/{id}/structured-detail")
    public ResumeDetailResponse updateStructuredDetail(@PathVariable("id") Long id,
                                                       @RequestBody ResumeStructuredDetail detail) throws IOException {
        if (candidateService == null) throw new IllegalStateException("简历服务未启用");
        var resume = candidateService.updateResumeStructuredDetail(id, objectMapper.writeValueAsString(detail));
        return new ResumeDetailResponse(ResumeResponse.from(resume), readStructuredDetail(resume.structuredDetailJson()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable("id") Long id) {
        if (candidateService == null || fileStorage == null) throw new IllegalStateException("简历下载服务未启用");
        var resume = candidateService.getResume(id);
        StoredFileContent content = fileStorage.open(resume.storageObjectName());
        String contentType = resume.contentType() == null || resume.contentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : resume.contentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(content.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(resume.originalFilename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new InputStreamResource(content.inputStream()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        if (candidateService == null || fileStorage == null) throw new IllegalStateException("简历删除服务未启用");
        var resume = candidateService.deleteResume(id);
        fileStorage.delete(resume.storageObjectName());
    }

    @PostMapping("/{id}/extract-profile")
    @ResponseStatus(HttpStatus.OK)
    public CandidateResponse extractProfile(@PathVariable("id") Long id) {
        return CandidateResponse.from(profileExtractionService.extractProfile(id));
    }

    @PostMapping("/{id}/parse")
    public CandidateResponse parse(@PathVariable("id") Long id) {
        textExtractionService.extractText(id);
        return CandidateResponse.from(profileExtractionService.extractProfile(id));
    }

    @PostMapping("/{id}/parse-task")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AgentTaskResponse submitParseTask(@PathVariable("id") Long id) {
        if (parsingTaskService == null) {
            throw new IllegalStateException("简历异步解析服务未启用");
        }
        return AgentTaskResponse.from(parsingTaskService.submit(id));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeResponse upload(@RequestParam("candidateId") Long candidateId,
                                 @RequestPart("file") MultipartFile file) throws IOException {
        return store(candidateId, file);
    }

    @PostMapping(value = "/batch-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResumeResponse> batchUpload(@RequestParam("candidateId") Long candidateId,
                                            @RequestPart("files") List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) throw new IllegalArgumentException("简历文件不能为空");
        List<ResumeResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) responses.add(store(candidateId, file));
        return List.copyOf(responses);
    }

    private ResumeResponse store(Long candidateId, MultipartFile file) throws IOException {
        if (candidateService == null || fileStorage == null) throw new IllegalStateException("简历上传服务未启用");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("简历文件不能为空");
        StoredFile stored = fileStorage.store(new FileUpload(file.getOriginalFilename(), file.getContentType(),
                file.getSize(), file.getInputStream()));
        return ResumeResponse.from(candidateService.addResume(new AddResumeCommand(candidateId, stored.originalFilename(),
                stored.contentType(), stored.size(), stored.objectName())));
    }

    private ResumeStructuredDetail readStructuredDetail(String json) {
        if (json == null || json.isBlank()) {
            return ResumeStructuredDetail.empty();
        }
        try {
            return objectMapper.readValue(json, ResumeStructuredDetail.class);
        } catch (IOException exception) {
            return ResumeStructuredDetail.empty();
        }
    }
}
