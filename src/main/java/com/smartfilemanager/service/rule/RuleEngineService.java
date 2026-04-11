package com.smartfilemanager.service.rule;

import com.smartfilemanager.dao.OrganizeRuleDAO;
import com.smartfilemanager.model.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RuleEngineService {

    @Autowired
    private OrganizeRuleDAO organizeRuleDAO;

    /**
     * 获取匹配文件的所有规则
     */
    public List<OrganizeRule> getMatchingRules(FileRecord fileRecord,  Long watchFolderId) {
        try {
            System.out.println("watchFolderId：" + watchFolderId);
            // 1. 获取该监控文件夹下的所有启用规则
            List<OrganizeRule> rules = organizeRuleDAO.getRulesByWatchFolderId(watchFolderId);
            System.out.println("获取匹配规则：" + rules);

            // 2. 过滤出匹配的规则
            return rules.stream()
                    .filter(rule -> matchesAllConditions(rule, fileRecord))
                    .sorted((r1, r2) -> r1.getPriority().compareTo(r2.getPriority()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("获取匹配规则失败", e);
        }
    }

    /**
     * 检查文件是否匹配规则的所有条件
     */
    private boolean matchesAllConditions(OrganizeRule rule, FileRecord fileRecord) {
        List<RuleCondition> conditions = rule.getConditions();

        if (conditions == null || conditions.isEmpty()) {
            return true; // 无条件则默认匹配
        }

        if ("AND".equalsIgnoreCase(rule.getConditionLogic())) {
            // 所有条件都必须满足
            return conditions.stream()
                    .allMatch(condition -> evaluateCondition(condition, fileRecord));
        } else {
            // OR逻辑：至少满足一个条件
            return conditions.stream()
                    .anyMatch(condition -> evaluateCondition(condition, fileRecord));
        }
    }

    /**
     * 评估单个条件
     */
    private boolean evaluateCondition(RuleCondition condition, FileRecord fileRecord) {
        switch (condition.getConditionType()) {
            case FILE_NAME:
                return evaluateFileNameCondition(condition, fileRecord);
            case FILE_EXTENSION:
                return evaluateExtensionCondition(condition, fileRecord);
            case FILE_SIZE:
                return evaluateSizeCondition(condition, fileRecord);
            case FILE_DATE:
                return evaluateDateCondition(condition, fileRecord);
            case FILE_PATH:
                return evaluatePathCondition(condition, fileRecord);
            default:
                return false;
        }
    }

    /**
     * 评估文件名条件
     */
    private boolean evaluateFileNameCondition(RuleCondition condition, FileRecord fileRecord) {
        String fileName = fileRecord.getFileName();
        String value = condition.getValue();

        switch (condition.getOperator()) {
            case CONTAINS:
                return fileName.contains(value);
            case EQUALS:
                return fileName.equals(value);
            case STARTS_WITH:
                return fileName.startsWith(value);
            case ENDS_WITH:
                return fileName.endsWith(value);
            case MATCHES_REGEX:
                return fileName.matches(value);
            default:
                return false;
        }
    }

    /**
     * 评估文件扩展名条件
     */
    private boolean evaluateExtensionCondition(RuleCondition condition, FileRecord fileRecord) {
        String fileName = fileRecord.getFileName();
        String extension = getFileExtension(fileName).toLowerCase();
        String value = condition.getValue().toLowerCase();

        switch (condition.getOperator()) {
            case EQUALS:
                return extension.equals(value);
            case IN:
                String[] extensions = value.split(",");
                for (String ext : extensions) {
                    if (extension.equals(ext.trim())) {
                        return true;
                    }
                }
                return false;
            case NOT_IN:
                String[] notInExtensions = value.split(",");
                for (String ext : notInExtensions) {
                    if (extension.equals(ext.trim())) {
                        return false;
                    }
                }
                return true;
            default:
                return false;
        }
    }

    /**
     * 评估文件大小条件
     */
    private boolean evaluateSizeCondition(RuleCondition condition, FileRecord fileRecord) {
        Path filePath = Paths.get(fileRecord.getFilePath());
        long fileSize = 0;
        try {
            fileSize = Files.size(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        long threshold = parseSizeString(condition.getValue());

        switch (condition.getOperator()) {
            case GREATER_THAN:
                return fileSize > threshold;
            case LESS_THAN:
                return fileSize < threshold;
            case GREATER_EQUAL:
                return fileSize >= threshold;
            case LESS_EQUAL:
                return fileSize <= threshold;
            case EQUALS:
                return fileSize == threshold;
            default:
                return false;
        }
    }

    /**
     * 评估文件日期条件
     */
    private boolean evaluateDateCondition(RuleCondition condition, FileRecord fileRecord) {
        try {
            Path filePath = Paths.get(fileRecord.getFilePath());
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            long fileTimeMillis = attrs.lastModifiedTime().toMillis();
            LocalDateTime fileDateTime = LocalDateTime.ofInstant(
                    new Date(fileTimeMillis).toInstant(), ZoneId.systemDefault());

            int daysAgo = Integer.parseInt(condition.getValue());
            LocalDateTime threshold = LocalDateTime.now().minusDays(daysAgo);

            switch (condition.getOperator()) {
                case GREATER_THAN:
                    return fileDateTime.isAfter(threshold);
                case LESS_THAN:
                    return fileDateTime.isBefore(threshold);
                default:
                    return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 评估文件路径条件
     */
    private boolean evaluatePathCondition(RuleCondition condition, FileRecord fileRecord) {
        String filePath = fileRecord.getFilePath();
        String value = condition.getValue();

        switch (condition.getOperator()) {
            case CONTAINS:
                return filePath.contains(value);
            case STARTS_WITH:
                return filePath.startsWith(value);
            default:
                return false;
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    /**
     * 解析文件大小字符串（如 "10MB" -> 字节）
     */
    private long parseSizeString(String sizeStr) {
        sizeStr = sizeStr.toUpperCase().trim();

        if (sizeStr.endsWith("KB")) {
            return Long.parseLong(sizeStr.replace("KB", "").trim()) * 1024;
        } else if (sizeStr.endsWith("MB")) {
            return Long.parseLong(sizeStr.replace("MB", "").trim()) * 1024 * 1024;
        } else if (sizeStr.endsWith("GB")) {
            return Long.parseLong(sizeStr.replace("GB", "").trim()) * 1024 * 1024 * 1024;
        } else {
            return Long.parseLong(sizeStr);
        }
    }

    /**
     * 解析文件大小（从数据库可能是字符串或数字）
     */
    private long parseFileSize(Object fileSizeObj) {
        if (fileSizeObj instanceof Number) {
            return ((Number) fileSizeObj).longValue();
        } else if (fileSizeObj instanceof String) {
            return parseSizeString((String) fileSizeObj);
        }
        return 0;
    }
}

