package com.dataplatform.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 文件路径配置
 */
@Slf4j
@Configuration
public class FilePathConfig {

    @Value("${file.path.logs:../runtime/logs}")
    private String logsPath;

    @Value("${file.path.exports:../runtime/exports}")
    private String exportsPath;

    @Value("${file.path.uploads:../runtime/uploads}")
    private String uploadsPath;

    @Value("${file.path.temp:../runtime/temp}")
    private String tempPath;

    @Value("${export.file.path:../runtime/exports}")
    private String exportFilePath;

    @PostConstruct
    public void init() {
        createDirectoryIfNotExists(logsPath, "日志");
        createDirectoryIfNotExists(exportsPath, "导出");
        createDirectoryIfNotExists(uploadsPath, "上传");
        createDirectoryIfNotExists(tempPath, "临时");
        createDirectoryIfNotExists(exportFilePath, "导出文件");
    }

    private void createDirectoryIfNotExists(String path, String name) {
        File dir = new File(path);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) log.info("创建{}目录: {}", name, dir.getAbsolutePath());
            else log.warn("创建{}目录失败: {}", name, dir.getAbsolutePath());
        } else {
            log.debug("{}目录已存在: {}", name, dir.getAbsolutePath());
        }
    }

    public String getLogsPath() { return logsPath; }
    public String getExportsPath() { return exportsPath; }
    public String getUploadsPath() { return uploadsPath; }
    public String getTempPath() { return tempPath; }
}
