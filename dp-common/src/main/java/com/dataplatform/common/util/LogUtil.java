package com.dataplatform.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * 统一日志工具类
 * 
 * <h3>日志规范：</h3>
 * <ul>
 *   <li>格式: [模块] 操作 | 关键参数 | 结果/耗时</li>
 *   <li>INFO: 正常业务流程、关键操作入口</li>
 *   <li>DEBUG: 详细调试信息、方法执行细节</li>
 *   <li>WARN: 可恢复异常、业务警告</li>
 *   <li>ERROR: 系统异常、需要关注的错误</li>
 * </ul>
 * 
 * @author dataplatform
 */
public class LogUtil {

    private static final Logger OPERATION_LOG = LoggerFactory.getLogger("OPERATION_LOG");
    private static final Logger SQL_LOG = LoggerFactory.getLogger("SQL_LOG");
    private static final Logger API_LOG = LoggerFactory.getLogger("API_LOG");

    // ==================== 日志前缀常量 ====================
    public static final String AUTH = "Auth";
    public static final String USER = "User";
    public static final String ROLE = "Role";
    public static final String MENU = "Menu";
    public static final String DATA = "Data";
    public static final String AI = "AI";
    public static final String REPORT = "Report";
    public static final String PAGE = "Page";
    public static final String CHART = "Chart";
    public static final String EXPORT = "Export";
    public static final String IMPORT = "Import";
    public static final String CACHE = "Cache";
    public static final String SYNC = "Sync";
    public static final String SCHEDULER = "Scheduler";
    public static final String PIPELINE = "Pipeline";
    public static final String NOTIFY = "Notify";
    public static final String FILE = "File";
    public static final String DB = "DB";
    public static final String SYSTEM = "System";

    public static final String MDC_USER = "user";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_IP = "ip";
    public static final String MDC_TRACE_ID = "traceId";

    public static void setMDC(String user, String requestId, String ip) {
        if (user != null) MDC.put(MDC_USER, user);
        if (requestId != null) MDC.put(MDC_REQUEST_ID, requestId);
        if (ip != null) MDC.put(MDC_IP, ip);
    }

    public static void clearMDC() { MDC.clear(); }

    // ==================== API 请求日志 ====================
    
    /**
     * 记录 API 请求开始
     */
    public static void apiRequest(Logger log, String module, String action, Object... params) {
        if (log.isInfoEnabled()) {
            if (params != null && params.length > 0) {
                log.info("[{}] {} | params={}", module, action, formatParams(params));
            } else {
                log.info("[{}] {}", module, action);
            }
        }
    }

    /**
     * 记录 API 请求成功
     */
    public static void apiSuccess(Logger log, String module, String action, long durationMs) {
        log.info("[{}] {} | OK | {}ms", module, action, durationMs);
    }

    /**
     * 记录 API 请求成功（带结果摘要）
     */
    public static void apiSuccess(Logger log, String module, String action, long durationMs, String resultSummary) {
        log.info("[{}] {} | OK | {}ms | {}", module, action, durationMs, resultSummary);
    }

    /**
     * 记录 API 请求失败
     */
    public static void apiFail(Logger log, String module, String action, String reason) {
        log.warn("[{}] {} | FAIL | {}", module, action, reason);
    }

    /**
     * 记录 API 请求异常
     */
    public static void apiError(Logger log, String module, String action, Throwable e) {
        log.error("[{}] {} | ERROR | {}", module, action, e.getMessage(), e);
    }

    public static void operation(String module, String action, String detail) {
        String user = MDC.get(MDC_USER);
        String ip = MDC.get(MDC_IP);
        OPERATION_LOG.info("[{}] [{}] [{}] {} - {}",
            user != null ? user : "system", ip != null ? ip : "-", module, action, detail);
    }

    public static void operation(String module, String action, String detail, long durationMs) {
        String user = MDC.get(MDC_USER);
        String ip = MDC.get(MDC_IP);
        OPERATION_LOG.info("[{}] [{}] [{}] {} - {} ({}ms)",
            user != null ? user : "system", ip != null ? ip : "-", module, action, detail, durationMs);
    }

    public static void operationSuccess(String module, String action, String detail) {
        operation(module, action, "SUCCESS: " + detail);
    }

    public static void operationFailed(String module, String action, String detail, Throwable e) {
        String user = MDC.get(MDC_USER);
        String ip = MDC.get(MDC_IP);
        OPERATION_LOG.error("[{}] [{}] [{}] {} - FAILED: {} | Error: {}",
            user != null ? user : "system", ip != null ? ip : "-", module, action, detail, e.getMessage());
    }

    public static void sql(String operation, String sql, long durationMs) {
        SQL_LOG.debug("[{}] {}ms | {}", operation, durationMs, sql);
    }

    public static void sql(String operation, String sql, Object params, long durationMs) {
        SQL_LOG.debug("[{}] {}ms | {} | params: {}", operation, durationMs, sql, params);
    }

