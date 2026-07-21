package com.recruit.agent.knowledge.api;

import com.recruit.agent.knowledge.application.KnowledgeApplicationService;
import com.recruit.agent.vector.domain.KnowledgeChunkMatch;
import com.recruit.agent.vector.domain.KnowledgeDocumentSummary;
import com.recruit.agent.common.document.DocumentContent;
import com.recruit.agent.common.document.DocumentTextExtractor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final KnowledgeApplicationService service;
    private final DocumentTextExtractor textExtractor;
    public KnowledgeController(KnowledgeApplicationService service, DocumentTextExtractor textExtractor) {
        this.service = service;
        this.textExtractor = textExtractor;
    }

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public KnowledgeApplicationService.IndexedKnowledgeDocument index(@Valid @RequestBody IndexKnowledgeRequest request) {
        return service.index(request.sourceName(), request.content());
    }

    @GetMapping("/search")
    public List<KnowledgeChunkMatch> search(@RequestParam("query") String query,
                                            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return service.search(query, limit);
    }

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public KnowledgeApplicationService.IndexedKnowledgeDocument upload(@RequestPart("file") MultipartFile file)
            throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("知识文件不能为空");
        var document = new DocumentContent(file.getOriginalFilename(), file.getContentType(), file.getInputStream());
        if (!textExtractor.supports(document)) throw new IllegalArgumentException("不支持该知识文件格式");
        return service.index(file.getOriginalFilename(), textExtractor.extract(document).rawText());
    }

    @GetMapping("/documents")
    public List<KnowledgeDocumentSummary> documents() {
        return service.listDocuments();
    }

    @DeleteMapping("/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("documentId") Long documentId) {
        service.delete(documentId);
    }
}
