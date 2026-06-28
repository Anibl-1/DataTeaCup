package com.dataplatform.data.service;

import com.dataplatform.data.entity.DataView;
import com.dataplatform.system.entity.Menu;
import com.dataplatform.data.mapper.DataViewMapper;
import com.dataplatform.system.mapper.MenuMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class DataViewService {

    @Autowired
    private DataViewMapper dataViewMapper;

    @Autowired
    private MenuMapper menuMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<DataView> getList(String keyword) {
        return dataViewMapper.selectList(keyword);
    }

    public DataView getById(Long id) {
        return dataViewMapper.selectById(id);
    }

    public DataView getByCode(String code) {
        return dataViewMapper.selectByCode(code);
    }

    @Transactional
    public int create(DataView dataView, List<Map<String, Object>> columns) throws JsonProcessingException {
        dataView.setCreateTime(LocalDateTime.now());
        dataView.setUpdateTime(LocalDateTime.now());
        
        // 序列化列配置
        if (columns != null) {
            dataView.setColumnsConfig(objectMapper.writeValueAsString(columns));
        }
        
        // 设置默认值
        if (dataView.getStatus() == null) dataView.setStatus(1);
        if (dataView.getAllowQuery() == null) dataView.setAllowQuery(1);
        if (dataView.getAllowInsert() == null) dataView.setAllowInsert(1);
        if (dataView.getAllowUpdate() == null) dataView.setAllowUpdate(1);
        if (dataView.getAllowDelete() == null) dataView.setAllowDelete(1);
        if (dataView.getAllowImport() == null) dataView.setAllowImport(1);
        if (dataView.getAllowExport() == null) dataView.setAllowExport(1);
        if (dataView.getPageSize() == null) dataView.setPageSize(20);
        if (dataView.getDefaultOrderDir() == null) dataView.setDefaultOrderDir("DESC");
        
        int result = dataViewMapper.insert(dataView);
        
        // 如果需要生成菜单
        if (dataView.getGenerateMenu() != null && dataView.getGenerateMenu() == 1) {
            Long menuId = createMenu(dataView);
            dataViewMapper.updateMenuId(dataView.getId(), menuId);
        }
        
        return result;
    }

    @Transactional
    public int update(DataView dataView, List<Map<String, Object>> columns) throws JsonProcessingException {
        dataView.setUpdateTime(LocalDateTime.now());
        
        // 序列化列配置
        if (columns != null) {
            dataView.setColumnsConfig(objectMapper.writeValueAsString(columns));
        }
        
        // 处理菜单
        DataView existing = dataViewMapper.selectById(dataView.getId());
        if (dataView.getGenerateMenu() != null && dataView.getGenerateMenu() == 1) {
            if (existing.getMenuId() != null) {
                // 更新现有菜单
                updateMenu(dataView, existing.getMenuId());
            } else {
                // 创建新菜单
                Long menuId = createMenu(dataView);
                dataView.setMenuId(menuId);
            }
        } else if (existing.getMenuId() != null) {
            // 删除关联的菜单
            menuMapper.delete(existing.getMenuId());
            dataView.setMenuId(null);
        }
        
        return dataViewMapper.update(dataView);
    }

    @Transactional
    public int delete(Long id) {
        DataView dataView = dataViewMapper.selectById(id);
        if (dataView != null && dataView.getMenuId() != null) {
            menuMapper.delete(dataView.getMenuId());
        }
        return dataViewMapper.deleteById(id);
    }

    private Long createMenu(DataView dataView) {
        Menu menu = new Menu();
        menu.setMenuName(dataView.getMenuName() != null ? dataView.getMenuName() : dataView.getName());
        menu.setMenuCode("DataView_" + dataView.getCode());
        menu.setParentId(dataView.getMenuParentId() != null ? dataView.getMenuParentId() : 0L);
        menu.setMenuType("menu");
        menu.setRoutePath("/data-view/" + dataView.getCode());
        menu.setComponentPath("@/views/DataViewPage.vue");
        menu.setIcon(dataView.getMenuIcon() != null ? dataView.getMenuIcon() : "GridOutline");
        menu.setSortOrder(dataView.getMenuSort() != null ? dataView.getMenuSort() : 0);
        menu.setIsVisible(1);
        menu.setPermissionCode("dataview:" + dataView.getCode());
        menu.setCreateTime(new Date());
        menu.setUpdateTime(new Date());
        
        menuMapper.insert(menu);
        return menu.getId();
    }

    private void updateMenu(DataView dataView, Long menuId) {
        Menu menu = menuMapper.selectById(menuId);
        if (menu != null) {
            menu.setMenuName(dataView.getMenuName() != null ? dataView.getMenuName() : dataView.getName());
            menu.setParentId(dataView.getMenuParentId() != null ? dataView.getMenuParentId() : 0L);
            menu.setIcon(dataView.getMenuIcon() != null ? dataView.getMenuIcon() : "GridOutline");
            menu.setSortOrder(dataView.getMenuSort() != null ? dataView.getMenuSort() : 0);
            menu.setUpdateTime(new Date());
            menuMapper.update(menu);
        }
    }
}
