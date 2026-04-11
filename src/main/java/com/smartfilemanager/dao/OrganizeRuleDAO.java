package com.smartfilemanager.dao;


import com.smartfilemanager.model.domain.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrganizeRuleDAO {

    @Autowired
    private DatabaseManager databaseManager;

    private final Gson gson = new Gson();
    private final Type actionListType = new TypeToken<List<RuleAction>>(){}.getType();

    /**
     * 插入规则（含条件）
     */
    public Long insertRule(OrganizeRule rule) throws SQLException {
        String ruleSql = "INSERT INTO rules (name, description, enabled, priority, watch_folder_id, " +
                         "condition_logic, actions_json) VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            conn.setAutoCommit(false);

            // 1. 插入规则主表
            Long ruleId;
            try (PreparedStatement stmt = conn.prepareStatement(ruleSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, rule.getName());
                stmt.setString(2, rule.getDescription());
                stmt.setInt(3, rule.getEnabled() ? 1 : 0);
                stmt.setInt(4, rule.getPriority());
                stmt.setLong(5, rule.getWatchFolderId());
                stmt.setString(6, rule.getConditionLogic());
                stmt.setString(7, gson.toJson(rule.getActions()));

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        ruleId = rs.getLong(1);
                    } else {
                        throw new SQLException("获取生成的规则ID失败");
                    }
                }
            }

            // 2. 插入规则条件
            if (rule.getConditions() != null && !rule.getConditions().isEmpty()) {
                insertConditions(conn, ruleId, rule.getConditions());
            }

            conn.commit();
            return ruleId;

        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }

    /**
     * 更新规则（含条件）
     */
    public void updateRule(OrganizeRule rule) throws SQLException {
        String ruleSql = "UPDATE rules SET name=?, description=?, enabled=?, priority=?, " +
                         "watch_folder_id=?, condition_logic=?, actions_json=?, updated_at=CURRENT_TIMESTAMP " +
                         "WHERE id=?";

        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            conn.setAutoCommit(false);

            // 1. 更新规则主表
            try (PreparedStatement stmt = conn.prepareStatement(ruleSql)) {
                stmt.setString(1, rule.getName());
                stmt.setString(2, rule.getDescription());
                stmt.setInt(3, rule.getEnabled() ? 1 : 0);
                stmt.setInt(4, rule.getPriority());
                stmt.setLong(5, rule.getWatchFolderId());
                stmt.setString(6, rule.getConditionLogic());
                stmt.setString(7, gson.toJson(rule.getActions()));
                stmt.setLong(8, rule.getId());

                stmt.executeUpdate();
            }

            // 2. 删除旧条件
            deleteConditions(conn, rule.getId());

            // 3. 插入新条件
            if (rule.getConditions() != null && !rule.getConditions().isEmpty()) {
                insertConditions(conn, rule.getId(), rule.getConditions());
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }

    /**
     * 删除规则
     */
    public void deleteRule(Long ruleId) throws SQLException {
        String sql = "DELETE FROM rules WHERE id=?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, ruleId);
            stmt.executeUpdate();
        }
    }

    /**
     * 获取所有启用的规则
     */
    public List<OrganizeRule> getAllEnabledRules() throws SQLException {
        String sql = "SELECT * FROM rules WHERE enabled=1 ORDER BY priority ASC";
        return queryRules(sql);
    }

    /**
     * 获取所有规则
     */
    public List<OrganizeRule> getAllRules() throws SQLException {
        String sql = "SELECT * FROM rules ORDER BY priority ASC";
        return queryRules(sql);
    }

    /**
     * 根据监控文件夹获取规则
     */
    public List<OrganizeRule> getRulesByWatchFolderId(Long watchFolderId) throws SQLException {
        String sql = "SELECT * FROM rules WHERE watch_folder_id=? AND enabled=1 ORDER BY priority ASC";

        List<OrganizeRule> rules = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, watchFolderId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OrganizeRule rule = mapResultSetToRule(rs);
                    rule.setConditions(loadConditionsForRule(conn, rule.getId()));
                    rules.add(rule);
                }
            }
        }
        return rules;
    }

    /**
     * 查询规则列表
     */
    private List<OrganizeRule> queryRules(String sql) throws SQLException {
        List<OrganizeRule> rules = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                OrganizeRule rule = mapResultSetToRule(rs);
                rule.setConditions(loadConditionsForRule(conn, rule.getId()));
                rules.add(rule);
            }
        }
        return rules;
    }

    /**
     * 映射结果集到规则对象
     */
    private OrganizeRule mapResultSetToRule(ResultSet rs) throws SQLException {
        OrganizeRule rule = new OrganizeRule();
        rule.setId(rs.getLong("id"));
        rule.setName(rs.getString("name"));
        rule.setDescription(rs.getString("description"));
        rule.setEnabled(rs.getInt("enabled") == 1);
        rule.setPriority(rs.getInt("priority"));
        rule.setWatchFolderId(rs.getLong("watch_folder_id"));
        rule.setConditionLogic(rs.getString("condition_logic"));

        // 解析动作JSON
        String actionsJson = rs.getString("actions_json");
        if (actionsJson != null && !actionsJson.isEmpty()) {
            rule.setActions(gson.fromJson(actionsJson, actionListType));
        }

        return rule;
    }

    /**
     * 加载规则的条件列表
     */
    private List<RuleCondition> loadConditionsForRule(Connection conn, Long ruleId) throws SQLException {
        List<RuleCondition> conditions = new ArrayList<>();
        String sql = "SELECT * FROM rule_conditions WHERE rule_id=? ORDER BY sort_order ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ruleId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RuleCondition condition = new RuleCondition();
                    condition.setId(rs.getLong("id"));
                    condition.setRuleId(rs.getLong("rule_id"));
                    condition.setConditionType(ConditionType.valueOf(rs.getString("condition_type")));
                    condition.setOperator(ConditionOperator.valueOf(rs.getString("operator")));
                    condition.setValue(rs.getString("value"));
                    condition.setSortOrder(rs.getInt("sort_order"));
                    conditions.add(condition);
                }
            }
        }
        return conditions;
    }

    /**
     * 插入条件列表
     */
    private void insertConditions(Connection conn, Long ruleId, List<RuleCondition> conditions)
            throws SQLException {
        String sql = "INSERT INTO rule_conditions (rule_id, condition_type, operator, value, sort_order) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < conditions.size(); i++) {
                RuleCondition condition = conditions.get(i);
                stmt.setLong(1, ruleId);
                stmt.setString(2, condition.getConditionType().name());
                stmt.setString(3, condition.getOperator().name());
                stmt.setString(4, condition.getValue());
                stmt.setInt(5, condition.getSortOrder() != null ? condition.getSortOrder() : i);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    /**
     * 删除规则的所有条件
     */
    private void deleteConditions(Connection conn, Long ruleId) throws SQLException {
        String sql = "DELETE FROM rule_conditions WHERE rule_id=?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ruleId);
            stmt.executeUpdate();
        }
    }
}

