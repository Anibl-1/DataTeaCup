package com.dataplatform.data.service.cache;

/**
 * 告警通知器接口
 * 
 * 定义告警通知的抽象接口，支持多种通知渠道扩展
 */
public interface AlertNotifier {
    
    /**
     * 发送告警通知
     * 
     * @param alert 告警信息
     */
    void notify(CacheAlert alert);
    
    /**
     * 获取通知器名称
     * 
     * @return 通知器名称
     */
    String getName();
    
    /**
     * 检查通知器是否可用
     * 
     * @return 是否可用
     */
    boolean isAvailable();
}
