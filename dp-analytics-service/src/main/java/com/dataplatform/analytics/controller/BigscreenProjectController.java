package com.dataplatform.analytics.controller;

import com.dataplatform.common.PageResult;
import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bigscreen-project")
@RequirePermission("page:read")
public class BigscreenProjectController {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initSchema() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS bigscreen_project (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
            "project_name VARCHAR(100) NOT NULL, " +
            "project_code VARCHAR(100) NOT NULL, " +
            "description VARCHAR(500) DEFAULT NULL, " +
            "cover_image VARCHAR(500) DEFAULT NULL, " +
            "default_config TEXT DEFAULT NULL, " +
            "status TINYINT DEFAULT 1, " +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
            "UNIQUE KEY uk_bigscreen_project_code (project_code), " +
            "KEY idx_bigscreen_project_status (status)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        page = Math.max(1, page == null ? 1 : page);
        pageSize = Math.max(1, Math.min(200, pageSize == null ? 10 : pageSize));
        int offset = (page - 1) * pageSize;

        String where = "";
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where = " WHERE project_name LIKE ? OR project_code LIKE ? OR description LIKE ?";
            String like = "%" + keyword.trim() + "%";
            Collections.addAll(args, like, like, like);
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bigscreen_project" + where, Long.class, args.toArray());
        args.add(pageSize);
        args.add(offset);
        List<Map<String, Object>> projects = jdbcTemplate.queryForList(
            "SELECT id, project_name AS projectName, project_code AS projectCode, description, " +
                "cover_image AS coverImage, default_config AS defaultConfig, status, create_time AS createTime, update_time AS updateTime " +
                "FROM bigscreen_project" + where + " ORDER BY update_time DESC, id DESC LIMIT ? OFFSET ?",
            args.toArray());

        projects.forEach(this::attachProjectPages);
        return Result.success(new PageResult<>(projects, total == null ? 0 : total));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, project_name AS projectName, project_code AS projectCode, description, " +
                "cover_image AS coverImage, default_config AS defaultConfig, status, create_time AS createTime, update_time AS updateTime " +
                "FROM bigscreen_project WHERE id = ?",
            id);
        if (rows.isEmpty()) {
            return Result.error("Bigscreen project not found");
        }
        Map<String, Object> project = rows.get(0);
        attachProjectPages(project);
        return Result.success(project);
    }

    @PostMapping
    @Transactional
    @RequirePermission("page:manage")
    @OperationLog(module = "BigscreenProject", type = OperationLog.OperationType.CREATE, description = "Create bigscreen project")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String projectName = stringValue(body.get("projectName"));
        String projectCode = stringValue(body.get("projectCode"));
        if (projectName == null || projectName.isBlank()) return Result.error("projectName is required");
        if (projectCode == null || projectCode.isBlank()) return Result.error("projectCode is required");

        jdbcTemplate.update(
            "INSERT INTO bigscreen_project (project_name, project_code, description, cover_image, default_config, status) VALUES (?, ?, ?, ?, ?, ?)",
            projectName,
            projectCode,
            stringValue(body.get("description")),
            stringValue(body.get("coverImage")),
            jsonOrString(body.get("defaultConfig")),
            intValue(body.get("status"), 1));
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        updateProjectPages(id, body.get("pageIds"));
        return detail(id);
    }

    @PutMapping("/{id}")
    @Transactional
    @RequirePermission("page:manage")
    @OperationLog(module = "BigscreenProject", type = OperationLog.OperationType.UPDATE, description = "Update bigscreen project")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
            "UPDATE bigscreen_project SET project_name = ?, project_code = ?, description = ?, cover_image = ?, default_config = ?, status = ?, update_time = NOW() WHERE id = ?",
            stringValue(body.get("projectName")),
            stringValue(body.get("projectCode")),
            stringValue(body.get("description")),
            stringValue(body.get("coverImage")),
            jsonOrString(body.get("defaultConfig")),
            intValue(body.get("status"), 1),
            id);
        if (body.containsKey("pageIds")) {
            updateProjectPages(id, body.get("pageIds"));
        }
        return detail(id);
    }

    @DeleteMapping("/{id}")
    @Transactional
    @RequirePermission("page:manage")
    @OperationLog(module = "BigscreenProject", type = OperationLog.OperationType.DELETE, description = "Delete bigscreen project")
    public Result<Void> delete(@PathVariable Long id) {
        jdbcTemplate.update("UPDATE page_definition SET project_id = NULL WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM bigscreen_project WHERE id = ?", id);
        return Result.success(null);
    }

    @PutMapping("/{id}/pages")
    @Transactional
    @RequirePermission("page:manage")
    public Result<Void> updatePages(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        updateProjectPages(id, body == null ? null : body.get("pageIds"));
        return Result.success(null);
    }

    private void attachProjectPages(Map<String, Object> project) {
        Long id = numberValue(project.get("id"));
        if (id == null) return;
        List<Map<String, Object>> pages = jdbcTemplate.queryForList(
            "SELECT id, page_name AS pageName, page_code AS pageCode, description, layout_mode AS layoutMode, " +
                "status, create_time AS createTime, update_time AS updateTime " +
                "FROM page_definition WHERE project_id = ? ORDER BY update_time DESC, id DESC",
            id);
        List<Long> pageIds = pages.stream()
            .map(page -> numberValue(page.get("id")))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        project.put("pageIds", pageIds);
        project.put("pages", pages);
    }

    private void updateProjectPages(Long projectId, Object rawPageIds) {
        jdbcTemplate.update("UPDATE page_definition SET project_id = NULL WHERE project_id = ?", projectId);
        List<Long> pageIds = toLongList(rawPageIds);
        if (pageIds.isEmpty()) return;
        String placeholders = pageIds.stream().map(id -> "?").collect(Collectors.joining(","));
        jdbcTemplate.update(
            "UPDATE page_definition SET project_id = ? WHERE id IN (" + placeholders + ")",
            buildProjectPageArgs(projectId, pageIds));
    }

    private Object[] buildProjectPageArgs(Long projectId, List<Long> pageIds) {
        List<Object> args = new ArrayList<>();
        args.add(projectId);
        args.addAll(pageIds);
        return args.toArray();
    }

    private List<Long> toLongList(Object value) {
        if (!(value instanceof List<?> rawList)) return Collections.emptyList();
        return rawList.stream()
            .map(this::numberValue)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private Long numberValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer intValue(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String jsonOrString(Object value) {
        if (value == null) return null;
        if (value instanceof String text) return text;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (Exception ignored) {
            return String.valueOf(value);
        }
    }
}
