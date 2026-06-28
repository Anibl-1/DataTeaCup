# DataTeaCup SQL

This directory is generated from the verified local `data_platform` database and is the source of truth for fresh installations.

## Layout

```text
docs/sql/
├── schema/
│   └── 01_schema.sql        # database, tables, views, indexes
├── data/
│   └── 02_seed_data.sql     # safe seed data only
├── full/
│   └── datateacup_full.sql # one-step install script
└── README.md
```

## Fresh Install

Recommended one-step install:

```bash
mysql -u root -p < docs/sql/full/datateacup_full.sql
```

Split install:

```bash
mysql -u root -p < docs/sql/schema/01_schema.sql
mysql -u root -p < docs/sql/data/02_seed_data.sql
```

Docker Compose mounts `docs/sql/full/datateacup_full.sql` into the MySQL init directory, so the database is created automatically on the first container start.

## Seed Data

The seed file includes only installation data:

- default tenant: `default`
- default admin user: `admin / admin123`
- roles, permissions, role relations, menu tree
- system config and empty AI provider keys
- dictionary, organization, post and workflow defaults
- built-in report, style, masking and subscription templates
- notification templates

The seed file intentionally excludes runtime data, including health checks, monitor metrics, login logs, operation logs, cache data, chat history, SQL history, export records and temporary test data.

## Current Integrity Baseline

The verified local database baseline contains:

- 116 base tables
- 8 views
- 124 total objects
- 22 seeded tables

`02_seed_data.sql` and `datateacup_full.sql` use extended `INSERT INTO` statements. One `INSERT INTO` can contain many rows, so the number of insert statements is smaller than the number of initialized rows.

## License Behavior

No license file is shipped. Without a license file the project starts normally in default limited mode.

- container path: `/app/license/datateacup-license.json`
- local path: `runtime/license/datateacup-license.json`
- env override: `DATATEACUP_LICENSE_FILE`
- Spring property override: `datateacup.license.file`

Open-source releases must not include `.env`, `runtime/`, license files, private keys, real API keys or real database passwords.

## Integrity Checks

After import, these checks should pass:

```sql
SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'data_platform';
SELECT TABLE_TYPE, COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'data_platform' GROUP BY TABLE_TYPE;
SELECT COUNT(*) FROM data_platform.tenant WHERE tenant_code = 'default';
SELECT username FROM data_platform.sys_user WHERE username = 'admin';
SELECT config_value FROM data_platform.sys_config WHERE config_key = 'system.title';
SELECT * FROM data_platform.sys_menu
WHERE menu_code IN ('GrayRelease', 'LicenseManage')
   OR route_path IN ('/gray-release', '/license-manage')
   OR permission_code IN ('gray:manage', 'license:manage');
```

The last query should return no rows.
