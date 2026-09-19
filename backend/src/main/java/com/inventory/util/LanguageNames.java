package com.inventory.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class LanguageNames {

    private static final Map<String, String> FULL = new LinkedHashMap<>();

    static {
        FULL.put("en", "English");
        FULL.put("te", "Telugu");
        FULL.put("hi", "Hindi");
        FULL.put("ta", "Tamil");
        FULL.put("kn", "Kannada");
    }

    private LanguageNames() {}

    public static String toFull(String code) {
        if (code == null || code.isBlank()) {
            return "English";
        }
        String c = code.trim().toLowerCase(Locale.ROOT);
        if (FULL.containsKey(c)) {
            return FULL.get(c);
        }
        for (Map.Entry<String, String> e : FULL.entrySet()) {
            if (e.getValue().equalsIgnoreCase(code.trim()) || c.startsWith(e.getKey())) {
                return e.getValue();
            }
        }
        return code;
    }

    public static String toCode(String fullOrCode) {
        if (fullOrCode == null || fullOrCode.isBlank()) {
            return null;
        }
        String v = fullOrCode.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> e : FULL.entrySet()) {
            if (e.getKey().equals(v) || e.getValue().equalsIgnoreCase(fullOrCode.trim())) {
                return e.getKey();
            }
        }
        return v.length() <= 5 ? v : null;
    }

    public static Map<String, String> all() {
        return Map.copyOf(FULL);
    }
}
