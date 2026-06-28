package com.dataplatform.system.mapper;

import com.dataplatform.system.entity.Menu;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 菜单Mapper接口
 * 
 * @author dataplatform
 */
public interface MenuMapper {
    Menu selectById(Long id);
    Menu selectByCode(String menuCode);
    List<Menu> selectAll();
    List<Menu> selectByParentId(Long parentId);
    List<Menu> selectByParentIdAndVisible(@Param("parentId") Long parentId, @Param("isVisible") Integer isVisible);
    int insert(Menu menu);
    int update(Menu menu);
    int delete(Long id);
    long countByParentId(Long parentId);
}
