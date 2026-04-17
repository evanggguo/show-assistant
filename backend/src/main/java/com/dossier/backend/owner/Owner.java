package com.dossier.backend.owner;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * TDD 5.1 — Owner 实体
 * 展示者/产品拥有者的核心信息，每个 Dossier 实例对应一个 Owner
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "owners")
public class Owner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "tagline", length = 255)
    private String tagline;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contact", columnDefinition = "jsonb")
    private Map<String, Object> contact;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

    /** 登录用户名（仅英文/数字，唯一） */
    @Column(name = "username", unique = true, length = 50)
    private String username;

    /** BCrypt 密码哈希 */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
