package com.smartfilemanager.test;

import com.smartfilemanager.config.AIConfig;
import com.smartfilemanager.model.domain.FileRecord;
import com.smartfilemanager.model.domain.ClassificationResult;
import com.smartfilemanager.service.ai.XunfeiAIService;
import com.google.gson.Gson;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * AI分类功能测试类
 */
public class AIClassificationTest {

    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        System.out.println("========== AI智能分类功能测试 ==========\n");

        // 测试1：测试简单问答
        testSimpleChat();

        // 测试2：测试文件分类规则生成
        testFileClassification();

        // 测试3：测试异常情况
        testErrorHandling();
    }

    /**
     * 测试1：简单问答测试
     */
    private static void testSimpleChat() {
        System.out.println("【测试1】简单问答测试");
        System.out.println("----------------------------------------");

        try {
            // 创建AI服务实例
            XunfeiAIService aiService = createAIService();

            // 创建简单的测试提示词
            String prompt = "请用一句话介绍你自己";

            System.out.println("发送问题：" + prompt);
            System.out.println("\nAI回答：");

            // 调用API
            String response = aiService.callSparkAI(prompt);

            System.out.println("\n\n✓ 测试1完成：成功收到AI回复");
            System.out.println("完整响应长度：" + response.length() + " 字符\n\n");

        } catch (Exception e) {
            System.out.println("\n✗ 测试1失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试2：文件分类规则生成测试
     */
    private static void testFileClassification() {
        System.out.println("【测试2】文件分类规则生成测试");
        System.out.println("----------------------------------------");

        try {
            // 创建AI服务实例
            XunfeiAIService aiService = createAIService();

            // 模拟文件列表
            List<FileRecord> mockFiles = createMockFileList();

            System.out.println("准备测试文件数量：" + mockFiles.size());
            System.out.println("\n文件示例：");
            for (int i = 0; i < Math.min(5, mockFiles.size()); i++) {
                FileRecord file = mockFiles.get(i);
                System.out.println("  - " + file.getFileName() +
                    (Boolean.TRUE.equals(file.getIsFolder()) ? " [文件夹]" : ""));
            }
            if (mockFiles.size() > 5) {
                System.out.println("  ... 还有 " + (mockFiles.size() - 5) + " 个文件");
            }

            System.out.println("\n正在调用AI生成分类规则，请稍候...\n");

            // 调用AI生成分类规则
            String jsonResult = aiService.callSparkAI(buildPrompt(gson.toJson(mockFiles)));

            System.out.println("\n\nAI返回的JSON结果：");
            System.out.println(jsonResult);

            // 尝试解析JSON
            try {
                ClassificationResult result = gson.fromJson(jsonResult, ClassificationResult.class);

                System.out.println("\n\n✓ 测试2完成：JSON解析成功");
                System.out.println("\n解析结果：");
                System.out.println("总结：" + result.getSummary());

                if (result.getCategories() != null) {
                    System.out.println("\n分类数量：" + result.getCategories().size());
                    for (ClassificationResult.CategoryInfo category : result.getCategories()) {
                        System.out.println("\n  分类名称：" + category.getName());
                        System.out.println("  说明：" + category.getDescription());
                        System.out.println("  扩展名：" + category.getExtensions());
                        System.out.println("  预估文件数：" + category.getEstimatedCount());
                    }
                }

                if (result.getRecommendations() != null && !result.getRecommendations().isEmpty()) {
                    System.out.println("\n建议：");
                    for (String rec : result.getRecommendations()) {
                        System.out.println("  - " + rec);
                    }
                }

            } catch (Exception e) {
                System.out.println("\n⚠ JSON解析失败（可能是格式问题）：" + e.getMessage());
                System.out.println("但AI请求已成功，需要检查返回的JSON格式");
            }

        } catch (Exception e) {
            System.out.println("\n✗ 测试2失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试3：异常处理测试
     */
    private static void testErrorHandling() {
        System.out.println("【测试3】异常处理测试");
        System.out.println("----------------------------------------");

        try {
            XunfeiAIService aiService = createAIService();

            // 测试空列表
            System.out.println("测试场景1：空文件列表");
            List<FileRecord> emptyList = new ArrayList<>();
            String result1 = aiService.callSparkAI(buildPrompt(gson.toJson(emptyList)));
            System.out.println("✓ 空列表处理正常\n");

            // 测试超长提示词（应该被截断）
            System.out.println("测试场景2：超长文本处理");
            StringBuilder longText = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longText.append("测试文件").append(i).append(".txt ");
            }
            String result2 = aiService.callSparkAI("请总结以下内容：" + longText.toString());
            System.out.println("✓ 长文本处理正常，响应长度：" + result2.length() + "\n");

            System.out.println("✓ 测试3完成：异常处理正常\n\n");

        } catch (Exception e) {
            System.out.println("\n⚠ 异常处理测试出现预期外错误：" + e.getMessage());
        }
    }

    /**
     * 创建AI服务实例（手动配置）
     */
    private static XunfeiAIService createAIService() {
        XunfeiAIService aiService = new XunfeiAIService();

        // 从 application.yml 读取配置
        AIConfig config = loadConfigFromYaml();

        try {
            // 使用反射设置私有字段aiConfig
            java.lang.reflect.Field field = XunfeiAIService.class.getDeclaredField("aiConfig");
            field.setAccessible(true);
            field.set(aiService, config);
        } catch (Exception e) {
            throw new RuntimeException("无法设置AI配置", e);
        }

        return aiService;
    }

    /**
     * 从 application.yml 加载配置
     */
    private static AIConfig loadConfigFromYaml() {
        AIConfig config = new AIConfig();

        try {
            java.io.InputStream inputStream = AIClassificationTest.class.getClassLoader()
                    .getResourceAsStream("application.yml");

            if (inputStream == null) {
                System.err.println("错误：无法找到 application.yml");
                return getDefaultConfig();
            }

            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Map<String, Object> yamlMap = yaml.load(inputStream);

            Map<String, Object> aiConfig = (Map<String, Object>) yamlMap.get("ai");
            if (aiConfig == null) {
                System.err.println("错误：YAML中没有找到ai配置节点");
                return getDefaultConfig();
            }

            Map<String, Object> xunfeiConfig = (Map<String, Object>) aiConfig.get("xunfei");
            if (xunfeiConfig == null) {
                System.err.println("错误：YAML中没有找到xunfei配置节点");
                return getDefaultConfig();
            }

            // 【关键修改】使用 String.valueOf 安全获取配置，防止类型转换异常
            // 即使 YAML 将其解析为数字，这里也能正确转为字符串
            String appId = xunfeiConfig.get("app-id") != null ? String.valueOf(xunfeiConfig.get("app-id")) : "";
            String apiSecret = xunfeiConfig.get("api-secret") != null ? String.valueOf(xunfeiConfig.get("api-secret")) : "";
            String apiKey = xunfeiConfig.get("api-key") != null ? String.valueOf(xunfeiConfig.get("api-key")) : "";
            String hostUrl = xunfeiConfig.get("host-url") != null ? String.valueOf(xunfeiConfig.get("host-url")) : "https://spark-api.xf-yun.com/v3.5/chat";
            String domain = xunfeiConfig.get("domain") != null ? String.valueOf(xunfeiConfig.get("domain")) : "generalv3.5";

            config.setAppId(appId);
            config.setApiSecret(apiSecret);
            config.setApiKey(apiKey);
            config.setHostUrl(hostUrl);
            config.setDomain(domain);

            // 处理数值类型
            Object temperature = xunfeiConfig.get("temperature");
            config.setTemperature(temperature instanceof Number ? ((Number) temperature).doubleValue() : 0.5);

            Object maxTokens = xunfeiConfig.get("max-tokens");
            config.setMaxTokens(maxTokens instanceof Number ? ((Number) maxTokens).intValue() : 4096);

            // 验证配置是否读取成功
            if (appId.isEmpty() || apiSecret.isEmpty() || apiKey.isEmpty()) {
                System.err.println("错误：AI配置不完整，请检查 application.yml");
            } else {
                System.out.println("✓ 成功从 application.yml 加载AI配置");
                System.out.println("  APP ID: " + maskSensitiveInfo(appId));
                System.out.println("  Host URL: " + hostUrl);
                System.out.println("  Domain: " + domain);
            }

        } catch (Exception e) {
            System.err.println("错误：解析 application.yml 失败。错误：" + e.getMessage());
            e.printStackTrace();
            return getDefaultConfig();
        }

        return config;
    }
    /**
     * 获取默认配置
     */
    private static AIConfig getDefaultConfig() {
        AIConfig config = new AIConfig();
        config.setHostUrl("https://spark-api.xf-yun.com/v3.5/chat");
        config.setDomain("generalv3.5");
        config.setTemperature(0.5);
        config.setMaxTokens(4096);
        System.out.println("使用默认AI配置（请确保在application.yml中配置了正确的密钥）");
        return config;
    }
    /**
     * 脱敏显示敏感信息
     */
    private static String maskSensitiveInfo(String info) {
        if (info == null || info.isEmpty()) {
            return "(未配置)";
        }
        if (info.length() <= 8) {
            return "****";
        }
        return info.substring(0, 4) + "****" + info.substring(info.length() - 4);
    }

    /**
     * 创建模拟文件列表
     */
    private static List<FileRecord> createMockFileList() {
        List<FileRecord> files = new ArrayList<>();

        // 图片文件
        files.add(new FileRecord("C:/Users/test/Pictures/photo1.jpg", "photo1.jpg", false, "1001", 1L));
        files.add(new FileRecord("C:/Users/test/Pictures/photo2.png", "photo2.png", false, "1002", 1L));
        files.add(new FileRecord("C:/Users/test/Pictures/screenshot.bmp", "screenshot.bmp", false, "1003", 1L));
        files.add(new FileRecord("C:/Users/test/Pictures/icon.gif", "icon.gif", false, "1004", 1L));

        // 文档文件
        files.add(new FileRecord("C:/Users/test/Documents/report.docx", "report.docx", false, "2001", 1L));
        files.add(new FileRecord("C:/Users/test/Documents/thesis.pdf", "thesis.pdf", false, "2002", 1L));
        files.add(new FileRecord("C:/Users/test/Documents/notes.txt", "notes.txt", false, "2003", 1L));
        files.add(new FileRecord("C:/Users/test/Documents/data.xlsx", "data.xlsx", false, "2004", 1L));
        files.add(new FileRecord("C:/Users/test/Documents/presentation.pptx", "presentation.pptx", false, "2005", 1L));

        // 视频文件
        files.add(new FileRecord("C:/Users/test/Videos/movie.mp4", "movie.mp4", false, "3001", 1L));
        files.add(new FileRecord("C:/Users/test/Videos/clip.avi", "clip.avi", false, "3002", 1L));
        files.add(new FileRecord("C:/Users/test/Videos/tutorial.mkv", "tutorial.mkv", false, "3003", 1L));

        // 音频文件
        files.add(new FileRecord("C:/Users/test/Music/song1.mp3", "song1.mp3", false, "4001", 1L));
        files.add(new FileRecord("C:/Users/test/Music/song2.flac", "song2.flac", false, "4002", 1L));
        files.add(new FileRecord("C:/Users/test/Music/podcast.wav", "podcast.wav", false, "4003", 1L));

        // 代码文件
        files.add(new FileRecord("C:/Users/test/Projects/Main.java", "Main.java", false, "5001", 1L));
        files.add(new FileRecord("C:/Users/test/Projects/app.py", "app.py", false, "5002", 1L));
        files.add(new FileRecord("C:/Users/test/Projects/index.html", "index.html", false, "5003", 1L));
        files.add(new FileRecord("C:/Users/test/Projects/style.css", "style.css", false, "5004", 1L));
        files.add(new FileRecord("C:/Users/test/Projects/script.js", "script.js", false, "5005", 1L));

        // 压缩包
        files.add(new FileRecord("C:/Users/test/Downloads/archive.zip", "archive.zip", false, "6001", 1L));
        files.add(new FileRecord("C:/Users/test/Downloads/backup.rar", "backup.rar", false, "6002", 1L));
        files.add(new FileRecord("C:/Users/test/Downloads/files.7z", "files.7z", false, "6003", 1L));

        // 文件夹
        files.add(new FileRecord("C:/Users/test/Projects", "Projects", true, "7001", 1L));
        files.add(new FileRecord("C:/Users/test/Documents", "Documents", true, "7002", 1L));

        return files;
    }

    /**
     * 构建测试提示词
     */
    private static String buildPrompt(String fileInfo) {
        return String.format(
            "你是一个智能文件管理助手。根据以下文件信息，帮我生成合理的文件分类规则。\n\n" +
            "【文件信息】\n%s\n\n" +
            "【任务要求】\n" +
            "1. 分析文件类型分布（如：图片、文档、视频、音频、代码、压缩包等）\n" +
            "2. 建议分类文件夹名称\n" +
            "3. 为每个分类生成匹配规则（基于文件扩展名、文件名特征等）\n\n" +
            "【输出格式】\n" +
            "请严格按照以下JSON格式输出（不要包含其他解释文字）：\n" +
            "{\n" +
            "  \"categories\": [\n" +
            "    {\n" +
            "      \"name\": \"分类名称\",\n" +
            "      \"description\": \"分类说明\",\n" +
            "      \"extensions\": [\"扩展名1\", \"扩展名2\"],\n" +
            "      \"filePatterns\": [\"文件名匹配模式1\"],\n" +
            "      \"estimatedCount\": 10\n" +
            "    }\n" +
            "  ],\n" +
            "  \"summary\": \"总体分析总结\",\n" +
            "  \"recommendations\": [\"建议1\", \"建议2\"]\n" +
            "}",
            fileInfo
        );
    }
}
