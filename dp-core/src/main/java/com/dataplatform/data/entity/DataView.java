package com.dataplatform.data.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据视图实体
 */
@Data
public class DataView {
    private Long id;
    
    /** 视图名称 */
    private String name;
    
    /** 视图编码（唯一） */
    private String code;
    
    /** 数据源ID */
    private Long dataSourceId;
    
    /** 数据表名 */
    private String tableName;
    
    /** 描述 */
    private String description;
    
    /** 状态: 0-禁用 1-启用 */
    private Integer status;
    
    /** 列配置JSON */
    private String columnsConfig;
    
    /** 允许查询 */
    private Integer allowQuery;
    
    /** 允许新增 */
    private Integer allowInsert;
    
    /** 允许编辑 */
    private Integer allowUpdate;
    
    /** 允许删除 */
    private Integer allowDelete;
    
    /** 允许导入 */
    private Integer allowImport;
    
    /** 允许导出 */
    private Integer allowExport;
    
    /** 默认排序字段 */
    private String defaultOrderBy;
    
    /** 默认排序方向 */
    private String defaultOrderDir;
    
    /** 每页条数 */
    private Integer pageSize;
    
    /** 是否生成菜单 */
    private Integer generateMenu;
    
    /** 菜单名称 */
    private String menuName;
    
    /** 父菜单ID */
    private Long menuParentId;
    
    /** 菜单图标 */
    private String menuIcon;
    
    /** 菜单排序 */
    private Integer menuSort;
    
    /** 关联的菜单ID */
    private Long menuId;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}
