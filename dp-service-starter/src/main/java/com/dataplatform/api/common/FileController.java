package com.dataplatform.api.common;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${file.path.uploads:../runtime/uploads}")
    private String uploadsPath;

    /** 最大文件大小：30MB */
    private static final long MAX_FILE_SIZE = 30 * 1024 * 1024;

    /** 允许上传的文件扩展名 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            // 文档
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".csv", ".md", ".rtf", ".odt", ".ods",
            // 图片
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg", ".ico",
            // 压缩包
            ".zip", ".rar", ".7z", ".tar", ".gz",
            // 数据
            ".json", ".xml", ".yaml", ".yml", ".sql",
            // 日志
            ".log"
    );

    /** 危险扩展名黑名单（双重保险） */
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            ".exe", ".bat", ".cmd", ".sh", ".ps1", ".vbs", ".js",
            ".msi", ".dll", ".com", ".scr", ".pif", ".hta",
            ".cpl", ".inf", ".reg", ".ws", ".wsf", ".jar", ".class",
            ".php", ".jsp", ".asp", ".aspx", ".cgi", ".py", ".rb", ".pl"
    );

    /**
     * 通用文件上传
     */
    @RequirePermission("file:upload")
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        // 文件大小校验
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("文件大小不能超过 " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }

        // 扩展名校验
        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);

        if (BLOCKED_EXTENSIONS.contains(ext)) {
            return Result.error("禁止上传此类型文件: " + ext);
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return Result.error("不支持的文件类型: " + ext + "，允许的类型: 文档/图片/压缩包/数据文件");
        }

        try {
            // 将上传根目录转为规范路径（解析..和符号链接）
            File uploadRoot = new File(uploadsPath).getCanonicalFile();

            // 按日期分目录
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            File dir = new File(uploadRoot, datePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

            // 保存文件（使用getCanonicalFile解析..路径，Files.copy绕过Tomcat相对路径问题）
            File dest = new File(dir, fileName).getCanonicalFile();
            Files.copy(file.getInputStream(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 返回文件信息
            Map<String, Object> result = new HashMap<>();
            result.put("url", "/file/download/" + datePath + "/" + fileName);
            result.put("name", originalName);
            result.put("size", file.getSize());
            result.put("type", file.getContentType());

            log.info("文件上传成功: {} -> {}", originalName, dest.getAbsolutePath());
            return Result.success(result);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 文件下载/访问
     */
    @GetMapping("/download/**")
    public void download(jakarta.servlet.http.HttpServletRequest request,
                         jakarta.servlet.http.HttpServletResponse response) throws IOException {
        // 提取路径: /file/download/2026/02/14/xxx.pdf -> 2026/02/14/xxx.pdf
        String path = request.getRequestURI();
        String prefix = "/file/download/";
        int idx = path.indexOf(prefix);
        if (idx < 0) {
            response.sendError(404);
            return;
        }
        String filePath = path.substring(idx + prefix.length());

        // 安全检查：防止路径穿越
        if (filePath.contains("..")) {
            response.sendError(403, "非法路径");
            return;
        }

        File uploadRoot = new File(uploadsPath).getCanonicalFile();
        File file = new File(uploadRoot, filePath).getCanonicalFile();
        // 确保文件在上传根目录内（双重防护）
        if (!file.toPath().startsWith(uploadRoot.toPath())) {
            response.sendError(403, "非法路径");
            return;
        }
        if (!file.exists() || !file.isFile()) {
            response.sendError(404, "文件不存在");
            return;
        }

        // 设置响应头
        String fileName = file.getName();
        String contentType = request.getServletContext().getMimeType(fileName);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
        response.setContentLengthLong(file.length());

        // 输出文件内容
        try (var in = new java.io.FileInputStream(file);
             var out = response.getOutputStream()) {
            in.transferTo(out);
        }
    }

    /**
     * 提取小写扩展名
     */
    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }
}
