package com.inventory.service;

import com.inventory.entity.User;
import com.inventory.enums.Role;
import com.inventory.enums.UserStatus;
import com.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    public static final String BOT_EMAIL = "assistant@voicestock.bot";

    private final UserRepository userRepository;
    private final ChatService chatService;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final AlertService alertService;

    @Transactional
    public User ensureBotUser() {
        return userRepository.findByEmail(BOT_EMAIL).orElseGet(() ->
                userRepository.save(User.builder()
                        .firstName("VoiceStock")
                        .lastName("Assistant")
                        .email(BOT_EMAIL)
                        .passwordHash(passwordEncoder.encode("Bot@NeverLogin1"))
                        .role(Role.ADMIN)
                        .location("Hyderabad")
                        .language("en")
                        .phone("1800-VOICE")
                        .status(UserStatus.ACTIVE)
                        .build()));
    }

    @Transactional
    public Map<String, Object> openBotChat() {
        User bot = ensureBotUser();
        Map<String, Object> conversation = chatService.getOrCreateConversation(bot.getId());
        return conversation;
    }

    @Transactional
    public Map<String, Object> ask(String message) {
        User bot = ensureBotUser();
        User current = userService.getCurrentUser();
        Map<String, Object> conversation = chatService.getOrCreateConversation(bot.getId());

        // Save user message via chat service
        com.inventory.dto.ChatMessageRequest userMsg = new com.inventory.dto.ChatMessageRequest();
        userMsg.setConversationId(((Number) conversation.get("id")).longValue());
        userMsg.setReceiverId(bot.getId());
        userMsg.setMessageText(message);
        userMsg.setSourceLanguage(current.getLanguage());
        chatService.sendText(userMsg);

        String reply = generateReply(message, current);

        // Save bot reply as SYSTEM-like text from bot
        // Temporarily authenticate as bot is hard; save message directly through chat with swapped roles
        com.inventory.dto.ChatMessageRequest botMsg = new com.inventory.dto.ChatMessageRequest();
        botMsg.setConversationId(((Number) conversation.get("id")).longValue());
        botMsg.setReceiverId(current.getId());
        botMsg.setMessageText(reply);
        botMsg.setSourceLanguage("en");
        // sendText uses current user as sender — need sendAsBot
        Map<String, Object> saved = chatService.sendAsUser(bot, current, botMsg);

        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversation.get("id"));
        result.put("reply", reply);
        result.put("message", saved);
        return result;
    }

    private String generateReply(String message, User user) {
        String m = message == null ? "" : message.toLowerCase(Locale.ROOT);
        try {
            if (m.contains("low stock") || m.contains("alert")) {
                var low = inventoryService.listLowStock();
                if (low.isEmpty()) {
                    return "You have no low-stock items right now. Nice work!";
                }
                String names = low.stream()
                        .map(i -> i.getProductName() + " (" + i.getQuantity() + " " + i.getUnit() + ")")
                        .reduce((a, b) -> a + ", " + b).orElse("");
                return "Low stock items: " + names + ". Tip: open Vendors to book restock.";
            }
            if (m.contains("order") || m.contains("book")) {
                var orders = orderService.listOrders();
                long pending = orders.stream().filter(o -> "PENDING".equals(String.valueOf(o.get("status")))).count();
                return "You have " + orders.size() + " order(s), " + pending
                        + " pending. Say “book 10 rice” on a vendor’s items to place an order, "
                        + "or “withdraw rice” to cancel and restore stock.";
            }
            if (m.contains("vendor") || m.contains("supplier")) {
                return "Go to Vendors to see all suppliers. Open Available Items, then Book or use voice: "
                        + "“book 10 kg rice”. Cost per unit is shown on each item.";
            }
            if (m.contains("hello") || m.contains("hi") || m.contains("help")) {
                return "Hi " + user.getFirstName() + "! I’m the VoiceStock Assistant. Ask me about "
                        + "low stock, orders, vendors, or how to add items with voice and photos.";
            }
            if (m.contains("add") || m.contains("inventory") || m.contains("photo")) {
                return "To add items: open Add Items, capture/upload a product photo (required), "
                        + "set name, quantity, unit, category, and cost per unit. "
                        + "Voice example: “10 kg rice add cheyyi”.";
            }
            if (m.contains("language") || m.contains("telugu") || m.contains("hindi")) {
                return "Set your preferred language in Profile. The whole app UI updates to English, Telugu, Hindi, Tamil, or Kannada.";
            }
            var alerts = alertService.listAlerts();
            long unread = alerts.stream()
                    .filter(a -> Boolean.FALSE.equals(a.get("isRead")) || Boolean.FALSE.equals(a.get("read")))
                    .count();
            return "I’m not sure about that. Try asking about low stock, orders, or vendors. "
                    + "You currently have " + unread + " unread alert(s).";
        } catch (Exception e) {
            return "Hi! Ask me about inventory, orders, vendors, or low stock.";
        }
    }
}
