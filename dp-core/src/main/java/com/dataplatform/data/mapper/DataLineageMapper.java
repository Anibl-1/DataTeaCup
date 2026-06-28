package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.DataLineage;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 数据血缘关系Mapper
 */
@Mapper
public interface DataLineageMapper {
    
    @Insert("INSERT INTO data_lineage (source_type, source_id, source_name, source_database, source_table, source_column, " +
            "target_type, target_id, target_name, target_database, target_table, target_column, " +
            "lineage_type, transform_logic, sql_content, create_by, create_time, update_time) " +
            "VALUES (#{sourceType}, #{sourceId}, #{sourceName}, #{sourceDatabase}, #{sourceTable}, #{sourceColumn}, " +
            "#{targetType}, #{targetId}, #{targetName}, #{targetDatabase}, #{targetTable}, #{targetColumn}, " +
            "#{lineageType}, #{transformLogic}, #{sqlContent}, #{createBy}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DataLineage lineage);
    
    @Update("UPDATE data_lineage SET source_type=#{sourceType}, source_id=#{sourceId}, source_name=#{sourceName}, " +
            "source_database=#{sourceDatabase}, source_table=#{sourceTable}, source_column=#{sourceColumn}, " +
            "target_type=#{targetType}, target_id=#{targetId}, target_name=#{targetName}, " +
            "target_database=#{targetDatabase}, target_table=#{targetTable}, target_column=#{targetColumn}, " +
            "lineage_type=#{lineageType}, transform_logic=#{transformLogic}, sql_content=#{sqlContent}, " +
            "update_time=NOW() WHERE id=#{id}")
    int update(DataLineage lineage);
    
    @Delete("DELETE FROM data_lineage WHERE id=#{id}")
    int deleteById(@Param("id") Long id);
    
    @Select("SELECT * FROM data_lineage WHERE id=#{id}")
    @Results(id = "lineageResultMap", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "sourceType", column = "source_type"),
        @Result(property = "sourceId", column = "source_id"),
        @Result(property = "sourceName", column = "source_name"),
        @Result(property = "sourceDatabase", column = "source_database"),
        @Result(property = "sourceTable", column = "source_table"),
        @Result(property = "sourceColumn", column = "source_column"),
        @Result(property = "targetType", column = "target_type"),
        @Result(property = "targetId", column = "target_id"),
        @Result(property = "targetName", column = "target_name"),
        @Result(property = "targetDatabase", column = "target_database"),
        @Result(property = "targetTable", column = "target_table"),
        @Result(property = "targetColumn", column = "target_column"),
        @Result(property = "lineageType", column = "lineage_type"),
        @Result(property = "transformLogic", column = "transform_logic"),
        @Result(property = "sqlContent", column = "sql_content"),
        @Result(property = "createBy", column = "create_by"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time")
    })
    DataLineage findById(@Param("id") Long id);
    
    @Select("SELECT * FROM data_lineage ORDER BY create_time DESC")
    @ResultMap("lineageResultMap")
    List<DataLineage> findAll();
    
    @Select("SELECT * FROM data_lineage WHERE target_table=#{tableName} OR source_table=#{tableName}")
    @ResultMap("lineageResultMap")
    List<DataLineage> findByTableName(@Param("tableName") String tableName);
    
    @Select("SELECT * FROM data_lineage WHERE source_table=#{tableName}")
    @ResultMap("lineageResultMap")
    List<DataLineage> findDownstream(@Param("tableName") String tableName);
    
    @Select("SELECT * FROM data_lineage WHERE target_table=#{tableName}")
    @ResultMap("lineageResultMap")
    List<DataLineage> findUpstream(@Param("tableName") String tableName);
    
    @Select("SELECT COUNT(*) FROM data_lineage")
    int count();
}
