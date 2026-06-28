package com.dataplatform.serviceapi.data.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 跨服务数据源信息 DTO
 */
@Data
public class DataSourceInfoDTO implements Serializable {
    private Long id;
    private String name;
    private String type;
    private String host;
    private Integer port;
    private String databaseName;
    private String status;
}
