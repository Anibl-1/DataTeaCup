package com.dataplatform.data.service.upgrade;

import java.util.List;

/**
 * 升级服务接口
 * 需求: 31.1, 31.2, 31.3, 31.4, 31.5, 31.6, 31.7, 31.8
 */
public interface UpgradeService {

    /** 检查新版本 */
    VersionInfo checkForUpdate();

    /** 获取当前版本 */
    String getCurrentVersion();

    /** 执行升级 */
    UpgradeResult performUpgrade(String targetVersion);

    /** 回滚到指定版本 */
    UpgradeResult rollback(String targetVersion);

    /** 应用热补丁 */
    UpgradeResult applyHotfix(String hotfixId);

    /** 获取升级历史 */
    List<UpgradeRecord> getUpgradeHistory();

    /** 创建备份 */
    BackupInfo createBackup();

    /** 版本信息 */
    class VersionInfo {
        private String currentVersion;
        private String latestVersion;
        private boolean updateAvailable;
        private String releaseNotes;
        private String downloadUrl;
        private long fileSize;
        private boolean incremental;

        public String getCurrentVersion() { return currentVersion; }
        public void setCurrentVersion(String v) { this.currentVersion = v; }
        public String getLatestVersion() { return latestVersion; }
        public void setLatestVersion(String v) { this.latestVersion = v; }
        public boolean isUpdateAvailable() { return updateAvailable; }
        public void setUpdateAvailable(boolean b) { this.updateAvailable = b; }
        public String getReleaseNotes() { return releaseNotes; }
        public void setReleaseNotes(String n) { this.releaseNotes = n; }
        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String u) { this.downloadUrl = u; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long s) { this.fileSize = s; }
        public boolean isIncremental() { return incremental; }
        public void setIncremental(boolean i) { this.incremental = i; }
    }

    /** 升级结果 */
    class UpgradeResult {
        private boolean success;
        private String message;
        private String fromVersion;
        private String toVersion;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean s) { this.success = s; }
        public String getMessage() { return message; }
        public void setMessage(String m) { this.message = m; }
        public String getFromVersion() { return fromVersion; }
        public void setFromVersion(String v) { this.fromVersion = v; }
        public String getToVersion() { return toVersion; }
        public void setToVersion(String v) { this.toVersion = v; }
    }

    /** 升级记录 */
    class UpgradeRecord {
        private String fromVersion;
        private String toVersion;
        private String type; // upgrade, rollback, hotfix
        private boolean success;
        private String message;
        private java.time.LocalDateTime timestamp;

        public String getFromVersion() { return fromVersion; }
        public void setFromVersion(String v) { this.fromVersion = v; }
        public String getToVersion() { return toVersion; }
        public void setToVersion(String v) { this.toVersion = v; }
        public String getType() { return type; }
        public void setType(String t) { this.type = t; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean s) { this.success = s; }
        public String getMessage() { return message; }
        public void setMessage(String m) { this.message = m; }
        public java.time.LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(java.time.LocalDateTime t) { this.timestamp = t; }
    }

    /** 备份信息 */
    class BackupInfo {
        private String backupId;
        private String version;
        private String path;
        private long size;
        private java.time.LocalDateTime timestamp;

        public String getBackupId() { return backupId; }
        public void setBackupId(String id) { this.backupId = id; }
        public String getVersion() { return version; }
        public void setVersion(String v) { this.version = v; }
        public String getPath() { return path; }
        public void setPath(String p) { this.path = p; }
        public long getSize() { return size; }
        public void setSize(long s) { this.size = s; }
        public java.time.LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(java.time.LocalDateTime t) { this.timestamp = t; }
    }
}
