package com.smartfilemanager.model.domain;


import lombok.Data;
import jakarta.persistence.*;

@Data
@Entity
public class FileActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;//主键id
    @Column(name = "file_id")
    private String fileId;
    @Column(name = "file_path")
    private String filePath;
    @Column(name = "file_name")
    private String fileName;
    @Column(name = "activity_type")
    private ActivityType activityType;
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    @Column(name = "folder_id")
    private Long folderId;//父目录id
    public FileActivity() {

    }
    public FileActivity(String fileId, String filePath, String fileName, ActivityType activityType,  Long folderId) {
        this.fileId = fileId;
        this.filePath = filePath;
        this.fileName = fileName;
        this.activityType = activityType;
        this.folderId = folderId;
    }
}
