package com.smartfilemanager.dao;

import com.smartfilemanager.model.domain.MonitorFolders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MonitorFoldersDAO {
    @Autowired
    private DatabaseManager databaseManager;

    public void insertMonitorFolder(MonitorFolders monitorFolder) throws SQLException {
        String sql = "INSERT INTO monitor_folders (folder_path, created_at) VALUES (?, ?)";

        // 使用 try-with-resources 自动关闭 PreparedStatement
        try (PreparedStatement stmt = databaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, monitorFolder.getFolderPath());

            // 注意：SQLite 通常接受 LocalDateTime 或 ISO-8601 字符串
            if (monitorFolder.getCreatedAt() != null) {
                stmt.setString(2, monitorFolder.getCreatedAt().toString());
            } else {
                // 如果实体中时间为空，可以使用数据库默认值 CURRENT_TIMESTAMP，这里设为 null 让 SQLite 处理默认值
                // 或者显式传入当前时间字符串
                stmt.setString(2, LocalDateTime.now().toString());
            }

            int rowsAffected = stmt.executeUpdate();
            System.out.println("测试点二：成功插入 " + rowsAffected + " 行记录");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public List<MonitorFolders> getAllMonitorFolders() throws SQLException {
        String sql = "SELECT * FROM monitor_folders";
        List<MonitorFolders> monitorFolders;
        // 使用 try-with-resources 自动关闭 PreparedStatement
        try (PreparedStatement stmt = databaseManager.getConnection().prepareStatement(sql)) {
            // 使用 try-with-resources 自动关闭 ResultSet
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                monitorFolders = new ArrayList<>();
                while (rs.next()) {
                    MonitorFolders monitorFolder = new MonitorFolders();
                    monitorFolder.setId(rs.getLong("id"));
                    monitorFolder.setFolderPath(rs.getString("folder_path"));
                    //monitorFolder.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    String createdAtStr = rs.getString("created_at");
                    if (createdAtStr != null && !createdAtStr.isEmpty()) {
                        try {
                            // 清理格式：SQLite 可能返回带纳秒的 ISO 格式，截断到毫秒位 (23位) 以兼容 LocalDateTime.parse
                            // 格式示例：2026-03-14T20:16:39.007954300 -> 2026-03-14T20:16:39.007
                            String cleanDate = createdAtStr.length() > 23 ? createdAtStr.substring(0, 23) : createdAtStr;

                            // 如果数据库存的是空格分隔符，替换为 T 以便解析
                            if (!cleanDate.contains("T") && cleanDate.contains(" ")) {
                                cleanDate = cleanDate.replace(" ", "T");
                            }

                            monitorFolder.setCreatedAt(java.time.LocalDateTime.parse(cleanDate));
                        } catch (Exception e) {
                            System.err.println("时间格式解析失败：" + createdAtStr + "，使用当前时间代替。错误：" + e.getMessage());
                            monitorFolder.setCreatedAt(java.time.LocalDateTime.now());
                        }
                    }
                    monitorFolders.add(monitorFolder);
                }
            }
            return monitorFolders;
        }
    }

    /*
     *
     *
     * @param null
     * @return
     * @author wjd
     * @create 2026/3/17
     * @description  根据文件路径删除数据库中监控文件夹表中条目
     **/
    public int deleteMonitorFolderByPath(String selectedFolder) {
        String sql="DELETE FROM monitor_folders WHERE folder_path= ?";
        try (PreparedStatement stmt = databaseManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, selectedFolder);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("删除监控文件夹：" + selectedFolder + "，影响行数：" + rowsAffected);
            return rowsAffected;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
