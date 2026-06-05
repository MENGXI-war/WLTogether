<template>
  <div class="docs-page">
    <!-- Top bar (same style as landing page) -->
    <header class="docs-header">
      <div class="header-left">
        <router-link to="/" class="header-logo-link">
          <img src="/Logs.png" alt="WLTogether" class="header-logo" />
        </router-link>
        <el-button text class="header-btn" @click="router.push('/')">
          <el-icon><HomeFilled /></el-icon> 首页
        </el-button>
        <el-button text class="header-btn" @click="onToggleTheme">
          <el-icon><component :is="isDark ? Sunny : Moon" /></el-icon> 主题切换
        </el-button>
        <el-button text class="header-btn" @click="scrollToSection('about')">
          <el-icon><InfoFilled /></el-icon> 关于
        </el-button>
      </div>
      <div class="header-right">
        <template v-if="authStore.isAuthenticated">
          <el-button text class="header-btn" @click="router.push('/groups')">
            <el-icon><HomeFilled /></el-icon> 主页
          </el-button>
          <el-dropdown trigger="click" @command="onUserCommand">
            <el-avatar :size="34" :src="authStore.user?.avatarUrl" class="avatar-btn">
              {{ authStore.user?.nickname?.charAt(0) || 'U' }}
            </el-avatar>
            <template #dropdown>
              <el-dropdown-item command="settings" :icon="Setting">设置</el-dropdown-item>
              <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" class="login-btn" @click="router.push('/login')">
            <el-icon><UserFilled /></el-icon> 注册 / 登录
          </el-button>
        </template>
      </div>
    </header>

    <!-- Body: sidebar + content -->
    <div class="docs-body">
      <!-- Left sidebar -->
      <aside class="docs-sidebar">
        <div class="sidebar-search">
          <el-input v-model="searchText" placeholder="搜索文档..." :prefix-icon="Search" clearable size="small" />
        </div>
        <nav class="sidebar-nav">
          <div v-for="category in filteredNav" :key="category.title" class="nav-category">
            <div class="nav-cat-title" @click="toggleCategory(category.title)">
              <el-icon><component :is="expandedCats.has(category.title) ? ArrowDown : ArrowRight" /></el-icon>
              <span>{{ category.title }}</span>
            </div>
            <div v-if="expandedCats.has(category.title)" class="nav-cat-items">
              <div
                v-for="item in category.items"
                :key="item.id"
                :class="['nav-item', { active: activeSection === item.id }]"
                @click="scrollToDoc(item.id)"
              >
                {{ item.label }}
              </div>
            </div>
          </div>
        </nav>
      </aside>

      <!-- Right content -->
      <main class="docs-content" ref="contentEl">
        <div class="docs-article" v-html="renderedContent"></div>
      </main>
    </div>

    <PageFooter />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  HomeFilled, Sunny, Moon, InfoFilled, UserFilled, Setting, SwitchButton,
  Search, ArrowDown, ArrowRight
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useWebSocket } from '@/composables/useWebSocket'
import { useTheme } from '@/composables/useTheme'
import PageFooter from '@/components/PageFooter.vue'

const router = useRouter()
const authStore = useAuthStore()
const { isDark, setTheme } = useTheme()
const { disconnect } = useWebSocket()

const contentEl = ref(null)
const searchText = ref('')
const activeSection = ref('quickstart')
const expandedCats = ref(new Set(['使用手册', '常见问题']))

// ==================== Navigation Structure ====================
const navStructure = [
  {
    title: '使用手册',
    items: [
      { id: 'quickstart', label: '快速开始' },
      { id: 'register', label: '注册与登录' },
      { id: 'groups', label: '群组管理' },
      { id: 'sessions', label: '发起同步会话' },
      { id: 'playback', label: '播放同步' },
      { id: 'chat', label: '聊天功能' },
      { id: 'wlp', label: '离线包 (.wlp)' },
      { id: 'p2p', label: 'P2P 传输' },
      { id: 'settings', label: '设置与个性化' },
      { id: 'deploy', label: '部署与运维' }
    ]
  },
  {
    title: '常见问题',
    items: [
      { id: 'faq-account', label: '账号相关' },
      { id: 'faq-playback', label: '播放问题' },
      { id: 'faq-connection', label: '连接与同步' },
      { id: 'faq-wlp', label: '离线包问题' },
      { id: 'faq-other', label: '其他问题' }
    ]
  }
]

