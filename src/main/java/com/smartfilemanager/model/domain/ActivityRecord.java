package com.smartfilemanager.model.domain;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ActivityRecord {
    private final StringProperty time = new SimpleStringProperty();
    private final StringProperty operation = new SimpleStringProperty();
    private final StringProperty fileName = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();

    public ActivityRecord() {
    }

    public ActivityRecord(String time, String operation, String fileName, String status) {
        this.time.set(time);
        this.operation.set(operation);
        this.fileName.set(fileName);
        this.status.set(status);
    }

    public String getTime() {
        return time.get();
    }

    public void setTime(String time) {
        this.time.set(time);
    }

    public StringProperty timeProperty() {
        return time;
    }

    public String getOperation() {
        return operation.get();
    }

    public void setOperation(String operation) {
        this.operation.set(operation);
    }

    public StringProperty operationProperty() {
        return operation;
    }

    public String getFileName() {
        return fileName.get();
    }

    public void setFileName(String fileName) {
        this.fileName.set(fileName);
    }

    public StringProperty fileNameProperty() {
        return fileName;
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    public StringProperty statusProperty() {
        return status;
    }
}
