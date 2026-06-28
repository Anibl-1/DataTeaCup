package com.dataplatform.infra.sentinel;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel流量控制配置
 * 实现限流、熔断、热点参数限流、系统自适应保护
 */
@Slf4j
@Configuration
public class SentinelConfig {

    @Value("${sentinel.flow.api-qps:1000}")
    private int apiQpsLimit;

    @Value("${sentinel.flow.query-concurrent:100}")
    private int queryConcurrentLimit;

    @Value("${sentinel.degrade.error-ratio:0.5}")
    private double degradeErrorRatio;

    @Value("${sentinel.degrade.time-window:30}")
    private int degradeTimeWindow;

    @Value("${sentinel.system.max-qps:5000}")
    private double systemMaxQps;

    @Value("${sentinel.system.max-thread:500}")
    private int systemMaxThread;

    @Value("${sentinel.system.max-load:0.8}")
    private double systemMaxLoad;

    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    @PostConstruct
    public void init() {
        initFlowRules();
        initDegradeRules();
        initParamFlowRules();
        initSystemRules();
        log.info("Sentinel规则初始化完成");
    }

    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        FlowRule apiRule = new FlowRule();
        apiRule.setResource("api-request");
        apiRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        apiRule.setCount(apiQpsLimit);
        apiRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_WARM_UP);
        apiRule.setWarmUpPeriodSec(10);
        rules.add(apiRule);

        FlowRule queryRule = new FlowRule();
        queryRule.setResource("data-query");
        queryRule.setGrade(RuleConstant.FLOW_GRADE_THREAD);
        queryRule.setCount(queryConcurrentLimit);
        rules.add(queryRule);

        FlowRule exportRule = new FlowRule();
        exportRule.setResource("report-export");
        exportRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        exportRule.setCount(50);
        exportRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER);
        exportRule.setMaxQueueingTimeMs(5000);
        rules.add(exportRule);

        FlowRule loginRule = new FlowRule();
        loginRule.setResource("user-login");
        loginRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        loginRule.setCount(100);
        rules.add(loginRule);

        FlowRuleManager.loadRules(rules);
        log.info("加载限流规则: {} 条", rules.size());
    }

    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        DegradeRule externalServiceRule = new DegradeRule();
        externalServiceRule.setResource("external-service");
        externalServiceRule.setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType());
        externalServiceRule.setCount(degradeErrorRatio);
        externalServiceRule.setTimeWindow(degradeTimeWindow);
        externalServiceRule.setMinRequestAmount(10);
        externalServiceRule.setStatIntervalMs(10000);
        rules.add(externalServiceRule);

        DegradeRule dbRule = new DegradeRule();
        dbRule.setResource("database-operation");
        dbRule.setGrade(CircuitBreakerStrategy.SLOW_REQUEST_RATIO.getType());
        dbRule.setCount(0.5);
        dbRule.setTimeWindow(30);
        dbRule.setSlowRatioThreshold(0.5);
        dbRule.setMinRequestAmount(10);
        dbRule.setStatIntervalMs(10000);
        rules.add(dbRule);

        DegradeRule thirdPartyRule = new DegradeRule();
        thirdPartyRule.setResource("third-party-api");
        thirdPartyRule.setGrade(CircuitBreakerStrategy.ERROR_COUNT.getType());
        thirdPartyRule.setCount(10);
        thirdPartyRule.setTimeWindow(60);
        thirdPartyRule.setMinRequestAmount(5);
        rules.add(thirdPartyRule);

        DegradeRuleManager.loadRules(rules);
        log.info("加载熔断规则: {} 条", rules.size());
    }

    private void initParamFlowRules() {
        List<ParamFlowRule> rules = new ArrayList<>();

        ParamFlowRule userRule = new ParamFlowRule();
        userRule.setResource("user-operation");
        userRule.setParamIdx(0);
        userRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        userRule.setCount(100);
        userRule.setDurationInSec(1);
        rules.add(userRule);

        ParamFlowRule tenantRule = new ParamFlowRule();
        tenantRule.setResource("tenant-operation");
        tenantRule.setParamIdx(0);
        tenantRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        tenantRule.setCount(500);
        tenantRule.setDurationInSec(1);
        rules.add(tenantRule);

        ParamFlowRule datasourceRule = new ParamFlowRule();
        datasourceRule.setResource("datasource-query");
        datasourceRule.setParamIdx(0);
        datasourceRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        datasourceRule.setCount(200);
        datasourceRule.setDurationInSec(1);
        rules.add(datasourceRule);

        ParamFlowRuleManager.loadRules(rules);
        log.info("加载热点参数限流规则: {} 条", rules.size());
    }

    private void initSystemRules() {
        List<SystemRule> rules = new ArrayList<>();

        SystemRule qpsRule = new SystemRule();
        qpsRule.setQps(systemMaxQps);
        rules.add(qpsRule);

        SystemRule threadRule = new SystemRule();
        threadRule.setMaxThread(systemMaxThread);
        rules.add(threadRule);

        SystemRule loadRule = new SystemRule();
        loadRule.setHighestSystemLoad(systemMaxLoad);
        rules.add(loadRule);

        SystemRule cpuRule = new SystemRule();
        cpuRule.setHighestCpuUsage(0.9);
        rules.add(cpuRule);

        SystemRuleManager.loadRules(rules);
        log.info("加载系统保护规则: {} 条", rules.size());
    }

    public void updateFlowRule(String resource, int qps) {
        List<FlowRule> currentRules = FlowRuleManager.getRules();
        List<FlowRule> newRules = new ArrayList<>();

        boolean found = false;
        for (FlowRule rule : currentRules) {
            if (rule.getResource().equals(resource)) {
                rule.setCount(qps);
                found = true;
            }
            newRules.add(rule);
        }

        if (!found) {
            FlowRule newRule = new FlowRule();
            newRule.setResource(resource);
            newRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
            newRule.setCount(qps);
            newRules.add(newRule);
        }

        FlowRuleManager.loadRules(newRules);
        log.info("更新限流规则: resource={}, qps={}", resource, qps);
    }

    public void updateDegradeRule(String resource, double errorRatio, int timeWindow) {
        List<DegradeRule> currentRules = DegradeRuleManager.getRules();
        List<DegradeRule> newRules = new ArrayList<>();

        boolean found = false;
        for (DegradeRule rule : currentRules) {
            if (rule.getResource().equals(resource)) {
                rule.setCount(errorRatio);
                rule.setTimeWindow(timeWindow);
                found = true;
            }
            newRules.add(rule);
        }

        if (!found) {
            DegradeRule newRule = new DegradeRule();
            newRule.setResource(resource);
            newRule.setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType());
            newRule.setCount(errorRatio);
            newRule.setTimeWindow(timeWindow);
            newRule.setMinRequestAmount(10);
            newRules.add(newRule);
        }

        DegradeRuleManager.loadRules(newRules);
        log.info("更新熔断规则: resource={}, errorRatio={}, timeWindow={}", resource, errorRatio, timeWindow);
    }

    public List<FlowRule> getFlowRules() {
        return FlowRuleManager.getRules();
    }

    public List<DegradeRule> getDegradeRules() {
        return DegradeRuleManager.getRules();
    }

    public List<ParamFlowRule> getParamFlowRules() {
        return ParamFlowRuleManager.getRules();
    }

    public List<SystemRule> getSystemRules() {
        return SystemRuleManager.getRules();
    }
}
