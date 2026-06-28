package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.CollectTask;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采集任务Mapper接口
 * 
 * @author dataplatform
 */
public interface CollectTaskMapper {
    /**
     * 根据ID查询采集任务
     * 
     * @param id 任务ID
     * @return 采集任务信息
     */
    CollectTask selectById(Long id);
    
    /**
     * 分页查询采集任务列表
     * 
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @param filters 筛选条件列表
     * @return 采集任务列表
     */
    List<CollectTask> selectList(@Param("offset") Integer offset, @Param("pageSize") Integer pageSize, 
                                 @Param("filters") java.util.List<com.dataplatform.common.dto.FilterCondition> filters);
    
    /**
     * 查询采集任务总数
     * 
     * @param filters 筛选条件列表
     * @return 采集任务总数
     */
    long count(@Param("filters") java.util.List<com.dataplatform.common.dto.FilterCondition> filters);
    
    /**
     * 查询今日创建的采集任务数量
     * 
     * @return 今日创建的任务数量
     */
    long countToday();
    
    /**
     * 插入采集任务
     * 
     * @param task 采集任务信息
     * @return 影响行数
     */
    int insert(CollectTask task);
    
    /**
     * 更新采集任务
     * 
     * @param task 采集任务信息
     * @return 影响行数
     */
    int update(CollectTask task);
    
    /**
     * 删除采集任务
     * 
     * @param id 任务ID
     * @return 影响行数
     */
    int delete(Long id);
    
    /**
     * 查询需要执行的定时任务
     * 条件：启用定时任务 且 下次执行时间 <= 当前时间 且 状态不是运行中
     * 
     * @return 待执行的任务列表
     */
    List<CollectTask> selectScheduledTasksToExecute();
    
    /**
     * 查询运行中且启用定时的任务
     * 用于调度器检查
     * 
     * @return 运行中的定时任务列表
     */
    List<CollectTask> selectScheduledRunningTasks();
    
    /**
     * 更新下次执行时间
     * 
     * @param id 任务ID
     * @param nextExecuteTime 下次执行时间
     * @return 影响行数
     */
    int updateNextExecuteTime(@Param("id") Long id, @Param("nextExecuteTime") LocalDateTime nextExecuteTime);
    
    /**
     * 增加执行次数
     * 
     * @param id 任务ID
     * @return 影响行数
     */
    int incrementExecuteCount(@Param("id") Long id);
    
    /**
     * 增加成功次数
     * 
     * @param id 任务ID
     * @return 影响行数
     */
    int incrementSuccessCount(@Param("id") Long id);
    
    /**
     * 增加失败次数
     * 
     * @param id 任务ID
     * @return 影响行数
     */
    int incrementFailCount(@Param("id") Long id);

    /**
     * 按日期统计采集任务数量
     * 
     * @param date 日期字符串 (yyyy-MM-dd)
     * @return 该日期创建的任务数量
     */
    long countByDate(@Param("date") String date);
    
    /**
     * 统计本周创建的采集任务数量
     * 
     * @return 本周创建的任务数量
     */
    long countThisWeek();
    
    /**
     * 按状态统计采集任务数量
     * 
     * @param status 任务状态
     * @return 该状态的任务数量
     */
    long countByStatus(@Param("status") Integer status);
}

