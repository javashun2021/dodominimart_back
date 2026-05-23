# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build entire project (skip tests)
mvn clean package -DskipTests

# Run the application
cd ruoyi-admin && mvn spring-boot:run
# or after packaging:
java -jar ruoyi-admin/target/ruoyi-admin.jar

# Run tests (note: the project currently has no test files)
mvn test

# Build a single module
mvn clean package -pl ruoyi-system -am -DskipTests
```

The app runs on **http://localhost:8080** by default. Default credentials: `admin / admin123`.

## Database Setup

Before running, create a MySQL database and execute the SQL scripts in order:
1. `sql/ry_20190215.sql` — system schema and seed data
2. `sql/quartz.sql` — Quartz scheduler tables

Then configure the connection in `ruoyi-admin/src/main/resources/application-druid.yml` (the only file modified from the upstream project per git status).

## Module Architecture

This is a multi-module Maven project (RuoYi v3.x). The modules are:

| Module | Role |
|--------|------|
| `ruoyi-admin` | Spring Boot entry point, web controllers, Thymeleaf templates |
| `ruoyi-framework` | Shiro security, Druid datasource, session management, AOP aspects |
| `ruoyi-system` | Business logic: users, roles, depts, menus, dicts, logs (service + mapper + domain) |
| `ruoyi-common` | Shared utilities, base classes, annotations, constants, enums |
| `ruoyi-quartz` | Quartz scheduler integration (job management) |
| `ruoyi-generator` | Code generation using Velocity templates |

Dependency flow: `ruoyi-admin` → `ruoyi-framework` → `ruoyi-system` → `ruoyi-common`. `ruoyi-quartz` and `ruoyi-generator` are independent modules assembled by `ruoyi-admin`.

## Key Architecture Patterns

**Layered structure** within `ruoyi-system` (mirrored in `ruoyi-quartz`/`ruoyi-generator`):
- `domain/` — JPA-style POJOs extending `BaseEntity`
- `mapper/` — MyBatis mapper interfaces + XML in `resources/mapper/`
- `service/` — `ISysXxxService` interface + `SysXxxServiceImpl`
- Controllers live in `ruoyi-admin/src/main/java/com/ruoyi/web/controller/`

**Base classes to extend:**
- `BaseEntity` — provides `createBy`, `createTime`, `updateBy`, `updateTime`, `remark`, `params` (map for arbitrary query conditions)
- `BaseController` — provides `startPage()`, `getDataTable()`, `toAjax()`, `getCurrentUser()`, and Shiro helpers

**Custom annotations (processed via AOP in `ruoyi-framework`):**
- `@Log(title, businessType)` — operation audit logging (async via `AsyncManager`)
- `@DataScope(deptAlias, userAlias)` — row-level dept/user filtering; appends SQL to `entity.params`
- `@DataSource(value)` — dynamic datasource switching (master/slave)
- `@Excel` — marks fields for POI-based Excel export/import

**Response format:** All AJAX endpoints return `AjaxResult` with `code`/`msg`/`data`. Use `success()`, `error()`, `toAjax(rows)` from `BaseController`.

**Pagination:** Call `startPage()` before any list query; PageHelper intercepts the next MyBatis query automatically. Return `getDataTable(list)` from controllers.

**Security (Shiro):** Permissions are strings like `"system:user:edit"`. Use `@RequiresPermissions` on controller methods. Data scope is applied at the service layer via `@DataScope`.

## Configuration Reference

- **Port / context path:** `ruoyi-admin/src/main/resources/application.yml`
- **Database / Druid pool:** `ruoyi-admin/src/main/resources/application-druid.yml`
- **Logging:** `ruoyi-admin/src/main/resources/logback.xml` (console + file, 60-day retention)
- **MyBatis mappers:** scanned from `classpath*:mapper/**/*Mapper.xml`
- **File uploads:** stored under path configured by `ruoyi.profile` in `application.yml`
- **Code generator defaults:** package `com.ruoyi.system`, table prefix `sys_`

## Naming Conventions

- Packages: `com.ruoyi.{module}.{layer}` (e.g. `com.ruoyi.system.service`)
- Services: interface `ISysXxxService`, impl `SysXxxServiceImpl`
- Mappers: `SysXxxMapper` (interface) + `SysXxxMapper.xml`
- Controllers: class names end with `Controller`, URL prefix matches feature (e.g. `/system/user`)
