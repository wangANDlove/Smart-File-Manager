package com.smartfilemanager.model.domain;

/**
 * 条件类型枚举
 */
public enum ConditionType {
    FILE_NAME("文件名"),
    FILE_EXTENSION("文件扩展名"),
    FILE_SIZE("文件大小"),
    FILE_DATE("文件日期"),
    FILE_PATH("文件路径");

    private final String description;

    ConditionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
