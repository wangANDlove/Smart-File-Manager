package com.smartfilemanager.model.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizeRule {
    private Long id;
    private String name;
    private String description;
    private Boolean enabled;
    private Integer priority;
    private Long watchFolderId;
    private String conditionLogic;  // AND / OR

    private List<RuleCondition> conditions;  // 条件列表
    private List<RuleAction> actions;        // 动作列表

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrganizeRule(String name, Long watchFolderId) {
        this.name = name;
        this.watchFolderId = watchFolderId;
        this.enabled = true;
        this.priority = 0;
        this.conditionLogic = "AND";
        this.conditions = new ArrayList<>();
        this.actions = new ArrayList<>();
    }

    public void addCondition(RuleCondition condition) {
        if (conditions == null) {
            conditions = new ArrayList<>();
        }
        condition.setRuleId(this.id);
        condition.setSortOrder(conditions.size());
        conditions.add(condition);
    }

    public void addAction(RuleAction action) {
        if (actions == null) {
            actions = new ArrayList<>();
        }
        actions.add(action);
    }
}

