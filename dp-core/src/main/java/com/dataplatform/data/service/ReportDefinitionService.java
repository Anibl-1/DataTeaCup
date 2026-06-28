package com.dataplatform.data.service;

import com.dataplatform.common.constants.Constants;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.dto.ReportDefinitionCreateDTO;
import com.dataplatform.data.dto.ReportDefinitionUpdateDTO;
import com.dataplatform.data.dto.ReportFieldDTO;
import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.entity.ReportDefinition;
import com.dataplatform.data.entity.ReportField;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.mapper.ReportDefinitionMapper;
import com.dataplatform.data.mapper.ReportFieldMapper;
import com.dataplatform.data.service.DbConnectionUtil;
import com.dataplatform.common.util.SqlParamUtil;
import com.dataplatform.common.util.SqlSecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 报表定义服务类
 * 处理报表定义的增删改查和SQL执行
 * 
 * @author dataplatform
 */
@Slf4j
@Service
public class ReportDefinitionService {
    
    @org.springframework.beans.factory.annotation.Value("${db.query.timeout:300}")
    private int queryTimeout;  // 查询超时（秒）
    
    @org.springframework.beans.factory.annotation.Value("${export.pdf.max-rows:5000}")
    private int pdfMaxRows;  // PDF导出最大行数
    
    @Autowired
    private ReportDefinitionMapper reportDefinitionMapper;

    @Autowired
    private LicenseLimitService licenseLimitService;
    
    @Autowired
    private ReportFieldMapper reportFieldMapper;
    
    @Autowired
    private DataSourceMapper dataSourceMapper;
    
    @Autowired
    private DataSourceService dataSourceService;
    
    @Autowired
    private DbConnectionUtil dbConnectionUtil;
    
    @Autowired
    private com.dataplatform.data.service.DataSourceConnectionPoolManager connectionPoolManager;
    
    @Autowired
    private com.dataplatform.system.mapper.UserMapper userMapper;
    
    /**
     * 获取报表定义列表（分页）
     * 
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @param keyword 搜索关键词（可选）
     * @return 报表定义列表
     */
    public List<ReportDefinition> getReportDefinitionList(Integer page, Integer pageSize, String keyword) {
        if (page == null || page < 1) {
            page = Constants.DEFAULT_PAGE;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = Constants.DEFAULT_PAGE_SIZE;
        }
        int offset = (page - 1) * pageSize;
        return reportDefinitionMapper.selectList(offset, pageSize, keyword);
    }
    
    /**
     * 获取报表定义总数
     * 
     * @param keyword 搜索关键词（可选）
     * @return 报表定义总数
     */
    public long getReportDefinitionCount(String keyword) {
        return reportDefinitionMapper.count(keyword);
    }
    
    /**
     * 获取所有可用的报表列表（用于图表设计器选择）
     * 只返回启用状态的报表
     * 
     * @return 可用报表列表
     */
    public List<ReportDefinition> getAvailableReports() {
        return reportDefinitionMapper.selectAvailableReports();
    }
    
    /**
     * 根据ID获取报表定义
     * 
     * @param id 报表ID
     * @return 报表定义信息（包含字段列表）
     */
    public ReportDefinition getReportDefinitionById(Long id) {
        ReportDefinition report = reportDefinitionMapper.selectById(id);
        if (report != null) {
            List<ReportField> fields = reportFieldMapper.selectByReportId(id);
            report.setFields(fields);
        }
        return report;
    }
    
    /**
     * 根据编码获取报表定义
     * 
     * @param reportCode 报表编码
     * @return 报表定义信息（包含字段列表）
     */
    public ReportDefinition getReportDefinitionByCode(String reportCode) {
        ReportDefinition report = reportDefinitionMapper.selectByCode(reportCode);
        if (report != null) {
            List<ReportField> fields = reportFieldMapper.selectByReportId(report.getId());
            report.setFields(fields);
        }
        return report;
    }
    
    /**
     * 创建报表定义
     * 
     * @param dto 报表定义创建DTO
     * @param createBy 创建人ID
     * @return 报表定义ID
     */
    @Transactional
    public Long createReportDefinition(ReportDefinitionCreateDTO dto, Long createBy) {
        licenseLimitService.assertReportPageCreationAllowed(reportDefinitionMapper.countAll());

        // 验证数据源是否存在
        DataSource dataSource = dataSourceMapper.selectById(dto.getDataSourceId());
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        // 验证SQL安全性（先去除 ${} 参数再验证）
        SqlSecurityUtil.validateSql(SqlParamUtil.stripCustomParamsForTest(dto.getSqlContent()));
        
        // 检查编码是否已存在
        ReportDefinition existing = reportDefinitionMapper.selectByCode(dto.getReportCode());
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表编码已存在: " + dto.getReportCode());
        }
        
        // 测试SQL并获取字段信息
        List<ReportFieldDTO> fields = testSqlAndGetFields(dto.getDataSourceId(), dto.getSqlContent());
        
        // 如果没有提供字段配置，使用从数据库获取的字段信息
        if (dto.getFields() == null || dto.getFields().isEmpty()) {
            dto.setFields(fields);
        }
        
        // 创建报表定义
        ReportDefinition report = new ReportDefinition();
        report.setReportName(dto.getReportName());
        report.setReportCode(dto.getReportCode());
        report.setDataSourceId(dto.getDataSourceId());
        report.setSqlContent(SqlSecurityUtil.sanitizeSql(dto.getSqlContent()));
        report.setDescription(dto.getDescription());
        report.setParams(dto.getParams());
        report.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        report.setAllowExportExcel(dto.getAllowExportExcel() != null && dto.getAllowExportExcel() ? 1 : (dto.getAllowExportExcel() == null ? 1 : 0));
        report.setAllowExportPdf(dto.getAllowExportPdf() != null && dto.getAllowExportPdf() ? 1 : (dto.getAllowExportPdf() == null ? 1 : 0));
        report.setAllowPrint(dto.getAllowPrint() != null && dto.getAllowPrint() ? 1 : (dto.getAllowPrint() == null ? 1 : 0));
        report.setPdfWatermark(dto.getPdfWatermark());
        report.setCreateBy(createBy);
        
        reportDefinitionMapper.insert(report);
        
