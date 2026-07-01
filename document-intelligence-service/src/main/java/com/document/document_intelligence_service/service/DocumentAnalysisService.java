package com.document.document_intelligence_service.service;

import com.document.document_intelligence_service.model.CniData;
import com.document.document_intelligence_service.model.CountryInfo;
import com.document.document_intelligence_service.model.DocumentAnalysisResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.function.Consumer;

@Service
public class DocumentAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(DocumentAnalysisService.class);

    private final OcrService ocrService;
    private final DocumentClassifierService classifierService;
    private final CountryDetectionService countryDetectionService;
    private final CniExtractionService cniExtractionService;
    private final FaceDetectionService faceDetectionService;
    private final DocumentExtractionService documentExtractionService;
    private final DocumentValidationService documentValidationService;

    public DocumentAnalysisService(
            OcrService ocrService,
            DocumentClassifierService classifierService,
            CountryDetectionService countryDetectionService,
            CniExtractionService cniExtractionService,
            FaceDetectionService faceDetectionService,
            DocumentExtractionService documentExtractionService,
            DocumentValidationService documentValidationService) {
        this.ocrService = ocrService;
        this.classifierService = classifierService;
        this.countryDetectionService = countryDetectionService;
        this.cniExtractionService = cniExtractionService;
        this.faceDetectionService = faceDetectionService;
        this.documentExtractionService = documentExtractionService;
        this.documentValidationService = documentValidationService;
    }

    public DocumentAnalysisResponse analyze(MultipartFile file, String preferredDocumentType) {
        String extractedText = ocrService.extractText(file);
        CniData cniData = cniExtractionService.extract(extractedText);

        String documentType = preferredDocumentType != null && !preferredDocumentType.isBlank()
                ? preferredDocumentType.toUpperCase().trim()
                : classifierService.classify(extractedText);

        if ("UNKNOWN".equals(documentType) && hasExtractedCniId(cniData)) {
            documentType = "CAMEROON_CNI";
        }

        if ("UNKNOWN".equals(documentType) && containsCameroonHint(extractedText)) {
            documentType = "CAMEROON_CNI";
        }

        CountryInfo countryInfo = countryDetectionService.detect(documentType, extractedText);
        Map<String, String> extractedFields = documentExtractionService.extractFields(documentType, extractedText);
        Map<String, String> verificationResults = documentValidationService.validate(documentType, extractedFields);
        int faceCount = faceDetectionService.countFaces(file);
        boolean faceDetected = faceCount > 0;

        DocumentAnalysisResponse response = new DocumentAnalysisResponse();
        response.setDocumentType(documentType);
        response.setDocumentStatus(verificationResults.getOrDefault("documentStatus", "REVIEW"));
        response.setExtractedFields(extractedFields);
        response.setVerificationResults(verificationResults);
        response.setProcessingNotes("OCR and document intelligence analysis completed.");
        response.setCountry(countryInfo.getCountry());
        response.setFaceDetected(faceDetected);
        response.setFaceCount(faceCount);
        response.setCountryCode(countryInfo.getCountryCode());
        response.setFlag(countryInfo.getFlag());
        response.setExtractedText(extractedText);

        if (cniData != null) {
            if (cniData.getIdNumber() != null && !cniData.getIdNumber().isBlank()) {
                response.setIdNumber(cniData.getIdNumber());
            }
            if (cniData.getSurname() != null && !cniData.getSurname().isBlank()) {
                response.setSurname(cniData.getSurname());
            }
            if (cniData.getGivenNames() != null && !cniData.getGivenNames().isBlank()) {
                response.setGivenNames(cniData.getGivenNames());
            }
            if (cniData.getSex() != null && !cniData.getSex().isBlank()) {
                response.setSex(cniData.getSex());
            }
            if (cniData.getBirthDate() != null && !cniData.getBirthDate().isBlank()) {
                response.setBirthDate(cniData.getBirthDate());
            }
            if (cniData.getBirthPlace() != null && !cniData.getBirthPlace().isBlank()) {
                response.setBirthPlace(cniData.getBirthPlace());
            }
        }

        if (extractedFields.containsKey("idNumber") && response.getIdNumber() == null) {
            response.setIdNumber(extractedFields.get("idNumber"));
        }
        if (extractedFields.containsKey("surname") && response.getSurname() == null) {
            response.setSurname(extractedFields.get("surname"));
        }
        if (extractedFields.containsKey("givenNames") && response.getGivenNames() == null) {
            response.setGivenNames(extractedFields.get("givenNames"));
        }
        if (extractedFields.containsKey("sex") && response.getSex() == null) {
            response.setSex(extractedFields.get("sex"));
        }
        if (extractedFields.containsKey("birthDate") && response.getBirthDate() == null) {
            response.setBirthDate(extractedFields.get("birthDate"));
        }
        if (extractedFields.containsKey("birthPlace") && response.getBirthPlace() == null) {
            response.setBirthPlace(extractedFields.get("birthPlace"));
        }

        copyExtractedField(extractedFields, response, "documentNumber", response::setDocumentNumber);
        copyExtractedField(extractedFields, response, "nationality", response::setNationality);
        copyExtractedField(extractedFields, response, "expiryDate", response::setExpiryDate);
        copyExtractedField(extractedFields, response, "fullName", response::setFullName);
        copyExtractedField(extractedFields, response, "address", response::setAddress);
        copyExtractedField(extractedFields, response, "city", response::setCity);
        copyExtractedField(extractedFields, response, "postalCode", response::setPostalCode);
        copyExtractedField(extractedFields, response, "issueDate", response::setIssueDate);
        copyExtractedField(extractedFields, response, "employer", response::setEmployer);
        copyExtractedField(extractedFields, response, "employee", response::setEmployee);
        copyExtractedField(extractedFields, response, "payPeriod", response::setPayPeriod);
        copyExtractedField(extractedFields, response, "netPay", response::setNetPay);
        copyExtractedField(extractedFields, response, "grossPay", response::setGrossPay);
        copyExtractedField(extractedFields, response, "bankName", response::setBankName);
        copyExtractedField(extractedFields, response, "accountNumber", response::setAccountNumber);
        copyExtractedField(extractedFields, response, "statementPeriod", response::setStatementPeriod);
        copyExtractedField(extractedFields, response, "balance", response::setBalance);
        copyExtractedField(extractedFields, response, "startDate", response::setStartDate);
        copyExtractedField(extractedFields, response, "contractType", response::setContractType);
        copyExtractedField(extractedFields, response, "salary", response::setSalary);
        copyExtractedField(extractedFields, response, "referenceNumber", response::setReferenceNumber);
        copyExtractedField(extractedFields, response, "issuer", response::setIssuer);
        copyExtractedField(extractedFields, response, "documentTitle", response::setDocumentTitle);
        copyExtractedField(extractedFields, response, "primaryContact", response::setPrimaryContact);

        log.debug("Document analysis complete: type={}, country={}, faceDetected={}", documentType, countryInfo.getCountry(), faceDetected);
        return response;
    }

    private boolean containsCameroonHint(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String normalized = normalizeText(text);
        String cleaned = normalized.replaceAll("[^A-Z0-9 ]", " ").replaceAll(" +", " ").trim();

        return containsAny(cleaned,
                "REPUBLIQUE DU CAMEROUN",
                "REPUBLIC OF CAMEROON",
                "CAMEROUN",
                "CAMEROON",
                "CARTE NATIONALE DIDENTITE",
                "CARTE NATIONALE D IDENTITE",
                "CARTE NATIONALE",
                "NATIONAL IDENTITY CARD",
                "IDENTITE",
                "IDENTITY",
                "CNI",
                "C N I");
    }

    private String normalizeText(String text) {
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        return normalized.toUpperCase(java.util.Locale.ROOT);
    }

    private boolean containsAny(String text, String... candidates) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String candidate : candidates) {
            if (candidate != null && text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasExtractedCniId(CniData cniData) {
        if (cniData == null) {
            return false;
        }
        String idNumber = cniData.getIdNumber();
        return idNumber != null && !idNumber.isBlank() && !"NOT_FOUND".equalsIgnoreCase(idNumber);
    }

    private void copyExtractedField(Map<String, String> extractedFields,
                                    DocumentAnalysisResponse response,
                                    String fieldName,
                                    Consumer<String> setter) {
        if (extractedFields.containsKey(fieldName)) {
            String value = extractedFields.get(fieldName);
            if (value != null && !value.isBlank()) {
                setter.accept(value);
            }
        }
    }
}
