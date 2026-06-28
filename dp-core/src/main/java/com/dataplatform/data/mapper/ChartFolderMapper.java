package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.ChartFolder;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 图表文件夹Mapper
 */
@Mapper
public interface ChartFolderMapper {

    @Select("SELECT * FROM sys_chart_folder ORDER BY sort_order, id")
    List<ChartFolder> selectAll();

    @Select("SELECT * FROM sys_chart_folder WHERE id = #{id}")
    ChartFolder selectById(@Param("id") Long id);

    @Insert("INSERT INTO sys_chart_folder (folder_name, parent_id, sort_order, create_time) " +
            "VALUES (#{folderName}, #{parentId}, #{sortOrder}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChartFolder folder);

    @Update("UPDATE sys_chart_folder SET folder_name = #{folderName}, parent_id = #{parentId}, sort_order = #{sortOrder} WHERE id = #{id}")
    int update(ChartFolder folder);

    @Delete("DELETE FROM sys_chart_folder WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM sys_chart WHERE folder_id = #{folderId}")
    int countChartsByFolderId(@Param("folderId") Long folderId);

    @Select("SELECT COUNT(*) FROM sys_chart_folder WHERE parent_id = #{parentId}")
    int countChildren(@Param("parentId") Long parentId);
}
