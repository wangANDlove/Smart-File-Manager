package com.smartfilemanager.dao;

import com.smartfilemanager.model.domain.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TagDAO {

    @Autowired
    private DatabaseManager databaseManager;

    /**
     * 创建新标签
     */
    public Tag createTag(String name, String color) throws SQLException {
        String sql = "INSERT INTO tags (name, color) VALUES (?, ?)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            stmt.setString(2, color != null ? color : "#4A90E2");
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                Tag tag = new Tag();
                tag.setId(rs.getInt(1));
                tag.setName(name);
                tag.setColor(color != null ? color : "#4A90E2");
                return tag;
            }
            throw new SQLException("创建标签失败");
        }
    }

    /**
     * 为文件添加标签
     */
    public void addTagToFile(String fileId, Integer tagId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO file_tags (file_id, tag_id) VALUES (?, ?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fileId);
            stmt.setInt(2, tagId);
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                incrementTagUsageCount(tagId);
            }
        }
    }
    private void incrementTagUsageCount(Integer tagId) throws SQLException {
        String sql = "UPDATE tags SET usage_count = usage_count + 1 WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, tagId);
            stmt.executeUpdate();
        }
    }
    private void decrementTagUsageCount(Integer tagId) throws SQLException {
        String sql = "UPDATE tags SET usage_count = usage_count - 1 WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, tagId);
            stmt.executeUpdate();
        }
    }
    /**
     * 移除文件的标签
     */
    public void removeTagFromFile(String fileId, Integer tagId) throws SQLException {
        String sql = "DELETE FROM file_tags WHERE file_id = ? AND tag_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fileId);
            stmt.setInt(2, tagId);
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                decrementTagUsageCount(tagId);

                if (getTagUsageCount(tagId) == 0) {
                    deleteTag(tagId);
                }
            }
        }
    }
    private int getTagUsageCount(Integer tagId) throws SQLException {
        String sql = "SELECT usage_count FROM tags WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, tagId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("usage_count");
                }
            }
        }
        return 0;
    }



    public List<Tag> getTagsByFileId(String fileId) throws SQLException {
        String sql = "SELECT t.* FROM tags t INNER JOIN file_tags ft ON t.id = ft.tag_id WHERE ft.file_id = ? ORDER BY t.name";
        List<Tag> tags = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Tag tag = new Tag();
                    tag.setId(rs.getInt("id"));
                    tag.setName(rs.getString("name"));
                    tag.setColor(rs.getString("color"));
                    tag.setUsageCount(rs.getInt("usage_count"));
                    tag.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
                    tags.add(tag);
                }
            }
        }
        return tags;
    }






    /**
     * 根据标签搜索文件
     */
    public List<String> getFileIdsByTag(Integer tagId) throws SQLException {
        String sql = "SELECT file_id FROM file_tags WHERE tag_id = ?";

        List<String> fileIds = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, tagId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                fileIds.add(rs.getString("file_id"));
            }
        }
        return fileIds;
    }

    /**
     * 根据多个标签搜索文件（AND逻辑）
     */
    public List<String> getFileIdsByTags(List<Integer> tagIds) throws SQLException {
        if (tagIds == null || tagIds.isEmpty()) {
            return new ArrayList<>();
        }

        StringBuilder sql = new StringBuilder(
                "SELECT file_id FROM file_tags WHERE tag_id IN ("
        );
        for (int i = 0; i < tagIds.size(); i++) {
            sql.append("?");
            if (i < tagIds.size() - 1) sql.append(", ");
        }
        sql.append(") GROUP BY file_id HAVING COUNT(DISTINCT tag_id) = ?");

        List<String> fileIds = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < tagIds.size(); i++) {
                stmt.setInt(i + 1, tagIds.get(i));
            }
            stmt.setInt(tagIds.size() + 1, tagIds.size());

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                fileIds.add(rs.getString("file_id"));
            }
        }
        return fileIds;
    }

    /**
     * 获取热门标签（按使用频率排序）
     */
    public List<Map<String, Object>> getPopularTags(int limit) throws SQLException {
        String sql = "SELECT t.id, t.name, t.color, COUNT(ft.file_id) as usage_count " +
                "FROM tags t " +
                "LEFT JOIN file_tags ft ON t.id = ft.tag_id " +
                "GROUP BY t.id " +
                "ORDER BY usage_count DESC " +
                "LIMIT ?";

        List<Map<String, Object>> popularTags = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> tagInfo = new HashMap<>();
                tagInfo.put("id", rs.getInt("id"));
                tagInfo.put("name", rs.getString("name"));
                tagInfo.put("color", rs.getString("color"));
                tagInfo.put("usageCount", rs.getInt("usage_count"));
                popularTags.add(tagInfo);
            }
        }
        return popularTags;
    }

    /**
     * 获取所有标签
     */
    public List<Tag> getAllTags() throws SQLException {
        String sql = "SELECT * FROM tags ORDER BY name";

        List<Tag> tags = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Tag tag = new Tag();
                tag.setId(rs.getInt("id"));
                tag.setName(rs.getString("name"));
                tag.setColor(rs.getString("color"));
                tags.add(tag);
            }
        }
        return tags;
    }

    /**
     * 删除标签
     */
    public void deleteTag(Integer tagId) throws SQLException {
        String sql = "DELETE FROM tags WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, tagId);
            stmt.executeUpdate();
        }
    }

    /**
     * 更新标签
     */
    public void updateTag(Tag tag) throws SQLException {
        String sql = "UPDATE tags SET name = ?, color = ? WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tag.getName());
            stmt.setString(2, tag.getColor());
            stmt.setInt(3, tag.getId());
            stmt.executeUpdate();
        }
    }

    public Tag getTagByName(String tagName) throws SQLException {
        String sql = "SELECT * FROM tags WHERE name = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tagName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Tag tag = new Tag();
                tag.setId(rs.getInt("id"));
                tag.setName(rs.getString("name"));
                tag.setColor(rs.getString("color"));
                tag.setUsageCount(rs.getInt("usage_count"));
                tag.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
                return tag;
            }
        }
        return null;
    }

    public Tag getTagById(Integer tagId) throws SQLException {
        String sql = "SELECT * FROM tags WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, tagId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Tag tag = new Tag();
                tag.setId(rs.getInt("id"));
                tag.setName(rs.getString("name"));
                tag.setColor(rs.getString("color"));
                tag.setUsageCount(rs.getInt("usage_count"));
                tag.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
                return tag;
            }
        }
        return null;
    }

    public void createTag(Tag tag) throws SQLException {
        String sql = "INSERT INTO tags (name, color, usage_count) VALUES (?, ?, ?)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, tag.getName());
            stmt.setString(2, tag.getColor() != null ? tag.getColor() : "#4A90E2");
            stmt.setInt(3, tag.getUsageCount() != null ? tag.getUsageCount() : 0);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                tag.setId(rs.getInt(1));
            }
        }
    }


}
