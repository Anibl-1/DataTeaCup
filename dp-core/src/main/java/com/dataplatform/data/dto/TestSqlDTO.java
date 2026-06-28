package com.dataplatform.data.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 测试SQL DTO
 * 
 * @author dataplatform
 */
@Data
public class TestSqlDTO {
    /** 数据源ID */
    @NotNull(message = "数据源ID不能为空")
    private Long dataSourceId;
    
    /** SQL查询语句 */
    @NotBlank(message = "SQL查询语句不能为空")
    private String sql;
}

