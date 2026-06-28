package com.dataplatform.data.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.Pipeline;
import com.dataplatform.data.entity.PipelineExecution;
import com.dataplatform.data.entity.PipelineNode;
import com.dataplatform.data.mapper.PipelineMapper;
import com.dataplatform.data.mapper.PipelineNodeMapper;
import com.dataplatform.data.mapper.PipelineExecutionMapper;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.service.DbConnectionUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class PipelineService {
    private static final int MAX_SUB_PROCESS_DEPTH = 10;
    /** 单个源节点读取的最大行数，防止 OOM */
    private static final int MAX_SOURCE_ROWS = 500_000;
    /** 执行日志最大字符数，防止日志 OOM */
    private static final int MAX_LOG_SIZE = 2 * 1024 * 1024; // 2MB
    /** depend() 节点最大检查次数，防止无限轮询 */
    private static final int MAX_DEPEND_CHECKS = 360; // 默认最多轮询360次
    private static final ThreadLocal<Set<Long>> subProcessStack = ThreadLocal.withInitial(HashSet::new);
    private final PipelineMapper pipelineMapper;
    private final PipelineNodeMapper nodeMapper;
    private final PipelineExecutionMapper executionMapper;
    private final ObjectMapper objectMapper;
    private final DbConnectionUtil dbConnectionUtil;
    private final DataSourceMapper dataSourceMapper;
    private final AlertNotificationService alertNotificationService;
    private final Executor taskExecutor;
    private final WecomNotifyService wecomNotifyService;
    private final EmailNotifyService emailNotifyService;
    private final SmsNotifyService smsNotifyService;
    private final DingtalkNotifyService dingtalkNotifyService;
    private final com.dataplatform.data.service.DataSourceConnectionPoolManager connectionPoolManager;
    private final com.dataplatform.data.engine.TransferEngineSelector engineSelector;
    private final LicenseLimitService licenseLimitService;

    public PipelineService(
            PipelineMapper pipelineMapper,
            PipelineNodeMapper nodeMapper,
            PipelineExecutionMapper executionMapper,
            ObjectMapper objectMapper,
            DbConnectionUtil dbConnectionUtil,
            DataSourceMapper dataSourceMapper,
            @Nullable AlertNotificationService alertNotificationService,
            @Qualifier("taskExecutor") Executor taskExecutor,
            @Nullable WecomNotifyService wecomNotifyService,
            @Nullable EmailNotifyService emailNotifyService,
            @Nullable SmsNotifyService smsNotifyService,
            @Nullable DingtalkNotifyService dingtalkNotifyService,
            com.dataplatform.data.service.DataSourceConnectionPoolManager connectionPoolManager,
            com.dataplatform.data.engine.TransferEngineSelector engineSelector,
            LicenseLimitService licenseLimitService) {
        this.pipelineMapper = pipelineMapper;
        this.nodeMapper = nodeMapper;
        this.executionMapper = executionMapper;
        this.objectMapper = objectMapper;
        this.dbConnectionUtil = dbConnectionUtil;
        this.dataSourceMapper = dataSourceMapper;
        this.alertNotificationService = alertNotificationService;
        this.taskExecutor = taskExecutor;
        this.wecomNotifyService = wecomNotifyService;
        this.emailNotifyService = emailNotifyService;
        this.smsNotifyService = smsNotifyService;
        this.dingtalkNotifyService = dingtalkNotifyService;
        this.connectionPoolManager = connectionPoolManager;
        this.engineSelector = engineSelector;
        this.licenseLimitService = licenseLimitService;
    }

    public List<Pipeline> findAll() { return pipelineMapper.findAll(); }
    public Pipeline findById(Long id) { return pipelineMapper.findById(id); }
    public List<Pipeline> search(String kw, Integer type, Integer status, Integer schedule) {
        return pipelineMapper.search(kw, type, status, schedule);
    }

    @Transactional
    public Pipeline create(Pipeline p) {
        licenseLimitService.assertPipelineCreationAllowed(pipelineMapper.countAll());

        if (p.getPipelineCode() == null) p.setPipelineCode("PL" + System.currentTimeMillis());
        if (p.getPipelineStatus() == null) p.setPipelineStatus(0);
        if (p.getScheduleType() == null) p.setScheduleType(0);
        if (p.getVersion() == null) p.setVersion(1);
        if (p.getTimeoutSeconds() == null) p.setTimeoutSeconds(3600);
        if (p.getRetryCount() == null) p.setRetryCount(0);
        if (p.getAlertOnFailure() == null) p.setAlertOnFailure(0);
        pipelineMapper.insert(p);
        return p;
    }

    @Transactional
    public Pipeline update(Pipeline p) {
        pipelineMapper.update(p);
        return pipelineMapper.findById(p.getId());
    }

    @Transactional
    public void delete(Long id) {
        pipelineMapper.deleteById(id);
        nodeMapper.deleteByPipelineId(id);
    }

    public void updateStatus(Long id, Integer s) {
        pipelineMapper.updateStatus(id, s);
    }

    @Transactional
    public Pipeline copy(Long id) {
        licenseLimitService.assertPipelineCreationAllowed(pipelineMapper.countAll());

        Pipeline src = pipelineMapper.findById(id);
        if (src == null) throw new BusinessException(ErrorCode.PIPELINE_NOT_FOUND, "流程不存在");
        Pipeline p = new Pipeline();
        p.setPipelineName(src.getPipelineName() + "_copy");
        p.setPipelineCode("PL" + System.currentTimeMillis());
        p.setPipelineDesc(src.getPipelineDesc());
        p.setPipelineType(src.getPipelineType());
        p.setFlowJson(src.getFlowJson());
        p.setCronExpression(src.getCronExpression());
        p.setScheduleType(src.getScheduleType());
        p.setPipelineStatus(0);
        p.setVersion(1);
        p.setTimeoutSeconds(src.getTimeoutSeconds());
        p.setRetryCount(src.getRetryCount());
        p.setAlertOnFailure(src.getAlertOnFailure());
        p.setCreateBy(src.getCreateBy());
        pipelineMapper.insert(p);
        List<PipelineNode> nodes = nodeMapper.findByPipelineId(id);
        if (nodes != null) {
            for (PipelineNode n : nodes) {
                PipelineNode nn = new PipelineNode();
                nn.setPipelineId(p.getId());
                nn.setNodeCode(n.getNodeCode());
                nn.setNodeName(n.getNodeName());
                nn.setNodeType(n.getNodeType());
                nn.setNodeConfig(n.getNodeConfig());
                nn.setPositionX(n.getPositionX());
                nn.setPositionY(n.getPositionY());
                nn.setSortOrder(n.getSortOrder());
                nn.setIsEnabled(n.getIsEnabled());
                nodeMapper.insert(nn);
            }
        }
        return p;
    }

    public Map<String, Object> getDesign(Long id) {
        Pipeline p = pipelineMapper.findById(id);
        List<PipelineNode> nodes = nodeMapper.findByPipelineId(id);
        // 转换前置任务字符串为数组
        for (PipelineNode n : nodes) {
            if (n.getPreTaskCodesStr() != null && !n.getPreTaskCodesStr().isEmpty()) {
                n.setPreTaskCodes(Arrays.asList(n.getPreTaskCodesStr().split(",")));
            } else {
                n.setPreTaskCodes(new ArrayList<>());
            }
        }
        Map<String, Object> r = new HashMap<>();
        r.put("pipeline", p);
        r.put("nodes", nodes);
        if (p != null && p.getFlowJson() != null) {
            try {
                Map<String, Object> f = objectMapper.readValue(p.getFlowJson(), new TypeReference<Map<String, Object>>() {});
                r.put("edges", f.get("edges"));
            } catch (Exception e) {
                log.error("Parse error", e);
            }
        }
        return r;
    }

    @Transactional
    public void saveDesign(Long id, String json, List<PipelineNode> nodes) {
        Pipeline p = pipelineMapper.findById(id);
        if (p != null) {
            p.setFlowJson(json);
            pipelineMapper.update(p);
        }
        nodeMapper.deleteByPipelineId(id);
        if (nodes != null && !nodes.isEmpty()) {
            for (PipelineNode n : nodes) {
                n.setPipelineId(id);
                // 将前端的数组转换为逗号分隔的字符串
                if (n.getPreTaskCodes() != null && !n.getPreTaskCodes().isEmpty()) {
                    n.setPreTaskCodesStr(String.join(",", n.getPreTaskCodes()));
                }
            }
            nodeMapper.batchInsert(nodes);
        }
    }

    @Transactional
    public PipelineExecution execute(Long id, Long userId) {
        Pipeline p = pipelineMapper.findById(id);
        if (p == null) throw new BusinessException(ErrorCode.PIPELINE_NOT_FOUND, "流程不存在");
        if (p.getPipelineStatus() != 1) throw new BusinessException(ErrorCode.PIPELINE_EXECUTION_FAILED, "流程未发布，无法执行");
        PipelineExecution e = new PipelineExecution();
        e.setPipelineId(id);
        e.setPipelineName(p.getPipelineName());
        e.setExecutionNo("EX" + System.currentTimeMillis());
        e.setTriggerType(1);
        e.setStatus(2);
        e.setStartTime(new Date());
        e.setExecuteBy(userId);
        executionMapper.insert(e);
        runAsync(e, p);
        return e;
    }

    // 更新执行日志（实时）
    private void updateExecutionLog(PipelineExecution ex, String log) {
        ex.setExecuteLog(log);
        executionMapper.updateLog(ex.getId(), log);
    }

    /** 安全追加日志，超过 MAX_LOG_SIZE 后截断 */
    private void safeAppend(StringBuilder sb, String text) {
        if (sb.length() >= MAX_LOG_SIZE) return;
        if (sb.length() + text.length() > MAX_LOG_SIZE) {
            sb.append(text, 0, MAX_LOG_SIZE - sb.length());
            sb.append("\n... 日志过大，已截断 ...\n");
        } else {
            sb.append(text);
        }
    }

    private void runAsync(PipelineExecution ex, Pipeline pl) {
        taskExecutor.execute(() -> {
            StringBuilder logBuilder = new StringBuilder();
            try {
                log.info("Start: {}", pl.getPipelineName());
                logBuilder.append("========== 流程开始 ==========\n");
                logBuilder.append("流程名称: ").append(pl.getPipelineName()).append("\n");
                logBuilder.append("开始时间: ").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
                updateExecutionLog(ex, logBuilder.toString());
                
                // 流程级超时（默认3600秒）
                int pipelineTimeout = pl.getTimeoutSeconds() != null ? pl.getTimeoutSeconds() : 3600;
                long pipelineDeadline = System.currentTimeMillis() + pipelineTimeout * 1000L;
                
                List<PipelineNode> nodes = nodeMapper.findByPipelineId(pl.getId());
                // 转换前置任务字符串为数组
                for (PipelineNode n : nodes) {
                    if (n.getPreTaskCodesStr() != null && !n.getPreTaskCodesStr().isEmpty()) {
                        n.setPreTaskCodes(Arrays.asList(n.getPreTaskCodesStr().split(",")));
                    } else {
                        n.setPreTaskCodes(new ArrayList<>());
                    }
                }

                // 执行前检测循环依赖
                String cycle = detectCycle(nodes);
                if (cycle != null) {
                    throw new BusinessException(ErrorCode.PIPELINE_EXECUTION_FAILED, "流程存在循环依赖: " + cycle);
                }
                
                long ti = 0, to = 0;
                
                // 记录已完成的节点
                Set<String> completedNodes = new HashSet<>();
                Set<String> failedNodes = new HashSet<>();
                Set<String> skippedNodes = new HashSet<>();
                Map<String, List<Map<String, Object>>> nodeOutputs = new HashMap<>();
                
                // 过滤出启用的节点
                List<PipelineNode> enabledNodes = new ArrayList<>();
                for (PipelineNode n : nodes) {
                    if (n.getIsEnabled() != null && n.getIsEnabled() != 1) {
                        logBuilder.append("[跳过] ").append(n.getNodeName()).append(" (已禁用)\n");
                        skippedNodes.add(n.getNodeCode());
                        updateExecutionLog(ex, logBuilder.toString());
                    } else {
                        enabledNodes.add(n);
                    }
                }
                
                // 循环执行，直到所有节点完成或无法继续
                int maxIterations = enabledNodes.size() * 2; // 防止死循环
                int iteration = 0;
                while (completedNodes.size() + failedNodes.size() + skippedNodes.size() < nodes.size() && iteration < maxIterations) {
                    iteration++;
                    boolean progress = false;
                    
                    for (PipelineNode n : enabledNodes) {
                        // 跳过已完成、已失败、已跳过的节点
                        if (completedNodes.contains(n.getNodeCode()) || failedNodes.contains(n.getNodeCode()) || skippedNodes.contains(n.getNodeCode())) {
                            continue;
                        }
                        
                        // 检查前置任务是否完成
                        List<Map<String, Object>> data = new ArrayList<>();
                        if (n.getPreTaskCodes() != null && !n.getPreTaskCodes().isEmpty()) {
                            boolean allPreCompleted = true;
                            boolean anyPreFailed = false;
                            for (String preCode : n.getPreTaskCodes()) {
                                if (failedNodes.contains(preCode)) {
                                    anyPreFailed = true;
                                    break;
                                }
                                if (!completedNodes.contains(preCode) && !skippedNodes.contains(preCode)) {
                                    allPreCompleted = false;
                                    break;
                                }
                            }
                            if (anyPreFailed) {
                                logBuilder.append("[跳过] ").append(n.getNodeName()).append(" (前置任务失败)\n");
                                updateExecutionLog(ex, logBuilder.toString());
                                skippedNodes.add(n.getNodeCode());
                                progress = true;
                                continue;
                            }
                            if (!allPreCompleted) {
                                continue; // 等待前置任务完成
                            }
                            // 合并前置任务的输出数据
                            for (String preCode : n.getPreTaskCodes()) {
                                if (nodeOutputs.containsKey(preCode)) {
                                    data.addAll(nodeOutputs.get(preCode));
                                }
                            }
                        }
                        
                        progress = true;
                        String ts = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                        logBuilder.append("\n[").append(ts).append("] ▶ ").append(n.getNodeName()).append(" (").append(n.getNodeType()).append(")\n");
                        if (n.getDescription() != null && !n.getDescription().isEmpty()) {
                            logBuilder.append("  描述: ").append(n.getDescription()).append("\n");
                        }
                        updateExecutionLog(ex, logBuilder.toString());
                    
                        // 重试逻辑
                        int retryTimes = n.getRetryTimes() != null ? n.getRetryTimes() : 0;
                        int retryInterval = n.getRetryInterval() != null ? n.getRetryInterval() : 1;
                        int attempt = 0;
                        Exception lastError = null;
                        NR r = null;
                    
                        while (attempt <= retryTimes) {
                            try {
                                if (attempt > 0) {
                                    logBuilder.append("  [重试] 第 ").append(attempt).append(" 次重试...\n");
                                    updateExecutionLog(ex, logBuilder.toString());
                                    Thread.sleep(retryInterval * 1000L);
                                }
                            
                                // 超时处理
                                int timeoutSeconds = n.getTimeoutSeconds() != null ? n.getTimeoutSeconds() : 0;
                                boolean timeoutEnabled = n.getTimeoutFlag() != null && n.getTimeoutFlag() == 1;

                                if (timeoutEnabled && timeoutSeconds > 0) {
                                    // 用Future包裹实现真正的超时中断
                                    final List<Map<String, Object>> nodeData = data;
                                    Future<NR> future = CompletableFuture.supplyAsync(() -> {
                                        try {
                                            return run(pl, n, nodeData, logBuilder, failedNodes);
                                        } catch (Exception ex2) {
                                            throw new CompletionException(ex2);
                                        }
                                    }, taskExecutor);
                                    try {
                                        r = future.get(timeoutSeconds, TimeUnit.SECONDS);
                                    } catch (TimeoutException te) {
                                        future.cancel(true);
                                        String timeoutMsg = "节点执行超时: 超过" + timeoutSeconds + "秒限制";
                                        logBuilder.append("  [超时] ").append(timeoutMsg).append("\n");
                                        updateExecutionLog(ex, logBuilder.toString());
                                        if (n.getTimeoutStrategy() != null && n.getTimeoutStrategy() == 1) {
                                            throw new BusinessException(ErrorCode.PIPELINE_EXECUTION_FAILED, timeoutMsg);
                                        }
                                        // 策略非中止时，标记完成并继续
                                        r = null;
                                    } catch (ExecutionException ee) {
                                        throw ee.getCause() != null ? (Exception) ee.getCause() : ee;
                                    }
                                } else {
                                    r = run(pl, n, data, logBuilder, failedNodes);
                                }
                                updateExecutionLog(ex, logBuilder.toString());
                            
                                lastError = null;
                                break;
                            } catch (Exception e) {
                                lastError = e;
                                attempt++;
                                if (attempt > retryTimes) {
                                    logBuilder.append("  [失败] 重试次数已用尽: ").append(e.getMessage()).append("\n");
                                    updateExecutionLog(ex, logBuilder.toString());
                                }
                            }
                        }
                    
                        if (lastError != null) {
                            failedNodes.add(n.getNodeCode());
                            // 检查失败策略
                            if (n.getFailStrategy() != null && n.getFailStrategy() == 1) {
                                logBuilder.append("  [继续] 失败策略: 继续执行后续节点\n");
                                updateExecutionLog(ex, logBuilder.toString());
                                continue;
                            }
                            throw lastError;
                        }
                    
                        if (r != null) {
                            if (r.d != null) {
                                nodeOutputs.put(n.getNodeCode(), r.d);
                            }
                            ti += r.i;
                            to += r.o;
                            String ts2 = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                            logBuilder.append("[").append(ts2).append("] ✓ 完成: 输入=").append(r.i).append(", 输出=").append(r.o).append("\n");
                            updateExecutionLog(ex, logBuilder.toString());
                        }
                        completedNodes.add(n.getNodeCode());
                    }
                    
                    // 如果这轮没有任何进展，说明有循环依赖或配置错误
                    if (!progress) {
                        break;
                    }
                    // 检查流程级超时
                    if (System.currentTimeMillis() > pipelineDeadline) {
                        throw new BusinessException(ErrorCode.PIPELINE_EXECUTION_FAILED,
                            "流程执行超时: 超过" + pipelineTimeout + "秒限制");
                    }
                }
                
                logBuilder.append("\n========== 流程完成 ==========\n");
                logBuilder.append("结束时间: ").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
                logBuilder.append("总输入: ").append(ti).append(", 总输出: ").append(to).append("\n");
                
                ex.setStatus(1);
                ex.setEndTime(new Date());
                ex.setDuration(ex.getEndTime().getTime() - ex.getStartTime().getTime());
                ex.setInputCount(ti);
                ex.setOutputCount(to);
                ex.setExecuteLog(logBuilder.toString());
                executionMapper.update(ex);
                pipelineMapper.updateLastExecute(pl.getId(), 1);
            } catch (Exception e) {
                log.error("Failed", e);
                logBuilder.append("\n========== 流程失败 ==========\n");
                logBuilder.append("错误: ").append(e.getMessage()).append("\n");
                ex.setStatus(0);
                ex.setEndTime(new Date());
                ex.setDuration(ex.getEndTime().getTime() - ex.getStartTime().getTime());
                ex.setErrorMessage(e.getMessage());
                ex.setExecuteLog(logBuilder.toString());
                executionMapper.update(ex);
                pipelineMapper.updateLastExecute(pl.getId(), 0);
                // 发送流程失败告警
                if (alertNotificationService != null) {
                    alertNotificationService.sendTaskFailureAlert(
                            pl.getPipelineName(), "数据流程", e.getMessage());
                }
            }
        });
    }

    private NR run(Pipeline pl, PipelineNode n, List<Map<String, Object>> in, StringBuilder logBuilder, Set<String> failedNodes) throws Exception {
        NR r = new NR();
        r.i = in != null ? in.size() : 0;
        Map<String, Object> c = new HashMap<>();
        if (n.getNodeConfig() != null && !n.getNodeConfig().isEmpty()) {
            c = objectMapper.readValue(n.getNodeConfig(), new TypeReference<Map<String, Object>>() {});
        }
        switch (n.getNodeType()) {
            case "data": return data(c, in, logBuilder);
            case "source": return src(c, logBuilder);
            case "sink": return sink(c, in, logBuilder);
            case "script": return scr(c, in, logBuilder);
            case "shell": return shell(c, logBuilder);
            case "http": return http(c, logBuilder);
            case "condition": return condition(c, in, logBuilder);
            case "error_handler": return errorHandler(c, in, logBuilder);
            case "sub_process": return subProcess(c, in, logBuilder);
            case "wait": return wait(c, in, logBuilder);
            case "depend": return depend(c, in, logBuilder);
            case "wecom": return notifyWecom(c, in, logBuilder, pl, n, failedNodes);
            case "email": return notifyEmail(c, in, logBuilder, pl, n, failedNodes);
            case "sms": return notifySms(c, in, logBuilder, pl, n, failedNodes);
            case "dingtalk": return notifyDingtalk(c, in, logBuilder, pl, n, failedNodes);
            default: r.d = in; r.o = in != null ? in.size() : 0; return r;
        }
    }

    /**
     * 检测节点依赖图中的循环依赖，返回环路描述字符串，无循环时返回null
     */
    private String detectCycle(List<PipelineNode> nodes) {
        Map<String, List<String>> graph = new HashMap<>();
        Set<String> allCodes = new HashSet<>();
        for (PipelineNode n : nodes) {
            allCodes.add(n.getNodeCode());
            graph.put(n.getNodeCode(), n.getPreTaskCodes() != null ? n.getPreTaskCodes() : new ArrayList<>());
        }
        // DFS: 0=unvisited, 1=in-stack, 2=done
        Map<String, Integer> state = new HashMap<>();
        for (String code : allCodes) state.put(code, 0);

        for (String code : allCodes) {
            if (state.get(code) == 0) {
                String cycle = dfsCycle(code, graph, state, new ArrayList<>());
                if (cycle != null) return cycle;
            }
        }
        return null;
    }

    private String dfsCycle(String node, Map<String, List<String>> graph, Map<String, Integer> state, List<String> path) {
        state.put(node, 1);
        path.add(node);
        List<String> deps = graph.getOrDefault(node, Collections.emptyList());
        for (String dep : deps) {
            if (!state.containsKey(dep)) continue; // 引用了不存在的节点，忽略
            if (state.get(dep) == 1) {
                // 找到环路，构造描述
                int idx = path.indexOf(dep);
                List<String> cycle = new ArrayList<>(path.subList(idx, path.size()));
                cycle.add(dep);
                return String.join(" → ", cycle);
            }
            if (state.get(dep) == 0) {
                String result = dfsCycle(dep, graph, state, path);
                if (result != null) return result;
            }
        }
        path.remove(path.size() - 1);
        state.put(node, 2);
        return null;
    }

    // 检查通知发送条件是否满足
    private boolean shouldSendNotification(Map<String, Object> config, Set<String> failedNodes, StringBuilder logBuilder, String channelName) {
        String sendCondition = (String) config.getOrDefault("sendCondition", "always");
        boolean hasFailed = failedNodes != null && !failedNodes.isEmpty();
        switch (sendCondition) {
            case "success":
                if (hasFailed) {
                    logBuilder.append("  [").append(channelName).append("] 发送条件为“仅成功时”，但存在失败节点，跳过\n");
                    return false;
                }
                break;
            case "failure":
                if (!hasFailed) {
                    logBuilder.append("  [").append(channelName).append("] 发送条件为“仅失败时”，暂无失败节点，跳过\n");
                    return false;
                }
                break;
            default: // always
                break;
        }
        return true;
    }

    // 合并后的数据节点处理
    private NR data(Map<String, Object> c, List<Map<String, Object>> in, StringBuilder logBuilder) throws Exception {
        String operation = (String) c.getOrDefault("operation", "read");
        if ("sync".equals(operation)) {
            // 数据同步模式：从源读取，写入目标
            return dataSync(c, logBuilder);
        } else if ("write".equals(operation)) {
            return sink(c, in, logBuilder);
        } else {
            return src(c, logBuilder);
        }
    }

    // 数据同步：委托给 TransferEngine（JDBC/DataX）执行
    private NR dataSync(Map<String, Object> c, StringBuilder logBuilder) throws Exception {
        NR r = new NR();

        // 1. 解析配置
        Long srcDsId = c.get("sourceDataSourceId") != null ? Long.valueOf(c.get("sourceDataSourceId").toString()) : null;
        String srcTable = (String) c.get("sourceTable");
        String readMode = (String) c.get("readMode");
        Long tgtDsId = c.get("targetDataSourceId") != null ? Long.valueOf(c.get("targetDataSourceId").toString()) : null;
        String tgtTable = (String) c.get("targetTable");
        String writeMode = (String) c.getOrDefault("writeMode", "insert");
        String engineType = (String) c.getOrDefault("engineType", "jdbc");
        String dataxHome = (String) c.get("dataxHome");
        Integer batchSize = c.get("batchSize") != null ? Integer.valueOf(c.get("batchSize").toString()) : 1000;
        Integer channelCount = c.get("channelCount") != null ? Integer.valueOf(c.get("channelCount").toString()) : 3;

        if (srcDsId == null) throw new BusinessException(ErrorCode.PIPELINE_NODE_CONFIG_ERROR, "Sync: 源数据源ID不能为空");
        if (tgtDsId == null) throw new BusinessException(ErrorCode.PIPELINE_NODE_CONFIG_ERROR, "Sync: 目标数据源ID不能为空");
        if (tgtTable == null || tgtTable.isEmpty()) throw new BusinessException(ErrorCode.PIPELINE_NODE_CONFIG_ERROR, "Sync: 目标表名不能为空");
        if (srcTable == null || srcTable.isEmpty()) throw new BusinessException(ErrorCode.PIPELINE_NODE_CONFIG_ERROR, "Sync: 源表名或SQL不能为空");

        com.dataplatform.data.entity.DataSource srcDs = dataSourceMapper.selectById(srcDsId);
        com.dataplatform.data.entity.DataSource tgtDs = dataSourceMapper.selectById(tgtDsId);
        if (srcDs == null) throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "源数据源不存在: " + srcDsId);
        if (tgtDs == null) throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "目标数据源不存在: " + tgtDsId);

        // 2. 将 Pipeline 节点配置转为 DataxJob（引擎接口的输入）
        com.dataplatform.data.entity.DataxJob job = new com.dataplatform.data.entity.DataxJob();
        job.setJobName("pipeline-sync-node");
        job.setSourceDataSourceId(srcDsId);
        job.setTargetDataSourceId(tgtDsId);
        job.setTargetTable(tgtTable);
        job.setWriteMode(writeMode);
        job.setBatchSize(batchSize);
        job.setChannelCount(channelCount);
        job.setEngineType(engineType);
        job.setDataxHome(dataxHome);
        job.setIncrementType(0); // Pipeline 节点使用 SQL 或表名模式，非增量
        // readMode=sql: sourceTable 存 SQL；readMode=table: sourceTable 存表名
        if ("sql".equals(readMode)) {
            job.setSourceQuerySql(srcTable);
        } else {
            job.setSourceTable(srcTable);
        }

        logBuilder.append("  [同步] ").append(srcDs.getDatabase()).append(" -> ").append(tgtDs.getDatabase()).append("\n");
        logBuilder.append("  引擎: ").append(engineType).append(", 目标表: ").append(tgtTable).append(", 模式: ").append(writeMode).append("\n");

        // 3. 选择引擎并执行
        com.dataplatform.data.engine.TransferEngine engine = engineSelector.select(engineType);
        com.dataplatform.data.engine.TransferEngine.TransferResult result = engine.execute(
                job, srcDs, tgtDs, null,
                update -> {
                    // 引擎内的进度回调写入日志（避免高频刷屏：只记录 message）
                    Object msg = update.get("message");
                    if (msg != null) logBuilder.append("    [").append(engineType).append("] ").append(msg).append("\n");
                }
        );

        logBuilder.append("  读取 ").append(result.readCount()).append(" 行, 写入 ").append(result.writeCount()).append(" 行, 耗时 ").append(result.durationMs()).append("ms\n");
        if (result.engineLog() != null && !result.engineLog().isEmpty()) {
            logBuilder.append("  引擎日志(尾部):\n").append(result.engineLog()).append("\n");
        }

        if (!result.success()) {
            logBuilder.append("  ❌ 同步失败: ").append(result.errorMessage()).append("\n");
            throw new RuntimeException("数据同步失败: " + result.errorMessage());
        }

        r.i = result.readCount();
        r.o = result.writeCount();
        // Pipeline 下游节点需要传递数据时才保留 d；同步模式不保留全量数据（引擎内部流式处理）
        r.d = new ArrayList<>();
        return r;
    }

    private NR src(Map<String, Object> c, StringBuilder logBuilder) throws Exception {
        NR r = new NR();
        List<Map<String, Object>> d = new ArrayList<>();
        Long dsId = c.get("dataSourceId") != null ? Long.valueOf(c.get("dataSourceId").toString()) : null;
        String sql = (String) c.get("sql");
        String tbl = (String) c.get("tableName");
        String readMode = (String) c.get("readMode");
        if (dsId == null) throw new BusinessException(ErrorCode.PIPELINE_NODE_CONFIG_ERROR, "Source: dataSourceId required");
        com.dataplatform.data.entity.DataSource ds = dataSourceMapper.selectById(dsId);
        if (ds == null) throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在: " + dsId);
        // 根据 readMode 决定如何使用 tableName 字段
        if ("sql".equals(readMode) && tbl != null && !tbl.isEmpty()) {
            sql = tbl; // tableName 字段存储的是 SQL
        } else if (sql == null || sql.isEmpty()) {
            if (tbl != null && !tbl.isEmpty()) sql = "SELECT * FROM " + safeTableName(tbl);
            else throw new BusinessException(ErrorCode.PIPELINE_NODE_CONFIG_ERROR, "Source: sql/table required");
        }
        logBuilder.append("  Source: dsId=").append(dsId).append(", db=").append(ds.getDatabase()).append("\n");
        logBuilder.append("  SQL: ").append(sql).append("\n");
        String url = dbConnectionUtil.buildJdbcUrl(ds);
        try (java.sql.Connection cn = connectionPoolManager.getConnection(ds);
             java.sql.Statement st = cn.createStatement();
             java.sql.ResultSet rs = st.executeQuery(sql)) {
            java.sql.ResultSetMetaData m = rs.getMetaData();
            int cc = m.getColumnCount();
            List<String> colNames = new ArrayList<>();
            for (int i = 1; i <= cc; i++) colNames.add(m.getColumnName(i));
            logBuilder.append("  Columns: ").append(String.join(", ", colNames)).append("\n");
            while (rs.next()) {
                if (d.size() >= MAX_SOURCE_ROWS) {
                    logBuilder.append("  ⚠ 达到最大行数限制(").append(MAX_SOURCE_ROWS).append(")，停止读取\n");
                    break;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cc; i++) row.put(m.getColumnName(i), rs.getObject(i));
                d.add(row);
            }
            logBuilder.append("  Read ").append(d.size()).append(" rows\n");
        } catch (Exception e) {
            logBuilder.append("  Source ERROR: ").append(e.getMessage()).append("\n");
            throw e;
        }
        r.d = d; r.i = 0; r.o = d.size();
        return r;
    }

    private NR sink(Map<String, Object> c, List<Map<String, Object>> in, StringBuilder logBuilder) throws Exception {
        NR r = new NR();
        r.i = in != null ? in.size() : 0;
        if (in == null || in.isEmpty()) {
            logBuilder.append("  Sink: No data to write\n");
            r.o = 0; r.d = new ArrayList<>();
            return r;
        }
        // 兼容 dataSourceId 和 targetDataSourceId
        Object dsIdObj = c.get("dataSourceId");
        if (dsIdObj == null) dsIdObj = c.get("targetDataSourceId");
        Long dsId = dsIdObj != null ? Long.valueOf(dsIdObj.toString()) : null;
        // 兼容 tableName 和 targetTable
        String tbl = (String) c.get("tableName");
        if (tbl == null || tbl.isEmpty()) tbl = (String) c.get("targetTable");
        String mode = (String) c.get("writeMode");
        if (dsId == null) throw new BusinessException(ErrorCode.PIPELINE_NODE_CONFIG_ERROR, "Sink: dataSourceId/targetDataSourceId required");
        if (tbl == null || tbl.isEmpty()) throw new BusinessException(ErrorCode.PIPELINE_NODE_CONFIG_ERROR, "Sink: tableName required");
        com.dataplatform.data.entity.DataSource ds = dataSourceMapper.selectById(dsId);
        if (ds == null) throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在: " + dsId);
        if (mode == null) mode = "insert";
        logBuilder.append("  Sink: dsId=").append(dsId).append(", table=").append(tbl).append(", mode=").append(mode).append(", rows=").append(in.size()).append("\n");
        String url = dbConnectionUtil.buildJdbcUrl(ds);
        int cnt = 0;
        try (java.sql.Connection cn = connectionPoolManager.getConnection(ds)) {
            cn.setAutoCommit(false);
            List<String> cols = new ArrayList<>(in.get(0).keySet());
            logBuilder.append("  Columns: ").append(String.join(", ", cols)).append("\n");
            String safeTbl = safeTableName(tbl);
            String safeCols = cols.stream().map(this::safeColumnName).collect(java.util.stream.Collectors.joining(","));
            String sql = ("replace".equals(mode) ? "REPLACE INTO " : "INSERT INTO ") + safeTbl + " (" +
                safeCols + ") VALUES (" +
                String.join(",", cols.stream().map(x -> "?").toArray(String[]::new)) + ")";
            logBuilder.append("  SQL: ").append(sql).append("\n");
            try (java.sql.PreparedStatement ps = cn.prepareStatement(sql)) {
                for (Map<String, Object> row : in) {
                    for (int i = 0; i < cols.size(); i++) {
                        ps.setObject(i + 1, row.get(cols.get(i)));
                    }
                    ps.addBatch();
                    cnt++;
                    if (cnt % 1000 == 0) {
                        int[] results = ps.executeBatch();
                        cn.commit();
                        logBuilder.append("  Batch: ").append(results.length).append(" rows\n");
                    }
                }
                int[] results = ps.executeBatch();
                cn.commit();
                logBuilder.append("  Final: ").append(results.length).append(" rows, total=").append(cnt).append("\n");
            }
        } catch (Exception e) {
            logBuilder.append("  Sink ERROR: ").append(e.getMessage()).append("\n");
            throw e;
        }
        r.o = cnt; r.d = in;
        return r;
    }

    private NR scr(Map<String, Object> c, List<Map<String, Object>> in, StringBuilder logBuilder) throws Exception {
        NR r = new NR();
        r.i = in != null ? in.size() : 0;
        Long dsId = c.get("dataSourceId") != null ? Long.valueOf(c.get("dataSourceId").toString()) : null;
        // 兼容 script 和 scriptContent
        String script = (String) c.get("script");
        if (script == null || script.isEmpty()) script = (String) c.get("scriptContent");
        if (script == null || script.isEmpty()) {
            r.d = in; r.o = in != null ? in.size() : 0;
            return r;
        }
        if (dsId == null) throw new BusinessException(ErrorCode.PIPELINE_NODE_CONFIG_ERROR, "Script: dataSourceId required");
        com.dataplatform.data.entity.DataSource ds = dataSourceMapper.selectById(dsId);
        if (ds == null) throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在: " + dsId);
        logBuilder.append("  Script: ").append(script).append("\n");
        String url = dbConnectionUtil.buildJdbcUrl(ds);
        List<Map<String, Object>> out = new ArrayList<>();
        try (java.sql.Connection cn = connectionPoolManager.getConnection(ds);
             java.sql.Statement st = cn.createStatement()) {
            if (st.execute(script)) {
                try (java.sql.ResultSet rs = st.getResultSet()) {
                    java.sql.ResultSetMetaData m = rs.getMetaData();
                    int cc = m.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= cc; i++) row.put(m.getColumnName(i), rs.getObject(i));
                        out.add(row);
                    }
                }
                r.d = out; r.o = out.size();
            } else {
                int uc = st.getUpdateCount();
                r.d = in; r.o = uc >= 0 ? uc : 0;
            }
        }
        return r;
    }

    private static class NR {
        List<Map<String, Object>> d;
        long i, o;
    }

    // 校验并转义表名：仅允许字母、数字、下划线、点号，用反引号包裹
    private String safeTableName(String tableName) {
        if (tableName == null || !tableName.matches("^[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)?$")) {
            throw new BusinessException(ErrorCode.PIPELINE_NODE_CONFIG_ERROR, "不安全的表名: " + tableName);
        }
        return "`" + tableName.replace(".", "`.`") + "`";
    }

    // 校验并转义列名：仅允许字母、数字、下划线
    private String safeColumnName(String columnName) {
        if (columnName == null || !columnName.matches("^[a-zA-Z0-9_]+$")) {
            throw new BusinessException(ErrorCode.PIPELINE_NODE_CONFIG_ERROR, "不安全的列名: " + columnName);
        }
        return "`" + columnName + "`";
    }

    // Shell脚本危险命令黑名单
    private static final List<String> DANGEROUS_COMMANDS = Arrays.asList(
        "rm -rf", "rm -fr", "mkfs", "dd if=", "format ", "del /f", "del /s",
        "curl ", "wget ", "nc ", "ncat ", "telnet ", "ssh ", "scp ",
        "chmod 777", "chown ", "passwd ", "useradd ", "userdel ",
        "shutdown", "reboot", "halt", "poweroff", "init 0", "init 6",
        "> /dev/", ">/dev/", "| bash", "|bash", "| sh", "|sh",
        "eval ", "exec ", "python -c", "perl -e", "ruby -e",
        "base64 -d", "base64 --decode"
    );
    private static final int MAX_SCRIPT_LENGTH = 4096;
    private static final int SHELL_TIMEOUT_SECONDS = 300;

    // Shell脚本执行
    private NR shell(Map<String, Object> c, StringBuilder logBuilder) throws Exception {
        NR r = new NR();
        String script = (String) c.get("script");
        if (script == null || script.isEmpty()) {
            logBuilder.append("  Shell: 脚本内容为空\n");
            r.o = 0;
            return r;
        }
        // 安全校验：脚本长度限制
        if (script.length() > MAX_SCRIPT_LENGTH) {
            throw new BusinessException(ErrorCode.PIPELINE_NODE_CONFIG_ERROR, 
                "Shell脚本长度超出限制(" + MAX_SCRIPT_LENGTH + "字符)");
        }
        // 安全校验：危险命令黑名单
        String scriptLower = script.toLowerCase();
        for (String dangerous : DANGEROUS_COMMANDS) {
            if (scriptLower.contains(dangerous)) {
                log.warn("Shell脚本包含危险命令被拒绝: {}", dangerous);
                throw new BusinessException(ErrorCode.PIPELINE_NODE_CONFIG_ERROR, 
                    "Shell脚本包含不允许的危险命令: " + dangerous.trim());
            }
        }
        logBuilder.append("  Shell: 执行脚本...\n");
        Process p = null;
        try {
            ProcessBuilder pb;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", script);
            } else {
                pb = new ProcessBuilder("sh", "-c", script);
            }
            pb.redirectErrorStream(true);
            p = pb.start();
            StringBuilder output = new StringBuilder();
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append("\n");
                    // 限制输出大小防止OOM
                    if (output.length() > 1024 * 1024) {
                        output.append("\n... 输出过大，已截断 ...\n");
                        break;
                    }
                }
            }
            boolean finished = p.waitFor(SHELL_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new BusinessException(ErrorCode.PIPELINE_EXECUTION_FAILED, 
                    "Shell执行超时(" + SHELL_TIMEOUT_SECONDS + "秒)，已强制终止");
            }
            int exitCode = p.exitValue();
            logBuilder.append("  输出: ").append(output.toString().trim()).append("\n");
            logBuilder.append("  退出码: ").append(exitCode).append("\n");
            if (exitCode != 0) {
                throw new BusinessException(ErrorCode.PIPELINE_EXECUTION_FAILED, "Shell执行失败，退出码: " + exitCode);
            }
            r.o = 1;
        } catch (Exception e) {
            logBuilder.append("  Shell ERROR: ").append(e.getMessage()).append("\n");
            throw e;
        } finally {
            if (p != null && p.isAlive()) {
                p.destroyForcibly();
            }
        }
        return r;
    }

    // HTTP请求执行
    private NR http(Map<String, Object> c, StringBuilder logBuilder) throws Exception {
        NR r = new NR();
        String method = (String) c.getOrDefault("httpMethod", "GET");
        String url = (String) c.get("url");
        String headers = (String) c.get("headers");
        String body = (String) c.get("body");
        Integer timeout = c.get("connectTimeout") != null ? Integer.valueOf(c.get("connectTimeout").toString()) : 30;
        
        if (url == null || url.isEmpty()) {
            throw new BusinessException(ErrorCode.PIPELINE_NODE_CONFIG_ERROR, "HTTP: URL不能为空");
        }
        logBuilder.append("  HTTP: ").append(method).append(" ").append(url).append("\n");
        
        try {
            java.net.URL urlObj = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(timeout * 1000);
            conn.setReadTimeout(timeout * 1000);
            
            // 设置请求头
            if (headers != null && !headers.isEmpty()) {
                Map<String, String> headerMap = objectMapper.readValue(headers, new TypeReference<Map<String, String>>() {});
                for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            
            // 发送请求体
            if (body != null && !body.isEmpty() && ("POST".equals(method) || "PUT".equals(method))) {
                conn.setDoOutput(true);
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes("UTF-8"));
                }
            }
            
            int responseCode = conn.getResponseCode();
            logBuilder.append("  响应码: ").append(responseCode).append("\n");
            
            // 读取响应
            StringBuilder response = new StringBuilder();
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            String respStr = response.toString();
            if (respStr.length() > 500) {
                logBuilder.append("  响应: ").append(respStr.substring(0, 500)).append("...\n");
            } else {
                logBuilder.append("  响应: ").append(respStr).append("\n");
            }
            
            if (responseCode >= 400) {
                throw new BusinessException(ErrorCode.PIPELINE_EXECUTION_FAILED, "HTTP请求失败: " + responseCode);
            }
            
            // 尝试解析JSON响应
            try {
                List<Map<String, Object>> data = new ArrayList<>();
                if (respStr.startsWith("[")) {
                    data = objectMapper.readValue(respStr, new TypeReference<List<Map<String, Object>>>() {});
                } else if (respStr.startsWith("{")) {
                    Map<String, Object> obj = objectMapper.readValue(respStr, new TypeReference<Map<String, Object>>() {});
                    data.add(obj);
                }
                r.d = data;
                r.o = data.size();
            } catch (Exception e) {
                r.o = 1;
            }
        } catch (Exception e) {
            logBuilder.append("  HTTP ERROR: ").append(e.getMessage()).append("\n");
            throw e;
        }
        return r;
    }

    // 条件分支（增强版）
    private NR condition(Map<String, Object> c, List<Map<String, Object>> in, StringBuilder logBuilder) throws Exception {
        NR r = new NR();
        r.i = in != null ? in.size() : 0;
        String conditionType = (String) c.getOrDefault("conditionType", "expression");
        String expression = (String) c.get("conditionExpression");
        String trueBranch = (String) c.get("trueBranch");   // TRUE分支节点Code
        String falseBranch = (String) c.get("falseBranch"); // FALSE分支节点Code
        
        logBuilder.append("  条件分支: 类型=").append(conditionType).append("\n");
        logBuilder.append("  表达式: ").append(expression).append("\n");
        
        boolean result = evaluateCondition(expression, in, c);
        
        logBuilder.append("  结果: ").append(result ? "TRUE" : "FALSE").append("\n");
        if (trueBranch != null || falseBranch != null) {
            logBuilder.append("  分支路由: ").append(result ? trueBranch : falseBranch).append("\n");
        }
        
        // 如果条件举FALSE且有FALSE分支，返回空数据（跳过后续默认节点）
        if (!result && "skip".equals(c.get("falseAction"))) {
            r.d = new ArrayList<>();
            r.o = 0;
            logBuilder.append("  FALSE分支操作: 跳过后续节点\n");
        } else {
            r.d = in;
            r.o = in != null ? in.size() : 0;
        }
        return r;
    }

    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(String expression, List<Map<String, Object>> in, Map<String, Object> config) {
        if (expression == null || expression.isEmpty()) return true;
        
        // 数据为空判断
        if (expression.contains("isEmpty")) {
            return in == null || in.isEmpty();
        }
        if (expression.contains("notEmpty")) {
            return in != null && !in.isEmpty();
        }
        
        // 数据行数比较
        int dataSize = in != null ? in.size() : 0;
        if (expression.contains(">=")) {
            try {
                String[] parts = expression.split(">=");
                int right = Integer.parseInt(parts[1].trim());
                return dataSize >= right;
            } catch (Exception e) { return true; }
        }
        if (expression.contains("<=")) {
            try {
                String[] parts = expression.split("<=");
                int right = Integer.parseInt(parts[1].trim());
                return dataSize <= right;
            } catch (Exception e) { return true; }
        }
        if (expression.contains(">")) {
            try {
                String[] parts = expression.split(">");
                int right = Integer.parseInt(parts[1].trim());
                return dataSize > right;
            } catch (Exception e) { return true; }
        }
        if (expression.contains("<")) {
            try {
                String[] parts = expression.split("<");
                int right = Integer.parseInt(parts[1].trim());
                return dataSize < right;
            } catch (Exception e) { return true; }
        }
        if (expression.contains("==")) {
            try {
                String[] parts = expression.split("==");
                int right = Integer.parseInt(parts[1].trim());
                return dataSize == right;
            } catch (Exception e) { return true; }
        }
        
        // 字段值判断：fieldName=value
        if (expression.contains("=") && !expression.contains("==") && in != null && !in.isEmpty()) {
            try {
                String[] parts = expression.split("=");
                String fieldName = parts[0].trim();
                String expectedValue = parts[1].trim();
                Object actual = in.get(0).get(fieldName);
                return actual != null && actual.toString().equals(expectedValue);
            } catch (Exception e) { return true; }
        }
        
        return true;
    }

    /**
     * 错误处理节点 - 捕获前置任务的异常并执行補救操作
     */
    private NR errorHandler(Map<String, Object> c, List<Map<String, Object>> in, StringBuilder logBuilder) throws Exception {
        NR r = new NR();
        r.i = in != null ? in.size() : 0;
        
        String errorAction = (String) c.getOrDefault("errorAction", "log");
        String errorMessage = (String) c.get("errorMessage");
        String fallbackSql = (String) c.get("fallbackSql");
        
        logBuilder.append("  错误处理节点: 动作=").append(errorAction).append("\n");
        
        switch (errorAction) {
            case "log":
                // 仅记录日志，继续执行
                logBuilder.append("  [日志] ").append(errorMessage != null ? errorMessage : "错误处理节点已触发").append("\n");
                r.d = in;
                r.o = in != null ? in.size() : 0;
                break;
                
            case "retry":
                // 重试逻辑（通过节点层面的 retryTimes 配置处理）
                logBuilder.append("  [重试] 通过节点重试机制处理\n");
                r.d = in;
                r.o = in != null ? in.size() : 0;
                break;
                
            case "fallback":
                // 执行降级SQL
                if (fallbackSql != null && !fallbackSql.isEmpty()) {
                    Long dsId = c.get("dataSourceId") != null ? Long.valueOf(c.get("dataSourceId").toString()) : null;
                    if (dsId != null) {
                        logBuilder.append("  [降级] 执行降级SQL: ").append(fallbackSql).append("\n");
                        com.dataplatform.data.entity.DataSource ds = dataSourceMapper.selectById(dsId);
                        if (ds != null) {
                            String url = dbConnectionUtil.buildJdbcUrl(ds);
                            List<Map<String, Object>> fallbackData = new ArrayList<>();
                            try (java.sql.Connection cn = connectionPoolManager.getConnection(ds);
                                 java.sql.Statement st = cn.createStatement();
                                 java.sql.ResultSet rs = st.executeQuery(fallbackSql)) {
                                java.sql.ResultSetMetaData m = rs.getMetaData();
                                int cc = m.getColumnCount();
                                while (rs.next()) {
                                    Map<String, Object> row = new LinkedHashMap<>();
                                    for (int i = 1; i <= cc; i++) row.put(m.getColumnName(i), rs.getObject(i));
                                    fallbackData.add(row);
                                }
                            }
                            r.d = fallbackData;
                            r.o = fallbackData.size();
                            logBuilder.append("  [降级] 获取 ").append(fallbackData.size()).append(" 行降级数据\n");
                            break;
                        }
                    }
                }
                logBuilder.append("  [降级] 无降级配置，透传数据\n");
                r.d = in;
                r.o = in != null ? in.size() : 0;
                break;
                
            case "skip":
                // 跳过，返回空数据
                logBuilder.append("  [跳过] 清空数据继续执行\n");
                r.d = new ArrayList<>();
                r.o = 0;
                break;
                
            case "abort":
                // 中止流程
                logBuilder.append("  [中止] 流程将被中止\n");
                throw new BusinessException(ErrorCode.PIPELINE_EXECUTION_FAILED, "错误处理节点触发中止: " + (errorMessage != null ? errorMessage : "流程异常"));
                
            default:
                r.d = in;
                r.o = in != null ? in.size() : 0;
        }
        
        return r;
    }

    // 子流程执行
    private NR subProcess(Map<String, Object> c, List<Map<String, Object>> in, StringBuilder logBuilder) throws Exception {
        NR r = new NR();
        r.i = in != null ? in.size() : 0;
        Long subProcessId = c.get("subProcessId") != null ? Long.valueOf(c.get("subProcessId").toString()) : null;
        
        if (subProcessId == null) {
            throw new BusinessException(ErrorCode.PIPELINE_NODE_CONFIG_ERROR, "子流程: 未指定子流程ID");
        }
        
        // 循环检测和深度限制
        Set<Long> visited = subProcessStack.get();
        if (visited.contains(subProcessId)) {
            throw new BusinessException(ErrorCode.PIPELINE_EXECUTION_FAILED, "子流程循环引用: ID=" + subProcessId);
        }
        if (visited.size() >= MAX_SUB_PROCESS_DEPTH) {
            throw new BusinessException(ErrorCode.PIPELINE_EXECUTION_FAILED, "子流程嵌套深度超过限制(" + MAX_SUB_PROCESS_DEPTH + ")" );
        }
        
        Pipeline subPipeline = pipelineMapper.findById(subProcessId);
        if (subPipeline == null) {
            throw new BusinessException(ErrorCode.PIPELINE_NOT_FOUND, "子流程不存在: " + subProcessId);
        }
        
        logBuilder.append("  子流程: ").append(subPipeline.getPipelineName()).append(" (ID=").append(subProcessId).append(", 深度=").append(visited.size() + 1).append(")\n");
        
        visited.add(subProcessId);
        try {
            // 执行子流程
            List<PipelineNode> subNodes = nodeMapper.findByPipelineId(subProcessId);
            subNodes.sort((a, b) -> (a.getSortOrder() != null ? a.getSortOrder() : 0) - (b.getSortOrder() != null ? b.getSortOrder() : 0));
            
            List<Map<String, Object>> data = in != null ? new ArrayList<>(in) : new ArrayList<>();
            long totalOut = 0;
            
            for (PipelineNode n : subNodes) {
                if (n.getIsEnabled() != null && n.getIsEnabled() != 1) continue;
                logBuilder.append("    [子] ").append(n.getNodeName()).append("\n");
                NR nr = run(subPipeline, n, data, logBuilder, new HashSet<>());
                if (nr.d != null) data = nr.d;
                totalOut += nr.o;
            }
            
            logBuilder.append("  子流程完成，输出: ").append(totalOut).append("\n");
            r.d = data;
            r.o = totalOut;
            return r;
        } finally {
            visited.remove(subProcessId);
            if (visited.isEmpty()) {
                subProcessStack.remove();
            }
        }
    }

    // 等待节点
    private NR wait(Map<String, Object> c, List<Map<String, Object>> in, StringBuilder logBuilder) throws Exception {
        NR r = new NR();
        r.i = in != null ? in.size() : 0;
        
        int waitSeconds = c.get("waitSeconds") != null ? Integer.valueOf(c.get("waitSeconds").toString()) : 1;
        if (waitSeconds < 1) waitSeconds = 1;
        if (waitSeconds > 86400) waitSeconds = 86400; // 最长24小时
        
        logBuilder.append("  等待: ").append(waitSeconds).append(" 秒\n");
        
        Thread.sleep(waitSeconds * 1000L);
        
        logBuilder.append("  等待完成\n");
        r.d = in;
        r.o = in != null ? in.size() : 0;
        return r;
    }

    // 前置任务节点 - 等待其他流程执行完成（参考DolphinScheduler的Dependent Task）
    @SuppressWarnings("unchecked")
    private NR depend(Map<String, Object> c, List<Map<String, Object>> in, StringBuilder logBuilder) throws Exception {
        NR r = new NR();
        r.i = in != null ? in.size() : 0;
        
        List<Object> dependPipelineIdsRaw = (List<Object>) c.get("dependPipelineIds");
        List<Long> dependPipelineIds = new ArrayList<>();
        if (dependPipelineIdsRaw != null) {
            for (Object id : dependPipelineIdsRaw) {
                dependPipelineIds.add(Long.valueOf(id.toString()));
            }
        }
        
        String dependRelation = (String) c.getOrDefault("dependRelation", "AND");
        String checkCycle = (String) c.getOrDefault("checkCycle", "day");
        int checkInterval = c.get("checkInterval") != null ? Integer.valueOf(c.get("checkInterval").toString()) : 10;
        int dependTimeout = c.get("dependTimeout") != null ? Integer.valueOf(c.get("dependTimeout").toString()) : 0;
        String timeoutStrategy = (String) c.getOrDefault("timeoutStrategy", "FAIL");
        
        if (dependPipelineIds.isEmpty()) {
            logBuilder.append("  [前置任务] 无依赖流程配置，直接通过\n");
            r.d = in;
            r.o = in != null ? in.size() : 0;
            return r;
        }
        
        // 获取依赖流程名称
        List<String> pipelineNames = new ArrayList<>();
        for (Long pid : dependPipelineIds) {
            Pipeline p = pipelineMapper.findById(pid);
            pipelineNames.add(p != null ? p.getPipelineName() : "ID:" + pid);
        }
        
        logBuilder.append("  [前置任务] 开始检查依赖流程\n");
        logBuilder.append("    依赖流程: ").append(String.join(", ", pipelineNames)).append("\n");
        logBuilder.append("    依赖关系: ").append(dependRelation).append("\n");
        logBuilder.append("    检查周期: ").append(checkCycle).append("\n");
        logBuilder.append("    检查间隔: ").append(checkInterval).append("秒\n");
        if (dependTimeout > 0) {
            logBuilder.append("    超时时间: ").append(dependTimeout).append("秒\n");
        }
        
        long startTime = System.currentTimeMillis();
        boolean allSuccess = false;
        int checkCount = 0;
        
        while (!allSuccess) {
            checkCount++;
            if (checkCount > MAX_DEPEND_CHECKS) {
                throw new BusinessException(ErrorCode.PIPELINE_EXECUTION_FAILED,
                    "依赖检查超过最大次数限制(" + MAX_DEPEND_CHECKS + ")");
            }
            logBuilder.append("    第 ").append(checkCount).append(" 次检查...\n");
            
            int successCount = 0;
            int failCount = 0;
            
            for (Long pid : dependPipelineIds) {
                // 根据检查周期查询执行记录
                PipelineExecution lastExec = getLastExecutionByCycle(pid, checkCycle);
                
                if (lastExec == null) {
                    logBuilder.append("      流程[").append(pipelineNames.get(dependPipelineIds.indexOf(pid)))
                              .append("] 在").append(getCycleDesc(checkCycle)).append("内无执行记录\n");
                    failCount++;
                } else if (lastExec.getStatus() == 1) {
                    logBuilder.append("      流程[").append(pipelineNames.get(dependPipelineIds.indexOf(pid)))
                              .append("] 执行成功 ✓\n");
                    successCount++;
                } else if (lastExec.getStatus() == 0) {
                    logBuilder.append("      流程[").append(pipelineNames.get(dependPipelineIds.indexOf(pid)))
                              .append("] 执行失败 ✗\n");
                    failCount++;
                } else {
                    logBuilder.append("      流程[").append(pipelineNames.get(dependPipelineIds.indexOf(pid)))
                              .append("] 正在执行中...\n");
                }
            }
            
            // 判断是否满足依赖条件
            if ("AND".equals(dependRelation)) {
                allSuccess = (successCount == dependPipelineIds.size());
            } else { // OR
                allSuccess = (successCount > 0);
            }
            
            if (allSuccess) {
                logBuilder.append("  [前置任务] 依赖检查通过 ✓\n");
                break;
            }
            
            // 检查超时
            if (dependTimeout > 0) {
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                if (elapsed >= dependTimeout) {
                    String msg = "依赖检查超时: " + elapsed + "秒";
                    logBuilder.append("  [前置任务] ").append(msg).append("\n");
                    if ("FAIL".equals(timeoutStrategy)) {
                        throw new BusinessException(ErrorCode.PIPELINE_EXECUTION_FAILED, msg);
                    } else {
                        logBuilder.append("  [前置任务] 超时策略为告警，继续等待...\n");
                    }
                }
            }
            
            // 等待下次检查
            logBuilder.append("    等待 ").append(checkInterval).append(" 秒后重新检查...\n");
            Thread.sleep(checkInterval * 1000L);
        }
        
        r.d = in;
        r.o = in != null ? in.size() : 0;
        return r;
    }
    
    // 根据检查周期获取最近一次执行记录
    private PipelineExecution getLastExecutionByCycle(Long pipelineId, String checkCycle) {
        java.time.LocalDate today = java.time.LocalDate.now();
        String startDate = null;
        String endDate = today.plusDays(1).toString();
        
        switch (checkCycle) {
            case "day":
                startDate = today.toString();
                break;
            case "week":
                startDate = today.minusDays(today.getDayOfWeek().getValue() - 1).toString();
                break;
            case "month":
                startDate = today.withDayOfMonth(1).toString();
                break;
            case "last":
            default:
                // 不限制时间，获取最近一次
                startDate = null;
                endDate = null;
                break;
        }
        
        List<PipelineExecution> executions = executionMapper.search(pipelineId, null, null, startDate, endDate);
        return executions != null && !executions.isEmpty() ? executions.get(0) : null;
    }
    
    // 获取周期描述
    private String getCycleDesc(String checkCycle) {
        switch (checkCycle) {
            case "day": return "当天";
            case "week": return "本周";
            case "month": return "本月";
            case "last": return "历史";
            default: return checkCycle;
        }
    }

    public void stopExecution(Long id) { executionMapper.updateStatus(id, 3); }
    public List<PipelineExecution> getExecutions(Long pid, Integer s, Integer t, String sd, String ed) {
        return executionMapper.search(pid, s, t, sd, ed);
    }
    public PipelineExecution getExecution(Long id) { return executionMapper.findById(id); }
    public List<PipelineExecution> getRunningExecutions() { return executionMapper.findRunning(); }

    // 通用通知节点：企微/邮件/短信/钉钉共用逻辑
    private NR notifyGeneric(String channelName, NotifySender sender,
            Map<String, Object> c, List<Map<String, Object>> in, StringBuilder logBuilder,
            Pipeline pl, PipelineNode n, Set<String> failedNodes) throws Exception {
        NR r = new NR();
        r.i = in != null ? in.size() : 0;
        if (!shouldSendNotification(c, failedNodes, logBuilder, channelName)) {
            r.d = in; r.o = in != null ? in.size() : 0; return r;
        }
        logBuilder.append("  [").append(channelName).append("] 发送中...\n");
        try {
            if (sender != null) {
                sender.send(c, buildNotifyContext(pl, n));
                logBuilder.append("  [").append(channelName).append("] 发送成功 ✓\n");
            } else {
                logBuilder.append("  [").append(channelName).append("] 服务未配置，跳过\n");
            }
        } catch (Exception e) {
            logBuilder.append("  [").append(channelName).append("] 发送失败: ").append(e.getMessage()).append("\n");
            throw e;
        }
        r.d = in;
        r.o = in != null ? in.size() : 0;
        return r;
    }

    // 通知发送器函数式接口
    @FunctionalInterface
    private interface NotifySender {
        void send(Map<String, Object> config, Map<String, String> context);
    }

    // 通知节点快捷方法
    private NR notifyWecom(Map<String, Object> c, List<Map<String, Object>> in, StringBuilder logBuilder, Pipeline pl, PipelineNode n, Set<String> failedNodes) throws Exception {
        return notifyGeneric("企微通知", wecomNotifyService != null ? wecomNotifyService::send : null, c, in, logBuilder, pl, n, failedNodes);
    }
    private NR notifyEmail(Map<String, Object> c, List<Map<String, Object>> in, StringBuilder logBuilder, Pipeline pl, PipelineNode n, Set<String> failedNodes) throws Exception {
        return notifyGeneric("邮件通知", emailNotifyService != null ? emailNotifyService::send : null, c, in, logBuilder, pl, n, failedNodes);
    }
    private NR notifySms(Map<String, Object> c, List<Map<String, Object>> in, StringBuilder logBuilder, Pipeline pl, PipelineNode n, Set<String> failedNodes) throws Exception {
        return notifyGeneric("短信通知", smsNotifyService != null ? smsNotifyService::send : null, c, in, logBuilder, pl, n, failedNodes);
    }
    private NR notifyDingtalk(Map<String, Object> c, List<Map<String, Object>> in, StringBuilder logBuilder, Pipeline pl, PipelineNode n, Set<String> failedNodes) throws Exception {
        return notifyGeneric("钉钉通知", dingtalkNotifyService != null ? dingtalkNotifyService::send : null, c, in, logBuilder, pl, n, failedNodes);
    }

    // 构建通知上下文变量
    private Map<String, String> buildNotifyContext(Pipeline pl, PipelineNode n) {
        Map<String, String> ctx = new HashMap<>();
        ctx.put("timestamp", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        ctx.put("pipeline.name", pl != null ? pl.getPipelineName() : "");
        ctx.put("pipeline.code", pl != null && pl.getPipelineCode() != null ? pl.getPipelineCode() : "");
        ctx.put("node.name", n != null ? n.getNodeName() : "");
        ctx.put("node.type", n != null ? n.getNodeType() : "");
        ctx.put("node.status", "running");
        return ctx;
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> s = new HashMap<>();
        s.put("totalPipelines", pipelineMapper.countAll());
        s.put("publishedPipelines", pipelineMapper.countByStatus(1));
        s.put("totalExecutions", executionMapper.countAll());
        s.put("todayExecutions", executionMapper.countToday());
        s.put("successExecutions", executionMapper.countByStatus(1));
        s.put("failedExecutions", executionMapper.countByStatus(0));
        s.put("runningExecutions", executionMapper.countByStatus(2));
        return s;
    }

    public List<Map<String, Object>> getExecutionTrend(int days) {
        List<Map<String, Object>> dbData = executionMapper.countDailyTrend(days);
        // Build a map keyed by date string for quick lookup
        Map<String, Map<String, Object>> dataMap = new LinkedHashMap<>();
        for (Map<String, Object> row : dbData) {
            String day = String.valueOf(row.get("day"));
            if (day.length() > 10) day = day.substring(0, 10);
            dataMap.put(day, row);
        }
        // Fill in missing days with zero counts
        List<Map<String, Object>> result = new ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = days - 1; i >= 0; i--) {
            String dayStr = today.minusDays(i).format(fmt);
            Map<String, Object> entry = new HashMap<>();
            entry.put("day", dayStr);
            if (dataMap.containsKey(dayStr)) {
                Map<String, Object> row = dataMap.get(dayStr);
                entry.put("successCount", row.getOrDefault("success_count", 0));
                entry.put("failedCount", row.getOrDefault("failed_count", 0));
            } else {
                entry.put("successCount", 0);
                entry.put("failedCount", 0);
            }
            result.add(entry);
        }
        return result;
    }
}
