package com.smartfilemanager.controller;

import com.smartfilemanager.dao.MonitorFoldersDAO;
import com.smartfilemanager.model.domain.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;


import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class RuleEditController implements Initializable {

    @FXML private TextField ruleNameField;
    @FXML private TextArea ruleDescriptionArea;
    @FXML private ComboBox<String> watchFolderCombo;
    @FXML private Spinner<Integer> prioritySpinner;
    @FXML private RadioButton andLogicRadio;
    @FXML private RadioButton orLogicRadio;
    @FXML private ToggleGroup conditionLogicGroup;
    @FXML private CheckBox enabledCheckBox;

    @FXML private TableView<ConditionRow> conditionsTable;
    @FXML private TableColumn<ConditionRow, String> conditionTypeColumn;
    @FXML private TableColumn<ConditionRow, String> operatorColumn;
    @FXML private TableColumn<ConditionRow, String> valueColumn;
    @FXML private TableColumn<ConditionRow, Void> conditionActionColumn;

    @FXML private TableView<ActionRow> actionsTable;
    @FXML private TableColumn<ActionRow, String> actionTypeColumn;
    @FXML private TableColumn<ActionRow, String> targetColumn;

    @FXML private Label previewRuleName;
    @FXML private Label previewDescription;
    @FXML private Label previewWatchFolder;
    @FXML private ListView<String> previewConditionsList;
    @FXML private ListView<String> previewActionsList;



    private MonitorFoldersDAO monitorFoldersDAO;

    private ObservableList<ConditionRow> conditions = FXCollections.observableArrayList();
    private ObservableList<ActionRow> actions = FXCollections.observableArrayList();

    private OrganizeRule editingRule;
    private boolean isEditMode = false;

    private java.util.Map<String, Long> folderPathToIdMap = new java.util.HashMap<>();

    /**
     * 设置依赖（由 MainController 调用）
     */
    public void setMonitorFoldersDAO(MonitorFoldersDAO monitorFoldersDAO) {
        this.monitorFoldersDAO = monitorFoldersDAO;
        // 依赖注入后立即加载监控文件夹
        loadWatchFolders();
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSpinners();
        setupTables();
        //loadWatchFolders();
        setupPreviewListeners();
    }


    private void setupSpinners() {
        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0);
        prioritySpinner.setValueFactory(factory);
    }

    private void setupTables() {
        // 条件表格
        conditionsTable.setItems(conditions);
        conditionsTable.setEditable(true);

        conditionTypeColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("conditionType"));
        conditionTypeColumn.setCellFactory(ComboBoxTableCell.forTableColumn(
            ConditionType.FILE_NAME.toString(),
            ConditionType.FILE_EXTENSION.toString(),
            ConditionType.FILE_SIZE.toString(),
            ConditionType.FILE_DATE.toString(),
            ConditionType.FILE_PATH.toString()
        ));
        conditionTypeColumn.setOnEditCommit(event -> {
            ConditionRow row = event.getRowValue();
            row.setConditionType(event.getNewValue());
            updateConditionsPreview();
        });

        operatorColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("operator"));
        operatorColumn.setCellFactory(ComboBoxTableCell.forTableColumn(
            ConditionOperator.CONTAINS.toString(),
            ConditionOperator.EQUALS.toString(),
            ConditionOperator.GREATER_THAN.toString(),
            ConditionOperator.LESS_THAN.toString(),
            ConditionOperator.IN.toString(),
            ConditionOperator.STARTS_WITH.toString(),
            ConditionOperator.ENDS_WITH.toString(),
            ConditionOperator.MATCHES_REGEX.toString()
        ));
        operatorColumn.setOnEditCommit(event -> {
            ConditionRow row = event.getRowValue();
            row.setOperator(event.getNewValue());
            updateConditionsPreview();
        });

        valueColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("value"));
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        valueColumn.setOnEditCommit(event -> {
            ConditionRow row = event.getRowValue();
            String newValue = event.getNewValue() != null ? event.getNewValue().trim() : "";
            row.setValue(newValue);
            System.out.println("条件值已更新: " + newValue);
            updateConditionsPreview();
        });
        // 设置条件删除按钮列
        conditionActionColumn.setCellFactory(param -> new TableCell<ConditionRow, Void>() {
            private final Button deleteButton = new Button("❌");

            {
                deleteButton.setStyle("-fx-background-color: #ff5252; -fx-text-fill: white; -fx-cursor: hand;");
                deleteButton.setOnAction(event -> {
                    ConditionRow row = getTableView().getItems().get(getIndex());
                    handleDeleteCondition(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteButton);
                }
            }
        });

        // 动作表格
        actionsTable.setItems(actions);
        actionsTable.setEditable(true);

        actionTypeColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("type"));
        actionTypeColumn.setCellFactory(ComboBoxTableCell.forTableColumn(
                ActionType.MOVE.toString(),
                ActionType.COPY.toString(),
                ActionType.RENAME.toString(),
                ActionType.DELETE.toString()
        ));
        actionTypeColumn.setOnEditCommit(event -> {
            ActionRow row = event.getRowValue();
            row.setType(event.getNewValue());
            updateActionsPreview();
        });

        targetColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("target"));
        targetColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        targetColumn.setOnEditCommit(event -> {
            ActionRow row = event.getRowValue();
            String newValue = event.getNewValue() != null ? event.getNewValue().trim() : "";
            row.setTarget(newValue);
            System.out.println("动作目标已更新: " + newValue);
            updateActionsPreview();
        });
    }

    private void loadWatchFolders() {
        try {
            List<MonitorFolders> folders = monitorFoldersDAO.getAllMonitorFolders();
            List<String> folderPaths = folders.stream()
                .map(MonitorFolders::getFolderPath)
                .collect(Collectors.toList());

            folderPathToIdMap.clear();
            for (MonitorFolders folder : folders) {
                folderPathToIdMap.put(folder.getFolderPath(), folder.getId());
            }

            watchFolderCombo.getItems().addAll(folderPaths);

            if (!folderPaths.isEmpty()) {
                watchFolderCombo.getSelectionModel().selectFirst();
            }
        } catch (SQLException e) {
            showError("加载监控文件夹失败", e.getMessage());
        }
    }

    private void setupPreviewListeners() {
        ruleNameField.textProperty().addListener((obs, old, newVal) -> {
            previewRuleName.setText(newVal.isEmpty() ? "-" : newVal);
        });

        ruleDescriptionArea.textProperty().addListener((obs, old, newVal) -> {
            previewDescription.setText(newVal.isEmpty() ? "-" : newVal);
        });

        watchFolderCombo.valueProperty().addListener((obs, old, newVal) -> {
            previewWatchFolder.setText(newVal == null ? "-" : newVal);
        });

        conditions.addListener((javafx.collections.ListChangeListener.Change<? extends ConditionRow> c) -> {
            updateConditionsPreview();
        });

        actions.addListener((javafx.collections.ListChangeListener.Change<? extends ActionRow> c) -> {
            updateActionsPreview();
        });
    }

    @FXML
    private void handleAddCondition() {
        ConditionRow newRow = new ConditionRow(
            ConditionType.FILE_EXTENSION,
            ConditionOperator.EQUALS,
            ""
        );
        conditions.add(newRow);
        conditionsTable.scrollTo(newRow);
    }

    @FXML
    private void handleDeleteCondition(ConditionRow row) {
        conditions.remove(row);
    }

    @FXML
    private void handleAddAction() {
        ActionRow newRow = new ActionRow(
            ActionType.MOVE,
            ""
        );
        actions.add(newRow);
        actionsTable.scrollTo(newRow);
    }

    @FXML
    private void handleDeleteAction(ActionRow row) {
        actions.remove(row);
    }

    @FXML
    private void handleRefreshPreview() {
        updateConditionsPreview();
        updateActionsPreview();
    }

    private void updateConditionsPreview() {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < conditions.size(); i++) {
            ConditionRow row = conditions.get(i);
            String logic = (i == 0) ? "如果" : (andLogicRadio.isSelected() ? "并且" : "或者");
            items.add(String.format("%s [%s] %s %s",
                logic,
                row.getConditionType(),
                row.getOperator(),
                row.getValue()));
        }

        if (items.isEmpty()) {
            items.add("未设置条件（将匹配所有文件）");
        }

        previewConditionsList.setItems(FXCollections.observableArrayList(items));
    }

    private void updateActionsPreview() {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < actions.size(); i++) {
            ActionRow row = actions.get(i);
            items.add(String.format("%d. %s → %s",
                i + 1,
                row.getType(),
                row.getTarget().isEmpty() ? "（未设置）" : row.getTarget()));
        }

        if (items.isEmpty()) {
            items.add("未设置动作");
        }

        previewActionsList.setItems(FXCollections.observableArrayList(items));
    }

    @FXML
    public void handleTestRule() {
        if (!validateInputs()) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("规则测试");
        alert.setHeaderText("规则配置验证通过 ✓");
        alert.setContentText("规则配置看起来没问题！\n\n建议：在实际使用前，先在测试文件夹中验证规则效果。");
        alert.showAndWait();
    }

    private boolean validateInputs() {
        if (ruleNameField.getText().trim().isEmpty()) {
            showError("验证失败", "请输入规则名称");
            ruleNameField.requestFocus();
            return false;
        }

        if (watchFolderCombo.getValue() == null || watchFolderCombo.getValue().isEmpty()) {
            showError("验证失败", "请选择监控文件夹");
            return false;
        }

        if (conditions.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText("未设置任何条件");
            alert.setContentText("这将匹配所有文件，确定要继续吗？");

            var result = alert.showAndWait();
            return result.isPresent() && result.get().getButtonData() == javafx.scene.control.ButtonBar.ButtonData.OK_DONE;
        }

        if (actions.isEmpty()) {
            showError("验证失败", "请至少添加一个动作");
            return false;
        }

        return true;
    }

    public OrganizeRule getRule() {
        if (!validateInputs()) {
            return null;
        }
        String selectedFolderPath = watchFolderCombo.getValue();
        Long watchFolderId = folderPathToIdMap.get(selectedFolderPath);

        if (watchFolderId == null) {
            showError("验证失败", "无法获取监控文件夹ID");
            return null;
        }

        OrganizeRule rule = new OrganizeRule();
        rule.setName(ruleNameField.getText().trim());
        rule.setDescription(ruleDescriptionArea.getText().trim());
        rule.setWatchFolderId(watchFolderId);
        rule.setPriority(prioritySpinner.getValue());
        rule.setEnabled(enabledCheckBox.isSelected());
        rule.setConditionLogic(andLogicRadio.isSelected() ? "AND" : "OR");

        // 转换条件
        List<RuleCondition> ruleConditions = new ArrayList<>();
        for (int i = 0; i < conditions.size(); i++) {
            ConditionRow row = conditions.get(i);
            RuleCondition condition = new RuleCondition();
            condition.setConditionType(ConditionType.valueOf(row.getConditionType()));
            condition.setOperator(ConditionOperator.valueOf(row.getOperator()));
            condition.setValue(row.getValue());
            condition.setSortOrder(i);
            ruleConditions.add(condition);
        }
        rule.setConditions(ruleConditions);

        // 转换动作
        List<RuleAction> ruleActions = new ArrayList<>();
        for (ActionRow row : actions) {
            RuleAction action = new RuleAction();
            action.setType(ActionType.valueOf(row.getType()));
            action.setTarget(row.getTarget());
            ruleActions.add(action);
        }
        rule.setActions(ruleActions);

        return rule;
    }

    public void setRule(OrganizeRule rule) {
        this.editingRule = rule;
        this.isEditMode = true;

        ruleNameField.setText(rule.getName());
        ruleDescriptionArea.setText(rule.getDescription());
        MonitorFolders folder = monitorFoldersDAO.getFolderById(rule.getWatchFolderId());
        if (folder != null) {
            watchFolderCombo.setValue(folder.getFolderPath());
        }
        prioritySpinner.getValueFactory().setValue(rule.getPriority());
        enabledCheckBox.setSelected(rule.getEnabled());

        if ("OR".equalsIgnoreCase(rule.getConditionLogic())) {
            orLogicRadio.setSelected(true);
        } else {
            andLogicRadio.setSelected(true);
        }

        // 加载条件
        if (rule.getConditions() != null) {
            for (RuleCondition condition : rule.getConditions()) {
                conditions.add(new ConditionRow(
                    condition.getConditionType(),
                    condition.getOperator(),
                    condition.getValue()
                ));
            }
        }

        // 加载动作
        if (rule.getActions() != null) {
            for (RuleAction action : rule.getActions()) {
                actions.add(new ActionRow(
                    action.getType(),
                    action.getTarget()
                ));
            }
        }

        updateConditionsPreview();
        updateActionsPreview();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // 数据行类
    public static class ConditionRow {
        private String conditionType;
        private String operator;
        private String value;

        public ConditionRow(ConditionType type, ConditionOperator op, String val) {
            this.conditionType = type.toString();
            this.operator = op.toString();
            this.value = val;
        }

        public String getConditionType() { return conditionType; }
        public void setConditionType(String type) { this.conditionType = type; }

        public String getOperator() { return operator; }
        public void setOperator(String op) { this.operator = op; }

        public String getValue() { return value; }
        public void setValue(String val) { this.value = val; }
    }

    public static class ActionRow {
        private String type;
        private String target;

        public ActionRow(ActionType type, String target) {
            this.type = type.toString();
            this.target = target;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
    }
}

