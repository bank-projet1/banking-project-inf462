package com.document.document_intelligence_service.model;

public class OcrResponse {

    private String extractedText;

    public OcrResponse() {
    }

    public OcrResponse(String extractedText) {
        this.extractedText = extractedText;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }
}
