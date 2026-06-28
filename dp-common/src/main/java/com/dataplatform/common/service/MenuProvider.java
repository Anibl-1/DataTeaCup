package com.dataplatform.common.service;

/**
 * 菜单提供者接口
 * 用于跨模块创建菜单，避免 dp-data 直接依赖 dp-system
 */
public interface MenuProvider {

    /**
     * 创建菜单
     *
     * @param menuName      菜单名称
     * @param menuCode      菜单编码
     * @param parentId      父菜单ID
     * @param menuType      菜单类型
     * @param icon          图标
     * @param isVisible     是否可见
     * @param sortOrder     排序号
     * @param routePath     路由路径
     * @param componentPath 组件路径
     * @param reportId      关联报表ID
     * @param chartId       关联图表ID
     * @param pageId        关联页面ID
     * @param dataViewCode  关联数据视图编码
     * @param openMode      打开方式: tab/window/drawer
     * @param badge         角标文案
     * @return 菜单ID
     */
    Long createMenu(String menuName, String menuCode, Long parentId, String menuType,
                    String icon, Integer isVisible, Integer sortOrder,
                    String routePath, String componentPath, Long reportId,
                    Long chartId, Long pageId, String dataViewCode,
                    String openMode, String badge);
}
