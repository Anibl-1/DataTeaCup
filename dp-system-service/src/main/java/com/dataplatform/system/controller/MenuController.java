package com.dataplatform.system.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.Result;
import com.dataplatform.data.dto.MenuCreateDTO;
import com.dataplatform.data.dto.MenuUpdateDTO;
import com.dataplatform.system.entity.Menu;
import com.dataplatform.system.service.MenuService;
import com.dataplatform.common.security.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/menu")
@RequirePermission("menu:manage")
public class MenuController {
    @Autowired
    private MenuService menuService;
    
    @Autowired
    private com.dataplatform.system.mapper.MenuMapper menuMapper;
    
    @RequirePermission("menu:manage")
    @GetMapping("/all")
    public Result<List<Menu>> getAllMenus() {
        List<Menu> menus = menuMapper.selectAll();
        return Result.success(menus);
    }
    
    @RequirePermission("menu:manage")
    @GetMapping("/tree")
    public Result<List<Menu>> getMenuTree() {
        List<Menu> menus = menuService.getAllMenus();
        return Result.success(menus);
    }
    
    @RequirePermission("menu:read")
    @GetMapping("/visible")
    public Result<List<Menu>> getVisibleMenus() {
        String username = SecurityContext.getCurrentUsername();
        List<Menu> menus = menuService.getVisibleMenus(username);
        return Result.success(menus);
    }
    
    @RequirePermission("menu:read")
    @GetMapping("/{id}")
    public Result<Menu> getMenuById(@PathVariable Long id) {
        Menu menu = menuService.getMenuById(id);
        return Result.success(menu);
    }
    
    @RequirePermission("menu:read")
    @GetMapping("/code/{code}")
    public Result<Menu> getMenuByCode(@PathVariable String code) {
        Menu menu = menuService.getMenuByCode(code);
        return Result.success(menu);
    }
    
    @RequirePermission("menu:manage")
    @OperationLog(module = "菜单管理", type = OperationLog.OperationType.CREATE, description = "创建菜单")
    @PostMapping("/create")
    public Result<Long> createMenu(@Valid @RequestBody MenuCreateDTO dto) {
        Long id = menuService.createMenu(
            dto.getMenuName(), dto.getMenuCode(), dto.getParentId(),
            dto.getMenuType(), dto.getIcon(), dto.getIsVisible(),
            dto.getSortOrder(), dto.getRoutePath(), dto.getComponentPath(),
            dto.getReportId(), dto.getChartId(), dto.getPageId(),
            dto.getDataViewCode(), dto.getOpenMode(), dto.getBadge()
        );
        return Result.success(id);
    }
    
    @RequirePermission("menu:manage")
    @OperationLog(module = "菜单管理", type = OperationLog.OperationType.UPDATE, description = "更新菜单")
    @PostMapping("/update")
    public Result<Void> updateMenu(@Valid @RequestBody MenuUpdateDTO dto) {
        menuService.updateMenu(
            dto.getId(), dto.getMenuName(), dto.getMenuCode(), dto.getParentId(),
            dto.getMenuType(), dto.getIcon(), dto.getIsVisible(),
            dto.getSortOrder(), dto.getRoutePath(), dto.getComponentPath(),
            dto.getReportId(), dto.getChartId(), dto.getPageId(),
            dto.getDataViewCode(), dto.getOpenMode(), dto.getBadge()
        );
        return Result.success(null);
    }
    
    @RequirePermission("menu:manage")
    @OperationLog(module = "菜单管理", type = OperationLog.OperationType.DELETE, description = "删除菜单")
    @DeleteMapping("/{id}")
    public Result<Void> deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return Result.success(null);
    }
}
