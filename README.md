# WLTogether

**私有自托管网络媒体同步播放器**

WLTogether 是一个基于 Web 的媒体同步播放工具，支持视频和音频在多设备间实时同步播放。服务器仅作为协调中枢，不存储、不传输、不处理任何媒体文件。

---

## 法律声明

**本项目仅供学习、研究与技术交流使用。**

- 本项目是一个开源的技术研究项目，旨在探索 WebRTC、P2P 传输、时钟同步等技术的工程实现。
- 使用者须自行确保对同步播放的媒体内容拥有合法的使用权。本项目开发者不提供任何媒体内容，也不对用户使用本软件播放的内容承担任何责任。
- 使用者须遵守所在地法律法规。**严禁将本软件用于任何侵犯他人知识产权或其他非法用途。**
- 本项目采用离线包（`.wlp`）加密传输机制，旨在研究数据加密与安全传输技术，使用者不得将其用于规避版权保护措施。
- 本项目开发者不承担因使用本软件而产生的任何直接或间接责任。

**By using this software, you acknowledge that you are solely responsible for your use of it and agree to comply with all applicable laws and regulations.**

---

## 核心架构原则

| 原则 | 说明 |
|------|------|
| 服务器角色 | 纯协调中枢：认证、群组管理、授时同步、聊天持久化、P2P 信令中继 |
| 媒体文件 | **永远只在客户端之间 P2P 传输，服务器不接触媒体数据** |
| 双用户体系 | 注册用户（邮箱验证，完整功能）+ 设备用户（P2P 自组网，7 天证书），两套体系完全隔离 |
| 传输方案 | P2P 直连 → 种子做种 → 离线包 → LAN → 服务器中转，5 种互为备选 |
| 离线包 | `.wlp` 格式，三层可选安全：群组绑定 + AES-256-GCM 加密 + Ed25519 签名 |

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 21, Spring Boot 3.2, Spring Data JPA, Spring WebSocket + STOMP |
| 数据库 | MySQL 8 / SQLite 双兼容（`spring.profiles.active=sqlite|mysql`），Flyway 迁移 |
| 认证 | Spring Security + JWT（Access 2h + Refresh 30d），邮箱验证码（163 SMTP SSL 465） |
| 限流 | Resilience4j RateLimiter + CircuitBreaker |
| Web 前端 | Vue 3 (Composition API) + Vite 8 + Element Plus + Pinia + Plyr + WebTorrent |
| 加密 | Web Crypto API (AES-256-GCM + ECDSA P-256) |
| 部署 | Debian + Nginx (HTTPS/WSS 终结) + Let's Encrypt + systemd |

---

## 功能概览

- **媒体同步播放**：视频和音频的多设备实时同步，支持播放/暂停/跳转控制
- **群组管理**：创建群组、邀请成员、角色管理（群主/管理员/成员）、入群审批
- **实时聊天**：文字消息、图片消息、输入状态提示、消息持久化
- **P2P 传输**：WebTorrent + WebRTC 直连，媒体文件不经过服务器
- **离线包**：`.wlp` 加密媒体容器，支持 AES-256-GCM 加密与离线分发
- **播放队列**：多文件队列管理，支持拖拽排序、切换、删除
- **进度记忆**：离开会话自动保存播放位置，再次进入自动恢复
- **深色模式**：浅色/深色/跟随系统三档切换
- **响应式布局**：适配桌面与移动端浏览器

---

## 快速开始

### 环境要求

- JDK 21
- Node.js 18+
- pnpm
- MySQL 8（可选，默认使用 SQLite）

### 后端启动

```bash
cd wltogether-server

# SQLite 模式（开发推荐，无需安装数据库）
mvn spring-boot:run -Dspring-boot.run.profiles=sqlite

# MySQL 模式
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

启动后访问 `http://localhost:8080`，API 基础路径为 `/api`。

### Web 前端启动

```bash
cd wltogether-web
pnpm install
pnpm dev
```

启动后访问 `http://localhost:5173`，Vite 自动代理 API 请求到后端。

### 生产部署

```bash
# 后端 fat jar
cd wltogether-server && mvn package -DskipTests
java -jar target/wltogether-server-0.1.0.jar --spring.profiles.active=sqlite

# Web 前端静态文件
cd wltogether-web && pnpm build
# 将 dist/ 目录部署到 Nginx
```

---

## 配置要点

- 服务器**仅监听 `127.0.0.1:8080`**，不对外暴露端口，由 Nginx 反向代理
- 不使用 Docker/K8s（面向 2 核 2G 小服务器，直接 jar 部署）
- 不使用 Redis（用户量 <20，密钥仅内存暂存 24 小时）
- 不使用 FFmpeg/FFprobe（服务器不处理媒体）
- JPA 参数化查询，禁止原生 SQL 拼接
- 密钥/密码不写入日志，不在数据库中持久化

---

## 项目结构

```
WLTogether/
├── wltogether-server/          # Spring Boot 后端
│   └── src/main/java/com/wltogether/
│       ├── config/             # Security、WebSocket 配置
│       ├── controller/         # REST 控制器
│       ├── model/
│       │   ├── entity/         # JPA 实体
│       │   └── dto/            # 数据传输对象
│       ├── repository/         # Spring Data JPA 仓库
│       ├── security/           # JWT 认证与过滤器
│       ├── service/            # 业务逻辑层
│       └── websocket/          # STOMP 消息处理器
├── wltogether-web/             # Vue 3 前端
│   └── src/
│       ├── api/                # 后端 API 封装
│       ├── components/         # 可复用组件
│       ├── composables/        # Vue Composition API 逻辑
│       ├── router/             # Vue Router 路由
│       ├── stores/             # Pinia 状态管理
│       ├── utils/              # 工具函数（加密等）
│       └── views/              # 页面视图
└── 架构设计方案.md              # 完整架构文档
```

---

## 许可证

本项目仅供学习、研究与技术交流使用。使用前请阅读上文法律声明。

---

## 免责声明

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
