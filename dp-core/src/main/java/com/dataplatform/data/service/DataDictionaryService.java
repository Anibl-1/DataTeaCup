package com.dataplatform.data.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.DataDictionary;
import com.dataplatform.data.mapper.DataDictionaryMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 数据字典服务
 */
@Slf4j
@Service
public class DataDictionaryService {

    @Autowired
    private DataDictionaryMapper dataDictionaryMapper;

    @Autowired
    private CacheService cacheService;

    private static final String CACHE_KEY_DICT_TYPE_PREFIX = "dict:type:";
    private static final String CACHE_KEY_DICT_TYPES = "dict:types";
    private static final long DICT_CACHE_TTL = 3600; // 1小时

    /**
     * 分页查询
     */
    public IPage<DataDictionary> getPage(int page, int pageSize, String dictType, String keyword, Integer status) {
        Page<DataDictionary> pageParam = new Page<>(page, pageSize);
        return dataDictionaryMapper.selectPage(pageParam, dictType, keyword, status);
    }

    /**
     * 列表查询
     */
    public List<DataDictionary> getList(String dictType, String keyword, Integer status) {
        return dataDictionaryMapper.selectList(dictType, keyword, status);
    }

    /**
     * 按字典类型查询（前端下拉用）
     */
    public List<DataDictionary> getByType(String dictType) {
        String cacheKey = CACHE_KEY_DICT_TYPE_PREFIX + dictType;
        List<DataDictionary> cached = cacheService.get(cacheKey, new TypeReference<List<DataDictionary>>() {});
        if (cached != null) {
            return cached;
        }
        List<DataDictionary> list = dataDictionaryMapper.selectByType(dictType);
        cacheService.set(cacheKey, list, DICT_CACHE_TTL, TimeUnit.SECONDS);
        return list;
    }

    /**
     * 获取所有字典类型
     */
    public List<String> getDistinctTypes() {
        List<String> cached = cacheService.get(CACHE_KEY_DICT_TYPES, new TypeReference<List<String>>() {});
        if (cached != null) {
            return cached;
        }
        List<String> types = dataDictionaryMapper.selectDistinctTypes();
        cacheService.set(CACHE_KEY_DICT_TYPES, types, DICT_CACHE_TTL, TimeUnit.SECONDS);
        return types;
    }

    /**
     * 根据ID获取
     */
    public DataDictionary getById(Long id) {
        return dataDictionaryMapper.selectById(id);
    }

    /**
     * 创建字典项
     */
    @Transactional
    public int create(DataDictionary dict) {
        // 校验唯一性
        if (dataDictionaryMapper.countByTypeAndCode(dict.getDictType(), dict.getDictCode()) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典类型+编码已存在");
        }
        dict.setCreateTime(LocalDateTime.now());
        dict.setUpdateTime(LocalDateTime.now());
        if (dict.getStatus() == null) {
            dict.setStatus(1);
        }
        if (dict.getSortOrder() == null) {
            dict.setSortOrder(0);
        }
        if (dict.getIsDefault() == null) {
            dict.setIsDefault(false);
        }
        int result = dataDictionaryMapper.insert(dict);
        clearDictCache(dict.getDictType());
        return result;
    }

    /**
     * 更新字典项
     */
    @Transactional
    public int update(DataDictionary dict) {
        DataDictionary existing = dataDictionaryMapper.selectById(dict.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "字典项不存在");
        }
        // 如果修改了类型或编码，检查唯一性
        if (!existing.getDictType().equals(dict.getDictType()) || !existing.getDictCode().equals(dict.getDictCode())) {
            if (dataDictionaryMapper.countByTypeAndCode(dict.getDictType(), dict.getDictCode()) > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "字典类型+编码已存在");
            }
        }
        dict.setUpdateTime(LocalDateTime.now());
        int result = dataDictionaryMapper.update(dict);
        clearDictCache(dict.getDictType());
        // 如果类型变更，也清除旧类型缓存
        if (!existing.getDictType().equals(dict.getDictType())) {
            clearDictCache(existing.getDictType());
        }
        return result;
    }

    /**
     * 删除字典项
     */
    @Transactional
    public int delete(Long id) {
        DataDictionary existing = dataDictionaryMapper.selectById(id);
        int result = dataDictionaryMapper.deleteById(id);
        if (existing != null) {
            clearDictCache(existing.getDictType());
        }
        return result;
    }

    /**
     * 批量删除
     */
    @Transactional
    public int batchDelete(List<Long> ids) {
        int result = dataDictionaryMapper.batchDelete(ids);
        // 批量删除时清除所有字典缓存
        cacheService.deleteByPattern(CACHE_KEY_DICT_TYPE_PREFIX + "*");
        cacheService.delete(CACHE_KEY_DICT_TYPES);
        return result;
    }

    /**
     * 清除字典缓存
     */
    private void clearDictCache(String dictType) {
        if (dictType != null) {
            cacheService.delete(CACHE_KEY_DICT_TYPE_PREFIX + dictType);
        }
        cacheService.delete(CACHE_KEY_DICT_TYPES);
        log.info("字典缓存已清除: type={}", dictType);
    }
}
