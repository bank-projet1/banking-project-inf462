package com.document.document_intelligence_service.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentExtractionService {

    public Map<String, String> extractFields(String documentType, String text) {
        if (text == null) {
            text = "";
        }

        String normalizedType = documentType == null ? "UNKNOWN" : documentType.toUpperCase(Locale.ROOT).trim();
        String cleanedText = normalizeText(text);
        Map<String, String> fields = new LinkedHashMap<>();

        fields.put("documentType", normalizedType);
        fields.put("rawText", cleanedText);

        switch (normalizedType) {
            case "CAMEROON_CNI":
            case "IDENTITY_CARD":
                extractCniFields(fields, cleanedText);
                break;
            case "CAMEROON_PASSPORT":
            case "PASSPORT":
                extractPassportFields(fields, cleanedText);
                break;
            case "ADDRESS_PROOF":
                extractAddressProofFields(fields, cleanedText);
                break;
            case "PAYSLIP":
                extractPayslipFields(fields, cleanedText);
                break;
            case "BANK_STATEMENT":
                extractBankStatementFields(fields, cleanedText);
                break;
            case "WORK_CONTRACT":
                extractWorkContractFields(fields, cleanedText);
                break;
            case "ADMINISTRATION_DOCUMENT":
                extractAdministrationFields(fields, cleanedText);
                break;
            default:
                extractGenericFields(fields, cleanedText);
                break;
        }

        return fields;
    }

    private void extractCniFields(Map<String, String> fields, String text) {
        fields.put("idNumber", findFirstGroup(text,
                "(?:N(?:°|º|O|o)|NO|NUMERO|NUMÉRO|CNI)[\\s]*[:\\-]?[\\s]*([A-Z0-9 ]{4,30})(?=\\s|$)",
                "CARTE NATIONALE D'IDENTITÉ[\\s]*[:\\-]?[\\s]*(?:N(?:°|º|O|o)|NO|NUMERO|NUMÉRO)[\\s]*[:\\-]?[\\s]*([A-Z0-9 ]{4,30})(?=\\s|$)"));
        fields.put("surname", findFirstGroup(text,
                "(?:NOM|NAME)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ' ]+?)(?=\\s+(?:PRENOMS|PRÉNOMS|DATE|LIEU|SEXE|SEX|CNI|$))"));
        fields.put("givenNames", findFirstGroup(text,
                "(?:PRENOMS|PRÉNOMS|GIVEN NAMES|GIVEN NAME)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ' ]+?)(?=\\s+(?:DATE|LIEU|SEXE|SEX|NOM|NAME|$))"));
        fields.put("sex", findFirstGroup(text,
                "(?:SEXE|SEX)[\\s]*[:\\-]?[\\s]*(MASCULIN|FEMININ|MALE|FEMALE|M|F)(?=\\s|$)"));
        fields.put("birthDate", findFirstGroup(text,
                "(?:DATE DE NAISSANCE|DATE OF BIRTH)[\\s]*[:\\-]?[\\s]*([0-3]?\\d[\\/\\-][0-1]?\\d[\\/\\-][0-9]{2,4})(?=\\s|$)"));
        fields.put("birthPlace", findFirstGroup(text,
                "(?:LIEU DE NAISSANCE|PLACE OF BIRTH)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ' ]+?)(?=\\s+(?:DATE|SEXE|SEX|NOM|NAME|$))"));
    }

    private void extractPassportFields(Map<String, String> fields, String text) {
        fields.put("documentNumber", findFirstGroup(text,
                "(?:NUMERO|NUMBER|N(?:°|º|O|o)|NO|PASSPORT\s*NO)[\\s]*[:\\-]?[\\s]*([A-Z0-9]{6,20})"));
        fields.put("surname", findFirstGroup(text,
                "(?:NOM|NAME|SURNAME)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ' ]{2,50})"));
        fields.put("givenNames", findFirstGroup(text,
                "(?:PRENOMS|PRÉNOMS|GIVEN NAMES|GIVEN NAME|FORNAMES)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ' ]{2,80})"));
        fields.put("nationality", findFirstGroup(text,
                "(?:NATIONALITE|NATIONALITY)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ ]{2,50})"));
        fields.put("birthDate", findFirstGroup(text,
                "(?:DATE DE NAISSANCE|DATE OF BIRTH)[\\s]*[:\\-]?[\\s]*([0-3]?\\d[\\/\\-][0-1]?\\d[\\/\\-][0-9]{2,4})"));
        fields.put("expiryDate", findFirstGroup(text,
                "(?:DATE D'EXPIRATION|EXPIRY DATE|VALID UNTIL)[\\s]*[:\\-]?[\\s]*([0-3]?\\d[\\/\\-][0-1]?\\d[\\/\\-][0-9]{2,4})"));
    }

    private void extractAddressProofFields(Map<String, String> fields, String text) {
        fields.put("fullName", findFirstGroup(text,
                "(?:NOM|NAME|BÉNÉFICIAIRE|BENEFICIARY)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ0-9' ]{2,80})"));
        fields.put("address", findFirstGroup(text,
                "(?:ADRESSE|ADDRESS)[\\s]*[:\\-]?[\\s]*([A-Z0-9ÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ' ,.-]{5,120})"));
        fields.put("city", findFirstGroup(text,
                "(?:VILLE|CITY)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ' -]{2,60})"));
        fields.put("postalCode", findFirstGroup(text,
                "(?:CODE POSTAL|POSTAL CODE)[\\s]*[:\\-]?[\\s]*([0-9]{4,6})"));
        fields.put("issueDate", findFirstGroup(text,
                "(?:DATE|DATE D'EMISSION|ISSUE DATE)[\\s]*[:\\-]?[\\s]*([0-3]?\\d[\\/\\-][0-1]?\\d[\\/\\-][0-9]{2,4})"));
    }

    private void extractPayslipFields(Map<String, String> fields, String text) {
        fields.put("employer", findFirstGroup(text,
                "(?:EMPLOYEUR|EMPLOYER)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ0-9' ]{2,80})"));
        fields.put("employee", findFirstGroup(text,
                "(?:EMPLOYE|EMPLOYEE)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ0-9' ]{2,80})"));
        fields.put("payPeriod", findFirstGroup(text,
                "(?:PERIODE|PERIOD|MOIS)[\\s]*[:\\-]?[\\s]*([A-Z0-9 /-]{2,50})"));
        fields.put("netPay", findFirstGroup(text,
                "(?:NET A PAYER|NET PAY|NET)[\\s]*[:\\-]?[\\s]*([0-9]+(?:[.,][0-9]{2})?)"));
        fields.put("grossPay", findFirstGroup(text,
                "(?:BRUT|GROSS TOTAL|GROSS PAY)[\\s]*[:\\-]?[\\s]*([0-9]+(?:[.,][0-9]{2})?)"));
    }

    private void extractBankStatementFields(Map<String, String> fields, String text) {
        fields.put("bankName", findFirstGroup(text,
                "(?:BANQUE|BANK)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ0-9' ]{3,80})"));
        fields.put("accountNumber", findFirstGroup(text,
                "(?:COMPTE|ACCOUNT)[\\s]*(?:N(?:°|º|O|o)|NO|NUMBER)?[\\s]*[:\\-]?[\\s]*([A-Z0-9]{6,40})"));
        fields.put("statementPeriod", findFirstGroup(text,
                "(?:PERIODE|PERIOD|FROM)[\\s]*[:\\-]?[\\s]*([A-Z0-9 /-]{2,80})"));
        fields.put("balance", findFirstGroup(text,
                "(?:SOLDE|BALANCE)[\\s]*[:\\-]?[\\s]*([0-9]+(?:[.,][0-9]{2})?)"));
    }

    private void extractWorkContractFields(Map<String, String> fields, String text) {
        fields.put("employer", findFirstGroup(text,
                "(?:EMPLOYEUR|EMPLOYER)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ0-9' ]{2,80})"));
        fields.put("employee", findFirstGroup(text,
                "(?:SALARIÉ|SALARIE|EMPLOYEE)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ0-9' ]{2,80})"));
        fields.put("startDate", findFirstGroup(text,
                "(?:DATE DE DÉBUT|DATE DE DEBUT|START DATE)[\\s]*[:\\-]?[\\s]*([0-3]?\\d[\\/\\-][0-1]?\\d[\\/\\-][0-9]{2,4})"));
        fields.put("contractType", findFirstGroup(text,
                "(?:TYPE DE CONTRAT|CONTRACT TYPE|CONTRACT)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ0-9' ]{2,80})"));
        fields.put("salary", findFirstGroup(text,
                "(?:SALAIRE|SALARY|REMUNERATION)[\\s]*[:\\-]?[\\s]*([0-9]+(?:[.,][0-9]{2})?)"));
    }

    private void extractAdministrationFields(Map<String, String> fields, String text) {
        fields.put("referenceNumber", findFirstGroup(text,
                "(?:REFERENCE|REF|N(?:°|º|O|o)|NO|NUMERO)[\\s]*[:\\-]?[\\s]*([A-Z0-9]{4,30})"));
        fields.put("issuer", findFirstGroup(text,
                "(?:EMETTEUR|EMETTEUR|ISSUED BY|ISSUER)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ0-9' ]{2,80})"));
        fields.put("issueDate", findFirstGroup(text,
                "(?:DATE|DATE D'EMISSION|ISSUE DATE)[\\s]*[:\\-]?[\\s]*([0-3]?\\d[\\/\\-][0-1]?\\d[\\/\\-][0-9]{2,4})"));
        fields.put("fullName", findFirstGroup(text,
                "(?:NOM|NAME|BÉNÉFICIAIRE|BENEFICIARY)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ0-9' ]{2,80})"));
    }

    private void extractGenericFields(Map<String, String> fields, String text) {
        fields.put("documentTitle", findFirstGroup(text,
                "(?m)^(.*?(?:DOCUMENT|STATEMENT|CERTIFICATE|CONTRACT|CONTRAT|PASSPORT|BULLETIN|RELEV[EÉ] |JUSTIFICATIF).*?)$"));
        fields.put("primaryContact", findFirstGroup(text,
                "(?:NOM|NAME|ADRESSE|ADDRESS)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ0-9' ,.-]{5,80})"));
    }

    private String findFirstGroup(String text, String... patterns) {
        if (text == null || text.isBlank()) {
            return "";
        }

        for (String regex : patterns) {
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find() && matcher.groupCount() >= 1) {
                String value = matcher.group(1).trim();
                if (!value.isBlank()) {
                    return normalizeResult(value);
                }
            }
        }

        return "";
    }

    private String normalizeText(String text) {
        return text.replaceAll("[\r\n\t]+", " ")
                .replaceAll(" +", " ")
                .trim();
    }

    private String normalizeResult(String value) {
        return value.replaceAll("[\u00A0]+", " ").trim();
    }
}
