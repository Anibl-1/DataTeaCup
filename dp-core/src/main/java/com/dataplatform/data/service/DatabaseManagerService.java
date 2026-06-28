package com.dataplatform.data.service;

import java.util.List;
import java.util.Map;

/**
 * 数据库管理服务接口
 * 提供类似Navicat的数据库管理功能
 */
public interface DatabaseManagerService {

    /**
     * 创建代理连接
     */
    String createProxyConnection(Map<String, Object> connectionInfo) throws Exception;

    /**
     * 关闭代理连接
     */
    void closeProxyConnection(String sessionId);

    /**
     * 测试数据库连接
     */
    String testConnection(Map<String, Object> connectionInfo) throws Exception;

    /**
     * 获取表列表（代理模式）
     */
    List<Map<String, Object>> getTablesProxy(String sessionId) throws Exception;

    /**
     * 获取视图列表（代理模式）
     */
    List<Map<String, Object>> getViewsProxy(String sessionId) throws Exception;

    /**
     * 获取存储过程/函数列表（代理模式）
     */
    List<Map<String, Object>> getProceduresProxy(String sessionId) throws Exception;

    /**
     * 获取表结构（代理模式）
     */
    List<Map<String, Object>> getTableStructureProxy(String sessionId, Map<String, Object> params) throws Exception;

    /**
     * 查询表数据（代理模式）
     */
    Map<String, Object> queryTableDataProxy(String sessionId, Map<String, Object> params) throws Exception;

    /**
     * 执行SQL（代理模式）
     */
    Map<String, Object> executeSqlProxy(String sessionId, Map<String, Object> params) throws Exception;

    /**
     * 获取视图定义（代理模式）
     */
    String getViewDefinitionProxy(String sessionId, Map<String, Object> params) throws Exception;

    /**
     * 获取存储过程定义（代理模式）
     */
    String getProcedureDefinitionProxy(String sessionId, Map<String, Object> params) throws Exception;

    /**
     * 获取表索引（代理模式）
     */
    List<Map<String, Object>> getTableIndexesProxy(String sessionId, Map<String, Object> params) throws Exception;

    /**
     * 获取SQL执行历史记录
     */
    Map<String, Object> getSqlHistory(String sessionId, String keyword, String status,
                                       Integer page, Integer pageSize);

    /**
     * 清空会话SQL历史
     */
    void clearSqlHistory(String sessionId);

    /**
     * 保存SQL草稿到历史
     */
    void saveSqlDraft(String sessionId, String sqlContent);

    /**
     * 执行EXPLAIN执行计划
     */
    Map<String, Object> explainSql(String sessionId, Map<String, Object> params) throws Exception;
}
