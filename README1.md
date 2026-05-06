# EchoMark 项目（2025-2026Fail-SSE-SoftwareEngineeringCourseProject）

> 说明：本 README 在保留原始说明基础上合并并扩展了项目的最新结构与改动（截至 2025-12-13）。请在项目结构变化后同步更新本文件。

## 目录结构说明（保留并扩展）

```
2025-2026Fail-SSE-SoftwareEngineeringCourseProject【VoiceMark】

|-- SRS/                    # 需求与设计文档
|-- 需求规约/               # 需求规约文档
|-- programming/            # 源代码根文件夹
|   |-- Backends/
|       |-- common/         # 跨服务共用代码（DTO/exception/security/config）
|       |-- gateway/        # API 网关（可选）
|       |-- services/       # 各微服务
|           |-- AuthService/
|           |-- AudioService/
|           |-- SocialService/  # 新增：社交功能服务（点赞/评论/回复）
|           |-- UserService/    # 新增：用户服务（个人信息管理）
|-- storage/                # 示例资源/上传文件（非源码）
```

### Microservice 目录约定（以 AuthService 为例）

```
AuthService/
├─ src/main/java/org/tongji/sse/
│  ├─ controller/   # REST 控制层
│  ├─ service/      # 业务接口与 impl
│  ├─ repository/   # JPA Repository
│  ├─ entity/       # JPA 实体
│  ├─ security/     # Jwt/Filter/SecurityConfig
│  └─ dto/          # 请求/响应 DTO
└─ src/main/resources/
   └─ application.properties
```

---

## 项目概览（精要）

VoiceMark 是一个以音频为核心的微服务系统，支持音频的录制、上传、管理与社交互动（点赞、评论、回复）。

技术栈：Java 17+、Spring Boot、Spring Data JPA、Maven 多模块

主要模块：

- `AuthService`：用户认证/授权（JWT）
- `UserService`：用户个人信息管理（新增模块）
- `AudioService`：音频上传、元数据管理、详情查询
- `SocialService`：社交功能（新增模块，负责点赞/评论/回复/聚合统计）
- `common`：跨服务共享逻辑（DTO、异常处理、Security 工具、日志配置）
- `gateway`（可选）：统一对外路由

本仓库还包含 SRS、ER 图、需求文档等资料，便于功能对齐与设计回顾。

---

## 本次主要改动简介（摘录）

- AuthService 更新：
    - 注册：新增手机号字段，支持邮箱/手机号唯一性校验。
    - 登录：支持用户名/邮箱/手机号登录。
    - 密码重置：新增请求验证码与重置密码接口。
    - 退出登录：新增 logout 接口。
- 新增 UserService 模块：
    - 提供用户个人信息（UserProfile）的管理（查询/更新）。
    - 支持查看他人公开信息。
- 新增 `SocialService` 模块，提供音频的点赞、评论、回复和评论聚合（含批量统计，避免 N+1）。
- 将 `OpenApiConfig` 提取到 `common`，移除服务内重复配置。
- `common` 中新增：`TestJwtGenerator`（开发调试用）、全局 `logback-spring.xml`、以及 `GlobalExceptionHandler` 的日志改进（使用 `@Slf4j`）。
- `SecurityUtil` 增加 `getUserIdOrNull(HttpServletRequest)`，用于允许匿名访问的场景。
- `AudioService`：实体与 DTO 调整（增加 userId、userName），服务中对匿名读取与发布权限的校验改进。
- 在 `programming/Backends/services/pom.xml` 中注册 `SocialService` 和 `UserService`。

---

## 各模块详解

### 1. common（programming/Backends/common）
职责：公共 DTO、异常处理、Security 工具、日志与全局配置。

关键点：

- `OpenApiConfig`：集中管理 OpenAPI 配置。
- `GlobalExceptionHandler`：统一异常响应，使用 `log.error()` 记录未处理异常。
- `SecurityUtil`：提供 `getUserIdOrNull`（允许在匿名请求中安全获取 userId）。
- `TestJwtGenerator`：用于本地生成开发用 JWT（仅开发环境）。
- `logback-spring.xml`：统一日志输出与文件滚动策略。

使用建议：服务间共享 `common` 以减少重复代码与保持行为一致。

### 2. AuthService

职责：用户注册、登录、颁发/校验 JWT、密码重置。

关键改动：
- 注册支持手机号，并校验邮箱/手机号唯一性。
- 登录支持用户名、邮箱、手机号三种方式。
- 新增密码重置流程接口（请求/验证）。

注意：`OpenApiConfig` 已移入 `common`，AuthService 的安全实现仍保留在其 `security` 包中（JwtUtil、JwtAuthenticationFilter 等）。

### 3. UserService（programming/Backends/services/UserService）

职责：用户个人信息管理（UserProfile）。

关键点：
- `UserProfile` 实体：包含昵称、头像、简介、自我描述。
- 提供查询/更新当前用户信息的接口。
- 提供查看他人公开信息的接口。

### 4. AudioService

职责：音频文件上传、存储、元数据管理、发布与详情查询。

关键改动：

