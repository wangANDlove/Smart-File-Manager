package com.smartfilemanager.service.core;

import com.smartfilemanager.model.domain.FileActivity;

@FunctionalInterface
public interface FileActivityListener {
    void onNewActivity(FileActivity activity);
}
