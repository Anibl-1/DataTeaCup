package com.dataplatform.data.service.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * 热点参数限流服务
 * 针对特定参数值进行精细化限流控制
 */
@Slf4j
@Service
public class ParamFlowService {

    /**
     * 执行带热点参数限流的用户操作
     */
    public <T> T executeUserOperation(Long userId, Supplier<T> supplier, Supplier<T> fallback) {
        return executeWithParamFlow("user-operation", userId, supplier, fallback);
    }

    /**
     * 执行带热点参数限流的租户操作
     */
    public <T> T executeTenantOperation(Long tenantId, Supplier<T> supplier, Supplier<T> fallback) {
        return executeWithParamFlow("tenant-operation", tenantId, supplier, fallback);
    }

    /**
     * 执行带热点参数限流的数据源查询
     */
    public <T> T executeDatasourceQuery(Long datasourceId, Supplier<T> supplier, Supplier<T> fallback) {
        return executeWithParamFlow("datasource-query", datasourceId, supplier, fallback);
    }

    /**
     * 通用热点参数限流执行
     */
    public <T> T executeWithParamFlow(String resource, Object param, Supplier<T> supplier, Supplier<T> fallback) {
        Entry entry = null;
        try {
            entry = SphU.entry(resource, com.alibaba.csp.sentinel.EntryType.IN, 1, param);
            return supplier.get();
        } catch (BlockException e) {
            log.warn("热点参数限流触发: resource={}, param={}", resource, param);
            return fallback != null ? fallback.get() : null;
        } finally {
            if (entry != null) {
                entry.exit(1, param);
            }
        }
    }

    /**
     * 添加热点参数限流规则
     */
    public void addParamFlowRule(String resource, int paramIndex, int qps) {
        List<ParamFlowRule> currentRules = new ArrayList<>(ParamFlowRuleManager.getRules());
        
        ParamFlowRule rule = new ParamFlowRule();
        rule.setResource(resource);
        rule.setParamIdx(paramIndex);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(qps);
        rule.setDurationInSec(1);
        
        currentRules.add(rule);
        ParamFlowRuleManager.loadRules(currentRules);
        log.info("添加热点参数限流规则: resource={}, paramIndex={}, qps={}", resource, paramIndex, qps);
    }

    /**
     * 添加带特例的热点参数限流规则
     * 可以为特定参数值设置不同的限流阈值
     */
    public void addParamFlowRuleWithExceptions(String resource, int paramIndex, int defaultQps, 
                                                List<ParamFlowItem> exceptions) {
        List<ParamFlowRule> currentRules = new ArrayList<>(ParamFlowRuleManager.getRules());
        
        ParamFlowRule rule = new ParamFlowRule();
        rule.setResource(resource);
        rule.setParamIdx(paramIndex);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(defaultQps);
        rule.setDurationInSec(1);
        rule.setParamFlowItemList(exceptions);
        
        currentRules.add(rule);
        ParamFlowRuleManager.loadRules(currentRules);
        log.info("添加带特例的热点参数限流规则: resource={}, paramIndex={}, defaultQps={}, exceptions={}", 
            resource, paramIndex, defaultQps, exceptions.size());
    }

    /**
     * 为VIP用户设置更高的限流阈值
     */
    public void setVipUserQuota(String resource, Long userId, int qps) {
        List<ParamFlowRule> currentRules = new ArrayList<>(ParamFlowRuleManager.getRules());
        
        // 查找现有规则
        ParamFlowRule targetRule = null;
        for (ParamFlowRule rule : currentRules) {
            if (rule.getResource().equals(resource)) {
                targetRule = rule;
                break;
            }
        }
        
        if (targetRule == null) {
            targetRule = new ParamFlowRule();
            targetRule.setResource(resource);
            targetRule.setParamIdx(0);
            targetRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
            targetRule.setCount(100);
            targetRule.setDurationInSec(1);
            currentRules.add(targetRule);
        }
        
        // 添加VIP用户特例
        List<ParamFlowItem> items = targetRule.getParamFlowItemList();
        if (items == null) {
            items = new ArrayList<>();
        }
        
        ParamFlowItem vipItem = new ParamFlowItem();
        vipItem.setObject(String.valueOf(userId));
        vipItem.setClassType(Long.class.getName());
        vipItem.setCount(qps);
        items.add(vipItem);
        
        targetRule.setParamFlowItemList(items);
        ParamFlowRuleManager.loadRules(currentRules);
        log.info("设置VIP用户限流配额: resource={}, userId={}, qps={}", resource, userId, qps);
    }

    /**
     * 为租户设置限流配额
     */
    public void setTenantQuota(Long tenantId, int qps) {
        setVipUserQuota("tenant-operation", tenantId, qps);
    }

    /**
     * 移除热点参数限流规则
     */
    public void removeParamFlowRule(String resource) {
        List<ParamFlowRule> currentRules = new ArrayList<>(ParamFlowRuleManager.getRules());
        currentRules.removeIf(rule -> rule.getResource().equals(resource));
        ParamFlowRuleManager.loadRules(currentRules);
        log.info("移除热点参数限流规则: resource={}", resource);
    }

    /**
     * 获取所有热点参数限流规则
     */
    public List<ParamFlowRule> getAllParamFlowRules() {
        return ParamFlowRuleManager.getRules();
    }

    /**
     * 获取指定资源的热点参数限流规则
     */
    public ParamFlowRule getParamFlowRule(String resource) {
        return ParamFlowRuleManager.getRules().stream()
            .filter(rule -> rule.getResource().equals(resource))
            .findFirst()
            .orElse(null);
    }
}
