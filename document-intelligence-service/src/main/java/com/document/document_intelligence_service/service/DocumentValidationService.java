package com.document.document_intelligence_service.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class DocumentValidationService {

    public Map<String, String> validate(String documentType, Map<String, String> extractedFields) {
        Map<String, String> results = new HashMap<>();
        if (extractedFields == null) {
            results.put("documentStatus", "ERROR");
            results.put("reason", "No extracted fields available.");
            return results;
        }

        String normalizedType = documentType == null ? "UNKNOWN" : documentType.toUpperCase(Locale.ROOT).trim();
        boolean isValid = true;
        StringBuilder note = new StringBuilder();

        switch (normalizedType) {
            case "CAMEROON_CNI":
            case "IDENTITY_CARD":
                isValid &= validateFieldPresent(results, extractedFields, "idNumber", "Document number missing.");
                isValid &= validateFieldPresent(results, extractedFields, "surname", "Surname missing.");
                isValid &= validateFieldPresent(results, extractedFields, "birthDate", "Birth date missing.");
                break;
            case "CAMEROON_PASSPORT":
            case "PASSPORT":
                isValid &= validateFieldPresent(results, extractedFields, "documentNumber", "Passport number missing.");
                isValid &= validateFieldPresent(results, extractedFields, "surname", "Surname missing.");
                isValid &= validateFieldPresent(results, extractedFields, "nationality", "Nationality missing.");
                break;
            case "ADDRESS_PROOF":
                isValid &= validateFieldPresent(results, extractedFields, "address", "Address missing.");
                break;
            case "PAYSLIP":
                isValid &= validateFieldPresent(results, extractedFields, "netPay", "Net pay missing.");
                break;
            case "BANK_STATEMENT":
                isValid &= validateFieldPresent(results, extractedFields, "accountNumber", "Account number missing.");
                break;
            case "WORK_CONTRACT":
                isValid &= validateFieldPresent(results, extractedFields, "employer", "Employer missing.");
                break;
            default:
                isValid &= validateFieldPresent(results, extractedFields, "documentTitle", "Document title or type could not be identified.");
                break;
        }

        String overallStatus = isValid ? "VALID" : "REVIEW";
        results.put("documentStatus", overallStatus);
        if (note.length() > 0) {
            results.put("validationNotes", note.toString().trim());
        }

        return results;
    }

    private boolean validateFieldPresent(Map<String, String> results, Map<String, String> extractedFields, String field, String message) {
        String value = extractedFields.get(field);
        boolean present = value != null && !value.isBlank();
        results.put(field + "Present", String.valueOf(present));
        if (!present) {
            results.put(field + "Message", message);
        }
        return present;
    }
}
