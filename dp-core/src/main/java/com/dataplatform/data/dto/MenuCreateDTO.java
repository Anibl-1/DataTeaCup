package com.dataplatform.data.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 菜单创建DTO
 * 
 * @author dataplatform
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MenuCreateDTO {
    /** 菜单名称 */
    @NotBlank(message = "菜单名称不能为空")
    private String menuName;
    
    /** 菜单编码（唯一标识，用于路由） */
    @NotBlank(message = "菜单编码不能为空")
    private String menuCode;
    
    /** 父菜单ID，0表示顶级菜单 */
    private Long parentId;
    
    /** 菜单类型：menu-菜单，button-按钮 */
    private String menuType;
    
    /** 路由路径 */
    private String routePath;
    
    /** 组件路径 */
    private String componentPath;
    
    /** 图标 */
    private String icon;
    
    /** 排序顺序 */
    private Integer sortOrder;
    
    /** 是否可见：1-可见，0-隐藏 */
    private Integer isVisible;
    
    /** 权限编码 */
    private String permissionCode;
    
    /** 关联的报表ID（如果是报表菜单） */
    private Long reportId;
    
    /** 关联的图表ID */
    private Long chartId;
    
    /** 关联的页面ID */
    private Long pageId;
    
    /** 关联的数据视图编码 */
    private String dataViewCode;
    
    /** 打开方式: tab-标签页, window-新窗口, drawer-抽屉 */
    private String openMode;
    
    /** 角标文案，如 New、Beta */
    private String badge;
}

