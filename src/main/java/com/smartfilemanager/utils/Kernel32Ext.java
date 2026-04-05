package com.smartfilemanager.utils;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinNT;
public interface Kernel32Ext extends Library {
    Kernel32Ext INSTANCE = Native.load("kernel32", Kernel32Ext.class);

    boolean GetFileInformationByHandle(WinNT.HANDLE hFile, BY_HANDLE_FILE_INFORMATION lpFileInformation);
}
