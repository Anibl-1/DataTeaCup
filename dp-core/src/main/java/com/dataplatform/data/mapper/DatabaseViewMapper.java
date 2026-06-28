package com.dataplatform.data.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

/**
 * 数据库视图查询Mapper
 * 使用视图简化复杂查询，提升性能
 */
@Mapper
public interface DatabaseViewMapper {
    
    /**
     * 查询采集任务详情（包含数据源信息）
     * 使用视图：v_collect_task_detail
     * 优势：一次查询获取完整信息，比传统3次查询快3倍
     */
    @Select("SELECT * FROM v_collect_task_detail WHERE id = #{id}")
    Map<String, Object> getTaskDetailById(@Param("id") Long id);
    
    /**
     * 查询所有采集任务详情（分页）
     */
    @Select("SELECT * FROM v_collect_task_detail ORDER BY id DESC LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> getTaskDetailList(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 查询活跃任务（正在运行的任务）
     * 使用视图：v_active_tasks
     * 应用场景：实时监控页面
     */
    @Select("SELECT * FROM v_active_tasks ORDER BY running_minutes DESC")
    List<Map<String, Object>> getActiveTasks();
    
    /**
     * 查询增量采集任务及其进度
     * 使用视图：v_incremental_tasks
     * 应用场景：增量采集监控
     */
    @Select("SELECT * FROM v_incremental_tasks ORDER BY last_execute_time DESC")
    List<Map<String, Object>> getIncrementalTasks();
    
    /**
     * 查询数据源使用情况统计
     * 使用视图：v_datasource_usage
     * 应用场景：数据源管理页面、统计分析
     */
    @Select("SELECT * FROM v_datasource_usage")
    List<Map<String, Object>> getDataSourceUsage();
    
    /**
     * 查询用户权限（用户→角色→权限）
     * 使用视图：v_user_permissions
     * 应用场景：权限验证、用户管理
     */
    @Select("SELECT DISTINCT permission_code, permission_name FROM v_user_permissions WHERE username = #{username}")
    List<Map<String, Object>> getUserPermissions(@Param("username") String username);
    
    /**
     * 查询用户菜单
     * 使用视图：v_user_menus
     * 应用场景：动态菜单生成
     */
    @Select("SELECT * FROM v_user_menus WHERE username = #{username} ORDER BY sort_order")
    List<Map<String, Object>> getUserMenus(@Param("username") String username);
    
    /**
     * 查询未读通知
     * 使用视图：v_unread_notifications
     * 应用场景：通知中心
     */
    @Select("SELECT * FROM v_unread_notifications WHERE target_user_id = #{userId} LIMIT #{limit}")
    List<Map<String, Object>> getUnreadNotifications(@Param("userId") Long userId, @Param("limit") int limit);
    
    /**
     * 查询操作日志汇总（最近7天）
     * 使用视图：v_operation_log_summary
     * 应用场景：系统监控、日志分析
     */
    @Select("SELECT * FROM v_operation_log_summary ORDER BY operation_count DESC LIMIT 20")
    List<Map<String, Object>> getOperationLogSummary();
}