- `Audio` 实体增加 `userId` 与 `User` 关联（ManyToOne），用于返回作者信息。
- `AudioResponseDto` 增加 `userName`。
- `AudioRepository` 新增 `existsByIdAndDeletedFalseAndStatus` 用以校验音频状态。
- `AudioServiceImpl` 使用 `SecurityUtil.getUserIdOrNull` 支持匿名查询，同时在写入/发布等操作校验用户权限与存在性。
- `SecurityConfig` 放行部分 GET 接口（音频列表/详情）以支持匿名访问。

部署建议：若微服务间数据库不共享，AudioService 仅保存 `userId`，并在需要时通过 AuthService API 或缓存查询用户名。

### 5. SocialService（programming/Backends/services/SocialService）

职责与实现概述：

- 提供音频点赞（AudioLike）、评论（AudioComment）、评论回复（AudioCommentReply）以及对评论/回复点赞（CommentReplyLike）。
- 提供批量统计点赞数和批量获取用户已点赞目标集合的 Repository 方法，以避免 N+1 查询。
- Controller 层提供对外 REST API（`LikeController`, `CommentController`）。
- Service 层实现关键业务逻辑（toggle like、add comment/reply、delete comment/reply、getAudioDetails 聚合）。

注意事项：

- 数据一致性：在分布式部署下，`audio_id`、`user_id` 多为引用 ID；如需强一致，可在部署架构上采用单一数据库或使用跨服务事务/补偿机制。
- 性能：对评论/回复做分页，批量统计点赞并缓存热点数据（如用户名、计数）以提高响应性能。
- 本地化：服务内异常信息目前为中文（便于前端直接展示）；若要支持多语言，请引入 i18n 机制。

---

## 开发约定与代码风格

- 包名以 `org.tongji.sse` 为根，按层次划分：`controller`, `service`, `service.impl`, `repository`, `entity`, `dto`, `config`, `security`。
- 实体放在 `entity` 包，请在实体上显式声明列名与外键约束（利于数据库迁移与文档化）。
- DTO 仅包含对外字段，避免直接暴露实体给 Controller。
- 异常：抛出业务明确异常（`NotFoundException`, `BadRequestException`, `AccessDeniedException`），由 `common` 的全局异常处理器统一格式化输出。
- 日志：使用 SLF4J（Lombok `@Slf4j`），`logback-spring.xml` 位于 `common`。
- 安全：JWT 在 `common/security` 或服务的 `security` 包内实现；优先使用 `SecurityUtil.getUserIdOrNull` 在可匿名接口中安全读取用户 id。

---

## 构建、运行与测试

环境：JDK 17+、Maven 3.8+

构建整个项目（跳过测试）：

```cmd
mvn clean install -DskipTests
```

构建/打包单个模块（示例：SocialService）：

```cmd
mvn -pl programming/Backends/services/SocialService -am -DskipTests package
```

运行：在 IDE 中直接运行某个模块的 `*Application` 启动类，或在本地同时启动多个服务进行联调。若需要完整联调，建议使用 docker-compose 或在本地分别启动 AuthService、AudioService、SocialService。

测试：

- 单元/集成测试位于对应模块的 `src/test/java`，建议使用 H2 内存数据库与 Mockito。
- 运行模块测试：

```cmd
mvn -pl programming/Backends/services/SocialService test
```

CI 建议：在 CI 流水线执行 `mvn clean verify` 并运行静态代码检查工具（Checkstyle/SpotBugs）与单元测试。

---

## 重要变更记录（摘要）

1. 将 `OpenApiConfig` 移动到 `common`，删除服务中的重复配置。
2. `common` 中新增 `TestJwtGenerator`、`logback-spring.xml`，并改进 `GlobalExceptionHandler`。
3. 新增 `SocialService` 模块并在 `programming/Backends/services/pom.xml` 注册。
4. `SecurityUtil` 增加 `getUserIdOrNull` 支持匿名访问。
5. `AudioService` 做了实体/DTO/Service 层的调整以支持 user 关联与匿名读取。

---

## 部署与架构注意事项

- 跨服务数据库一致性：若每个微服务使用独立数据库，避免在 SocialService 中写跨库外键约束；优先存储引用 ID 并通过服务间调用或缓存获取详情。
- 缓存：建议使用 Redis 缓存热点数据（用户名、计数等）以减少跨服务延迟。
- 安全：`TestJwtGenerator` 仅适用于开发，请勿将开发密钥或生成的 token 提交到远程仓库。

---

## 后续建议（Roadmap）

- 使用 Feign/HTTP client 做跨服务调用并在关键处缓存/限流。
- 为 SocialService 实现分页、排序、限流与防刷机制。
- 增强跨服务集成测试（Testcontainers/docker-compose）。
- 实施消息队列/事件驱动（如 Kafka）以实现异步统计或补偿事务。

---

## 关于 pom.xml（简要保留原说明）

顶层 `pom.xml` 管理全局依赖、插件与版本；子模块在各自 `pom.xml` 中声明特定依赖。多模块项目通过 `modules` 实现聚合构建。

---

## 联系与贡献

如需修改 README 或对项目提出改动，请以 Issue 方式提出或直接提交 PR。请在 PR 描述中说明变更要点、影响范围及测试步骤。

---

*文件生成时间：2025-12-13*