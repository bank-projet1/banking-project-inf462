package com.bankingproject.loanservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class DocumentAnalysisDTO {

    @JsonProperty("document_type")
    public String documentType;

    @JsonProperty("raw_text")
    public String rawText;

    @JsonProperty("extracted_data")
    public Map<String, Object> extractedData;

    public Double score;
    public List<String> checks;
    public String decision;
}
