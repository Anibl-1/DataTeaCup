package com.dataplatform.system.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.common.service.SystemConfigProvider;
import com.dataplatform.common.util.AesEncryptUtil;
import com.dataplatform.system.entity.SystemConfig;
import com.dataplatform.system.mapper.SystemConfigMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SystemConfigService implements SystemConfigProvider {

    private static final String PASSWORD_MASK = "******";

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private ConfigCacheService configCacheService;

    public IPage<SystemConfig> getPage(int page, int pageSize, String keyword) {
        Page<SystemConfig> pageParam = new Page<>(page, pageSize);
        IPage<SystemConfig> result = systemConfigMapper.selectPage(pageParam, keyword);
        maskPasswordValues(result.getRecords());
        return result;
    }

    public List<SystemConfig> getList(String keyword) {
        List<SystemConfig> list = systemConfigMapper.selectList(keyword);
        maskPasswordValues(list);
        return list;
    }

    /**
     * 获取所有配置分组名称
     */
    public List<String> listConfigGroups() {
        return systemConfigMapper.selectConfigGroups();
    }

    /**
     * 按分组查询配置项
     */
    public List<SystemConfig> listByGroup(String configGroup) {
        List<SystemConfig> list = systemConfigMapper.selectByGroup(configGroup);
        maskPasswordValues(list);
        return list;
    }

    /**
     * 对 password 类型的配置项进行脱敏处理
     */
    private void maskPasswordValues(List<SystemConfig> configs) {
        if (configs == null) {
            return;
        }
        for (SystemConfig config : configs) {
            if ("password".equals(config.getConfigType())) {
                config.setConfigValue(PASSWORD_MASK);
            }
        }
    }

    @Override
    public String getValueByKey(String configKey) {
        SystemConfig config = systemConfigMapper.selectByKey(configKey);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public String getValueByKey(String configKey, String defaultValue) {
        String value = getValueByKey(configKey);
        return value != null ? value : defaultValue;
    }

    public SystemConfig getById(Long id) {
        return systemConfigMapper.selectById(id);
    }

    @Transactional
    public int create(SystemConfig config) {
        if (systemConfigMapper.selectByKey(config.getConfigKey()) != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "配置键已存在");
        }
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        if (config.getConfigType() == null) { config.setConfigType("string"); }
        if (config.getIsSystem() == null) { config.setIsSystem(false); }
        if (config.getConfigGroup() == null || config.getConfigGroup().isBlank()) {
            config.setConfigGroup("默认");
        }
        validateConfigValue(config.getConfigType(), config.getConfigValue());
        encryptIfPassword(config);
        int result = systemConfigMapper.insert(config);
        if (result > 0) {
            configCacheService.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }

    @Transactional
    public int update(SystemConfig config) {
        config.setUpdateTime(LocalDateTime.now());
        if (config.getConfigGroup() != null && config.getConfigGroup().isBlank()) {
            config.setConfigGroup("默认");
        }
        validateConfigValue(config.getConfigType(), config.getConfigValue());
        encryptIfPassword(config);
        // Resolve configKey for cache sync if not provided on the update object
        String cacheKey = config.getConfigKey();
        if (cacheKey == null && config.getId() != null) {
            SystemConfig existing = systemConfigMapper.selectById(config.getId());
            if (existing != null) {
                cacheKey = existing.getConfigKey();
            }
        }
        int result = systemConfigMapper.update(config);
        if (result > 0 && cacheKey != null) {
            configCacheService.put(cacheKey, config.getConfigValue());
        }
        return result;
    }

    /**
     * 如果配置类型为 password，则对 configValue 进行 AES 加密
     */
    private void encryptIfPassword(SystemConfig config) {
        if ("password".equals(config.getConfigType()) && config.getConfigValue() != null) {
            config.setConfigValue(AesEncryptUtil.encrypt(config.getConfigValue()));
        }
    }

    /**
     * 根据配置类型校验配置值的格式
     * number: 必须可解析为 Double
     * boolean: 必须为 "true" 或 "false"（不区分大小写）
     * json: 必须为有效 JSON
     * password/string: 不做格式校验
     */
    private void validateConfigValue(String configType, String configValue) {
        if (configType == null || configValue == null) {
            return;
        }
        switch (configType) {
            case "number":
                try {
                    Double.parseDouble(configValue);
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "配置值不是有效数字");
                }
                break;
            case "boolean":
                if (!"true".equalsIgnoreCase(configValue) && !"false".equalsIgnoreCase(configValue)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "配置值必须为 true 或 false");
                }
                break;
            case "json":
                try {
                    new ObjectMapper().readTree(configValue);
                } catch (Exception e) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "配置值不是有效 JSON");
                }
                break;
            case "password":
                // password 类型不做格式校验
                break;
            default:
                // string 及其他类型不做格式校验
                break;
        }
    }

    @Transactional
    public int delete(Long id) {
        SystemConfig existing = systemConfigMapper.selectById(id);
        if (existing != null && Boolean.TRUE.equals(existing.getIsSystem())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "系统配置不允许删除");
        }
        int result = systemConfigMapper.deleteById(id);
        if (result > 0 && existing != null) {
            configCacheService.evict(existing.getConfigKey());
        }
        return result;
    }
}
