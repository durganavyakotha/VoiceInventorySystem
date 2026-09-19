package com.inventory.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Free translation: MyMemory API + local phrase dictionary fallback.
 */
@Service
public class TranslationService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Pattern TRANSLATED_TEXT = Pattern.compile(
            "\"translatedText\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private static final Map<String, Map<String, String>> DICTIONARY = new HashMap<>();

    static {
        Map<String, String> enTe = new HashMap<>();
        enTe.put("hello", "నమస్కారం");
        enTe.put("hi", "హాయ్");
        enTe.put("need", "కావాలి");
        enTe.put("rice", "బియ్యం");
        enTe.put("oil", "నూనె");
        enTe.put("soap", "సబ్బు");
        enTe.put("order", "ఆర్డర్");
        enTe.put("thank you", "ధన్యవాదాలు");
        enTe.put("how much", "ఎంత");
        enTe.put("available", "అందుబాటులో ఉంది");
        enTe.put("yes", "అవును");
        enTe.put("no", "కాదు");
        enTe.put("please send", "దయచేసి పంపండి");
        enTe.put("i need", "నాకు కావాలి");
        DICTIONARY.put("en-te", enTe);

        Map<String, String> teEn = new HashMap<>();
        teEn.put("నమస్కారం", "hello");
        teEn.put("కావాలి", "need");
        teEn.put("బియ్యం", "rice");
        teEn.put("నూనె", "oil");
        teEn.put("సబ్బు", "soap");
        teEn.put("ధన్యవాదాలు", "thank you");
        teEn.put("అవును", "yes");
        teEn.put("కాదు", "no");
        DICTIONARY.put("te-en", teEn);

        Map<String, String> enHi = new HashMap<>();
        enHi.put("hello", "नमस्ते");
        enHi.put("hi", "नमस्ते");
        enHi.put("need", "चाहिए");
        enHi.put("rice", "चावल");
        enHi.put("oil", "तेल");
        enHi.put("soap", "साबुन");
        enHi.put("thank you", "धन्यवाद");
        enHi.put("yes", "हाँ");
        enHi.put("no", "नहीं");
        enHi.put("i need", "मुझे चाहिए");
        enHi.put("available", "उपलब्ध है");
        DICTIONARY.put("en-hi", enHi);

        Map<String, String> hiEn = new HashMap<>();
        hiEn.put("नमस्ते", "hello");
        hiEn.put("चाहिए", "need");
        hiEn.put("चावल", "rice");
        hiEn.put("तेल", "oil");
        hiEn.put("साबुन", "soap");
        hiEn.put("धन्यवाद", "thank you");
        hiEn.put("हाँ", "yes");
        hiEn.put("नहीं", "no");
        DICTIONARY.put("hi-en", hiEn);

        Map<String, String> enTa = new HashMap<>();
        enTa.put("hello", "வணக்கம்");
        enTa.put("need", "வேண்டும்");
        enTa.put("rice", "அரிசி");
        enTa.put("oil", "எண்ணெய்");
        enTa.put("thank you", "நன்றி");
        DICTIONARY.put("en-ta", enTa);

        Map<String, String> enKn = new HashMap<>();
        enKn.put("hello", "ನಮಸ್ಕಾರ");
        enKn.put("need", "ಬೇಕು");
        enKn.put("rice", "ಅಕ್ಕಿ");
        enKn.put("oil", "ಎಣ್ಣೆ");
        enKn.put("thank you", "ಧನ್ಯವಾದಗಳು");
        DICTIONARY.put("en-kn", enKn);
    }

    private static String norm(String lang) {
        if (lang == null) return "en";
        String l = lang.toLowerCase(Locale.ROOT);
        if (l.startsWith("te") || l.contains("telugu")) return "te";
        if (l.startsWith("hi") || l.contains("hindi")) return "hi";
        if (l.startsWith("ta") || l.contains("tamil")) return "ta";
        if (l.startsWith("kn") || l.contains("kannada")) return "kn";
        return "en";
    }

    public String translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String src = norm(sourceLang);
        String tgt = norm(targetLang);
        if (src.equals(tgt)) {
            return text;
        }

        String local = translateLocal(text, src, tgt);
        if (local != null) {
            return local;
        }

        try {
            String apiResult = translateMyMemory(text, src, tgt);
            if (apiResult != null && !apiResult.isBlank()) {
                return apiResult;
            }
        } catch (Exception ignored) {
            // offline fallback
        }

        return text;
    }

    private String translateLocal(String text, String src, String tgt) {
        Map<String, String> dict = DICTIONARY.get(src + "-" + tgt);
        if (dict == null) return null;
        String lower = text.toLowerCase(Locale.ROOT).trim();
        if (dict.containsKey(lower)) {
            return dict.get(lower);
        }
        String bestKey = null;
        for (String key : dict.keySet()) {
            if (lower.contains(key) && (bestKey == null || key.length() > bestKey.length())) {
                bestKey = key;
            }
        }
        if (bestKey != null) {
            return text.replaceAll("(?i)" + Pattern.quote(bestKey), dict.get(bestKey));
        }
        StringBuilder sb = new StringBuilder();
        boolean any = false;
        for (String word : text.split("\\s+")) {
            String t = dict.get(word.toLowerCase(Locale.ROOT));
            if (t != null) {
                any = true;
                sb.append(t);
            } else {
                sb.append(word);
            }
            sb.append(' ');
        }
        return any ? sb.toString().trim() : null;
    }

    private String translateMyMemory(String text, String src, String tgt) throws Exception {
        String langpair = src + "|" + tgt;
        String url = "https://api.mymemory.translated.net/get?q="
                + URLEncoder.encode(text, StandardCharsets.UTF_8)
                + "&langpair=" + URLEncoder.encode(langpair, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return null;
        }
        Matcher m = TRANSLATED_TEXT.matcher(response.body());
        if (!m.find()) {
            return null;
        }
        String out = m.group(1)
                .replace("\\u", "\\u")
                .replace("\\\"", "\"")
                .replace("\\n", " ")
                .replace("\\\\", "\\");
        // Decode basic unicode escapes \u0C00
        out = decodeUnicodeEscapes(out);
        if (out.toUpperCase(Locale.ROOT).contains("MYMEMORY WARNING")) {
            return null;
        }
        return out;
    }

    private static String decodeUnicodeEscapes(String s) {
        Matcher m = Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            char ch = (char) Integer.parseInt(m.group(1), 16);
            m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(ch)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public String toSpeechLocale(String lang) {
        return switch (norm(lang)) {
            case "te" -> "te-IN";
            case "hi" -> "hi-IN";
            case "ta" -> "ta-IN";
            case "kn" -> "kn-IN";
            default -> "en-IN";
        };
    }
}