const filteredNav = computed(() => {
  if (!searchText.value.trim()) return navStructure
  const q = searchText.value.toLowerCase()
  return navStructure.map(cat => ({
    ...cat,
    items: cat.items.filter(item =>
      item.label.toLowerCase().includes(q) ||
      (docSections[item.id] || '').toLowerCase().includes(q)
    )
  })).filter(cat => cat.items.length > 0)
})

// ==================== Documentation Content ====================
// Each section written based on ACTUAL codebase implementation

const docSections = {
  quickstart: `
## 快速开始

### 1. 访问网站

在浏览器中打开 WLTogether 服务器地址（例如 \`https://your-server.com\`），进入首页。

### 2. 注册账号

点击右上角「注册 / 登录」，填写**邮箱**、**用户名**和**密码**（至少 6 位）。系统会向你的邮箱发送一封 6 位数字验证码，验证后即可登录。

### 3. 创建或加入群组

登录后进入主页，点击左侧群组栏顶部的 **+** 按钮创建新群组。你可以设置：
- **群组名称**和**描述**
- **加群方式**：开放加入 / 需要审批 / 仅邀请

分享群组给朋友，让他们加入同一个群组。

### 4. 发起同步会话

在群组中点击「发起同步会话」，选择一个本地媒体文件，选择视频/音乐模式，即可创建会话。其他群成员可以看到并加入该会话。

### 5. 开始同步播放

加入会话后，选择一个本地文件（需与会话发起者的文件匹配），即可进入同步播放。**主持人**控制播放/暂停/进度，其他参与者自动跟随。

> **提示**：会话会在最后一人离开时自动结束；如果所有人因网络问题离线超过 10 分钟，会话也会被自动清理。
`,

  register: `
## 注册与登录

### 注册流程

WLTogether 使用**邮箱验证码**机制确保账号安全：

1. 在登录页点击「注册」标签
2. 填写邮箱（用于接收验证码）、用户名、密码（至少 6 位）
3. 点击「发送验证码」—— 系统通过 **163 SMTP** 发送 6 位数字验证码
4. 输入验证码完成验证

**注意事项**：
- 验证码有效期 **5 分钟**，过期需重新发送
- 同一邮箱 **60 秒内**只能请求一次验证码
- 每天最多发送 **20 封**验证邮件
- 最多允许 **5 次**验证码输入错误

### 登录

验证通过后即可登录。登录成功会获取两个 JWT Token：
- **Access Token**：有效期 2 小时，用于 API 认证
- **Refresh Token**：有效期 30 天，用于自动续期

### 密码找回

登录页提供「忘记密码」功能，通过邮箱验证码重置密码。
`,

  groups: `
## 群组管理

### 创建群组

点击左侧群组栏顶部的 **+** 图标，或通过顶部导航栏的「新建群组」按钮：

1. 填写**群组名称**（必填，最长 100 字）
2. 填写**群组描述**（可选，最长 500 字）
3. 创建后自动成为**群主 (OWNER)**

### 群组设置

在群组中点击「设置」可修改：
- **群头像**：上传 PNG/JPEG/WebP（最大 2MB，自动压缩为 256×256）
- **群名称**和**描述**
- **标签**：空格分隔，用于搜索
- **加群方式**：
  - **开放加入**：任何人可加入
  - **需要审批**：提交申请后由管理员审核
  - **仅邀请**：只有被邀请的用户能加入

### 成员管理

群主和管理员可以：
- **邀请成员**：通过用户名或 UID 邀请
- **移除成员**：将成员移出群组（不能移除群主）
- **禁言**：设置 1 小时 / 24 小时 / 7 天的禁言
- **设置管理员**：群主可将成员提升为管理员
- **转让群主**：群主可将所有权转让给其他成员

### 入群审批

当加群方式为「需要审批」时：
1. 用户点击「申请加入」
2. 群主/管理员在群组设置中看到待审批列表
3. 可以**通过**或**拒绝**申请
`,

  sessions: `
## 发起同步会话

### 创建会话

在群组中点击「发起同步会话」按钮：

1. **选择本地文件**：点击选择要同步播放的媒体文件
2. **设置会话名称**：默认为文件名
3. **选择媒体类型**：视频 (VIDEO) 或 音乐 (MUSIC)
4. 点击「发起」创建会话

创建后，群组内会显示新的会话卡片。创建者自动成为**主持人**。

### 加入会话

其他群成员看到活动会话后，点击进入。进入后需要选择**本地对应的媒体文件**（可通过文件哈希匹配验证）。

### 会话生命周期

| 状态 | 含义 |
|------|------|
| CREATED | 已创建，等待第一个参与者 |
| ACTIVE | 进行中 |
| ENDED | 已结束 |

- 主持人可随时**结束会话**
- 最后一人离开时**自动结束**
- 所有人因网络断开离线超过 **10 分钟**，会话**自动销毁**（防止僵尸会话泄露）
- 单个用户最多同时发起 **5 个**活跃会话
- 单个会话最多 **20 人**

### 会话视图

进入会话后：
- **顶栏**：浮动毛玻璃样式，包含返回、同步状态、同步按钮、更多菜单
- **播放器**：Plyr 播放器，显示当前时间 / 总时长
- **播放队列**：支持多文件列表、点击切换、上移/下移/删除
- **聊天面板**：右侧聊天区域
`,

  playback: `
## 播放同步

### 同步原理

WLTogether 通过**时钟同步 + 播放命令广播**实现多设备同步：

1. 客户端每 **5 秒**向服务器发送 \`/sync.ping\`
2. 服务器返回 \`{ clientTime, serverTime }\`
3. 客户端计算 \`offset = serverTime - (clientTime + RTT/2)\`
4. 当偏差超过 **500 毫秒**时，触发 seek 修正

### 播放模式

| 模式 | 说明 |
|------|------|
| **HOST_ONLY**（默认） | 仅主持人可控制播放/暂停/进度 |
| **SHARED_CONTROL** | 所有人可控制（每人每 3 秒最多一次 seek） |
| **FREE** | 各自独立播放，不同步 |

可在设置页的「播放设置」中修改默认模式。

### 播放命令

通过 WebSocket 发送的播放命令：

- \`/playback.play\` — 开始播放（携带 position）
- \`/playback.pause\` — 暂停（携带 position）
- \`/playback.seek\` — 跳转进度（携带 position）
- \`/playback.next\` — 切换到下一曲（音乐模式，2 秒缓冲）

所有命令通过权限检查后广播到 \`/topic/session/{id}\`。

### 播放进度记忆

离开会话时，当前播放位置自动保存到浏览器本地存储。再次进入同一会话时**自动恢复到上次位置**（保持暂停状态）。

### 播放队列

- 支持**多文件**队列
- 点击队列项**切换播放**
- 上移/下移按钮**调整顺序**
- 删除按钮**移除**队列项
- 可在会话内**添加新文件**或**导入离线包**
`,

  chat: `
## 聊天功能

### 消息类型

- **文字消息**：回车发送，支持多行
- **图片消息**：点击图片按钮选择 PNG/JPEG/WebP/GIF，最大 10MB
- **系统消息**：成员加入/离开等事件自动发送

### 聊天特性

- **输入状态提示**：正在输入时会显示打字动画
- **消息时间**：当天显示时间，跨天显示日期
- **加载历史**：向上滚动加载更早的消息
- **头像显示**：同一用户连续消息合并头像

### WebSocket 通信

聊天消息通过 STOMP WebSocket 发送：
- 客户端发送至 \`/chat.send\`
- 服务器广播至 \`/topic/group/{groupId}\`
- 服务器将消息**持久化到数据库**

### 消息显示

- **自己发的消息**：头像右侧，消息气泡左侧，浅蓝色圆角框
- **别人发的消息**：头像左侧，消息气泡右侧，同色圆角框
- 暗色模式下气泡自动切换为深蓝色
`,

  wlp: `
## 离线包 (.wlp)

### 什么是离线包？

.wlp 是 WLTogether 的**加密离线媒体容器**。你可以把媒体文件打包成 .wlp，脱离服务器通过任何方式（网盘/U盘/即时通讯）分享给朋友。

### 导出离线包

通过顶部导航栏「📦 导出」按钮进入导出页：

**Step 1 — 选择文件**：选择要打包的本地媒体文件。

**Step 2 — 安全选项**：
| 选项 | 说明 |
|------|------|
| 绑定群组（必选） | 只有当前群组成员可解密 |
| AES-256-GCM 加密（推荐） | 用随机 256 位密钥加密 |
| Ed25519 签名 | 防篡改验证 |

**过期时间**（加密时）：
- 1 小时 / 24 小时 / 7 天 / 30 天 / **永久有效**

**Step 3 — 下载**：
- 点击下载 \`.wlp\` 离线包文件
- 如果选择了加密，同时下载 \`.wlk\` 密钥文件

### 导入离线包

在视频/音乐会话中，点击顶栏更多菜单 →「导入离线包」：

1. 弹出导入对话框
2. 选择 \`.wlp\` 文件 → 显示文件信息和加密状态
3. 如果已加密 → 选择 \`.wlk\` 密钥文件 → 解密
4. 解密成功后文件自动加入播放队列

### .wlp 文件格式

\`\`\`
[4B 魔数 "WLP\\1"]
[1B 标志: bit0=群组绑定, bit1=AES加密, bit2=签名]
[8B 群组ID（大端序）]
[?B 可选签名: 2B长度 + 签名数据]
[?B 加密: 12B IV + 密文 | 明文: 元数据 + 文件数据]
\`\`\`

### .wlk 密钥文件格式

\`\`\`
[4B 魔数 "WLK\\1"]
[4B 密钥长度]
[32B AES-256 原始密钥]
[8B 过期时间戳（毫秒，0=永久）]
\`\`\`

### 安全建议

- **.wlp 和 .wlk 分开发送**（不同渠道）更安全
- 密钥过期后 **永久无法解密**，请确认接收方能在过期前使用
- 选择「永久有效」的密钥可以**离线时或离开服务器后**使用
- 服务器**不存储密钥**——密钥仅在 .wlk 文件中
`,

  p2p: `
## P2P 传输

### 传输模式

WLTogether 支持 5 种传输方式，按优先级自动选择：

| 模式 | 说明 |
|------|------|
| **P2P 直连** | 通过 WebRTC Data Channel 在两个浏览器间直接传输 |
| **种子做种** | 通过私有 BitTorrent tracker 以 WebTorrent 协议传输 |
| **离线包** | .wlp 文件离线分发 |
| **局域网** | 同局域网内通过 NSD/mDNS 发现并直连 |
| **服务器中转** | 由服务器代理传输（需管理员在服务端启用） |

### WebTorrent（默认）

Web 端使用 WebTorrent 库通过 WebRTC 传输数据：
- 发送方**创建种子**并生成 magnet URI
- 接收方通过 magnet URI **加入 swarm**
- 数据通过浏览器间的 WebRTC Data Channel 传输
- 使用私有 tracker（\`/api/tracker/announce\`）协调 peer 发现

### WebRTC 信令

P2P 直连通过 WebSocket 信令建立：
1. 发送方创建 RTCPeerConnection
2. 通过 STOMP \`/p2p.signal\` 发送 SDP offer
3. 服务器验证双方属于同一群组后中继信令
4. 接收方回复 SDP answer
5. 通过 ICE (STUN: \`stun.l.google.com:19302\`) 建立直连
6. 通过 RTCDataChannel（标签 \`filetransfer\`，有序传输）发送数据，大文件分 16KB 块

### P2P 开关

- **设置页** → P2P 传输 → 启用/禁用开关
- **播放页** → 更多菜单 → P2P 切换
- 禁用 P2P 后，媒体文件通过服务器中转传输（需服务器支持）

### P2P 速度限制

可在设置页配置：
- 最大上传速度：不限制 / 1 / 5 / 10 MB/s
- 最大下载速度：不限制 / 1 / 5 / 10 MB/s
`,

  settings: `
## 设置与个性化

### 个人资料

通过顶部导航栏头像菜单 →「设置」进入：

- **头像**：上传 PNG/JPEG/WebP（最大 2MB），可移除
- **昵称**：修改显示名称（最长 100 字）
- **用户名**和**邮箱**：不可修改

### 外观

- **浅色** / **深色** / **跟随系统** 三种主题模式
- 深色模式下所有页面自动适配暗色调
- Element Plus 组件和自定义样式均支持暗色

### 播放设置

- **默认播放模式**：仅主持人 / 共享控制 / 自由模式
- **时钟同步间隔**：1–30 秒
- **同步偏差阈值**：100–2000 毫秒

### P2P 传输

- 启用/禁用 P2P 传输
- 最大上传/下载速度限制

### 安全

- **修改密码**：输入当前密码 + 新密码（至少 6 位）+ 确认新密码
- 密码修改后自动退出登录，需重新登录
`,

  deploy: `
## 部署与运维

### 环境要求

| 组件 | 要求 |
|------|------|
| Java | JDK 21 |
| 数据库 | MySQL 8 或 SQLite |
| Node.js | 18+（前端构建） |
| 操作系统 | Debian / Ubuntu（推荐） |
| 内存 | 2 核 2G（最小） |

### 后端启动

\`\`\`bash
# SQLite 模式（开发推荐）
cd wltogether-server
mvn spring-boot:run -Dspring-boot.run.profiles=sqlite

# MySQL 模式
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
\`\`\`

### 前端构建

\`\`\`bash
cd wltogether-web
pnpm install
pnpm build
# 静态文件输出到 web-dist/
\`\`\`

### 生产部署

1. 后端打包为 fat jar：\`mvn package -DskipTests\`
2. Nginx 反向代理 + HTTPS/WSS 终结
3. Let's Encrypt 自动证书
4. systemd 服务管理

### 配置要点

- 服务器**仅监听 127.0.0.1:8080**，不对外暴露端口
- Nginx 负责 HTTPS/WSS 终结
- **不使用 Docker/K8s**（小服务器直接 jar 部署）
- **不使用 Redis**（用户量 <20，密钥仅内存暂存 24 小时）
- 不支持 FFmpeg（服务器不处理媒体）
`,

  // ==================== FAQ Sections ====================

  'faq-account': `
## 账号相关

### 收不到验证邮件怎么办？

1. 检查邮箱的**垃圾邮件**文件夹
2. 确认邮箱地址输入正确
3. 验证码有 **60 秒冷却期**，请勿频繁点击发送
4. 每天最多 **20 封**邮件，超出后请次日再试
5. 如果问题持续，请联系服务器管理员检查 SMTP 配置

### 忘记密码了？

在登录页点击「忘记密码」，通过邮箱验证码重置密码。

### 可以修改邮箱吗？

目前不支持修改邮箱。如有需要请联系服务器管理员。

### 什么是 UID？

每个用户注册时会生成一个 **8 位数字 UID**。其他用户可以通过 UID 搜索并邀请你加入群组。

### 账号会被禁用吗？

服务器管理员可以将违规账号设置为 DISABLED 状态，被禁用的账号无法登录或使用任何功能。
`,

  'faq-playback': `
## 播放问题

### 为什么选好文件后播放器没有显示？

请确认：
1. 文件格式是否被浏览器支持（推荐 MP4/H.264 视频、MP3 音频）
2. 文件大小是否正常（超大文件加载可能较慢）
3. 控制台是否有报错（F12 打开开发者工具查看）

### 视频/音频没有声音？

1. 检查浏览器是否**静音**了该标签页
2. 检查系统音量
3. 部分浏览器要求用户先与页面交互（点击）后才能播放音频

### 播放卡顿怎么办？

1. 确认 P2P 传输已启用（设置页）
2. 检查网络带宽
3. 尝试降低 P2P 速度限制
4. 如果是视频，关闭其他占用带宽的应用

### 为什么只能看到画面没有声音（或有声音没画面）？

这是浏览器对媒体编码格式的支持问题：
- 推荐使用 **MP4 (H.264 + AAC)** 格式，兼容性最好
- 避免使用 MKV 容器（部分浏览器不支持）
- 音乐推荐 **MP3 / AAC / FLAC** 格式

### Apple 设备（iPhone/iPad）上播放异常？

iOS Safari 对媒体自动播放有限制。请确保：
1. 先手动点击播放按钮
2. 不要使用"静音"模式
3. 避免在后台标签页中播放
`,

  'faq-connection': `
## 连接与同步

### 加入会话后无法同步？

1. 确认顶栏同步状态标签显示「同步良好」（偏差 < 100ms）
2. 如果显示「偏差 > 500ms」，点击「同步」按钮手动触发
3. 检查 WebSocket 连接是否正常（F12 → Network → WS）
4. 确认你选择了**相同内容**的本地媒体文件

### WebSocket 经常断开？

1. 检查网络稳定性
2. 如果使用了代理/VPN，尝试关闭
3. 确认服务器 Nginx 的 WebSocket 超时配置足够长

### 时钟同步偏差很大？

1. 检查本地系统时间是否正确
2. 在设置中增大「同步偏差阈值」
3. 手动点击「同步」按钮

### 所有人在会话中掉线了？

如果所有人因网络问题同时断开 WebSocket：
- 会话进入「全部离线」追踪状态
- 如果 **10 分钟内**有人重新连入，会话继续
- 如果 **10 分钟后**仍无人连入，会话自动销毁
`,

  'faq-wlp': `
## 离线包问题

### 导入离线包时提示「密钥已过期」？

.wlk 密钥文件有有效期限制。过期后**永久无法解密**，需要导出方重新生成。

### 导入时提示「解密失败」？

可能的原因：
1. **.wlk 密钥和 .wlp 文件不匹配**（通常是一起生成的配对文件）
2. **文件损坏**——传输过程中文件不完整
3. **群组不匹配**——.wlp 绑定到特定群组，你不在该群组中

### 没有密钥的离线包能用吗？

可以。导出时取消勾选 **AES-256-GCM 加密**，生成的 .wlp 无需密钥即可导入。但这样做**不加密不安全**，仅建议在信任环境中使用。

### 永久密钥和限时密钥的区别？

| 特性 | 永久密钥 | 限时密钥 |
|------|---------|---------|
| 有效期 | 永不过期 | 1h/24h/7d/30d |
| 场景 | 离线长期使用 | 限时分享 |
| .wlk 内容 | 密钥 + expiresAt=0 | 密钥 + 具体过期时间 |

### .wlk 文件丢失了怎么办？

**无法恢复。** 密钥只在 .wlk 文件中，服务器不存储。建议将 .wlk 和 .wlp 分开发送给接收者，防止同时丢失。
`,

  'faq-other': `
## 其他问题

### WLTogether 安全吗？

- **端到端加密**：媒体文件通过 AES-256-GCM 加密（离线包模式）
- **零服务端存储**：服务器不存储、不处理任何媒体文件
- **BCrypt 密码哈希**：用户密码不存储明文
- **JWT 认证**：2 小时 Access Token + 30 天 Refresh Token
- **群组隔离**：用户只能访问自己所在群组的数据

### 支持哪些浏览器？

推荐使用最新版 **Chrome** 或 **Edge**。Firefox 和 Safari 基本支持，但部分 WebRTC/P2P 功能可能受限。

### 手机能用吗？

- **Web 端**：手机浏览器可直接访问（响应式布局）
- **Android 端**：独立的 Kotlin + Jetpack Compose 应用（规划中）
- iOS Safari 部分功能受限

### 如何自建服务器？

参见「部署与运维」章节。项目完全开源，可以在自己的 Debian/Ubuntu 服务器上部署。

### 数据存储在哪里？

| 数据 | 存储位置 |
|------|---------|
| 用户账号 | 服务器数据库（MySQL/SQLite） |
| 聊天记录 | 服务器数据库 |
| 媒体文件 | **仅在本地设备**，不上传 |
| 密钥 | 仅在 .wlk 文件中或浏览器内存 |
| 播放位置 | 浏览器 localStorage |

### 如何反馈问题或贡献代码？

访问项目的 GitHub 仓库提交 Issue 或 Pull Request。
`
}

