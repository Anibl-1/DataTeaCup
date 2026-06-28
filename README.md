# DataTeaCup

DataTeaCup（中文名：数据茶杯）是一套面向企业数据团队的开源数据集成与智能 BI 平台，覆盖数据源接入、数据采集、DataX 同步、可视化图表、报表设计、页面搭建、数据质量、数据血缘、团队协作和 AI 辅助分析等场景。

项目采用 Spring Boot 3、Spring Cloud、Java 17 微服务架构，前端使用 Vue 3、TypeScript 和 Vite。开源版本默认可以正常启动并使用基础功能，部分高级额度通过许可文件控制，适合二次开发、私有化部署和商业版本扩展。

本项目使用 Apache License 2.0 开源协议发布。

## 功能特点

| 模块 | 能力 |
| --- | --- |
| 数据源管理 | 支持 MySQL、PostgreSQL、Oracle、SQL Server、SQLite、ClickHouse、Trino 等数据源，提供连接测试、元数据读取和密码加密存储 |
| 数据采集 | 支持全量、增量和自定义 SQL 采集，内置任务状态、日志、调度和重试能力 |
| DataX 同步 | 支持 DataX 引擎配置、库表同步、参数化任务、执行日志和监控 |
| 报表中心 | 支持低代码报表配置、多数据源查询、分页预览、Excel/CSV/ZIP 导出和导出任务追踪 |
| 图表中心 | 基于 ECharts 的可视化配置，支持折线、柱状、饼图、雷达、漏斗、指标卡、词云等图表 |
| 页面设计 | 支持仪表板页面、移动端页面、组件布局、图表嵌入和在线预览 |
| 数据质量 | 支持质量规则、字段质量检查、质量评分、HTML 报告和 AI 总结 |
| 数据血缘 | 支持手工录入、SQL 解析、上下游查询、影响分析和血缘健康报告 |
| AI 助手 | 支持 OpenAI-compatible API、Qwen、DeepSeek、Ollama 等模型配置，提供 SQL 生成、数据分析、优化建议和上下文问答 |
| 团队协作 | 内置团队空间、会话、文件共享、资源共享、通知和知识库 |
| 权限安全 | RBAC 用户、角色、菜单和按钮权限，支持操作审计、脱敏规则和登录安全控制 |
| 运维监控 | 提供服务健康检查、系统监控、导出中心、缓存策略、Redis 会话和 Docker Compose 部署 |

## 开源版本限制

没有许可文件时，系统会进入默认限制模式。项目仍会正常启动，普通功能可用，高级额度按默认值限制。

| 限制项 | 默认值 |
| --- | ---: |
| 报表导出最大行数 | 100,000 行 |
| 图表中心页面数量 | 10 个 |
| 报表中心页面数量 | 20 个 |
| AI 每日提问次数 | 20 次 |
| 数据流程数量 | 10 个 |

商业授权使用 Ed25519 签名 JSON 文件。默认固定路径：

- Docker 容器：`/app/license/datateacup-license.json`
- 本地运行：`runtime/license/datateacup-license.json`
- 环境变量覆盖：`DATATEACUP_LICENSE_FILE`
- Spring 配置覆盖：`datateacup.license.file`

开源发布包不包含 `.env`、`runtime/`、许可文件、私钥、真实 API Key 或真实数据库密码。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.2、Spring Cloud、MyBatis、MyBatis-Plus、Druid、Sa-Token、JWT |
| 前端 | Vue 3、TypeScript、Vite 5、Naive UI、Pinia、Vue Router、ECharts、CodeMirror |
| 数据 | MySQL 8.0、Redis 7、Caffeine、DataX |
| 构建 | Maven、npm、Docker、Docker Compose、Nginx |
| AI | OpenAI-compatible API、Qwen、DeepSeek、Ollama |

## 项目结构

