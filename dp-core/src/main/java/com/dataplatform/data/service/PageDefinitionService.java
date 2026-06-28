package com.dataplatform.data.service;

import com.dataplatform.common.constants.Constants;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.PageChart;
import com.dataplatform.data.entity.PageDefinition;
import com.dataplatform.data.mapper.PageChartMapper;
import com.dataplatform.data.mapper.PageDefinitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 页面定义服务类
 * 
 * @author dataplatform
 */
@Service
@RequiredArgsConstructor
public class PageDefinitionService {
    private final PageDefinitionMapper pageDefinitionMapper;
    private final PageChartMapper pageChartMapper;
    private final LicenseLimitService licenseLimitService;
    
    /**
     * 获取页面定义列表（分页，按布局模式过滤）
     */
    public List<PageDefinition> getPageDefinitionList(Integer page, Integer pageSize, String keyword, String layoutMode) {
        if (page == null || page < 1) {
            page = Constants.DEFAULT_PAGE;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = Constants.DEFAULT_PAGE_SIZE;
        }
        int offset = (page - 1) * pageSize;
        return pageDefinitionMapper.selectList(offset, pageSize, keyword, layoutMode);
    }
    
    /**
     * 获取页面定义总数（按布局模式过滤）
     */
    public long getPageDefinitionCount(String keyword, String layoutMode) {
        return pageDefinitionMapper.count(keyword, layoutMode);
    }
    
    /**
     * 按布局模式统计页面数量
     */
    public long countByLayoutMode(String layoutMode) {
        return pageDefinitionMapper.countByLayoutMode(layoutMode);
    }
    
    /**
     * 根据ID获取页面定义（包含图表列表）
     */
    public PageDefinition getPageDefinitionById(Long id) {
        PageDefinition page = pageDefinitionMapper.selectById(id);
        if (page != null) {
            List<PageChart> charts = pageChartMapper.selectByPageId(id);
            page.setCharts(charts);
        }
        return page;
    }
    
    /**
     * 根据编码获取页面定义
     */
    public PageDefinition getPageDefinitionByCode(String pageCode) {
        PageDefinition page = pageDefinitionMapper.selectByCode(pageCode);
        if (page != null) {
            List<PageChart> charts = pageChartMapper.selectByPageId(page.getId());
            page.setCharts(charts);
        }
        return page;
    }
    
    /**
     * 创建页面定义
     */
    @Transactional
    public PageDefinition createPageDefinition(PageDefinition page, List<PageChart> charts) {
        licenseLimitService.assertChartPageCreationAllowed(pageDefinitionMapper.countAll());

        if (!StringUtils.hasText(page.getPageName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "页面名称不能为空");
        }
        if (!StringUtils.hasText(page.getPageCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "页面编码不能为空");
        }
        
        // 检查编码是否已存在
        PageDefinition existing = pageDefinitionMapper.selectByCode(page.getPageCode());
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "页面编码已存在: " + page.getPageCode());
        }
        
        if (page.getStatus() == null) {
            page.setStatus(Constants.STATUS_ENABLED);
        }
        
        pageDefinitionMapper.insert(page);
        
        // 保存图表关联
        if (charts != null && !charts.isEmpty()) {
            for (PageChart chart : charts) {
                chart.setPageId(page.getId());
            }
            pageChartMapper.batchInsert(charts);
        }
        
        return page;
    }
    
    /**
     * 更新页面定义
     */
    @Transactional
    public PageDefinition updatePageDefinition(PageDefinition page, List<PageChart> charts) {
        if (page.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "页面ID不能为空");
        }
        
        PageDefinition existing = pageDefinitionMapper.selectById(page.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "页面定义不存在");
        }
        
        if (StringUtils.hasText(page.getPageCode()) && !page.getPageCode().equals(existing.getPageCode())) {
            PageDefinition codeExists = pageDefinitionMapper.selectByCode(page.getPageCode());
            if (codeExists != null && !codeExists.getId().equals(page.getId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "页面编码已存在: " + page.getPageCode());
            }
        }
        
        pageDefinitionMapper.update(page);
        
        // 删除旧的图表关联，重新插入
        pageChartMapper.deleteByPageId(page.getId());
        if (charts != null && !charts.isEmpty()) {
            for (PageChart chart : charts) {
                chart.setPageId(page.getId());
            }
            pageChartMapper.batchInsert(charts);
        }
        
        return getPageDefinitionById(page.getId());
    }
    
    /**
     * 删除页面定义
     */
    @Transactional
    public void deletePageDefinition(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "页面ID不能为空");
        }
        
        PageDefinition page = pageDefinitionMapper.selectById(id);
        if (page == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "页面定义不存在");
        }
        
        // 删除图表关联（CASCADE会自动删除）
        pageChartMapper.deleteByPageId(id);
        // 删除页面定义
        pageDefinitionMapper.delete(id);
    }
}

