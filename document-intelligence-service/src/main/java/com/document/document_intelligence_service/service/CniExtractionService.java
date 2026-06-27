package com.document.document_intelligence_service.service;

import com.document.document_intelligence_service.model.CniData;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CniExtractionService {

    private static final String DEFAULT_NOT_FOUND = "NOT_FOUND";

    public CniData extract(String text) {
        CniData cniData = new CniData();
        if (text == null || text.isBlank()) {
            fillNotFound(cniData);
            return cniData;
        }

        String normalized = normalizeText(text);

        cniData.setIdNumber(extractValue(normalized, "(?:N(?:°|º|O|o)|NO|NUMERO|NUMÉRO|CNI)[\\s]*[:\\-]?[\\s]*([A-Z0-9 ]{4,30})(?=\\s|$)"));
        cniData.setSurname(extractValue(normalized, "(?:NOM|NAME)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ' ]+?)(?=\\s+(?:PRENOMS|PRÉNOMS|DATE|LIEU|SEXE|SEX|CNI|$))"));
        cniData.setGivenNames(extractValue(normalized, "(?:PRENOMS|PRÉNOMS|GIVEN NAMES|GIVEN NAME)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ' ]+?)(?=\\s+(?:DATE|LIEU|SEXE|SEX|NOM|NAME|$))"));
        cniData.setSex(extractValue(normalized, "(?:SEXE|SEX)[\\s]*[:\\-]?[\\s]*(MASCULIN|FEMININ|MALE|FEMALE|M|F)(?=\\s|$)"));
        cniData.setBirthDate(extractValue(normalized, "(?:DATE DE NAISSANCE|DATE OF BIRTH)[\\s]*[:\\-]?[\\s]*([0-3]?[0-9][\\/\\-][0-1]?[0-9][\\/\\-][0-9]{2,4})(?=\\s|$)"));
        cniData.setBirthPlace(extractValue(normalized, "(?:LIEU DE NAISSANCE|PLACE OF BIRTH)[\\s]*[:\\-]?[\\s]*([A-ZÀÂÄÉÈÊËÎÏÔÖÙÛÜŸÇ' ]+?)(?=\\s+(?:DATE|SEXE|SEX|NOM|NAME|$))"));

        return cniData;
    }

    private void fillNotFound(CniData cniData) {
        cniData.setIdNumber(DEFAULT_NOT_FOUND);
        cniData.setSurname(DEFAULT_NOT_FOUND);
        cniData.setGivenNames(DEFAULT_NOT_FOUND);
        cniData.setSex(DEFAULT_NOT_FOUND);
        cniData.setBirthDate(DEFAULT_NOT_FOUND);
        cniData.setBirthPlace(DEFAULT_NOT_FOUND);
    }

    private String normalizeText(String text) {
        return text.replace("\r", " ")
                .replace("\n", " ")
                .replace("\t", " ")
                .replaceAll(" +", " ")
                .trim();
    }

    private String extractValue(String text, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String found = matcher.group(1).trim();
            if (!found.isBlank()) {
                return found;
            }
        }
        return DEFAULT_NOT_FOUND;
    }
}
