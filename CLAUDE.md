# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目定位

WLTogether 是一个私有自托管网络媒体**同步播放器**，面向用户数量 <20 人。聊天和文件传输是辅助功能。服务器**不存储、不处理任何媒体文件**。

## 核心架构原则

- **服务器角色**：纯协调中枢（认证、群组管理、授时同步、聊天持久化、P2P 信令中继）
- **媒体文件**：永远只在客户端之间 P2P 传输，服务器不碰
- **双用户体系**：注册用户（邮箱验证，完整功能）+ 设备用户（P2P 自组网，7 天证书）
- **两套体系完全隔离**，不可互通
- **传输方案**：P2P 直连 → 种子做种 → 离线包 → LAN → 服务器中转，5 种互为备选
- **离线包** `.wlp`：三层可选安全（群组绑定 + AES-256-GCM + Ed25519 签名），私钥 `.wlk` 文件防 24h TTL 过期

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Java 21, Spring Boot 3.2, Spring Data JPA, Spring WebSocket + STOMP |
| 数据库 | MySQL 8 / SQLite 双兼容（Spring Profile: `mysql` / `sqlite`），Flyway 迁移 |
| 认证 | Spring Security + JWT (jjwt, Access 2h + Refresh 30d)，邮箱验证码（163 SMTP SSL 465） |
| 限流 | Resilience4j RateLimiter + CircuitBreaker |
| Web 前端 | Vue 3 (Composition API) + Vite + Element Plus + Pinia + Plyr + WebTorrent |
| Android | Kotlin + Jetpack Compose + Hilt + Retrofit + ExoPlayer + libtorrent |
| 部署 | Debian + Nginx (HTTPS/WSS 终结) + Let's Encrypt + systemd |

## 关键文件

- `架构设计方案.md` — 19 节完整架构文档，所有接口、协议、数据库、安全策略的权威来源
- `编码执行计划.md` — 7 阶段执行计划，含文件级交付清单

## 构建与运行（规划中，代码创建后可用）

```bash
# 后端 (SQLite 模式，适合开发)
cd wltogether-server
mvn spring-boot:run -Dspring-boot.run.profiles=sqlite

# 后端 (MySQL 模式)
mvn spring-boot:run -Dspring-boot.run.profiles=mysql

# Web 前端
cd wltogether-web
pnpm install
pnpm dev

# Android
# 用 Android Studio 打开 wltogether-android/，Gradle Sync → Run

# 构建部署包
mvn package -DskipTests         # 后端 fat jar → target/
cd wltogether-web && pnpm build # Vue 静态资源 → web-dist/
```

## 设计约束

- 服务器仅监听 `127.0.0.1:8080`，不对外暴露端口（Nginx 反向代理）
- 不支持 Docker/K8s（2核2G 小服务器直接 jar 部署）
- 不使用 Redis（用户量 <20，密钥仅内存暂存 24h）
- 不使用 FFmpeg/FFprobe（服务器不处理媒体）
- JPA 参数化查询，禁止原生 SQL 拼接
- 密钥/密码不写入日志，不在数据库中持久化
- 导入的 JSON/配置文件必须做格式校验，防注入
