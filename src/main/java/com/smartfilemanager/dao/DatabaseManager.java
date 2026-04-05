package com.smartfilemanager.dao;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.hibernate.annotations.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Repository
public class DatabaseManager {
    private static final String DB_NAME = "smart_file_manager.db";
    private static final String INIT_SCRIPT = "db/init.sql";
    private  Connection connection;
    //@Autowired
    private final DataSource dataSource;
    @Autowired
    public DatabaseManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 初始化数据库
     */
    @PostConstruct
    public void initialize() throws SQLException {
        // 获取数据库路径（存储在用户目录下）
        String userHome = System.getProperty("user.home");
        String appDataDir = Paths.get(userHome, ".smartfilemanager").toString();
        String dbPath = Paths.get(appDataDir, DB_NAME).toString();
        System.out.println("测试点1： 数据库路径：" + dbPath);

        // 确保应用数据目录存在
        createAppDataDirectory(appDataDir);

        // 创建数据库连接
        connection = dataSource.getConnection();

        // 设置连接属性:关闭自动提交
        connection.setAutoCommit(false);

        // 创建表（如果不存在）
        createTablesIfNotExist();

        // 检查数据库版本，执行升级脚本
        //checkAndUpdateDatabaseVersion();
    }

    /**
     * 创建应用数据目录
     */
    private void createAppDataDirectory(String path) {
        //System.out.println("测试点2： 创建应用数据目录");
        // 实现目录创建逻辑
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        java.io.File dir = Paths.get(path).toFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Failed to create directory: " + path);
            }
        }else {
            System.out.println("应用数据目录已存在");
        }
    }

    /**
     * 创建所有表
     */
    private void createTablesIfNotExist() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // 读取初始化脚本并执行
            String initSql = loadInitScript();
            if (initSql != null) {
                stmt.executeUpdate(initSql);
                connection.commit();
            }

        }
    }

    private String loadInitScript() {
        try (InputStream inputStream = DatabaseManager.class.getClassLoader().getResourceAsStream(INIT_SCRIPT)) {
            if (inputStream == null) {
                throw new RuntimeException("无法找到初始化脚本文件: db/init.sql");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取初始化脚本失败", e);
        }
    }

    /**
     * 检查并更新数据库版本
     */
    private static void checkAndUpdateDatabaseVersion() throws SQLException {
        // 检查db_version表是否存在
        // 比较当前版本与期望版本
        // 执行必要的升级脚本
    }

    // 注入 DataSource
//    public static void setDataSource(DataSource dataSource) {
//        DatabaseManager.dataSource = dataSource;
//    }

    /**
     * 获取数据库连接
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * 关闭数据库连接
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("关闭数据库连接时出错: " + e.getMessage());
        }
    }

    /**
     * 执行数据库备份
     */
    //@PreDestroy
    public void backupDatabase(String backupPath) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("BACKUP TO '" + backupPath + "'");
        }
    }

    /**
     * 从备份恢复数据库
     */
    public void restoreDatabase(String backupPath) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("RESTORE FROM '" + backupPath + "'");
        }
    }

    /**
     * 执行数据库维护
     */
    public void performMaintenance() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // 清理过期数据
            stmt.executeUpdate("DELETE FROM ai_category_cache WHERE expires_at < datetime('now')");

            // 清理旧的搜索历史
            stmt.executeUpdate("DELETE FROM search_history WHERE created_at < datetime('now', '-30 days')");

            // 清理完成的操作日志
            stmt.executeUpdate("DELETE FROM operation_logs WHERE status = 'SUCCESS' AND operation_time < datetime('now', '-90 days')");

            // 执行VACUUM优化数据库
            stmt.executeUpdate("VACUUM");

            connection.commit();
        }
    }

    /**
     * 获取数据库统计信息
     */
    public Map<String, Object> getDatabaseStats() throws SQLException {
        Map<String, Object> stats = new HashMap<>();

        try (Statement stmt = connection.createStatement()) {
            // 获取表记录数
            String[] tables = {"monitor_folders", "file_rules", "file_records", "tags",
                    "file_tag_relations", "operation_logs", "ai_category_cache"};

            for (String table : tables) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM " + table);
                if (rs.next()) {
                    stats.put(table + "_count", rs.getInt("count"));
                }
            }

            // 获取数据库大小
            ResultSet rs = stmt.executeQuery("PRAGMA page_count * PRAGMA page_size");
            if (rs.next()) {
                stats.put("database_size_kb", rs.getLong(1) / 1024);
            }
        }

        return stats;
    }
}
