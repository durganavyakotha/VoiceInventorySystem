package com.inventory.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Phonetic / regional near-word aliases for product names (free offline). */
public final class ProductAliases {

    private static final Map<String, String> ALIAS_TO_CANONICAL = new HashMap<>();

    static {
        alias("Rice", "rice", "rais", "ryce", "biyyam", "biyam", "chawal", "chaval", "akki", "arisi");
        alias("Oil", "oil", "oyl", "nune", "tel", "ennai", "enne");
        alias("Soap", "soap", "sope", "sabbu", "sabun", "saboon");
        alias("Pepsi", "pepsi", "pepsi bottle", "pepsy", "pepsii");
        alias("Biscuits", "biscuits", "biscuit", "biskut", "biskoot", "cookies", "cookie");
        alias("Sugar", "sugar", "chekkera", "chini", "sakkare");
        alias("Salt", "salt", "uppu", "namak");
        alias("Milk", "milk", "paalu", "doodh", "haal");
        alias("Wheat", "wheat", "godhumalu", "gehu", "godhi");
        alias("Dal", "dal", "pappu", "daal", "lentils");
    }

    private ProductAliases() {}

    private static void alias(String canonical, String... aliases) {
        ALIAS_TO_CANONICAL.put(canonical.toLowerCase(Locale.ROOT), canonical);
        for (String a : aliases) {
            ALIAS_TO_CANONICAL.put(a.toLowerCase(Locale.ROOT), canonical);
        }
    }

    public static String resolve(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        String key = raw.trim().toLowerCase(Locale.ROOT);
        if (ALIAS_TO_CANONICAL.containsKey(key)) {
            return ALIAS_TO_CANONICAL.get(key);
        }
        for (String token : key.split("\\s+")) {
            if (ALIAS_TO_CANONICAL.containsKey(token)) {
                return ALIAS_TO_CANONICAL.get(token);
            }
        }
        return raw.trim();
    }

    public static Set<String> knownNames() {
        return Set.copyOf(ALIAS_TO_CANONICAL.values());
    }
}
