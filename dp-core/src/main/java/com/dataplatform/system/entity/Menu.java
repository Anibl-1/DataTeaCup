package com.dataplatform.system.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * 菜单实体类
 * 
 * @author dataplatform
 */
@Data
public class Menu {
    /** 菜单ID */
    private Long id;
    
    /** 菜单名称 */
    private String menuName;
    
    /** 菜单编码（唯一标识，用于路由） */
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
    
    /** 移动端可见：1-可见，0-隐藏 */
    private Integer mobileVisible;
    
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
    
    /** 打开方式：tab-标签页，window-新窗口，drawer-抽屉 */
    private String openMode;
    
    /** 角标文案，如 New、Beta */
    private String badge;
    
    /** 创建时间 */
    private Date createTime;
    
    /** 更新时间 */
    private Date updateTime;
    
    /** 子菜单列表 */
    private List<Menu> children;
    
    /**
     * 关联的报表信息
     * NOTE: 此字段引用 com.dataplatform.entity.ReportDefinition，
     * 该类将在 dp-data 模块创建后可用。当前暂用 Object 类型占位，
     * 待 dp-data 模块完成后需要解决跨模块依赖。
     */
    private Object report;
}
