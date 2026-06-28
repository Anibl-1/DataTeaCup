package com.dataplatform.data.service.alert;

import java.util.List;

/**
 * 告警渠道接口
 * 需求: 15.1
 */
public interface AlertChannel {

    /**
     * 发送告警
     *
     * @param title     告警标题
     * @param content   告警内容
     * @param level     告警级别: info/warning/critical/emergency
     * @param receivers 接收人列表
     */
    void send(String title, String content, String level, List<String> receivers);

    /**
     * 获取渠道类型
     */
    String getChannelType();

    /**
     * 渠道是否可用
     */
    boolean isAvailable();
}
