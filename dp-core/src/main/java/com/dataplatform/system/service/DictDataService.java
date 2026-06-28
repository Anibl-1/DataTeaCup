package com.dataplatform.system.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.system.entity.DictData;
import com.dataplatform.system.mapper.DictDataMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典数据服务
 */
@Service
public class DictDataService {

    private final DictDataMapper dictDataMapper;
    private final DictDataService self;

    public DictDataService(DictDataMapper dictDataMapper, @Lazy DictDataService self) {
        this.dictDataMapper = dictDataMapper;
        this.self = self;
    }

    /**
     * 根据ID查询字典数据
     */
    public DictData getById(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典数据ID不能为空");
        }
        return dictDataMapper.selectById(id);
    }

    /**
     * 按类型编码查询所有数据项
     */
    public List<DictData> listByDictCode(String dictCode) {
        if (!StringUtils.hasText(dictCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典类型编码不能为空");
        }
        return dictDataMapper.selectByDictCode(dictCode);
    }

    /**
     * 按类型编码查询启用的数据项（过滤 status=1，按 sortOrder 升序排列）
     * 使用 Redis 缓存，key 格式 dict:data:{dictCode}
     */
    @Cacheable(cacheNames = "dictDataCache", key = "'dict:data:' + #dictCode")
    public List<DictData> listEnabledByDictCode(String dictCode) {
        if (!StringUtils.hasText(dictCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典类型编码不能为空");
        }
        return dictDataMapper.selectEnabledByDictCode(dictCode);
    }

    /**
     * 批量查询多个字典类型的数据（过滤启用+排序）
     * 通过 self 代理调用 listEnabledByDictCode，确保缓存注解生效
     */
    public Map<String, List<DictData>> listBatch(List<String> dictCodes) {
        Map<String, List<DictData>> result = new HashMap<>();
        if (dictCodes == null || dictCodes.isEmpty()) {
            return result;
        }
        for (String dictCode : dictCodes) {
            if (StringUtils.hasText(dictCode)) {
                result.put(dictCode, self.listEnabledByDictCode(dictCode));
            }
        }
        return result;
    }

    /**
     * 创建字典数据项，清除对应 dictCode 的缓存
     */
    @Transactional
    @CacheEvict(cacheNames = "dictDataCache", key = "'dict:data:' + #dictData.dictCode")
    public DictData create(DictData dictData) {
        if (dictData == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典数据信息不能为空");
        }
        if (!StringUtils.hasText(dictData.getDictCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典类型编码不能为空");
        }
        if (!StringUtils.hasText(dictData.getLabel())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典标签不能为空");
        }
        if (!StringUtils.hasText(dictData.getValue())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典值不能为空");
        }
        if (dictData.getStatus() == null) {
            dictData.setStatus(1);
        }
        if (dictData.getSortOrder() == null) {
            dictData.setSortOrder(0);
        }
        dictData.setCreateTime(LocalDateTime.now());
        dictData.setUpdateTime(LocalDateTime.now());
        dictDataMapper.insert(dictData);
        return dictData;
    }

    /**
     * 更新字典数据项，清除对应 dictCode 的缓存
     */
    @Transactional
    @CacheEvict(cacheNames = "dictDataCache", key = "'dict:data:' + #dictData.dictCode")
    public DictData update(DictData dictData) {
        if (dictData == null || dictData.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典数据信息或ID不能为空");
        }
        DictData existing = dictDataMapper.selectById(dictData.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "字典数据不存在");
        }
        dictData.setUpdateTime(LocalDateTime.now());
        dictDataMapper.update(dictData);
        return dictDataMapper.selectById(dictData.getId());
    }

    /**
     * 删除字典数据项，清除对应 dictCode 的缓存
     */
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典数据ID不能为空");
        }
        DictData existing = dictDataMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "字典数据不存在");
        }
        dictDataMapper.deleteById(id);
        self.evictDictDataCache(existing.getDictCode());
    }

    /**
     * 清除指定 dictCode 的缓存（供 DictTypeService 级联删除时调用）
     */
    @CacheEvict(cacheNames = "dictDataCache", key = "'dict:data:' + #dictCode")
    public void evictDictDataCache(String dictCode) {
        // 仅用于清除缓存，无业务逻辑
    }
}
