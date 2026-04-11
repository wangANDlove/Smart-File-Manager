package com.smartfilemanager.model.domain;

/**
 * 操作符枚举
 */
public enum ConditionOperator {
    CONTAINS("包含"),
    EQUALS("等于"),
    NOT_EQUALS("不等于"),
    GREATER_THAN("大于"),
    LESS_THAN("小于"),
    GREATER_EQUAL("大于等于"),
    LESS_EQUAL("小于等于"),
    IN("在列表中"),
    NOT_IN("不在列表中"),
    STARTS_WITH("以...开头"),
    ENDS_WITH("以...结尾"),
    MATCHES_REGEX("匹配正则");

    private final String description;

    ConditionOperator(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}