// ==================== Simple Markdown Renderer ====================
function renderMarkdown(md) {
  if (!md) return ''
  let html = md
    // Escape HTML entities
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    // Code blocks (```)
    .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
    // Inline code (`)
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    // Headers
    .replace(/^### (.+)$/gm, '<h3>$1</h3>')
    .replace(/^## (.+)$/gm, '<h2>$1</h2>')
    // Bold
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    // Unordered list items
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    // Horizontal rules
    .replace(/^---$/gm, '<hr>')
    // Blockquote
    .replace(/^> (.+)$/gm, '<blockquote>$1</blockquote>')

  // Wrap consecutive <li> in <ul>
  html = html.replace(/((?:<li>.*<\/li>\n?)+)/g, '<ul>$1</ul>')

  // Paragraphs: wrap lines that aren't already HTML tags
  html = html.replace(/^(?!<[a-zA-Z/])(.+)$/gm, (match) => {
    if (match.trim() === '') return ''
    return '<p>' + match + '</p>'
  })

  // Clean up extra newlines
  html = html.replace(/\n{3,}/g, '\n\n')

  return html
}

const renderedContent = computed(() => {
  const sectionId = activeSection.value
  const md = docSections[sectionId] || ''
  return renderMarkdown(md)
})

// ==================== Navigation ====================
function toggleCategory(title) {
  if (expandedCats.value.has(title)) {
    expandedCats.value.delete(title)
  } else {
    expandedCats.value.add(title)
  }
  // Trigger reactivity
  expandedCats.value = new Set(expandedCats.value)
}

function scrollToDoc(id) {
  activeSection.value = id
  nextTick(() => {
    if (contentEl.value) {
      contentEl.value.scrollTop = 0
    }
  })
}

// Triggered from header
function scrollToSection(id) {
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}

function onToggleTheme() {
  const next = isDark.value ? 'light' : 'dark'
  setTheme(next)
}

function onUserCommand(cmd) {
  if (cmd === 'logout') {
    disconnect()
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  } else if (cmd === 'settings') {
    router.push('/settings')
  }
}

onMounted(() => {
  // Check if there's a hash in the URL for direct linking
  const hash = window.location.hash?.replace('#', '')
  if (hash && docSections[hash]) {
    activeSection.value = hash
  }
})
</script>

<style scoped>
.docs-page {
  min-height: 100vh;
  background: var(--color-bg);
}

/* ====== Header (same style as landing page) ====== */
.docs-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 24px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: transparent;
  pointer-events: none;
  border-bottom: 1px solid var(--color-border);
  backdrop-filter: blur(14px) saturate(140%);
  -webkit-backdrop-filter: blur(14px) saturate(140%);
}

.docs-header > * {
  pointer-events: auto;
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-logo-link {
  display: flex;
  align-items: center;
  margin-right: 16px;
}

.header-logo {
  height: 36px;
  width: 36px;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s;
}

.header-logo:hover {
  transform: scale(1.08);
}

.header-btn {
  font-size: 14px;
  color: var(--color-text);
  border-radius: 8px;
  padding: 6px 12px;
  transition: background 0.15s;
}

.header-btn:hover {
  background: rgba(128, 128, 128, 0.12);
}

.avatar-btn {
  cursor: pointer;
  transition: transform 0.2s;
}

.avatar-btn:hover {
  transform: scale(1.08);
}

.login-btn {
  border-radius: 10px;
  font-size: 14px;
  padding: 8px 20px;
}

/* ====== Body Layout ====== */
.docs-body {
  display: flex;
  padding-top: 57px;
  min-height: 100vh;
}

/* ====== Sidebar ====== */
.docs-sidebar {
  width: 260px;
  flex-shrink: 0;
  border-right: 1px solid var(--color-border);
  background: var(--color-card-bg);
  overflow-y: auto;
  position: sticky;
  top: 57px;
  height: calc(100vh - 57px);
}

.sidebar-search {
  padding: 12px;
  position: sticky;
  top: 0;
  background: var(--color-card-bg);
  z-index: 2;
}

.sidebar-nav {
  padding: 0 8px 24px;
}

.nav-category {
  margin-bottom: 4px;
}

.nav-cat-title {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  border-radius: 6px;
  transition: background 0.15s;
  user-select: none;
}

.nav-cat-title:hover {
  background: rgba(128, 128, 128, 0.08);
}

.nav-cat-title .el-icon {
  font-size: 12px;
  flex-shrink: 0;
}

.nav-cat-items {
  padding-left: 22px;
}

.nav-item {
  padding: 6px 10px;
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.15s;
  line-height: 1.4;
}

.nav-item:hover {
  color: var(--color-text);
  background: rgba(128, 128, 128, 0.06);
}

.nav-item.active {
  color: var(--color-primary);
  background: rgba(64, 158, 255, 0.08);
  font-weight: 500;
}

/* ====== Content ====== */
.docs-content {
  flex: 1;
  overflow-y: auto;
  padding: 32px 48px;
  min-width: 0;
}

.docs-article {
  max-width: 780px;
}

/* Markdown rendered styles */
.docs-article :deep(h2) {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 20px 0;
  padding-bottom: 10px;
  border-bottom: 2px solid var(--color-primary);
}

.docs-article :deep(h3) {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
  margin: 28px 0 12px 0;
}

.docs-article :deep(p) {
  font-size: 15px;
  line-height: 1.8;
  color: var(--color-text-secondary);
  margin: 0 0 14px 0;
}

.docs-article :deep(strong) {
  color: var(--color-text);
}

.docs-article :deep(code) {
  background: rgba(128, 128, 128, 0.12);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'SF Mono', 'Cascadia Code', 'Consolas', monospace;
  color: var(--color-text);
}

.docs-article :deep(pre) {
  background: rgba(128, 128, 128, 0.08);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 14px 18px;
  overflow-x: auto;
  margin: 12px 0 18px;
}

.docs-article :deep(pre code) {
  background: none;
  padding: 0;
  font-size: 13px;
  line-height: 1.7;
}

.docs-article :deep(ul) {
  margin: 8px 0 16px;
  padding-left: 24px;
}

.docs-article :deep(li) {
  font-size: 15px;
  line-height: 1.8;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.docs-article :deep(blockquote) {
  border-left: 3px solid var(--color-primary);
  margin: 12px 0 18px;
  padding: 8px 16px;
  background: rgba(64, 158, 255, 0.05);
  border-radius: 0 6px 6px 0;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.docs-article :deep(hr) {
  border: none;
  border-top: 1px solid var(--color-border);
  margin: 24px 0;
}

.docs-article :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0 18px;
  font-size: 14px;
}

.docs-article :deep(th),
.docs-article :deep(td) {
  border: 1px solid var(--color-border);
  padding: 8px 12px;
  text-align: left;
}

.docs-article :deep(th) {
  background: rgba(128, 128, 128, 0.06);
  font-weight: 600;
  color: var(--color-text);
}

.docs-article :deep(td) {
  color: var(--color-text-secondary);
}
</style>
