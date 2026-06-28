package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.DataView;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DataViewMapper {

    @Select("<script>" +
            "SELECT * FROM sys_data_view " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (name LIKE CONCAT('%', #{keyword}, '%') OR code LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "</where>" +
            " ORDER BY create_time DESC" +
            "</script>")
    List<DataView> selectList(@Param("keyword") String keyword);

    @Select("SELECT * FROM sys_data_view WHERE id = #{id}")
    DataView selectById(@Param("id") Long id);

    @Select("SELECT * FROM sys_data_view WHERE code = #{code}")
    DataView selectByCode(@Param("code") String code);

    @Insert("INSERT INTO sys_data_view (name, code, data_source_id, table_name, description, status, " +
            "columns_config, allow_query, allow_insert, allow_update, allow_delete, allow_import, allow_export, " +
            "default_order_by, default_order_dir, page_size, generate_menu, menu_name, menu_parent_id, " +
            "menu_icon, menu_sort, menu_id, create_time, update_time) " +
            "VALUES (#{name}, #{code}, #{dataSourceId}, #{tableName}, #{description}, #{status}, " +
            "#{columnsConfig}, #{allowQuery}, #{allowInsert}, #{allowUpdate}, #{allowDelete}, #{allowImport}, #{allowExport}, " +
            "#{defaultOrderBy}, #{defaultOrderDir}, #{pageSize}, #{generateMenu}, #{menuName}, #{menuParentId}, " +
            "#{menuIcon}, #{menuSort}, #{menuId}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DataView dataView);

    @Update("UPDATE sys_data_view SET name = #{name}, code = #{code}, data_source_id = #{dataSourceId}, " +
            "table_name = #{tableName}, description = #{description}, status = #{status}, " +
            "columns_config = #{columnsConfig}, allow_query = #{allowQuery}, allow_insert = #{allowInsert}, " +
            "allow_update = #{allowUpdate}, allow_delete = #{allowDelete}, allow_import = #{allowImport}, " +
            "allow_export = #{allowExport}, default_order_by = #{defaultOrderBy}, default_order_dir = #{defaultOrderDir}, " +
            "page_size = #{pageSize}, generate_menu = #{generateMenu}, menu_name = #{menuName}, " +
            "menu_parent_id = #{menuParentId}, menu_icon = #{menuIcon}, menu_sort = #{menuSort}, " +
            "menu_id = #{menuId}, update_time = #{updateTime} WHERE id = #{id}")
    int update(DataView dataView);

    @Delete("DELETE FROM sys_data_view WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Update("UPDATE sys_data_view SET menu_id = #{menuId}, update_time = NOW() WHERE id = #{id}")
    int updateMenuId(@Param("id") Long id, @Param("menuId") Long menuId);

    @Update("UPDATE sys_data_view SET menu_id = NULL, update_time = NOW() WHERE menu_id = #{menuId}")
    int clearMenuId(@Param("menuId") Long menuId);
}