        // 保存字段配置
        if (dto.getFields() != null && !dto.getFields().isEmpty()) {
            List<ReportField> fieldList = new ArrayList<>();
            int order = 0;
            for (ReportFieldDTO fieldDto : dto.getFields()) {
                ReportField field = new ReportField();
                field.setReportId(report.getId());
                field.setFieldName(fieldDto.getFieldName());
                field.setFieldLabel(StringUtils.hasText(fieldDto.getFieldLabel()) 
                    ? fieldDto.getFieldLabel() : fieldDto.getFieldName());
                field.setFieldType(fieldDto.getFieldType());
                field.setSortOrder(fieldDto.getSortOrder() != null ? fieldDto.getSortOrder() : order++);
                field.setIsVisible(fieldDto.getIsVisible() != null ? fieldDto.getIsVisible() : 1);
                field.setWidth(fieldDto.getWidth());
                field.setAlign(StringUtils.hasText(fieldDto.getAlign()) ? fieldDto.getAlign() : "left");
                field.setDictType(fieldDto.getDictType());
                fieldList.add(field);
            }
            if (!fieldList.isEmpty()) {
                reportFieldMapper.batchInsert(fieldList);
            }
        }
        
        return report.getId();
    }
    
    /**
     * 更新报表定义
     * 
     * @param dto 报表定义更新DTO
     */
    @Transactional
    public void updateReportDefinition(ReportDefinitionUpdateDTO dto) {
        ReportDefinition existing = reportDefinitionMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表定义不存在");
        }
        
        // 如果修改了SQL，需要验证
        if (StringUtils.hasText(dto.getSqlContent()) && !dto.getSqlContent().equals(existing.getSqlContent())) {
            SqlSecurityUtil.validateSql(SqlParamUtil.stripCustomParamsForTest(dto.getSqlContent()));
            
            // 验证数据源是否存在
            Long dataSourceId = dto.getDataSourceId() != null ? dto.getDataSourceId() : existing.getDataSourceId();
            DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
            if (dataSource == null) {
                throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
            }
        }
        
        // 如果修改了编码，检查是否已存在
        if (StringUtils.hasText(dto.getReportCode()) && !dto.getReportCode().equals(existing.getReportCode())) {
            ReportDefinition codeExisting = reportDefinitionMapper.selectByCode(dto.getReportCode());
            if (codeExisting != null && !codeExisting.getId().equals(dto.getId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "报表编码已存在: " + dto.getReportCode());
            }
        }
        
        // 更新报表定义
        ReportDefinition report = new ReportDefinition();
        report.setId(dto.getId());
        if (StringUtils.hasText(dto.getReportName())) {
            report.setReportName(dto.getReportName());
        }
        if (StringUtils.hasText(dto.getReportCode())) {
            report.setReportCode(dto.getReportCode());
        }
        if (dto.getDataSourceId() != null) {
            report.setDataSourceId(dto.getDataSourceId());
        }
        if (StringUtils.hasText(dto.getSqlContent())) {
            report.setSqlContent(SqlSecurityUtil.sanitizeSql(dto.getSqlContent()));
        }
        if (dto.getDescription() != null) {
            report.setDescription(dto.getDescription());
        }
        if (dto.getParams() != null) {
            report.setParams(dto.getParams());
        }
        if (dto.getStatus() != null) {
            report.setStatus(dto.getStatus());
        }
        if (dto.getReportType() != null) {
            report.setReportType(dto.getReportType());
        }
        if (dto.getAllowExportExcel() != null) {
            report.setAllowExportExcel(dto.getAllowExportExcel());
        }
        if (dto.getAllowExportPdf() != null) {
            report.setAllowExportPdf(dto.getAllowExportPdf());
        }
        if (dto.getAllowPrint() != null) {
            report.setAllowPrint(dto.getAllowPrint());
        }
        if (dto.getPdfWatermark() != null) {
            report.setPdfWatermark(dto.getPdfWatermark());
        }
        if (dto.getWatermarkType() != null) {
            report.setWatermarkType(dto.getWatermarkType());
        }
        
        reportDefinitionMapper.update(report);
        
        // 如果提供了字段配置，更新字段
        if (dto.getFields() != null) {
            // 删除原有字段
            reportFieldMapper.deleteByReportId(dto.getId());
            
            // 插入新字段
            if (!dto.getFields().isEmpty()) {
                List<ReportField> fieldList = new ArrayList<>();
                int order = 0;
                for (ReportFieldDTO fieldDto : dto.getFields()) {
                    ReportField field = new ReportField();
                    field.setReportId(dto.getId());
                    field.setFieldName(fieldDto.getFieldName());
                    field.setFieldLabel(StringUtils.hasText(fieldDto.getFieldLabel()) 
                        ? fieldDto.getFieldLabel() : fieldDto.getFieldName());
                    field.setFieldType(fieldDto.getFieldType());
                    field.setSortOrder(fieldDto.getSortOrder() != null ? fieldDto.getSortOrder() : order++);
                    field.setIsVisible(fieldDto.getIsVisible() != null ? fieldDto.getIsVisible() : 1);
                    field.setWidth(fieldDto.getWidth());
                    field.setAlign(StringUtils.hasText(fieldDto.getAlign()) ? fieldDto.getAlign() : "left");
                    field.setDictType(fieldDto.getDictType());
                    fieldList.add(field);
                }
                reportFieldMapper.batchInsert(fieldList);
            }
        }
    }
    
    /**
     * 删除报表定义
     * 
     * @param id 报表ID
     */
    @Transactional
    public void deleteReportDefinition(Long id) {
        ReportDefinition report = reportDefinitionMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表定义不存在");
        }
        // 删除字段（CASCADE会自动删除）
        reportFieldMapper.deleteByReportId(id);
        // 删除报表定义
        reportDefinitionMapper.delete(id);
    }
    
    /**
     * 复制报表定义
     * 
     * @param id 源报表ID
     * @return 新报表定义
     */
    @Transactional
    public ReportDefinition copyReportDefinition(Long id) {
        licenseLimitService.assertReportPageCreationAllowed(reportDefinitionMapper.countAll());

        ReportDefinition source = getReportDefinitionById(id);
        if (source == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表定义不存在");
        }

        // 生成新编码
        String newCode = source.getReportCode() + "_copy_" + System.currentTimeMillis();
        String newName = source.getReportName() + " (副本)";

        ReportDefinition copy = new ReportDefinition();
        copy.setReportName(newName);
        copy.setReportCode(newCode);
        copy.setDataSourceId(source.getDataSourceId());
        copy.setSqlContent(source.getSqlContent());
        copy.setDescription(source.getDescription());
        copy.setParams(source.getParams());
        copy.setStatus(0); // 默认禁用
        copy.setCreateBy(source.getCreateBy());

        reportDefinitionMapper.insert(copy);

        // 复制字段配置
        if (source.getFields() != null && !source.getFields().isEmpty()) {
            List<ReportField> fieldList = new ArrayList<>();
            for (ReportField srcField : source.getFields()) {
                ReportField field = new ReportField();
                field.setReportId(copy.getId());
                field.setFieldName(srcField.getFieldName());
                field.setFieldLabel(srcField.getFieldLabel());
                field.setFieldType(srcField.getFieldType());
                field.setSortOrder(srcField.getSortOrder());
                field.setIsVisible(srcField.getIsVisible());
                field.setWidth(srcField.getWidth());
                field.setAlign(srcField.getAlign());
                field.setDictType(srcField.getDictType());
                fieldList.add(field);
            }
            if (!fieldList.isEmpty()) {
                reportFieldMapper.batchInsert(fieldList);
            }
        }

        copy.setFields(reportFieldMapper.selectByReportId(copy.getId()));
        return copy;
    }

    /**
     * 执行报表SQL查询（带分页）
     * 
     * @param reportId 报表ID
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 查询结果
     */
    public List<Map<String, Object>> executeReportQuery(Long reportId, Integer page, Integer pageSize) {
        ReportDefinition report = getReportDefinitionById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表定义不存在");
        }
        if (report.getStatus() == null || report.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表已禁用");
        }
        
        return executeSql(report.getDataSourceId(), report.getSqlContent(), page, pageSize, report.getFields(), null, null);
    }
    
    /**
     * 执行报表SQL查询（带分页和总数）
     * 
     * @param reportId 报表ID
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @param filters 筛选条件（JSON字符串）
     * @return 分页查询结果（包含总数）
     */
    public com.dataplatform.common.PageResult<Map<String, Object>> executeReportQueryWithPagination(
            Long reportId, Integer page, Integer pageSize, String filters, String customParams) {
        ReportDefinition report = getReportDefinitionById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表定义不存在");
        }
        if (report.getStatus() == null || report.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表已禁用");
        }
        
        List<Map<String, Object>> list = executeSql(report.getDataSourceId(), report.getSqlContent(), 
                                                     page, pageSize, report.getFields(), filters, customParams);
        long total = countSqlResult(report.getDataSourceId(), report.getSqlContent(), filters, customParams);
        
        return new com.dataplatform.common.PageResult<>(list, total);
    }
    
    /**
     * 执行报表SQL查询（根据编码）
     * 
     * @param reportCode 报表编码
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @param filters 筛选条件（JSON字符串）
     * @return 查询结果
     */
    public List<Map<String, Object>> executeReportQueryByCode(String reportCode, Integer page, Integer pageSize, String filters) {
        ReportDefinition report = getReportDefinitionByCode(reportCode);
        if (report == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表定义不存在");
        }
        if (report.getStatus() == null || report.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表已禁用");
        }
        
        return executeSql(report.getDataSourceId(), report.getSqlContent(), page, pageSize, report.getFields(), filters, null);
    }
    
    /**
     * 执行报表SQL查询（根据编码，带分页和总数）
     * 
     * @param reportCode 报表编码
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @param filters 筛选条件（JSON字符串）
     * @return 分页查询结果（包含总数）
     */
    public com.dataplatform.common.PageResult<Map<String, Object>> executeReportQueryByCodeWithPagination(
            String reportCode, Integer page, Integer pageSize, String filters, String customParams) {
        ReportDefinition report = getReportDefinitionByCode(reportCode);
        if (report == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表定义不存在");
        }
        if (report.getStatus() == null || report.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表已禁用");
        }
        
        List<Map<String, Object>> list = executeSql(report.getDataSourceId(), report.getSqlContent(), 
                                                     page, pageSize, report.getFields(), filters, customParams);
        long total = countSqlResult(report.getDataSourceId(), report.getSqlContent(), filters, customParams);
        
        return new com.dataplatform.common.PageResult<>(list, total);
    }
    
    /**
     * 测试SQL并获取字段信息
     * 
     * @param dataSourceId 数据源ID
     * @param sql SQL语句
     * @return 字段列表
     */
    public List<ReportFieldDTO> testSqlAndGetFields(Long dataSourceId, String sql) {
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        // 验证SQL安全性（对去除 ${} 后的SQL进行验证）
        String cleanSqlForValidation = SqlParamUtil.stripCustomParamsForTest(sql);
        SqlSecurityUtil.validateSql(cleanSqlForValidation);
        
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        
        try {
            Class.forName(driver);
            List<ReportFieldDTO> fields = new ArrayList<>();
            
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                // 限制只查询一条记录以获取元数据（将 ${} 替换为 NULL 以便测试执行）
                String testSql = addLimitClause(cleanSqlForValidation, dataSource.getDbType(), 1);
                
                // 调试日志：输出实际执行的SQL
                log.info("【测试SQL】原始SQL: {}", sql);
                log.info("【测试SQL】清理后SQL: {}", cleanSqlForValidation);
                log.info("【测试SQL】最终测试SQL: {}", testSql);
                
                try (PreparedStatement ps = conn.prepareStatement(testSql);
                     ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    // 尝试获取字段注释（MySQL支持）
                    Map<String, String> columnComments = new HashMap<>();
                    if ("mysql".equalsIgnoreCase(dataSource.getDbType())) {
                        try {
                            String commentSql = "SELECT COLUMN_NAME, COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS " +
                                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME IN (SELECT DISTINCT TABLE_NAME FROM (" + 
                                sql + " LIMIT 1) AS temp_table)";
                            // 简化处理：尝试从SQL中提取表名
                            String tableName = extractTableName(sql);
                            if (StringUtils.hasText(tableName)) {
                                commentSql = "SELECT COLUMN_NAME, COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS " +
                                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
                                try (PreparedStatement commentPs = conn.prepareStatement(commentSql)) {
                                    commentPs.setString(1, dataSource.getDatabase());
                                    commentPs.setString(2, tableName);
                                    try (ResultSet commentRs = commentPs.executeQuery()) {
                                        while (commentRs.next()) {
                                            String colName = commentRs.getString("COLUMN_NAME");
                                            String comment = commentRs.getString("COLUMN_COMMENT");
                                            if (StringUtils.hasText(comment)) {
                                                columnComments.put(colName, comment);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.debug("获取列注释失败，使用默认逻辑: {}", e.getMessage());
                        }
                    }
                    
                    for (int i = 1; i <= columnCount; i++) {
                        ReportFieldDTO field = new ReportFieldDTO();
                        String columnName = metaData.getColumnName(i);
                        String columnLabel = metaData.getColumnLabel(i);
                        
                        field.setFieldName(columnName);
                        
                        // 优先使用数据库字段注释，其次使用列标签，最后使用列名
                        String displayLabel = columnComments.getOrDefault(columnName, null);
                        if (!StringUtils.hasText(displayLabel)) {
                            if (StringUtils.hasText(columnLabel) && !columnLabel.equals(columnName)) {
                                displayLabel = columnLabel;
                            } else {
                                displayLabel = columnName;
                            }
                        }
                        field.setFieldLabel(displayLabel);
                        field.setFieldType(metaData.getColumnTypeName(i));
                        field.setSortOrder(i - 1);
                        field.setIsVisible(1);
                        field.setAlign("left");
                        fields.add(field);
                    }
                }
            }
            
            return fields;
        } catch (ClassNotFoundException e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                "数据库驱动加载失败: " + e.getMessage());
        } catch (SQLException e) {
            String errorMsg = e.getMessage();
            // 提供更友好的错误信息
            if (errorMsg != null && errorMsg.contains("Table") && errorMsg.contains("doesn't exist")) {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "SQL执行失败：表不存在，请检查SQL语句中的表名是否正确");
            } else if (errorMsg != null && errorMsg.contains("Column")) {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "SQL执行失败：列不存在，请检查SQL语句中的字段名是否正确");
            } else if (errorMsg != null && errorMsg.contains("syntax")) {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "SQL执行失败：SQL语法错误，请检查SQL语句是否正确");
            } else {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "SQL执行失败: " + errorMsg);
            }
        } catch (BusinessException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "测试SQL失败: " + e.getMessage());
        }
    }
    
    // ==================== 自定义参数 ${} 替换（委托 SqlParamUtil）====================
    
    /**
     * 将筛选条件插入到SQL中
     * 正确处理 ORDER BY、GROUP BY、HAVING、LIMIT 等子句
     * 
     * @param sql 原始SQL
     * @param whereClause 筛选条件的WHERE子句（包含 " WHERE " 前缀）
     * @return 带筛选条件的SQL
     */
    private String insertFilterConditions(String sql, String whereClause) {
        if (whereClause == null || whereClause.isEmpty()) {
            return sql;
        }
        
        String upperSql = sql.toUpperCase().trim();
        
        // 找到需要在其之前插入条件的关键字位置
        // 优先级：ORDER BY > LIMIT > 末尾
        int insertPosition = sql.length();
        
        // 查找 ORDER BY 位置（不在子查询中的）
        int orderByPos = findKeywordPosition(upperSql, "ORDER BY");
        if (orderByPos > 0) {
            insertPosition = orderByPos;
        }
        
        // 查找 LIMIT 位置（如果没有 ORDER BY）
        if (insertPosition == sql.length()) {
            int limitPos = findKeywordPosition(upperSql, "LIMIT");
            if (limitPos > 0) {
                insertPosition = limitPos;
            }
        }
        
        // 构建新的SQL
        String beforeInsert = sql.substring(0, insertPosition).trim();
        String afterInsert = insertPosition < sql.length() ? " " + sql.substring(insertPosition).trim() : "";
        
        // 检查原SQL是否已包含WHERE
        if (upperSql.contains(" WHERE ")) {
            // 提取AND条件部分（去掉 " WHERE " 前缀）
            String andClause = whereClause.substring(7); // 去掉 " WHERE "
            return beforeInsert + " AND " + andClause + afterInsert;
        } else {
            return beforeInsert + whereClause + afterInsert;
        }
    }
    
    /**
     * 查找SQL关键字位置（排除子查询中的关键字）
     * 
     * @param upperSql 大写的SQL
     * @param keyword 关键字
     * @return 关键字位置，如果不存在返回-1
     */
    private int findKeywordPosition(String upperSql, String keyword) {
        int pos = upperSql.lastIndexOf(keyword);
        if (pos < 0) {
            return -1;
        }
        
        // 检查是否在子查询中（通过计算括号数量）
        int openParens = 0;
        for (int i = 0; i < pos; i++) {
            char c = upperSql.charAt(i);
            if (c == '(') openParens++;
            else if (c == ')') openParens--;
        }
        
        // 如果括号不平衡，说明关键字在子查询中
        if (openParens > 0) {
            return -1;
        }
        
        return pos;
    }
    
    /**
     * 执行SQL查询
     * 
     * @param dataSourceId 数据源ID
     * @param sql SQL语句
     * @param page 页码
     * @param pageSize 每页大小
     * @param fields 字段配置（用于过滤可见字段）
     * @param filters 筛选条件（JSON字符串）
     * @return 查询结果
     */
    private List<Map<String, Object>> executeSql(Long dataSourceId, String sql, 
                                                   Integer page, Integer pageSize, 
                                                   List<ReportField> fields, String filters,
                                                   String customParams) {
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        // 验证SQL安全性（先去除 ${} 参数再验证）
        SqlSecurityUtil.validateSql(SqlParamUtil.stripCustomParamsForTest(sql));
        
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        
        try {
            Class.forName(driver);
            List<Map<String, Object>> result = new ArrayList<>();
            
            // 构建可见字段映射
            Map<String, ReportField> fieldMap = new HashMap<>();
            if (fields != null) {
                for (ReportField field : fields) {
                    if (field.getIsVisible() != null && field.getIsVisible() == 1) {
                        fieldMap.put(field.getFieldName(), field);
                    }
                }
            }
            
            // 1) 自定义参数替换: ${param_name} -> ?
            Map<String, String> paramMap = SqlParamUtil.parseParamsJson(customParams);
            Object[] paramResult = SqlParamUtil.replaceCustomParams(sql, paramMap);
            String paramReplacedSql = (String) paramResult[0];
            @SuppressWarnings("unchecked")
            List<Object> customParamValues = (List<Object>) paramResult[1];
            
            // 2) 解析和应用筛选条件
            List<com.dataplatform.common.dto.FilterCondition> filterList = com.dataplatform.common.util.FilterUtil.parseFilters(filters);
            com.dataplatform.common.util.FilterUtil.FilterWhereClause whereClause = com.dataplatform.common.util.FilterUtil.buildDynamicWhereClause(filterList);
            
            // 构建带筛选条件的SQL（正确处理ORDER BY等子句）
            String filteredSql = insertFilterConditions(paramReplacedSql, whereClause.getWhereClause());
            
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                // 添加分页限制
                String pageSql = addLimitClause(filteredSql, dataSource.getDbType(), page, pageSize);
                
                try (PreparedStatement ps = conn.prepareStatement(pageSql)) {
                    // 设置查询超时和优化参数
                    ps.setQueryTimeout(queryTimeout);
                    ps.setFetchSize(500);
                    
                    // 设置自定义参数（先于 filter 参数）
                    int paramIndex = 1;
                    for (Object val : customParamValues) {
                        ps.setObject(paramIndex++, val);
                    }
                    
                    // 设置筛选条件的参数
                    List<Object> parameters = whereClause.getParameters();
                    for (int i = 0; i < parameters.size(); i++) {
                        ps.setObject(paramIndex++, parameters.get(i));
                    }
                    
                    log.debug("执行报表查询SQL: {}", pageSql);
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        
                        while (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                String columnName = metaData.getColumnName(i);
                                // 如果配置了字段过滤，只返回可见字段
                                if (fieldMap.isEmpty() || fieldMap.containsKey(columnName)) {
                                    Object value = rs.getObject(i);
                                    row.put(columnName, value);
                                }
                            }
                            result.add(row);
                        }
                    }
                }
            }
            
            return result;
        } catch (ClassNotFoundException e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                "数据库驱动加载失败: " + e.getMessage());
        } catch (SQLException e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                "SQL执行失败: " + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "执行SQL失败: " + e.getMessage());
        }
    }
    
    /**
     * 添加分页限制子句
     * 
     * @param sql 原始SQL
     * @param dbType 数据库类型
     * @param page 页码
     * @param pageSize 每页大小
     * @return 带分页的SQL
     */
    private String addLimitClause(String sql, String dbType, Integer page, Integer pageSize) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > 10000) {
            pageSize = 10000; // 限制最大每页大小
        }
        
        int offset = (page - 1) * pageSize;
        
        // 移除原SQL中的LIMIT子句（如果存在），以便应用新的分页
        String cleanSql = sql.trim();
        // 移除末尾分号
        if (cleanSql.endsWith(";")) {
            cleanSql = cleanSql.substring(0, cleanSql.length() - 1).trim();
        }
        // 移除已有的LIMIT子句（MySQL格式: LIMIT n 或 LIMIT n, m 或 LIMIT n OFFSET m）
        cleanSql = cleanSql.replaceAll("(?i)\\s+LIMIT\\s+\\d+\\s*(,\\s*\\d+)?\\s*$", "");
        cleanSql = cleanSql.replaceAll("(?i)\\s+LIMIT\\s+\\d+\\s+OFFSET\\s+\\d+\\s*$", "");
        
        String dbTypeLower = dbType.toLowerCase();
        if ("mysql".equals(dbTypeLower)) {
            return cleanSql + " LIMIT " + offset + ", " + pageSize;
        } else if ("postgresql".equals(dbTypeLower)) {
            return cleanSql + " LIMIT " + pageSize + " OFFSET " + offset;
        } else if ("oracle".equals(dbTypeLower)) {
            // Oracle使用ROWNUM分页
            int endRow = offset + pageSize;
            return "SELECT * FROM (SELECT ROWNUM rn, t.* FROM (" + cleanSql + ") t WHERE ROWNUM <= " + endRow + ") WHERE rn > " + offset;
        } else if ("sqlserver".equals(dbTypeLower)) {
            // SQL Server使用ROW_NUMBER分页
            return "SELECT * FROM (SELECT ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn, * FROM (" + cleanSql + ") t) t WHERE rn > " + offset + " AND rn <= " + (offset + pageSize);
        } else {
            // 默认使用MySQL语法
            return cleanSql + " LIMIT " + offset + ", " + pageSize;
        }
    }
    
    /**
     * 统计SQL查询结果总数
     * 
     * @param dataSourceId 数据源ID
     * @param sql SQL语句
     * @param filters 筛选条件（JSON字符串）
     * @return 总数
     */
    private long countSqlResult(Long dataSourceId, String sql, String filters, String customParams) {
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        // 验证SQL安全性（先去除 ${} 参数再验证）
        SqlSecurityUtil.validateSql(SqlParamUtil.stripCustomParamsForTest(sql));
        
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        
        try {
            Class.forName(driver);
            
            // 1) 自定义参数替换: ${param_name} -> ?
            Map<String, String> paramMap = SqlParamUtil.parseParamsJson(customParams);
            Object[] paramResult = SqlParamUtil.replaceCustomParams(sql, paramMap);
            String paramReplacedSql = (String) paramResult[0];
            @SuppressWarnings("unchecked")
            List<Object> customParamValues = (List<Object>) paramResult[1];
            
            // 2) 解析和应用筛选条件
            List<com.dataplatform.common.dto.FilterCondition> filterList = com.dataplatform.common.util.FilterUtil.parseFilters(filters);
            com.dataplatform.common.util.FilterUtil.FilterWhereClause whereClause = com.dataplatform.common.util.FilterUtil.buildDynamicWhereClause(filterList);
            
            // 构建带筛选条件的SQL（正确处理ORDER BY等子句）
            String filteredSql = insertFilterConditions(paramReplacedSql, whereClause.getWhereClause());
            
            // 构建统计SQL（将SELECT * 替换为 SELECT COUNT(*))
            String countSql = buildCountSql(filteredSql, dataSource.getDbType());
            
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    // 设置查询超时
                    ps.setQueryTimeout(queryTimeout);
                    
                    // 设置自定义参数（先于 filter 参数）
                    int paramIndex = 1;
                    for (Object val : customParamValues) {
                        ps.setObject(paramIndex++, val);
                    }
                    
                    // 设置筛选条件的参数
                    List<Object> parameters = whereClause.getParameters();
                    for (int i = 0; i < parameters.size(); i++) {
                        ps.setObject(paramIndex++, parameters.get(i));
                    }
                    
                    log.debug("执行报表计数SQL: {}", countSql);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong(1);
                        }
                    }
                }
            }
            
            return 0;
        } catch (ClassNotFoundException e) {
            log.error("数据库驱动加载失败", e);
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                "数据库驱动加载失败: " + e.getMessage());
        } catch (SQLException e) {
            // 统计失败时记录日志，返回-1表示无法统计
            log.warn("统计查询失败: {}", e.getMessage());
            return -1;
        } catch (Exception e) {
            log.warn("统计查询异常: {}", e.getMessage());
            return -1;
        }
    }
    
    /**
     * 构建COUNT SQL语句
     * 
     * @param sql 原始SQL
     * @param dbType 数据库类型
     * @return COUNT SQL
     */
    private String buildCountSql(String sql, String dbType) {
        String upperSql = sql.toUpperCase().trim();
        
        // 如果SQL包含LIMIT、GROUP BY、聚合函数或ROWNUM，使用子查询包装
        // 这样可以正确统计带LIMIT的SQL结果数量
        if (upperSql.contains("LIMIT") || upperSql.contains("ROWNUM") || 
            upperSql.contains("ROW_NUMBER") || upperSql.contains("TOP ") ||
            upperSql.contains("GROUP BY") || upperSql.contains("COUNT(") || 
            upperSql.contains("SUM(") || upperSql.contains("AVG(") ||
            upperSql.contains("MAX(") || upperSql.contains("MIN(")) {
            return "SELECT COUNT(*) FROM (" + sql + ") AS count_table";
        }
        
        // 移除SELECT和FROM之间的内容，替换为COUNT(*)
        // 简单实现：找到FROM关键字，替换前面的内容
        int fromIndex = upperSql.indexOf("FROM");
        if (fromIndex > 0) {
            return "SELECT COUNT(*) " + sql.substring(fromIndex);
        }
        
        // 如果找不到FROM，使用子查询
        return "SELECT COUNT(*) FROM (" + sql + ") AS count_table";
    }
    
    /**
     * 从SQL中提取表名（简单实现）
     * 
     * @param sql SQL语句
     * @return 表名，如果无法提取则返回null
     */
    private String extractTableName(String sql) {
        if (!StringUtils.hasText(sql)) {
            return null;
        }
        
        // 移除注释
        String cleanSql = sql.replaceAll("(?m)(--.*$|/\\*[\\s\\S]*?\\*/|#.*$)", " ");
        cleanSql = cleanSql.toUpperCase().trim();
        
        // 简单的正则匹配：SELECT ... FROM table_name
        Pattern pattern = Pattern.compile("FROM\\s+([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(cleanSql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 尝试匹配 JOIN 语句
        pattern = Pattern.compile("JOIN\\s+([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(cleanSql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * 添加LIMIT子句（仅限制条数，用于测试）
     * 
     * @param sql 原始SQL
     * @param dbType 数据库类型
     * @param limit 限制条数
     * @return 带LIMIT的SQL
     */
    private String addLimitClause(String sql, String dbType, int limit) {
        String upperSql = sql.toUpperCase().trim();
        
        // 检查SQL是否已包含LIMIT子句
        if (upperSql.contains("LIMIT") || upperSql.contains("ROWNUM") || upperSql.contains("ROW_NUMBER")) {
            return sql;
        }
        
        String dbTypeLower = dbType.toLowerCase();
        if ("mysql".equals(dbTypeLower)) {
            return sql + " LIMIT " + limit;
        } else if ("postgresql".equals(dbTypeLower)) {
            return sql + " LIMIT " + limit;
        } else if ("oracle".equals(dbTypeLower)) {
            return "SELECT * FROM (" + sql + ") WHERE ROWNUM <= " + limit;
        } else if ("sqlserver".equals(dbTypeLower)) {
            return "SELECT TOP " + limit + " * FROM (" + sql + ") t";
        } else {
            return sql + " LIMIT " + limit;
        }
    }
    
    // Excel单Sheet最大行数（保留1行给表头）
    private static final int MAX_ROWS_PER_SHEET = 1000000;
    
    /**
     * 导出报表数据为Excel（根据ID）
     * 支持超过100万行数据自动分Sheet
     * 
     * @param reportId 报表ID
     * @param filters 筛选条件（JSON字符串，可选）
     * @return Excel文件字节数组
     * @throws BusinessException 导出失败时抛出
     */
    public byte[] exportReport(Long reportId, String filters, String customParams) {
        ReportDefinition report = getReportDefinitionById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表定义不存在");
        }
        if (report.getStatus() == null || report.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表已禁用");
        }

        long estimatedRows = countSqlResult(report.getDataSourceId(), report.getSqlContent(), filters, customParams);
        if (estimatedRows >= 0) {
            licenseLimitService.assertReportExportRowsAllowed(estimatedRows);
        }
        
        DataSource dataSource = dataSourceMapper.selectById(report.getDataSourceId());
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        
        // 验证SQL安全性（先去除 ${} 参数再验证）
        SqlSecurityUtil.validateSql(SqlParamUtil.stripCustomParamsForTest(report.getSqlContent()));
        
        // 使用SXSSFWorkbook支持百万级数据导出
        SXSSFWorkbook workbook = new SXSSFWorkbook(500);
        workbook.setCompressTempFiles(true);
        
        try {
            Class.forName(driver);
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                // 解析和应用筛选条件
                List<com.dataplatform.common.dto.FilterCondition> filterList = com.dataplatform.common.util.FilterUtil.parseFilters(filters);
                com.dataplatform.common.util.FilterUtil.FilterWhereClause whereClause = com.dataplatform.common.util.FilterUtil.buildDynamicWhereClause(filterList);
                
                // 构建带筛选条件的SQL
                String sql = report.getSqlContent();
                
                // 处理自定义参数 ${}
                List<Object> customParamValues = new ArrayList<>();
                if (customParams != null && !customParams.isBlank()) {
                    Map<String, String> paramMap = SqlParamUtil.parseParamsJson(customParams);
                    Object[] paramResult = SqlParamUtil.replaceCustomParams(sql, paramMap);
                    sql = (String) paramResult[0];
                    @SuppressWarnings("unchecked")
                    List<Object> cpv = (List<Object>) paramResult[1];
                    customParamValues = cpv;
                } else {
                    // 没有传参数时，将 ${} 替换为 NULL 避免语法错误
                    sql = SqlParamUtil.stripCustomParamsForTest(sql);
                }
                
                String filteredSql = insertFilterConditions(sql, whereClause.getWhereClause());
                
                // 设置流式查询模式（MySQL需要）
                conn.setAutoCommit(false);
                
                try (PreparedStatement ps = conn.prepareStatement(filteredSql, 
                        ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                    ps.setQueryTimeout(queryTimeout * 2);
                    ps.setFetchSize(Integer.MIN_VALUE);
                    
                    // 先设置自定义参数
                    int paramIndex = 1;
                    for (Object val : customParamValues) {
                        ps.setObject(paramIndex++, val);
                    }
                    // 再设置筛选条件参数
                    List<Object> parameters = whereClause.getParameters();
                    for (int i = 0; i < parameters.size(); i++) {
                        ps.setObject(paramIndex++, parameters.get(i));
                    }
                    
                    log.info("开始导出报表数据，SQL: {}", filteredSql);
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        
                        // 获取字段配置
                        List<ReportField> fields = reportFieldMapper.selectByReportId(reportId);
                        Map<String, String> fieldLabelMap = new HashMap<>();
                        for (ReportField field : fields) {
                            if (StringUtils.hasText(field.getFieldLabel())) {
                                fieldLabelMap.put(field.getFieldName(), field.getFieldLabel());
                            }
                        }
                        
                        // 构建表头标签数组
                        String[] headerLabels = new String[columnCount];
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            String headerLabel = fieldLabelMap.getOrDefault(columnName, null);
                            if (!StringUtils.hasText(headerLabel)) {
                                String columnLabel = metaData.getColumnLabel(i);
                                headerLabel = StringUtils.hasText(columnLabel) && !columnLabel.equals(columnName) 
                                    ? columnLabel : columnName;
                            }
                            headerLabels[i - 1] = headerLabel;
                        }
                        
                        // 创建表头样式
                        CellStyle headerStyle = workbook.createCellStyle();
                        Font headerFont = workbook.createFont();
                        headerFont.setBold(true);
                        headerStyle.setFont(headerFont);
                        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        
                        // 初始化第一个Sheet
                        int sheetIndex = 1;
                        Sheet currentSheet = workbook.createSheet("数据_" + sheetIndex);
                        createHeaderRow(currentSheet, headerLabels, headerStyle);
                        
                        int rowNumInSheet = 1;  // 当前Sheet中的行号（0是表头）
                        long totalRows = 0;
                        
                        while (rs.next()) {
                            // 检查是否需要创建新Sheet
                            if (rowNumInSheet >= MAX_ROWS_PER_SHEET) {
                                // 刷新当前Sheet
                                ((org.apache.poi.xssf.streaming.SXSSFSheet) currentSheet).flushRows();
                                
                                // 创建新Sheet
                                sheetIndex++;
                                currentSheet = workbook.createSheet("数据_" + sheetIndex);
                                createHeaderRow(currentSheet, headerLabels, headerStyle);
                                rowNumInSheet = 1;
                                log.info("导出进度：已处理 {} 行，创建第 {} 个Sheet", totalRows, sheetIndex);
                            }
                            
                            Row row = currentSheet.createRow(rowNumInSheet++);
                            for (int i = 1; i <= columnCount; i++) {
                                Cell cell = row.createCell(i - 1);
                                Object value = rs.getObject(i);
                                if (value != null) {
                                    String strValue = value.toString();
                                    // 限制单元格内容长度，避免超出Excel限制
                                    if (strValue.length() > 32767) {
                                        strValue = strValue.substring(0, 32760) + "...[截断]";
                                    }
                                    cell.setCellValue(strValue);
                                }
                            }
                            
                            totalRows++;
                            licenseLimitService.assertReportExportRowsAllowed(totalRows);
                            // 每1000行刷新一次
                            if (totalRows % 1000 == 0) {
                                ((org.apache.poi.xssf.streaming.SXSSFSheet) currentSheet).flushRows();
                            }
                        }
                        
                        log.info("导出完成，总计 {} 行，共 {} 个Sheet", totalRows, sheetIndex);
                    }
                }
            }
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (ClassNotFoundException e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, "数据库驱动加载失败: " + e.getMessage());
        } catch (SQLException e) {
            log.error("数据库查询失败", e);
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, "数据库查询失败: " + e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("导出数据失败", e);
            throw new BusinessException(ErrorCode.ERROR, "导出数据失败: " + e.getMessage());
        } finally {
            try {
                workbook.dispose();
                workbook.close();
            } catch (Exception e) {
                log.trace("关闭SXSSFWorkbook异常: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 创建表头行
     */
    private void createHeaderRow(Sheet sheet, String[] headerLabels, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headerLabels.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headerLabels[i]);
            cell.setCellStyle(headerStyle);
        }
    }
    
    /**
     * 导出报表数据为Excel（根据编码）
     * 
     * @param reportCode 报表编码
     * @param filters 筛选条件（JSON字符串，可选）
     * @return Excel文件字节数组
     * @throws BusinessException 导出失败时抛出
     */
    public byte[] exportReportByCode(String reportCode, String filters, String customParams) {
        ReportDefinition report = getReportDefinitionByCode(reportCode);
        if (report == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表定义不存在");
        }
        return exportReport(report.getId(), filters, customParams);
    }
    
    /**
     * 导出报表数据为PDF（根据ID）
     */
    public byte[] exportReportAsPdf(Long reportId, String filters, String customParams) {
        ReportDefinition report = getReportDefinitionById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表定义不存在");
        }
        if (report.getStatus() == null || report.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表已禁用");
        }
        
        DataSource dataSource = dataSourceMapper.selectById(report.getDataSourceId());
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        SqlSecurityUtil.validateSql(SqlParamUtil.stripCustomParamsForTest(report.getSqlContent()));
        
        try {
            Class.forName(driver);
            // 获取数据（限制PDF最多导出pdfMaxRows行）
            List<Map<String, Object>> data = executeSql(report.getDataSourceId(), report.getSqlContent(), 1, pdfMaxRows + 1, report.getFields(), filters, customParams);
            boolean truncated = data.size() > pdfMaxRows;
            if (truncated) {
                data = data.subList(0, pdfMaxRows);
            }
            
            // 获取字段配置
            List<ReportField> fields = reportFieldMapper.selectByReportId(reportId);
            Map<String, String> fieldLabelMap = new java.util.LinkedHashMap<>();
            if (fields != null && !fields.isEmpty()) {
                for (ReportField field : fields) {
                    if (field.getIsVisible() != null && field.getIsVisible() == 1) {
                        fieldLabelMap.put(field.getFieldName(), 
                            StringUtils.hasText(field.getFieldLabel()) ? field.getFieldLabel() : field.getFieldName());
                    }
                }
            }
            // 如果没有字段配置，从数据中获取列名
            if (fieldLabelMap.isEmpty() && !data.isEmpty()) {
                for (String key : data.get(0).keySet()) {
                    fieldLabelMap.put(key, key);
                }
            }
            
            List<String> columnKeys = new ArrayList<>(fieldLabelMap.keySet());
            List<String> columnLabels = new ArrayList<>(fieldLabelMap.values());
            
            // 生成PDF
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate(), 20, 20, 30, 30);
            com.lowagie.text.pdf.PdfWriter writer = com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            
            // 解析水印文字（支持 watermarkType）
            String watermarkText = resolveWatermarkText(report.getWatermarkType(), report.getPdfWatermark());
            if (watermarkText != null && !watermarkText.trim().isEmpty()) {
                writer.setPageEvent(new com.lowagie.text.pdf.PdfPageEventHelper() {
                    @Override
                    public void onEndPage(com.lowagie.text.pdf.PdfWriter w, com.lowagie.text.Document doc) {
                        try {
                            com.lowagie.text.pdf.PdfContentByte canvas = w.getDirectContentUnder();
                            com.lowagie.text.pdf.BaseFont bf = com.lowagie.text.pdf.BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED);
                            canvas.saveState();
                            canvas.setGState(new com.lowagie.text.pdf.PdfGState() {{ setFillOpacity(0.15f); }});
                            canvas.beginText();
                            canvas.setFontAndSize(bf, 50);
                            canvas.setColorFill(new java.awt.Color(180, 180, 180));
                            // 在页面多个位置添加水印
                            float pageWidth = doc.getPageSize().getWidth();
                            float pageHeight = doc.getPageSize().getHeight();
                            canvas.showTextAligned(com.lowagie.text.Element.ALIGN_CENTER, watermarkText, pageWidth / 4, pageHeight / 3, 45);
                            canvas.showTextAligned(com.lowagie.text.Element.ALIGN_CENTER, watermarkText, pageWidth * 3 / 4, pageHeight / 3, 45);
                            canvas.showTextAligned(com.lowagie.text.Element.ALIGN_CENTER, watermarkText, pageWidth / 2, pageHeight * 2 / 3, 45);
                            canvas.endText();
                            canvas.restoreState();
                        } catch (Exception e) {
                            log.warn("添加PDF水印失败", e);
                        }
                    }
                });
            }
            
            document.open();
            
            // 使用内置字体支持中文
            com.lowagie.text.pdf.BaseFont bfChinese = com.lowagie.text.pdf.BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED);
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(bfChinese, 16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headerFont = new com.lowagie.text.Font(bfChinese, 9, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font cellFont = new com.lowagie.text.Font(bfChinese, 8, com.lowagie.text.Font.NORMAL);
            
            // 标题
            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph(report.getReportName(), titleFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            title.setSpacingAfter(15);
            document.add(title);
            
            // 表格
            int colCount = Math.min(columnKeys.size(), 20); // PDF表格最多20列
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(colCount);
            table.setWidthPercentage(100);
            table.setSpacingBefore(5);
            
            // 表头
            for (int i = 0; i < colCount; i++) {
                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(columnLabels.get(i), headerFont));
                cell.setBackgroundColor(new java.awt.Color(220, 230, 241));
                cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }
            
            // 数据行
            for (Map<String, Object> row : data) {
                for (int i = 0; i < colCount; i++) {
                    Object val = row.get(columnKeys.get(i));
                    String text = val != null ? val.toString() : "";
                    if (text.length() > 100) text = text.substring(0, 97) + "...";
                    com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(text, cellFont));
                    cell.setPadding(4);
                    table.addCell(cell);
                }
            }
            
            document.add(table);
            
            // 添加页脚信息
            com.lowagie.text.Font footerFont = new com.lowagie.text.Font(bfChinese, 8, com.lowagie.text.Font.ITALIC);
            String footerText = "共 " + data.size() + " 条记录 | 导出时间: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            if (truncated) {
                footerText += " | ⚠ 数据量超过PDF导出上限（" + pdfMaxRows + "行），仅显示前" + pdfMaxRows + "条，完整数据请使用Excel导出";
            }
            com.lowagie.text.Paragraph footer = new com.lowagie.text.Paragraph(footerText, footerFont);
            footer.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            footer.setSpacingBefore(10);
            document.add(footer);
            
            document.close();
            return out.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("PDF导出失败", e);
            throw new BusinessException(ErrorCode.ERROR, "PDF导出失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据水印类型解析最终水印文字
     * @param watermarkType 水印类型: none-无水印, user_ip-用户名_IP, custom-自定义文本
     * @param customText 自定义水印文字（仅 type=custom 时使用）
     * @return 解析后的水印文字
     */
    private String resolveWatermarkText(String watermarkType, String customText) {
        if (watermarkType == null || "none".equals(watermarkType)) {
            return "";
        }
        
        if ("user_ip".equals(watermarkType)) {
            // 获取当前用户名
            String username = "未知用户";
            try {
                if (cn.dev33.satoken.stp.StpUtil.isLogin()) {
                    Object loginId = cn.dev33.satoken.stp.StpUtil.getLoginId();
                    if (loginId != null) {
                        com.dataplatform.system.entity.User user = userMapper.selectById(Long.parseLong(loginId.toString()));
                        if (user != null) {
                            username = user.getNickname() != null ? user.getNickname() : user.getUsername();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取用户信息失败", e);
            }
            
            // 获取IP地址（从请求上下文）
            String ip = "未知IP";
            try {
                org.springframework.web.context.request.ServletRequestAttributes attributes = 
                    (org.springframework.web.context.request.ServletRequestAttributes) 
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    jakarta.servlet.http.HttpServletRequest request = attributes.getRequest();
                    ip = request.getHeader("X-Forwarded-For");
                    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                        ip = request.getHeader("X-Real-IP");
                    }
                    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                        ip = request.getRemoteAddr();
                    }
                    // 处理多个IP的情况（取第一个）
                    if (ip != null && ip.contains(",")) {
                        ip = ip.split(",")[0].trim();
                    }
                }
            } catch (Exception e) {
                log.warn("获取IP地址失败", e);
            }
            
            return username + "_" + ip;
        }
        
        if ("custom".equals(watermarkType)) {
            return customText != null ? customText : "";
        }
        
        // 兼容旧数据：如果 watermarkType 不是已知类型，当作自定义文本
        return customText != null ? customText : "";
    }
}

