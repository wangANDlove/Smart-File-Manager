package com.smartfilemanager.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import java.util.Arrays;
import java.util.List;


// ... existing code ...


public class FileIdFetcher {
    public static long getFileId(String filePath) {
        try {
            WinNT.HANDLE hFile = Kernel32.INSTANCE.CreateFile(
                    filePath,
                    Kernel32.GENERIC_READ,
                    Kernel32.FILE_SHARE_READ | Kernel32.FILE_SHARE_WRITE,
                    null,
                    Kernel32.OPEN_EXISTING,
                    Kernel32.FILE_FLAG_BACKUP_SEMANTICS, // 支持打开文件夹
                    null
            );

            if (hFile == WinBase.INVALID_HANDLE_VALUE) {
                System.err.println("无法打开文件/文件夹：" + filePath);
                return -1; // 返回 -1 表示失败
            }

            BY_HANDLE_FILE_INFORMATION fileInfo = new BY_HANDLE_FILE_INFORMATION();
            if (!Kernel32Ext.INSTANCE.GetFileInformationByHandle(hFile, fileInfo)) {
                Kernel32.INSTANCE.CloseHandle(hFile);
                System.err.println("无法获取文件信息：" + filePath);
                return -1;
            }

            Kernel32.INSTANCE.CloseHandle(hFile);
            return ((long) fileInfo.nFileIndexHigh << 32) | (fileInfo.nFileIndexLow & 0xFFFFFFFFL);

        } catch (Exception e) {
            System.err.println("获取文件 ID 失败：" + filePath + ", 错误：" + e.getMessage());
            return -1;
        }
    }


}

