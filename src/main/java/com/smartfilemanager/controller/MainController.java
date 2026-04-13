package com.smartfilemanager.controller;

import com.smartfilemanager.dao.DatabaseManager;
import com.smartfilemanager.dao.FileActivityDAO;
import com.smartfilemanager.dao.MonitorFoldersDAO;
import com.smartfilemanager.dao.OrganizeRuleDAO;
import com.smartfilemanager.model.domain.ActivityRecord;
import com.smartfilemanager.model.domain.FileRecord;
import com.smartfilemanager.model.domain.MonitorFolders;
import com.smartfilemanager.model.domain.OrganizeRule;
import com.smartfilemanager.service.core.FileMonitorService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Controller
public class MainController implements Initializable {
//主界面控制器

    // 顶部组件
    @FXML private MenuBar menuBar;
    @FXML private ToolBar toolBar;
    //顶部区域：菜单栏：监控菜单
    @FXML private MenuItem startMonitorMenuItem;
    @FXML private MenuItem stopMonitorMenuItem;

    @FXML private Button startMonitorToolbarButton;
    @FXML private Button stopMonitorToolbarButton;



    // 左侧导航组件
    @FXML private Label monitorStatusLabel;
    @FXML private Label monitorFolderCountLabel;
    @FXML private Label activeRuleCountLabel;
    @FXML private Label organizedFileCountLabel;
    @FXML private FlowPane tagCloudPane;
    @FXML private ToggleButton aiRecommendationToggle;
    @FXML private ProgressBar aiAccuracyProgress;

    // 中间组件 - 仪表板
    @FXML private TabPane mainTabPane;
    @FXML private Label monitoredFoldersLabel;
    @FXML private Label totalRulesLabel;
    @FXML private Label totalFilesLabel;
    @FXML private Label spaceSavedLabel;
    @FXML private TableView<ActivityRecord> recentActivitiesTable;

    // 中间组件 - 规则管理
    @FXML private TextField ruleSearchField;
    @FXML private TableView<OrganizeRule> rulesTable;
    @FXML private TableColumn<OrganizeRule, Boolean> ruleEnabledColumn;
    @FXML private TableColumn<OrganizeRule, String> ruleNameColumn;
    @FXML private TableColumn<OrganizeRule, String> ruleWatchFolderColumn;
    @FXML private TableColumn<OrganizeRule, Integer> rulePriorityColumn;
    @FXML private TableColumn<OrganizeRule, Integer> ruleConditionCountColumn;
    @FXML private Label totalRulesCountLabel;

    // 中间组件 - 文件监控
    @FXML private ToggleButton monitorToggleButton;
    @FXML private ListView<String> monitorFoldersList;
    @FXML private TableView<ActivityRecord> fileActivitiesTable;

    // 监控文件夹中的文件列表
    @FXML private TableView<FileRecord> monitoredFilesTable;
    @FXML private TableColumn<FileRecord, String> monitoredFileNameColumn;
    @FXML private TableColumn<FileRecord, String> monitoredFilePathColumn;
    @FXML private TableColumn<FileRecord, String> monitoredFileTypeColumn;
    @FXML private TableColumn<FileRecord, String> monitoredFileSizeColumn;
    @FXML private TableColumn<FileRecord, String> monitoredFileModifiedColumn;

    // 中间组件 - 智能搜索
    @FXML private TextField searchInput;
    @FXML private ToggleGroup searchTypeGroup;
    @FXML private ComboBox<String> timeRangeCombo;
    @FXML private TableView<SearchResult> searchResultsTable;
    @FXML private Label searchResultCountLabel;

    // 右侧组件
    @FXML private VBox fileDetailsPane;
    @FXML private Label detailFileName;
    @FXML private Label detailFilePath;
    @FXML private Label detailFileSize;
    @FXML private Label detailFileModified;
    @FXML private Label detailFileType;
    @FXML private FlowPane detailFileTags;
    @FXML private Label noFileSelectedLabel;

    @FXML private ProgressBar cpuUsageBar;
    @FXML private Label cpuUsageLabel;
    @FXML private ProgressBar memoryUsageBar;
    @FXML private Label memoryUsageLabel;
    @FXML private ProgressBar diskUsageBar;
    @FXML private Label diskUsageLabel;

