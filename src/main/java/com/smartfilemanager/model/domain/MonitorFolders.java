package com.smartfilemanager.model.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应数据库表 file_records 的实体类
 */
@Data
@Entity
public class MonitorFolders {

    // 对应 id INTEGER PRIMARY KEY AUTOINCREMENT
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folder_path", nullable = false, unique = true)
    // 对应 folder_path TEXT
    private String folderPath;
    // 对应 created_at DATETIME
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public MonitorFolders() {
    }

    public MonitorFolders(String folderPath) {
        this.folderPath = folderPath;
        this.createdAt = LocalDateTime.now();
    }


}

