package com.dataplatform.data.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 数据源更新数据传输对象
 * 
 * @author dataplatform
 */
@Data
public class DataSourceUpdateDTO {
    /** 数据源ID */
    @NotNull(message = "数据源ID不能为空")
    private Long id;

    /** 数据源名称 */
    private String name;

    /** 数据库类型：mysql、postgresql、oracle、sqlserver */
    private String dbType;

    /** 主机地址 */
    private String host;

    /** 端口号 */
    @Positive(message = "端口必须为正数")
    private Integer port;

    /** 数据库名 */
    private String database;

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 分组名称 */
    private String groupName;
}

