package com.recruit.agent.candidate.api;

import com.recruit.agent.candidate.application.AddResumeCommand;
import com.recruit.agent.candidate.application.CandidateApplicationService;
import com.recruit.agent.candidate.application.RegisterCandidateCommand;
import com.recruit.agent.candidate.domain.CandidateStatus;
import com.recruit.agent.common.api.PageResponse;
import com.recruit.agent.common.storage.FileStorage;
import com.recruit.agent.common.storage.FileUpload;
import com.recruit.agent.common.storage.StoredFile;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final CandidateApplicationService service;
    private final FileStorage fileStorage;

    public CandidateController(CandidateApplicationService service, FileStorage fileStorage) {
        this.service = service;
        this.fileStorage = fileStorage;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CandidateResponse register(@Valid @RequestBody RegisterCandidateRequest request) {
        return CandidateResponse.from(service.register(new RegisterCandidateCommand(
                request.name(),
                request.phoneHash(),
                request.emailHash(),
                request.educationLevel(),
                request.totalExperienceYears(),
                request.expectedCity(),
                request.skills()
        )));
    }

    @GetMapping("/{id}")
    public CandidateResponse get(@PathVariable("id") Long id) {
        return CandidateResponse.from(service.get(id));
    }

    @GetMapping
    public Object list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "status", required = false) CandidateStatus status,
                       @RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "size", required = false) Integer size) {
        boolean hasFilter = keyword != null || status != null;
        List<CandidateResponse> candidates = (hasFilter ? service.search(keyword, status) : service.list()).stream()
                .map(CandidateResponse::from)
                .toList();
        if (page != null || size != null) {
            return PageResponse.of(candidates, page == null ? 1 : page, size == null ? 20 : size);
        }
        return candidates;
    }

    @PutMapping("/{id}")
    public CandidateResponse update(@PathVariable("id") Long id, @Valid @RequestBody RegisterCandidateRequest request) {
        return CandidateResponse.from(service.update(id, new RegisterCandidateCommand(
                request.name(),
                request.phoneHash(),
                request.emailHash(),
                request.educationLevel(),
                request.totalExperienceYears(),
                request.expectedCity(),
                request.skills()
        )));
    }

    @PatchMapping("/{id}/disable")
    public CandidateResponse disable(@PathVariable("id") Long id) {
        return CandidateResponse.from(service.disable(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/resumes")
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeResponse addResume(@PathVariable("id") Long id, @Valid @RequestBody AddResumeRequest request) {
        return ResumeResponse.from(service.addResume(new AddResumeCommand(
                id,
                request.originalFilename(),
                request.contentType(),
                request.fileSize(),
                request.storageObjectName()
        )));
    }

    @PostMapping(value = "/{id}/resumes/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeResponse uploadResume(@PathVariable("id") Long id, @RequestPart("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("简历文件不能为空");
        }
        StoredFile storedFile = fileStorage.store(new FileUpload(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream()
        ));
        return ResumeResponse.from(service.addResume(new AddResumeCommand(
                id,
                storedFile.originalFilename(),
                storedFile.contentType(),
                storedFile.size(),
                storedFile.objectName()
        )));
    }

    @GetMapping("/{id}/resumes")
    public List<ResumeResponse> listResumes(@PathVariable("id") Long id) {
        return service.listResumes(id).stream()
                .map(ResumeResponse::from)
                .toList();
    }
}
