package com.smartfilemanager.model.domain;

public enum ActionType {
    MOVE("移动"),
    COPY("复制"),
    RENAME("重命名"),
    TAG("打标签"),
    DELETE("删除"),
    COMPRESS("压缩");

    private final String description;

    ActionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
