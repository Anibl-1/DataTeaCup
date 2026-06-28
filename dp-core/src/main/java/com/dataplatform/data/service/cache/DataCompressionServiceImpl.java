package com.dataplatform.data.service.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 数据压缩服务实现
 * 
 * 提供通用的 GZIP 压缩和解压功能，支持：
 * - 字节数组压缩/解压
 * - 数据列表压缩/解压（自动 JSON 序列化）
 * - 字符串压缩/解压
 * - 自动检测数据大小，超过阈值时压缩
 * - 压缩率统计
 * 
 * 需求引用：
 * - 需求 9.3: THE Cache_Manager SHALL 对超过 1MB 的缓存数据使用 GZIP 压缩存储
 * - 需求 9.5: WHEN 缓存数据超过配置的大小限制时，THE Cache_Manager SHALL 自动压缩或分片存储
 * 
 * @see DataCompressionService 接口定义
 */
@Slf4j
@Service
public class DataCompressionServiceImpl implements DataCompressionService {
    
    /**
     * GZIP 缓冲区大小
     */
    private static final int BUFFER_SIZE = 8192;
    
    /**
     * JSON 序列化器
     */
    private final ObjectMapper objectMapper;
    
    /**
     * 压缩阈值（字节）
     */
    private volatile long compressionThreshold;
    
    // 统计计数器
    private final AtomicLong compressionCount = new AtomicLong(0);
    private final AtomicLong decompressionCount = new AtomicLong(0);
    private final AtomicLong totalOriginalBytes = new AtomicLong(0);
    private final AtomicLong totalCompressedBytes = new AtomicLong(0);
    
    @Autowired
    public DataCompressionServiceImpl(
            ObjectMapper objectMapper,
            @Value("${cache.compression.threshold:1048576}") long compressionThreshold) {
        this.objectMapper = objectMapper;
        this.compressionThreshold = compressionThreshold;
        log.info("数据压缩服务初始化完成，压缩阈值: {} bytes ({} MB)", 
                compressionThreshold, compressionThreshold / (1024.0 * 1024.0));
    }
    
    @Override
    public byte[] compress(byte[] data) {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
                gzipOut.write(data);
            }
            
            byte[] compressed = baos.toByteArray();
            
            // 更新统计
            compressionCount.incrementAndGet();
            totalOriginalBytes.addAndGet(data.length);
            totalCompressedBytes.addAndGet(compressed.length);
            
            double ratio = (double) compressed.length / data.length;
            log.debug("数据压缩完成 - 原始大小: {} bytes, 压缩后: {} bytes, 压缩率: {:.2f}%", 
                    data.length, compressed.length, (1 - ratio) * 100);
            
