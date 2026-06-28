package com.dataplatform.data.service.cache;

import java.util.List;
import java.util.Map;

/**
 * 数据压缩服务接口
 * 
 * 提供通用的数据压缩和解压功能，支持 GZIP 压缩算法。
 * 该服务可被多个组件复用，如分页缓存、导出服务等。
 * 
 * 功能特性：
 * - GZIP 压缩和解压
 * - 自动检测数据大小，超过阈值时压缩
 * - 支持配置压缩阈值
 * - 压缩率统计
 * 
 * 需求引用：
 * - 需求 9.3: THE Cache_Manager SHALL 对超过 1MB 的缓存数据使用 GZIP 压缩存储
 * - 需求 9.5: WHEN 缓存数据超过配置的大小限制时，THE Cache_Manager SHALL 自动压缩或分片存储
 * 
 * @see DataCompressionServiceImpl 默认实现
 */
public interface DataCompressionService {
    
    /**
     * 默认压缩阈值（1MB）
     */
    long DEFAULT_COMPRESSION_THRESHOLD_BYTES = 1024 * 1024;
    
    /**
     * 压缩数据
     * 
     * 使用 GZIP 算法压缩字节数组。
     * 
     * @param data 要压缩的原始数据
     * @return 压缩后的字节数组
     * @throws CompressionException 压缩失败时抛出
     */
    byte[] compress(byte[] data);
    
    /**
     * 解压数据
     * 
     * 解压 GZIP 压缩的数据。
     * 
     * @param compressed 压缩的字节数组
     * @return 解压后的原始数据
     * @throws CompressionException 解压失败时抛出
     */
    byte[] decompress(byte[] compressed);
    
    /**
     * 压缩数据列表
     * 
     * 将数据列表序列化为 JSON 后进行 GZIP 压缩。
     * 
     * @param data 要压缩的数据列表
     * @return 压缩后的字节数组
     * @throws CompressionException 压缩失败时抛出
     */
    byte[] compressData(List<Map<String, Object>> data);
    
    /**
     * 解压数据列表
     * 
     * 解压 GZIP 压缩的数据并反序列化为数据列表。
     * 
     * @param compressed 压缩的字节数组
     * @return 解压后的数据列表
     * @throws CompressionException 解压失败时抛出
     */
    List<Map<String, Object>> decompressData(byte[] compressed);
    
    /**
     * 压缩字符串
     * 
     * 将字符串进行 GZIP 压缩。
     * 
     * @param text 要压缩的字符串
     * @return 压缩后的字节数组
     * @throws CompressionException 压缩失败时抛出
     */
    byte[] compressString(String text);
    
    /**
     * 解压字符串
     * 
     * 解压 GZIP 压缩的数据为字符串。
     * 
     * @param compressed 压缩的字节数组
     * @return 解压后的字符串
     * @throws CompressionException 解压失败时抛出
     */
    String decompressString(byte[] compressed);
    
    /**
     * 估算数据大小（字节）
     * 
     * @param data 数据列表
     * @return 估算的字节大小
     */
    long estimateSize(List<Map<String, Object>> data);
    
    /**
     * 判断数据是否需要压缩
     * 
     * 根据配置的压缩阈值判断数据是否需要压缩。
     * 
     * @param data 数据列表
     * @return 如果数据大小超过压缩阈值返回 true
     */
    boolean shouldCompress(List<Map<String, Object>> data);
    
    /**
     * 判断字节数组是否需要压缩
     * 
     * @param data 字节数组
     * @return 如果数据大小超过压缩阈值返回 true
     */
    boolean shouldCompress(byte[] data);
    
    /**
     * 获取压缩阈值（字节）
     * 
     * @return 当前配置的压缩阈值
     */
    long getCompressionThreshold();
    
    /**
     * 设置压缩阈值（字节）
     * 
     * @param threshold 新的压缩阈值
     */
    void setCompressionThreshold(long threshold);
    
    /**
     * 获取压缩统计信息
     * 
     * @return 压缩统计信息
     */
    CompressionStats getStats();
    
    /**
     * 重置压缩统计信息
     */
    void resetStats();
    
    /**
     * 压缩统计信息
     */
    interface CompressionStats {
        /**
         * 获取压缩操作次数
         */
        long getCompressionCount();
        
        /**
         * 获取解压操作次数
         */
        long getDecompressionCount();
        
        /**
         * 获取压缩前总字节数
         */
        long getTotalOriginalBytes();
        
        /**
         * 获取压缩后总字节数
         */
        long getTotalCompressedBytes();
        
        /**
         * 获取平均压缩率（0-1，越小压缩效果越好）
         */
        double getAverageCompressionRatio();
        
        /**
         * 获取总节省字节数
         */
        long getTotalBytesSaved();
    }
    
    /**
     * 压缩异常
     */
    class CompressionException extends RuntimeException {
        public CompressionException(String message) {
            super(message);
        }
        
        public CompressionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
