package com.smartfilemanager.config;

import com.smartfilemanager.dao.DatabaseManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.sql.DataSource;
import java.nio.file.Paths;

@Configuration
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        // 使用 System.getProperty() 直接获取用户目录
        String userHome = System.getProperty("user.home");
        String dbPath = Paths.get(userHome, ".smartfilemanager", "smart_file_manager.db").toString();
        dataSource.setUrl("jdbc:sqlite:" + dbPath);
        dataSource.setDriverClassName("org.sqlite.JDBC");
        return dataSource;
    }


}
