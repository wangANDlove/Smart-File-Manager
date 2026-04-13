package com.smartfilemanager.model.domain;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 文件标签关联类
 */
@Entity
@Data
@Table(name = "file_tags")
public class FileTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "file_id", nullable = false)
    private String fileId;

    @Column(name = "tag_id", nullable = false)
    private Integer tagId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
