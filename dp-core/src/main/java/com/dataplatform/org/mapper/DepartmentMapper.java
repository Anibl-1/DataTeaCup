package com.dataplatform.org.mapper;

import com.dataplatform.org.entity.Department;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 部门Mapper
 * 
 * @author dataplatform
 */
@Mapper
public interface DepartmentMapper {

    @Select("SELECT d.*, pd.dept_name AS parent_name FROM sys_department d " +
            "LEFT JOIN sys_department pd ON d.parent_id = pd.id " +
            "WHERE d.del_flag = 0 ORDER BY d.sort_order, d.id")
    List<Department> selectAll();

    @Select("<script>" +
            "SELECT d.*, pd.dept_name AS parent_name FROM sys_department d " +
            "LEFT JOIN sys_department pd ON d.parent_id = pd.id " +
            "WHERE d.del_flag = 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (d.dept_name LIKE CONCAT('%', #{keyword}, '%') OR d.dept_code LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='status != null'>AND d.status = #{status}</if>" +
            " ORDER BY d.sort_order, d.id" +
            "</script>")
    List<Department> selectList(@Param("keyword") String keyword, @Param("status") Integer status);

    @Select("SELECT * FROM sys_department WHERE id = #{id} AND del_flag = 0")
    Department selectById(@Param("id") Long id);

    @Select("SELECT * FROM sys_department WHERE dept_code = #{deptCode} AND del_flag = 0")
    Department selectByDeptCode(@Param("deptCode") String deptCode);

    @Select("SELECT * FROM sys_department WHERE parent_id = #{parentId} AND del_flag = 0 ORDER BY sort_order, id")
    List<Department> selectByParentId(@Param("parentId") Long parentId);

    @Insert("INSERT INTO sys_department (dept_name, dept_code, parent_id, ancestors, leader, phone, email, sort_order, status) " +
            "VALUES (#{deptName}, #{deptCode}, #{parentId}, #{ancestors}, #{leader}, #{phone}, #{email}, #{sortOrder}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Department department);

    @Update("<script>" +
            "UPDATE sys_department " +
            "<set>" +
            "  <if test='deptName != null'>dept_name = #{deptName},</if>" +
            "  <if test='deptCode != null'>dept_code = #{deptCode},</if>" +
            "  <if test='parentId != null'>parent_id = #{parentId},</if>" +
            "  <if test='ancestors != null'>ancestors = #{ancestors},</if>" +
            "  leader = #{leader}," +
            "  phone = #{phone}," +
            "  email = #{email}," +
            "  <if test='sortOrder != null'>sort_order = #{sortOrder},</if>" +
            "  <if test='status != null'>status = #{status},</if>" +
            "</set>" +
            "WHERE id = #{id} AND del_flag = 0" +
            "</script>")
    int update(Department department);

    @Update("UPDATE sys_department SET del_flag = 1 WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Update("UPDATE sys_department SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM sys_department WHERE parent_id = #{parentId} AND del_flag = 0")
    int countChildren(@Param("parentId") Long parentId);

    @Select("SELECT COUNT(*) FROM sys_user WHERE dept_id = #{deptId}")
    int countUsersByDeptId(@Param("deptId") Long deptId);

    @Select("SELECT COUNT(*) FROM sys_department WHERE del_flag = 0")
    long countAll();

    @Update("UPDATE sys_department SET ancestors = REPLACE(ancestors, #{oldAncestors}, #{newAncestors}) " +
            "WHERE ancestors LIKE CONCAT(#{oldAncestors}, ',%') AND del_flag = 0")
    int updateChildrenAncestors(@Param("oldAncestors") String oldAncestors, @Param("newAncestors") String newAncestors);

    @Update("UPDATE sys_department SET sort_order = #{sortOrder} WHERE id = #{id} AND del_flag = 0")
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);
}
