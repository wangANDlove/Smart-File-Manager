package com.smartfilemanager.dao;

import com.smartfilemanager.model.domain.FileRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Repository
public class FileRecordDAO {
    // 创建数据库连接
    @Autowired
    private DatabaseManager databaseManager;

    public FileRecordDAO() {

    }
    //插入文件记录
    public void insertFileRecord(FileRecord fileRecord) throws SQLException {
        String sql = "INSERT OR REPLACE INTO file_records (file_path, file_name, is_folder,file_id,folder_id) VALUES (?, ?, ?, ?,?)";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, fileRecord.getFilePath());
            statement.setString(2, fileRecord.getFileName());
            statement.setBoolean(3, fileRecord.getIsFolder());
            statement.setString(4, fileRecord.getFileId());
            if (fileRecord.getFolderId() != null) {
                statement.setLong(5, fileRecord.getFolderId());
            } else {
                throw new RuntimeException("folder_id 不能为空，每个文件记录必须关联一个监控文件夹");
            }
            statement.executeUpdate();
            //手动提交到数据库
            //connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException("插入文件记录失败", e);
        }
    }

    public void deleteFileRecord(FileRecord fileRecord) {
        String sql = "DELETE FROM file_records WHERE file_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fileRecord.getFileId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("删除文件记录失败",ex);
        }
    }
}
