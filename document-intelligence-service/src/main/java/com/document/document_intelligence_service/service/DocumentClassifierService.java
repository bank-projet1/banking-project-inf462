package com.document.document_intelligence_service.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class DocumentClassifierService {

    public String classify(String text) {
        if (text == null || text.isBlank()) {
            return "UNKNOWN";
        }

        String normalized = normalizeText(text);
        String cleaned = normalized.replaceAll("[^A-Z0-9 ]", " ").replaceAll(" +", " ").trim();
        String compact = cleaned.replaceAll(" ", "");

        boolean hasCameroon = containsAny(cleaned,
                "REPUBLIQUE DU CAMEROUN", "REPUBLIC OF CAMEROON", "CAMEROUN", "CAMEROON")
                || containsAny(compact,
                "REPUBLIQUEDUCAMEROUN", "REPUBLICOFCAMEROON", "CAMEROUN", "CAMEROON");
        boolean hasCniKeywords = containsAny(cleaned,
                "CARTE NATIONALE DIDENTITE", "CARTE NATIONALE D IDENTITE", "CARTE NATIONALE", "NATIONAL IDENTITY CARD",
                "IDENTITE", "IDENTITY", "CNI", "CARTE")
                || containsAny(compact,
                "CARTENATIONALEDIDENTITE", "CNI", "IDENTITE", "IDENTITY", "CARTE");
        boolean hasPassportKeywords = containsAny(cleaned, "PASSEPORT", "PASSPORT")
                || containsAny(compact, "PASSEPORT", "PASSPORT");
        boolean hasIdHeaders = containsAny(cleaned,
                "NOM", "PRENOMS", "DATE DE NAISSANCE", "LIEU DE NAISSANCE", "BIRTH", "N ", "N°", "NO ", "NUMERO", "NUMÉRO")
                || containsAny(compact,
                "NOM", "PRENOMS", "DATEDENAISSANCE", "LIEUDENAISSANCE", "BIRTH", "N", "N°", "NO", "NUMERO", "NUMERO");

        if (hasCameroon && hasPassportKeywords) {
            return "CAMEROON_PASSPORT";
        }

        if (hasCameroon && (hasCniKeywords || hasIdHeaders)) {
            return "CAMEROON_CNI";
        }

        if (hasPassportKeywords) {
            return hasCameroon ? "CAMEROON_PASSPORT" : "PASSPORT";
        }

        if (hasCniKeywords && hasIdHeaders) {
            return hasCameroon ? "CAMEROON_CNI" : "IDENTITY_CARD";
        }

        if (containsAny(cleaned, "JUSTIFICATIF DE DOMICILE", "PROOF OF ADDRESS", "DOMICILE", "ADDRESS PROOF")) {
            return "ADDRESS_PROOF";
        }

        if (containsAny(cleaned, "BULLETIN DE SALAIRE", "PAYSLIP", "PAY SLIP", "PAYROLL")) {
            return "PAYSLIP";
        }

        if (containsAny(cleaned, "RELEVE BANCAIRE", "BANK STATEMENT", "STATEMENT")) {
            return "BANK_STATEMENT";
        }

        if (containsAny(cleaned, "CONTRAT DE TRAVAIL", "EMPLOYMENT CONTRACT", "WORK CONTRACT")) {
            return "WORK_CONTRACT";
        }

        if (containsAny(cleaned, "ADMINISTRATIF", "ADMINISTRATIVE", "CERFA", "ATTESTATION", "CERTIFICAT")) {
            return "ADMINISTRATION_DOCUMENT";
        }

        if (containsAny(cleaned, "CARTE NATIONALE", "NATIONAL IDENTITY CARD", "IDENTITY CARD")) {
            return hasCameroon ? "CAMEROON_CNI" : "IDENTITY_CARD";
        }

        return "UNKNOWN";
    }

    private String normalizeText(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        return normalized.toUpperCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
