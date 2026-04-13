CREATE TABLE IF NOT EXISTS monitor_folders (
                                               id INTEGER PRIMARY KEY AUTOINCREMENT,
                                               folder_path TEXT NOT NULL UNIQUE,
                                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS file_records (
                                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                                            file_id TEXT, -- 对应 FileRecord 中的 fileId 字段
                                            file_name TEXT NOT NULL, -- 对应 FileRecord 中的 fileName 字段
                                            file_path TEXT NOT NULL, -- 对应 FileRecord 中的 filePath 字段
                                            is_folder BOOLEAN, -- 对应 FileRecord 中的 isFolder 字段
                                            folder_id INTEGER NOT NULL, -- 外键关联 monitor_folders 表
                                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                            FOREIGN KEY (folder_id) REFERENCES monitor_folders(id) ON DELETE CASCADE,
                                            UNIQUE (file_id)
    );
CREATE TABLE IF NOT EXISTS file_activities(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_id TEXT,
    file_name TEXT NOT NULL,
    file_path TEXT NOT NULL,
    activity_type TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    folder_id INTEGER NOT NULL,
    FOREIGN KEY (folder_id) REFERENCES monitor_folders(id) ON DELETE CASCADE
);
-- rules 规则主表
CREATE TABLE IF NOT EXISTS rules (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    enabled         INTEGER DEFAULT 1,
    priority        INTEGER DEFAULT 0,
    watch_folder_id INTEGER NOT NULL,
    condition_logic VARCHAR(10) DEFAULT 'AND',
    actions_json    TEXT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (watch_folder_id) REFERENCES monitor_folders(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_enabled_priority ON rules(enabled, priority);
CREATE INDEX IF NOT EXISTS idx_watch_folder_id ON rules(watch_folder_id);

-- rule_conditions 规则条件表
CREATE TABLE IF NOT EXISTS rule_conditions (
                                               id              INTEGER PRIMARY KEY AUTOINCREMENT,
                                               rule_id         INTEGER NOT NULL,
                                               condition_type  VARCHAR(50) NOT NULL,
    operator        VARCHAR(20) NOT NULL,
    value           TEXT NOT NULL,
    sort_order      INTEGER DEFAULT 0,
    FOREIGN KEY (rule_id) REFERENCES rules(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_rule_id ON rule_conditions(rule_id);

-- 标签表
CREATE TABLE IF NOT EXISTS tags (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                                    name TEXT NOT NULL UNIQUE,
                                    color TEXT DEFAULT '#4A90E2',
                                    usage_count INTEGER DEFAULT 0,
                                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 文件标签关联表（多对多）
CREATE TABLE IF NOT EXISTS file_tags (
                                         id INTEGER PRIMARY KEY AUTOINCREMENT,
                                         file_id TEXT NOT NULL,
                                         tag_id INTEGER NOT NULL,
                                         created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                         FOREIGN KEY (file_id) REFERENCES file_records(file_id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE,
    UNIQUE (file_id, tag_id)
    );

-- 创建索引优化查询性能
CREATE INDEX IF NOT EXISTS idx_file_tags_file_id ON file_tags(file_id);
CREATE INDEX IF NOT EXISTS idx_file_tags_tag_id ON file_tags(tag_id);
CREATE INDEX IF NOT EXISTS idx_tags_name ON tags(name);
CREATE INDEX IF NOT EXISTS idx_tags_usage_count ON tags(usage_count);



