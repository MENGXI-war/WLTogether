<template>
  <div class="landing-page">
    <!--
      ====== 更换背景图片 ======
      将 background-image 改为你的图片路径，例如：
      background-image: url('/bg.jpg');
      或者使用网络图片：
      background-image: url('https://example.com/bg.jpg');
      如果只需要纯色背景，删除 background-image 这一行即可。
    -->
    <div class="landing-bg" />

    <!-- Top bar -->
    <header class="landing-header">
      <div class="header-left">
        <router-link to="/" class="header-logo-link">
          <img src="/Logs.png" alt="WLTogether" class="header-logo" />
        </router-link>
        <el-button text class="header-btn" @click="scrollToSection('home')">
          <el-icon><HomeFilled /></el-icon> 首页
        </el-button>
        <el-button text class="header-btn" @click="router.push('/docs')">
          <el-icon><QuestionFilled /></el-icon> 常见问题
        </el-button>
        <el-button text class="header-btn" @click="onToggleTheme">
          <el-icon><component :is="isDark ? Sunny : Moon" /></el-icon> 主题切换
        </el-button>
        <el-button text class="header-btn" @click="scrollToSection('about')">
          <el-icon><InfoFilled /></el-icon> 关于
        </el-button>
      </div>
      <div class="header-right">
        <el-button type="primary" class="login-btn" @click="router.push('/login')">
          <el-icon><UserFilled /></el-icon> 注册 / 登录
        </el-button>
      </div>
    </header>

    <!-- Hero section -->
    <main class="landing-hero">
      <div class="hero-content">
        <h1 class="hero-title">WLTogether</h1>
        <p class="hero-subtitle">异地同步媒体播放器</p>
        <el-button
          v-if="!authStore.isAuthenticated"
          type="primary"
          size="large"
          class="hero-cta"
          @click="router.push('/login')"
        >
          <el-icon><UserFilled /></el-icon> 注册 / 登录
        </el-button>
        <el-button
          v-else
          type="primary"
          size="large"
          class="hero-cta"
          @click="router.push('/groups')"
        >
          <el-icon><HomeFilled /></el-icon> 进入主页
        </el-button>
      </div>
    </main>

    <!-- FAQ section -->
    <section id="faq" class="landing-section">
      <div class="section-container">
        <h2 class="section-title">常见问题</h2>
        <div class="faq-list">
          <div class="faq-item" v-for="item in faqItems" :key="item.q">
            <h4 class="faq-q">{{ item.q }}</h4>
            <p class="faq-a">{{ item.a }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- About section -->
    <section id="about" class="landing-section">
      <div class="section-container">
        <h2 class="section-title">关于 WLTogether</h2>
        <div class="about-content">
          <p>WLTogether 是一个私有自托管网络媒体同步播放器。</p>
          <p>支持视频和音频的跨设备同步播放，内置聊天和文件传输功能。</p>
          <p>服务器不存储、不处理任何媒体文件 —— 您的数据始终在您手中。</p>
          <p class="version-text">版本 0.1.0</p>
        </div>
      </div>
    </section>

    <PageFooter />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { HomeFilled, QuestionFilled, Sunny, Moon, InfoFilled, UserFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useTheme } from '@/composables/useTheme'
import PageFooter from '@/components/PageFooter.vue'

const router = useRouter()
const authStore = useAuthStore()
const { isDark, setTheme } = useTheme()

const faqItems = [
  { q: 'WLTogether 是什么？', a: 'WLTogether 是一个私有自托管的网络媒体同步播放器，让您和朋友们可以跨地域同步观看视频或聆听音乐。' },
  { q: '需要安装什么软件？', a: '只需一个现代浏览器（推荐 Chrome 或 Edge），无需安装任何客户端。手机上也可以直接使用浏览器访问。' },
  { q: '媒体文件会上传到服务器吗？', a: '不会。WLTogether 的服务器仅负责协调同步，媒体文件始终在您的本地设备上，不会上传到任何地方。' },
  { q: '如何与朋友同步观看？', a: '注册账号 → 创建群组 → 邀请朋友加入 → 发起同步会话 → 选择本地媒体文件即可同步播放。' },
  { q: '什么是离线包？', a: '离线包(.wlp)是加密的媒体文件容器，可以脱离服务器离线分享。支持 AES-256-GCM 加密，密钥(.wlk)可作为凭证分发给接收者。' },
  { q: 'WLTogether 免费吗？', a: '完全开源免费。代码托管在 GitHub，您可以自行部署在自己的服务器上。' }
]

function onToggleTheme() {
  const next = isDark.value ? 'light' : 'dark'
  setTheme(next)
}

function scrollToSection(id) {
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}
</script>

<style scoped>
.landing-page {
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
}

/*
  ====== 背景配置 ======
  默认：纯白 / 深色 #1a1a2e
  更换为图片：取消下面 .landing-bg 中 background-image 的注释，
  并将 url 替换为你的图片路径。
*/
.landing-bg {
  position: fixed;
  inset: 0;
  z-index: -1;
  background: var(--color-bg);
  /* 更换为图片背景（取消下面这行的注释并替换路径）: */
  /* background: url('/bg.jpg') center / cover no-repeat; */
}

/* ====== Header ====== */
.landing-header {
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
}

.landing-header > * {
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
  height: 40px;
  width: 40px;
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

.login-btn {
  border-radius: 10px;
  font-size: 14px;
  padding: 8px 20px;
}

/* ====== Hero ====== */
.landing-hero {
  min-height: 100vh;
  display: flex;
  align-items: center;
  padding: 0 10vw;
}

.hero-content {
  max-width: 560px;
}

.hero-title {
  font-size: 64px;
  font-weight: 800;
  color: #409eff;
  margin: 0 0 8px 0;
  letter-spacing: -1px;
  line-height: 1.1;
}

.hero-subtitle {
  font-size: 16px;
  color: var(--color-text-secondary);
  margin: 0 0 32px 0;
  line-height: 1.6;
}

.hero-cta {
  border-radius: 12px;
  font-size: 16px;
  padding: 14px 40px;
  height: auto;
}

/* ====== Sections ====== */
.landing-section {
  padding: 80px 10vw;
  position: relative;
  z-index: 1;
}

.section-container {
  max-width: 800px;
}

.section-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 32px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid #409eff;
  display: inline-block;
}

.faq-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.faq-item {
  background: var(--color-card-bg);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 16px 20px;
}

.faq-q {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 8px 0;
}

.faq-a {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.7;
  margin: 0;
}

.about-content {
  background: var(--color-card-bg);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 20px 24px;
}

.about-content p {
  font-size: 15px;
  color: var(--color-text-secondary);
  line-height: 1.8;
  margin: 0 0 8px 0;
}

.about-content p:last-child {
  margin-bottom: 0;
}

.version-text {
  margin-top: 16px !important;
  font-size: 13px !important;
  color: var(--color-text-secondary);
  opacity: 0.6;
}
</style>
