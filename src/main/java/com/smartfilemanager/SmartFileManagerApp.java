package com.smartfilemanager;

import com.smartfilemanager.dao.FileActivityDAO;
import com.smartfilemanager.dao.MonitorFoldersDAO;
import com.smartfilemanager.service.core.FileMonitorService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import com.smartfilemanager.controller.MainController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class SmartFileManagerApp extends Application {

    private Stage primaryStage;
    private static ConfigurableApplicationContext applicationContext;

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.primaryStage = primaryStage;
//        MainController controller = new MainController();

        try {
            // 加载主界面FXML
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/fxml/main-view.fxml")
            );
            Parent root = loader.load();

            // 获取控制器并传递主舞台引用
            MainController controller = loader.getController();
            System.out.println("DEBUG: applicationContext is null? " + (applicationContext == null));
            // 手动注入 Spring 管理的依赖
            if (applicationContext != null) {
                FileActivityDAO fileActivityDAO = applicationContext.getBean(FileActivityDAO.class);
                controller.setFileActivityDAO(fileActivityDAO);

                // 注入 MonitorFoldersDAO（用于规则编辑器）
                try {
                    MonitorFoldersDAO monitorFoldersDAO = applicationContext.getBean(MonitorFoldersDAO.class);
                    controller.setMonitorFoldersDAO(monitorFoldersDAO);
                    System.out.println("MonitorFoldersDAO 注入成功");
                } catch (Exception e) {
                    System.err.println("MonitorFoldersDAO 注入失败: " + e.getMessage());
                }

                // 注入 OrganizeRuleDAO（用于规则管理）
                try {
                    com.smartfilemanager.dao.OrganizeRuleDAO organizeRuleDAO =
                            applicationContext.getBean(com.smartfilemanager.dao.OrganizeRuleDAO.class);
                    controller.setOrganizeRuleDAO(organizeRuleDAO);
                    System.out.println("OrganizeRuleDAO 注入成功");
                } catch (Exception e) {
                    System.err.println("OrganizeRuleDAO 注入失败: " + e.getMessage());
                }

                // 从 Spring 容器获取 FileMonitorService
                FileMonitorService fileMonitorService = applicationContext.getBean(FileMonitorService.class);
                controller.setFileMonitorService(fileMonitorService);

            } else {
                System.err.println("严重错误：Spring 上下文尚未初始化！无法注入 FileMonitorService");
            }

            // 创建场景
            Scene scene = new Scene(root, 1200, 800);

            // 设置主窗口属性
            primaryStage.setTitle("智能文件管家 v1.0.0");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            // 设置应用图标
            try {
                Image icon = new Image(getClass().getResourceAsStream(
                        "/image/logo/icon.png"
                ));
                primaryStage.getIcons().add(icon);
            } catch (Exception e) {
                System.err.println("无法加载应用图标: " + e.getMessage());
            }

            // 设置关闭请求处理
            primaryStage.setOnCloseRequest(event -> {
                event.consume(); // 先阻止默认关闭行为
                handleExit();
            });

            // 显示窗口
            primaryStage.show();

            // 应用启动后初始化
            Platform.runLater(() -> {
                controller.initialize(primaryStage);
                // 可以在这里添加启动后的初始化操作
            });

        } catch (IOException e) {
            System.err.println("无法加载主界面: " + e.getMessage());
            e.printStackTrace();
            Platform.exit();
        }
    }

    /**
     * 处理应用退出
     */
    private void handleExit() {
        // 这里可以添加保存设置、清理资源等操作
        System.out.println("智能文件管家正在退出...");

        // 确认退出
        // TODO: 可以添加确认对话框

        // 执行退出
        Platform.exit();
        System.exit(0);
    }

    /**
     * 获取主舞台
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        // 设置JavaFX线程异常处理器
//        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
//            System.err.println("未捕获的异常: " + throwable.getMessage());
//            throwable.printStackTrace();
//        });
        applicationContext = SpringApplication.run(SmartFileManagerApp.class, args);

        // 启动JavaFX应用
        launch(args);
    }
}