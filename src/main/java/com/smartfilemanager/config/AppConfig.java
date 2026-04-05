package com.smartfilemanager.config;

import lombok.Data;
import org.springframework.context.annotation.Configuration;
import java.nio.file.Paths;

@Data
@Configuration
public class AppConfig {
    private String watchedPath = Paths.get(System.getProperty("user.home"), "test").toString(); // 默认为用户主目录
    private String backupPath = Paths.get(System.getProperty("user.home"), "backup").toString();
}
