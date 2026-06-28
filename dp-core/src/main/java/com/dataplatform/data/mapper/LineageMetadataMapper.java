package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataplatform.data.entity.LineageMetadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 血缘元数据Mapper接口
 * 
 * @author dataplatform
 */
@Mapper
public interface LineageMetadataMapper extends BaseMapper<LineageMetadata> {
    
    /**
     * 根据源数据源ID和源表名查询下游血缘
     * 
     * @param sourceDsId 源数据源ID
     * @param sourceTable 源表名
     * @return 下游血缘列表
     */
    List<LineageMetadata> selectBySource(@Param("sourceDsId") Long sourceDsId, @Param("sourceTable") String sourceTable);
    
    /**
     * 根据目标数据源ID和目标表名查询上游血缘
     * 
     * @param targetDsId 目标数据源ID
     * @param targetTable 目标表名
     * @return 上游血缘列表
     */
    List<LineageMetadata> selectByTarget(@Param("targetDsId") Long targetDsId, @Param("targetTable") String targetTable);
    
    /**
     * 根据转换类型和任务ID查询血缘
     * 
     * @param transformType 转换类型
     * @param transformId 任务ID
     * @return 血缘列表
     */
    List<LineageMetadata> selectByTransform(@Param("transformType") String transformType, @Param("transformId") Long transformId);
    
    /**
     * 删除指定转换任务的血缘记录
     * 
     * @param transformType 转换类型
     * @param transformId 任务ID
     * @return 影响行数
     */
    int deleteByTransform(@Param("transformType") String transformType, @Param("transformId") Long transformId);
}
