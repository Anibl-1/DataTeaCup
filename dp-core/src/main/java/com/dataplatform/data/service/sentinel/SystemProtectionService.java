package com.dataplatform.data.service.sentinel;

import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统自适应保护服务
 * 根据系统负载自动调整保护策略
 */
@Slf4j
@Service
public class SystemProtectionService {

    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hardware = systemInfo.getHardware();
    private final CentralProcessor processor = hardware.getProcessor();
    private final GlobalMemory memory = hardware.getMemory();

    /**
     * 获取当前系统状态
     */
    public SystemStatus getSystemStatus() {
        SystemStatus status = new SystemStatus();
        
        // CPU使用率
        double[] loadAverage = processor.getSystemLoadAverage(1);
        status.setCpuUsage(loadAverage.length > 0 ? loadAverage[0] / processor.getLogicalProcessorCount() : 0);
        
        // 内存使用
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        status.setMemoryUsage((double) (totalMemory - availableMemory) / totalMemory);
        status.setTotalMemory(totalMemory);
        status.setAvailableMemory(availableMemory);
        
        // JVM信息
        Runtime runtime = Runtime.getRuntime();
        status.setJvmHeapUsed(runtime.totalMemory() - runtime.freeMemory());
        status.setJvmHeapMax(runtime.maxMemory());
        status.setJvmHeapUsage((double) status.getJvmHeapUsed() / status.getJvmHeapMax());
        
        // 线程数
        status.setActiveThreads(Thread.activeCount());
        
        // 系统负载
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        status.setSystemLoad(osBean.getSystemLoadAverage());
        
        // 运行时间
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        status.setUptime(runtimeBean.getUptime());
        
        return status;
    }

    /**
     * 更新系统QPS保护阈值
     */
    public void updateQpsProtection(double maxQps) {
        List<SystemRule> rules = new ArrayList<>(SystemRuleManager.getRules());
        
        // 移除旧的QPS规则
        rules.removeIf(rule -> rule.getQps() > 0);
        
        // 添加新规则
        SystemRule qpsRule = new SystemRule();
        qpsRule.setQps(maxQps);
        rules.add(qpsRule);
        
        SystemRuleManager.loadRules(rules);
        log.info("更新系统QPS保护阈值: {}", maxQps);
    }

    /**
     * 更新系统线程数保护阈值
     */
    public void updateThreadProtection(int maxThread) {
        List<SystemRule> rules = new ArrayList<>(SystemRuleManager.getRules());
        
        // 移除旧的线程数规则
        rules.removeIf(rule -> rule.getMaxThread() > 0);
        
        // 添加新规则
        SystemRule threadRule = new SystemRule();
        threadRule.setMaxThread(maxThread);
        rules.add(threadRule);
        
        SystemRuleManager.loadRules(rules);
        log.info("更新系统线程数保护阈值: {}", maxThread);
    }

    /**
     * 更新系统负载保护阈值
     */
    public void updateLoadProtection(double maxLoad) {
        List<SystemRule> rules = new ArrayList<>(SystemRuleManager.getRules());
        
        // 移除旧的负载规则
        rules.removeIf(rule -> rule.getHighestSystemLoad() > 0);
        
        // 添加新规则
        SystemRule loadRule = new SystemRule();
        loadRule.setHighestSystemLoad(maxLoad);
        rules.add(loadRule);
        
        SystemRuleManager.loadRules(rules);
        log.info("更新系统负载保护阈值: {}", maxLoad);
    }

    /**
     * 更新CPU使用率保护阈值
     */
    public void updateCpuProtection(double maxCpuUsage) {
        List<SystemRule> rules = new ArrayList<>(SystemRuleManager.getRules());
        
        // 移除旧的CPU规则
        rules.removeIf(rule -> rule.getHighestCpuUsage() > 0);
        
        // 添加新规则
        SystemRule cpuRule = new SystemRule();
        cpuRule.setHighestCpuUsage(maxCpuUsage);
        rules.add(cpuRule);
        
        SystemRuleManager.loadRules(rules);
        log.info("更新CPU使用率保护阈值: {}", maxCpuUsage);
    }

    /**
     * 根据当前系统状态自动调整保护阈值
     */
    public void autoAdjustProtection() {
        SystemStatus status = getSystemStatus();
        
        // 根据CPU使用率调整
        if (status.getCpuUsage() > 0.8) {
            log.warn("CPU使用率过高: {}%, 降低QPS阈值", status.getCpuUsage() * 100);
            updateQpsProtection(getCurrentQpsLimit() * 0.8);
        } else if (status.getCpuUsage() < 0.5) {
            log.info("CPU使用率正常: {}%, 可以提高QPS阈值", status.getCpuUsage() * 100);
            updateQpsProtection(Math.min(getCurrentQpsLimit() * 1.1, 10000));
        }
        
        // 根据内存使用率调整
        if (status.getJvmHeapUsage() > 0.85) {
            log.warn("JVM堆内存使用率过高: {}%, 降低并发阈值", status.getJvmHeapUsage() * 100);
            updateThreadProtection((int) (getCurrentThreadLimit() * 0.8));
        }
    }

    /**
     * 获取当前QPS限制
     */
    public double getCurrentQpsLimit() {
        return SystemRuleManager.getRules().stream()
            .filter(rule -> rule.getQps() > 0)
            .mapToDouble(SystemRule::getQps)
            .findFirst()
            .orElse(5000);
    }

    /**
     * 获取当前线程数限制
     */
    public int getCurrentThreadLimit() {
        return SystemRuleManager.getRules().stream()
            .filter(rule -> rule.getMaxThread() > 0)
            .mapToInt(rule -> (int) rule.getMaxThread())
            .findFirst()
            .orElse(500);
    }

    /**
     * 获取所有系统保护规则
     */
    public List<SystemRule> getAllSystemRules() {
        return SystemRuleManager.getRules();
    }

    /**
     * 检查系统是否健康
     */
    public boolean isSystemHealthy() {
        SystemStatus status = getSystemStatus();
        return status.getCpuUsage() < 0.9 
            && status.getJvmHeapUsage() < 0.9 
            && status.getMemoryUsage() < 0.9;
    }

    /**
     * 系统状态数据类
     */
    @Data
    public static class SystemStatus {
        private double cpuUsage;
        private double memoryUsage;
        private long totalMemory;
        private long availableMemory;
        private long jvmHeapUsed;
        private long jvmHeapMax;
        private double jvmHeapUsage;
        private int activeThreads;
        private double systemLoad;
        private long uptime;
    }
}
