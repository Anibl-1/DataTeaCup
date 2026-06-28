package com.dataplatform.data.service.metadata;

import com.dataplatform.data.service.connector.ConnectorConfig;
import com.dataplatform.data.service.connector.ConnectorManager;
import com.dataplatform.data.service.connector.ConnectorResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * 元数据采集与管理服务
 * 需求: 20.1, 20.2, 20.3, 20.5, 20.6, 20.7, 20.8
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataCollector {

    private final ConnectorManager connectorManager;

    // 当前元数据: key = dataSourceId:tableName:columnName
    private final Map<String, MetadataEntry> metadataMap = new ConcurrentHashMap<>();
    // 元数据变更历史
    private final Map<String, Deque<MetadataEntry>> versionHistory = new ConcurrentHashMap<>();

    /**
     * 采集数据源元数据（需求: 20.1）
     */
    public List<MetadataEntry> collectMetadata(String dataSourceId, ConnectorConfig config) {
        List<MetadataEntry> collected = new ArrayList<>();
        try {
            List<String> tables = connectorManager.listTables(config);
            for (String table : tables) {
                List<ConnectorResult.ColumnMeta> columns = connectorManager.getColumns(config, table);
                for (ConnectorResult.ColumnMeta col : columns) {
                    MetadataEntry entry = new MetadataEntry();
                    entry.setId(dataSourceId + ":" + table + ":" + col.getName());
                    entry.setDataSourceId(dataSourceId);
                    entry.setTableName(table);
                    entry.setColumnName(col.getName());
                    entry.setColumnType(col.getType());
                    entry.setNullable(col.isNullable());
                    entry.setCollectedAt(LocalDateTime.now());
                    entry.setVersion(1);

                    // 检查是否有变更（需求: 20.2）
                    MetadataEntry existing = metadataMap.get(entry.getId());
                    if (existing != null) {
                        if (!existing.getColumnType().equals(entry.getColumnType())
                                || existing.isNullable() != entry.isNullable()) {
                            entry.setVersion(existing.getVersion() + 1);
                            entry.setBusinessName(existing.getBusinessName());
                            entry.setDescription(existing.getDescription());
                            entry.setCategory(existing.getCategory());
                            entry.setTags(existing.getTags());
                            storeVersion(existing);
                        } else {
                            // 无变更，保留业务元数据
                            entry.setBusinessName(existing.getBusinessName());
                            entry.setDescription(existing.getDescription());
                            entry.setCategory(existing.getCategory());
                            entry.setTags(existing.getTags());
                            entry.setVersion(existing.getVersion());
                        }
                    }

                    entry.setUpdatedAt(LocalDateTime.now());
                    metadataMap.put(entry.getId(), entry);
                    collected.add(entry);
                }
            }
            log.info("[元数据采集] 完成: dataSourceId={}, entries={}", dataSourceId, collected.size());
        } catch (Exception e) {
            log.error("[元数据采集] 失败: dataSourceId={}", dataSourceId, e);
        }
        return collected;
    }

    /**
     * 更新业务元数据（需求: 20.3）
     */
    public MetadataEntry updateBusinessMetadata(String entryId, String businessName,
                                                 String description, String category) {
        MetadataEntry entry = metadataMap.get(entryId);
        if (entry == null) {
            throw new IllegalArgumentException("元数据不存在: " + entryId);
        }
        entry.setBusinessName(businessName);
        entry.setDescription(description);
        entry.setCategory(category);
        entry.setUpdatedAt(LocalDateTime.now());
        return entry;
    }

    /**
     * 管理标签（需求: 20.7）
     */
    public MetadataEntry addTag(String entryId, String tag) {
        MetadataEntry entry = metadataMap.get(entryId);
        if (entry != null) {
            entry.getTags().add(tag);
        }
        return entry;
    }

    public MetadataEntry removeTag(String entryId, String tag) {
        MetadataEntry entry = metadataMap.get(entryId);
        if (entry != null) {
            entry.getTags().remove(tag);
        }
        return entry;
    }

    /**
     * 搜索元数据（需求: 20.6）
     */
    public List<MetadataEntry> search(String keyword) {
        String lower = keyword.toLowerCase();
        return metadataMap.values().stream()
                .filter(e -> matches(e, lower))
                .collect(Collectors.toList());
    }

    /**
     * 获取版本历史（需求: 20.2）
     */
    public List<MetadataEntry> getVersionHistory(String entryId) {
        Deque<MetadataEntry> history = versionHistory.get(entryId);
        if (history == null) return Collections.emptyList();
        return new ArrayList<>(history);
    }

    /**
     * 获取数据资产目录（需求: 20.8）
     */
    public Map<String, Object> getAssetCatalog() {
        Map<String, Object> catalog = new LinkedHashMap<>();
        catalog.put("totalEntries", metadataMap.size());

        // 按数据源分组
        Map<String, List<MetadataEntry>> bySource = metadataMap.values().stream()
                .collect(Collectors.groupingBy(MetadataEntry::getDataSourceId));
        catalog.put("dataSources", bySource.size());

        // 按表分组
        Map<String, Long> tableCount = metadataMap.values().stream()
                .collect(Collectors.groupingBy(
                        e -> e.getDataSourceId() + ":" + e.getTableName(),
                        Collectors.counting()));
        catalog.put("tables", tableCount.size());

        // 按分类分组
        Map<String, Long> byCategory = metadataMap.values().stream()
                .filter(e -> e.getCategory() != null)
                .collect(Collectors.groupingBy(MetadataEntry::getCategory, Collectors.counting()));
        catalog.put("categories", byCategory);

        return catalog;
    }

    /**
     * 获取所有元数据
     */
    public List<MetadataEntry> listAll() {
        return new ArrayList<>(metadataMap.values());
    }

    private boolean matches(MetadataEntry entry, String keyword) {
        if (entry.getTableName() != null && entry.getTableName().toLowerCase().contains(keyword)) return true;
        if (entry.getColumnName() != null && entry.getColumnName().toLowerCase().contains(keyword)) return true;
        if (entry.getBusinessName() != null && entry.getBusinessName().toLowerCase().contains(keyword)) return true;
        if (entry.getDescription() != null && entry.getDescription().toLowerCase().contains(keyword)) return true;
        return entry.getTags().stream().anyMatch(t -> t.toLowerCase().contains(keyword));
    }

    private void storeVersion(MetadataEntry entry) {
        versionHistory.computeIfAbsent(entry.getId(), k -> new ConcurrentLinkedDeque<>()).addLast(entry);
    }
}
