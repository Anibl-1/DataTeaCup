package com.dataplatform.data.service.upgrade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 升级服务实现
 * 需求: 31.1-31.8
 */
@Service
public class UpgradeServiceImpl implements UpgradeService {

    private static final Logger log = LoggerFactory.getLogger(UpgradeServiceImpl.class);

    @Value("${app.version:1.0.0}")
    private String currentVersion;

    private final List<UpgradeRecord> upgradeHistory = new CopyOnWriteArrayList<>();
    private final List<BackupInfo> backups = new CopyOnWriteArrayList<>();

    @Override
    public VersionInfo checkForUpdate() {
        VersionInfo info = new VersionInfo();
        info.setCurrentVersion(currentVersion);
        // 实际场景中会调用远程升级服务器
        info.setLatestVersion(currentVersion);
        info.setUpdateAvailable(false);
        info.setIncremental(true);
        log.info("版本检查完成: current={}", currentVersion);
        return info;
    }

    @Override
    public String getCurrentVersion() {
        return currentVersion;
    }

    @Override
    public UpgradeResult performUpgrade(String targetVersion) {
        log.info("开始升级: {} -> {}", currentVersion, targetVersion);
        UpgradeResult result = new UpgradeResult();
        result.setFromVersion(currentVersion);
        result.setToVersion(targetVersion);

        try {
            // 1. 创建备份
            BackupInfo backup = createBackup();
            log.info("升级前备份完成: {}", backup.getBackupId());

            // 2. 执行升级（实际场景中下载并安装新版本）
            String oldVersion = currentVersion;
            currentVersion = targetVersion;

            result.setSuccess(true);
            result.setMessage("升级成功");

            recordHistory(oldVersion, targetVersion, "upgrade", true, "升级成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("升级失败: " + e.getMessage());
            recordHistory(currentVersion, targetVersion, "upgrade", false, e.getMessage());
            log.error("升级失败", e);
        }

        return result;
    }

    @Override
    public UpgradeResult rollback(String targetVersion) {
        log.info("开始回滚: {} -> {}", currentVersion, targetVersion);
        UpgradeResult result = new UpgradeResult();
        result.setFromVersion(currentVersion);
        result.setToVersion(targetVersion);

        try {
            String oldVersion = currentVersion;
            currentVersion = targetVersion;
            result.setSuccess(true);
            result.setMessage("回滚成功");
            recordHistory(oldVersion, targetVersion, "rollback", true, "回滚成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("回滚失败: " + e.getMessage());
            recordHistory(currentVersion, targetVersion, "rollback", false, e.getMessage());
        }

        return result;
    }

    @Override
    public UpgradeResult applyHotfix(String hotfixId) {
        log.info("应用热补丁: {}", hotfixId);
        UpgradeResult result = new UpgradeResult();
        result.setFromVersion(currentVersion);
        result.setToVersion(currentVersion + "-hotfix-" + hotfixId);

        try {
            // 实际场景中会动态加载补丁类
            result.setSuccess(true);
            result.setMessage("热补丁应用成功");
            recordHistory(currentVersion, result.getToVersion(), "hotfix", true, "热补丁应用成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("热补丁应用失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public List<UpgradeRecord> getUpgradeHistory() {
        return Collections.unmodifiableList(upgradeHistory);
    }

    @Override
    public BackupInfo createBackup() {
        BackupInfo backup = new BackupInfo();
        backup.setBackupId(UUID.randomUUID().toString().substring(0, 8));
        backup.setVersion(currentVersion);
        backup.setPath("/backups/" + backup.getBackupId());
        backup.setTimestamp(LocalDateTime.now());
        backups.add(backup);
        log.info("创建备份: id={}, version={}", backup.getBackupId(), backup.getVersion());
        return backup;
    }

    private void recordHistory(String from, String to, String type, boolean success, String message) {
        UpgradeRecord record = new UpgradeRecord();
        record.setFromVersion(from);
        record.setToVersion(to);
        record.setType(type);
        record.setSuccess(success);
        record.setMessage(message);
        record.setTimestamp(LocalDateTime.now());
        upgradeHistory.add(record);
    }
}