    public static void slowSql(String sql, long durationMs, long threshold) {
        if (durationMs > threshold) SQL_LOG.warn("[SLOW SQL] {}ms (threshold: {}ms) | {}", durationMs, threshold, sql);
    }

    public static void methodStart(Logger log, String methodName, Object... params) {
        if (log.isDebugEnabled()) {
            if (params != null && params.length > 0) log.debug(">>> {} 开始执行, 参数: {}", methodName, params);
            else log.debug(">>> {} 开始执行", methodName);
        }
    }

    public static void methodEnd(Logger log, String methodName, long durationMs) {
        if (log.isDebugEnabled()) log.debug("<<< {} 执行完成, 耗时: {}ms", methodName, durationMs);
    }

    public static void methodEnd(Logger log, String methodName, long durationMs, Object result) {
        if (log.isDebugEnabled()) log.debug("<<< {} 执行完成, 耗时: {}ms, 结果: {}", methodName, durationMs, result);
    }

    public static void businessError(Logger log, String operation, String message) {
        log.warn("业务异常 - {}: {}", operation, message);
    }

    public static void systemError(Logger log, String operation, Throwable e) {
        log.error("系统异常 - {}: {}", operation, e.getMessage(), e);
    }

    public static void externalCall(Logger log, String service, String api, long durationMs, boolean success) {
        if (success) log.info("外部调用成功 - {}.{} 耗时: {}ms", service, api, durationMs);
        else log.warn("外部调用失败 - {}.{} 耗时: {}ms", service, api, durationMs);
    }

    public static void scheduledTask(Logger log, String taskName, String status, long durationMs) {
        log.info("定时任务 [{}] {} - 耗时: {}ms", taskName, status, durationMs);
    }

    public static void dataChange(Logger log, String entity, String operation, Object id) {
        log.info("数据变更 - {} {} ID: {}", entity, operation, id);
    }

    public static void dataChangeBatch(Logger log, String entity, String operation, int count) {
        log.info("批量数据变更 - {} {} 数量: {}", entity, operation, count);
    }

    // ==================== 辅助方法 ====================

    /**
     * 格式化参数为简洁字符串
     */
    private static String formatParams(Object... params) {
        if (params == null || params.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) sb.append(", ");
            Object p = params[i];
            if (p == null) {
                sb.append("null");
            } else if (p instanceof String) {
                String s = (String) p;
                sb.append(s.length() > 100 ? s.substring(0, 100) + "..." : s);
            } else {
                sb.append(p);
            }
        }
        return sb.toString();
    }

    /**
     * 截断长字符串用于日志
     */
    public static String truncate(String s, int maxLen) {
        if (s == null) return "null";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    /**
     * 安全获取请求参数摘要
     */
    public static String paramSummary(String key, Object value) {
        if (value == null) return key + "=null";
        String str = String.valueOf(value);
        return key + "=" + truncate(str, 50);
    }

    // ==================== 认证相关日志 ====================

    public static void authSuccess(Logger log, String username, String action) {
        log.info("[{}] {} | user={}", AUTH, action, username);
    }

    public static void authFail(Logger log, String action, String reason) {
        log.warn("[{}] {} | FAIL | {}", AUTH, action, reason);
    }

    // ==================== 数据库操作日志 ====================

    public static void dbQuery(Logger log, String table, int rowCount, long durationMs) {
        log.debug("[{}] SELECT {} | rows={} | {}ms", DB, table, rowCount, durationMs);
    }

    public static void dbInsert(Logger log, String table, Object id) {
        log.info("[{}] INSERT {} | id={}", DB, table, id);
    }

    public static void dbUpdate(Logger log, String table, Object id) {
        log.info("[{}] UPDATE {} | id={}", DB, table, id);
    }

    public static void dbDelete(Logger log, String table, Object id) {
        log.info("[{}] DELETE {} | id={}", DB, table, id);
    }

    public static void dbBatch(Logger log, String operation, String table, int count) {
        log.info("[{}] BATCH {} {} | count={}", DB, operation, table, count);
    }

    // ==================== 缓存日志 ====================

    public static void cacheHit(Logger log, String cacheName, String key) {
        log.debug("[{}] HIT | cache={} | key={}", CACHE, cacheName, truncate(key, 50));
    }

    public static void cacheMiss(Logger log, String cacheName, String key) {
        log.debug("[{}] MISS | cache={} | key={}", CACHE, cacheName, truncate(key, 50));
    }

    public static void cacheEvict(Logger log, String cacheName, String key) {
        log.info("[{}] EVICT | cache={} | key={}", CACHE, cacheName, key != null ? truncate(key, 50) : "ALL");
    }

    // ==================== 外部服务调用日志 ====================

    public static void externalRequest(Logger log, String service, String api) {
        log.debug("[External] {} {} | START", service, api);
    }

    public static void externalSuccess(Logger log, String service, String api, long durationMs) {
        log.info("[External] {} {} | OK | {}ms", service, api, durationMs);
    }

    public static void externalFail(Logger log, String service, String api, long durationMs, String error) {
        log.warn("[External] {} {} | FAIL | {}ms | {}", service, api, durationMs, error);
    }
}
