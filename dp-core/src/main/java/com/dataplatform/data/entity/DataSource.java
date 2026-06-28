package com.dataplatform.data.entity;

import lombok.Data;
import java.util.Date;

/**
 * 数据源实体类
 * 
 * @author dataplatform
 */
@Data
public class DataSource {
    /** 数据源ID */
    private Long id;
    
    /** 数据源名称 */
    private String name;
    
    /** 数据库类型：mysql、postgresql、oracle、sqlserver */
    private String dbType;
    
    /** 主机地址 */
    private String host;
    
    /** 端口号 */
    private Integer port;
    
    /** 数据库名 */
    private String database;
    
    /** 用户名 */
    private String username;
    
    /** 密码 */
    private String password;
    
    /** 分组名称 */
    private String groupName;
    
    /** 最大连接池大小 */
    private Integer maxPoolSize;
    
    /** 最小空闲连接数 */
    private Integer minIdle;
    
    /** 连接超时(秒) */
    private Integer connectTimeout;
    
    /** 查询超时(秒) */
    private Integer queryTimeout;
    
    /** 最后测试结果: 1-成功 0-失败 */
    private Integer lastTestResult;
    
    /** 最后测试时间 */
    private Date lastTestTime;
    
    /** 最后测试错误信息 */
    private String lastTestError;
    
    /** 创建时间 */
    private Date createTime;
    
    /** 更新时间 */
    private Date updateTime;
}

