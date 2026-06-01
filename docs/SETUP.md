# EchoMark 环境搭建指南

## 前置要求

| 软件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | 后端编译和运行 |
| Maven | 3.9+ | 后端构建 |
| Node.js | 20+ | 前端开发服务器 |
| Python | 3.10+ | 资源服务器（需安装 Flask） |
| MySQL | 8.0+ | 主数据库 |
| Redis | 5.0+ | 缓存和幂等性（可选，已内置） |

```bash
# 安装 Python 依赖
pip install flask
```

## 快速开始

### 1. 克隆仓库

```bash
git clone https://github.com/CNnx686/EchoMark.git
cd EchoMark
```

### 2. 初始化数据库

确保 MySQL 已安装并运行，然后：

```bash
# Windows PowerShell
.\scripts\setup-db.ps1 -MySQLPassword "your_mysql_password"

# 如果 root 无密码
.\scripts\setup-db.ps1
```

### 3. 配置密钥

编辑以下文件，填入实际密钥：

| 文件 | 配置项 | 说明 |
|------|--------|------|
| `programming/Backends/services/AuthService/src/main/resources/application.properties` | `spring.mail.password` | QQ 邮箱授权码（注册/验证码功能） |
| `programming/Backends/services/LLMService/src/main/resources/application.properties` | `llm.api.key` | DeepSeek API Key（AI 功能） |

不配置以上密钥，核心功能（注册/登录/音频管理/社交）不受影响，仅邮件发送和 AI 功能不可用。

### 4. 启动所有服务

```bash
# 自动编译并启动全部 11 个服务
.\scripts\start-all.ps1

# 如果已经编译过，跳过编译
.\scripts\start-all.ps1 -SkipBuild
```

### 5. 停止所有服务

```bash
.\scripts\stop-all.ps1
```

## 服务架构

| 服务 | 端口 | 技术栈 |
|------|------|--------|
| Resource Server | 5000 | Python Flask |
| API Gateway | 8080 | Spring Cloud Gateway |
| AuthService | 5001 | Spring Boot |
| UserService | 5002 | Spring Boot |
| AudioService | 5003 | Spring Boot |
| SocialService | 5004 | Spring Boot |
| NotificationService | 5005 | Spring Boot |
| SseService | 5006 | Spring Boot |
| LLMService | 5007 | Spring Boot |
| UserPersonaService | 5008 | Spring Boot |
| Frontend | 5173 | Vue 3 + Vite |

## 数据库

- **类型**: MySQL 8.0
- **数据库名**: `sound_map`
- **JDBC URL**: `jdbc:mysql://localhost:3306/sound_map`
- **建表脚本**: `schema.sql`

## 注意事项

1. MySQL 需提前安装，项目不包含 MySQL 二进制文件
2. Redis 和 RabbitMQ 二进制位于 `runtime/` 目录，**不在 Git 仓库中**
3. 所有后端服务共享同一个数据库 `sound_map`
4. 前端通过 Vite 代理将 `/api/*` 请求转发到 Gateway (8080)
