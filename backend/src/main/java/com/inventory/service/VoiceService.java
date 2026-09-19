package com.inventory.service;

import com.inventory.dto.InventoryResponse;
import com.inventory.dto.VoiceCommandRequest;
import com.inventory.dto.VoiceCommandResponse;
import com.inventory.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class VoiceService {

    private final InventoryService inventoryService;
    private final VendorService vendorService;
    private final UserService userService;

    private static final Pattern ADD_PATTERN = Pattern.compile(
            "(?i).*(?:add|stock\\s*add|cheyyi|pettu|add\\s*cheyyi|stock\\s*cheyyi|जोड़ो|जोड़|சேர்).*");

    private static final Pattern REMOVE_PATTERN = Pattern.compile(
            "(?i).*(?:remove|delete|theeyandi|thiyyi|remove\\s*cheyyi|हटाओ|நீக்கு).*");

    private static final Pattern LOW_STOCK_PATTERN = Pattern.compile(
            "(?i).*(?:low\\s*stock|thakkuva|takkuva|show\\s*my\\s*low|low\\s*items).*");

    private static final Pattern UNIT_PATTERN = Pattern.compile(
            "(?i)\\b(kgs?|kilograms?|grams?|g|litres?|liters?|l|ml|packets?|packs?|bottles?|pieces?|pcs?|units?|items?|bags?)\\b");

    public VoiceCommandResponse process(VoiceCommandRequest request) {
        String command = request.getCommand() != null ? request.getCommand().trim() : "";
        if (command.isBlank()) {
            return fail("UNKNOWN", localize("empty", lang()));
        }

        String normalized = command.toLowerCase(Locale.ROOT);
        String language = request.getLanguage() != null ? request.getLanguage() : lang();

        if (LOW_STOCK_PATTERN.matcher(normalized).matches()
                || normalized.contains("low stock")
                || normalized.contains("thakkuva stock")) {
            return handleLowStock(language);
        }

        if (isVendorCommand(normalized)) {
            return handleFindVendors(command, normalized, language);
        }

        if (REMOVE_PATTERN.matcher(normalized).matches()
                || normalized.contains("remove")
                || normalized.contains("thiyyi")
                || normalized.contains("theeyandi")) {
            return handleRemove(command, language);
        }

        if (ADD_PATTERN.matcher(normalized).matches()
                || normalized.contains("add")
                || normalized.contains("pettu")
                || normalized.contains("cheyyi")) {
            return handleAdd(command, language);
        }

        return fail("UNKNOWN", localize("unknown", language) + ": " + command);
    }

    private VoiceCommandResponse handleAdd(String command, String language) {
        ParsedQtyProduct parsed = parseQtyProduct(command);
        if (parsed == null) {
            return fail("ADD_STOCK", localize("need_qty_product", language));
        }
        InventoryResponse item = inventoryService.addStockByProductName(parsed.product, parsed.qty, parsed.unit);
        String spoken = formatAdded(parsed.product, parsed.qty, item.getUnit(), language);
        return VoiceCommandResponse.builder()
                .action("ADD_STOCK")
                .success(true)
                .spokenResponse(spoken)
                .data(item)
                .build();
    }

    private VoiceCommandResponse handleRemove(String command, String language) {
        ParsedQtyProduct parsed = parseQtyProduct(command);
        if (parsed == null) {
            return fail("REMOVE_STOCK", localize("need_qty_product", language));
        }
        InventoryResponse item = inventoryService.removeStockByProductName(parsed.product, parsed.qty);
        String spoken = formatRemoved(parsed.product, parsed.qty, item.getUnit(), item.getQuantity(), language);
        return VoiceCommandResponse.builder()
                .action("REMOVE_STOCK")
                .success(true)
                .spokenResponse(spoken)
                .data(item)
                .build();
    }

    private VoiceCommandResponse handleLowStock(String language) {
        List<InventoryResponse> low = inventoryService.listLowStock();
        String spoken;
        if (low.isEmpty()) {
            spoken = localize("no_low", language);
        } else {
            String names = low.stream()
                    .map(i -> i.getProductName() + " (" + i.getQuantity() + " " + i.getUnit() + ")")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            spoken = localize("low_list", language) + ": " + names;
        }
        return VoiceCommandResponse.builder()
                .action("LIST_LOW_STOCK")
                .success(true)
                .spokenResponse(spoken)
                .data(low)
                .build();
    }

    private VoiceCommandResponse handleFindVendors(String command, String normalized, String language) {
        String product = extractVendorProduct(command);
        double radius = extractRadius(normalized);
        if (product == null || product.isBlank()) {
            return fail("FIND_VENDORS", localize("need_vendor", language));
        }
        List<Map<String, Object>> vendors = vendorService.searchNearby(product, radius, 1, null, null);
        String spoken = vendors.isEmpty()
                ? localize("no_vendors", language) + " " + product
                : localize("found_vendors", language) + " " + vendors.size() + " — " + product;
        return VoiceCommandResponse.builder()
                .action("FIND_VENDORS")
                .success(true)
                .spokenResponse(spoken)
                .vendors(vendors)
                .data(vendors)
                .build();
    }

    private boolean isVendorCommand(String normalized) {
        return normalized.contains("vendor")
                || normalized.contains("chupinchu")
                || normalized.contains("supplier");
    }

    private String extractVendorProduct(String command) {
        Matcher m = Pattern.compile("(?i)([a-zA-Z\\u0C00-\\u0C7F][\\w\\u0C00-\\u0C7F]*)\\s+vendors?").matcher(command);
        if (m.find()) {
            return cleanProduct(m.group(1));
        }
        String[] parts = command.split("\\s+");
        if (parts.length > 0) {
            return cleanProduct(parts[0]);
        }
        return null;
    }

    private double extractRadius(String normalized) {
        Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:km|kilometers?|kms?)").matcher(normalized);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return 5.0;
    }

    private ParsedQtyProduct parseQtyProduct(String command) {
        // "10 kg rice add" / "Rice 10 kg add cheyyi" / "10 biscuit packets add"
        Matcher m = Pattern.compile(
                "(?i)(\\d+)\\s*(kgs?|kilograms?|grams?|g|litres?|liters?|l|ml|packets?|packs?|bottles?|pieces?|pcs?|units?|items?|bags?)?\\s+" +
                        "([a-zA-Z\\u0C00-\\u0C7F][\\w\\s\\u0C00-\\u0C7F]*?)\\s*" +
                        "(?:add|remove|cheyyi|pettu|theeyandi|thiyyi|stock|जोड़ो|हटाओ)?").matcher(command);
        if (m.find()) {
            int qty = Integer.parseInt(m.group(1));
            String unit = m.group(2) != null ? m.group(2) : extractTrailingUnit(m.group(3));
            String product = cleanProduct(stripUnit(m.group(3)));
            if (unit == null) {
                unit = "pieces";
            }
            return new ParsedQtyProduct(qty, product, InventoryService.normalizeUnit(unit));
        }

        // "Rice — 10 kg" / "Biscuit packets — 10"
        m = Pattern.compile("(?i)([a-zA-Z\\u0C00-\\u0C7F][\\w\\s\\u0C00-\\u0C7F]+?)\\s*[—\\-:]\\s*(\\d+)\\s*" +
                "(kgs?|kilograms?|packets?|packs?|bottles?|pieces?|bags?|l|ml|g)?").matcher(command);
        if (m.find()) {
            String productPart = m.group(1);
            int qty = Integer.parseInt(m.group(2));
            String unit = m.group(3);
            if (unit == null) {
                unit = extractTrailingUnit(productPart);
            }
            if (unit == null) {
                unit = "pieces";
            }
            return new ParsedQtyProduct(qty, cleanProduct(stripUnit(productPart)), InventoryService.normalizeUnit(unit));
        }

        m = Pattern.compile("(?i)(\\d+)\\s+(.+?)\\s+(?:add|remove|cheyyi|pettu|theeyandi|thiyyi|stock).*").matcher(command);
        if (m.find()) {
            String rest = m.group(2);
            String unit = extractTrailingUnit(rest);
            return new ParsedQtyProduct(Integer.parseInt(m.group(1)), cleanProduct(stripUnit(rest)),
                    InventoryService.normalizeUnit(unit != null ? unit : "pieces"));
        }
        return null;
    }

    private String extractTrailingUnit(String text) {
        if (text == null) return null;
        Matcher m = UNIT_PATTERN.matcher(text);
        String last = null;
        while (m.find()) {
            last = m.group(1);
        }
        return last;
    }

    private String stripUnit(String raw) {
        return raw.replaceAll("(?i)\\b(kgs?|kilograms?|grams?|g|litres?|liters?|l|ml|packets?|packs?|bottles?|pieces?|pcs?|units?|items?|bags?)\\b", " ");
    }

    private String cleanProduct(String raw) {
        return raw.replaceAll("(?i)\\b(ni|lo|cheyyi|add|remove|stock)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String lang() {
        try {
            User u = userService.getCurrentUser();
            return u.getLanguage() != null ? u.getLanguage() : "en";
        } catch (Exception e) {
            return "en";
        }
    }

    private String formatAdded(String product, int qty, String unit, String language) {
        String u = unit != null ? unit : "pieces";
        return switch (normalizeLang(language)) {
            case "te" -> product + " — " + qty + " " + u + " జోడించబడింది";
            case "hi" -> product + " — " + qty + " " + u + " जोड़े गए";
            case "ta" -> product + " — " + qty + " " + u + " சேர்க்கப்பட்டது";
            case "kn" -> product + " — " + qty + " " + u + " ಸೇರಿಸಲಾಗಿದೆ";
            default -> product + " — " + qty + " " + u + " added";
        };
    }

    private String formatRemoved(String product, int qty, String unit, int remaining, String language) {
        String u = unit != null ? unit : "pieces";
        return switch (normalizeLang(language)) {
            case "te" -> qty + " " + u + " " + product + " తొలగించబడింది. మిగిలినవి: " + remaining;
            case "hi" -> qty + " " + u + " " + product + " हटाए गए। शेष: " + remaining;
            case "ta" -> qty + " " + u + " " + product + " நீக்கப்பட்டது. மீதம்: " + remaining;
            case "kn" -> qty + " " + u + " " + product + " ತೆಗೆದುಹಾಕಲಾಗಿದೆ. ಉಳಿದ: " + remaining;
            default -> "Removed " + qty + " " + u + " " + product + ". Remaining: " + remaining;
        };
    }

    private String localize(String key, String language) {
        String lang = normalizeLang(language);
        return switch (key) {
            case "empty" -> switch (lang) {
                case "te" -> "దయచేసి ఒక ఆదేశం చెప్పండి.";
                case "hi" -> "कृपया एक आदेश बोलें।";
                case "ta" -> "தயவுசெய்து ஒரு கட்டளையைச் சொல்லுங்கள்.";
                case "kn" -> "ದಯವಿಟ್ಟು ಒಂದು ಆದೇಶ ಹೇಳಿ.";
                default -> "Please say a command.";
            };
            case "unknown" -> switch (lang) {
                case "te" -> "అర్థం కాలేదు";
                case "hi" -> "समझ नहीं आया";
                case "ta" -> "புரியவில்லை";
                case "kn" -> "ಅರ್ಥವಾಗಲಿಲ್ಲ";
                default -> "Sorry, I could not understand";
            };
            case "need_qty_product" -> switch (lang) {
                case "te" -> "ఉదా: 10 kg rice add cheyyi లేదా biscuit packets — 10 add";
                case "hi" -> "उदा: 10 kg rice add या biscuit packets — 10 add";
                default -> "Say quantity, unit and product, e.g. 10 kg rice add or biscuit packets — 10 added";
            };
            case "no_low" -> switch (lang) {
                case "te" -> "అన్ని వస్తువులు తగినంత స్టాక్‌లో ఉన్నాయి.";
                case "hi" -> "सभी वस्तुएँ पर्याप्त स्टॉक में हैं।";
                default -> "All items are above threshold. No low stock items.";
            };
            case "low_list" -> switch (lang) {
                case "te" -> "తక్కువ స్టాక్ వస్తువులు";
                case "hi" -> "कम स्टॉक वस्तुएँ";
                default -> "Low stock items";
            };
            case "need_vendor" -> "Say product and radius, e.g. Rice vendors ni 5 kilometers lo chupinchu";
            case "no_vendors" -> switch (lang) {
                case "te" -> "విక్రేతలు కనబడలేదు";
                case "hi" -> "कोई विक्रेता नहीं मिला";
                default -> "No vendors found for";
            };
            case "found_vendors" -> switch (lang) {
                case "te" -> "విక్రేతలు దొరికారు:";
                case "hi" -> "विक्रेता मिले:";
                default -> "Found vendors:";
            };
            default -> key;
        };
    }

    private String normalizeLang(String language) {
        if (language == null) return "en";
        String l = language.toLowerCase(Locale.ROOT);
        if (l.startsWith("te") || l.contains("telugu")) return "te";
        if (l.startsWith("hi") || l.contains("hindi")) return "hi";
        if (l.startsWith("ta") || l.contains("tamil")) return "ta";
        if (l.startsWith("kn") || l.contains("kannada")) return "kn";
        return "en";
    }

    private VoiceCommandResponse fail(String action, String message) {
        return VoiceCommandResponse.builder()
                .action(action)
                .success(false)
                .spokenResponse(message)
                .build();
    }

    private record ParsedQtyProduct(int qty, String product, String unit) {}
}
