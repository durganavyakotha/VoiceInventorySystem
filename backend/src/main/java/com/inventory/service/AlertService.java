package com.inventory.service;

import com.inventory.enums.AlertType;
import com.inventory.entity.Alert;
import com.inventory.entity.User;
import com.inventory.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserService userService;

    @Transactional
    public Alert createAlert(User user, AlertType type, String message) {
        return alertRepository.save(Alert.builder()
                .user(user)
                .type(type)
                .message(message)
                .isRead(false)
                .build());
    }

    public List<Map<String, Object>> listAlerts() {
        User user = userService.getCurrentUser();
        return alertRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> markRead(Long alertId) {
        User user = userService.getCurrentUser();
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        if (!alert.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        alert.setIsRead(true);
        return toMap(alertRepository.save(alert));
    }

    @Transactional
    public void markAllRead() {
        User user = userService.getCurrentUser();
        List<Alert> alerts = alertRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId());
        alerts.forEach(a -> a.setIsRead(true));
        alertRepository.saveAll(alerts);
    }

    private Map<String, Object> toMap(Alert alert) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", alert.getId());
        map.put("type", alert.getType());
        map.put("message", alert.getMessage());
        map.put("isRead", alert.getIsRead());
        map.put("createdAt", alert.getCreatedAt());
        return map;
    }
}
