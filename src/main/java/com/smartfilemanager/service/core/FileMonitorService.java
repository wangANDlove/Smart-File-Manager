package com.smartfilemanager.service.core;

import com.smartfilemanager.config.AppConfig;
import com.smartfilemanager.dao.FileActivityDAO;
import com.smartfilemanager.dao.FileRecordDAO;
import com.smartfilemanager.dao.MonitorFoldersDAO;
import com.smartfilemanager.model.domain.ActivityType;
import com.smartfilemanager.model.domain.FileActivity;
import com.smartfilemanager.model.domain.FileRecord;
import com.smartfilemanager.model.domain.MonitorFolders;
import com.smartfilemanager.utils.FileIdFetcher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class FileMonitorService implements InitializingBean {
    private WatchService watcher;//创建WatchService实例
    @Autowired
    private AppConfig appConfig;
    private List<Path> monitoredDirectories;
    private Thread moniterThread;
    private boolean isRunning = false;
    private java.util.Map<WatchKey, Path> watchKeyToPathMap;


    @Autowired
    private FileRecordDAO fileRecordDAO;
    @Autowired
    private MonitorFoldersDAO monitorFoldersDAO = new MonitorFoldersDAO();
    @Autowired
    private FileActivityDAO fileActivityDAO;

    @Autowired
    private com.smartfilemanager.service.rule.FileOrganizeService fileOrganizeService;


    private final List<FileActivityListener> activityListeners = new CopyOnWriteArrayList<>();

    public FileMonitorService() throws IOException {
        // 初始化WatchService实例
        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    @Override
    public void afterPropertiesSet(){
        // 初始化路径
        updateDirectory();
//        moniterThread = new Thread(this::startMonitoring);
//        moniterThread.setDaemon( true);
//        moniterThread.setName("FileMonitorService");
//        moniterThread.start();

    }

    /**
     * 更新监控目录
     */
    public void updateDirectory() {
        monitoredDirectories=new ArrayList<>();
        try {
            // 从数据库获取所有监控文件夹
            List<MonitorFolders> monitorFolders = monitorFoldersDAO.getAllMonitorFolders();

            if (monitorFolders != null && !monitorFolders.isEmpty()) {
                for (MonitorFolders folder : monitorFolders) {
                    Path path = Paths.get(folder.getFolderPath());
                    if (Files.exists(path) && Files.isDirectory(path)) {
                        monitoredDirectories.add(path);
                        System.out.println("加载监控目录：" + path);
                    } else {
                        System.out.println("监控目录不存在或不是有效目录：" + path);
                    }
                }
            }

            // 如果数据库中没有配置，则使用默认配置
            if (monitoredDirectories.isEmpty()) {
                if (appConfig != null && appConfig.getWatchedPath() != null) {
                    Path defaultDir = Paths.get(appConfig.getWatchedPath());
                    monitoredDirectories.add(defaultDir);
                    System.out.println("使用默认监控目录：" + defaultDir);
                } else {
                    Path userHome = Paths.get(System.getProperty("user.home"));
                    monitoredDirectories.add(userHome);
                    System.out.println("使用用户主目录作为默认监控目录：" + userHome);
                }
            }

            System.out.println("共加载 " + monitoredDirectories.size() + " 个监控目录");

        } catch (SQLException e) {
            throw new RuntimeException("从数据库加载监控目录失败", e);
        }
    }

    /**
     * 开始监控
     */
    public void startMonitoring() {
        if(isRunning){
            System.out.println("FileMonitorService is already running.");
            return;
        }
        updateDirectory();//更新监控目录
        // 扫描所有监控目录并保存文件信息到数据库
        for (Path directory : monitoredDirectories) {
            scanAndSaveFiles(directory);
        }
        watchKeyToPathMap = new java.util.HashMap<>();

        // 注册所有监控目录到 WatchService
        for (Path directory : monitoredDirectories) {
            try {
                WatchKey watchKey = directory.register(watcher,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
                watchKeyToPathMap.put(watchKey, directory);
                System.out.println("已注册监控目录：" + directory);
            } catch (IOException e) {
                System.out.println("注册监控目录失败：" + directory + ", 错误：" + e.getMessage());
            }
        }
        isRunning = true;
        moniterThread = new Thread(this::monitorLoop);
        moniterThread.setDaemon(true);
        moniterThread.setName("FileMonitorService");
        moniterThread.start();
        System.out.println("监控服务已启动");

    }

    /**
     * 监控循环
     */
    private void monitorLoop() {
        while (isRunning && !Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            Path directory = watchKeyToPathMap.get(key);

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                Path fileName = (Path) event.context();
                Path fullPath = directory != null ? directory.resolve(fileName) : null;

                System.out.println("监控目录: " + directory);
                System.out.println("事件类型: " + kind.name());
                System.out.println("文件名称: " + fileName);
                System.out.println("完整路径: " + fullPath);
                String fileId = String.valueOf(FileIdFetcher.getFileId(fullPath.toString()));

                String folderId = monitorFoldersDAO.getFolderIdByPath(directory.toString());
                ActivityType activityType =  mapToActivityType(kind);

                boolean isFolder = Files.isDirectory(fullPath);
                FileActivity fileActivity = new FileActivity(
                        fileId,
                        fullPath.toString(),
                        fileName.toString(),
                        activityType,Long.parseLong(folderId));
                //对监控到的文件活动进行处理
                handleFileActivity(fileActivity);
                System.out.println(kind.name() + ": " + fileName);
                notifyActivityListeners(fileActivity);

                if (!key.reset()) {
                    watchKeyToPathMap.remove(key);
                    break;
                }
            }
        }
    }

    /**
     * 处理文件活动
     */
    private void handleFileActivity(FileActivity fileActivity) {
        try {
            System.out.println("处理文件活动: " + fileActivity.getActivityType() + " - " + fileActivity.getFilePath());

            switch (fileActivity.getActivityType()) {
                case CREATE:
                    handleFileCreate(fileActivity);
                    break;
                case MODIFY:
                    handleFileModify(fileActivity);
                    break;
                case DELETE:
                    handleFileDelete(fileActivity);
                    break;
                default:
                    System.out.println("未知的活动类型: " + fileActivity.getActivityType());
                    break;
            }
            // 应用文件整理规则（仅对新创建的文件）
            if (fileActivity.getActivityType() == ActivityType.CREATE) {
                fileOrganizeService.processFileWithRules(fileActivity);
            }
        } catch (Exception e) {
            System.err.println("处理文件活动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理文件删除事件
     */
    private void handleFileDelete(FileActivity fileActivity) {
        FileRecord fileRecord = new FileRecord(
                fileActivity.getFilePath(),
                fileActivity.getFileName(),
                Files.isDirectory(Path.of(fileActivity.getFilePath())),
                fileActivity.getFileId(),
                fileActivity.getFolderId()
        );
        fileRecordDAO.deleteFileRecord(fileRecord);
        try {
            fileActivityDAO.insertFileActivity(fileActivity);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 处理文件修改事件
     */
    private void handleFileModify(FileActivity fileActivity) {
        //文件修改事件和文件创建事件处理逻辑一样，这里只处理文件创建事件
        //因为sql语句是insert or replace into
        handleFileCreate(fileActivity);
    }

    /**
     * 处理文件创建事件
     */
    private void handleFileCreate(FileActivity fileActivity) {
        //保存文件信息到数据库，包括更新文件记录表和文件活动表
        FileRecord fileRecord = new FileRecord(
                fileActivity.getFilePath(),
                fileActivity.getFileName(),
                Files.isDirectory(Path.of(fileActivity.getFilePath())),
                fileActivity.getFileId(),
                fileActivity.getFolderId()
        );
        try {

            fileRecordDAO.insertFileRecord(fileRecord);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {

            fileActivityDAO.insertFileActivity(fileActivity);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 将 WatchEvent.Kind 映射为 ActivityType
     */
    private ActivityType mapToActivityType(WatchEvent.Kind<?> kind) {
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            return ActivityType.CREATE;
        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            return ActivityType.MODIFY;
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            return ActivityType.DELETE;
        } else {
            throw new IllegalArgumentException("未知的事件类型: " + kind);
        }
    }
    public void stopMonitoring() {
        if (!isRunning) {
            System.out.println("FileMonitorService is not running.");
            return;
        }
        isRunning = false;
        if(moniterThread!=null){
            moniterThread.interrupt();
            System.out.println("监控服务已停止");
        }

    }
    /**
     * 扫描目录并保存文件信息到数据库
     */
    private void scanAndSaveFiles(Path directory) {
        try {
            Long folderId = FileIdFetcher.getFileId(directory.toString()); // 获取目录ID
            if(folderId==null){
                System.out.println("目录ID获取失败：" + directory);
                return;
            }
            Files.walk(directory).
                    skip(1).//跳过根目录
                    forEach(file -> {
                long fileId = FileIdFetcher.getFileId(file.toString());

                if(fileId==-1){
                    System.out.println("文件ID获取失败：" + file);
                    return;
                }
                boolean isFolder = Files.isDirectory(file);
                FileRecord fileRecord = new FileRecord(file.toString(),
                        file.getFileName().toString(), isFolder, String.valueOf(fileId),folderId);
                try {
                    System.out.println("保存文件信息到数据库：" + fileRecord);
                    fileRecordDAO.insertFileRecord(fileRecord);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 添加监控目录到数据库
     *
     */
    public void addMonitorFolder(String folderPath) {
        try {
            Path folder = Paths.get(folderPath);
            if (!Files.exists(folder)) {
                System.out.println("目录不存在：" + folder);
                return;
            }
            if (!Files.isDirectory(folder)) {
                System.out.println("不是有效的目录：" + folder);
                return;
            }
            // 检查目录是否已经添加过
            if (isMonitorFolderExist(folder)) {
                System.out.println("目录已经添加过：" + folder);
                return;
            }
            // 添加目录到数据库
            try {
                System.out.println("添加目录到数据库：" + folderPath);
                //todo: 添加到监控文件夹表monitor_folders
                monitorFoldersDAO.insertMonitorFolder(new MonitorFolders(folderPath));
                System.out.println("测试点一添加目录到数据库成功：" + folder);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * 检查监控目录是否在数据库中
     */
    private boolean isMonitorFolderExist(Path folder) {
        return false;
    }

    public List<MonitorFolders> getAllMonitorFolders() {
        try {
            return monitorFoldersDAO.getAllMonitorFolders();
        } catch (SQLException e) {
            throw new RuntimeException("获取监控文件夹列表失败", e);
        }
    }

    /**
     * 删除监控目录
     */
    public void removeMonitorFolder(String selectedFolder) {
        if(selectedFolder== null|| selectedFolder.isEmpty()){
            System.out.println("文件夹为空");
            return;
        }
        int rowsAffected = monitorFoldersDAO.deleteMonitorFolderByPath(selectedFolder);
        if (rowsAffected > 0) {
            System.out.println("成功移除监控文件夹：" + selectedFolder);
        } else {
            System.out.println("未找到要移除的监控文件夹：" + selectedFolder);
        }
    }
    public void addActivityListener(FileActivityListener listener) {
        activityListeners.add(listener);
    }

    public void removeActivityListener(FileActivityListener listener) {
        activityListeners.remove(listener);
    }

    private void notifyActivityListeners(FileActivity activity) {
        for (FileActivityListener listener : activityListeners) {
            try {
                listener.onNewActivity(activity);
            } catch (Exception e) {
                System.err.println("通知监听器失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}
