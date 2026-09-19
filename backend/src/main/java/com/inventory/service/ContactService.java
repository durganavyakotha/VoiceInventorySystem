package com.inventory.service;

import com.inventory.dto.ContactRequest;
import com.inventory.dto.QueryUpdateRequest;
import com.inventory.entity.ContactQuery;
import com.inventory.entity.User;
import com.inventory.enums.QueryStatus;
import com.inventory.repository.ContactQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactQueryRepository contactQueryRepository;
    private final UserService userService;

    @Transactional
    public Map<String, Object> submit(ContactRequest request) {
        User user = userService.getCurrentUser();
        ContactQuery query = contactQueryRepository.save(ContactQuery.builder()
                .user(user)
                .subject(request.getSubject().trim())
                .message(request.getMessage().trim())
                .status(QueryStatus.NEW)
                .build());
        return toMap(query);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAll() {
        return contactQueryRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> updateStatus(Long id, QueryUpdateRequest request) {
        ContactQuery query = contactQueryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Query not found"));
        query.setStatus(request.getStatus());
        return toMap(contactQueryRepository.save(query));
    }

    private Map<String, Object> toMap(ContactQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", query.getId());
        map.put("userId", query.getUser().getId());
        map.put("userEmail", query.getUser().getEmail());
        map.put("userName", query.getUser().getFirstName() + " " + query.getUser().getLastName());
        map.put("subject", query.getSubject());
        map.put("message", query.getMessage());
        map.put("status", query.getStatus());
        map.put("createdAt", query.getCreatedAt());
        map.put("updatedAt", query.getUpdatedAt());
        return map;
    }
}
