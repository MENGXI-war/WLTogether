<template>
  <footer class="page-footer">
    <div class="footer-inner">
      <!-- Left: brand + description -->
      <div class="footer-col footer-brand">
        <div class="footer-logo-row">
          <img src="/Logs.png" alt="WLTogether" class="footer-logo" />
          <span class="footer-name">WLTogether</span>
        </div>
        <p class="footer-desc">私有自托管网络媒体同步播放器。<br/>您的数据始终在您手中。</p>
      </div>

      <!-- Center: links -->
      <div class="footer-col footer-links">
        <h4 class="footer-col-title">快速链接</h4>
        <router-link to="/docs" class="footer-link">帮助文档</router-link>
        <router-link to="/docs#faq-account" class="footer-link">常见问题</router-link>
        <router-link to="/docs#deploy" class="footer-link">部署指南</router-link>
      </div>

      <!-- Right: about -->
      <div class="footer-col footer-about">
        <h4 class="footer-col-title">关于</h4>
        <p class="footer-text">WLTogether 是一个开源项目，采用自托管架构。服务器不存储、不处理任何媒体文件。所有媒体内容仅在客户端之间 P2P 传输。</p>
      </div>
    </div>

    <!-- Bottom bar -->
    <div class="footer-bottom">
      <div class="footer-bottom-inner">
        <span>&copy; {{ currentYear }} WLTogether. Open Source Project.</span>
        <!--
          ====== 备案信息 ======
          如果在中国大陆部署，请取消下面这行的注释并填写备案号：
        -->
        <!-- <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener">ICP备案号</a> -->
        <span class="footer-sep">|</span>
        <span class="footer-disclaimer">
          本软件仅供学习、研究及合法用途。使用者须遵守所在地法律法规。
        </span>
      </div>
    </div>
  </footer>
</template>

<script setup>
const currentYear = new Date().getFullYear()
</script>

<style scoped>
.page-footer {
  background: #1a1a2e;
  color: #c8c8d8;
  font-size: 14px;
  position: relative;
  z-index: 1;
}

.footer-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 48px 32px 32px;
  display: flex;
  gap: 48px;
  flex-wrap: wrap;
}

.footer-col {
  min-width: 180px;
}

.footer-brand {
  flex: 1.2;
  min-width: 240px;
}

.footer-logo-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.footer-logo {
  height: 32px;
  width: 32px;
  border-radius: 6px;
}

.footer-name {
  font-size: 18px;
  font-weight: 700;
  color: #e8e8f0;
}

.footer-desc {
  font-size: 13px;
  line-height: 1.8;
  color: #9090a8;
  margin: 0;
}

.footer-col-title {
  font-size: 14px;
  font-weight: 600;
  color: #e0e0ea;
  margin: 0 0 14px 0;
}

.footer-link {
  display: block;
  color: #9090a8;
  text-decoration: none;
  font-size: 13px;
  line-height: 2;
  transition: color 0.15s;
}

.footer-link:hover {
  color: #79bbff;
}

.footer-about {
  flex: 1.3;
  min-width: 220px;
}

.footer-text {
  font-size: 13px;
  line-height: 1.8;
  color: #9090a8;
  margin: 0;
}

/* Bottom bar */
.footer-bottom {
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.footer-bottom-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 16px 32px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 16px;
  font-size: 12px;
  color: #686888;
}

.footer-bottom-inner a {
  color: #686888;
  text-decoration: none;
}

.footer-bottom-inner a:hover {
  color: #9090a8;
}

.footer-sep {
  color: rgba(255, 255, 255, 0.1);
}

.footer-disclaimer {
  color: #585878;
}
</style>
