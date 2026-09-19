package com.inventory.service;

import com.inventory.dto.InventoryResponse;
import com.inventory.dto.VoiceCommandRequest;
import com.inventory.dto.VoiceCommandResponse;
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

    private static final Pattern QTY_PRODUCT = Pattern.compile(
            "(?i)(\\d+)\\s+([a-zA-Z\\u0C00-\\u0C7F][\\w\\s\\u0C00-\\u0C7F]*?)\\s+(?:bottles?|packs?|items?|units?)?\\s*" +
                    "(?:add|remove|stock|cheyyi|cheyandi|pettu|theeyandi|thiyyi)?");

    private static final Pattern ADD_PATTERN = Pattern.compile(
            "(?i).*(?:add|stock\\s*add|cheyyi|pettu|add\\s*cheyyi|stock\\s*cheyyi).*");

    private static final Pattern REMOVE_PATTERN = Pattern.compile(
            "(?i).*(?:remove|delete|theeyandi|thiyyi|remove\\s*cheyyi).*");

    private static final Pattern LOW_STOCK_PATTERN = Pattern.compile(
            "(?i).*(?:low\\s*stock|thakkuva|takkuva|show\\s*my\\s*low|low\\s*items).*");

    private static final Pattern VENDOR_PATTERN = Pattern.compile(
            "(?i)(.+?)\\s*(?:vendors?|supplier|sambhandam|ni)?\\s*(?:(\\d+(?:\\.\\d+)?)\\s*(?:km|kilometers?|kms?))?\\s*" +
                    "(?:lo\\s*)?(?:chupinchu|chupinchi|show|find|search).*|" +
                    "(?i).*(?:find|show|search)\\s+(.+?)\\s+vendors?.*?(?:(\\d+(?:\\.\\d+)?)\\s*(?:km|kilometers?))?.*");

    public VoiceCommandResponse process(VoiceCommandRequest request) {
        String command = request.getCommand() != null ? request.getCommand().trim() : "";
        if (command.isBlank()) {
            return fail("UNKNOWN", "Please say a command.");
        }

        String normalized = command.toLowerCase(Locale.ROOT);

        if (LOW_STOCK_PATTERN.matcher(normalized).matches()
                || normalized.contains("low stock")
                || normalized.contains("thakkuva stock")) {
            return handleLowStock();
        }

        if (isVendorCommand(normalized, command)) {
            return handleFindVendors(command, normalized);
        }

        if (REMOVE_PATTERN.matcher(normalized).matches()
                || normalized.contains("remove")
                || normalized.contains("thiyyi")
                || normalized.contains("theeyandi")) {
            return handleRemove(command);
        }

        if (ADD_PATTERN.matcher(normalized).matches()
                || normalized.contains("add")
                || normalized.contains("pettu")
                || normalized.contains("cheyyi")) {
            return handleAdd(command);
        }

        return fail("UNKNOWN", "Sorry, I could not understand: " + command);
    }

    private VoiceCommandResponse handleAdd(String command) {
        ParsedQtyProduct parsed = parseQtyProduct(command);
        if (parsed == null) {
            return fail("ADD_STOCK", "Please say quantity and product, e.g. 10 Pepsi bottles add cheyyi");
        }
        InventoryResponse item = inventoryService.addStockByProductName(parsed.product, parsed.qty);
        return VoiceCommandResponse.builder()
                .action("ADD_STOCK")
                .success(true)
                .spokenResponse("Added " + parsed.qty + " " + parsed.product + ". Current stock: " + item.getQuantity())
                .data(item)
                .build();
    }

    private VoiceCommandResponse handleRemove(String command) {
        ParsedQtyProduct parsed = parseQtyProduct(command);
        if (parsed == null) {
            return fail("REMOVE_STOCK", "Please say quantity and product, e.g. 5 soaps remove cheyyi");
        }
        InventoryResponse item = inventoryService.removeStockByProductName(parsed.product, parsed.qty);
        return VoiceCommandResponse.builder()
                .action("REMOVE_STOCK")
                .success(true)
                .spokenResponse("Removed " + parsed.qty + " " + parsed.product + ". Remaining: " + item.getQuantity())
                .data(item)
                .build();
    }

    private VoiceCommandResponse handleLowStock() {
        List<InventoryResponse> low = inventoryService.listLowStock();
        String spoken;
        if (low.isEmpty()) {
            spoken = "All items are above threshold. No low stock items.";
        } else {
            String names = low.stream()
                    .map(i -> i.getProductName() + " (" + i.getQuantity() + ")")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            spoken = "Low stock items: " + names;
        }
        return VoiceCommandResponse.builder()
                .action("LIST_LOW_STOCK")
                .success(true)
                .spokenResponse(spoken)
                .data(low)
                .build();
    }

    private VoiceCommandResponse handleFindVendors(String command, String normalized) {
        String product = extractVendorProduct(command, normalized);
        double radius = extractRadius(normalized);

        if (product == null || product.isBlank()) {
            return fail("FIND_VENDORS", "Please say product and radius, e.g. Rice vendors ni 5 kilometers lo chupinchu");
        }

        List<Map<String, Object>> vendors = vendorService.searchNearby(product, radius, 1, null, null);
        String spoken;
        if (vendors.isEmpty()) {
            spoken = "No vendors found for " + product + " within " + radius + " kilometers.";
        } else {
            spoken = "Found " + vendors.size() + " vendor(s) for " + product + " within " + radius + " km.";
        }
        return VoiceCommandResponse.builder()
                .action("FIND_VENDORS")
                .success(true)
                .spokenResponse(spoken)
                .vendors(vendors)
                .data(vendors)
                .build();
    }

    private boolean isVendorCommand(String normalized, String command) {
        return normalized.contains("vendor")
                || normalized.contains("chupinchu")
                || normalized.contains("supplier")
                || VENDOR_PATTERN.matcher(command).matches();
    }

    private String extractVendorProduct(String command, String normalized) {
        Matcher m = Pattern.compile("(?i)([a-zA-Z\\u0C00-\\u0C7F][\\w\\u0C00-\\u0C7F]*)\\s+vendors?").matcher(command);
        if (m.find()) {
            return cleanProduct(m.group(1));
        }
        m = Pattern.compile("(?i)(?:find|show|search)\\s+([a-zA-Z\\u0C00-\\u0C7F][\\w\\s\\u0C00-\\u0C7F]*?)\\s+vendors?").matcher(command);
        if (m.find()) {
            return cleanProduct(m.group(1));
        }
        // "Rice vendors ni 5 kilometers lo chupinchu"
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
        Matcher m = Pattern.compile("(?i)(\\d+)\\s+(.+?)\\s+(?:add|remove|cheyyi|pettu|theeyandi|thiyyi|stock).*").matcher(command);
        if (m.find()) {
            return new ParsedQtyProduct(Integer.parseInt(m.group(1)), cleanProduct(m.group(2)));
        }
        m = QTY_PRODUCT.matcher(command);
        if (m.find()) {
            return new ParsedQtyProduct(Integer.parseInt(m.group(1)), cleanProduct(m.group(2)));
        }
        m = Pattern.compile("(?i)(\\d+)\\s+([a-zA-Z][\\w\\s]*?)(?:\\s+|$)").matcher(command);
        if (m.find()) {
            return new ParsedQtyProduct(Integer.parseInt(m.group(1)), cleanProduct(m.group(2)));
        }
        return null;
    }

    private String cleanProduct(String raw) {
        return raw.replaceAll("(?i)\\b(bottles?|packs?|items?|units?|ni|lo|cheyyi|add|remove)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private VoiceCommandResponse fail(String action, String message) {
        return VoiceCommandResponse.builder()
                .action(action)
                .success(false)
                .spokenResponse(message)
                .build();
    }

    private record ParsedQtyProduct(int qty, String product) {}
}
