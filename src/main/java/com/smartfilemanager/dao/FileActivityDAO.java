package com.smartfilemanager.dao;

import com.smartfilemanager.model.domain.FileActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;

import java.util.List;
import java.util.ArrayList;

@Repository
public class FileActivityDAO {
    @Autowired
    private DatabaseManager databaseManager;
    public void insertFileActivity(FileActivity fileActivity) throws SQLException {
        String sql = "INSERT OR REPLACE INTO file_activities (file_id, file_path, file_name, activity_type, folder_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fileActivity.getFileId());
            statement.setString(2, fileActivity.getFilePath());
            statement.setString(3, fileActivity.getFileName());
            statement.setString(4, fileActivity.getActivityType().toString());
            statement.setLong(5, Long.parseLong(String.valueOf(fileActivity.getFolderId())));
            statement.executeUpdate();
            System.out.println("插入文件活动记录成功");
        }catch (SQLException e){
            throw new RuntimeException("插入文件活动记录失败", e);
        }
    }

    public List<FileActivity> getRecentActivities(int limit) throws SQLException {
        String sql = "SELECT * FROM file_activities ORDER BY id DESC LIMIT ?";
        List<FileActivity> activities = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    FileActivity activity = new FileActivity();
                    activity.setId(rs.getInt("id"));
                    activity.setFileId(rs.getString("file_id"));
                    activity.setFilePath(rs.getString("file_path"));
                    activity.setFileName(rs.getString("file_name"));
                    activity.setActivityType(com.smartfilemanager.model.domain.ActivityType.valueOf(rs.getString("activity_type")));
                    activity.setFolderId(rs.getLong("folder_id"));
                    String createdAtStr = rs.getString("created_at");
                    if (createdAtStr != null && !createdAtStr.isEmpty()) {
                        try {
                            String cleanDate = createdAtStr.length() > 23 ? createdAtStr.substring(0, 23) : createdAtStr;
                            if (!cleanDate.contains("T") && cleanDate.contains(" ")) {
                                cleanDate = cleanDate.replace(" ", "T");
                            }
                            activity.setCreatedAt(java.time.LocalDateTime.parse(cleanDate));
                        } catch (Exception e) {
                            System.err.println("时间格式解析失败：" + createdAtStr);
                        }
                    }

                    activities.add(activity);
                }
            }
        }

        return activities;
    }

}
