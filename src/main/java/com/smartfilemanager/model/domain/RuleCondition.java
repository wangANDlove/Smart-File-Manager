package com.smartfilemanager.model.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleCondition {
    private Long id;
    private Long ruleId;
    private ConditionType conditionType;
    private ConditionOperator operator;
    private String value;
    private Integer sortOrder;

    public RuleCondition(ConditionType conditionType, ConditionOperator operator, String value) {
        this.conditionType = conditionType;
        this.operator = operator;
        this.value = value;
    }
}
