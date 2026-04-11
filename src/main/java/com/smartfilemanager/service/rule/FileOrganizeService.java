package com.smartfilemanager.service.rule;

import com.smartfilemanager.dao.OrganizeRuleDAO;
import com.smartfilemanager.model.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class FileOrganizeService {

    @Autowired
    private RuleEngineService ruleEngineService;

    @Autowired
    private OrganizeRuleDAO organizeRuleDAO;

    /**
     * 处理文件活动 - 应用规则
     */
    public void processFileWithRules(FileActivity fileActivity) {
        try {
            // 1. 构建文件记录
            FileRecord fileRecord = buildFileRecord(fileActivity);
            Long watchFolderId = fileActivity.getFolderId();

            // 2. 获取匹配的规则
            var matchingRules = ruleEngineService.getMatchingRules(
                    fileRecord, watchFolderId);

            if (matchingRules.isEmpty()) {
                System.out.println("没有匹配的规则: " + fileActivity.getFilePath());
                return;
            }

            // 3. 执行最高优先级的规则
            OrganizeRule highestPriorityRule = matchingRules.get(0);
            executeRule(highestPriorityRule, fileRecord);

        } catch (Exception e) {
            System.err.println("处理文件规则失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 执行规则
     */
    private void executeRule(OrganizeRule rule, FileRecord fileRecord) {
        for (RuleAction action : rule.getActions()) {
            try {
                executeAction(action, fileRecord);
            } catch (Exception e) {
                System.err.println("执行动作失败: " + action.getType() +
                                 ", 错误: " + e.getMessage());
            }
        }
    }

    /**
     * 执行单个动作
     */
    private void executeAction(RuleAction action, FileRecord fileRecord) throws IOException {
        Path sourcePath = Paths.get(fileRecord.getFilePath());

        switch (action.getType()) {
            case MOVE:
                executeMove(action, sourcePath, fileRecord);
                break;
            case COPY:
                executeCopy(action, sourcePath, fileRecord);
                break;
            case RENAME:
                executeRename(action, sourcePath, fileRecord);
                break;
            case DELETE:
                executeDelete(sourcePath);
                break;
            default:
                System.out.println("未支持的动作类型: " + action.getType());
        }
    }

    /**
     * 执行移动操作
     */
    private void executeMove(RuleAction action, Path sourcePath, FileRecord fileRecord)
            throws IOException {
        String targetDir = resolveTargetPath(action.getTarget(), fileRecord);
        Path targetPath = Paths.get(targetDir).resolve(sourcePath.getFileName());

        Files.createDirectories(targetPath.getParent());
        Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("文件已移动: " + sourcePath + " -> " + targetPath);
    }

    /**
     * 执行复制操作
     */
    private void executeCopy(RuleAction action, Path sourcePath, FileRecord fileRecord)
            throws IOException {
        String targetDir = resolveTargetPath(action.getTarget(), fileRecord);
        Path targetPath = Paths.get(targetDir).resolve(sourcePath.getFileName());

        Files.createDirectories(targetPath.getParent());
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("文件已复制: " + sourcePath + " -> " + targetPath);
    }

    /**
     * 执行重命名操作
     */
    private void executeRename(RuleAction action, Path sourcePath, FileRecord fileRecord)
            throws IOException {
        String newName = applyRenamePattern(action.getPattern(), fileRecord);
        Path newPath = sourcePath.resolveSibling(newName);

        Files.move(sourcePath, newPath, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("文件已重命名: " + sourcePath + " -> " + newPath);
    }

    /**
     * 执行删除操作
     */
    private void executeDelete(Path sourcePath) throws IOException {
        Files.deleteIfExists(sourcePath);
        System.out.println("文件已删除: " + sourcePath);
    }

    /**
     * 解析目标路径（支持变量替换）
     */
    private String resolveTargetPath(String targetPath, FileRecord fileRecord) {
        if (targetPath == null) return targetPath;

        targetPath = targetPath.replace("{year}",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy")));
        targetPath = targetPath.replace("{month}",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM")));
        targetPath = targetPath.replace("{day}",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd")));
        targetPath = targetPath.replace("{fileType}",
                getFileExtension(fileRecord.getFileName()));

        return targetPath;
    }

    /**
     * 应用重命名模式
     */
    private String applyRenamePattern(String pattern, FileRecord fileRecord) {
        if (pattern == null) return fileRecord.getFileName();

        String newName = pattern;
        newName = newName.replace("{date}",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        newName = newName.replace("{originalName}",
                getFileNameWithoutExtension(fileRecord.getFileName()));
        newName = newName.replace("{extension}",
                getFileExtension(fileRecord.getFileName()));

        return newName;
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    private String getFileNameWithoutExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }

    private FileRecord buildFileRecord(FileActivity fileActivity) {
        // TODO: 从数据库或文件系统获取完整的文件信息
        return new FileRecord();
    }
}
