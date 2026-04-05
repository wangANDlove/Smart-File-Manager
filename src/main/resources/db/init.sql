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
                                            FOREIGN KEY (folder_id) REFERENCES monitor_folders(id) ON DELETE CASCADE
                                            UNIQUE (file_id)
    );


