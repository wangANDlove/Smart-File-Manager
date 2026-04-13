package com.smartfilemanager.model.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String color;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Tag() {
        this.usageCount = 0;
    }

    public Tag(String name, String color) {
        this.name = name;
        this.color = color;
        this.usageCount = 0;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public void incrementUsageCount() {
        this.usageCount++;
    }

    public void decrementUsageCount() {
        if (this.usageCount > 0) {
            this.usageCount--;
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.usageCount == null) {
            this.usageCount = 0;
        }
    }
}
