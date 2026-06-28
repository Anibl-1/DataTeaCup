package com.dataplatform.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 数据源创建数据传输对象
 * 
 * @author dataplatform
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataSourceCreateDTO {
    /** ID（编辑时使用） */
    private Long id;
    
    /** 数据源名称 */
    @NotBlank(message = "数据源名称不能为空")
    private String name;

    /** 数据库类型：mysql、postgresql、oracle、sqlserver */
    @NotBlank(message = "数据库类型不能为空")
    private String dbType;

    /** 主机地址 */
    @NotBlank(message = "主机地址不能为空")
    private String host;

    /** 端口号 */
    @NotNull(message = "端口不能为空")
    @Positive(message = "端口必须为正数")
    private Integer port;

    /** 数据库名 */
    @NotBlank(message = "数据库名不能为空")
    private String database;

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 分组名称 */
    private String groupName;
}

