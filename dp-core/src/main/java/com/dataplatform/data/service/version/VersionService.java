package com.dataplatform.data.service.version;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 版本管理服务
 * 需求: 24.1, 24.2, 24.3, 24.4, 24.5, 24.6
 */
@Slf4j
@Service
public class VersionService {

    // key = resourceType:resourceId
    private final Map<String, List<ResourceVersion>> versionMap = new ConcurrentHashMap<>();
    // 当前内容: key = resourceType:resourceId
    private final Map<String, String> currentContent = new ConcurrentHashMap<>();

    /**
     * 保存版本（需求: 24.1）
     */
    public ResourceVersion saveVersion(String resourceType, String resourceId,
                                        String content, String description, String userId) {
        String key = resourceType + ":" + resourceId;
        List<ResourceVersion> versions = versionMap.computeIfAbsent(key, k -> new ArrayList<>());

        ResourceVersion version = new ResourceVersion();
        version.setId(UUID.randomUUID().toString().substring(0, 12));
        version.setResourceType(resourceType);
        version.setResourceId(resourceId);
        version.setVersionNumber(versions.size() + 1);
        version.setContent(content);
        version.setChangeDescription(description);
        version.setCreatedBy(userId);
        version.setCreatedAt(LocalDateTime.now());

        versions.add(version);
        currentContent.put(key, content);

        log.info("[版本管理] 保存: resource={}/{}, version={}", resourceType, resourceId, version.getVersionNumber());
        return version;
    }

    /**
     * 获取版本历史（需求: 24.1）
     */
    public List<ResourceVersion> getVersionHistory(String resourceType, String resourceId) {
        String key = resourceType + ":" + resourceId;
        List<ResourceVersion> versions = versionMap.get(key);
        if (versions == null) return Collections.emptyList();
        // 返回倒序（最新在前）
        List<ResourceVersion> result = new ArrayList<>(versions);
        Collections.reverse(result);
        return result;
    }

    /**
     * 获取指定版本（需求: 24.1）
     */
    public ResourceVersion getVersion(String resourceType, String resourceId, int versionNumber) {
        String key = resourceType + ":" + resourceId;
        List<ResourceVersion> versions = versionMap.get(key);
        if (versions == null) return null;
        return versions.stream()
                .filter(v -> v.getVersionNumber() == versionNumber)
                .findFirst().orElse(null);
    }

    /**
     * 回滚到指定版本（需求: 24.3）
     */
    public String rollback(String resourceType, String resourceId, int versionNumber) {
        ResourceVersion version = getVersion(resourceType, resourceId, versionNumber);
        if (version == null) {
            throw new IllegalArgumentException("版本不存在: " + versionNumber);
        }
        String key = resourceType + ":" + resourceId;
        currentContent.put(key, version.getContent());
        log.info("[版本管理] 回滚: resource={}/{}, toVersion={}", resourceType, resourceId, versionNumber);
        return version.getContent();
    }

    /**
     * 获取当前内容
     */
    public String getCurrentContent(String resourceType, String resourceId) {
        return currentContent.get(resourceType + ":" + resourceId);
    }

    /**
     * 版本对比（需求: 24.2）
     * 返回两个版本内容的简单差异
     */
    public VersionDiff compareVersions(String resourceType, String resourceId,
                                        int version1, int version2) {
        ResourceVersion v1 = getVersion(resourceType, resourceId, version1);
        ResourceVersion v2 = getVersion(resourceType, resourceId, version2);
        if (v1 == null || v2 == null) {
            throw new IllegalArgumentException("版本不存在");
        }

        VersionDiff diff = new VersionDiff();
        diff.setVersion1(version1);
        diff.setVersion2(version2);
        diff.setContent1(v1.getContent());
        diff.setContent2(v2.getContent());
        diff.setSame(v1.getContent().equals(v2.getContent()));
        return diff;
    }

    @Data
    public static class VersionDiff {
        private int version1;
        private int version2;
        private String content1;
        private String content2;
        private boolean same;
    }
}
