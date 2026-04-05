package com.smartfilemanager.utils;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinBase;

import java.util.Arrays;
import java.util.List;

// 手动实现 BY_HANDLE_FILE_INFORMATION 结构体
@Structure.FieldOrder({
        "dwFileAttributes",
        "ftCreationTime",
        "ftLastAccessTime",
        "ftLastWriteTime",
        "dwVolumeSerialNumber",
        "nFileSizeHigh",
        "nFileSizeLow",
        "nNumberOfLinks",
        "nFileIndexHigh",
        "nFileIndexLow"
})
public class BY_HANDLE_FILE_INFORMATION extends Structure {
    public int dwFileAttributes;
    public WinBase.FILETIME ftCreationTime;
    public WinBase.FILETIME ftLastAccessTime;
    public WinBase.FILETIME ftLastWriteTime;
    public int dwVolumeSerialNumber;
    public int nFileSizeHigh;
    public int nFileSizeLow;
    public int nNumberOfLinks;
    public int nFileIndexHigh;
    public int nFileIndexLow;

    // 构造函数
    public BY_HANDLE_FILE_INFORMATION() {
        super();
    }

}
