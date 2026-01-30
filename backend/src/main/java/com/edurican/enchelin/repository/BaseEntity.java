package com.edurican.enchelin.repository;

import com.edurican.enchelin.enums.EntityStatus;
import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private EntityStatus status = EntityStatus.ACTIVE;

    @CreationTimestamp
    @Column
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void active() {
        status = EntityStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == EntityStatus.ACTIVE;
    }

    public void deleted() {
        status = EntityStatus.DELETED;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

}
