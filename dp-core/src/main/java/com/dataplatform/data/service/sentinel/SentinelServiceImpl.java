package com.dataplatform.data.service.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.node.ClusterNode;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * Sentinel服务实现
 */
@Slf4j
@Service
public class SentinelServiceImpl implements SentinelService {

    @Override
    public <T> T executeWithFlowControl(String resource, Supplier<T> supplier, Supplier<T> fallback) {
        Entry entry = null;
        try {
            entry = SphU.entry(resource);
            return supplier.get();
        } catch (BlockException e) {
            log.warn("资源被限流: resource={}", resource);
            return fallback != null ? fallback.get() : null;
        } catch (Exception e) {
            Tracer.traceEntry(e, entry);
            throw e;
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    @Override
    public <T> T executeWithCircuitBreaker(String resource, Supplier<T> supplier, Supplier<T> fallback) {
        Entry entry = null;
        try {
            entry = SphU.entry(resource);
            return supplier.get();
        } catch (BlockException e) {
            log.warn("资源被熔断: resource={}", resource);
            return fallback != null ? fallback.get() : null;
        } catch (Exception e) {
            Tracer.traceEntry(e, entry);
            throw e;
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    @Override
    public <T> T executeWithParamFlowControl(String resource, Object param, Supplier<T> supplier, Supplier<T> fallback) {
        Entry entry = null;
        try {
            entry = SphU.entry(resource, com.alibaba.csp.sentinel.EntryType.IN, 1, param);
            return supplier.get();
        } catch (BlockException e) {
            log.warn("热点参数被限流: resource={}, param={}", resource, param);
            return fallback != null ? fallback.get() : null;
        } catch (Exception e) {
            Tracer.traceEntry(e, entry);
            throw e;
        } finally {
            if (entry != null) {
                entry.exit(1, param);
            }
        }
    }

    @Override
    public boolean isResourceAvailable(String resource) {
        Entry entry = null;
        try {
            entry = SphU.entry(resource);
            return true;
        } catch (BlockException e) {
            return false;
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    @Override
    public double getCurrentQps(String resource) {
        ClusterNode clusterNode = ClusterBuilderSlot.getClusterNode(resource);
        if (clusterNode != null) {
            return clusterNode.passQps();
        }
        return 0;
    }

    @Override
    public int getCurrentConcurrency(String resource) {
        ClusterNode clusterNode = ClusterBuilderSlot.getClusterNode(resource);
        if (clusterNode != null) {
            return clusterNode.curThreadNum();
        }
        return 0;
    }

    @Override
    public boolean isCircuitBreakerOpen(String resource) {
        ClusterNode clusterNode = ClusterBuilderSlot.getClusterNode(resource);
        if (clusterNode != null) {
            // 通过错误率判断熔断器状态
            double errorRatio = clusterNode.exceptionQps() / Math.max(clusterNode.totalQps(), 1);
            return errorRatio > 0.5;
        }
        return false;
    }
}