            return compressed;
            
        } catch (IOException e) {
            log.error("数据压缩失败: {}", e.getMessage(), e);
            throw new CompressionException("数据压缩失败", e);
        }
    }
    
    @Override
    public byte[] decompress(byte[] compressed) {
        if (compressed == null || compressed.length == 0) {
            return new byte[0];
        }
        
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            try (GZIPInputStream gzipIn = new GZIPInputStream(bais)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int len;
                while ((len = gzipIn.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
            }
            
            byte[] decompressed = baos.toByteArray();
            
            // 更新统计
            decompressionCount.incrementAndGet();
            
            log.debug("数据解压完成 - 压缩大小: {} bytes, 解压后: {} bytes", 
                    compressed.length, decompressed.length);
            
            return decompressed;
            
        } catch (IOException e) {
            log.error("数据解压失败: {}", e.getMessage(), e);
            throw new CompressionException("数据解压失败", e);
        }
    }
    
    @Override
    public byte[] compressData(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return new byte[0];
        }
        
        try {
            String json = objectMapper.writeValueAsString(data);
            byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
            return compress(jsonBytes);
            
        } catch (JsonProcessingException e) {
            log.error("数据序列化失败: {}", e.getMessage(), e);
            throw new CompressionException("数据序列化失败", e);
        }
    }
    
    @Override
    public List<Map<String, Object>> decompressData(byte[] compressed) {
        if (compressed == null || compressed.length == 0) {
            return Collections.emptyList();
        }
        
        try {
            byte[] decompressed = decompress(compressed);
            String json = new String(decompressed, StandardCharsets.UTF_8);
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            
        } catch (IOException e) {
            log.error("数据反序列化失败: {}", e.getMessage(), e);
            throw new CompressionException("数据反序列化失败", e);
        }
    }
    
    @Override
    public byte[] compressString(String text) {
        if (text == null || text.isEmpty()) {
            return new byte[0];
        }
        
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        return compress(textBytes);
    }
    
    @Override
    public String decompressString(byte[] compressed) {
        if (compressed == null || compressed.length == 0) {
            return "";
        }
        
        byte[] decompressed = decompress(compressed);
        return new String(decompressed, StandardCharsets.UTF_8);
    }
    
    @Override
    public long estimateSize(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return 0;
        }
        
        try {
            String json = objectMapper.writeValueAsString(data);
            return json.getBytes(StandardCharsets.UTF_8).length;
        } catch (JsonProcessingException e) {
            log.warn("估算数据大小失败: {}", e.getMessage());
            // 粗略估算：每条记录约 500 字节
            return data.size() * 500L;
        }
    }
    
    @Override
    public boolean shouldCompress(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        
        long estimatedSize = estimateSize(data);
        return estimatedSize > compressionThreshold;
    }
    
    @Override
    public boolean shouldCompress(byte[] data) {
        if (data == null) {
            return false;
        }
        return data.length > compressionThreshold;
    }
    
    @Override
    public long getCompressionThreshold() {
        return compressionThreshold;
    }
    
    @Override
    public void setCompressionThreshold(long threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("压缩阈值不能为负数");
        }
        this.compressionThreshold = threshold;
        log.info("压缩阈值已更新为: {} bytes ({} MB)", 
                threshold, threshold / (1024.0 * 1024.0));
    }
    
    @Override
    public CompressionStats getStats() {
        return new CompressionStatsImpl(
                compressionCount.get(),
                decompressionCount.get(),
                totalOriginalBytes.get(),
                totalCompressedBytes.get()
        );
    }
    
    @Override
    public void resetStats() {
        compressionCount.set(0);
        decompressionCount.set(0);
        totalOriginalBytes.set(0);
        totalCompressedBytes.set(0);
        log.info("压缩统计信息已重置");
    }
    
    /**
     * 压缩统计信息实现
     */
    private static class CompressionStatsImpl implements CompressionStats {
        private final long compressionCount;
        private final long decompressionCount;
        private final long totalOriginalBytes;
        private final long totalCompressedBytes;
        
        public CompressionStatsImpl(long compressionCount, long decompressionCount,
                                    long totalOriginalBytes, long totalCompressedBytes) {
            this.compressionCount = compressionCount;
            this.decompressionCount = decompressionCount;
            this.totalOriginalBytes = totalOriginalBytes;
            this.totalCompressedBytes = totalCompressedBytes;
        }
        
        @Override
        public long getCompressionCount() {
            return compressionCount;
        }
        
        @Override
        public long getDecompressionCount() {
            return decompressionCount;
        }
        
        @Override
        public long getTotalOriginalBytes() {
            return totalOriginalBytes;
        }
        
        @Override
        public long getTotalCompressedBytes() {
            return totalCompressedBytes;
        }
        
        @Override
        public double getAverageCompressionRatio() {
            if (totalOriginalBytes == 0) {
                return 0.0;
            }
            return (double) totalCompressedBytes / totalOriginalBytes;
        }
        
        @Override
        public long getTotalBytesSaved() {
            return totalOriginalBytes - totalCompressedBytes;
        }
        
        @Override
        public String toString() {
            return String.format(
                    "CompressionStats{compressionCount=%d, decompressionCount=%d, " +
                    "totalOriginalBytes=%d, totalCompressedBytes=%d, " +
                    "averageCompressionRatio=%.2f%%, totalBytesSaved=%d}",
                    compressionCount, decompressionCount,
                    totalOriginalBytes, totalCompressedBytes,
                    getAverageCompressionRatio() * 100, getTotalBytesSaved()
            );
        }
    }
}
