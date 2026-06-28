package com.dataplatform.system.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.system.entity.DictType;
import com.dataplatform.system.mapper.DictDataMapper;
import com.dataplatform.system.mapper.DictTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 字典类型服务
 */
@Service
@RequiredArgsConstructor
public class DictTypeService {

    private final DictTypeMapper dictTypeMapper;
    private final DictDataMapper dictDataMapper;
    private final DictDataService dictDataService;

    /**
     * 查询所有字典类型
     */
    public List<DictType> list() {
        return dictTypeMapper.selectList();
    }

    /**
     * 根据ID查询字典类型
     */
    public DictType getById(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典类型ID不能为空");
        }
        return dictTypeMapper.selectById(id);
    }

    /**
     * 根据编码查询字典类型
     */
    public DictType getByDictCode(String dictCode) {
        if (!StringUtils.hasText(dictCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典类型编码不能为空");
        }
        return dictTypeMapper.selectByDictCode(dictCode);
    }

    /**
     * 创建字典类型（编码唯一性验证）
     */
    @Transactional
    public DictType create(DictType dictType) {
        if (dictType == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典类型信息不能为空");
        }
        if (!StringUtils.hasText(dictType.getDictCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典类型编码不能为空");
        }
        if (!StringUtils.hasText(dictType.getDictName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典类型名称不能为空");
        }
        // 编码唯一性验证
        DictType existing = dictTypeMapper.selectByDictCode(dictType.getDictCode());
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典类型编码已存在: " + dictType.getDictCode());
        }
        if (dictType.getStatus() == null) {
            dictType.setStatus(1);
        }
        dictType.setCreateTime(LocalDateTime.now());
        dictType.setUpdateTime(LocalDateTime.now());
        dictTypeMapper.insert(dictType);
        return dictType;
    }

    /**
     * 更新字典类型
     */
    @Transactional
    public DictType update(DictType dictType) {
        if (dictType == null || dictType.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典类型信息或ID不能为空");
        }
        DictType existing = dictTypeMapper.selectById(dictType.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "字典类型不存在");
        }
        dictType.setUpdateTime(LocalDateTime.now());
        dictTypeMapper.update(dictType);
        return dictTypeMapper.selectById(dictType.getId());
    }

    /**
     * 删除字典类型（级联删除该类型下所有数据项）
     */
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字典类型ID不能为空");
        }
        DictType dictType = dictTypeMapper.selectById(id);
        if (dictType == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "字典类型不存在");
        }
        // 级联删除该类型下所有数据项
        dictDataMapper.deleteByDictCode(dictType.getDictCode());
        dictTypeMapper.deleteById(id);
        // 清除该字典类型对应的缓存
        dictDataService.evictDictDataCache(dictType.getDictCode());
    }
}
