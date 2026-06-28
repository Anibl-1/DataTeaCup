package com.dataplatform.data.mapper;

import com.dataplatform.data.service.sync.SyncTask;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 同步任务Mapper接口
 */
public interface SyncTaskMapper {

    SyncTask selectById(String id);

    List<SyncTask> selectAll();

    int insert(SyncTask task);

    int updateStatus(@Param("id") String id, @Param("status") String status);

    int update(SyncTask task);

    int deleteById(String id);
}
