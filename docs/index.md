# DataTeaCup 文档

DataTeaCup（数据茶杯）文档目录只保留开源发布需要的简洁文档。项目介绍、功能特点和部署入口见根目录 [README.md](../README.md)，数据库脚本说明见 [docs/sql/README.md](sql/README.md)。

## 文档清单

| 文档 | 用途 |
| --- | --- |
| `README.md` | 项目介绍、功能特点、快速启动、开源版本限制、Docker 部署 |
| `docs/sql/README.md` | SQL 脚本目录、全新部署、初始化数据、许可限制和完整性检查 |
| `docs/sql/full/datateacup_full.sql` | 完整一键初始化脚本 |
| `docs/sql/schema/01_schema.sql` | 数据库结构脚本 |
| `docs/sql/data/02_seed_data.sql` | 初始化数据脚本 |

## 发布边界

开源发布包只保留源代码、SQL 脚本和必要 Markdown 文档。以下目录或文件不进入发布仓库：

- `runtime/`
- `docs/design/`
- `docs/functional/`
- `docs/guide/`
- `docs/specs/`
- `docs/项目详细设计/`
- `.env`
- `.idea/`
- `.devin/`
- `test-results/`
- `_build.log`
- `_build.err.log`
- `target/`
- `node_modules/`
- `dp-ui/dist/`
- 临时脚本、测试截图、许可文件、私钥、真实 API Key 和真实数据库密码

Markdown 可以保留截图说明或链接，但截图图片本身不进入发布仓库。

## 部署入口

```bash
cp .env.example .env
docker compose up -d --build
```

Docker Compose 默认启动 MySQL、Redis、后端微服务、API 网关和前端 Nginx。首次启动会自动执行 `docs/sql/full/datateacup_full.sql`。

默认访问：

- 前端：`http://localhost`
- 网关健康检查：`http://localhost:8888/api/health`
- 默认账号：`admin`
- 默认密码：`admin123`
