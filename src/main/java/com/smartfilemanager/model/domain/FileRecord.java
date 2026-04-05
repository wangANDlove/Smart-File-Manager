package com.smartfilemanager.model.domain;

import lombok.Data;


import jakarta.persistence.*;

@Entity
@Data
public class FileRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "file_id")
    private String fileId;//windows系统中唯一标识的id
    @Column(name = "file_path")
    private String filePath;
    @Column(name = "file_name")
    private String fileName;
    @Column(name = "is_folder")
    private Boolean isFolder;//文件夹和文件都采用这一实体
    @Column(name = "folder_id")
    private Long folderId;//关联监控文件夹ID
    public FileRecord() {
    }
    public FileRecord(String filePath, String fileName, Boolean isFolder,String fileId) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.isFolder = isFolder;
        this.fileId = fileId;
    }
    public FileRecord(String filePath, String fileName, Boolean isFolder, String fileId, Long folderId) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.isFolder = isFolder;
        this.fileId = fileId;
        this.folderId = folderId;
    }
}
