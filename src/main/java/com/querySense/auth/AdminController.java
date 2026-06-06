package com.querySense.auth;

import com.querySense.audit.AuditLog;
import com.querySense.audit.AuditRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditRepository auditRepository;

    public AdminController(UserRepository userRepository,
                           AuditRepository auditRepository) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> listUsers() {
        return userRepository.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "username", u.getUsername(),
                        "role", u.getRole()))
                .toList();
    }

    @GetMapping("/audit")
    public List<AuditLog> recentAudit() {
        return auditRepository.findTop50ByOrderByCreatedAtDesc();
    }
}