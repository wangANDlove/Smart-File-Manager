package com.smartfilemanager.service.rule;

import com.smartfilemanager.dao.TagDAO;
import com.smartfilemanager.model.domain.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TagService {

    @Autowired
    private TagDAO tagDAO;

    /**
     * 为文件添加标签
     */
    public void addTagToFile(String fileId, String tagName) {
        try {
            Tag tag = tagDAO.getTagByName(tagName);

            if (tag == null) {
                tag = new Tag(tagName, generateRandomColor());
                tagDAO.createTag(tag);
                tag = tagDAO.getTagByName(tagName);
            }

            tagDAO.addTagToFile(fileId, tag.getId());

            System.out.println("标签 '" + tagName + "' 已添加到文件，当前使用次数: " + (tag.getUsageCount() + 1));

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("添加标签失败", e);
        }
    }

    /**
     * 批量为文件添加标签
     */
    public void addTagsToFile(String fileId, List<String> tagNames) throws SQLException {
        for (String tagName : tagNames) {
            addTagToFile(fileId, tagName);
        }
    }

    /**
     * 移除文件的标签
     */
    public void removeTagFromFile(String fileId, Integer tagId) throws SQLException {
        try {
            Tag tag = tagDAO.getTagById(tagId);
            if (tag != null) {
                System.out.println("标签 '" + tag.getName() + "' 当前使用次数: " + tag.getUsageCount());
            }

            tagDAO.removeTagFromFile(fileId, tagId);

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("删除标签失败", e);
        }
    }

    /**
     * 获取文件的标签列表
     */
    public List<Tag> getFileTags(String fileId) throws SQLException {
        return tagDAO.getTagsByFileId(fileId);
    }

    /**
     * 根据标签搜索文件ID
     */
    public List<String> searchFilesByTags(List<Integer> tagIds) throws SQLException {
        return tagDAO.getFileIdsByTags(tagIds);
    }

    /**
     * 获取热门标签
     */
    public List<Map<String, Object>> getPopularTags(int limit) throws SQLException {
        try {
            List<Map<String, Object>> popularTags = tagDAO.getPopularTags(limit);

            for (Map<String, Object> tagInfo : popularTags) {
                int usageCount = (Integer) tagInfo.get("usageCount");
                if (usageCount == 0) {
                    Integer tagId = (Integer) tagInfo.get("id");
                    tagDAO.deleteTag(tagId);
                    System.out.println("热门标签中发现使用次数为0的标签，已删除: " + tagInfo.get("name"));
                }
            }

            return tagDAO.getPopularTags(limit);

        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 获取所有标签
     */
    public List<Tag> getAllTags() throws SQLException {
        return tagDAO.getAllTags();
    }

    /**
     * 查找或创建标签
     */
    private Tag findOrCreateTag(String tagName) throws SQLException {
        List<Tag> allTags = tagDAO.getAllTags();
        Tag existingTag = allTags.stream()
                .filter(t -> t.getName().equalsIgnoreCase(tagName))
                .findFirst()
                .orElse(null);

        if (existingTag != null) {
            return existingTag;
        }

        String[] colors = {"#4A90E2", "#7ED321", "#F5A623", "#D0021B", "#BD10E0", "#9013FE"};
        String randomColor = colors[(int)(Math.random() * colors.length)];
        return tagDAO.createTag(tagName, randomColor);
    }
    /**
     * 生成随机颜色
     */
    private String generateRandomColor() {
        String[] colors = {"#4A90E2", "#7ED321", "#F5A623", "#D0021B", "#BD10E0", "#9013FE", "#50E3C2", "#B8E986"};
        return colors[(int)(Math.random() * colors.length)];
    }

}

