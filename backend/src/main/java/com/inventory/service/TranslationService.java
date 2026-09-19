package com.inventory.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class TranslationService {

    private static final Map<String, Map<String, String>> DICTIONARY = new HashMap<>();

    static {
        Map<String, String> enToTe = new HashMap<>();
        enToTe.put("hello", "namaskaram");
        enToTe.put("need", "kavali");
        enToTe.put("rice", "biyyam");
        enToTe.put("oil", "nune");
        enToTe.put("soap", "sabbu");
        enToTe.put("order", "order");
        enToTe.put("thank you", "dhanyavadalu");
        DICTIONARY.put("en-te", enToTe);

        Map<String, String> teToEn = new HashMap<>();
        teToEn.put("namaskaram", "hello");
        teToEn.put("kavali", "need");
        teToEn.put("biyyam", "rice");
        teToEn.put("nune", "oil");
        teToEn.put("sabbu", "soap");
        teToEn.put("dhanyavadalu", "thank you");
        DICTIONARY.put("te-en", teToEn);

        Map<String, String> enToHi = new HashMap<>();
        enToHi.put("hello", "namaste");
        enToHi.put("need", "chahiye");
        enToHi.put("rice", "chawal");
        enToHi.put("oil", "tel");
        enToHi.put("soap", "sabun");
        DICTIONARY.put("en-hi", enToHi);
    }

    public String translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.isBlank()) {
            return text;
        }
        if (sourceLang == null || targetLang == null
                || sourceLang.equalsIgnoreCase(targetLang)) {
            return text;
        }

        String key = sourceLang.toLowerCase(Locale.ROOT) + "-" + targetLang.toLowerCase(Locale.ROOT);
        Map<String, String> dict = DICTIONARY.get(key);
        if (dict != null) {
            String lower = text.toLowerCase(Locale.ROOT).trim();
            if (dict.containsKey(lower)) {
                return dict.get(lower);
            }
            StringBuilder sb = new StringBuilder();
            for (String word : text.split("\\s+")) {
                String translated = dict.get(word.toLowerCase(Locale.ROOT));
                sb.append(translated != null ? translated : word).append(' ');
            }
            return "[Translated to " + targetLang + "]: " + sb.toString().trim();
        }

        return "[Translated to " + targetLang + "]: " + text;
    }
}
