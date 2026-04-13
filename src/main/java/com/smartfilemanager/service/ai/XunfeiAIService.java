package com.smartfilemanager.service.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smartfilemanager.config.AIConfig;
import com.smartfilemanager.model.domain.FileRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import okhttp3.*;

/**
 * 讯飞星火AI服务
 */
@Service
public class XunfeiAIService extends WebSocketListener {

    @Autowired
    private AIConfig aiConfig;

    private static final Gson gson = new Gson();
    private AtomicReference<StringBuilder> responseBuilder = new AtomicReference<>(new StringBuilder());
    private AtomicBoolean wsCloseFlag = new AtomicBoolean(false);
    private CountDownLatch latch = new CountDownLatch(1);
    private CountDownLatch connectionLatch = new CountDownLatch(1);
    private AtomicReference<Exception> errorRef = new AtomicReference<>();

    /**
     * 根据文件列表生成分类规则
     */
    public CompletableFuture<String> generateClassificationRules(List<FileRecord> files) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 构建文件信息摘要
                String fileInfo = buildFileInfoSummary(files);

                // 构建提示词
                String prompt = buildPrompt(fileInfo);

                // 调用AI API
                String response = callSparkAI(prompt);

                return response;
            } catch (Exception e) {
                errorRef.set(e);
                throw new RuntimeException("AI分类规则生成失败", e);
            }
        });
    }

    /**
     * 构建文件信息摘要
     */
    private String buildFileInfoSummary(List<FileRecord> files) {
        if (files == null || files.isEmpty()) {
            return "没有找到文件";
        }

        // 限制文件数量，避免token超限
        List<FileRecord> sampleFiles = files.size() > 100 ? files.subList(0, 100) : files;

        StringBuilder sb = new StringBuilder();
        sb.append("文件总数：").append(files.size()).append("\n");
        sb.append("示例文件信息：\n\n");

        int index = 1;
        for (FileRecord file : sampleFiles) {
            sb.append(index++).append(". ");
            sb.append("文件名: ").append(file.getFileName());
            sb.append(", 类型: ").append(Boolean.TRUE.equals(file.getIsFolder()) ? "文件夹" : "文件");

            // 获取文件扩展名和大小信息
            if (!Boolean.TRUE.equals(file.getIsFolder())) {
                String ext = getFileExtension(file.getFileName());
                sb.append(", 扩展名: ").append(ext.isEmpty() ? "无" : ext);

                try {
                    java.nio.file.Path path = java.nio.file.Paths.get(file.getFilePath());
                    long size = java.nio.file.Files.size(path);
                    sb.append(", 大小: ").append(formatFileSize(size));

                    // 获取修改时间
                    java.nio.file.attribute.BasicFileAttributes attrs =
                            java.nio.file.Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class);
                    java.time.LocalDateTime modTime = java.time.LocalDateTime.ofInstant(
                            attrs.lastModifiedTime().toInstant(), java.time.ZoneId.systemDefault());
                    sb.append(", 修改时间: ").append(modTime.format(
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                } catch (Exception e) {
                    // 忽略文件访问错误
                }
            }
            sb.append("\n");
        }

        if (files.size() > 100) {
            sb.append("\n... 还有 ").append(files.size() - 100).append(" 个文件");
        }

        return sb.toString();
    }

    /**
     * 构建AI提示词
     */
    private String buildPrompt(String fileInfo) {
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

    /**
     * 调用讯飞星火API（公开方法供测试使用）
     */
    public String callSparkAI(String prompt) throws Exception {
        // 重置状态
        responseBuilder.set(new StringBuilder());
        wsCloseFlag.set(false);
        latch = new CountDownLatch(1);
        connectionLatch = new CountDownLatch(1);
        errorRef.set(null);

        // 构建鉴权URL（getAuthUrl 已经处理了协议转换）
        String authUrl = getAuthUrl(aiConfig.getHostUrl(), aiConfig.getApiKey(), aiConfig.getApiSecret());

        System.out.println("正在连接AI服务: " + authUrl);

        // 创建WebSocket连接
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder().url(authUrl).build();
        WebSocket webSocket = client.newWebSocket(request, this);

        // 等待连接建立
        if (!connectionLatch.await(30, TimeUnit.SECONDS)) {
            throw new RuntimeException("WebSocket连接超时");
        }

        // 检查连接错误
        if (errorRef.get() != null) {
            throw new RuntimeException("WebSocket连接失败", errorRef.get());
        }

        // 发送请求
        sendRequest(webSocket, prompt);

        // 等待响应完成
        if (!latch.await(60, TimeUnit.SECONDS)) {
            throw new RuntimeException("等待AI响应超时");
        }

        // 检查响应错误
        if (errorRef.get() != null) {
            throw new RuntimeException("AI响应错误", errorRef.get());
        }

        webSocket.close(1000, "");

        String fullResponse = responseBuilder.get().toString();
        System.out.println("\nAI完整响应：\n" + fullResponse);

        return extractJsonFromResponse(fullResponse);
    }

    /**
     * 发送请求到AI
     */
    private void sendRequest(WebSocket webSocket, String prompt) {
        try {
            JsonObject requestJson = new JsonObject();

            // header参数
            JsonObject header = new JsonObject();
            header.addProperty("app_id", aiConfig.getAppId());
            header.addProperty("uid", UUID.randomUUID().toString().substring(0, 10));

            // parameter参数
            JsonObject parameter = new JsonObject();
            JsonObject chat = new JsonObject();
            chat.addProperty("domain", aiConfig.getDomain());
            chat.addProperty("temperature", aiConfig.getTemperature());
            chat.addProperty("max_tokens", aiConfig.getMaxTokens());
            parameter.add("chat", chat);

            // payload参数
            JsonObject payload = new JsonObject();
            JsonObject message = new JsonObject();

            // 构建消息
            com.google.gson.JsonArray textArray = new com.google.gson.JsonArray();

            // 用户消息
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", prompt);
            textArray.add(userMessage);

            message.add("text", textArray);
            payload.add("message", message);

            requestJson.add("header", header);
            requestJson.add("parameter", parameter);
            requestJson.add("payload", payload);

            System.out.println("发送AI请求...");
            webSocket.send(requestJson.toString());
        } catch (Exception e) {
            errorRef.set(e);
            e.printStackTrace();
        }
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        System.out.println("AI WebSocket连接已建立");
        connectionLatch.countDown();
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
//        // 调试模式：打印完整响应（生产环境可注释）
//        System.out.println("\n=== AI原始响应 ===");
//        System.out.println(text);
//        System.out.println("==================\n");

        try {
            JsonObject json = JsonParser.parseString(text).getAsJsonObject();

            // 检查错误码
            JsonObject header = json.getAsJsonObject("header");
            int code = header.get("code").getAsInt();
            if (code != 0) {
                String sid = header.has("sid") ? header.get("sid").getAsString() : "unknown";
                String errorMsg = "AI请求失败，错误码：" + code + "，sid：" + sid;
                System.err.println(errorMsg);
                errorRef.set(new RuntimeException(errorMsg));
                latch.countDown();
                return;
            }

            // 提取响应文本
            JsonObject payload = json.getAsJsonObject("payload");
            if (payload == null) {
                System.err.println("警告：payload为空");
                return;
            }

            JsonObject choices = payload.getAsJsonObject("choices");
            if (choices == null) {
                System.err.println("警告：choices为空");
                return;
            }

            com.google.gson.JsonArray textArray = choices.getAsJsonArray("text");
            if (textArray != null && textArray.size() > 0) {
                JsonObject textObj = textArray.get(0).getAsJsonObject();

                // X2模型可能有reasoning_content和content两个字段
                StringBuilder contentBuilder = new StringBuilder();

                // 先获取推理内容（如果有）
                if (textObj.has("reasoning_content") && !textObj.get("reasoning_content").isJsonNull()) {
                    String reasoningContent = textObj.get("reasoning_content").getAsString();
                    if (!reasoningContent.isEmpty()) {
                        contentBuilder.append(reasoningContent);
                    }
                }

                // 再获取主要内容
                if (textObj.has("content") && !textObj.get("content").isJsonNull()) {
                    String content = textObj.get("content").getAsString();
                    contentBuilder.append(content);
                }

                String finalContent = contentBuilder.toString();
                if (!finalContent.isEmpty()) {
                    responseBuilder.get().append(finalContent);
                    System.out.print(finalContent);
                }
            }

            // 检查是否结束
            int status = header.get("status").getAsInt();
            if (status == 2) {
                System.out.println("\nAI响应完成");
                wsCloseFlag.set(true);
                latch.countDown();
            }
        } catch (Exception e) {
            errorRef.set(e);
            e.printStackTrace();
            latch.countDown();
        }
    }


    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        System.err.println("WebSocket连接失败: " + t.getMessage());
        errorRef.set(new RuntimeException("WebSocket连接失败", t));
        connectionLatch.countDown();
        latch.countDown();
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        System.out.println("WebSocket连接已关闭");
    }

    /**
     * 鉴权方法
     */
    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        // 将 wss:// 替换为 https:// 用于 URL 解析
        String httpUrl = hostUrl.replace("wss://", "https://").replace("ws://", "http://");

        URL url = new URL(httpUrl);

        // 时间
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());

        // 拼接签名原文
        String preStr = "host: " + url.getHost() + "\n" +
                "date: " + date + "\n" +
                "GET " + url.getPath() + " HTTP/1.1";

        // SHA256加密
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));

        // Base64加密（关键：移除换行符）
        String sha = Base64.getEncoder().withoutPadding().encodeToString(hexDigits);

        // 拼接authorization
        String authorization = String.format(
                "api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", sha
        );
        // 再次 Base64 编码整个 authorization 字符串（同样移除换行符）
        String authorizationBase64 = Base64.getEncoder().withoutPadding().encodeToString(
                authorization.getBytes(StandardCharsets.UTF_8));

        // 拼接URL（使用原始的 hostUrl）
        okhttp3.HttpUrl parsedUrl = okhttp3.HttpUrl.parse(httpUrl);
        if (parsedUrl == null) {
            throw new IllegalArgumentException("无法解析URL: " + httpUrl);
        }

        okhttp3.HttpUrl httpUrlObj = parsedUrl.newBuilder()
                .addQueryParameter("authorization", Base64.getEncoder().encodeToString(
                        authorization.getBytes(StandardCharsets.UTF_8)))
                .addQueryParameter("date", date)
                .addQueryParameter("host", url.getHost())
                .build();

        // 将 https:// 替换回 wss://
        String resultUrl = httpUrlObj.toString().replace("https://", "wss://").replace("http://", "ws://");

        return resultUrl;
    }

    /**
     * 从响应中提取JSON
     */
    private String extractJsonFromResponse(String response) {
        // 尝试找到JSON块
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd + 1);
        }

        return response;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }

    /**
     * 格式化文件大小
     */
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
}