    @FXML private Label tipTitle;
    @FXML private Label tipContent;

    // 底部状态栏
    @FXML private Label statusMessage;
    @FXML private Label statusMonitorStatus;
    @FXML private Label statusFileCount;
    @FXML private ProgressIndicator statusProgressIndicator;
    @FXML private Label statusOperation;
    @FXML private Label versionLabel;

    // 数据模型
    private Stage primaryStage;

    FileMonitorService fileMonitorService;

    private DatabaseManager databaseManager;

    private FileActivityDAO fileActivityDAO;

    private com.smartfilemanager.dao.OrganizeRuleDAO organizeRuleDAO;

    MonitorFoldersDAO monitorFoldersDAO;

    private com.smartfilemanager.dao.FileRecordDAO fileRecordDAO;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化UI组件
        initializeComponents();
        //loadInitialData();
        setupEventHandlers();
        // 初始化监控状态
        initializeMonitorStatus();


    }

    public void initialize(Stage primaryStage) {
        this.primaryStage = primaryStage;
        // 窗口相关初始化
    }

    private void initializeComponents() {
        // 初始化各个组件
        initializeDashboard();
        initializeRulesTab();
        initializeMonitorTab();
        initializeSearchTab();
        initializeStatusBar();
    }
    private void registerActivityListener() {
        if (fileMonitorService != null) {
            fileMonitorService.addActivityListener(activity -> {
                javafx.application.Platform.runLater(() -> {
                    loadRecentActivities();
                    loadFileActivities();
                    // 检查是否是扫描完成事件，刷新文件列表
                    if ("SCAN_COMPLETED".equals(activity.getFileName())) {
                        loadMonitoredFiles();
                    }
                });
            });
        }
    }

    private void loadInitialData() {
        // 加载初始数据
        loadStatistics();
        loadRecentActivities();
        loadRules();
        loadMonitorFolders();
        loadSystemStatus();
    }

    private void setupEventHandlers() {
        // 设置事件处理器
    }
    private void initializeMonitorStatus() {
        // 设置初始状态
        if (startMonitorMenuItem != null) {
            startMonitorMenuItem.setDisable(false);
        }
        if (stopMonitorMenuItem != null) {
            stopMonitorMenuItem.setDisable(true);
        }
        if(startMonitorToolbarButton != null){
            startMonitorToolbarButton.setDisable(false);
        }
        if(stopMonitorToolbarButton != null){
            stopMonitorToolbarButton.setDisable(true);
        }
        monitorStatusLabel.getStyleClass().removeAll("stopped", "running", "paused");
        monitorStatusLabel.getStyleClass().add("stopped");
    }

    // ========== 菜单事件处理方法 ==========

    @FXML
    private void handleNewRule() {
        // 新建规则
        showRuleEditDialog(null);
    }


    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleImportRule() {
        // 导入规则
    }

    @FXML
    private void handleExportRule() {
        // 导出规则
    }

    @FXML
    private void handleOpenSettings() {
        // 打开设置
    }

    @FXML
    private void handleExit() {
        // 退出应用
        if (primaryStage != null) {
            primaryStage.close();
        }
    }

    @FXML
    private void handleStartMonitoring() throws IOException {
        // 开始监控
        monitorToggleButton.setSelected(true);
        updateMonitorButtonStatus( true);
        System.out.println("开始监控");
        fileMonitorService.startMonitoring();

    }

    /**
     *
     *
     * @param
     * @return
     * @author wjd
     * @create 2026/3/14
     * @description     处理停止监控按钮
     **/
    @FXML
    private void handleStopMonitoring() {
        // 停止监控
        monitorToggleButton.setSelected(false);
        updateMonitorButtonStatus(false);
        System.out.println("停止监控");
        fileMonitorService.stopMonitoring();
    }
    private void updateMonitorButtonStatus(boolean isMonitoring) {
        // 更新菜单项状态
        if (startMonitorMenuItem != null) {
            startMonitorMenuItem.setDisable(isMonitoring);
        }
        if (stopMonitorMenuItem != null) {
            stopMonitorMenuItem.setDisable(!isMonitoring);
        }

        // 更新工具栏按钮状态
        if (startMonitorToolbarButton != null) {
            startMonitorToolbarButton.setDisable(isMonitoring);
        }
        if (stopMonitorToolbarButton != null) {
            stopMonitorToolbarButton.setDisable(!isMonitoring);
        }

        if(isMonitoring){
            //修改底部状态栏文本内容
            statusMonitorStatus.setText("正在监控");
            monitorStatusLabel.setText("正在监控");
            monitorStatusLabel.getStyleClass().removeAll("stopped", "running", "paused");
            monitorStatusLabel.getStyleClass().add("running");


        }else{
            statusMonitorStatus.setText("已停止");
            monitorStatusLabel.setText("已停止");
            monitorStatusLabel.getStyleClass().removeAll("stopped", "running", "paused");
            monitorStatusLabel.getStyleClass().add("stopped");
        }
    }

    /***
     * 添加监控文件夹
     */
    @FXML
    private void handleAddMonitorFolder() {
        // 1. 创建目录选择器
        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle("选择要监控的文件夹");

        // 可选：设置初始目录为当前用户主目录
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            directoryChooser.setInitialDirectory(new java.io.File(userHome));
        }

        // 2. 显示对话框并获取结果 (primaryStage 需要在控制器中正确注入或传递)
        if (this.primaryStage == null) {
            System.err.println("错误：primaryStage 未初始化，无法显示文件选择窗口");
            return;
        }

        java.io.File selectedDirectory = directoryChooser.showDialog(this.primaryStage);

        // 3. 处理用户选择的结果
        if (selectedDirectory != null) {
            String folderPath = selectedDirectory.getAbsolutePath();
            System.out.println("用户选择的文件夹：" + folderPath);

            //
            // 例如：fileMonitorService.addMonitorFolder(folderPath);
            fileMonitorService.addMonitorFolder(folderPath);

            // 提示用户成功 (可选)
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("添加成功");
            alert.setHeaderText(null);
            alert.setContentText("已添加监控文件夹:\n" + folderPath);
            alert.showAndWait();

            //
            loadMonitorFolders();
        } else {
            // 用户取消了选择
            System.out.println("用户取消了文件夹选择");
        }
    }


    @FXML
    private void handleManualOrganize() {
        // 手动整理
    }

    @FXML
    private void handleSmartRename() {
        // 智能重命名
    }

    @FXML
    private void handleBatchTag() {
        // 批量标签管理
    }

    @FXML
    private void handleFindDuplicates() {
        // 查找重复文件
    }

    @FXML
    private void handleClearCache() {
        // 清理缓存
    }

    @FXML
    private void handleStorageAnalysis() {
        // 存储分析
    }

    @FXML
    private void handleToggleStatusBar() {
        // 切换状态栏显示
    }

    @FXML
    private void handleToggleSidebar() {
        // 切换侧边栏显示
    }

    @FXML
    private void handleOpenManual() {
        // 打开用户手册
    }

    @FXML
    private void handleOpenTutorial() {
        // 打开视频教程
    }

    @FXML
    private void handleCheckUpdate() {
        // 检查更新
    }

    @FXML
    private void handleShowAbout() {
        // 显示关于对话框
    }

    // ========== 工具栏事件处理方法 ==========

    @FXML
    private void handleEditRule() {
        OrganizeRule selectedRule = rulesTable.getSelectionModel().getSelectedItem();
        if (selectedRule != null) {
            showRuleEditDialog(selectedRule);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText("请先在规则列表中选择要编辑的规则");
            alert.showAndWait();
        }
    }

    private void showRuleEditDialog(OrganizeRule rule) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/fxml/rule-edit-dialog.fxml")
            );

            DialogPane dialogPane = loader.load();
            RuleEditController controller = loader.getController();

            // 先注入依赖（这会自动触发 loadWatchFolders）
            if (monitorFoldersDAO != null) {
                controller.setMonitorFoldersDAO(monitorFoldersDAO);
            } else {
                System.err.println("警告：monitorFoldersDAO 未初始化");
                showAlert(Alert.AlertType.ERROR, "错误", "系统初始化未完成，请重启应用");
                return;
            }

            if (rule != null) {
                controller.setRule(rule);
            }

            Dialog<OrganizeRule> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle(rule == null ? "新建规则" : "编辑规则");

            // 设置结果转换器
            dialog.setResultConverter(buttonType -> {
                ButtonBar.ButtonData data = buttonType == null ? null : buttonType.getButtonData();
                if (data == ButtonBar.ButtonData.OK_DONE) {
                    return controller.getRule();
                }
                return null;
            });

            // 获取测试按钮并设置行为
            Button testButton = (Button) dialogPane.lookupButton(ButtonType.APPLY);
            if (testButton != null) {
                testButton.setText("测试规则");
                testButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                    controller.handleTestRule();
                    event.consume();
                });
            }

            //保存处理结果
            dialog.showAndWait().ifPresent(savedRule -> {
                try {
                    if (rule == null) {
                        System.out.println("保存规则: " + savedRule);
                        organizeRuleDAO.insertRule(savedRule);
                        showAlert(Alert.AlertType.INFORMATION, "成功", "规则创建成功");
                    } else {
                        savedRule.setId(rule.getId());
                        organizeRuleDAO.updateRule(savedRule);
                        showAlert(Alert.AlertType.INFORMATION, "成功", "规则更新成功");
                    }
                    loadRules();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "错误", "保存规则失败: " + e.getMessage());
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "打开规则编辑器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * 主页面删除规则按钮响应
     */
    @FXML
    private void handleDeleteRule() {
        OrganizeRule selectedRule = rulesTable.getSelectionModel().getSelectedItem();
        if (selectedRule == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText("请先在规则列表中选择要删除的规则");
            alert.showAndWait();
            return;
        }
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("确定要删除规则 \""+ selectedRule.getName() + "\" 吗？");
        java.util.Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                organizeRuleDAO.deleteRule(selectedRule.getId());
                showAlert(Alert.AlertType.INFORMATION, "成功", "规则删除成功");
                loadRules();//刷新规则列表
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "错误", "删除规则失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleOpenSearch() {
        // 打开搜索
        mainTabPane.getSelectionModel().select(3); // 切换到搜索标签页
    }

    @FXML
    private void handleOpenLogs() {
        // 打开日志
    }

    // ========== 左侧导航事件处理方法 ==========

    @FXML
    private void handleManageMonitorFolders() {
        // 管理监控文件夹
        // 切换到文件监控标签页（索引为 2）
        mainTabPane.getSelectionModel().select(2);
    }

    @FXML
    private void handleOrganizeDesktop() {
        // 整理桌面
    }

    @FXML
    private void handleOrganizeDownloads() {
        // 整理下载文件夹
    }

    @FXML
    private void handleSmartSearch() {
        // 智能搜索
        mainTabPane.getSelectionModel().select(3);
        searchInput.requestFocus();
    }

    @FXML
    private void handleManageTags() {
        // 管理标签
    }

    @FXML
    private void handleOpenTagManager() {
        // 打开标签管理器
    }

    @FXML
    private void handleToggleAIRecommendation() {
        // 切换AI推荐
    }

    @FXML
    private void handleAISettings() {
        // AI设置
    }

    // ========== 仪表板事件处理方法 ==========

    @FXML
    private void handleGetStarted() {
        // 开始使用指南
    }

    @FXML
    private void handleViewAllActivities() {
        // 查看全部活动
        mainTabPane.getSelectionModel().select(2);
    }

    // ========== 规则管理事件处理方法 ==========

    @FXML
    private void handleClearMonitorList() {
        // 清空监控列表
    }

    /*
     *
     *
     * @param null
     * @return
     * @author wjd
     * @create 2026/3/17
     * @description   移除选中的监控文件夹并刷新
     **/
    @FXML
    private void handleRemoveMonitorFolder() {
        // 移除监控文件夹
        String selectedFolder = monitorFoldersList.getSelectionModel().getSelectedItem(); // 获取选中的文件夹路径
        if (selectedFolder == null) {
            // 没有选中的条目，显示提示对话框
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText(null);
            alert.setContentText("请先从列表中选择一个要移除的监控文件夹");
            alert.showAndWait();
            return;
        }
        // 确认删除
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("确定要移除监控文件夹吗？\n" + selectedFolder);

        java.util.Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 用户确认删除，调用 Service 层执行删除
            fileMonitorService.removeMonitorFolder(selectedFolder);

            // 刷新列表
            loadMonitorFolders();

            // 显示成功提示
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("删除成功");
            successAlert.setHeaderText(null);
            successAlert.setContentText("已成功移除监控文件夹:\n" + selectedFolder);
            successAlert.showAndWait();
        }
    }

    // ========== 搜索事件处理方法 ==========

    @FXML
    private void handleSearch() {
        // 执行搜索
    }

    @FXML
    private void handleAdvancedSearch() {
        // 高级搜索
    }

    @FXML
    private void handleSaveSearch() {
        // 保存搜索
    }

    // ========== 右侧面板事件处理方法 ==========

    @FXML
    private void handleOpenFile() {
        // 打开文件
    }

    @FXML
    private void handleOpenFileLocation() {
        // 打开文件位置
    }

    @FXML
    private void handleAIClassify() {
        // AI智能分类
    }

    @FXML
    private void handleSmartRenameFile() {
        // 智能重命名文件
    }

    @FXML
    private void handleGenerateFileReport() {
        // 生成文件报告
    }

    @FXML
    private void handleNextTip() {
        // 下一个提示
    }

    // ========== 初始化具体方法 ==========

    private void initializeDashboard() {
        // 初始化仪表板
    }

    private void initializeRulesTab() {
        // 初始化规则管理标签页
        setupRulesTable();
    }
    private void setupRulesTable() {
        if (rulesTable == null) return;

        // 启用状态列
        ruleEnabledColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("enabled"));
        ruleEnabledColumn.setCellFactory(col -> new TableCell<OrganizeRule, Boolean>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setStyle("-fx-cursor: hand;");
                checkBox.setOnAction(event -> {
                    OrganizeRule rule = getTableView().getItems().get(getIndex());
                    if (rule != null) {
                        boolean newEnabled = checkBox.isSelected();
                        rule.setEnabled(newEnabled);

                        try {
                            organizeRuleDAO.updateRule(rule);
                            System.out.println("规则 '" + rule.getName() + "' 已" + (newEnabled ? "启用" : "禁用"));

                            loadRules();

                        } catch (SQLException e) {
                            System.err.println("更新规则状态失败: " + e.getMessage());
                            e.printStackTrace();
                            showAlert(Alert.AlertType.ERROR, "错误", "更新规则状态失败: " + e.getMessage());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(item);
                    setGraphic(checkBox);
                }
            }
        });

        // 规则名称列
        ruleNameColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));

        // 监控文件夹列
        ruleWatchFolderColumn.setCellValueFactory(data -> {
            OrganizeRule rule = data.getValue();
            if (rule == null || rule.getWatchFolderId() == null) {
                return new javafx.beans.property.SimpleStringProperty("-");
            }

            try {
                com.smartfilemanager.model.domain.MonitorFolders folder =
                        monitorFoldersDAO.getFolderById(rule.getWatchFolderId());
                String path = folder != null ? folder.getFolderPath() : "未知文件夹";
                return new javafx.beans.property.SimpleStringProperty(path);
            } catch (Exception e) {
                System.err.println("获取文件夹路径失败: " + e.getMessage());
                return new javafx.beans.property.SimpleStringProperty("错误");
            }
        });

        // 优先级列
        rulePriorityColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("priority"));

        // 条件数列
        ruleConditionCountColumn.setCellValueFactory(data -> {
            OrganizeRule rule = data.getValue();
            int count = rule.getConditions() != null ? rule.getConditions().size() : 0;
            return new javafx.beans.property.SimpleIntegerProperty(count).asObject();
        });
    }

    private void initializeMonitorTab() {
        // 初始化文件监控标签页
        setupMonitoredFilesTable();

    }
    private void setupMonitoredFilesTable() {
        if (monitoredFilesTable == null) return;

        // 文件名
        monitoredFileNameColumn.setCellValueFactory(
                new javafx.scene.control.cell.PropertyValueFactory<>("fileName"));

        // 路径
        monitoredFilePathColumn.setCellValueFactory(
                new javafx.scene.control.cell.PropertyValueFactory<>("filePath"));

        // 类型
        monitoredFileTypeColumn.setCellValueFactory(data -> {
            FileRecord record = data.getValue();
            String type = record != null && Boolean.TRUE.equals(record.getIsFolder()) ? "文件夹" : "文件";
            return new javafx.beans.property.SimpleStringProperty(type);
        });

        // 大小
        monitoredFileSizeColumn.setCellValueFactory(data -> {
            FileRecord record = data.getValue();
            if (record == null || Boolean.TRUE.equals(record.getIsFolder())) {
                return new javafx.beans.property.SimpleStringProperty("-");
            }
            try {
                java.nio.file.Path path = java.nio.file.Paths.get(record.getFilePath());
                long size = java.nio.file.Files.size(path);
                return new javafx.beans.property.SimpleStringProperty(formatFileSize(size));
            } catch (Exception e) {
                return new javafx.beans.property.SimpleStringProperty("-");
            }
        });

        // 修改时间
        monitoredFileModifiedColumn.setCellValueFactory(data -> {
            FileRecord record = data.getValue();
            if (record == null) {
                return new javafx.beans.property.SimpleStringProperty("-");
            }
            try {
                java.nio.file.Path path = java.nio.file.Paths.get(record.getFilePath());
                java.nio.file.attribute.BasicFileAttributes attrs =
                        java.nio.file.Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class);
                java.time.LocalDateTime modTime = java.time.LocalDateTime.ofInstant(
                        attrs.lastModifiedTime().toInstant(), java.time.ZoneId.systemDefault());
                return new javafx.beans.property.SimpleStringProperty(
                        modTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (Exception e) {
                return new javafx.beans.property.SimpleStringProperty("-");
            }
        });

        // 监听文件选择事件，显示文件详情
        monitoredFilesTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showFileDetails(newValue));
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    private void showFileDetails(FileRecord fileRecord) {
        if (fileRecord == null) {
            fileDetailsPane.setVisible(false);
            noFileSelectedLabel.setVisible(true);
            return;
        }

        fileDetailsPane.setVisible(true);
        noFileSelectedLabel.setVisible(false);

        detailFileName.setText(fileRecord.getFileName());
        detailFilePath.setText(fileRecord.getFilePath());

        // 获取文件大小
        if (Boolean.TRUE.equals(fileRecord.getIsFolder())) {
            detailFileSize.setText("-");
            detailFileType.setText("文件夹");
        } else {
            try {
                java.nio.file.Path path = java.nio.file.Paths.get(fileRecord.getFilePath());
                long size = java.nio.file.Files.size(path);
                detailFileSize.setText(formatFileSize(size));

                // 获取文件类型（扩展名）
                String fileName = fileRecord.getFileName();
                int lastDot = fileName.lastIndexOf('.');
                String extension = lastDot > 0 ? fileName.substring(lastDot + 1).toUpperCase() : "未知";
                detailFileType.setText(extension);
            } catch (Exception e) {
                detailFileSize.setText("-");
                detailFileType.setText("未知");
            }
        }

        // 获取修改时间
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(fileRecord.getFilePath());
            java.nio.file.attribute.BasicFileAttributes attrs =
                    java.nio.file.Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class);
            java.time.LocalDateTime modTime = java.time.LocalDateTime.ofInstant(
                    attrs.lastModifiedTime().toInstant(), java.time.ZoneId.systemDefault());
            detailFileModified.setText(modTime.format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } catch (Exception e) {
            detailFileModified.setText("-");
        }
    }

    @FXML
    private void handleRefreshFileList() {
        loadMonitoredFiles();
    }

    private void loadMonitoredFiles() {
        try {
            if (fileRecordDAO == null) {
                System.err.println("错误：fileRecordDAO 尚未注入");
                return;
            }

            List<FileRecord> allFiles = fileRecordDAO.getAllFileRecords();
            ObservableList<FileRecord> observableFiles = FXCollections.observableArrayList(allFiles);

            monitoredFilesTable.setItems(observableFiles);

            // 更新状态栏文件计数
            if (statusFileCount != null) {
                statusFileCount.setText(String.valueOf(allFiles.size()));
            }
            if (totalFilesLabel != null) {
                totalFilesLabel.setText(String.valueOf(allFiles.size()));
            }

            System.out.println("已加载 " + allFiles.size() + " 个监控文件");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("加载监控文件失败：" + e.getMessage());
        }
    }



    private void loadFileActivities() {
        try {
            if (fileActivityDAO == null) {
                System.err.println("错误1：fileActivityDAO 尚未注入");
                return;
            }

            List<com.smartfilemanager.model.domain.FileActivity> activities = fileActivityDAO.getRecentActivities(10);

            ObservableList<ActivityRecord> records = FXCollections.observableArrayList();
            for (com.smartfilemanager.model.domain.FileActivity activity : activities) {
                ActivityRecord record = new ActivityRecord();
                record.setTime(activity.getCreatedAt() != null ? activity.getCreatedAt().toString() : "未知时间");
                record.setOperation(activity.getActivityType() != null ? activity.getActivityType().toString() : "未知");
                record.setFileName(activity.getFilePath() != null ? activity.getFilePath() : "未知路径");
                record.setStatus("完成");
                records.add(record);
            }

            fileActivitiesTable.setItems(FXCollections.observableArrayList(records));

            System.out.println("已加载 " + records.size() + " 条文件活动记录到监控表格");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("加载文件活动记录失败：" + e.getMessage());
        }
    }

    private void initializeSearchTab() {
        // 初始化搜索标签页
    }

    private void initializeStatusBar() {
        // 初始化状态栏
    }

    private void loadStatistics() {
        // 加载统计数据
    }

    /**
     *  加载最近活动
     */
    private void loadRecentActivities() {
        try {
            if (fileActivityDAO == null) {
                System.err.println("错误：fileActivityDAO 尚未注入");
                return;
            }

            List<com.smartfilemanager.model.domain.FileActivity> activities = fileActivityDAO.getRecentActivities(10);

            ObservableList<ActivityRecord> records = FXCollections.observableArrayList();
            for (com.smartfilemanager.model.domain.FileActivity activity : activities) {
                ActivityRecord record = new ActivityRecord();
                record.setTime(activity.getCreatedAt() != null ? activity.getCreatedAt().toString() : "未知时间");
                record.setOperation(activity.getActivityType() != null ? activity.getActivityType().toString() : "未知");
                record.setFileName(activity.getFilePath() != null ? activity.getFilePath() : "未知路径");
                record.setStatus("完成");
                records.add(record);
            }

            recentActivitiesTable.setItems(FXCollections.observableArrayList(records));

            System.out.println("已加载 " + records.size() + " 条最近活动记录");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("加载最近活动记录失败：" + e.getMessage());
        }
    }

    private void loadRules() {
        // 加载规则
        try {
            if (organizeRuleDAO == null) {
                System.err.println("错误：organizeRuleDAO 尚未注入");
                return;
            }

            List<OrganizeRule> rules = organizeRuleDAO.getAllRules();
            ObservableList<OrganizeRule> observableRules = FXCollections.observableArrayList(rules);

            rulesTable.setItems(observableRules);

            // 更新规则计数
            if (totalRulesCountLabel != null) {
                totalRulesCountLabel.setText(String.valueOf(rules.size()));
            }
            if (totalRulesLabel != null) {
                totalRulesLabel.setText(String.valueOf(rules.size()));
            }
            if (activeRuleCountLabel != null) {
                long activeCount = rules.stream()
                        .filter(OrganizeRule::getEnabled)
                        .count();
                activeRuleCountLabel.setText(String.valueOf(activeCount));
            }

            System.out.println("已加载 " + rules.size() + " 个规则");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("加载规则失败：" + e.getMessage());
        }
    }


    private void loadMonitorFolders() {
        // 加载监控文件夹
        // 建议保留空值检查以便调试
        if (fileMonitorService == null) {
            System.err.println("错误：fileMonitorService 尚未注入");
            return;
        }

        try {
            // 1. 从 Service 获取数据
            List<MonitorFolders> folders = fileMonitorService.getAllMonitorFolders();

            // 2. 提取路径字符串 (ListView 默认显示 String，或者你可以自定义 CellFactory 显示对象)
            ObservableList<String> items = FXCollections.observableArrayList(
                    folders.stream()
                            .map(MonitorFolders::getFolderPath)
                            .collect(Collectors.toList())
            );

            // 3. 绑定到 ListView
            monitorFoldersList.setItems(items);

            // 更新左上角的状态计数
            if (monitorFolderCountLabel != null) {
                monitorFolderCountLabel.setText(String.valueOf(items.size()));
                monitoredFoldersLabel.setText(String.valueOf(items.size()));
            }

            System.out.println("已加载 " + items.size() + " 个监控文件夹");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("加载监控文件夹失败：" + e.getMessage());
            // 可以在 UI 上显示错误提示
        }
    }

    private void loadSystemStatus() {
        // 加载系统状态
    }

    public void handleToggleMonitoring(ActionEvent actionEvent) throws IOException {
        //开始监控
        //文件监控页面中的开始监控
        handleStartMonitoring();
    }

    /**
     * 注入 FileMonitorService（由 SmartFileManagerApp 调用）
     */
    public void setFileMonitorService(FileMonitorService fileMonitorService) {
        this.fileMonitorService = fileMonitorService;
        if (this.fileMonitorService != null) {
            System.out.println("FileMonitorService 注入成功，开始加载初始数据...");
            registerActivityListener();
            loadInitialData();
            initializeFileActivitiesTable();
        }
    }

    private void initializeFileActivitiesTable() {
        if (fileActivitiesTable == null) {
            System.err.println("错误：fileActivitiesTable 为 null");
            return;
        }

        if (fileActivitiesTable.getColumns().isEmpty()) {
            TableColumn<ActivityRecord, String> timeColumn = new TableColumn<>("时间");
            timeColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("time"));
            timeColumn.setPrefWidth(120);

            TableColumn<ActivityRecord, String> eventColumn = new TableColumn<>("事件");
            eventColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("operation"));
            eventColumn.setPrefWidth(100);

            TableColumn<ActivityRecord, String> fileColumn = new TableColumn<>("文件");
            fileColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("fileName"));
            fileColumn.setPrefWidth(250);

            TableColumn<ActivityRecord, String> actionColumn = new TableColumn<>("操作");
            actionColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("status"));
            actionColumn.setPrefWidth(100);

            fileActivitiesTable.getColumns().addAll(timeColumn, eventColumn, fileColumn, actionColumn);
        }

        loadFileActivities();
    }

    public void setFileActivityDAO(FileActivityDAO fileActivityDAO) {
        this.fileActivityDAO = fileActivityDAO;
        System.out.println("FileActivityDAO 注入成功");
    }

    /**
     * 注入 MonitorFoldersDAO（由 SmartFileManagerApp 调用）
     */
    public void setMonitorFoldersDAO(MonitorFoldersDAO monitorFoldersDAO) {
        this.monitorFoldersDAO = monitorFoldersDAO;
        System.out.println("MonitorFoldersDAO 注入成功");
    }

    /**
     * 注入 OrganizeRuleDAO（由 SmartFileManagerApp 调用）
     */
    public void setOrganizeRuleDAO(com.smartfilemanager.dao.OrganizeRuleDAO organizeRuleDAO) {
        this.organizeRuleDAO = organizeRuleDAO;
        System.out.println("OrganizeRuleDAO 注入成功");
    }

    /**
     * 注入 FileRecordDAO（由 SmartFileManagerApp 调用）
     */
    public void setFileRecordDAO(com.smartfilemanager.dao.FileRecordDAO fileRecordDAO) {
        this.fileRecordDAO = fileRecordDAO;
        System.out.println("FileRecordDAO 注入成功");
    }

    public void handleRefreshFileList(ActionEvent actionEvent) {
    }

    // ========== 数据模型类 ==========


//    public static class Rule {
//        // 规则数据模型
//    }

    public static class FileActivity {
        // 文件活动数据模型
    }

    public static class SearchResult {
        // 搜索结果数据模型
    }
}