package com.querySense.audit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;       // who made the request

    @Column(length = 2000)
    private String question;       // the English question

    @Column(length = 4000)
    private String generatedSql;   // the SQL the AI produced (may be null if blocked early)

    private String status;         // SUCCESS, BLOCKED, or ERROR

    private Integer rowCount;      // how many rows returned (null if not successful)

    @Column(length = 1000)
    private String detail;         // block reason / error message (null on success)

    private Instant createdAt;     // when it happened

    public AuditLog() {}           // JPA needs a no-args constructor

    public AuditLog(String username, String question, String generatedSql,
                    String status, Integer rowCount, String detail, Instant createdAt) {
        this.username = username;
        this.question = question;
        this.generatedSql = generatedSql;
        this.status = status;
        this.rowCount = rowCount;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getQuestion() { return question; }
    public String getGeneratedSql() { return generatedSql; }
    public String getStatus() { return status; }
    public Integer getRowCount() { return rowCount; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
}