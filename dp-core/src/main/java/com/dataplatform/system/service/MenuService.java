package com.dataplatform.system.service;

import com.dataplatform.common.constants.Constants;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.common.service.MenuProvider;
import com.dataplatform.system.entity.Menu;
import com.dataplatform.system.mapper.MenuMapper;
import com.dataplatform.system.mapper.RoleMenuMapper;
import com.dataplatform.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单服务
 * 
 * NOTE: 原始代码引用了以下跨模块类，待后续模块创建后解决：
 * - com.dataplatform.dto.MenuCreateDTO (dp-data)
 * - com.dataplatform.dto.MenuUpdateDTO (dp-data)
 * - com.dataplatform.mapper.DataViewMapper (dp-data)
 * - com.dataplatform.service.CacheService (dp-infra)
 * 
 * 当前保留这些引用的方法签名，但使用简化的参数类型。
 * 完整的跨模块依赖将在 dp-data 和 dp-api 模块创建后统一解决。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService implements MenuProvider {
    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final UserMapper userMapper;

    public List<Menu> getAllMenus() {
        List<Menu> allMenus = menuMapper.selectAll();
        return buildMenuTree(allMenus, 0L);
    }
    
    public List<Menu> getVisibleMenus(String username) {
        log.info("获取可见菜单, username={}", username);
        
        List<Menu> allMenus = menuMapper.selectAll();
        allMenus = allMenus.stream()
            .filter(menu -> menu.getIsVisible() != null && menu.getIsVisible() == 1)
            .collect(Collectors.toList());
        log.info("可见菜单总数: {}", allMenus.size());
        
        if (!StringUtils.hasText(username)) {
            log.info("用户名为空，返回空菜单");
            return new ArrayList<>();
        }
        
        if (Constants.ADMIN_USERNAME.equals(username)) {
            log.info("admin用户，返回所有可见菜单");
            return buildMenuTree(allMenus, 0L);
        }
        
        com.dataplatform.system.entity.User user = userMapper.selectByUsername(username);
        if (user == null) {
            log.warn("用户不存在: {}", username);
            return new ArrayList<>();
        }
        
        List<Long> menuIds = roleMenuMapper.selectMenuIdsByUserId(user.getId());
        Set<Long> menuIdSet = new HashSet<>(menuIds);
        log.info("用户 {} (ID={}) 拥有的菜单ID: {}", username, user.getId(), menuIdSet);
        
        if (menuIdSet.isEmpty()) {
            log.info("用户没有任何角色分配的菜单，只返回仪表盘");
            allMenus = allMenus.stream()
                .filter(menu -> "dashboard".equals(menu.getMenuCode()))
                .collect(Collectors.toList());
            return buildMenuTree(allMenus, 0L);
        }
        
        Set<Long> finalMenuIds = new HashSet<>(menuIdSet);
        allMenus.stream()
            .filter(menu -> "dashboard".equals(menu.getMenuCode()))
            .findFirst()
            .ifPresent(menu -> finalMenuIds.add(menu.getId()));
        
        for (Long menuId : new HashSet<>(menuIdSet)) {
            addParentMenuIds(allMenus, menuId, finalMenuIds);
        }
        log.info("最终菜单ID（含父菜单）: {}", finalMenuIds);
        
        allMenus = allMenus.stream()
            .filter(menu -> finalMenuIds.contains(menu.getId()))
            .collect(Collectors.toList());
        log.info("过滤后菜单数量: {}", allMenus.size());
        
        return buildMenuTree(allMenus, 0L);
    }
    
    private void addParentMenuIds(List<Menu> allMenus, Long menuId, Set<Long> menuIdSet) {
        for (Menu menu : allMenus) {
            if (menu.getId().equals(menuId) && menu.getParentId() != null && menu.getParentId() > 0) {
                menuIdSet.add(menu.getParentId());
                addParentMenuIds(allMenus, menu.getParentId(), menuIdSet);
                break;
            }
        }
    }
    
    public Menu getMenuById(Long id) { return menuMapper.selectById(id); }
    public Menu getMenuByCode(String menuCode) { return menuMapper.selectByCode(menuCode); }
    
    @Transactional
    public void deleteMenu(Long id) {
        Menu menu = menuMapper.selectById(id);
        if (menu == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "菜单不存在"); }
        long childCount = menuMapper.countByParentId(id);
        if (childCount > 0) { throw new BusinessException(ErrorCode.PARAM_ERROR, "该菜单下有子菜单，不能删除"); }
        menuMapper.delete(id);
    }
    
    private List<Menu> buildMenuTree(List<Menu> allMenus, Long parentId) {
        List<Menu> tree = new ArrayList<>();
        for (Menu menu : allMenus) {
            if (menu.getParentId() != null && menu.getParentId().equals(parentId)) {
                List<Menu> children = buildMenuTree(allMenus, menu.getId());
                menu.setChildren(children);
                tree.add(menu);
            }
        }
        return tree;
    }
    
    private boolean isDescendant(Long menuId, Long parentId) {
        Menu parent = menuMapper.selectById(parentId);
        if (parent == null) { return false; }
        Long currentParentId = parent.getParentId();
        if (currentParentId == null || currentParentId == 0) { return false; }
        if (currentParentId.equals(menuId)) { return true; }
        return isDescendant(menuId, currentParentId);
    }

    @Override
    @Transactional
    public Long createMenu(String menuName, String menuCode, Long parentId, String menuType,
                           String icon, Integer isVisible, Integer sortOrder,
                           String routePath, String componentPath, Long reportId,
                           Long chartId, Long pageId, String dataViewCode,
                           String openMode, String badge) {
        Menu existing = menuMapper.selectByCode(menuCode);
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "菜单编码已存在: " + menuCode);
        }
        if (parentId != null && parentId > 0) {
            Menu parent = menuMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "父菜单不存在");
            }
        }
        Menu menu = new Menu();
        menu.setMenuName(menuName);
        menu.setMenuCode(menuCode);
        menu.setParentId(parentId != null ? parentId : 0L);
        menu.setMenuType(StringUtils.hasText(menuType) ? menuType : "menu");
        menu.setRoutePath(routePath);
        menu.setComponentPath(componentPath);
        menu.setIcon(icon);
        menu.setSortOrder(sortOrder != null ? sortOrder : 0);
        menu.setIsVisible(isVisible != null ? isVisible : 1);
        menu.setReportId(reportId);
        menu.setChartId(chartId);
        menu.setPageId(pageId);
        menu.setDataViewCode(dataViewCode);
        menu.setOpenMode(StringUtils.hasText(openMode) ? openMode : "tab");
        menu.setBadge(badge);
        menuMapper.insert(menu);
        return menu.getId();
    }

    /**
     * 更新菜单（使用独立参数，避免跨模块DTO依赖）
     */
    @Transactional
    public void updateMenu(Long id, String menuName, String menuCode, Long parentId,
                           String menuType, String icon, Integer isVisible, Integer sortOrder,
                           String routePath, String componentPath, Long reportId,
                           Long chartId, Long pageId, String dataViewCode,
                           String openMode, String badge) {
        Menu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "菜单不存在");
        }
        if (menuName != null) menu.setMenuName(menuName);
        if (menuCode != null) {
            Menu existing = menuMapper.selectByCode(menuCode);
            if (existing != null && !existing.getId().equals(id)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "菜单编码已存在: " + menuCode);
            }
            menu.setMenuCode(menuCode);
        }
        if (parentId != null) {
            if (parentId.equals(id)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "不能将自己设为父菜单");
            }
            menu.setParentId(parentId);
        }
        if (menuType != null) menu.setMenuType(menuType);
        if (routePath != null) menu.setRoutePath(routePath);
        if (componentPath != null) menu.setComponentPath(componentPath);
        if (icon != null) menu.setIcon(icon);
        if (sortOrder != null) menu.setSortOrder(sortOrder);
        if (isVisible != null) menu.setIsVisible(isVisible);
        if (reportId != null) menu.setReportId(reportId);
        if (chartId != null) menu.setChartId(chartId);
        if (pageId != null) menu.setPageId(pageId);
        if (dataViewCode != null) menu.setDataViewCode(dataViewCode);
        if (openMode != null) menu.setOpenMode(openMode);
        if (badge != null) menu.setBadge(badge);
        menuMapper.update(menu);
    }

}
