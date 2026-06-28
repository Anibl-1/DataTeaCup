-- ============================================================
-- DataTeaCup full database initialization
-- Generated from local data_platform database.
-- Includes schema and safe seed data only.
-- ============================================================


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


-- ============================================================
-- Seed data
-- ============================================================


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

LOCK TABLES `sys_config` WRITE;
/*!40000 ALTER TABLE `sys_config` DISABLE KEYS */;
INSERT INTO `sys_config` (`id`, `config_key`, `config_value`, `config_type`, `config_desc`, `is_system`, `create_time`, `update_time`) VALUES (3,'system.title','DataTeaCup 数据平台','string','系统标题',1,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(4,'export.max_records','100000','number','最大导出记录数',1,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(5,'session.timeout','30','number','会话超时时间(分钟)',1,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(7,'system.copyright','DataTeaCup © 2026','string','系统版权信息',1,'2026-06-21 18:46:35','2026-06-21 18:46:35');
/*!40000 ALTER TABLE `sys_config` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_ai_config` WRITE;
/*!40000 ALTER TABLE `sys_ai_config` DISABLE KEYS */;
INSERT INTO `sys_ai_config` (`id`, `config_key`, `config_value`, `description`, `create_time`, `update_time`) VALUES (1,'provider','deepseek','AI服务商：openai/qwen/deepseek/ollama','2026-06-21 18:43:25','2026-06-21 18:43:25'),(2,'openai_api_key','','OpenAI API密钥','2026-06-21 18:43:25','2026-06-21 18:43:25'),(3,'openai_base_url','https://api.openai.com/v1','OpenAI API地址','2026-06-21 18:43:25','2026-06-21 18:43:25'),(4,'openai_model','gpt-3.5-turbo','OpenAI模型','2026-06-21 18:43:25','2026-06-21 18:43:25'),(5,'qwen_api_key','','千问API密钥','2026-06-21 18:43:25','2026-06-21 18:43:25'),(6,'qwen_base_url','https://dashscope.aliyuncs.com/api/v1','千问API地址','2026-06-21 18:43:25','2026-06-21 18:43:25'),(7,'qwen_model','qwen-turbo','千问模型','2026-06-21 18:43:25','2026-06-21 18:43:25'),(8,'deepseek_api_key','','DeepSeek API密钥','2026-06-21 18:43:25','2026-06-21 18:43:25'),(9,'deepseek_base_url','https://api.deepseek.com','DeepSeek API地址','2026-06-21 18:43:25','2026-06-21 18:43:25'),(10,'deepseek_model','deepseek-v4-flash','DeepSeek模型','2026-06-21 18:43:25','2026-06-21 18:43:25'),(11,'ollama_base_url','http://localhost:11434','Ollama API地址','2026-06-21 18:43:25','2026-06-21 18:43:25'),(12,'ollama_model','llama3','Ollama模型','2026-06-21 18:43:25','2026-06-21 18:43:25'),(13,'max_history','5','对话历史记录数','2026-06-21 18:43:25','2026-06-21 18:43:25');
/*!40000 ALTER TABLE `sys_ai_config` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_permission` WRITE;
/*!40000 ALTER TABLE `sys_permission` DISABLE KEYS */;
INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `description`, `create_time`) VALUES (35,'数据源管理','data:source','数据源增删改查','2026-06-21 18:43:25'),(36,'数据采集','data:collect','数据采集任务管理','2026-06-21 18:43:25'),(37,'采集日志','data:collect:log','采集日志查看','2026-06-21 18:43:25'),(38,'数据导入','data:import','数据导入功能','2026-06-21 18:43:25'),(39,'DataX任务','datax:job','DataX传输任务管理','2026-06-21 18:43:25'),(40,'DataX日志','datax:log','DataX执行日志查看','2026-06-21 18:43:25'),(41,'数据库管理','db:manager','数据库管理功能','2026-06-21 18:43:25'),(42,'流程管理','pipeline:manage','数据流程管理','2026-06-21 18:43:25'),(43,'流程设计','pipeline:design','数据流程设计','2026-06-21 18:43:25'),(44,'流程执行','pipeline:execute','数据流程执行监控','2026-06-21 18:43:25'),(45,'流程日志','pipeline:log','流程执行日志查看','2026-06-21 18:43:25'),(46,'报表查询','report:query','报表查询导出','2026-06-21 18:43:25'),(47,'报表管理','report:manage','报表增删改查','2026-06-21 18:43:25'),(48,'报表设计','report:design','报表设计编辑','2026-06-21 18:43:25'),(49,'图表管理','chart:manage','图表增删改查','2026-06-21 18:43:25'),(50,'图表设计','chart:design','图表设计编辑','2026-06-21 18:43:25'),(51,'页面管理','page:manage','页面增删改查','2026-06-21 18:43:25'),(52,'页面设计','page:design','页面设计编辑','2026-06-21 18:43:25'),(53,'数据视图管理','dataview:manage','数据视图配置管理','2026-06-21 18:43:25'),(54,'表数据管理','tabledata:manage','表数据增删改查','2026-06-21 18:43:25'),(55,'用户管理','user:manage','用户增删改查','2026-06-21 18:43:25'),(56,'角色管理','role:manage','角色增删改查','2026-06-21 18:43:25'),(57,'菜单管理','menu:manage','菜单增删改查','2026-06-21 18:43:25'),(58,'系统监控','system:monitor','系统监控查看','2026-06-21 18:43:25'),(59,'操作日志','log:operation','操作日志查询','2026-06-21 18:43:25'),(60,'公告管理','announcement:manage','公告增删改查','2026-06-21 18:43:25'),(61,'AI助手访问','ai:assistant','允许用户使用AI助手功能','2026-06-21 18:43:25'),(62,'数据血缘','data:lineage','数据血缘关系查看','2026-06-21 18:43:25'),(63,'数据字典','dict:manage','数据字典管理','2026-06-21 18:43:25'),(64,'系统配置','system:config','系统配置管理','2026-06-21 18:43:25'),(65,'部门管理','department:manage','部门增删改查','2026-06-21 18:43:25'),(66,'岗位管理','post:manage','岗位增删改查','2026-06-21 18:43:25'),(67,'即时通讯','chat:conversation','即时通讯功能','2026-06-21 18:43:25'),(68,'数据质量','data:quality','数据质量管理','2026-06-21 18:43:25'),(69,'查询构建器','query:builder','查询构建器功能','2026-06-21 18:43:25'),(70,'数据同步','datasync:manage','数据同步管理','2026-06-21 18:43:25'),(71,'报表版本','report:version','报表版本管理','2026-06-21 18:43:25'),(72,'行级安全','rls:config','行级安全配置','2026-06-21 18:43:25'),(73,'工单管理','ticket:manage','工单增删改查','2026-06-21 18:43:25'),(74,'知识库','ticket:knowledge','知识库管理','2026-06-21 18:43:25'),(75,'使用统计','usage:stats','使用统计查看','2026-06-21 18:43:25'),(76,'在线升级','upgrade:manage','在线升级管理','2026-06-21 18:43:25'),(77,'运维管理','ops:manage','运维工具管理','2026-06-21 18:43:25');
/*!40000 ALTER TABLE `sys_permission` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `route_path`, `component_path`, `icon`, `sort_order`, `is_visible`, `mobile_visible`, `permission_code`, `report_id`, `chart_id`, `page_id`, `data_view_code`, `open_mode`, `badge`, `create_time`, `update_time`) VALUES (1,'仪表盘','Dashboard',0,'menu','/dashboard',NULL,'HomeOutline',1,1,1,NULL,NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(2,'数据采集','DataCollect',0,'directory',NULL,NULL,'CloudDownloadOutline',2,1,1,NULL,NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(3,'数据传输','DataTransfer',0,'directory',NULL,NULL,'SwapHorizontalOutline',3,1,1,NULL,NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(4,'数据流程','DataPipeline',0,'directory',NULL,NULL,'GitNetworkOutline',4,1,1,NULL,NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(5,'报表中心','ReportCenter',0,'directory',NULL,NULL,'DocumentTextOutline',5,1,1,NULL,NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(6,'图表中心','ChartCenter',0,'directory',NULL,NULL,'BarChartOutline',6,1,1,NULL,NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(7,'数据管理','DataManage',0,'directory',NULL,NULL,'FolderOpenOutline',7,1,1,NULL,NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(8,'系统功能','System',0,'directory',NULL,NULL,'SettingsOutline',8,1,1,NULL,NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(9,'系统监控','SystemMonitor',0,'menu','/system-monitor',NULL,'PulseOutline',10,1,1,'system:monitor',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(10,'采集任务','CollectTask',2,'menu','/data-collect',NULL,'ListOutline',1,1,1,'data:collect',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(11,'采集日志','CollectLog',2,'menu','/collect-log',NULL,'DocumentTextOutline',2,1,1,'data:collect:log',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(12,'数据导入','DataImport',2,'menu','/data-import',NULL,'CloudUploadOutline',3,1,1,'data:import',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(20,'传输概览','DataxOverview',3,'menu','/datax',NULL,'GridOutline',1,1,1,'datax:job',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(21,'传输任务','DataxJob',3,'menu','/datax/job',NULL,'SwapHorizontalOutline',2,1,1,'datax:job',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(22,'传输日志','DataxLog',3,'menu','/datax/log',NULL,'DocumentTextOutline',3,1,1,'datax:log',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(30,'流程管理','PipelineManage',4,'menu','/pipeline/manage',NULL,'ListOutline',1,1,1,'pipeline:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(31,'执行监控','PipelineMonitor',4,'menu','/pipeline/monitor',NULL,'EyeOutline',2,1,1,'pipeline:execute',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(32,'执行日志','PipelineLog',4,'menu','/pipeline/log',NULL,'DocumentTextOutline',3,1,1,'pipeline:log',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(40,'报表查询','Report',5,'menu','/report',NULL,'SearchOutline',1,1,1,'report:query',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(41,'报表管理','ReportManage',5,'menu','/report-manage',NULL,'DocumentsOutline',2,1,1,'report:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(42,'报表版本','ReportVersion',5,'menu','/report-version',NULL,'GitCommitOutline',3,1,1,'report:version',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(50,'图表管理','ChartManage',6,'menu','/chart-manage',NULL,'StatsChartOutline',1,1,1,'chart:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(51,'图表查看','ChartCenterView',6,'menu','/chart-center',NULL,'AnalyticsOutline',2,1,1,NULL,NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(52,'页面管理','PageManage',6,'menu','/page-manage',NULL,'GridOutline',3,1,1,'page:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(53,'AI智能图表','AiChartDesign',6,'menu','/ai-chart-design',NULL,'SparklesOutline',4,1,1,'chart:design',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(60,'数据库管理','DatabaseManager',7,'menu','/db-manager',NULL,'ServerOutline',1,1,1,'db:manager',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(61,'数据视图管理','DataViewManage',7,'menu','/data-view-manage',NULL,'EyeOutline',2,1,1,'dataview:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(62,'表数据管理','TableDataManage',7,'menu','/table-data-manage',NULL,'GridOutline',3,1,1,'tabledata:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(63,'数据血缘','DataLineage',7,'menu','/data-lineage',NULL,'GitBranchOutline',4,1,1,'data:lineage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(64,'数据质量','DataQuality',7,'menu','/data-quality',NULL,'CheckmarkCircleOutline',5,1,1,'data:quality',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(65,'查询构建器','QueryBuilder',7,'menu','/query-builder',NULL,'CodeSlashOutline',6,1,1,'query:builder',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(66,'数据同步','DataSync',7,'menu','/data-sync',NULL,'SyncOutline',7,1,1,'datasync:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(67,'数据字典','DataDict',7,'menu','/data-dictionary',NULL,'BookOutline',8,1,1,'dict:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(68,'行级安全','RlsConfig',7,'menu','/rls-config',NULL,'ShieldOutline',9,1,1,'rls:config',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(70,'用户管理','User',8,'menu','/user',NULL,'PersonOutline',1,1,1,'user:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(71,'角色管理','Role',8,'menu','/role',NULL,'PeopleOutline',2,1,1,'role:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(72,'菜单管理','MenuManage',8,'menu','/menu-manage',NULL,'MenuOutline',3,1,1,'menu:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(73,'数据源管理','DataSource',8,'menu','/data-source',NULL,'ServerOutline',4,1,1,'data:source',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(74,'操作日志','OperationLog',8,'menu','/operation-log',NULL,'DocumentTextOutline',5,1,1,'log:operation',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(75,'公告管理','AnnouncementManage',8,'menu','/announcement-manage',NULL,'MegaphoneOutline',6,1,1,'announcement:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(76,'导出中心','ExportCenter',8,'menu','/export-center',NULL,'DownloadOutline',7,1,1,NULL,NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(78,'告警管理','AlertManage',8,'menu','/alert-manage',NULL,'WarningOutline',9,1,1,'system:monitor',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(79,'慢查询分析','SlowQuery',8,'menu','/slow-query',NULL,'TimerOutline',10,1,1,'system:monitor',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(80,'健康检查','HealthCheck',8,'menu','/health-check',NULL,'FitnessOutline',11,1,1,'system:monitor',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(82,'数据字典','DataDictionary',8,'menu','/data-dictionary',NULL,'BookOutline',13,1,1,'dict:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(83,'系统配置','SystemConfig',8,'menu','/system-config',NULL,'SettingsOutline',14,1,1,'system:config',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(84,'部门管理','DepartmentManage',8,'menu','/department-manage',NULL,'BusinessOutline',15,1,1,'department:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(85,'岗位管理','PostManage',8,'menu','/post',NULL,'BriefcaseOutline',16,1,1,'post:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(86,'登录日志','LoginLog',8,'menu','/login-log',NULL,'LogInOutline',17,1,1,'log:operation',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(87,'消息通道','MessageChannel',8,'menu','/message-channel',NULL,'MailOutline',18,1,1,'system:config',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(100,'个人工作台','Workspace',0,'menu','/workspace',NULL,'DesktopOutline',2,1,1,NULL,NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(101,'运维工具','OpsTools',0,'directory',NULL,NULL,'ConstructOutline',9,1,1,NULL,NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(102,'技术支持','TechSupport',0,'directory',NULL,NULL,'HelpCircleOutline',11,1,1,NULL,NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(111,'使用统计','UsageStats',101,'menu','/usage-stats',NULL,'StatsChartOutline',2,1,1,'usage:stats',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(112,'在线升级','UpgradeManage',101,'menu','/upgrade-manage',NULL,'CloudUploadOutline',3,1,1,'upgrade:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(113,'运维管理','OpsManage',101,'menu','/ops-manage',NULL,'HammerOutline',4,1,1,'ops:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(120,'工单管理','TicketManage',102,'menu','/ticket-manage',NULL,'ReceiptOutline',1,1,1,'ticket:manage',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(121,'知识库','KnowledgeBase',102,'menu','/knowledge-base',NULL,'LibraryOutline',2,1,1,'ticket:knowledge',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(130,'即时通讯','Chat',0,'menu','/chat',NULL,'ChatbubbleEllipsesOutline',12,1,1,'chat:conversation',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(131,'AI助手','ai-assistant',8,'permission',NULL,NULL,'sparkles',99,0,1,'ai:assistant',NULL,NULL,NULL,NULL,'tab',NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25');
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `create_time`, `update_time`) VALUES (4,'超级管理员','admin','拥有所有权限','2026-06-21 18:43:25','2026-06-21 18:43:25'),(5,'普通用户','user','普通用户角色','2026-06-21 18:43:25','2026-06-21 18:43:25'),(6,'数据管理员','data_admin','数据管理权限','2026-06-21 18:43:25','2026-06-21 18:43:25');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_role_permission` WRITE;
/*!40000 ALTER TABLE `sys_role_permission` DISABLE KEYS */;
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`) VALUES (26,4,61),(27,4,60),(28,4,50),(29,4,49),(30,4,67),(31,4,36),(32,4,37),(33,4,38),(34,4,62),(35,4,68),(36,4,35),(37,4,70),(38,4,53),(39,4,39),(40,4,40),(41,4,41),(42,4,65),(43,4,63),(44,4,59),(45,4,57),(46,4,77),(47,4,52),(48,4,51),(49,4,43),(50,4,44),(51,4,45),(52,4,42),(53,4,66),(54,4,69),(55,4,48),(56,4,47),(57,4,46),(58,4,71),(59,4,72),(60,4,56),(61,4,64),(62,4,58),(63,4,54),(64,4,74),(65,4,73),(66,4,76),(67,4,75),(68,4,55);
/*!40000 ALTER TABLE `sys_role_permission` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (71,4,1),(72,4,2),(73,4,3),(74,4,4),(75,4,5),(76,4,6),(77,4,7),(78,4,8),(79,4,9),(80,4,10),(81,4,11),(82,4,12),(83,4,20),(84,4,21),(85,4,22),(86,4,30),(87,4,31),(88,4,32),(89,4,40),(90,4,41),(91,4,42),(92,4,50),(93,4,51),(94,4,52),(95,4,53),(96,4,60),(97,4,61),(98,4,62),(99,4,63),(100,4,64),(101,4,65),(102,4,66),(103,4,67),(104,4,68),(105,4,70),(106,4,71),(107,4,72),(108,4,73),(109,4,74),(110,4,75),(111,4,76),(112,4,78),(113,4,79),(114,4,80),(115,4,81),(116,4,82),(117,4,83),(118,4,84),(119,4,85),(120,4,86),(121,4,87),(122,4,100),(123,4,101),(124,4,102),(125,4,111),(126,4,112),(127,4,113),(128,4,120),(129,4,121),(130,4,130);
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `avatar`, `dept_id`, `post_id`, `phone`, `gender`, `must_change_password`, `status`, `create_time`, `update_time`) VALUES (3,'admin','0192023a7bbd73250516f069df18b500','管理员','admin@example.com',NULL,NULL,NULL,NULL,0,0,1,'2026-06-21 18:43:25','2026-06-21 18:43:25');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`) VALUES (3,3,4);
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_dept` WRITE;
/*!40000 ALTER TABLE `sys_dept` DISABLE KEYS */;
INSERT INTO `sys_dept` (`id`, `parent_id`, `ancestors`, `dept_name`, `dept_code`, `leader_id`, `leader_name`, `phone`, `email`, `sort_order`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1,0,'0','总公司','HQ',NULL,NULL,NULL,NULL,1,1,0,NULL,'2026-01-20 22:54:50',NULL,NULL),(2,1,'0,1','技术部','TECH',NULL,NULL,NULL,NULL,1,1,0,NULL,'2026-01-20 22:54:50',NULL,NULL),(3,1,'0,1','人事部','PRODUCT',NULL,NULL,NULL,NULL,2,1,0,NULL,'2026-01-20 22:54:50',NULL,'2026-01-20 23:37:34'),(4,1,'0,1','财务部','OPERATION',NULL,NULL,NULL,NULL,3,1,0,NULL,'2026-01-20 22:54:50',NULL,'2026-01-20 23:37:34'),(5,2,'0,1,2','市场部','FRONTEND',NULL,NULL,NULL,NULL,1,1,0,NULL,'2026-01-20 22:54:50',NULL,'2026-01-20 23:37:34'),(6,2,'0,1,2','研发一组','BACKEND',NULL,NULL,NULL,NULL,2,1,0,NULL,'2026-01-20 22:54:50',NULL,'2026-01-20 23:37:34'),(7,2,'0,1,2','研发二组','QA',NULL,NULL,NULL,NULL,3,1,0,NULL,'2026-01-20 22:54:50',NULL,'2026-01-20 23:37:34'),(8,2,'0,1,2','测试组','TECH-QA',NULL,NULL,NULL,NULL,3,1,0,NULL,'2026-01-20 23:37:34',NULL,NULL);
/*!40000 ALTER TABLE `sys_dept` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_department` WRITE;
/*!40000 ALTER TABLE `sys_department` DISABLE KEYS */;
INSERT INTO `sys_department` (`id`, `dept_name`, `dept_code`, `parent_id`, `ancestors`, `leader`, `phone`, `email`, `sort_order`, `status`, `del_flag`, `create_time`, `update_time`) VALUES (1,'总公司','HQ',0,'0','admin',NULL,NULL,0,1,0,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(2,'技术部','TECH',1,'0,1',NULL,NULL,NULL,1,1,0,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(3,'数据部','DATA',1,'0,1',NULL,NULL,NULL,2,1,0,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(4,'运维部','OPS',1,'0,1',NULL,NULL,NULL,3,1,0,'2026-06-21 18:43:25','2026-06-21 18:43:25');
/*!40000 ALTER TABLE `sys_department` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_post` WRITE;
/*!40000 ALTER TABLE `sys_post` DISABLE KEYS */;
INSERT INTO `sys_post` (`id`, `post_code`, `post_name`, `post_level`, `sort_order`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1,'CEO','董事长',1,1,1,'公司最高管理者',NULL,'2026-01-20 23:37:34',NULL,NULL),(2,'GM','总经理',2,2,1,'公司总经理',NULL,'2026-01-20 23:37:34',NULL,NULL),(3,'DIRECTOR','总监',3,3,1,'部门总监',NULL,'2026-01-20 23:37:34',NULL,NULL),(4,'MANAGER','经理',4,4,1,'部门经理',NULL,'2026-01-20 23:37:34',NULL,NULL),(5,'SUPERVISOR','主管',5,5,1,'团队主管',NULL,'2026-01-20 23:37:34',NULL,NULL),(6,'SENIOR','高级专员',6,6,1,'高级专员/高级工程师',NULL,'2026-01-20 23:37:34',NULL,NULL),(7,'STAFF','专员',7,7,1,'普通专员/工程师',NULL,'2026-01-20 23:37:34',NULL,NULL),(8,'ASSISTANT','助理',8,8,1,'助理',NULL,'2026-01-20 23:37:34',NULL,NULL),(9,'INTERN','实习生',9,9,1,'实习生',NULL,'2026-01-20 23:37:34',NULL,NULL);
/*!40000 ALTER TABLE `sys_post` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_position` WRITE;
/*!40000 ALTER TABLE `sys_position` DISABLE KEYS */;
INSERT INTO `sys_position` (`id`, `position_name`, `position_code`, `position_level`, `sort_order`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1,'总经理','CEO',10,1,1,NULL,NULL,'2026-01-20 22:54:50',NULL,NULL),(2,'副总经理','VP',9,2,1,NULL,NULL,'2026-01-20 22:54:50',NULL,NULL),(3,'总监','DIRECTOR',8,3,1,NULL,NULL,'2026-01-20 22:54:50',NULL,NULL),(4,'经理','MANAGER',7,4,1,NULL,NULL,'2026-01-20 22:54:50',NULL,NULL),(5,'主管','SUPERVISOR',6,5,1,NULL,NULL,'2026-01-20 22:54:50',NULL,NULL),(6,'高级工程师','SENIOR_ENGINEER',5,6,1,NULL,NULL,'2026-01-20 22:54:50',NULL,NULL),(7,'工程师','ENGINEER',4,7,1,NULL,NULL,'2026-01-20 22:54:50',NULL,NULL),(8,'助理工程师','ASSISTANT',3,8,1,NULL,NULL,'2026-01-20 22:54:50',NULL,NULL),(9,'实习生','INTERN',1,9,1,NULL,NULL,'2026-01-20 22:54:50',NULL,NULL);
/*!40000 ALTER TABLE `sys_position` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `data_dictionary` WRITE;
/*!40000 ALTER TABLE `data_dictionary` DISABLE KEYS */;
INSERT INTO `data_dictionary` (`id`, `dict_type`, `dict_code`, `dict_label`, `dict_value`, `sort_order`, `is_default`, `status`, `remark`, `create_time`, `update_time`) VALUES (6,'db_type','mysql','MySQL','mysql',1,1,1,NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(7,'db_type','postgresql','PostgreSQL','postgresql',2,0,1,NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(8,'db_type','oracle','Oracle','oracle',3,0,1,NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(9,'db_type','sqlserver','SQL Server','sqlserver',4,0,1,NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(10,'chart_type','line','折线图','line',1,0,1,NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(11,'chart_type','bar','柱状图','bar',2,0,1,NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(12,'chart_type','pie','饼图','pie',3,0,1,NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(13,'chart_type','area','面积图','area',4,0,1,NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25'),(14,'chart_type','scatter','散点图','scatter',5,0,1,NULL,'2026-06-21 18:43:25','2026-06-21 18:43:25');
/*!40000 ALTER TABLE `data_dictionary` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `masking_rule` WRITE;
/*!40000 ALTER TABLE `masking_rule` DISABLE KEYS */;
INSERT INTO `masking_rule` (`id`, `name`, `data_source_id`, `table_name`, `field_name`, `field_pattern`, `sensitive_type`, `strategy_type`, `strategy_config`, `priority`, `enabled`, `description`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (1,'手机号脱敏',NULL,NULL,NULL,'(?i)(phone|mobile|tel|cellphone)','PHONE','MASK','{\"maskStart\": 3, \"maskEnd\": 4, \"maskChar\": \"*\"}',10,1,'手机号中间4位脱敏，如：138****8888','2026-02-21 18:47:52','2026-02-21 18:47:52',NULL,NULL),(2,'身份证号脱敏',NULL,NULL,NULL,'(?i)(idcard|id_card|identity|sfz)','ID_CARD','MASK','{\"maskStart\": 6, \"maskEnd\": 4, \"maskChar\": \"*\"}',10,1,'身份证号中间部分脱敏','2026-02-21 18:47:52','2026-02-21 18:47:52',NULL,NULL),(3,'银行卡号脱敏',NULL,NULL,NULL,'(?i)(bankcard|bank_card|cardno|card_no)','BANK_CARD','MASK','{\"maskStart\": 4, \"maskEnd\": 4, \"maskChar\": \"*\"}',10,1,'银行卡号中间部分脱敏','2026-02-21 18:47:52','2026-02-21 18:47:52',NULL,NULL),(4,'邮箱脱敏',NULL,NULL,NULL,'(?i)(email|mail)','EMAIL','MASK','{\"maskStart\": 3, \"maskEnd\": 0, \"maskChar\": \"*\", \"preserveDomain\": true}',10,1,'邮箱用户名部分脱敏','2026-02-21 18:47:52','2026-02-21 18:47:52',NULL,NULL),(5,'姓名脱敏',NULL,NULL,NULL,'(?i)(name|username|realname|real_name)','NAME','TRUNCATE','{\"keepLength\": 1, \"suffix\": \"*\"}',10,1,'姓名只显示第一个字','2026-02-21 18:47:52','2026-02-21 18:47:52',NULL,NULL),(6,'地址脱敏',NULL,NULL,NULL,'(?i)(address|addr)','ADDRESS','TRUNCATE','{\"keepLength\": 6, \"suffix\": \"***\"}',10,1,'地址只显示前6个字符','2026-02-21 18:47:52','2026-02-21 18:47:52',NULL,NULL),(7,'手机号脱敏',NULL,NULL,NULL,'(?i)(phone|mobile|tel|cellphone)','PHONE','MASK','{\"maskStart\": 3, \"maskEnd\": 4, \"maskChar\": \"*\"}',10,1,'手机号中间4位脱敏，如：138****8888','2026-02-21 18:56:52','2026-02-21 18:56:52',NULL,NULL),(8,'身份证号脱敏',NULL,NULL,NULL,'(?i)(idcard|id_card|identity|sfz)','ID_CARD','MASK','{\"maskStart\": 6, \"maskEnd\": 4, \"maskChar\": \"*\"}',10,1,'身份证号中间部分脱敏','2026-02-21 18:56:52','2026-02-21 18:56:52',NULL,NULL),(9,'银行卡号脱敏',NULL,NULL,NULL,'(?i)(bankcard|bank_card|cardno|card_no)','BANK_CARD','MASK','{\"maskStart\": 4, \"maskEnd\": 4, \"maskChar\": \"*\"}',10,1,'银行卡号中间部分脱敏','2026-02-21 18:56:52','2026-02-21 18:56:52',NULL,NULL),(10,'邮箱脱敏',NULL,NULL,NULL,'(?i)(email|mail)','EMAIL','MASK','{\"maskStart\": 3, \"maskEnd\": 0, \"maskChar\": \"*\", \"preserveDomain\": true}',10,1,'邮箱用户名部分脱敏','2026-02-21 18:56:52','2026-02-21 18:56:52',NULL,NULL),(11,'姓名脱敏',NULL,NULL,NULL,'(?i)(name|username|realname|real_name)','NAME','TRUNCATE','{\"keepLength\": 1, \"suffix\": \"*\"}',10,1,'姓名只显示第一个字','2026-02-21 18:56:52','2026-02-21 18:56:52',NULL,NULL),(12,'地址脱敏',NULL,NULL,NULL,'(?i)(address|addr)','ADDRESS','TRUNCATE','{\"keepLength\": 6, \"suffix\": \"***\"}',10,1,'地址只显示前6个字符','2026-02-21 18:56:52','2026-02-21 18:56:52',NULL,NULL);
/*!40000 ALTER TABLE `masking_rule` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `report_template` WRITE;
/*!40000 ALTER TABLE `report_template` DISABLE KEYS */;
INSERT INTO `report_template` (`id`, `name`, `category`, `description`, `sql_template`, `fields_config`, `params_config`, `preview_image`, `is_system`, `creator_id`, `use_count`, `status`, `create_time`, `update_time`) VALUES (1,'销售日报表','sales','按日期统计销售数据，包含销售额、订单数、客单价等核心指标','SELECT \n    DATE(order_date) as sale_date,\n    COUNT(*) as order_count,\n    SUM(amount) as total_amount,\n    AVG(amount) as avg_amount,\n    COUNT(DISTINCT customer_id) as customer_count\nFROM orders \nWHERE order_date >= \'${startDate}\' AND order_date <= \'${endDate}\'\n    AND status = \'completed\'\nGROUP BY DATE(order_date)\nORDER BY sale_date DESC','[\n        {\"name\": \"sale_date\", \"label\": \"销售日期\", \"type\": \"date\", \"width\": 120},\n        {\"name\": \"order_count\", \"label\": \"订单数\", \"type\": \"number\", \"width\": 100},\n        {\"name\": \"total_amount\", \"label\": \"销售总额\", \"type\": \"number\", \"format\": \"currency\", \"width\": 120},\n        {\"name\": \"avg_amount\", \"label\": \"客单价\", \"type\": \"number\", \"format\": \"currency\", \"width\": 100},\n        {\"name\": \"customer_count\", \"label\": \"客户数\", \"type\": \"number\", \"width\": 100}\n    ]','[\n        {\"name\": \"startDate\", \"label\": \"开始日期\", \"type\": \"date\", \"required\": true, \"defaultValue\": \"today-30\"},\n        {\"name\": \"endDate\", \"label\": \"结束日期\", \"type\": \"date\", \"required\": true, \"defaultValue\": \"today\"}\n    ]',NULL,1,NULL,0,1,'2026-02-21 18:56:52','2026-02-21 18:56:52'),(2,'销售区域分析','sales','按区域统计销售业绩，支持多维度分析','SELECT \n    region,\n    COUNT(*) as order_count,\n    SUM(amount) as total_amount,\n    SUM(amount) / COUNT(*) as avg_order_amount,\n    COUNT(DISTINCT customer_id) as customer_count,\n    SUM(amount) / COUNT(DISTINCT customer_id) as avg_customer_value\nFROM orders o\nJOIN customers c ON o.customer_id = c.id\nWHERE order_date >= \'${startDate}\' AND order_date <= \'${endDate}\'\nGROUP BY region\nORDER BY total_amount DESC','[\n        {\"name\": \"region\", \"label\": \"区域\", \"type\": \"string\", \"width\": 100},\n        {\"name\": \"order_count\", \"label\": \"订单数\", \"type\": \"number\", \"width\": 100},\n        {\"name\": \"total_amount\", \"label\": \"销售总额\", \"type\": \"number\", \"format\": \"currency\", \"width\": 120},\n        {\"name\": \"avg_order_amount\", \"label\": \"平均订单金额\", \"type\": \"number\", \"format\": \"currency\", \"width\": 120},\n        {\"name\": \"customer_count\", \"label\": \"客户数\", \"type\": \"number\", \"width\": 100},\n        {\"name\": \"avg_customer_value\", \"label\": \"客户平均价值\", \"type\": \"number\", \"format\": \"currency\", \"width\": 120}\n    ]','[\n        {\"name\": \"startDate\", \"label\": \"开始日期\", \"type\": \"date\", \"required\": true},\n        {\"name\": \"endDate\", \"label\": \"结束日期\", \"type\": \"date\", \"required\": true}\n    ]',NULL,1,NULL,0,1,'2026-02-21 18:56:52','2026-02-21 18:56:52'),(3,'收支明细表','finance','按月统计收入和支出明细，计算净利润','SELECT \n    DATE_FORMAT(transaction_date, \'%Y-%m\') as month,\n    SUM(CASE WHEN type = \'income\' THEN amount ELSE 0 END) as income,\n    SUM(CASE WHEN type = \'expense\' THEN amount ELSE 0 END) as expense,\n    SUM(CASE WHEN type = \'income\' THEN amount ELSE -amount END) as net_profit,\n    COUNT(*) as transaction_count\nFROM financial_transactions\nWHERE transaction_date >= \'${startDate}\' AND transaction_date <= \'${endDate}\'\nGROUP BY DATE_FORMAT(transaction_date, \'%Y-%m\')\nORDER BY month DESC','[\n        {\"name\": \"month\", \"label\": \"月份\", \"type\": \"string\", \"width\": 100},\n        {\"name\": \"income\", \"label\": \"收入\", \"type\": \"number\", \"format\": \"currency\", \"width\": 120, \"style\": {\"color\": \"#52c41a\"}},\n        {\"name\": \"expense\", \"label\": \"支出\", \"type\": \"number\", \"format\": \"currency\", \"width\": 120, \"style\": {\"color\": \"#ff4d4f\"}},\n        {\"name\": \"net_profit\", \"label\": \"净利润\", \"type\": \"number\", \"format\": \"currency\", \"width\": 120},\n        {\"name\": \"transaction_count\", \"label\": \"交易笔数\", \"type\": \"number\", \"width\": 100}\n    ]','[\n        {\"name\": \"startDate\", \"label\": \"开始日期\", \"type\": \"date\", \"required\": true},\n        {\"name\": \"endDate\", \"label\": \"结束日期\", \"type\": \"date\", \"required\": true}\n    ]',NULL,1,NULL,0,1,'2026-02-21 18:56:52','2026-02-21 18:56:52'),(4,'应收账款报表','finance','统计应收账款情况，包含账龄分析','SELECT \n    c.name as customer_name,\n    SUM(ar.amount) as total_receivable,\n    SUM(CASE WHEN DATEDIFF(NOW(), ar.due_date) <= 30 THEN ar.amount ELSE 0 END) as within_30_days,\n    SUM(CASE WHEN DATEDIFF(NOW(), ar.due_date) > 30 AND DATEDIFF(NOW(), ar.due_date) <= 60 THEN ar.amount ELSE 0 END) as days_31_60,\n    SUM(CASE WHEN DATEDIFF(NOW(), ar.due_date) > 60 AND DATEDIFF(NOW(), ar.due_date) <= 90 THEN ar.amount ELSE 0 END) as days_61_90,\n    SUM(CASE WHEN DATEDIFF(NOW(), ar.due_date) > 90 THEN ar.amount ELSE 0 END) as over_90_days\nFROM accounts_receivable ar\nJOIN customers c ON ar.customer_id = c.id\nWHERE ar.status = \'pending\'\nGROUP BY c.id, c.name\nORDER BY total_receivable DESC','[\n        {\"name\": \"customer_name\", \"label\": \"客户名称\", \"type\": \"string\", \"width\": 150},\n        {\"name\": \"total_receivable\", \"label\": \"应收总额\", \"type\": \"number\", \"format\": \"currency\", \"width\": 120},\n        {\"name\": \"within_30_days\", \"label\": \"30天内\", \"type\": \"number\", \"format\": \"currency\", \"width\": 100},\n        {\"name\": \"days_31_60\", \"label\": \"31-60天\", \"type\": \"number\", \"format\": \"currency\", \"width\": 100},\n        {\"name\": \"days_61_90\", \"label\": \"61-90天\", \"type\": \"number\", \"format\": \"currency\", \"width\": 100},\n        {\"name\": \"over_90_days\", \"label\": \"90天以上\", \"type\": \"number\", \"format\": \"currency\", \"width\": 100, \"style\": {\"color\": \"#ff4d4f\"}}\n    ]','[]',NULL,1,NULL,0,1,'2026-02-21 18:56:52','2026-02-21 18:56:52'),(5,'用户活跃度分析','operation','分析用户活跃情况，包含DAU、MAU等指标','SELECT \n    DATE(login_time) as date,\n    COUNT(DISTINCT user_id) as dau,\n    COUNT(*) as login_count,\n    AVG(session_duration) as avg_session_duration,\n    SUM(page_views) as total_page_views\nFROM user_activities\nWHERE login_time >= \'${startDate}\' AND login_time <= \'${endDate}\'\nGROUP BY DATE(login_time)\nORDER BY date DESC','[\n        {\"name\": \"date\", \"label\": \"日期\", \"type\": \"date\", \"width\": 120},\n        {\"name\": \"dau\", \"label\": \"日活用户\", \"type\": \"number\", \"width\": 100},\n        {\"name\": \"login_count\", \"label\": \"登录次数\", \"type\": \"number\", \"width\": 100},\n        {\"name\": \"avg_session_duration\", \"label\": \"平均会话时长(秒)\", \"type\": \"number\", \"width\": 140},\n        {\"name\": \"total_page_views\", \"label\": \"页面浏览量\", \"type\": \"number\", \"width\": 120}\n    ]','[\n        {\"name\": \"startDate\", \"label\": \"开始日期\", \"type\": \"date\", \"required\": true},\n        {\"name\": \"endDate\", \"label\": \"结束日期\", \"type\": \"date\", \"required\": true}\n    ]',NULL,1,NULL,0,1,'2026-02-21 18:56:52','2026-02-21 18:56:52'),(6,'订单转化漏斗','operation','分析订单转化各环节数据','SELECT \n    \'浏览商品\' as stage, COUNT(DISTINCT user_id) as user_count, 100.0 as conversion_rate\nFROM product_views WHERE view_time >= \'${startDate}\' AND view_time <= \'${endDate}\'\nUNION ALL\nSELECT \n    \'加入购物车\' as stage, COUNT(DISTINCT user_id) as user_count, \n    COUNT(DISTINCT user_id) * 100.0 / (SELECT COUNT(DISTINCT user_id) FROM product_views WHERE view_time >= \'${startDate}\' AND view_time <= \'${endDate}\') as conversion_rate\nFROM cart_items WHERE add_time >= \'${startDate}\' AND add_time <= \'${endDate}\'\nUNION ALL\nSELECT \n    \'提交订单\' as stage, COUNT(DISTINCT user_id) as user_count,\n    COUNT(DISTINCT user_id) * 100.0 / (SELECT COUNT(DISTINCT user_id) FROM product_views WHERE view_time >= \'${startDate}\' AND view_time <= \'${endDate}\') as conversion_rate\nFROM orders WHERE order_date >= \'${startDate}\' AND order_date <= \'${endDate}\'\nUNION ALL\nSELECT \n    \'完成支付\' as stage, COUNT(DISTINCT user_id) as user_count,\n    COUNT(DISTINCT user_id) * 100.0 / (SELECT COUNT(DISTINCT user_id) FROM product_views WHERE view_time >= \'${startDate}\' AND view_time <= \'${endDate}\') as conversion_rate\nFROM orders WHERE order_date >= \'${startDate}\' AND order_date <= \'${endDate}\' AND status = \'paid\'','[\n        {\"name\": \"stage\", \"label\": \"转化阶段\", \"type\": \"string\", \"width\": 120},\n        {\"name\": \"user_count\", \"label\": \"用户数\", \"type\": \"number\", \"width\": 100},\n        {\"name\": \"conversion_rate\", \"label\": \"转化率(%)\", \"type\": \"number\", \"format\": \"percent\", \"width\": 100}\n    ]','[\n        {\"name\": \"startDate\", \"label\": \"开始日期\", \"type\": \"date\", \"required\": true},\n        {\"name\": \"endDate\", \"label\": \"结束日期\", \"type\": \"date\", \"required\": true}\n    ]',NULL,1,NULL,0,1,'2026-02-21 18:56:52','2026-02-21 18:56:52'),(7,'库存盘点表','inventory','当前库存状态汇总，包含库存预警','SELECT \n    p.product_code,\n    p.product_name,\n    p.category,\n    i.quantity as current_stock,\n    i.safety_stock,\n    CASE \n        WHEN i.quantity <= 0 THEN \'缺货\'\n        WHEN i.quantity < i.safety_stock * 0.5 THEN \'严重不足\'\n        WHEN i.quantity < i.safety_stock THEN \'库存预警\'\n        ELSE \'正常\'\n    END as stock_status,\n    i.unit_cost,\n    i.quantity * i.unit_cost as stock_value\nFROM inventory i\nJOIN products p ON i.product_id = p.id\nWHERE (\'${category}\' = \'\' OR p.category = \'${category}\')\nORDER BY \n    CASE \n        WHEN i.quantity <= 0 THEN 1\n        WHEN i.quantity < i.safety_stock * 0.5 THEN 2\n        WHEN i.quantity < i.safety_stock THEN 3\n        ELSE 4\n    END,\n    p.product_name','[\n        {\"name\": \"product_code\", \"label\": \"商品编码\", \"type\": \"string\", \"width\": 120},\n        {\"name\": \"product_name\", \"label\": \"商品名称\", \"type\": \"string\", \"width\": 150},\n        {\"name\": \"category\", \"label\": \"分类\", \"type\": \"string\", \"width\": 100},\n        {\"name\": \"current_stock\", \"label\": \"当前库存\", \"type\": \"number\", \"width\": 100},\n        {\"name\": \"safety_stock\", \"label\": \"安全库存\", \"type\": \"number\", \"width\": 100},\n        {\"name\": \"stock_status\", \"label\": \"库存状态\", \"type\": \"string\", \"width\": 100},\n        {\"name\": \"unit_cost\", \"label\": \"单位成本\", \"type\": \"number\", \"format\": \"currency\", \"width\": 100},\n        {\"name\": \"stock_value\", \"label\": \"库存价值\", \"type\": \"number\", \"format\": \"currency\", \"width\": 120}\n    ]','[\n        {\"name\": \"category\", \"label\": \"商品分类\", \"type\": \"string\", \"required\": false, \"defaultValue\": \"\"}\n    ]',NULL,1,NULL,0,1,'2026-02-21 18:56:52','2026-02-21 18:56:52'),(8,'出入库明细','inventory','库存变动明细记录','SELECT \n    DATE(m.movement_date) as date,\n    p.product_code,\n    p.product_name,\n    m.movement_type,\n    m.quantity,\n    m.reference_no,\n    m.remark\nFROM inventory_movements m\nJOIN products p ON m.product_id = p.id\nWHERE m.movement_date >= \'${startDate}\' AND m.movement_date <= \'${endDate}\'\n    AND (\'${movementType}\' = \'\' OR m.movement_type = \'${movementType}\')\nORDER BY m.movement_date DESC, m.id DESC','[\n        {\"name\": \"date\", \"label\": \"日期\", \"type\": \"date\", \"width\": 120},\n        {\"name\": \"product_code\", \"label\": \"商品编码\", \"type\": \"string\", \"width\": 120},\n        {\"name\": \"product_name\", \"label\": \"商品名称\", \"type\": \"string\", \"width\": 150},\n        {\"name\": \"movement_type\", \"label\": \"变动类型\", \"type\": \"string\", \"width\": 100},\n        {\"name\": \"quantity\", \"label\": \"数量\", \"type\": \"number\", \"width\": 100},\n        {\"name\": \"reference_no\", \"label\": \"关联单号\", \"type\": \"string\", \"width\": 150},\n        {\"name\": \"remark\", \"label\": \"备注\", \"type\": \"string\", \"width\": 200}\n    ]','[\n        {\"name\": \"startDate\", \"label\": \"开始日期\", \"type\": \"date\", \"required\": true},\n        {\"name\": \"endDate\", \"label\": \"结束日期\", \"type\": \"date\", \"required\": true},\n        {\"name\": \"movementType\", \"label\": \"变动类型\", \"type\": \"select\", \"required\": false, \"options\": [{\"value\": \"\", \"label\": \"全部\"}, {\"value\": \"in\", \"label\": \"入库\"}, {\"value\": \"out\", \"label\": \"出库\"}]}\n    ]',NULL,1,NULL,0,1,'2026-02-21 18:56:52','2026-02-21 18:56:52'),(9,'员工花名册','hr','员工基本信息汇总','SELECT \n    e.employee_no,\n    e.name,\n    d.department_name,\n    e.position,\n    e.entry_date,\n    TIMESTAMPDIFF(MONTH, e.entry_date, NOW()) as tenure_months,\n    e.status\nFROM employees e\nJOIN departments d ON e.department_id = d.id\nWHERE (${departmentId} = 0 OR e.department_id = ${departmentId})\n    AND (\'${status}\' = \'\' OR e.status = \'${status}\')\nORDER BY d.department_name, e.employee_no','[\n        {\"name\": \"employee_no\", \"label\": \"工号\", \"type\": \"string\", \"width\": 100},\n        {\"name\": \"name\", \"label\": \"姓名\", \"type\": \"string\", \"width\": 100},\n        {\"name\": \"department_name\", \"label\": \"部门\", \"type\": \"string\", \"width\": 120},\n        {\"name\": \"position\", \"label\": \"职位\", \"type\": \"string\", \"width\": 120},\n        {\"name\": \"entry_date\", \"label\": \"入职日期\", \"type\": \"date\", \"width\": 120},\n        {\"name\": \"tenure_months\", \"label\": \"在职月数\", \"type\": \"number\", \"width\": 100},\n        {\"name\": \"status\", \"label\": \"状态\", \"type\": \"string\", \"width\": 80}\n    ]','[\n        {\"name\": \"departmentId\", \"label\": \"部门\", \"type\": \"number\", \"required\": false, \"defaultValue\": 0},\n        {\"name\": \"status\", \"label\": \"状态\", \"type\": \"select\", \"required\": false, \"options\": [{\"value\": \"\", \"label\": \"全部\"}, {\"value\": \"active\", \"label\": \"在职\"}, {\"value\": \"resigned\", \"label\": \"离职\"}]}\n    ]',NULL,1,NULL,0,1,'2026-02-21 18:56:52','2026-02-21 18:56:52'),(10,'考勤统计表','hr','员工考勤数据统计','SELECT \n    e.employee_no,\n    e.name,\n    d.department_name,\n    COUNT(CASE WHEN a.status = \'normal\' THEN 1 END) as normal_days,\n    COUNT(CASE WHEN a.status = \'late\' THEN 1 END) as late_days,\n    COUNT(CASE WHEN a.status = \'early_leave\' THEN 1 END) as early_leave_days,\n    COUNT(CASE WHEN a.status = \'absent\' THEN 1 END) as absent_days,\n    SUM(a.overtime_hours) as total_overtime_hours\nFROM employees e\nJOIN departments d ON e.department_id = d.id\nLEFT JOIN attendance a ON e.id = a.employee_id \n    AND a.attendance_date >= \'${startDate}\' AND a.attendance_date <= \'${endDate}\'\nWHERE (${departmentId} = 0 OR e.department_id = ${departmentId})\nGROUP BY e.id, e.employee_no, e.name, d.department_name\nORDER BY d.department_name, e.employee_no','[\n        {\"name\": \"employee_no\", \"label\": \"工号\", \"type\": \"string\", \"width\": 100},\n        {\"name\": \"name\", \"label\": \"姓名\", \"type\": \"string\", \"width\": 100},\n        {\"name\": \"department_name\", \"label\": \"部门\", \"type\": \"string\", \"width\": 120},\n        {\"name\": \"normal_days\", \"label\": \"正常出勤\", \"type\": \"number\", \"width\": 100},\n        {\"name\": \"late_days\", \"label\": \"迟到\", \"type\": \"number\", \"width\": 80},\n        {\"name\": \"early_leave_days\", \"label\": \"早退\", \"type\": \"number\", \"width\": 80},\n        {\"name\": \"absent_days\", \"label\": \"缺勤\", \"type\": \"number\", \"width\": 80},\n        {\"name\": \"total_overtime_hours\", \"label\": \"加班时长\", \"type\": \"number\", \"width\": 100}\n    ]','[\n        {\"name\": \"startDate\", \"label\": \"开始日期\", \"type\": \"date\", \"required\": true},\n        {\"name\": \"endDate\", \"label\": \"结束日期\", \"type\": \"date\", \"required\": true},\n        {\"name\": \"departmentId\", \"label\": \"部门\", \"type\": \"number\", \"required\": false, \"defaultValue\": 0}\n    ]',NULL,1,NULL,0,1,'2026-02-21 18:56:52','2026-02-21 18:56:52');
/*!40000 ALTER TABLE `report_template` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `style_template` WRITE;
/*!40000 ALTER TABLE `style_template` DISABLE KEYS */;
INSERT INTO `style_template` (`id`, `name`, `category`, `description`, `is_system`, `column_styles`, `conditional_rules`, `table_style`, `preview_image`, `created_by`, `use_count`, `status`, `create_time`, `update_time`) VALUES (1,'财务报表标准样式','finance','适用于财务报表，负数显示为红色括号格式，数值保留两位小数',1,'{\"amount|money|price|cost|revenue|profit\":{\"fieldPattern\":\"amount|money|price|cost|revenue|profit\",\"fieldType\":\"number\",\"defaultStyle\":{\"font\":{\"family\":\"Arial\"},\"alignment\":{\"horizontal\":\"right\"},\"format\":{\"type\":\"number\",\"config\":{\"decimalPlaces\":2,\"useThousandsSeparator\":true,\"negativeFormat\":\"redParentheses\"}}}}}','[{\"id\":\"negative-red\",\"name\":\"负数红色\",\"priority\":1,\"enabled\":true,\"condition\":{\"type\":\"value\",\"config\":{\"operator\":\"lt\",\"value\":0}},\"style\":{\"font\":{\"color\":\"#ff4d4f\"}}}]','{\"headerStyle\":{\"font\":{\"weight\":\"bold\",\"color\":\"#ffffff\"},\"background\":{\"type\":\"solid\",\"color\":\"#1890ff\"},\"alignment\":{\"horizontal\":\"center\"}},\"bodyStyle\":{\"font\":{\"size\":12},\"border\":{\"all\":{\"style\":\"solid\",\"width\":1,\"color\":\"#e8e8e8\"}}},\"alternateRowStyle\":{\"background\":{\"type\":\"solid\",\"color\":\"#fafafa\"}},\"borderStyle\":\"all\"}',NULL,NULL,0,1,'2026-02-21 18:47:52','2026-02-21 18:47:52'),(2,'销售仪表盘样式','sales','适用于销售报表，包含目标达成率色阶和同比环比箭头指示',1,'{\"rate|ratio|percent\":{\"fieldPattern\":\"rate|ratio|percent\",\"fieldType\":\"number\",\"defaultStyle\":{\"alignment\":{\"horizontal\":\"center\"},\"format\":{\"type\":\"number\",\"config\":{\"asPercentage\":true,\"decimalPlaces\":1}}}}}','[{\"id\":\"achievement-high\",\"name\":\"达成率高\",\"priority\":1,\"enabled\":true,\"condition\":{\"type\":\"value\",\"config\":{\"operator\":\"gte\",\"value\":1}},\"style\":{\"font\":{\"color\":\"#52c41a\"},\"background\":{\"type\":\"solid\",\"color\":\"#f6ffed\"}}},{\"id\":\"achievement-medium\",\"name\":\"达成率中\",\"priority\":2,\"enabled\":true,\"condition\":{\"type\":\"value\",\"config\":{\"operator\":\"gte\",\"value\":0.7}},\"style\":{\"font\":{\"color\":\"#faad14\"},\"background\":{\"type\":\"solid\",\"color\":\"#fffbe6\"}}},{\"id\":\"achievement-low\",\"name\":\"达成率低\",\"priority\":3,\"enabled\":true,\"condition\":{\"type\":\"value\",\"config\":{\"operator\":\"lt\",\"value\":0.7}},\"style\":{\"font\":{\"color\":\"#ff4d4f\"},\"background\":{\"type\":\"solid\",\"color\":\"#fff2f0\"}}}]','{\"headerStyle\":{\"font\":{\"weight\":\"bold\"},\"background\":{\"type\":\"solid\",\"color\":\"#f0f5ff\"}},\"bodyStyle\":{},\"borderStyle\":\"horizontal\"}',NULL,NULL,0,1,'2026-02-21 18:47:52','2026-02-21 18:47:52'),(3,'库存预警样式','inventory','适用于库存报表，包含库存预警红黄绿灯指示',1,'{\"stock|quantity|inventory\":{\"fieldPattern\":\"stock|quantity|inventory\",\"fieldType\":\"number\",\"defaultStyle\":{\"alignment\":{\"horizontal\":\"right\"},\"format\":{\"type\":\"number\",\"config\":{\"decimalPlaces\":0,\"useThousandsSeparator\":true}}}}}','[{\"id\":\"stock-danger\",\"name\":\"库存危险\",\"priority\":1,\"enabled\":true,\"condition\":{\"type\":\"formula\",\"config\":{\"expression\":\"${stock} < ${safetyStock} * 0.5\"}},\"style\":{\"background\":{\"type\":\"solid\",\"color\":\"#ff4d4f\"},\"font\":{\"color\":\"#ffffff\"}}},{\"id\":\"stock-warning\",\"name\":\"库存预警\",\"priority\":2,\"enabled\":true,\"condition\":{\"type\":\"formula\",\"config\":{\"expression\":\"${stock} < ${safetyStock}\"}},\"style\":{\"background\":{\"type\":\"solid\",\"color\":\"#faad14\"}}},{\"id\":\"stock-normal\",\"name\":\"库存正常\",\"priority\":3,\"enabled\":true,\"condition\":{\"type\":\"formula\",\"config\":{\"expression\":\"${stock} >= ${safetyStock}\"}},\"style\":{\"background\":{\"type\":\"solid\",\"color\":\"#52c41a\"},\"font\":{\"color\":\"#ffffff\"}}}]','{\"headerStyle\":{\"font\":{\"weight\":\"bold\"},\"background\":{\"type\":\"solid\",\"color\":\"#e6f7ff\"}},\"bodyStyle\":{},\"borderStyle\":\"all\"}',NULL,NULL,0,1,'2026-02-21 18:47:52','2026-02-21 18:47:52'),(4,'KPI仪表盘样式','kpi','适用于KPI仪表盘，包含进度条和星级评分样式',1,'{\"score|rating\":{\"fieldPattern\":\"score|rating\",\"fieldType\":\"number\",\"defaultStyle\":{\"alignment\":{\"horizontal\":\"center\"}}},\"progress|completion\":{\"fieldPattern\":\"progress|completion\",\"fieldType\":\"number\",\"defaultStyle\":{\"alignment\":{\"horizontal\":\"center\"},\"format\":{\"type\":\"number\",\"config\":{\"asPercentage\":true,\"decimalPlaces\":0}}}}}','[{\"id\":\"kpi-excellent\",\"name\":\"优秀\",\"priority\":1,\"enabled\":true,\"condition\":{\"type\":\"value\",\"config\":{\"operator\":\"gte\",\"value\":0.9}},\"style\":{\"font\":{\"color\":\"#52c41a\",\"weight\":\"bold\"}}},{\"id\":\"kpi-good\",\"name\":\"良好\",\"priority\":2,\"enabled\":true,\"condition\":{\"type\":\"value\",\"config\":{\"operator\":\"gte\",\"value\":0.7}},\"style\":{\"font\":{\"color\":\"#1890ff\"}}},{\"id\":\"kpi-warning\",\"name\":\"警告\",\"priority\":3,\"enabled\":true,\"condition\":{\"type\":\"value\",\"config\":{\"operator\":\"gte\",\"value\":0.5}},\"style\":{\"font\":{\"color\":\"#faad14\"}}},{\"id\":\"kpi-danger\",\"name\":\"危险\",\"priority\":4,\"enabled\":true,\"condition\":{\"type\":\"value\",\"config\":{\"operator\":\"lt\",\"value\":0.5}},\"style\":{\"font\":{\"color\":\"#ff4d4f\"}}}]','{\"headerStyle\":{\"font\":{\"weight\":\"bold\",\"size\":14},\"background\":{\"type\":\"solid\",\"color\":\"#f5f5f5\"},\"alignment\":{\"horizontal\":\"center\"}},\"bodyStyle\":{\"font\":{\"size\":13}},\"borderStyle\":\"horizontal\",\"borderColor\":\"#e8e8e8\"}',NULL,NULL,0,1,'2026-02-21 18:47:52','2026-02-21 18:47:52'),(5,'财务报表标准样式','finance','适用于财务报表，负数显示为红色括号格式，数值保留两位小数',1,'{\"amount|money|price|cost|revenue|profit\":{\"fieldPattern\":\"amount|money|price|cost|revenue|profit\",\"fieldType\":\"number\",\"defaultStyle\":{\"font\":{\"family\":\"Arial\"},\"alignment\":{\"horizontal\":\"right\"},\"format\":{\"type\":\"number\",\"config\":{\"decimalPlaces\":2,\"useThousandsSeparator\":true,\"negativeFormat\":\"redParentheses\"}}}}}','[{\"id\":\"negative-red\",\"name\":\"负数红色\",\"priority\":1,\"enabled\":true,\"condition\":{\"type\":\"value\",\"config\":{\"operator\":\"lt\",\"value\":0}},\"style\":{\"font\":{\"color\":\"#ff4d4f\"}}}]','{\"headerStyle\":{\"font\":{\"weight\":\"bold\",\"color\":\"#ffffff\"},\"background\":{\"type\":\"solid\",\"color\":\"#1890ff\"},\"alignment\":{\"horizontal\":\"center\"}},\"bodyStyle\":{\"font\":{\"size\":12},\"border\":{\"all\":{\"style\":\"solid\",\"width\":1,\"color\":\"#e8e8e8\"}}},\"alternateRowStyle\":{\"background\":{\"type\":\"solid\",\"color\":\"#fafafa\"}},\"borderStyle\":\"all\"}',NULL,NULL,0,1,'2026-02-21 18:56:52','2026-02-21 18:56:52'),(6,'销售仪表盘样式','sales','适用于销售报表，包含目标达成率色阶和同比环比箭头指示',1,'{\"rate|ratio|percent\":{\"fieldPattern\":\"rate|ratio|percent\",\"fieldType\":\"number\",\"defaultStyle\":{\"alignment\":{\"horizontal\":\"center\"},\"format\":{\"type\":\"number\",\"config\":{\"asPercentage\":true,\"decimalPlaces\":1}}}}}','[{\"id\":\"achievement-high\",\"name\":\"达成率高\",\"priority\":1,\"enabled\":true,\"condition\":{\"type\":\"value\",\"config\":{\"operator\":\"gte\",\"value\":1}},\"style\":{\"font\":{\"color\":\"#52c41a\"},\"background\":{\"type\":\"solid\",\"color\":\"#f6ffed\"}}},{\"id\":\"achievement-medium\",\"name\":\"达成率中\",\"priority\":2,\"enabled\":true,\"condition\":{\"type\":\"value\",\"config\":{\"operator\":\"gte\",\"value\":0.7}},\"style\":{\"font\":{\"color\":\"#faad14\"},\"background\":{\"type\":\"solid\",\"color\":\"#fffbe6\"}}},{\"id\":\"achievement-low\",\"name\":\"达成率低\",\"priority\":3,\"enabled\":true,\"condition\":{\"type\":\"value\",\"config\":{\"operator\":\"lt\",\"value\":0.7}},\"style\":{\"font\":{\"color\":\"#ff4d4f\"},\"background\":{\"type\":\"solid\",\"color\":\"#fff2f0\"}}}]','{\"headerStyle\":{\"font\":{\"weight\":\"bold\"},\"background\":{\"type\":\"solid\",\"color\":\"#f0f5ff\"}},\"bodyStyle\":{},\"borderStyle\":\"horizontal\"}',NULL,NULL,0,1,'2026-02-21 18:56:52','2026-02-21 18:56:52'),(7,'库存预警样式','inventory','适用于库存报表，包含库存预警红黄绿灯指示',1,'{\"stock|quantity|inventory\":{\"fieldPattern\":\"stock|quantity|inventory\",\"fieldType\":\"number\",\"defaultStyle\":{\"alignment\":{\"horizontal\":\"right\"},\"format\":{\"type\":\"number\",\"config\":{\"decimalPlaces\":0,\"useThousandsSeparator\":true}}}}}','[{\"id\":\"stock-danger\",\"name\":\"库存危险\",\"priority\":1,\"enabled\":true,\"condition\":{\"type\":\"formula\",\"config\":{\"expression\":\"${stock} < ${safetyStock} * 0.5\"}},\"style\":{\"background\":{\"type\":\"solid\",\"color\":\"#ff4d4f\"},\"font\":{\"color\":\"#ffffff\"}}},{\"id\":\"stock-warning\",\"name\":\"库存预警\",\"priority\":2,\"enabled\":true,\"condition\":{\"type\":\"formula\",\"config\":{\"expression\":\"${stock} < ${safetyStock}\"}},\"style\":{\"background\":{\"type\":\"solid\",\"color\":\"#faad14\"}}},{\"id\":\"stock-normal\",\"name\":\"库存正常\",\"priority\":3,\"enabled\":true,\"condition\":{\"type\":\"formula\",\"config\":{\"expression\":\"${stock} >= ${safetyStock}\"}},\"style\":{\"background\":{\"type\":\"solid\",\"color\":\"#52c41a\"},\"font\":{\"color\":\"#ffffff\"}}}]','{\"headerStyle\":{\"font\":{\"weight\":\"bold\"},\"background\":{\"type\":\"solid\",\"color\":\"#e6f7ff\"}},\"bodyStyle\":{},\"borderStyle\":\"all\"}',NULL,NULL,0,1,'2026-02-21 18:56:52','2026-02-21 18:56:52'),(8,'KPI仪表盘样式','kpi','适用于KPI仪表盘，包含进度条和星级评分样式',1,'{\"score|rating\":{\"fieldPattern\":\"score|rating\",\"fieldType\":\"number\",\"defaultStyle\":{\"alignment\":{\"horizontal\":\"center\"}}},\"progress|completion\":{\"fieldPattern\":\"progress|completion\",\"fieldType\":\"number\",\"defaultStyle\":{\"alignment\":{\"horizontal\":\"center\"},\"format\":{\"type\":\"number\",\"config\":{\"asPercentage\":true,\"decimalPlaces\":0}}}}}','[{\"id\":\"kpi-excellent\",\"name\":\"优秀\",\"priority\":1,\"enabled\":true,\"condition\":{\"type\":\"value\",\"config\":{\"operator\":\"gte\",\"value\":0.9}},\"style\":{\"font\":{\"color\":\"#52c41a\",\"weight\":\"bold\"}}},{\"id\":\"kpi-good\",\"name\":\"良好\",\"priority\":2,\"enabled\":true,\"condition\":{\"type\":\"value\",\"config\":{\"operator\":\"gte\",\"value\":0.7}},\"style\":{\"font\":{\"color\":\"#1890ff\"}}},{\"id\":\"kpi-warning\",\"name\":\"警告\",\"priority\":3,\"enabled\":true,\"condition\":{\"type\":\"value\",\"config\":{\"operator\":\"gte\",\"value\":0.5}},\"style\":{\"font\":{\"color\":\"#faad14\"}}},{\"id\":\"kpi-danger\",\"name\":\"危险\",\"priority\":4,\"enabled\":true,\"condition\":{\"type\":\"value\",\"config\":{\"operator\":\"lt\",\"value\":0.5}},\"style\":{\"font\":{\"color\":\"#ff4d4f\"}}}]','{\"headerStyle\":{\"font\":{\"weight\":\"bold\",\"size\":14},\"background\":{\"type\":\"solid\",\"color\":\"#f5f5f5\"},\"alignment\":{\"horizontal\":\"center\"}},\"bodyStyle\":{\"font\":{\"size\":13}},\"borderStyle\":\"horizontal\",\"borderColor\":\"#e8e8e8\"}',NULL,NULL,0,1,'2026-02-21 18:56:52','2026-02-21 18:56:52');
/*!40000 ALTER TABLE `style_template` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `subscription_plan` WRITE;
/*!40000 ALTER TABLE `subscription_plan` DISABLE KEYS */;
INSERT INTO `subscription_plan` (`id`, `plan_name`, `plan_code`, `description`, `plan_type`, `monthly_price`, `yearly_price`, `max_users`, `max_data_sources`, `max_storage_mb`, `features`, `quota_config`, `sort_order`, `enabled`, `create_time`, `update_time`) VALUES (1,'免费版','free',NULL,'FREE',0.00,0.00,5,2,1024,NULL,NULL,1,1,'2026-02-21 18:56:52','2026-02-21 18:56:52'),(2,'基础版','basic',NULL,'BASIC',99.00,999.00,20,5,5120,NULL,NULL,2,1,'2026-02-21 18:56:52','2026-02-21 18:56:52'),(3,'专业版','pro',NULL,'PRO',299.00,2999.00,50,20,20480,NULL,NULL,3,1,'2026-02-21 18:56:52','2026-02-21 18:56:52'),(4,'企业版','enterprise',NULL,'ENTERPRISE',999.00,9999.00,0,0,0,NULL,NULL,4,1,'2026-02-21 18:56:52','2026-02-21 18:56:52');
/*!40000 ALTER TABLE `subscription_plan` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `tenant` WRITE;
/*!40000 ALTER TABLE `tenant` DISABLE KEYS */;
INSERT INTO `tenant` (`id`, `tenant_code`, `tenant_name`, `domain`, `logo_url`, `status`, `isolation_mode`, `database_config`, `admin_user_id`, `contact_name`, `contact_phone`, `contact_email`, `trial_start_time`, `trial_end_time`, `subscription_plan_id`, `subscription_start_time`, `subscription_end_time`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`, `deleted`) VALUES (1,'default','默认租户',NULL,NULL,1,'SHARED',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'系统默认租户','2026-02-21 18:56:52','2026-02-21 18:56:52',NULL,NULL,0);
/*!40000 ALTER TABLE `tenant` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_notification_template` WRITE;
/*!40000 ALTER TABLE `sys_notification_template` DISABLE KEYS */;
INSERT INTO `sys_notification_template` (`id`, `template_code`, `template_name`, `channel`, `notification_type`, `subject`, `content`, `variables`, `is_enabled`, `create_time`, `update_time`) VALUES (1,'alert_default','默认告警模板','all','alert','[${alertLevel}] ${ruleName}','告警规则: ${ruleName}\n告警级别: ${alertLevel}\n指标名称: ${metricName}\n当前值: ${currentValue}\n阈值: ${threshold}\n时间: ${timestamp}','[\"alertLevel\",\"ruleName\",\"metricName\",\"currentValue\",\"threshold\",\"timestamp\"]',1,'2026-02-21 18:43:05','2026-02-21 18:43:05'),(2,'export_complete','导出完成模板','all','export','导出完成: ${taskName}','您的导出任务\"${taskName}\"已完成，共${totalRows}行数据，可前往导出中心下载。','[\"taskName\",\"totalRows\"]',1,'2026-02-21 18:43:05','2026-02-21 18:43:05'),(3,'task_failure','任务失败模板','all','task','任务失败: ${taskName}','任务\"${taskName}\"执行失败。\n错误信息: ${errorMessage}\n时间: ${timestamp}','[\"taskName\",\"errorMessage\",\"timestamp\"]',1,'2026-02-21 18:43:05','2026-02-21 18:43:05');
/*!40000 ALTER TABLE `sys_notification_template` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `wf_process_definition` WRITE;
/*!40000 ALTER TABLE `wf_process_definition` DISABLE KEYS */;
INSERT INTO `wf_process_definition` (`id`, `process_key`, `process_name`, `category`, `icon`, `description`, `process_config`, `form_config`, `version`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1,'leave_apply','请假申请','hr','CalendarOutline','员工请假申请流程',NULL,NULL,1,1,NULL,'2026-01-20 22:54:50',NULL,NULL),(2,'expense_claim','费用报销','finance','CashOutline','费用报销申请流程',NULL,NULL,1,1,NULL,'2026-01-20 22:54:50',NULL,NULL),(3,'purchase_apply','采购申请','purchase','CartOutline','物资采购申请流程',NULL,NULL,1,1,NULL,'2026-01-20 22:54:50',NULL,NULL),(4,'overtime_apply','加班申请','hr','TimeOutline','加班申请流程，需经直属领导审批',NULL,NULL,1,1,NULL,'2026-01-20 23:37:34',NULL,NULL);
/*!40000 ALTER TABLE `wf_process_definition` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `wf_approval_rule` WRITE;
/*!40000 ALTER TABLE `wf_approval_rule` DISABLE KEYS */;
INSERT INTO `wf_approval_rule` (`id`, `process_id`, `node_id`, `node_name`, `rule_type`, `rule_value`, `level_offset`, `is_skip_same`, `is_countersign`, `sort_order`, `create_time`, `update_time`) VALUES (1,1,'node_leader','??????','INITIATOR_LEADER',NULL,1,0,0,1,'2026-06-21 19:24:51',NULL),(2,1,'node_manager','??????','POST_LEVEL','4',1,0,0,2,'2026-06-21 19:24:51',NULL),(3,2,'node_leader','??????','INITIATOR_LEADER',NULL,1,0,0,1,'2026-06-21 19:24:51',NULL),(4,2,'node_finance','????','ROLE','6',1,0,0,2,'2026-06-21 19:24:51',NULL),(5,3,'node_manager','??????','POST_LEVEL','4',1,0,0,1,'2026-06-21 19:24:51',NULL),(6,3,'node_director','????','POST_LEVEL','3',1,0,0,2,'2026-06-21 19:24:51',NULL),(7,4,'node_leader','??????','INITIATOR_LEADER',NULL,1,0,0,1,'2026-06-21 19:24:51',NULL);
/*!40000 ALTER TABLE `wf_approval_rule` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


