<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <h1>WLTogether</h1>
        <p>一起看，一起听</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <!-- Login -->
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top">
            <el-form-item label="用户名/邮箱" prop="account">
              <el-input v-model="loginForm.account" placeholder="输入用户名或邮箱" :prefix-icon="User" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="输入密码" :prefix-icon="Lock"
                @keydown.enter="onLogin" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" style="width: 100%" @click="onLogin">
                登录
              </el-button>
            </el-form-item>
          </el-form>
          <p class="form-footer">
            <el-button link type="primary" @click="activeTab = 'forgot'">忘记密码？</el-button>
            <span>还没有账号？<el-button link type="primary" @click="activeTab = 'register'">注册</el-button></span>
          </p>
        </el-tab-pane>

        <!-- Register -->
        <el-tab-pane label="注册" name="register">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="registerForm.email" placeholder="输入邮箱地址" :prefix-icon="Message" />
            </el-form-item>
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="3-50个字符" :prefix-icon="User" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="6-128个字符" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" style="width: 100%" @click="onRegister">
                注册
              </el-button>
            </el-form-item>
          </el-form>
          <p class="form-footer">
            <span>已有账号？<el-button link type="primary" @click="activeTab = 'login'">登录</el-button></span>
          </p>
        </el-tab-pane>

        <!-- Verify Email -->
        <el-tab-pane label="验证邮箱" name="verify">
          <el-form :model="verifyForm" :rules="verifyRules" label-position="top">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="verifyForm.email" placeholder="输入注册邮箱" :prefix-icon="Message" />
            </el-form-item>
            <el-form-item label="验证码" prop="code">
              <el-input v-model="verifyForm.code" placeholder="输入邮箱收到的验证码" :prefix-icon="Key" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" style="width: 100%" @click="onVerify">
                验证并登录
              </el-button>
            </el-form-item>
          </el-form>
          <p class="form-footer">
            <el-button link type="primary" :loading="resending" @click="onResendCode">重新发送验证码</el-button>
          </p>
        </el-tab-pane>

        <!-- Forgot Password -->
        <el-tab-pane label="忘记密码" name="forgot">
          <el-form :model="forgotForm" :rules="forgotRules" label-position="top">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="forgotForm.email" placeholder="输入注册邮箱" :prefix-icon="Message" />
            </el-form-item>
            <el-form-item label="验证码" prop="code">
              <el-input v-model="forgotForm.code" placeholder="输入邮箱收到的验证码" :prefix-icon="Key" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="forgotForm.newPassword" type="password" placeholder="6-128个字符" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" style="width: 100%" @click="onResetPassword">
                重置密码
              </el-button>
            </el-form-item>
          </el-form>
          <p class="form-footer">
            <el-button link type="primary" @click="activeTab = 'login'">返回登录</el-button>
          </p>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, Key } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useWebSocket } from '@/composables/useWebSocket'
import authApi from '@/api/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const { connect } = useWebSocket()

const activeTab = ref('login')
const loading = ref(false)
const resending = ref(false)

// Login form
const loginFormRef = ref(null)
const loginForm = reactive({ account: '', password: '' })
const loginRules = {
  account: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// Register form
const registerFormRef = ref(null)
const registerForm = reactive({ email: '', username: '', password: '' })
const registerRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名3-50个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 128, message: '密码6-128个字符', trigger: 'blur' }
  ]
}

// Verify form
const verifyForm = reactive({ email: '', code: '' })
const verifyRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

// Forgot password form
const forgotForm = reactive({ email: '', code: '', newPassword: '' })
const forgotRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 128, message: '密码6-128个字符', trigger: 'blur' }
  ]
}

async function onLogin() {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(loginForm.account, loginForm.password)
    connect()
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/groups')
  } catch (err) {
    const data = err.response?.data
    if (data?.error === 'EMAIL_NOT_VERIFIED') {
      verifyForm.email = loginForm.account.includes('@') ? loginForm.account : ''
      activeTab.value = 'verify'
      ElMessage.warning('请先验证邮箱')
    } else {
      ElMessage.error(data?.message || '登录失败')
    }
  } finally {
    loading.value = false
  }
}

async function onRegister() {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.register(registerForm.email, registerForm.username, registerForm.password)
    ElMessage.success('注册成功，请查收验证邮件')
    verifyForm.email = registerForm.email
    activeTab.value = 'verify'
  } catch (err) {
    const data = err.response?.data
    ElMessage.error(data?.message || '注册失败')
  } finally {
    loading.value = false
  }
}

async function onVerify() {
  if (!verifyForm.email || !verifyForm.code) {
    ElMessage.warning('请填写邮箱和验证码')
    return
  }

  loading.value = true
  try {
    await authStore.verify(verifyForm.email, verifyForm.code)
    connect()
    ElMessage.success('验证成功')
    router.push(route.query.redirect || '/groups')
  } catch (err) {
    const data = err.response?.data
    ElMessage.error(data?.message || '验证失败')
  } finally {
    loading.value = false
  }
}

async function onResendCode() {
  if (!verifyForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  resending.value = true
  try {
    await authApi.resendCode({ email: verifyForm.email })
    ElMessage.success('验证码已重新发送')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '发送失败')
  } finally {
    resending.value = false
  }
}

async function onResetPassword() {
  if (!forgotForm.email || !forgotForm.code || !forgotForm.newPassword) {
    ElMessage.warning('请填写完整信息')
    return
  }

  loading.value = true
  try {
    // First send forgot password to get code
    if (!forgotForm._sent) {
      await authApi.forgotPassword({ email: forgotForm.email })
      forgotForm._sent = true
      ElMessage.success('验证码已发送')
      loading.value = false
      return
    }
    await authApi.resetPassword({
      email: forgotForm.email,
      code: forgotForm.code,
      newPassword: forgotForm.newPassword
    })
    ElMessage.success('密码已重置，请重新登录')
    activeTab.value = 'login'
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '重置失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.login-brand {
  text-align: center;
  margin-bottom: 24px;
}

.login-brand h1 {
  font-size: 28px;
  color: #667eea;
  margin-bottom: 4px;
}

.login-brand p {
  color: #909399;
  font-size: 14px;
}

.login-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: -8px;
  font-size: 13px;
  color: #909399;
}
</style>
