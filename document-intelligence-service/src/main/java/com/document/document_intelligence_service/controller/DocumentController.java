package com.document.document_intelligence_service.controller;

import com.document.document_intelligence_service.model.DocumentAnalysisResponse;
import com.document.document_intelligence_service.service.DocumentAnalysisService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentAnalysisService documentAnalysisService;

    public DocumentController(DocumentAnalysisService documentAnalysisService) {
        this.documentAnalysisService = documentAnalysisService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentAnalysisResponse> analyzeDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentType", required = false) String documentType) {

        DocumentAnalysisResponse response = documentAnalysisService.analyze(file, documentType);
        return ResponseEntity.ok(response);
    }
}
