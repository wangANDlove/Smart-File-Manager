package com.smartfilemanager.model.domain;

import lombok.Data;
import java.util.List;

/**
 * AI分类结果
 */
@Data
public class ClassificationResult {
    private List<CategoryInfo> categories;
    private String summary;
    private List<String> recommendations;

    @Data
    public static class CategoryInfo {
        private String name;           // 分类名称
        private String description;    // 分类说明
        private List<String> extensions;    // 文件扩展名列表
        private List<String> filePatterns;  // 文件名匹配模式
        private Integer estimatedCount;     // 预估文件数量
    }
}

