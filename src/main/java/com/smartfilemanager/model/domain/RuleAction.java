package com.smartfilemanager.model.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleAction {
    private ActionType type;
    private String target;        // 目标路径（MOVE/COPY时使用）
    private String pattern;       // 重命名模式（RENAME时使用）
    private String tag;           // 标签内容（TAG时使用）

    public RuleAction(ActionType type, String target) {
        this.type = type;
        this.target = target;
    }
}

