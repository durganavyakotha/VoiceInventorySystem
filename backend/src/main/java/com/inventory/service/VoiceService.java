package com.inventory.service;

import com.inventory.dto.InventoryResponse;
import com.inventory.dto.VoiceCommandRequest;
import com.inventory.dto.VoiceCommandResponse;
import com.inventory.entity.Product;
import com.inventory.entity.User;
import com.inventory.repository.ProductRepository;
import com.inventory.util.FuzzyMatcher;
import com.inventory.util.ProductAliases;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class VoiceService {

    private final InventoryService inventoryService;
    private final VendorService vendorService;
    private final UserService userService;
    private final ProductRepository productRepository;

    private static final Pattern ADD_PATTERN = Pattern.compile(
            "(?i).*(?:add|stock\\s*add|cheyyi|pettu|add\\s*cheyyi|stock\\s*cheyyi|जोड़ो|जोड़|சேர்).*");

    private static final Pattern REMOVE_PATTERN = Pattern.compile(
            "(?i).*(?:remove|delete|theeyandi|thiyyi|remove\\s*cheyyi|हटाओ|நீக்கு).*");

    private static final Pattern LOW_STOCK_PATTERN = Pattern.compile(
            "(?i).*(?:low\\s*stock|thakkuva|takkuva|show\\s*my\\s*low|low\\s*items).*");

    private static final Pattern UNIT_PATTERN = Pattern.compile(
            "(?i)\\b(kgs?|kilograms?|grams?|g|litres?|liters?|l|ml|packets?|packs?|bottles?|pieces?|pcs?|units?|items?|bags?)\\b");

    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "(?i)(?:(?:rs\\.?|rupees?|inr|₹|price|cost|rate)\\s*[:=]?\\s*(\\d+(?:\\.\\d+)?)|(\\d+(?:\\.\\d+)?)\\s*(?:rs\\.?|rupees?|inr|₹))");

    public VoiceCommandResponse process(VoiceCommandRequest request) {
        String command = request.getCommand() != null ? request.getCommand().trim() : "";
        if (command.isBlank()) {
            return fail("UNKNOWN", localize("empty", lang()));
        }

        String normalized = command.toLowerCase(Locale.ROOT);
        String language = request.getLanguage() != null ? request.getLanguage() : lang();
        boolean previewOnly = Boolean.TRUE.equals(request.getPreviewOnly());

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
                || normalized.contains("cheyyi")
                || previewOnly) {
            return handleAdd(command, language, previewOnly);
        }

        // Try parse as add preview even without explicit "add"
        ParsedItem parsed = parseItem(command);
        if (parsed != null) {
            return handleAdd(command, language, true);
        }

        return fail("UNKNOWN", localize("unknown", language) + ": " + command);
    }

    private VoiceCommandResponse handleAdd(String command, String language, boolean previewOnly) {
        ParsedItem parsed = parseItem(command);
        if (parsed == null) {
            return fail("ADD_STOCK", localize("need_qty_product", language));
        }

        ResolvedProduct resolved = resolveProductName(parsed.product);
        Double price = parsed.costPerUnit != null ? parsed.costPerUnit : 0.0;

        if (previewOnly) {
            String spoken = resolved.canonical + " — " + parsed.qty + " " + parsed.unit
                    + (price > 0 ? " @ ₹" + price : "");
            return VoiceCommandResponse.builder()
                    .action("PREVIEW_ADD")
                    .success(true)
                    .spokenResponse(spoken)
                    .productName(resolved.canonical)
                    .quantity(parsed.qty)
                    .unit(parsed.unit)
                    .costPerUnit(price)
                    .confidence(resolved.confidence)
                    .matchedFrom(resolved.matchedFrom)
                    .build();
        }

        InventoryResponse item = inventoryService.addStockByProductName(
                resolved.canonical, parsed.qty, parsed.unit, price);
        String spoken = formatAdded(resolved.canonical, parsed.qty, item.getUnit(), language);
        return VoiceCommandResponse.builder()
                .action("ADD_STOCK")
                .success(true)
                .spokenResponse(spoken)
                .productName(resolved.canonical)
                .quantity(parsed.qty)
                .unit(item.getUnit())
                .costPerUnit(item.getCostPerUnit())
                .confidence(resolved.confidence)
                .matchedFrom(resolved.matchedFrom)
                .data(item)
                .build();
    }

    private VoiceCommandResponse handleRemove(String command, String language) {
        ParsedItem parsed = parseItem(command);
        if (parsed == null) {
            return fail("REMOVE_STOCK", localize("need_qty_product", language));
        }
        ResolvedProduct resolved = resolveProductName(parsed.product);
        InventoryResponse item = inventoryService.removeStockByProductName(resolved.canonical, parsed.qty);
        String spoken = formatRemoved(resolved.canonical, parsed.qty, item.getUnit(), item.getQuantity(), language);
        return VoiceCommandResponse.builder()
                .action("REMOVE_STOCK")
                .success(true)
                .spokenResponse(spoken)
                .productName(resolved.canonical)
                .quantity(parsed.qty)
                .unit(item.getUnit())
                .data(item)
                .build();
    }

    private ResolvedProduct resolveProductName(String raw) {
        if (raw == null || raw.trim().length() < 2) {
            return new ResolvedProduct(raw != null ? raw.trim() : "", 0.0, raw);
        }
        String aliased = ProductAliases.resolve(raw.trim());
        Set<String> candidates = new HashSet<>(ProductAliases.knownNames());
        productRepository.findAll().forEach(p -> {
            if (p.getName() != null && p.getName().trim().length() >= 2) {
                candidates.add(p.getName().trim());
            }
        });
        try {
            inventoryService.listInventory().forEach(i -> {
                if (i.getProductName() != null && i.getProductName().length() >= 2) {
                    candidates.add(i.getProductName());
                }
            });
        } catch (Exception ignored) {
            // ignore
        }

        // Prefer exact / alias hit
        for (String c : candidates) {
            if (c.equalsIgnoreCase(aliased) || c.equalsIgnoreCase(raw.trim())) {
                return new ResolvedProduct(c, 1.0, raw);
            }
        }

        FuzzyMatcher.Match match = FuzzyMatcher.bestMatchDetailed(aliased, candidates, 0.78);
        if (match != null && match.value().length() >= 2) {
            // Title-case nicely if catalog uses Title Case
            return new ResolvedProduct(match.value(), match.score(), raw);
        }

        // Keep multi-word English name as typed (title case first letter)
        String canonical = aliased.substring(0, 1).toUpperCase(Locale.ROOT)
                + (aliased.length() > 1 ? aliased.substring(1) : "");
        return new ResolvedProduct(canonical, 0.9, raw);
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
        ResolvedProduct resolved = resolveProductName(product);
        List<Map<String, Object>> vendors = vendorService.searchNearby(resolved.canonical, radius, 1, null, null);
        String spoken = vendors.isEmpty()
                ? localize("no_vendors", language) + " " + resolved.canonical
                : localize("found_vendors", language) + " " + vendors.size() + " — " + resolved.canonical;
        return VoiceCommandResponse.builder()
                .action("FIND_VENDORS")
                .success(true)
                .spokenResponse(spoken)
                .vendors(vendors)
                .data(vendors)
                .productName(resolved.canonical)
                .confidence(resolved.confidence)
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

    private ParsedItem parseItem(String command) {
        if (command == null || command.isBlank()) {
            return null;
        }

        // Normalize smart dashes / quotes
        String raw = command
                .replace('\u2014', '-')
                .replace('\u2013', '-')
                .replaceAll("[\\u00A0\\s]+", " ")
                .trim();

        Double price = extractPrice(raw);
        // Remove price phrases so they do not pollute name/qty
        String work = raw.replaceAll(
                "(?i)(?:(?:rs\\.?|rupees?|inr|₹|price|cost|rate)\\s*[:=]?\\s*\\d+(?:\\.\\d+)?)|(?:\\d+(?:\\.\\d+)?\\s*(?:rs\\.?|rupees?|inr|₹))",
                " ");
        work = work.replaceAll("\\s+", " ").trim();

        // Tokenize
        String[] tokens = work.split("\\s+");
        if (tokens.length == 0) {
            return null;
        }

        Integer qty = null;
        String unit = null;
        java.util.List<String> nameTokens = new java.util.ArrayList<>();

        Set<String> units = Set.of(
                "kg", "kgs", "kilogram", "kilograms",
                "g", "gram", "grams",
                "l", "lt", "ltr", "litre", "litres", "liter", "liters", "ml",
                "packet", "packets", "pack", "packs",
                "bottle", "bottles",
                "piece", "pieces", "pcs", "pc", "unit", "units", "item", "items",
                "bag", "bags"
        );
        Set<String> stop = Set.of(
                "add", "remove", "stock", "cheyyi", "pettu", "theeyandi", "thiyyi",
                "please", "the", "a", "an", "of", "to", "my", "inventory",
                "జోడించు", "తీసివేయి", "जोड़ो", "हटाओ"
        );

        // Pattern A: <qty> <unit?> <name...> [add]
        // Pattern B: <name...> <qty> <unit?>
        boolean startsWithNumber = tokens[0].matches("\\d+");

        if (startsWithNumber) {
            qty = Integer.parseInt(tokens[0]);
            int i = 1;
            if (i < tokens.length && units.contains(tokens[i].toLowerCase(Locale.ROOT))) {
                unit = tokens[i];
                i++;
            }
            for (; i < tokens.length; i++) {
                String t = tokens[i];
                String lower = t.toLowerCase(Locale.ROOT);
                if (stop.contains(lower) || units.contains(lower)) {
                    continue;
                }
                if (t.matches("\\d+(?:\\.\\d+)?")) {
                    continue;
                }
                nameTokens.add(t);
            }
        } else {
            // name first, then qty
            for (int i = 0; i < tokens.length; i++) {
                String t = tokens[i];
                String lower = t.toLowerCase(Locale.ROOT);
                if (t.matches("\\d+") && qty == null) {
                    qty = Integer.parseInt(t);
                    if (i + 1 < tokens.length && units.contains(tokens[i + 1].toLowerCase(Locale.ROOT))) {
                        unit = tokens[i + 1];
                        i++;
                    }
                    continue;
                }
                if (units.contains(lower) || stop.contains(lower)) {
                    if (units.contains(lower) && unit == null) {
                        unit = t;
                    }
                    continue;
                }
                if (t.matches("\\d+(?:\\.\\d+)?")) {
                    continue;
                }
                nameTokens.add(t);
            }
        }

        if (qty == null || qty <= 0) {
            return null;
        }
        String product = String.join(" ", nameTokens).trim();
        product = product.replaceAll("(?i)[—\\-:]+", " ").replaceAll("\\s+", " ").trim();
        // Drop trailing junk words
        product = product.replaceAll("(?i)\\b(add|remove|stock|cheyyi)$", "").trim();

        if (product.length() < 2) {
            return null;
        }
        if (unit == null) {
            unit = "pieces";
        }
        return new ParsedItem(qty, product, InventoryService.normalizeUnit(unit), price);
    }

    private Double extractPrice(String command) {
        Matcher m = Pattern.compile(
                "(?i)(?:rs\\.?|rupees?|inr|₹|price|cost|rate)\\s*[:=]?\\s*(\\d+(?:\\.\\d+)?)")
                .matcher(command);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        m = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*(?:rs\\.?|rupees?|inr|₹)").matcher(command);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return null;
    }

    private String extractTrailingUnit(String text) {
        if (text == null) return null;
        Matcher m = UNIT_PATTERN.matcher(text);
        String last = null;
        while (m.find()) last = m.group(1);
        return last;
    }

    private String stripUnit(String raw) {
        return raw.replaceAll("(?i)\\b(kgs?|kilograms?|grams?|g|litres?|liters?|l|ml|packets?|packs?|bottles?|pieces?|pcs?|units?|items?|bags?)\\b", " ");
    }

    private String cleanProduct(String raw) {
        return raw.replaceAll("(?i)\\b(ni|lo|cheyyi|add|remove|stock|price|cost|rs|rupees)\\b", "")
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
                default -> "Please say a command.";
            };
            case "unknown" -> switch (lang) {
                case "te" -> "అర్థం కాలేదు";
                case "hi" -> "समझ नहीं आया";
                default -> "Sorry, I could not understand";
            };
            case "need_qty_product" ->
                    "Say quantity, unit, product and optional price, e.g. 10 kg rice add price 45";
            case "no_low" -> "All items are above threshold.";
            case "low_list" -> "Low stock items";
            case "need_vendor" -> "Say product and radius";
            case "no_vendors" -> "No vendors found for";
            case "found_vendors" -> "Found vendors:";
            default -> key;
        };
    }

    private String normalizeLang(String language) {
        if (language == null) return "en";
        String l = language.toLowerCase(Locale.ROOT);
        if (l.startsWith("te")) return "te";
        if (l.startsWith("hi")) return "hi";
        if (l.startsWith("ta")) return "ta";
        if (l.startsWith("kn")) return "kn";
        return "en";
    }

    private VoiceCommandResponse fail(String action, String message) {
        return VoiceCommandResponse.builder()
                .action(action)
                .success(false)
                .spokenResponse(message)
                .build();
    }

    private record ParsedItem(int qty, String product, String unit, Double costPerUnit) {}
    private record ResolvedProduct(String canonical, double confidence, String matchedFrom) {}
}
