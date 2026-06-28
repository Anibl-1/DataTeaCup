package com.dataplatform.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源权限实体
 * 支持资源级和操作级权限控制
 *
 * @author dataplatform
 */
@Data
@TableName("resource_permission")
public class ResourcePermission {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色ID */
    private Long roleId;

    /** 资源类型: datasource, report, dashboard, folder */
    private String resourceType;

    /** 资源ID */
    private Long resourceId;

    /** 操作权限JSON数组: ["view","edit","delete","export","share"] */
    private String operations;

    /** 是否继承自父资源 */
    private Boolean inherited;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
