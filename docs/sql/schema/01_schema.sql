
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `data_platform` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `data_platform`;
DROP TABLE IF EXISTS `ai_chat_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL COMMENT '会话ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `role` varchar(20) NOT NULL COMMENT '消息角色：user/assistant/system',
  `content` text COMMENT '消息内容',
  `data_source_id` bigint DEFAULT NULL COMMENT '关联数据源ID',
  `message_type` varchar(20) DEFAULT 'text' COMMENT '消息类型：text/sql/report/etl/diagnosis',
  `metadata` text COMMENT '额外数据JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话历史';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_data_quality`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_data_quality` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `data_source_id` bigint NOT NULL COMMENT '数据源ID',
  `table_name` varchar(128) NOT NULL COMMENT '表名',
  `column_name` varchar(128) DEFAULT NULL COMMENT '列名',
  `quality_type` varchar(50) NOT NULL COMMENT '质量类型：null_rate/duplicate/outlier/format',
  `quality_score` decimal(5,2) DEFAULT NULL COMMENT '质量分数0-100',
  `issue_count` int DEFAULT '0' COMMENT '问题数量',
  `sample_data` text COMMENT '样本数据JSON',
  `suggestion` text COMMENT 'AI建议',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ds_table` (`data_source_id`,`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI数据质量分析';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `bigscreen_project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bigscreen_project` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_name` varchar(100) NOT NULL COMMENT 'Project name',
  `project_code` varchar(100) NOT NULL COMMENT 'Project code',
  `description` varchar(500) DEFAULT NULL COMMENT 'Description',
  `cover_image` varchar(500) DEFAULT NULL COMMENT 'Cover image URL',
  `default_config` text COMMENT 'Default bigscreen config JSON',
  `status` tinyint DEFAULT '1' COMMENT 'Status: 1 enabled, 0 disabled',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bigscreen_project_code` (`project_code`),
  KEY `idx_bigscreen_project_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Bigscreen projects';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `cache_hotspot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cache_hotspot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cache_key` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '缓存键',
  `access_count` int DEFAULT '0' COMMENT '访问次数',
  `last_access_time` datetime DEFAULT NULL COMMENT '最后访问时间',
  `is_warmed` tinyint DEFAULT '0' COMMENT '是否已预热：0-否，1-是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cache_key` (`cache_key`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='缓存热点表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `cache_stats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cache_stats` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stat_time` datetime NOT NULL COMMENT '统计时间',
  `cache_level` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '缓存级别：L1/L2',
  `hit_count` bigint DEFAULT '0' COMMENT '命中次数',
  `miss_count` bigint DEFAULT '0' COMMENT '未命中次数',
  `hit_rate` decimal(5,4) DEFAULT '0.0000' COMMENT '命中率（0.0000-1.0000）',
  `memory_usage` bigint DEFAULT '0' COMMENT '内存占用字节',
  `entry_count` int DEFAULT '0' COMMENT '条目数',
  PRIMARY KEY (`id`),
  KEY `idx_time_level` (`stat_time`,`cache_level`),
  KEY `idx_cache_level` (`cache_level`),
  KEY `idx_stat_time` (`stat_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='缓存统计表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `chart_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chart_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `chart_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图表名称',
  `chart_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图表编码',
  `chart_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图表类型',
  `data_source_id` bigint NOT NULL COMMENT '数据源ID',
  `sql_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SQL语句',
  `chart_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '图表配置',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
  `status` int DEFAULT '1' COMMENT '状态',
  `mobile_enabled` tinyint DEFAULT '0' COMMENT '?????????1-?????-???',
  `allow_export_excel` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'allow Excel export',
  `allow_export_pdf` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'allow PDF export',
  `watermark_type` varchar(20) DEFAULT 'none' COMMENT 'watermark type',
  `pdf_watermark` varchar(100) DEFAULT NULL COMMENT 'PDF watermark',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `chart_code` (`chart_code`) USING BTREE,
  KEY `data_source_id` (`data_source_id`) USING BTREE,
  KEY `idx_chart_code` (`chart_code`) USING BTREE,
  CONSTRAINT `chart_definition_ibfk_1` FOREIGN KEY (`data_source_id`) REFERENCES `data_source` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='图表定义表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `chat_conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(20) NOT NULL COMMENT '会话类型: private-私聊 group-群组',
  `name` varchar(100) DEFAULT NULL COMMENT '会话名称（群组必填）',
  `avatar` varchar(500) DEFAULT NULL COMMENT '群组头像 URL',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `last_message_time` datetime DEFAULT NULL COMMENT '最后消息时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `chat_conversation_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_conversation_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_id` bigint NOT NULL COMMENT '会话 ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `unread_count` int DEFAULT '0' COMMENT '未读消息数',
  `join_time` datetime NOT NULL COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_user` (`conversation_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话成员表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_id` bigint NOT NULL COMMENT '会话 ID',
  `sender_id` bigint NOT NULL COMMENT '发送者 ID',
  `content_type` varchar(20) NOT NULL COMMENT '消息类型: text-文本 image-图片 file-文件',
  `content` text COMMENT '消息内容',
  `file_url` varchar(500) DEFAULT NULL COMMENT '文件 URL',
  `file_name` varchar(200) DEFAULT NULL COMMENT '文件名',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `send_time` datetime NOT NULL COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_send_time` (`conversation_id`,`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `chat_user_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_user_status` (
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `status` varchar(20) NOT NULL COMMENT '状态: online-在线 offline-离线',
  `last_active_time` datetime DEFAULT NULL COMMENT '最后活跃时间',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户在线状态表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `collect_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `collect_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `task_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '任务名称',
  `source_table` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '源表名',
  `target_table` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '目标表名',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'running' COMMENT '状态: running, success, failed',
  `row_count` int DEFAULT '0' COMMENT '采集行数',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` bigint DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '错误信息',
  `execute_sql` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '执行的SQL',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_task_id` (`task_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_start_time` (`start_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='采集日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `collect_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `collect_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `data_source_id` bigint NOT NULL COMMENT '源数据源ID',
  `data_source_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '源数据源名称',
  `target_data_source_id` bigint DEFAULT NULL COMMENT '目标数据源ID',
  `target_data_source_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '目标数据源名称',
  `table_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '源表名',
  `target_table_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '目标表名',
  `collect_mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'full' COMMENT '采集模式',
  `custom_sql` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '自定义SQL',
  `incremental_field` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '增量字段名',
  `incremental_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '增量字段类型',
  `last_collect_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '上次采集值',
  `field_mapping` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '字段映射',
  `transform_rules` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '转换规则',
  `batch_size` int DEFAULT '1000' COMMENT '批量大小',
  `auto_create_table` tinyint(1) DEFAULT '1' COMMENT '自动建表',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'stopped' COMMENT '状态',
  `last_execute_time` datetime DEFAULT NULL COMMENT '上次执行时间',
  `last_execute_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '执行结果',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `schedule_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用定时任务',
  `cron_expression` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Cron表达式',
  `schedule_description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '定时任务描述',
  `next_execute_time` datetime DEFAULT NULL COMMENT '下次执行时间',
  `execute_count` int DEFAULT '0' COMMENT '执行次数',
  `success_count` int DEFAULT '0' COMMENT '成功次数',
  `fail_count` int DEFAULT '0' COMMENT '失败次数',
  `max_retry_count` int DEFAULT '3' COMMENT '最大重试次数（0表示不重试）',
  `retry_interval` int DEFAULT '30' COMMENT '重试间隔（秒）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `data_source_id` (`data_source_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_schedule` (`schedule_enabled`,`status`) USING BTREE,
  CONSTRAINT `collect_task_ibfk_1` FOREIGN KEY (`data_source_id`) REFERENCES `data_source` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='采集任务表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dashboard_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dashboard_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(200) NOT NULL COMMENT '仪表盘名称',
  `description` text COMMENT '描述',
  `layout_json` longtext COMMENT '布局配置JSON',
  `global_filters_json` text COMMENT '全局筛选器配置JSON',
  `link_config_json` text COMMENT '图表联动配置JSON',
  `template_id` bigint DEFAULT NULL COMMENT '模板ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint DEFAULT '1' COMMENT '状态: 1-启用 0-禁用',
  PRIMARY KEY (`id`),
  KEY `idx_create_by` (`create_by`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仪表盘配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dashboard_share`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dashboard_share` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dashboard_id` bigint NOT NULL COMMENT '仪表盘ID',
  `share_token` varchar(64) NOT NULL COMMENT '分享token',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间（null表示永不过期）',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `status` tinyint DEFAULT '1' COMMENT '状态: 1-有效 0-已撤销',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_share_token` (`share_token`),
  KEY `idx_dashboard_id` (`dashboard_id`),
  KEY `idx_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仪表盘分享表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dashboard_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dashboard_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(200) NOT NULL COMMENT '模板名称',
  `category` varchar(50) DEFAULT NULL COMMENT '分类（销售分析/运营监控/财务概览）',
  `layout_json` longtext COMMENT '模板布局JSON',
  `thumbnail` varchar(500) DEFAULT NULL COMMENT '缩略图URL',
  `description` text COMMENT '描述',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仪表盘模板表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `data_asset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_asset` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_type` varchar(50) NOT NULL COMMENT '资产类型：database/table/column/report/etl',
  `asset_name` varchar(128) NOT NULL COMMENT '资产名称',
  `asset_code` varchar(128) DEFAULT NULL COMMENT '资产编码',
  `parent_id` bigint DEFAULT '0' COMMENT '父级ID',
  `data_source_id` bigint DEFAULT NULL COMMENT '关联数据源ID',
  `database_name` varchar(128) DEFAULT NULL COMMENT '数据库名',
  `table_name` varchar(128) DEFAULT NULL COMMENT '表名',
  `column_name` varchar(128) DEFAULT NULL COMMENT '字段名',
  `data_type` varchar(50) DEFAULT NULL COMMENT '数据类型',
  `description` text COMMENT '描述',
  `owner` varchar(64) DEFAULT NULL COMMENT '负责人',
  `tags` varchar(512) DEFAULT NULL COMMENT '标签JSON',
  `metadata` text COMMENT '元数据JSON',
  `status` int DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_asset_type` (`asset_type`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_data_source` (`data_source_id`),
  KEY `idx_table_name` (`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据资产目录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `data_dictionary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_dictionary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典类型',
  `dict_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典编码',
  `dict_label` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典标签',
  `dict_value` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典值',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `is_default` tinyint(1) DEFAULT '0' COMMENT '是否默认',
  `status` int DEFAULT '1' COMMENT '状态',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_dict_type_code` (`dict_type`,`dict_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='数据字典表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `data_lineage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_lineage` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '源类型: table/view/report/chart',
  `source_id` bigint DEFAULT NULL COMMENT '源对象ID',
  `source_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '源对象名称',
  `source_database` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '源数据库',
  `source_table` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '源表名',
  `source_column` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '源字段名',
  `target_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '目标类型: table/view/report/chart',
  `target_id` bigint DEFAULT NULL COMMENT '目标对象ID',
  `target_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '目标对象名称',
  `target_database` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '目标数据库',
  `target_table` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '目标表名',
  `target_column` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '目标字段名',
  `lineage_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '血缘类型: direct/indirect',
  `transform_logic` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '转换逻辑',
  `sql_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'SQL内容',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_source` (`source_type`,`source_id`) USING BTREE,
  KEY `idx_target` (`target_type`,`target_id`) USING BTREE,
  KEY `idx_source_table` (`source_database`,`source_table`) USING BTREE,
  KEY `idx_target_table` (`target_database`,`target_table`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='数据血缘关系表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `data_pipeline`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_pipeline` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pipeline_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程名称',
  `pipeline_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程编码',
  `pipeline_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程描述',
  `pipeline_type` int DEFAULT '1' COMMENT '流程类型: 1-ETL流程, 2-数据清洗, 3-数据同步, 4-数据聚合',
  `flow_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '流程图JSON配置',
  `cron_expression` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Cron表达式(定时执行)',
  `schedule_type` int DEFAULT '0' COMMENT '调度类型: 0-手动, 1-定时, 2-事件触发',
  `pipeline_status` int DEFAULT '0' COMMENT '流程状态: 0-草稿, 1-已发布, 2-已停用',
  `version` int DEFAULT '1' COMMENT '版本号',
  `timeout_seconds` int DEFAULT '3600' COMMENT '超时时间(秒)',
  `retry_count` int DEFAULT '0' COMMENT '失败重试次数',
  `alert_on_failure` int DEFAULT '0' COMMENT '失败告警: 0-否, 1-是',
  `last_execute_time` datetime DEFAULT NULL COMMENT '最后执行时间',
  `last_execute_status` int DEFAULT NULL COMMENT '最后执行状态',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `del_flag` int DEFAULT '0' COMMENT '删除标志: 0-正常, 1-删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_pipeline_status` (`pipeline_status`) USING BTREE,
  KEY `idx_schedule_type` (`schedule_type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='数据处理流程定义表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `data_quality_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_quality_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_id` bigint DEFAULT NULL COMMENT '规则ID',
  `data_source_id` bigint NOT NULL COMMENT '数据源ID',
  `table_name` varchar(200) NOT NULL COMMENT '表名',
  `score` int DEFAULT NULL COMMENT '质量评分（0-100）',
  `detail_json` longtext COMMENT '检测详情JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
  PRIMARY KEY (`id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_data_source_id` (`data_source_id`),
  KEY `idx_ds_table` (`data_source_id`,`table_name`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据质量报告表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `data_quality_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_quality_result` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `data_source_id` bigint DEFAULT NULL COMMENT '数据源ID',
  `table_name` varchar(128) DEFAULT NULL COMMENT '表名',
  `column_name` varchar(128) DEFAULT NULL COMMENT '字段名',
  `check_time` datetime DEFAULT NULL COMMENT '检查时间',
  `total_count` bigint DEFAULT NULL COMMENT '总记录数',
  `pass_count` bigint DEFAULT NULL COMMENT '通过数',
  `fail_count` bigint DEFAULT NULL COMMENT '失败数',
  `pass_rate` decimal(5,2) DEFAULT NULL COMMENT '通过率',
  `is_pass` tinyint DEFAULT NULL COMMENT '是否通过',
  `error_sample` text COMMENT '错误样本JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_check_time` (`check_time`),
  KEY `idx_table_name` (`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据质量检查结果';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `data_quality_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_quality_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_name` varchar(128) NOT NULL COMMENT '规则名称',
  `rule_code` varchar(64) DEFAULT NULL COMMENT '规则编码',
  `rule_type` varchar(50) NOT NULL COMMENT '规则类型：completeness/accuracy/consistency/timeliness/uniqueness',
  `data_source_id` bigint DEFAULT NULL COMMENT '数据源ID',
  `table_name` varchar(128) DEFAULT NULL COMMENT '表名',
  `column_name` varchar(128) DEFAULT NULL COMMENT '字段名',
  `check_sql` text COMMENT '检查SQL',
  `threshold` decimal(5,2) DEFAULT NULL COMMENT '阈值',
  `severity` varchar(20) DEFAULT 'medium' COMMENT '严重级别：low/medium/high',
  `description` text COMMENT '描述',
  `status` int DEFAULT '1' COMMENT '状态',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `rule_code` (`rule_code`),
  KEY `idx_rule_type` (`rule_type`),
  KEY `idx_table_name` (`table_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据质量规则';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `data_source`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_source` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据源名称',
  `db_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据库类型',
  `host` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主机地址',
  `port` int NOT NULL COMMENT '端口',
  `database` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据库名',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `group_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '数据源分组',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='数据源表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `data_standard`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_standard` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `standard_code` varchar(64) NOT NULL COMMENT '标准编码',
  `standard_name` varchar(128) NOT NULL COMMENT '标准名称',
  `category` varchar(64) DEFAULT NULL COMMENT '分类',
  `data_type` varchar(50) DEFAULT NULL COMMENT '数据类型',
  `length` int DEFAULT NULL COMMENT '长度',
  `precision_num` int DEFAULT NULL COMMENT '精度',
  `value_range` varchar(512) DEFAULT NULL COMMENT '取值范围',
  `default_value` varchar(256) DEFAULT NULL COMMENT '默认值',
  `format_pattern` varchar(256) DEFAULT NULL COMMENT '格式模式',
  `description` text COMMENT '描述',
  `status` int DEFAULT '1' COMMENT '状态',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `standard_code` (`standard_code`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据标准';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `datax_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `datax_job` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `job_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `job_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '任务描述',
  `job_type` int DEFAULT '1' COMMENT '任务类型: 1-数据库同步, 2-文件传输',
  `source_data_source_id` bigint DEFAULT NULL COMMENT '源数据源ID',
  `source_table` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '源表名',
  `source_query_sql` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '源查询SQL',
  `target_data_source_id` bigint DEFAULT NULL COMMENT '目标数据源ID',
  `target_table` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '目标表名',
  `write_mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'insert' COMMENT '写入模式: insert, update, replace',
  `column_mapping` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '字段映射配置(JSON)',
  `cron_expression` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Cron表达式',
  `job_status` int DEFAULT '0' COMMENT '任务状态: 0-停止, 1-运行中',
  `increment_type` int DEFAULT '0' COMMENT '增量类型: 0-全量, 1-增量',
  `increment_column` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '增量字段',
  `increment_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '增量值',
  `channel_count` int DEFAULT '3' COMMENT '并发通道数',
  `last_execute_time` datetime DEFAULT NULL COMMENT '最后执行时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `del_flag` int DEFAULT '0' COMMENT '删除标志: 0-正常, 1-删除',
  `parameter_definition` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '参数定义（JSON格式）',
  `default_parameters` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '默认参数值（JSON格式）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_job_status` (`job_status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  KEY `idx_source_ds` (`source_data_source_id`) USING BTREE,
  KEY `idx_target_ds` (`target_data_source_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='DataX数据传输任务表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `datax_job_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `datax_job_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `job_id` bigint NOT NULL COMMENT '任务ID',
  `job_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '任务名称',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `status` int DEFAULT '2' COMMENT '状态: 0-失败, 1-成功, 2-运行中',
  `trigger_type` int DEFAULT '1' COMMENT '触发方式: 1-手动, 2-定时',
  `read_count` bigint DEFAULT '0' COMMENT '读取行数',
  `write_count` bigint DEFAULT '0' COMMENT '写入行数',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '错误信息',
  `duration` bigint DEFAULT NULL COMMENT '执行时长(毫秒)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `execute_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '执行时传入的参数（JSON格式）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_job_id` (`job_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_start_time` (`start_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='DataX任务执行日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `export_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `export_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_name` varchar(200) NOT NULL COMMENT '任务名称',
  `task_type` varchar(50) NOT NULL DEFAULT 'report' COMMENT '任务类型: report-报表导出, table-表数据导出',
  `ref_id` bigint DEFAULT NULL COMMENT '关联ID（报表ID等）',
  `ref_code` varchar(100) DEFAULT NULL COMMENT '关联编码',
  `filters` text COMMENT '筛选条件JSON',
  `params` text COMMENT '自定义参数(JSON)',
  `status` int NOT NULL DEFAULT '0' COMMENT '状态: 0-等待中, 1-处理中, 2-已完成, 3-失败, 4-已取消, 5-已暂停',
  `progress` int NOT NULL DEFAULT '0' COMMENT '进度百分比 0-100',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径',
  `file_name` varchar(200) DEFAULT NULL COMMENT '文件名',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `total_rows` bigint DEFAULT NULL COMMENT '总行数',
  `data_type` varchar(20) DEFAULT 'xlsx' COMMENT '数据类型: xlsx-Excel文件, zip-压缩包',
  `error_msg` text COMMENT '错误信息',
  `checkpoint_offset` bigint DEFAULT '0' COMMENT '断点位置（已处理行数，用于断点续传）',
  `processed_rows` bigint DEFAULT '0' COMMENT '已处理行数',
  `temp_file_path` varchar(500) DEFAULT NULL COMMENT '临时文件路径（用于断点续传）',
  `export_sql` text COMMENT '导出SQL（用于断点续传）',
  `data_source_id` bigint DEFAULT NULL COMMENT '数据源ID（用于断点续传）',
  `create_by` bigint NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_by` (`create_by`),
  KEY `idx_status` (`status`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导出任务表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `field_style_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `field_style_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id` bigint NOT NULL COMMENT '关联报表ID',
  `field_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字段名',
  `style_config` text COLLATE utf8mb4_unicode_ci COMMENT '样式配置JSON，包含字体、对齐、边框、背景、内边距、数据格式等',
  `conditional_rules` text COLLATE utf8mb4_unicode_ci COMMENT '条件规则JSON，包含条件格式化规则列表',
  `sort_order` int DEFAULT '0' COMMENT '排序顺序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_field` (`report_id`,`field_name`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_field_name` (`field_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字段样式配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `lineage_impact_analysis`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lineage_impact_analysis` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `analysis_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分析类型: upstream/downstream',
  `object_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '对象类型',
  `object_id` bigint NOT NULL COMMENT '对象ID',
  `object_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '对象名称',
  `impact_level` int DEFAULT '1' COMMENT '影响层级',
  `impact_count` int DEFAULT '0' COMMENT '影响数量',
  `analysis_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '分析结果JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_object` (`object_type`,`object_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='血缘影响分析记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `lineage_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lineage_metadata` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_ds_id` bigint NOT NULL COMMENT '源数据源ID',
  `source_table` varchar(200) NOT NULL COMMENT '源表名',
  `target_ds_id` bigint NOT NULL COMMENT '目标数据源ID',
  `target_table` varchar(200) NOT NULL COMMENT '目标表名',
  `transform_type` varchar(50) DEFAULT NULL COMMENT '转换类型（etl/collect/sql）',
  `transform_id` bigint DEFAULT NULL COMMENT '关联的任务ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source_ds_id`,`source_table`),
  KEY `idx_target` (`target_ds_id`,`target_table`),
  KEY `idx_transform` (`transform_type`,`transform_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='血缘元数据表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `lineage_parse_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lineage_parse_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `task_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务类型: manual/auto',
  `parse_scope` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '解析范围: all/datasource/report/chart',
  `scope_id` bigint DEFAULT NULL COMMENT '范围ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'pending' COMMENT '状态: pending/running/success/failed',
  `total_count` int DEFAULT '0' COMMENT '总数',
  `success_count` int DEFAULT '0' COMMENT '成功数',
  `fail_count` int DEFAULT '0' COMMENT '失败数',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '错误信息',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='血缘解析任务表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `masking_audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `masking_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '操作用户ID',
  `operation_type` varchar(50) NOT NULL COMMENT '操作类型：query-查询脱敏 / export-导出脱敏',
  `data_source_id` bigint DEFAULT NULL COMMENT '数据源ID',
  `sql_hash` varchar(64) DEFAULT NULL COMMENT 'SQL哈希值（SHA-256）',
  `masked_fields` text COMMENT '脱敏字段列表JSON',
  `row_count` int DEFAULT NULL COMMENT '处理行数',
  `execution_time` bigint DEFAULT NULL COMMENT '脱敏执行时间（毫秒）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`create_time`),
  KEY `idx_ds_time` (`data_source_id`,`create_time`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='脱敏审计日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `masking_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `masking_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '规则名称',
  `data_source_id` bigint DEFAULT NULL COMMENT '数据源ID（为空表示适用所有数据源）',
  `table_name` varchar(200) DEFAULT NULL COMMENT '表名（为空表示适用所有表）',
  `field_name` varchar(100) DEFAULT NULL COMMENT '字段名（精确匹配）',
  `field_pattern` varchar(200) DEFAULT NULL COMMENT '字段名模式（正则匹配）',
  `sensitive_type` varchar(50) NOT NULL COMMENT '敏感类型（PHONE/ID_CARD/BANK_CARD/EMAIL/NAME/ADDRESS/IP_V4/IP_V6/CUSTOM）',
  `strategy_type` varchar(50) NOT NULL COMMENT '脱敏策略（MASK/TRUNCATE/HASH/REPLACE/RANGE/REGEX）',
  `strategy_config` text COMMENT '策略配置JSON',
  `priority` int DEFAULT '0' COMMENT '优先级（数字越小优先级越高）',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用：1-启用 0-禁用',
  `description` varchar(500) DEFAULT NULL COMMENT '规则描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  KEY `idx_field_name` (`field_name`),
  KEY `idx_sensitive_type` (`sensitive_type`),
  KEY `idx_data_source_id` (`data_source_id`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_priority` (`priority`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据脱敏规则表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `masking_rule_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `masking_rule_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_id` bigint NOT NULL COMMENT '脱敏规则ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否对该角色启用此规则：1-启用 0-禁用',
  `masking_level` int DEFAULT '1' COMMENT '脱敏级别：1-完全脱敏 2-部分脱敏 3-不脱敏',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_role` (`rule_id`,`role_id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `fk_masking_rule_role_rule` FOREIGN KEY (`rule_id`) REFERENCES `masking_rule` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='脱敏规则角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `monitor_metric`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `monitor_metric` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cpu_usage` double DEFAULT NULL COMMENT 'CPU使用率(%)',
  `memory_usage` double DEFAULT NULL COMMENT '内存使用率(%)',
  `disk_usage` double DEFAULT NULL COMMENT '磁盘使用率(%)',
  `heap_used` bigint DEFAULT NULL COMMENT 'JVM堆内存已用(MB)',
  `heap_max` bigint DEFAULT NULL COMMENT 'JVM堆内存最大(MB)',
  `thread_count` int DEFAULT NULL COMMENT 'JVM线程数',
  `gc_count` bigint DEFAULT NULL COMMENT 'GC总次数',
  `active_connections` int DEFAULT NULL COMMENT '活跃数据库连接数',
  `running_tasks` int DEFAULT NULL COMMENT '运行中任务数',
  `collect_time` datetime NOT NULL COMMENT '采集时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_collect_time` (`collect_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='监控指标历史记录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `page_chart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `page_chart` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `page_id` bigint NOT NULL COMMENT '页面ID',
  `chart_id` bigint DEFAULT NULL COMMENT '图表ID（引用模式使用，内联模式可为空）',
  `mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'referenced' COMMENT '图表模式：inline-内联, referenced-引用',
  `inline_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '内联图表配置（JSON格式）',
  `x` int DEFAULT '0',
  `y` int DEFAULT '0',
  `w` int DEFAULT '6',
  `h` int DEFAULT '4',
  `left_pos` int DEFAULT '0',
  `top_pos` int DEFAULT '0',
  `width` int DEFAULT '300',
  `height` int DEFAULT '200',
  `sort_order` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `page_id` (`page_id`) USING BTREE,
  KEY `chart_id` (`chart_id`) USING BTREE,
  CONSTRAINT `page_chart_ibfk_1` FOREIGN KEY (`page_id`) REFERENCES `page_definition` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `page_chart_ibfk_2` FOREIGN KEY (`chart_id`) REFERENCES `chart_definition` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=310 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='页面图表关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `page_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `page_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `page_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '页面名称',
  `page_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '页面编码',
  `layout_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '布局配置',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
  `theme` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'default' COMMENT '主题',
  `theme_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '主题配置',
  `parameter_panel` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '参数面板配置（JSON格式）',
  `status` int DEFAULT '1' COMMENT '状态',
  `mobile_enabled` tinyint DEFAULT '0' COMMENT '?????????1-?????-???',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `layout_mode` varchar(20) DEFAULT 'desktop' COMMENT '??????: desktop-?????mobile-?????bigscreen-???',
  `bigscreen_config` text COMMENT 'bigscreen config JSON',
  `mobile_layout_config` text COMMENT 'mobile layout config JSON',
  `project_id` bigint DEFAULT NULL COMMENT 'bigscreen project ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `page_code` (`page_code`) USING BTREE,
  KEY `idx_page_code` (`page_code`) USING BTREE,
  KEY `idx_page_definition_layout_mode` (`layout_mode`),
  KEY `idx_page_definition_project_id` (`project_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='页面定义表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `parameter_usage_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parameter_usage_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `report_id` bigint DEFAULT NULL COMMENT '报表ID（可选）',
  `chart_id` bigint DEFAULT NULL COMMENT '图表ID（可选）',
  `param_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参数名称',
  `param_value` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参数值（JSON格式）',
  `value_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'string' COMMENT '参数值类型',
  `usage_count` int DEFAULT '1' COMMENT '使用次数',
  `last_used_at` datetime NOT NULL COMMENT '最后使用时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_param` (`user_id`,`param_name`),
  KEY `idx_user_report_param` (`user_id`,`report_id`,`param_name`),
  KEY `idx_user_chart_param` (`user_id`,`chart_id`,`param_name`),
  KEY `idx_param_usage` (`param_name`,`usage_count` DESC),
  KEY `idx_last_used` (`last_used_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参数使用历史表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `pipeline_edge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pipeline_edge` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pipeline_id` bigint NOT NULL COMMENT '流程ID',
  `source_node_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '源节点编码',
  `target_node_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '目标节点编码',
  `edge_condition` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '连线条件(可选)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_pipeline_id` (`pipeline_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='流程节点连线表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `pipeline_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pipeline_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pipeline_id` bigint NOT NULL COMMENT '流程ID',
  `pipeline_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程名称',
  `execution_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '执行编号',
  `trigger_type` int DEFAULT '1' COMMENT '触发方式: 1-手动, 2-定时, 3-事件',
  `status` int DEFAULT '2' COMMENT '状态: 0-失败, 1-成功, 2-运行中, 3-已取消',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` bigint DEFAULT NULL COMMENT '执行时长(毫秒)',
  `input_count` bigint DEFAULT '0' COMMENT '输入数据量',
  `output_count` bigint DEFAULT '0' COMMENT '输出数据量',
  `error_count` bigint DEFAULT '0' COMMENT '错误数据量',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '错误信息',
  `execute_log` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '执行日志',
  `execute_by` bigint DEFAULT NULL COMMENT '执行人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_pipeline_id` (`pipeline_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_start_time` (`start_time`) USING BTREE,
  KEY `idx_execution_no` (`execution_no`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='流程执行记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `pipeline_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pipeline_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pipeline_id` bigint NOT NULL COMMENT '流程ID',
  `node_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点编码',
  `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点名称',
  `node_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点类型: source/script/sink/shell/http/condition/sub_process',
  `node_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '节点配置(JSON)',
  `position_x` int DEFAULT '0' COMMENT 'X坐标',
  `position_y` int DEFAULT '0' COMMENT 'Y坐标',
  `sort_order` int DEFAULT '0' COMMENT '执行顺序',
  `pre_task_codes` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '前置任务编码，逗号分隔',
  `fail_strategy` tinyint DEFAULT '0' COMMENT '失败策略: 0-停止流程 1-继续执行',
  `timeout_flag` tinyint DEFAULT '0' COMMENT '是否启用超时: 0-否 1-是',
  `timeout_seconds` int DEFAULT '0' COMMENT '超时时间(秒)',
  `timeout_strategy` tinyint DEFAULT '0' COMMENT '超时策略: 0-告警 1-失败',
  `retry_times` int DEFAULT '0' COMMENT '重试次数',
  `retry_interval` int DEFAULT '1' COMMENT '重试间隔(秒)',
  `priority` tinyint DEFAULT '2' COMMENT '优先级: 0-最高 1-高 2-中 3-低 4-最低',
  `condition_type` tinyint DEFAULT '0' COMMENT '条件类型: 0-无 1-成功分支 2-失败分支',
  `condition_result` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '条件结果判断',
  `run_flag` tinyint DEFAULT '1' COMMENT '是否运行: 0-禁止 1-正常',
  `is_enabled` int DEFAULT '1' COMMENT '是否启用: 0-否, 1-是',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '节点描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_pipeline_id` (`pipeline_id`) USING BTREE,
  KEY `idx_node_type` (`node_type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='流程节点定义表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `pipeline_node_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pipeline_node_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `execution_id` bigint NOT NULL COMMENT '流程执行ID',
  `node_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点编码',
  `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '节点名称',
  `status` int DEFAULT '2' COMMENT '状态: 0-失败, 1-成功, 2-运行中, 3-跳过',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` bigint DEFAULT NULL COMMENT '执行时长(毫秒)',
  `input_count` bigint DEFAULT '0' COMMENT '输入数据量',
  `output_count` bigint DEFAULT '0' COMMENT '输出数据量',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '错误信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_execution_id` (`execution_id`) USING BTREE,
  KEY `idx_node_code` (`node_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='节点执行记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `query_cache_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `query_cache_status` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cache_key` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '缓存键',
  `sql_text` text COLLATE utf8mb4_unicode_ci COMMENT 'SQL语句',
  `sql_hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SQL哈希值',
  `cache_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '缓存状态: L1_HIT, L2_HIT, MISS, SKIPPED',
  `execution_time` bigint NOT NULL DEFAULT '0' COMMENT '执行时间(毫秒)',
  `data_source_id` bigint DEFAULT NULL COMMENT '数据源ID',
  `is_slow_query` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否慢查询',
  `record_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_cache_key` (`cache_key`(255)),
  KEY `idx_sql_hash` (`sql_hash`),
  KEY `idx_cache_status` (`cache_status`),
  KEY `idx_record_time` (`record_time`),
  KEY `idx_slow_query` (`is_slow_query`,`record_time`),
  KEY `idx_ds_time` (`data_source_id`,`record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='查询缓存状态记录表 - 用于慢查询缓存关联分析（需求10.3）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `report_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `report_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '报表名称',
  `report_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '报表编码',
  `data_source_id` bigint NOT NULL COMMENT '数据源ID',
  `sql_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SQL语句',
  `params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '查询参数配置(JSON)',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
  `status` int DEFAULT '1' COMMENT '状态',
  `mobile_enabled` tinyint DEFAULT '0' COMMENT '?????????1-?????-???',
  `report_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'sql' COMMENT '报表类型: sql-SQL模式, visual-可视化模式',
  `allow_export_excel` tinyint(1) NOT NULL DEFAULT '1' COMMENT '允许导出Excel: 1-允许, 0-禁止',
  `allow_export_pdf` tinyint(1) NOT NULL DEFAULT '1' COMMENT '允许导出PDF: 1-允许, 0-禁止',
  `allow_print` tinyint DEFAULT '1' COMMENT '????????-?????-???',
  `pdf_watermark` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'PDF导出水印文字',
  `watermark_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'none' COMMENT '水印类型: none-无水印, user_ip-用户名_IP, custom-自定义文本',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `report_code` (`report_code`) USING BTREE,
  KEY `data_source_id` (`data_source_id`) USING BTREE,
  KEY `idx_report_code` (`report_code`) USING BTREE,
  CONSTRAINT `report_definition_ibfk_1` FOREIGN KEY (`data_source_id`) REFERENCES `data_source` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='报表定义表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `report_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_field` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `report_id` bigint NOT NULL COMMENT '报表ID',
  `field_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字段名',
  `field_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '显示名',
  `field_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '字段类型',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `is_visible` int DEFAULT '1' COMMENT '是否可见',
  `width` int DEFAULT NULL COMMENT '宽度',
  `align` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'left' COMMENT '对齐',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联数据字典类型',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `report_id` (`report_id`) USING BTREE,
  CONSTRAINT `report_field_ibfk_1` FOREIGN KEY (`report_id`) REFERENCES `report_definition` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='报表字段表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `report_share`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_share` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `report_id` bigint NOT NULL COMMENT '报表/图表ID',
  `share_token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分享Token',
  `share_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'report' COMMENT '分享类型：report/chart',
  `password_protected` tinyint(1) DEFAULT '0' COMMENT '是否启用密码保护',
  `access_password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '访问密码',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `max_access_count` int DEFAULT '0' COMMENT '最大访问次数',
  `access_count` int DEFAULT '0' COMMENT '已访问次数',
  `status` int DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_share_token` (`share_token`) USING BTREE,
  KEY `idx_report_id` (`report_id`,`share_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='报表/图表分享链接';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `report_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类：sales-销售报表, finance-财务报表, operation-运营报表, inventory-库存报表, hr-人员报表, custom-自定义',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '模板描述',
  `sql_template` text COLLATE utf8mb4_unicode_ci COMMENT 'SQL模板，支持参数占位符 ${paramName}',
  `fields_config` text COLLATE utf8mb4_unicode_ci COMMENT '字段配置JSON',
  `params_config` text COLLATE utf8mb4_unicode_ci COMMENT '参数配置JSON',
  `preview_image` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '预览图URL',
  `is_system` tinyint(1) DEFAULT '0' COMMENT '是否系统预设模板：1-是，0-否',
  `creator_id` bigint DEFAULT NULL COMMENT '创建者ID，系统模板为null',
  `use_count` int DEFAULT '0' COMMENT '使用次数统计',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_creator` (`creator_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_system` (`is_system`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表模板表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `report_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `report_id` bigint NOT NULL COMMENT '报表ID',
  `version_no` int NOT NULL COMMENT '版本号',
  `config_snapshot` longtext COMMENT '配置快照JSON',
  `sql_snapshot` text COMMENT 'SQL快照',
  `summary` varchar(500) DEFAULT NULL COMMENT '修改摘要',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_report_version` (`report_id`,`version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报表版本表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `resource_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resource_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `resource_type` varchar(50) NOT NULL COMMENT '资源类型: datasource, report, dashboard, folder',
  `resource_id` bigint NOT NULL COMMENT '资源ID',
  `operations` varchar(200) NOT NULL COMMENT '操作权限JSON数组: ["view","edit","delete","export","share"]',
  `inherited` tinyint DEFAULT '0' COMMENT '是否继承自父资源: 0-否, 1-是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_resource` (`role_id`,`resource_type`,`resource_id`),
  KEY `idx_resource` (`resource_type`,`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资源权限表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `rls_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rls_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `data_source_id` bigint NOT NULL COMMENT '数据源ID',
  `table_name` varchar(200) NOT NULL COMMENT '表名',
  `filter_field` varchar(200) NOT NULL COMMENT '过滤字段',
  `filter_operator` varchar(20) NOT NULL COMMENT '操作符（=, !=, >, <, IN, LIKE等）',
  `filter_value` varchar(500) NOT NULL COMMENT '过滤值（支持变量如 ${user.deptId}）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_data_source_id` (`data_source_id`),
  KEY `idx_role_datasource` (`role_id`,`data_source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='行级权限规则表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sql_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sql_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '会话ID',
  `db_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '数据库类型',
  `db_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '数据库名称',
  `sql_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '执行的SQL语句',
  `sql_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'SQL类型',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'success' COMMENT '执行状态',
  `affected_rows` int DEFAULT NULL COMMENT '影响行数',
  `execute_time` bigint DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '错误信息',
  `execute_at` datetime DEFAULT NULL COMMENT '执行时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_session_id` (`session_id`) USING BTREE,
  KEY `idx_execute_at` (`execute_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='SQL执行历史记录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sql_snippet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sql_snippet` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `sql_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SQL内容',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
  `db_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '数据库类型',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='SQL收藏片段';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `style_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `style_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类：finance-财务报表, sales-销售报表, inventory-库存报表, kpi-KPI仪表盘, custom-自定义',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '模板描述',
  `is_system` tinyint(1) DEFAULT '0' COMMENT '是否系统预设模板：1-是，0-否',
  `column_styles` text COLLATE utf8mb4_unicode_ci COMMENT '列样式配置JSON',
  `conditional_rules` text COLLATE utf8mb4_unicode_ci COMMENT '条件规则JSON',
  `table_style` text COLLATE utf8mb4_unicode_ci COMMENT '表格样式配置JSON',
  `preview_image` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '预览图URL',
  `created_by` bigint DEFAULT NULL COMMENT '创建者ID，系统模板为null',
  `use_count` int DEFAULT '0' COMMENT '使用次数统计',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_status` (`status`),
  KEY `idx_is_system` (`is_system`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='样式模板表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `subscription_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscription_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '计划名称',
  `plan_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '计划编码',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `plan_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型：FREE,BASIC,PRO,ENTERPRISE',
  `monthly_price` decimal(10,2) DEFAULT '0.00' COMMENT '月价格',
  `yearly_price` decimal(10,2) DEFAULT '0.00' COMMENT '年价格',
  `max_users` int DEFAULT '0' COMMENT '最大用户数',
  `max_data_sources` int DEFAULT '0' COMMENT '最大数据源数',
  `max_storage_mb` bigint DEFAULT '0' COMMENT '最大存储空间(MB)',
  `features` text COLLATE utf8mb4_unicode_ci COMMENT '功能列表(JSON)',
  `quota_config` text COLLATE utf8mb4_unicode_ci COMMENT '配额配置(JSON)',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_code` (`plan_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅计划表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_ai_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_ai_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(50) NOT NULL COMMENT '配置键',
  `config_value` text COMMENT '配置值',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_ai_prompt_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_ai_prompt_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL,
  `category` varchar(100) DEFAULT '通用',
  `content` text NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_alert_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_alert_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则名称',
  `metric_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '指标类型',
  `metric_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '指标名称',
  `metric_value` decimal(10,2) DEFAULT NULL COMMENT '指标值',
  `threshold_value` decimal(10,2) DEFAULT NULL COMMENT '阈值',
  `alert_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '告警级别',
  `alert_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '告警消息',
  `alert_time` datetime NOT NULL COMMENT '告警时间',
  `is_notified` tinyint(1) DEFAULT '0' COMMENT '是否已通知',
  `notification_time` datetime DEFAULT NULL COMMENT '通知时间',
  `is_resolved` tinyint(1) DEFAULT '0' COMMENT '是否已解决',
  `resolve_time` datetime DEFAULT NULL COMMENT '解决时间',
  `resolve_by` bigint DEFAULT NULL COMMENT '解决人',
  `resolve_note` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '解决备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_rule_id` (`rule_id`) USING BTREE,
  KEY `idx_alert_time` (`alert_time`) USING BTREE,
  KEY `idx_is_resolved` (`is_resolved`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  CONSTRAINT `sys_alert_record_ibfk_1` FOREIGN KEY (`rule_id`) REFERENCES `sys_alert_rule` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='告警记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_alert_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_alert_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则名称',
  `rule_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则编码',
  `metric_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '指标类型: cpu/memory/disk/jvm/db/task',
  `metric_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '指标名称',
  `threshold_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '阈值类型: gt/lt/eq/gte/lte',
  `threshold_value` decimal(10,2) NOT NULL COMMENT '阈值',
  `duration_seconds` int DEFAULT '60' COMMENT '持续时间(秒)',
  `alert_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'warning' COMMENT '告警级别: info/warning/error/critical',
  `alert_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '告警消息模板',
  `notification_channels` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '通知渠道: email,sms,dingtalk',
  `notification_users` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '通知用户ID列表',
  `is_enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `rule_code` (`rule_code`) USING BTREE,
  KEY `idx_rule_code` (`rule_code`) USING BTREE,
  KEY `idx_metric_type` (`metric_type`) USING BTREE,
  KEY `idx_is_enabled` (`is_enabled`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='告警规则表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_annotation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_annotation` (
  `id` varchar(36) NOT NULL COMMENT '标注ID',
  `chart_id` varchar(100) NOT NULL COMMENT '图表ID',
  `user_id` varchar(50) DEFAULT '' COMMENT '用户ID',
  `text` varchar(500) DEFAULT '' COMMENT '标注文本',
  `x` double DEFAULT '0' COMMENT 'X坐标(百分比)',
  `y` double DEFAULT '0' COMMENT 'Y坐标(百分比)',
  `color` varchar(20) DEFAULT '#ff0000' COMMENT '颜色',
  `type` varchar(20) DEFAULT 'point' COMMENT '类型: point/range/line',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_chart_id` (`chart_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图表标注';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_announcement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '公告内容',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'info' COMMENT '公告类型: info/success/warning/error',
  `priority` int DEFAULT '2' COMMENT '优先级: 1-低 2-中 3-高',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0-禁用 1-启用',
  `is_top` tinyint DEFAULT '0' COMMENT '是否置顶: 0-否 1-是',
  `target_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'all' COMMENT '发布范围: all/dept/role',
  `target_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '目标ID列表(JSON)',
  `read_count` int DEFAULT '0' COMMENT '已读人数',
  `attachments` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '附件JSON',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='系统公告表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_announcement_read`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_announcement_read` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `announcement_id` bigint NOT NULL COMMENT '公告ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `read_time` datetime NOT NULL COMMENT '阅读时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_announcement_user` (`announcement_id`,`user_id`),
  KEY `idx_announcement_id` (`announcement_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公告已读记录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_chart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_chart` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `chart_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图表编码',
  `chart_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图表名称',
  `chart_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图表描述',
  `chart_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图表类型: bar/line/pie/area/scatter/table',
  `data_source_id` bigint DEFAULT NULL COMMENT '数据源ID',
  `query_sql` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'SQL查询语句',
  `chart_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '图表配置JSON',
  `field_mapping` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '字段映射JSON',
  `folder_id` bigint DEFAULT NULL COMMENT '所属文件夹',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0-草稿 1-已发布',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `chart_code` (`chart_code`) USING BTREE,
  KEY `idx_chart_type` (`chart_type`) USING BTREE,
  KEY `idx_folder` (`folder_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='图表中心';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_chart_folder`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_chart_folder` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `folder_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `parent_id` bigint DEFAULT '0',
  `sort_order` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='图表文件夹';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_comment` (
  `id` varchar(36) NOT NULL COMMENT '评论ID',
  `resource_type` varchar(50) DEFAULT '' COMMENT '资源类型: dashboard/report/chart',
  `resource_id` varchar(100) DEFAULT '' COMMENT '资源ID',
  `parent_id` varchar(36) DEFAULT NULL COMMENT '父评论ID',
  `user_id` varchar(50) DEFAULT '' COMMENT '用户ID',
  `content` text COMMENT '评论内容',
  `mentions` varchar(500) DEFAULT '' COMMENT '@提及的用户ID(JSON数组)',
  `resolved` tinyint(1) DEFAULT '0' COMMENT '是否已解决',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_resource` (`resource_type`,`resource_id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '配置值',
  `config_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'string' COMMENT '配置类型',
  `config_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
  `is_system` tinyint(1) DEFAULT '0' COMMENT '是否系统配置',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `config_key` (`config_key`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='系统配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_data_view`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_data_view` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '视图名称',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '视图编码（唯一）',
  `data_source_id` bigint NOT NULL COMMENT '数据源ID',
  `table_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据表名',
  `primary_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '自定义主键字段',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0-禁用 1-启用',
  `columns_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '列配置JSON',
  `allow_query` tinyint DEFAULT '1' COMMENT '允许查询',
  `allow_insert` tinyint DEFAULT '1' COMMENT '允许新增',
  `allow_update` tinyint DEFAULT '1' COMMENT '允许编辑',
  `allow_delete` tinyint DEFAULT '1' COMMENT '允许删除',
  `allow_import` tinyint DEFAULT '1' COMMENT '允许导入',
  `allow_export` tinyint DEFAULT '1' COMMENT '允许导出',
  `default_order_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '默认排序字段',
  `default_order_dir` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'DESC' COMMENT '默认排序方向',
  `page_size` int DEFAULT '20' COMMENT '每页条数',
  `generate_menu` tinyint DEFAULT '1' COMMENT '是否生成菜单',
  `menu_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '菜单名称',
  `menu_parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `menu_icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'GridOutline' COMMENT '菜单图标',
  `menu_sort` int DEFAULT '0' COMMENT '菜单排序',
  `menu_id` bigint DEFAULT NULL COMMENT '关联的菜单ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_code` (`code`) USING BTREE,
  KEY `idx_data_source_id` (`data_source_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='数据视图配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_department` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `dept_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门名称',
  `dept_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '部门编码',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门ID',
  `ancestors` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '祖级列表',
  `leader` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '负责人',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '邮箱',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` int DEFAULT '1' COMMENT '状态: 0-禁用 1-启用',
  `del_flag` int DEFAULT '0' COMMENT '删除标志: 0-正常 1-删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_parent_id` (`parent_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门ID',
  `ancestors` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门名称',
  `dept_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '部门编码',
  `leader_id` bigint DEFAULT NULL COMMENT '部门负责人ID',
  `leader_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '部门负责人姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '邮箱',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态：1启用，0禁用',
  `del_flag` tinyint DEFAULT '0' COMMENT '删除标志：0正常，1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_parent_id` (`parent_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_code` varchar(100) NOT NULL COMMENT '关联字典类型编码',
  `label` varchar(100) NOT NULL COMMENT '字典标签',
  `value` varchar(100) NOT NULL COMMENT '字典值',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `css_class` varchar(100) DEFAULT NULL COMMENT '样式类名',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0-禁用 1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_dict_code` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_code` varchar(100) NOT NULL COMMENT '字典类型编码',
  `dict_name` varchar(100) NOT NULL COMMENT '字典类型名称',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0-禁用 1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_code` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_health_check`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_health_check` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_time` datetime NOT NULL COMMENT '检查时间',
  `check_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '检查类型: database/redis/mq/api',
  `check_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '检查名称',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态: healthy/unhealthy/degraded',
  `response_time` bigint DEFAULT NULL COMMENT '响应时间(ms)',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '错误信息',
  `details` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '详细信息JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_check_time` (`check_time`) USING BTREE,
  KEY `idx_check_type` (`check_type`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=73 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='系统健康检查表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_knowledge_article`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_knowledge_article` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL COMMENT '标题',
  `content` longtext COMMENT '内容',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签JSON数组',
  `attachments` text COMMENT '附件JSON数组',
  `view_count` int DEFAULT '0' COMMENT '浏览量',
  `helpful_count` int DEFAULT '0' COMMENT '有帮助数',
  `author_id` varchar(50) DEFAULT NULL COMMENT '作者ID',
  `author_name` varchar(100) DEFAULT NULL COMMENT '作者姓名',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库文章表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_login_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `ip_address` varchar(64) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(1000) DEFAULT NULL COMMENT 'User-Agent',
  `browser` varchar(50) DEFAULT NULL COMMENT '浏览器',
  `os` varchar(50) DEFAULT NULL COMMENT '操作系统',
  `status` varchar(20) NOT NULL DEFAULT 'success' COMMENT '状态: success/failure',
  `message` text COMMENT 'message',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_username` (`username`),
  KEY `idx_status` (`status`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
  `menu_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单编码',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `menu_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'menu' COMMENT '菜单类型',
  `route_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '路由路径',
  `component_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '组件路径',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图标',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `is_visible` int DEFAULT '1' COMMENT '是否可见',
  `mobile_visible` tinyint DEFAULT '1' COMMENT '?????????1-?????-???',
  `permission_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '权限编码',
  `report_id` bigint DEFAULT NULL COMMENT '关联报表ID',
  `chart_id` bigint DEFAULT NULL COMMENT 'Linked chart ID',
  `page_id` bigint DEFAULT NULL COMMENT 'Linked page ID',
  `data_view_code` varchar(100) DEFAULT NULL COMMENT 'Linked data view code',
  `open_mode` varchar(20) DEFAULT 'tab' COMMENT 'Open mode: tab/window/drawer',
  `badge` varchar(50) DEFAULT NULL COMMENT 'Menu badge text',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `menu_code` (`menu_code`) USING BTREE,
  KEY `report_id` (`report_id`) USING BTREE,
  KEY `idx_menu_code` (`menu_code`) USING BTREE,
  KEY `idx_parent_id` (`parent_id`) USING BTREE,
  CONSTRAINT `sys_menu_ibfk_1` FOREIGN KEY (`report_id`) REFERENCES `report_definition` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=132 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='菜单表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `user_id` bigint NOT NULL COMMENT '接收人ID',
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '消息内容',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'NOTICE' COMMENT '消息类型：TODO待办/DONE已办/NOTICE通知/ALERT提醒',
  `level` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'INFO' COMMENT '消息级别：INFO/WARNING/ERROR',
  `business_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '业务类型',
  `business_id` bigint DEFAULT NULL COMMENT '业务ID',
  `sender_id` bigint DEFAULT NULL COMMENT '发送人ID',
  `sender_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '发送人姓名',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读：0未读，1已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user` (`user_id`,`is_read`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息通知表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_message_channel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_message_channel` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `channel_name` varchar(100) NOT NULL COMMENT '通道名称',
  `channel_type` varchar(20) NOT NULL COMMENT '通道类型: email/wecom/dingtalk/sms',
  `config` text COMMENT '配置内容JSON',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否默认: 1-是, 0-否',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态: 1-启用, 0-禁用',
  `description` varchar(500) DEFAULT NULL COMMENT '描述说明',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_channel_type` (`channel_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息通道配置';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '内容',
  `notification_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型',
  `priority` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'normal' COMMENT '优先级',
  `target_user_id` bigint DEFAULT NULL COMMENT '目标用户',
  `sender_id` bigint DEFAULT NULL COMMENT '发送人ID',
  `sender_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '发送人名称',
  `dept_id` bigint DEFAULT NULL COMMENT '目标部门ID',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `related_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联类型',
  `related_id` bigint DEFAULT NULL COMMENT '关联ID',
  `attachments` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '附件JSON',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_target_user_id` (`target_user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='通知消息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_notification_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notification_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `notification_type` varchar(50) DEFAULT NULL COMMENT '通知类型: alert/export/task/report/system',
  `channel` varchar(20) NOT NULL COMMENT '发送渠道: site/email/sms/wecom/dingtalk/websocket',
  `recipient` varchar(500) DEFAULT NULL COMMENT '接收人(邮箱/手机号/用户ID/webhook)',
  `subject` varchar(200) DEFAULT NULL COMMENT '标题',
  `content` text COMMENT '内容摘要',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/success/failed',
  `error_message` text COMMENT '错误信息',
  `retry_count` int DEFAULT '0' COMMENT '重试次数',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_channel` (`channel`),
  KEY `idx_status` (`status`),
  KEY `idx_send_time` (`send_time`),
  KEY `idx_notification_type` (`notification_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知投递日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_notification_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notification_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_code` varchar(50) NOT NULL COMMENT '模板编码(唯一)',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `channel` varchar(20) NOT NULL DEFAULT 'all' COMMENT '适用渠道: email/sms/wecom/dingtalk/all',
  `notification_type` varchar(50) DEFAULT NULL COMMENT '通知类型: alert/export/task/report/system',
  `subject` varchar(200) DEFAULT NULL COMMENT '标题模板',
  `content` text COMMENT '内容模板(支持${variable}占位符)',
  `variables` text COMMENT '可用变量列表(JSON数组)',
  `is_enabled` tinyint(1) DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知模板表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户名',
  `operation_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型',
  `module_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模块名',
  `operation_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '操作描述',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '请求方法',
  `request_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '请求URL',
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '请求参数',
  `response_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '响应结果',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'IP地址',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'success' COMMENT '状态',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '错误信息',
  `before_data` text COMMENT 'before data JSON',
  `after_data` text COMMENT 'after data JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_page_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_page_version` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `page_id` bigint NOT NULL,
  `layout_config` text,
  `charts_json` text,
  `parameter_panel` text,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `remark` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_performance_monitor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_performance_monitor` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `monitor_time` datetime NOT NULL COMMENT '监控时间',
  `cpu_usage` decimal(5,2) DEFAULT NULL COMMENT 'CPU使用率(%)',
  `memory_usage` decimal(5,2) DEFAULT NULL COMMENT '内存使用率(%)',
  `memory_total` bigint DEFAULT NULL COMMENT '总内存(MB)',
  `memory_used` bigint DEFAULT NULL COMMENT '已用内存(MB)',
  `disk_usage` decimal(5,2) DEFAULT NULL COMMENT '磁盘使用率(%)',
  `disk_total` bigint DEFAULT NULL COMMENT '总磁盘(GB)',
  `disk_used` bigint DEFAULT NULL COMMENT '已用磁盘(GB)',
  `jvm_heap_usage` decimal(5,2) DEFAULT NULL COMMENT 'JVM堆使用率(%)',
  `jvm_heap_max` bigint DEFAULT NULL COMMENT 'JVM最大堆(MB)',
  `jvm_heap_used` bigint DEFAULT NULL COMMENT 'JVM已用堆(MB)',
  `thread_count` int DEFAULT NULL COMMENT '线程数',
  `active_thread_count` int DEFAULT NULL COMMENT '活跃线程数',
  `db_connection_active` int DEFAULT NULL COMMENT '活跃数据库连接',
  `db_connection_idle` int DEFAULT NULL COMMENT '空闲数据库连接',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_monitor_time` (`monitor_time`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='系统性能监控表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `permission_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限名称',
  `permission_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限编码',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `permission_code` (`permission_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=78 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='权限表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_position`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_position` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '职位ID',
  `position_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '职位名称',
  `position_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '职位编码',
  `position_level` int DEFAULT '1' COMMENT '职位级别（用于审批层级，数字越大级别越高）',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态：1启用，0禁用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_position_code` (`position_code`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='职位表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位名称',
  `post_level` int DEFAULT '5' COMMENT '岗位级别（1-10，数字越小级别越高，用于审批层级判断）',
  `sort_order` int DEFAULT '0' COMMENT '显示顺序',
  `status` tinyint DEFAULT '1' COMMENT '状态：1正常，0停用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_post_code` (`post_code`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_push_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_push_log` (
  `id` varchar(36) NOT NULL COMMENT '日志ID',
  `subscription_id` varchar(36) NOT NULL COMMENT '订阅ID',
  `status` varchar(20) DEFAULT '' COMMENT '状态: SUCCESS/FAILED/SKIPPED',
  `channel` varchar(20) DEFAULT '' COMMENT '推送渠道',
  `error_message` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `push_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '推送时间',
  PRIMARY KEY (`id`),
  KEY `idx_subscription_id` (`subscription_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='推送日志';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_report_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_report_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `report_id` bigint NOT NULL COMMENT '关联报表定义ID',
  `schedule_name` varchar(100) NOT NULL COMMENT '推送名称',
  `cron_expression` varchar(100) NOT NULL COMMENT 'Cron表达式',
  `recipients` varchar(500) NOT NULL COMMENT '收件人列表(逗号分隔邮箱)',
  `channels` varchar(100) DEFAULT 'email' COMMENT '推送渠道: email/wecom/dingtalk',
  `email_channel_id` bigint DEFAULT NULL COMMENT '邮件通道配置ID',
  `wecom_channel_id` bigint DEFAULT NULL COMMENT '企业微信通道配置ID',
  `dingtalk_channel_id` bigint DEFAULT NULL COMMENT '钉钉通道配置ID',
  `attach_excel` tinyint(1) DEFAULT '1' COMMENT '是否附带Excel附件',
  `attach_format` varchar(20) DEFAULT 'excel' COMMENT '附件格式: excel/pdf',
  `filter_params` text COMMENT '报表过滤参数JSON',
  `date_params` text COMMENT '日期函数参数JSON，如{"start_date":"$yesterday","end_date":"$today"}',
  `is_enabled` tinyint(1) DEFAULT '1',
  `last_run_time` datetime DEFAULT NULL COMMENT '上次执行时间',
  `last_run_status` varchar(20) DEFAULT NULL COMMENT '上次执行状态: success/failed',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_is_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报表定时推送表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_report_subscription`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_report_subscription` (
  `id` varchar(36) NOT NULL COMMENT '订阅ID',
  `user_id` varchar(50) NOT NULL COMMENT '用户ID',
  `resource_type` varchar(50) DEFAULT '' COMMENT '资源类型: report/dashboard',
  `resource_id` varchar(100) DEFAULT '' COMMENT '资源ID',
  `resource_name` varchar(200) DEFAULT '' COMMENT '资源名称',
  `cron_expression` varchar(100) DEFAULT '' COMMENT '推送频率(cron)',
  `push_channel` varchar(20) DEFAULT 'email' COMMENT '推送渠道: email/dingtalk/wecom',
  `recipients` varchar(1000) DEFAULT '' COMMENT '接收人(JSON数组)',
  `format` varchar(20) DEFAULT 'pdf' COMMENT '格式: pdf/excel/image',
  `condition` varchar(500) DEFAULT NULL COMMENT '条件推送表达式',
  `active` tinyint(1) DEFAULT '1' COMMENT '是否有效',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `last_push_at` datetime DEFAULT NULL COMMENT '最近推送时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报表订阅';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `role_code` (`role_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色与部门关联表（数据权限）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`) USING BTREE,
  KEY `menu_id` (`menu_id`) USING BTREE,
  CONSTRAINT `sys_role_menu_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `sys_role_menu_ibfk_2` FOREIGN KEY (`menu_id`) REFERENCES `sys_menu` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=134 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='角色菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `role_id` (`role_id`) USING BTREE,
  KEY `permission_id` (`permission_id`) USING BTREE,
  CONSTRAINT `sys_role_permission_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `sys_role_permission_ibfk_2` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=89 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_share_access_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_share_access_log` (
  `id` varchar(36) NOT NULL COMMENT '日志ID',
  `share_id` varchar(36) NOT NULL COMMENT '分享ID',
  `access_ip` varchar(50) DEFAULT '' COMMENT '访问IP',
  `user_agent` varchar(500) DEFAULT '' COMMENT '用户代理',
  `success` tinyint(1) DEFAULT '1' COMMENT '是否成功',
  `fail_reason` varchar(200) DEFAULT NULL COMMENT '失败原因',
  `access_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  PRIMARY KEY (`id`),
  KEY `idx_share_id` (`share_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分享访问日志';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_share_link`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_share_link` (
  `id` varchar(36) NOT NULL COMMENT '分享ID',
  `token` varchar(64) NOT NULL COMMENT '分享令牌',
  `resource_type` varchar(50) DEFAULT '' COMMENT '资源类型: dashboard/report/chart',
  `resource_id` varchar(100) DEFAULT '' COMMENT '资源ID',
  `resource_name` varchar(200) DEFAULT '' COMMENT '资源名称',
  `created_by` varchar(50) DEFAULT '' COMMENT '创建者',
  `access_type` varchar(20) DEFAULT 'public' COMMENT '访问类型: public/password/internal',
  `password` varchar(100) DEFAULT NULL COMMENT '访问密码',
  `expire_at` datetime DEFAULT NULL COMMENT '过期时间',
  `max_access_count` int DEFAULT '0' COMMENT '最大访问次数(0=无限)',
  `access_count` int DEFAULT '0' COMMENT '已访问次数',
  `watermark_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用水印',
  `embeddable` tinyint(1) DEFAULT '0' COMMENT '是否允许嵌入',
  `active` tinyint(1) DEFAULT '1' COMMENT '是否有效',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分享链接';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_slow_query_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_slow_query_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `data_source_id` bigint DEFAULT NULL COMMENT '数据源ID',
  `data_source_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '数据源名称',
  `sql_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SQL语句',
  `sql_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'SQL哈希',
  `execution_time` bigint NOT NULL COMMENT '执行时间(ms)',
  `rows_examined` bigint DEFAULT NULL COMMENT '扫描行数',
  `rows_returned` bigint DEFAULT NULL COMMENT '返回行数',
  `query_time` datetime NOT NULL COMMENT '查询时间',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户名',
  `client_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '客户端IP',
  `database_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '数据库名',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_data_source_id` (`data_source_id`) USING BTREE,
  KEY `idx_execution_time` (`execution_time`) USING BTREE,
  KEY `idx_query_time` (`query_time`) USING BTREE,
  KEY `idx_sql_hash` (`sql_hash`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='慢查询日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_team_space`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_team_space` (
  `id` varchar(36) NOT NULL COMMENT '空间ID',
  `name` varchar(100) NOT NULL COMMENT '空间名称',
  `description` varchar(500) DEFAULT '' COMMENT '空间描述',
  `owner_id` varchar(50) NOT NULL COMMENT '创建者ID',
  `visibility` varchar(20) DEFAULT 'private' COMMENT '可见性: public/private',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='团队空间';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_team_space_activity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_team_space_activity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `space_id` varchar(36) NOT NULL COMMENT '空间ID',
  `user_id` varchar(50) NOT NULL COMMENT '用户ID',
  `action` varchar(50) NOT NULL COMMENT '操作类型',
  `detail` varchar(500) DEFAULT '' COMMENT '操作详情',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_space_id` (`space_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='团队空间活动';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_team_space_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_team_space_file` (
  `id` varchar(36) NOT NULL COMMENT '文件ID',
  `space_id` varchar(36) NOT NULL COMMENT '空间ID',
  `name` varchar(255) NOT NULL COMMENT '文件名',
  `size` bigint DEFAULT '0' COMMENT '文件大小(字节)',
  `content_type` varchar(100) DEFAULT '' COMMENT '文件类型',
  `uploaded_by` varchar(50) NOT NULL COMMENT '上传者ID',
  `storage_path` varchar(500) DEFAULT '' COMMENT '存储路径',
  `uploaded_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_space_id` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='团队空间文件';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_team_space_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_team_space_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `space_id` varchar(36) NOT NULL COMMENT '空间ID',
  `user_id` varchar(50) NOT NULL COMMENT '用户ID',
  `role` varchar(20) DEFAULT 'viewer' COMMENT '角色: owner/admin/editor/viewer',
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_space_user` (`space_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='团队空间成员';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_team_space_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_team_space_message` (
  `id` varchar(36) NOT NULL COMMENT '消息ID',
  `space_id` varchar(36) NOT NULL COMMENT '空间ID',
  `sender_id` varchar(50) NOT NULL COMMENT '发送者ID',
  `sender_name` varchar(100) DEFAULT '' COMMENT '发送者名称',
  `content` text COMMENT '消息内容',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_space_id` (`space_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='团队空间消息';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_team_space_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_team_space_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `space_id` varchar(36) NOT NULL COMMENT '空间ID',
  `resource_id` varchar(100) NOT NULL COMMENT '资源ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_space_resource` (`space_id`,`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='团队空间资源';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_ticket`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_ticket` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticket_no` varchar(20) NOT NULL COMMENT '工单编号',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `description` text COMMENT '描述',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `priority` varchar(20) DEFAULT 'medium' COMMENT '优先级: low/medium/high/urgent',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态: pending/processing/resolved/closed',
  `submitter_id` varchar(50) DEFAULT NULL COMMENT '提交人ID',
  `submitter_name` varchar(100) DEFAULT NULL COMMENT '提交人姓名',
  `assignee_id` varchar(50) DEFAULT NULL COMMENT '处理人ID',
  `assignee_name` varchar(100) DEFAULT NULL COMMENT '处理人姓名',
  `attachments` text COMMENT '附件JSON数组',
  `resolution` text COMMENT '解决方案',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `resolved_time` datetime DEFAULT NULL COMMENT '解决时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_no` (`ticket_no`),
  KEY `idx_status` (`status`),
  KEY `idx_submitter` (`submitter_id`),
  KEY `idx_assignee` (`assignee_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_ticket_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_ticket_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticket_id` bigint NOT NULL COMMENT '工单ID',
  `user_id` varchar(50) DEFAULT NULL COMMENT '评论人ID',
  `user_name` varchar(100) DEFAULT NULL COMMENT '评论人姓名',
  `content` text NOT NULL COMMENT '评论内容',
  `internal` tinyint(1) DEFAULT '0' COMMENT '是否内部备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ticket_id` (`ticket_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单评论表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `post_id` bigint DEFAULT NULL COMMENT 'post id',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '手机号',
  `gender` int DEFAULT '0' COMMENT '性别: 0-未知 1-男 2-女',
  `must_change_password` tinyint(1) DEFAULT '0' COMMENT '是否必须修改密码',
  `status` int DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `username` (`username`) USING BTREE,
  KEY `idx_sys_user_post_id` (`post_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_post` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`,`post_id`) USING BTREE,
  KEY `idx_post_id` (`post_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户与岗位关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE,
  KEY `role_id` (`role_id`) USING BTREE,
  CONSTRAINT `sys_user_role_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `sys_user_role_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tenant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '租户ID',
  `tenant_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户编码（唯一标识）',
  `tenant_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户名称',
  `domain` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户域名',
  `logo_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户Logo URL',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '租户状态：0-禁用，1-启用，2-试用',
  `isolation_mode` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SHARED' COMMENT '数据隔离模式：SHARED-共享表，SCHEMA-独立Schema，DATABASE-独立数据库',
  `database_config` text COLLATE utf8mb4_unicode_ci COMMENT '独立数据库连接信息（JSON格式）',
  `admin_user_id` bigint DEFAULT NULL COMMENT '管理员用户ID',
  `contact_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系电话',
  `contact_email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系邮箱',
  `trial_start_time` datetime DEFAULT NULL COMMENT '试用开始时间',
  `trial_end_time` datetime DEFAULT NULL COMMENT '试用结束时间',
  `subscription_plan_id` bigint DEFAULT NULL COMMENT '订阅计划ID',
  `subscription_start_time` datetime DEFAULT NULL COMMENT '订阅开始时间',
  `subscription_end_time` datetime DEFAULT NULL COMMENT '订阅结束时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`),
  UNIQUE KEY `uk_domain` (`domain`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tenant_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` text COLLATE utf8mb4_unicode_ci COMMENT '配置值',
  `config_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'STRING' COMMENT '配置类型：STRING, NUMBER, BOOLEAN, JSON',
  `config_group` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'THEME' COMMENT '配置分组',
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_config` (`tenant_id`,`config_key`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_config_group` (`config_group`),
  CONSTRAINT `fk_tenant_config_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tenant_quota`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_quota` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配额ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `quota_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配额类型',
  `quota_limit` bigint NOT NULL DEFAULT '0' COMMENT '配额上限',
  `quota_used` bigint NOT NULL DEFAULT '0' COMMENT '已使用量',
  `unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '个' COMMENT '配额单位',
  `limit_strategy` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'WARN' COMMENT '限制策略：WARN-警告，THROTTLE-限速，BLOCK-禁止',
  `alert_threshold` int DEFAULT '80' COMMENT '预警阈值百分比',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_quota` (`tenant_id`,`quota_type`),
  KEY `idx_tenant_id` (`tenant_id`),
  CONSTRAINT `fk_tenant_quota_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户配额表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tenant_subscription`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_subscription` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `plan_id` bigint NOT NULL COMMENT '订阅计划ID',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE,EXPIRED,CANCELLED,SUSPENDED',
  `billing_cycle` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MONTHLY' COMMENT '计费周期：MONTHLY,YEARLY',
  `amount` decimal(10,2) DEFAULT '0.00' COMMENT '订阅金额',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `auto_renew` tinyint(1) DEFAULT '1' COMMENT '自动续费',
  `last_renew_time` datetime DEFAULT NULL COMMENT '上次续费时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`),
  KEY `fk_subscription_plan` (`plan_id`),
  CONSTRAINT `fk_subscription_plan` FOREIGN KEY (`plan_id`) REFERENCES `subscription_plan` (`id`),
  CONSTRAINT `fk_subscription_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户订阅表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `v_active_tasks`;
/*!50001 DROP VIEW IF EXISTS `v_active_tasks`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_active_tasks` AS SELECT 
 1 AS `id`,
 1 AS `task_name`,
 1 AS `collect_mode`,
 1 AS `data_source_name`,
 1 AS `table_name`,
 1 AS `status`,
 1 AS `last_execute_time`,
 1 AS `running_minutes`*/;
SET character_set_client = @saved_cs_client;
DROP TABLE IF EXISTS `v_collect_task_detail`;
/*!50001 DROP VIEW IF EXISTS `v_collect_task_detail`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_collect_task_detail` AS SELECT 
 1 AS `id`,
 1 AS `task_name`,
 1 AS `table_name`,
 1 AS `target_table_name`,
 1 AS `collect_mode`,
 1 AS `status`,
 1 AS `last_execute_time`,
 1 AS `last_execute_result`,
 1 AS `batch_size`,
 1 AS `auto_create_table`,
 1 AS `create_time`,
 1 AS `update_time`,
 1 AS `source_id`,
 1 AS `source_name`,
 1 AS `source_db_type`,
 1 AS `source_host`,
 1 AS `target_id`,
 1 AS `target_name`*/;
SET character_set_client = @saved_cs_client;
DROP TABLE IF EXISTS `v_datasource_usage`;
/*!50001 DROP VIEW IF EXISTS `v_datasource_usage`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_datasource_usage` AS SELECT 
 1 AS `id`,
 1 AS `name`,
 1 AS `db_type`,
 1 AS `host`,
 1 AS `port`,
 1 AS `source_task_count`,
 1 AS `target_task_count`,
 1 AS `total_task_count`,
 1 AS `create_time`*/;
SET character_set_client = @saved_cs_client;
DROP TABLE IF EXISTS `v_incremental_tasks`;
/*!50001 DROP VIEW IF EXISTS `v_incremental_tasks`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_incremental_tasks` AS SELECT 
 1 AS `id`,
 1 AS `task_name`,
 1 AS `table_name`,
 1 AS `collect_mode`,
 1 AS `incremental_field`,
 1 AS `incremental_type`,
 1 AS `last_collect_value`,
 1 AS `last_execute_time`,
 1 AS `status`,
 1 AS `data_source_name`,
 1 AS `db_type`*/;
SET character_set_client = @saved_cs_client;
DROP TABLE IF EXISTS `v_operation_log_summary`;
/*!50001 DROP VIEW IF EXISTS `v_operation_log_summary`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_operation_log_summary` AS SELECT 
 1 AS `module_name`,
 1 AS `operation_type`,
 1 AS `operation_count`,
 1 AS `success_count`,
 1 AS `failed_count`,
 1 AS `avg_duration`,
 1 AS `max_duration`,
 1 AS `min_duration`,
 1 AS `operation_date`*/;
SET character_set_client = @saved_cs_client;
DROP TABLE IF EXISTS `v_unread_notifications`;
/*!50001 DROP VIEW IF EXISTS `v_unread_notifications`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_unread_notifications` AS SELECT 
 1 AS `id`,
 1 AS `title`,
 1 AS `content`,
 1 AS `notification_type`,
 1 AS `priority`,
 1 AS `target_user_id`,
 1 AS `username`,
 1 AS `nickname`,
 1 AS `create_time`,
 1 AS `minutes_ago`*/;
SET character_set_client = @saved_cs_client;
DROP TABLE IF EXISTS `v_user_menus`;
/*!50001 DROP VIEW IF EXISTS `v_user_menus`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_user_menus` AS SELECT 
 1 AS `user_id`,
 1 AS `username`,
 1 AS `menu_id`,
 1 AS `menu_name`,
 1 AS `menu_code`,
 1 AS `parent_id`,
 1 AS `route_path`,
 1 AS `icon`,
 1 AS `sort_order`,
 1 AS `permission_code`*/;
SET character_set_client = @saved_cs_client;
DROP TABLE IF EXISTS `v_user_permissions`;
/*!50001 DROP VIEW IF EXISTS `v_user_permissions`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_user_permissions` AS SELECT 
 1 AS `user_id`,
 1 AS `username`,
 1 AS `nickname`,
 1 AS `role_id`,
 1 AS `role_name`,
 1 AS `role_code`,
 1 AS `permission_id`,
 1 AS `permission_name`,
 1 AS `permission_code`*/;
SET character_set_client = @saved_cs_client;
DROP TABLE IF EXISTS `wf_approval_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_approval_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `instance_id` bigint NOT NULL COMMENT '流程实例ID',
  `task_id` bigint DEFAULT NULL COMMENT '任务ID',
  `node_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '节点ID',
  `node_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '节点名称',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '操作人姓名',
  `operator_dept_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '操作人部门',
  `action` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型：START发起/APPROVE同意/REJECT驳回/TRANSFER转办/DELEGATE委托/CANCEL撤销/WITHDRAW撤回',
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '审批意见',
  `attachments` json DEFAULT NULL COMMENT '附件列表',
  `form_data` json DEFAULT NULL COMMENT '表单快照',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_instance` (`instance_id`) USING BTREE,
  KEY `idx_operator` (`operator_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `wf_approval_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_approval_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `process_id` bigint NOT NULL COMMENT '流程定义ID',
  `node_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点ID',
  `node_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '节点名称',
  `rule_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则类型：INITIATOR_LEADER发起人直属领导/DEPT_LEADER部门负责人/POSITION指定职位/USER指定用户/ROLE指定角色',
  `rule_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '规则值（职位ID/用户ID/角色ID等，多个用逗号分隔）',
  `level_offset` int DEFAULT '1' COMMENT '层级偏移（用于向上查找领导，1=直接上级，2=上上级）',
  `is_skip_same` tinyint DEFAULT '0' COMMENT '是否跳过相同审批人：0否，1是',
  `is_countersign` tinyint DEFAULT '0' COMMENT '是否会签：0否（或签），1是',
  `sort_order` int DEFAULT '0' COMMENT '节点顺序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_process_node` (`process_id`,`node_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批规则表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `wf_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `instance_id` bigint DEFAULT NULL COMMENT '关联流程实例ID',
  `task_id` bigint DEFAULT NULL COMMENT '关联任务ID',
  `sender_id` bigint DEFAULT NULL COMMENT '发送人ID',
  `sender_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '发送人姓名',
  `receiver_id` bigint NOT NULL COMMENT '接收人ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '消息内容',
  `msg_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'system' COMMENT '消息类型：system系统/todo待办/notice公告/remind提醒',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读：0未读，1已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_receiver` (`receiver_id`) USING BTREE,
  KEY `idx_is_read` (`is_read`) USING BTREE,
  KEY `idx_msg_type` (`msg_type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息通知表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `wf_process_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_process_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流程定义ID',
  `process_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程标识（唯一）',
  `process_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程名称',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'default' COMMENT '流程分类',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程图标',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '流程描述',
  `process_config` json DEFAULT NULL COMMENT '流程配置（节点、连线等）',
  `form_config` json DEFAULT NULL COMMENT '表单配置',
  `version` int DEFAULT '1' COMMENT '版本号',
  `status` tinyint DEFAULT '1' COMMENT '状态：1启用，0禁用',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_process_key` (`process_key`) USING BTREE,
  KEY `idx_category` (`category`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程定义表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `wf_process_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_process_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流程实例ID',
  `process_id` bigint NOT NULL COMMENT '流程定义ID',
  `process_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程标识',
  `process_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程名称',
  `business_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '业务主键',
  `business_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '业务类型',
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程标题',
  `initiator_id` bigint NOT NULL COMMENT '发起人ID',
  `initiator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '发起人姓名',
  `initiator_dept_id` bigint DEFAULT NULL COMMENT '发起人部门ID',
  `initiator_dept_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '发起人部门名称',
  `current_node` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '当前节点ID',
  `current_node_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '当前节点名称',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'RUNNING' COMMENT '状态：RUNNING进行中/COMPLETED已完成/REJECTED已驳回/CANCELLED已撤销',
  `form_data` json DEFAULT NULL COMMENT '表单数据',
  `variables` json DEFAULT NULL COMMENT '流程变量',
  `priority` int DEFAULT '0' COMMENT '优先级',
  `start_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` bigint DEFAULT NULL COMMENT '耗时（秒）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_process_id` (`process_id`) USING BTREE,
  KEY `idx_initiator` (`initiator_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_business` (`business_type`,`business_key`) USING BTREE,
  KEY `idx_start_time` (`start_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程实例表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `wf_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `instance_id` bigint NOT NULL COMMENT '流程实例ID',
  `process_id` bigint DEFAULT NULL COMMENT '流程定义ID',
  `node_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '节点ID',
  `node_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '节点名称',
  `node_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'APPROVE' COMMENT '节点类型：APPROVE审批/CC抄送/NOTIFY通知',
  `task_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'SINGLE' COMMENT '任务类型：SINGLE单人/OR或签/AND会签',
  `assignee_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `assignee_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理人姓名',
  `candidate_users` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '候选用户IDs（逗号分隔）',
  `candidate_roles` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '候选角色IDs（逗号分隔）',
  `candidate_depts` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '候选部门IDs（逗号分隔）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'PENDING' COMMENT '状态：PENDING待处理/CLAIMED已认领/COMPLETED已完成/CANCELLED已取消',
  `action` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '操作：APPROVE同意/REJECT驳回/TRANSFER转办/DELEGATE委托/WITHDRAW撤回',
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '审批意见',
  `form_data` json DEFAULT NULL COMMENT '任务表单数据',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `claim_time` datetime DEFAULT NULL COMMENT '认领时间',
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `due_date` datetime DEFAULT NULL COMMENT '截止时间',
  `duration` bigint DEFAULT NULL COMMENT '处理耗时（秒）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_instance` (`instance_id`) USING BTREE,
  KEY `idx_assignee` (`assignee_id`,`status`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50106 SET @save_time_zone= @@TIME_ZONE */ ;
/*!50106 DROP EVENT IF EXISTS `evt_archive_old_notifications` */;
DELIMITER ;;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;;
/*!50003 SET character_set_client  = utf8mb4 */ ;;
/*!50003 SET character_set_results = utf8mb4 */ ;;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;;
/*!50003 SET @saved_time_zone      = @@time_zone */ ;;
/*!50003 SET time_zone             = 'SYSTEM' */ ;;
/*!50106 CREATE*/ /*!50106 EVENT `evt_archive_old_notifications` ON SCHEDULE EVERY 1 WEEK STARTS '2025-11-22 00:32:26' ON COMPLETION NOT PRESERVE ENABLE DO BEGIN

    -- 将30天前的未读通知标记为已读

    UPDATE sys_notification

    SET is_read = TRUE, read_time = NOW()

    WHERE is_read = FALSE 

    AND create_time < DATE_SUB(NOW(), INTERVAL 30 DAY);

END */ ;;
/*!50003 SET time_zone             = @saved_time_zone */ ;;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;;
/*!50003 SET character_set_client  = @saved_cs_client */ ;;
/*!50003 SET character_set_results = @saved_cs_results */ ;;
/*!50003 SET collation_connection  = @saved_col_connection */ ;;
/*!50106 DROP EVENT IF EXISTS `evt_cleanup_old_logs` */;;
DELIMITER ;;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;;
/*!50003 SET character_set_client  = utf8mb4 */ ;;
/*!50003 SET character_set_results = utf8mb4 */ ;;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;;
/*!50003 SET @saved_time_zone      = @@time_zone */ ;;
/*!50003 SET time_zone             = 'SYSTEM' */ ;;
/*!50106 CREATE*/ /*!50106 EVENT `evt_cleanup_old_logs` ON SCHEDULE EVERY 1 DAY STARTS '2025-11-22 00:32:26' ON COMPLETION NOT PRESERVE ENABLE DO BEGIN

    -- 获取配置的日志保留天数

    DECLARE retention_days INT DEFAULT 90;

    

    SELECT config_value INTO retention_days

    FROM sys_config

    WHERE config_key = 'log.retention_days'

    LIMIT 1;

    

    -- 删除过期日志

    DELETE FROM sys_operation_log

    WHERE create_time < DATE_SUB(NOW(), INTERVAL retention_days DAY);

    

    -- 记录清理日志

    INSERT INTO sys_operation_log (

        operation_type, module_name, operation_desc, status

    ) VALUES (

        'cleanup', 'system', 

        CONCAT('自动清理', ROW_COUNT(), '条过期日志'), 

        'success'

    );

END */ ;;
/*!50003 SET time_zone             = @saved_time_zone */ ;;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;;
/*!50003 SET character_set_client  = @saved_cs_client */ ;;
/*!50003 SET character_set_results = @saved_cs_results */ ;;
/*!50003 SET collation_connection  = @saved_col_connection */ ;;
DELIMITER ;
/*!50106 SET TIME_ZONE= @save_time_zone */ ;

USE `data_platform`;
/*!50001 DROP VIEW IF EXISTS `v_active_tasks`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `v_active_tasks` AS select `ct`.`id` AS `id`,`ct`.`task_name` AS `task_name`,`ct`.`collect_mode` AS `collect_mode`,`ds`.`name` AS `data_source_name`,`ct`.`table_name` AS `table_name`,`ct`.`status` AS `status`,`ct`.`last_execute_time` AS `last_execute_time`,timestampdiff(MINUTE,`ct`.`last_execute_time`,now()) AS `running_minutes` from (`collect_task` `ct` left join `data_source` `ds` on((`ct`.`data_source_id` = `ds`.`id`))) where (`ct`.`status` = 'running') */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `v_collect_task_detail`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `v_collect_task_detail` AS select `ct`.`id` AS `id`,`ct`.`task_name` AS `task_name`,`ct`.`table_name` AS `table_name`,`ct`.`target_table_name` AS `target_table_name`,`ct`.`collect_mode` AS `collect_mode`,`ct`.`status` AS `status`,`ct`.`last_execute_time` AS `last_execute_time`,`ct`.`last_execute_result` AS `last_execute_result`,`ct`.`batch_size` AS `batch_size`,`ct`.`auto_create_table` AS `auto_create_table`,`ct`.`create_time` AS `create_time`,`ct`.`update_time` AS `update_time`,`ds_source`.`id` AS `source_id`,`ds_source`.`name` AS `source_name`,`ds_source`.`db_type` AS `source_db_type`,`ds_source`.`host` AS `source_host`,`ds_target`.`id` AS `target_id`,`ds_target`.`name` AS `target_name` from ((`collect_task` `ct` left join `data_source` `ds_source` on((`ct`.`data_source_id` = `ds_source`.`id`))) left join `data_source` `ds_target` on((`ct`.`target_data_source_id` = `ds_target`.`id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `v_datasource_usage`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `v_datasource_usage` AS select `ds`.`id` AS `id`,`ds`.`name` AS `name`,`ds`.`db_type` AS `db_type`,`ds`.`host` AS `host`,`ds`.`port` AS `port`,count(distinct (case when (`dj`.`source_data_source_id` = `ds`.`id`) then `dj`.`id` end)) AS `source_task_count`,count(distinct (case when (`dj`.`target_data_source_id` = `ds`.`id`) then `dj`.`id` end)) AS `target_task_count`,(count(distinct `ct`.`id`) + count(distinct `dj`.`id`)) AS `total_task_count`,`ds`.`create_time` AS `create_time` from ((`data_source` `ds` left join `collect_task` `ct` on((`ds`.`id` = `ct`.`data_source_id`))) left join `datax_job` `dj` on(((`ds`.`id` = `dj`.`source_data_source_id`) or (`ds`.`id` = `dj`.`target_data_source_id`)))) group by `ds`.`id`,`ds`.`name`,`ds`.`db_type`,`ds`.`host`,`ds`.`port`,`ds`.`create_time` order by `total_task_count` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `v_incremental_tasks`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `v_incremental_tasks` AS select `ct`.`id` AS `id`,`ct`.`task_name` AS `task_name`,`ct`.`table_name` AS `table_name`,`ct`.`collect_mode` AS `collect_mode`,`ct`.`incremental_field` AS `incremental_field`,`ct`.`incremental_type` AS `incremental_type`,`ct`.`last_collect_value` AS `last_collect_value`,`ct`.`last_execute_time` AS `last_execute_time`,`ct`.`status` AS `status`,`ds`.`name` AS `data_source_name`,`ds`.`db_type` AS `db_type` from (`collect_task` `ct` left join `data_source` `ds` on((`ct`.`data_source_id` = `ds`.`id`))) where (`ct`.`collect_mode` = 'incremental') order by `ct`.`last_execute_time` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `v_operation_log_summary`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `v_operation_log_summary` AS select `sys_operation_log`.`module_name` AS `module_name`,`sys_operation_log`.`operation_type` AS `operation_type`,count(0) AS `operation_count`,count((case when (`sys_operation_log`.`status` = 'success') then 1 end)) AS `success_count`,count((case when (`sys_operation_log`.`status` = 'failed') then 1 end)) AS `failed_count`,avg(`sys_operation_log`.`duration_ms`) AS `avg_duration`,max(`sys_operation_log`.`duration_ms`) AS `max_duration`,min(`sys_operation_log`.`duration_ms`) AS `min_duration`,cast(`sys_operation_log`.`create_time` as date) AS `operation_date` from `sys_operation_log` where (`sys_operation_log`.`create_time` >= (now() - interval 7 day)) group by `sys_operation_log`.`module_name`,`sys_operation_log`.`operation_type`,cast(`sys_operation_log`.`create_time` as date) order by `operation_count` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `v_unread_notifications`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `v_unread_notifications` AS select `sn`.`id` AS `id`,`sn`.`title` AS `title`,`sn`.`content` AS `content`,`sn`.`notification_type` AS `notification_type`,`sn`.`priority` AS `priority`,`sn`.`target_user_id` AS `target_user_id`,`u`.`username` AS `username`,`u`.`nickname` AS `nickname`,`sn`.`create_time` AS `create_time`,timestampdiff(MINUTE,`sn`.`create_time`,now()) AS `minutes_ago` from (`sys_notification` `sn` left join `sys_user` `u` on((`sn`.`target_user_id` = `u`.`id`))) where (`sn`.`is_read` = false) order by `sn`.`priority`,`sn`.`create_time` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `v_user_menus`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `v_user_menus` AS select distinct `u`.`id` AS `user_id`,`u`.`username` AS `username`,`m`.`id` AS `menu_id`,`m`.`menu_name` AS `menu_name`,`m`.`menu_code` AS `menu_code`,`m`.`parent_id` AS `parent_id`,`m`.`route_path` AS `route_path`,`m`.`icon` AS `icon`,`m`.`sort_order` AS `sort_order`,`m`.`permission_code` AS `permission_code` from ((((`sys_user` `u` left join `sys_user_role` `ur` on((`u`.`id` = `ur`.`user_id`))) left join `sys_role` `r` on((`ur`.`role_id` = `r`.`id`))) left join `sys_role_menu` `rm` on((`r`.`id` = `rm`.`role_id`))) left join `sys_menu` `m` on((`rm`.`menu_id` = `m`.`id`))) where ((`u`.`status` = 1) and (`m`.`is_visible` = 1)) order by `m`.`sort_order` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `v_user_permissions`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `v_user_permissions` AS select `u`.`id` AS `user_id`,`u`.`username` AS `username`,`u`.`nickname` AS `nickname`,`r`.`id` AS `role_id`,`r`.`role_name` AS `role_name`,`r`.`role_code` AS `role_code`,`p`.`id` AS `permission_id`,`p`.`permission_name` AS `permission_name`,`p`.`permission_code` AS `permission_code` from ((((`sys_user` `u` left join `sys_user_role` `ur` on((`u`.`id` = `ur`.`user_id`))) left join `sys_role` `r` on((`ur`.`role_id` = `r`.`id`))) left join `sys_role_permission` `rp` on((`r`.`id` = `rp`.`role_id`))) left join `sys_permission` `p` on((`rp`.`permission_id` = `p`.`id`))) where (`u`.`status` = 1) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


