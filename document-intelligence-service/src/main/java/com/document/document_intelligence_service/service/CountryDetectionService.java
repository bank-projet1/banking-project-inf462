package com.document.document_intelligence_service.service;

import com.document.document_intelligence_service.model.CountryInfo;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class CountryDetectionService {

    private static final Map<String, CountryInfo> COUNTRY_MAP = new HashMap<>();

    static {
        CountryInfo cameroon = new CountryInfo();
        cameroon.setCountry("Cameroon");
        cameroon.setCountryCode("CM");
        cameroon.setFlag("🇨🇲");
        COUNTRY_MAP.put("CAMEROON", cameroon);
        COUNTRY_MAP.put("CAMEROUN", cameroon);
        COUNTRY_MAP.put("REPUBLIQUE DU CAMEROUN", cameroon);

        CountryInfo france = new CountryInfo();
        france.setCountry("France");
        france.setCountryCode("FR");
        france.setFlag("🇫🇷");
        COUNTRY_MAP.put("FRANCE", france);

        CountryInfo unitedStates = new CountryInfo();
        unitedStates.setCountry("United States");
        unitedStates.setCountryCode("US");
        unitedStates.setFlag("🇺🇸");
        COUNTRY_MAP.put("UNITED STATES", unitedStates);

        CountryInfo unitedKingdom = new CountryInfo();
        unitedKingdom.setCountry("United Kingdom");
        unitedKingdom.setCountryCode("GB");
        unitedKingdom.setFlag("🇬🇧");
        COUNTRY_MAP.put("UNITED KINGDOM", unitedKingdom);

        CountryInfo germany = new CountryInfo();
        germany.setCountry("Germany");
        germany.setCountryCode("DE");
        germany.setFlag("🇩🇪");
        COUNTRY_MAP.put("GERMANY", germany);

        CountryInfo nigeria = new CountryInfo();
        nigeria.setCountry("Nigeria");
        nigeria.setCountryCode("NG");
        nigeria.setFlag("🇳🇬");
        COUNTRY_MAP.put("NIGERIA", nigeria);
    }

    public CountryInfo detect(String documentType, String text) {
        if (documentType != null && documentType.toUpperCase(Locale.ROOT).contains("CAMEROON")) {
            return COUNTRY_MAP.get("CAMEROON");
        }

        if (text != null && !text.isBlank()) {
            String normalized = text.toUpperCase(Locale.ROOT);
            for (Map.Entry<String, CountryInfo> entry : COUNTRY_MAP.entrySet()) {
                if (normalized.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }

        return new CountryInfo();
    }
}