```text
datateacup/
|-- dp-common/                  # 通用 DTO、异常、工具、缓存、Redis、Sa-Token 配置
|-- dp-core/                    # 核心业务服务与实体
|-- dp-service-starter/         # 共享 Controller、Feign、拦截器、AOP 和公共配置
|-- dp-gateway/                 # API 网关，默认端口 8888
|-- dp-system-service/          # 系统服务，默认端口 9001
|-- dp-data-service/            # 数据服务，默认端口 9002
|-- dp-analytics-service/       # 分析服务，默认端口 9003
|-- dp-collaboration-service/   # 协作服务，默认端口 9004
|-- dp-ui/                      # Vue 3 前端
|-- docs/sql/                   # 数据库结构、初始化数据和完整安装脚本
|-- docker-compose.yml
|-- Dockerfile
`-- README.md
```

## 快速启动

### Docker Compose

Docker Compose 会启动 MySQL、Redis、4 个后端服务、API 网关和前端 Nginx。首次启动时 MySQL 自动执行 `docs/sql/full/datateacup_full.sql`。

```bash
cp .env.example .env
docker compose up -d --build
```

默认访问：

- 前端：`http://localhost`
- 网关健康检查：`http://localhost:8888/api/health`
- 默认账号：`admin`
- 默认密码：`admin123`

### 本地开发

准备 JDK 17、Maven 3.8+、Node.js 18+、MySQL 8.0+。

```bash
mysql -u root -p < docs/sql/full/datateacup_full.sql
mvn clean package -DskipTests
```

分别启动后端服务：

```bash
java -jar dp-system-service/target/dp-system-service-2.1.0-exec.jar --spring.profiles.active=dev
java -jar dp-data-service/target/dp-data-service-2.1.0-exec.jar --spring.profiles.active=dev
java -jar dp-analytics-service/target/dp-analytics-service-2.1.0-exec.jar --spring.profiles.active=dev
java -jar dp-collaboration-service/target/dp-collaboration-service-2.1.0-exec.jar --spring.profiles.active=dev
java -jar dp-gateway/target/dp-gateway-2.1.0.jar
```

启动前端：

```bash
cd dp-ui
npm install
npm run dev
```

前端开发地址：`http://localhost:3000`

## 数据库脚本

SQL 脚本保存在 `docs/sql/`，由验证后的本地 `data_platform` 数据库整理生成。

- `docs/sql/full/datateacup_full.sql`：完整一键安装脚本，包含建库、建表、视图、索引和初始化数据
- `docs/sql/schema/01_schema.sql`：数据库结构脚本
- `docs/sql/data/02_seed_data.sql`：安全初始化数据

全新安装建议使用 `datateacup_full.sql`。拆分安装可以先执行 `schema/01_schema.sql`，再执行 `data/02_seed_data.sql`。详细说明见 [docs/sql/README.md](docs/sql/README.md)。

初始化数据仅包含默认管理员、角色权限、菜单、系统配置、字典、组织岗位、工作流默认规则和内置模板。运行日志、缓存、聊天记录、SQL 历史、导出记录、测试数据和临时内容不会写入开源初始化脚本。

## Docker 与 Redis

`docker-compose.yml` 默认启用 Redis：

```text
REDIS_ENABLED=true
REDIS_HOST=redis
REDIS_PORT=6379
```

Redis 用于分布式会话、权限缓存、热点数据缓存和跨服务状态共享。关闭 Redis 时系统会回退到内存缓存模式，生产环境建议保持启用。

## 常用命令

```bash
# 后端构建
mvn clean package -DskipTests

# 后端测试
mvn test

# 前端开发
cd dp-ui && npm run dev

# 前端生产构建，Docker 使用
cd dp-ui && npm run build:docker

# Docker 部署
docker compose up -d --build
docker compose logs -f gateway
docker compose down
```

## 发布说明

开源仓库只保留源代码、Docker 配置、SQL 脚本和必要 Markdown 文档。不发布以下内容：

- `.env`
- `runtime/`
- `test-results/`
- `_build.log`
- `_build.err.log`
- `target/`
- `node_modules/`
- `dp-ui/dist/`
- `.idea/`
- `.devin/`
- 许可文件、私钥、真实 API Key、真实数据库密码
- 详细设计、内部规格、临时测试脚本和截图素材

Markdown 文档可以通过文字、链接或相对路径说明截图位置和操作步骤；截图图片、浏览器遥测文件、运行日志和导出文件不进入开源仓库。

发布前建议执行：

```bash
mvn test
cd dp-ui && npm run build:docker
mysql -u root -p < docs/sql/full/datateacup_full.sql
```
