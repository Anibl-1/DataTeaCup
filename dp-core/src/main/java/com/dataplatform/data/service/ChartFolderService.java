package com.dataplatform.data.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.ChartFolder;
import com.dataplatform.data.mapper.ChartFolderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图表文件夹服务
 */
@Service
public class ChartFolderService {

    @Autowired
    private ChartFolderMapper chartFolderMapper;

    /**
     * 获取文件夹树
     */
    public List<ChartFolder> getTree() {
        List<ChartFolder> all = chartFolderMapper.selectAll();
        return buildTree(all, 0L);
    }

    /**
     * 获取全部列表
     */
    public List<ChartFolder> getAll() {
        return chartFolderMapper.selectAll();
    }

    /**
     * 创建文件夹
     */
    @Transactional
    public int create(ChartFolder folder) {
        folder.setCreateTime(LocalDateTime.now());
        if (folder.getParentId() == null) {
            folder.setParentId(0L);
        }
        if (folder.getSortOrder() == null) {
            folder.setSortOrder(0);
        }
        return chartFolderMapper.insert(folder);
    }

    /**
     * 更新文件夹
     */
    @Transactional
    public int update(ChartFolder folder) {
        return chartFolderMapper.update(folder);
    }

    /**
     * 删除文件夹
     */
    @Transactional
    public int delete(Long id) {
        // 检查是否有子文件夹
        if (chartFolderMapper.countChildren(id) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该文件夹下有子文件夹，不允许删除");
        }
        // 检查是否有关联的图表
        if (chartFolderMapper.countChartsByFolderId(id) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该文件夹下有图表，不允许删除");
        }
        return chartFolderMapper.deleteById(id);
    }

    private List<ChartFolder> buildTree(List<ChartFolder> all, Long parentId) {
        Map<Long, List<ChartFolder>> grouped = all.stream()
                .collect(Collectors.groupingBy(f -> f.getParentId() != null ? f.getParentId() : 0L));
        return buildChildren(grouped, parentId);
    }

    private List<ChartFolder> buildChildren(Map<Long, List<ChartFolder>> grouped, Long parentId) {
        List<ChartFolder> children = grouped.getOrDefault(parentId, new ArrayList<>());
        for (ChartFolder child : children) {
            child.setChildren(buildChildren(grouped, child.getId()));
        }
        return children;
    }
}
