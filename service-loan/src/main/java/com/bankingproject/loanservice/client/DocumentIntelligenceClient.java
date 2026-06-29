package com.bankingproject.loanservice.client;

import com.bankingproject.loanservice.dto.DocumentAnalysisDTO;
import com.bankingproject.loanservice.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class DocumentIntelligenceClient {

    private final RestTemplate restTemplate;
    private final String aiServiceUrl;

    public DocumentIntelligenceClient(
            RestTemplate restTemplate,
            @Value("${external.service.ai-url:http://localhost:5000}") String aiServiceUrl) {
        this.restTemplate = restTemplate;
        this.aiServiceUrl = aiServiceUrl;
    }

    public DocumentAnalysisDTO analyze(String documentType, MultipartFile file) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("documentType", documentType);
            body.add("file", new MultipartInputResource(file));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            return restTemplate.postForObject(
                    aiServiceUrl + "/analyze",
                    new HttpEntity<>(body, headers),
                    DocumentAnalysisDTO.class);
        } catch (IOException ex) {
            throw new ExternalServiceException("Unable to read uploaded loan document.", ex);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Unable to reach service-ai at " + aiServiceUrl, ex);
        }
    }

    private static class MultipartInputResource extends ByteArrayResource {
        private final String filename;

        MultipartInputResource(MultipartFile file) throws IOException {
            super(file.getBytes());
            this.filename = file.getOriginalFilename();
        }

        @Override
        public String getFilename() {
            return filename == null || filename.isBlank() ? "document" : filename;
        }
    }
}
