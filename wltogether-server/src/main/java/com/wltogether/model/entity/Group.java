package com.wltogether.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "groups_table")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(length = 500)
    private String description;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(length = 500)
    private String announcement;

    @Column(name = "join_mode", nullable = false, length = 20)
    @Builder.Default
    private String joinMode = "INVITE_ONLY";

    @Column(length = 500)
    private String tags;

    @Column(name = "storage_used_bytes", nullable = false)
    @Builder.Default
    private Long storageUsedBytes = 0L;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
