package com.querySense.audit;

import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class AuditService {

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void record(String username, String question, String sql,
                       String status, Integer rowCount, String detail) {
        try {
            auditRepository.save(new AuditLog(
                    username, question, sql, status, rowCount, detail, Instant.now()));
        } catch (Exception e) {
            // auditing must never break the actual request
        }
    }
}