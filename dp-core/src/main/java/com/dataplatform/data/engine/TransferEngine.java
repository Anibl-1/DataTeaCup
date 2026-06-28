package com.dataplatform.data.engine;

import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.entity.DataxJob;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 数据传输引擎抽象接口
 * 策略模式: 通过 engine_type 字段选择 JDBC 或 DataX 引擎
 */
public interface TransferEngine {

    /**
     * 引擎类型标识
     */
    String getType();

    /**
     * 执行数据传输
     *
     * @param job              任务配置
     * @param sourceDs         源数据源
     * @param targetDs         目标数据源
     * @param parameters       运行时参数
     * @param progressCallback 进度回调（可选），传入 Map 包含 readCount, writeCount, percent, message
     * @return 执行结果
     */
    TransferResult execute(DataxJob job, DataSource sourceDs, DataSource targetDs,
                           Map<String, Object> parameters, Consumer<Map<String, Object>> progressCallback);

    /**
     * 执行结果
     */
    record TransferResult(
            boolean success,
            long readCount,
            long writeCount,
            long durationMs,
            String errorMessage,
            String engineLog
    ) {
        public static TransferResult ok(long readCount, long writeCount, long durationMs) {
            return new TransferResult(true, readCount, writeCount, durationMs, null, null);
        }

        public static TransferResult ok(long readCount, long writeCount, long durationMs, String engineLog) {
            return new TransferResult(true, readCount, writeCount, durationMs, null, engineLog);
        }

        public static TransferResult fail(String errorMessage) {
            return new TransferResult(false, 0, 0, 0, errorMessage, null);
        }

        public static TransferResult fail(String errorMessage, long readCount, long writeCount, long durationMs) {
            return new TransferResult(false, readCount, writeCount, durationMs, errorMessage, null);
        }
    }
}
