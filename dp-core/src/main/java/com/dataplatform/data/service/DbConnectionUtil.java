package com.dataplatform.data.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 数据库连接工具类
 * 提供数据库连接相关的工具方法，包括构建JDBC URL、获取驱动类名、测试连接等
 * 
 * @author dataplatform
 */
@Component
public class DbConnectionUtil {
    
    @Value("${spring.datasource.url}")
    private String localDbUrl;
    
    @Value("${spring.datasource.username}")
    private String localDbUsername;
    
    @Value("${spring.datasource.password}")
    private String localDbPassword;

    /**
     * 构建JDBC URL
     * 根据数据源信息构建对应数据库类型的JDBC连接URL
     * 
     * @param dataSource 数据源信息
     * @return JDBC URL
     * @throws BusinessException 不支持的数据库类型或参数错误时抛出
     */
    public String buildJdbcUrl(DataSource dataSource) {
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源信息不能为空");
        }
        
        String dbType = dataSource.getDbType();
        String host = dataSource.getHost();
        Integer port = dataSource.getPort();
        String database = dataSource.getDatabase();
        
        // 参数验证
        if (!StringUtils.hasText(dbType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据库类型不能为空");
        }
        if (!StringUtils.hasText(host)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "主机地址不能为空");
        }
        if (port == null || port < 1 || port > 65535) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "端口号无效，必须在1-65535之间");
        }
        if (!StringUtils.hasText(database)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据库名不能为空");
        }
        
        // 清理参数（去除前后空格）
        dbType = dbType.trim().toLowerCase();
        host = host.trim();
        database = database.trim();
        
        // 根据数据库类型构建URL
        switch (dbType) {
            case "mysql":
                // MySQL URL格式: jdbc:mysql://host:port/database?params
                return String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                    host, port, database);
            case "mariadb":
                // MariaDB URL格式: jdbc:mariadb://host:port/database
                return String.format("jdbc:mariadb://%s:%d/%s?useUnicode=true&characterEncoding=utf8",
                    host, port, database);
            case "postgresql":
                // PostgreSQL URL格式: jdbc:postgresql://host:port/database
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            case "oracle":
                // Oracle URL格式: jdbc:oracle:thin:@host:port:SID 或 jdbc:oracle:thin:@host:port/service_name
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, database);
            case "sqlserver":
                // SQL Server URL格式: jdbc:sqlserver://host:port;databaseName=database
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, database);
            case "sqlite":
                // SQLite URL格式: jdbc:sqlite:path (database作为文件路径)
                return String.format("jdbc:sqlite:%s", database);
            case "dm":
                // 达梦 DM8 URL格式: jdbc:dm://host:port/database
                return String.format("jdbc:dm://%s:%d/%s", host, port, database);
            case "kingbase":
                // 人大金仓 KingbaseES URL格式: jdbc:kingbase8://host:port/database
                return String.format("jdbc:kingbase8://%s:%d/%s", host, port, database);
            case "gbase":
                // 南大通用 GBase URL格式: jdbc:gbase://host:port/database
                return String.format("jdbc:gbase://%s:%d/%s", host, port, database);
            case "tidb":
                // TiDB 兼容MySQL协议
                return String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai",
                    host, port, database);
            case "oceanbase":
                // OceanBase 兼容MySQL协议
                return String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=false",
                    host, port, database);
            case "clickhouse":
                // ClickHouse URL格式: jdbc:clickhouse://host:port/database
                return String.format("jdbc:clickhouse://%s:%d/%s", host, port, database);
            case "presto":
                // Presto/Trino URL格式: jdbc:trino://host:port/catalog/schema (支持查询Hive数据)
                return String.format("jdbc:trino://%s:%d/%s", host, port, database);
            default:
                throw new BusinessException(ErrorCode.DATA_SOURCE_UNSUPPORTED_TYPE, "不支持的数据库类型: " + dbType);
        }
    }

    /**
     * 获取数据库驱动类名
     * 
     * @param dbType 数据库类型：mysql、postgresql、oracle、sqlserver
     * @return 驱动类名
     * @throws BusinessException 不支持的数据库类型时抛出
     */
    public String getDriverClassName(String dbType) {
        switch (dbType.toLowerCase()) {
            case "mysql":
                return "com.mysql.cj.jdbc.Driver";
            case "mariadb":
                return "org.mariadb.jdbc.Driver";
            case "postgresql":
                return "org.postgresql.Driver";
            case "oracle":
                return "oracle.jdbc.driver.OracleDriver";
            case "sqlserver":
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "sqlite":
                return "org.sqlite.JDBC";
            case "dm":
                return "dm.jdbc.driver.DmDriver";
            case "kingbase":
                return "com.kingbase8.Driver";
            case "gbase":
                return "com.gbase.jdbc.Driver";
            case "tidb":
            case "oceanbase":
                // TiDB和OceanBase兼容MySQL协议，使用MySQL驱动
                return "com.mysql.cj.jdbc.Driver";
            case "clickhouse":
                return "com.clickhouse.jdbc.ClickHouseDriver";
            case "presto":
                return "io.trino.jdbc.TrinoDriver";
            default:
                throw new BusinessException(ErrorCode.DATA_SOURCE_UNSUPPORTED_TYPE, "不支持的数据库类型: " + dbType);
        }
    }
    
    /**
     * 检查数据库驱动是否可用
     * 
     * @param dbType 数据库类型
     * @return true=驱动可用, false=需要手动配置
     */
    public boolean isDriverAvailable(String dbType) {
        try {
            String driverClass = getDriverClassName(dbType);
            Class.forName(driverClass);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (BusinessException e) {
            return false;
        }
    }
    
    /**
     * 获取数据库默认端口
     * 
     * @param dbType 数据库类型
     * @return 默认端口号
     */
    public int getDefaultPort(String dbType) {
        switch (dbType.toLowerCase()) {
            case "mysql":
            case "mariadb":
            case "tidb":
                return 3306;
            case "postgresql":
            case "kingbase":
                return 5432;
            case "oracle":
                return 1521;
            case "sqlserver":
                return 1433;
            case "dm":
                return 5236;
            case "gbase":
                return 5258;
            case "oceanbase":
                return 2881;
            case "clickhouse":
                return 8123;
            case "presto":
                return 8080;
            case "sqlite":
                return 0; // SQLite不需要端口
            default:
                return 0;
        }
    }

    /**
     * 测试数据库连接
     * 
     * @param dataSource 数据源信息
     * @throws BusinessException 连接失败时抛出
     */
    public void testConnection(DataSource dataSource) {
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源信息不能为空");
        }
        
        // 验证必要参数
        if (!StringUtils.hasText(dataSource.getUsername())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名不能为空");
        }
        if (!StringUtils.hasText(dataSource.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能为空");
        }
        
        String url = null;
        String driver = null;
        
        try {
            // 构建JDBC URL
            url = buildJdbcUrl(dataSource);
            driver = getDriverClassName(dataSource.getDbType());
            
            // 加载驱动
            Class.forName(driver);
            
            // 测试连接
            try (Connection conn = DriverManager.getConnection(
                    url, 
                    dataSource.getUsername().trim(), 
                    dataSource.getPassword())) {
                // 连接成功，执行一个简单查询验证连接
                conn.isValid(5); // 5秒超时
            }
        } catch (ClassNotFoundException e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                "数据库驱动加载失败: " + e.getMessage() + "，请检查驱动是否正确配置");
        } catch (SQLException e) {
            String errorMsg = e.getMessage();
            // 提供更友好的错误信息
            if (errorMsg != null && errorMsg.contains("Malformed database URL")) {
                String urlInfo = url != null ? url : "URL构建失败";
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "数据库URL格式错误: " + urlInfo + "，请检查主机地址、端口和数据库名是否正确");
            } else if (errorMsg != null && errorMsg.contains("Access denied")) {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "数据库连接被拒绝，请检查用户名和密码是否正确");
            } else if (errorMsg != null && errorMsg.contains("Unknown database")) {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "数据库不存在: " + dataSource.getDatabase());
            } else if (errorMsg != null && errorMsg.contains("Connection refused")) {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "无法连接到数据库服务器 " + dataSource.getHost() + ":" + dataSource.getPort() + "，请检查服务器是否运行");
            } else {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "数据库连接失败: " + errorMsg);
            }
        } catch (BusinessException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                "连接测试失败: " + e.getMessage());
        }
    }

    /**
     * 获取本地数据库连接（用于数据采集存储）
     * 
     * @return 数据库连接
     * @throws SQLException 连接失败时抛出
     * @throws BusinessException 本地数据库配置未设置时抛出
     */
    public Connection getLocalConnection() throws SQLException {
        if (localDbUrl == null || localDbUrl.isEmpty()) {
            throw new BusinessException(ErrorCode.ERROR, "本地数据库配置未设置");
        }
        return DriverManager.getConnection(localDbUrl, localDbUsername, localDbPassword);
    }
}

