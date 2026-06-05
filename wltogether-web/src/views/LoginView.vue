<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <img src="/Logs.png" alt="WLTogether" class="login-logo" />
        <h1>WLTogether</h1>
      </div>

      <h2 class="view-title">{{ viewTitle }}</h2>

      <!-- ========== 登录 ========== -->
      <template v-if="currentView === 'login'">
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
          <el-button link type="primary" @click="currentView = 'forgot'">忘记密码？</el-button>
          <span>还没有账号？<el-button link type="primary" @click="currentView = 'register'">注册</el-button></span>
        </p>
      </template>

      <!-- ========== 注册（含验证码） ========== -->
      <template v-if="currentView === 'register'">
        <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top">
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="registerForm.email" placeholder="输入邮箱地址" :prefix-icon="Message">
              <template #append>
                <el-button :loading="resending" @click="onSendRegisterCode" :disabled="!registerForm.email">
                  发送验证码
                </el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="验证码" prop="code">
            <el-input v-model="registerForm.code" placeholder="输入邮箱收到的验证码" :prefix-icon="Key" />
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="registerForm.username" placeholder="3-50个字符" :prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="registerForm.password" type="password" placeholder="6-128个字符" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入密码" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" style="width: 100%" @click="onRegister">
              注册
            </el-button>
          </el-form-item>
        </el-form>
        <p class="form-footer">
          <el-button link type="primary" @click="currentView = 'login'">返回登录</el-button>
        </p>
      </template>

      <!-- ========== 验证邮箱（注册后自动跳转） ========== -->
      <template v-if="currentView === 'verify'">
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
      </template>

      <!-- ========== 忘记密码 ========== -->
      <template v-if="currentView === 'forgot'">
        <el-form ref="forgotFormRef" :model="forgotForm" :rules="forgotRules" label-position="top">
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="forgotForm.email" placeholder="输入注册邮箱" :prefix-icon="Message" />
          </el-form-item>
          <el-form-item label="验证码" prop="code">
            <el-input v-model="forgotForm.code" placeholder="输入邮箱收到的验证码" :prefix-icon="Key" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="forgotForm.newPassword" type="password" placeholder="6-128个字符" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="forgotForm.confirmPassword" type="password" placeholder="再次输入新密码" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" style="width: 100%" @click="onResetPassword">
              重置密码
            </el-button>
          </el-form-item>
        </el-form>
        <p class="form-footer">
          <el-button link type="primary" @click="currentView = 'login'">返回登录</el-button>
        </p>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
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

const currentView = ref('login')
const viewTitle = computed(() => {
  const titles = { login: '登录', register: '注册', verify: '验证邮箱', forgot: '忘记密码' }
  return titles[currentView.value] || ''
})
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
const registerForm = reactive({ email: '', code: '', username: '', password: '', confirmPassword: '' })
const validateRegisterConfirm = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}
const registerRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名3-50个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 128, message: '密码6-128个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateRegisterConfirm, trigger: 'blur' }
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
const forgotFormRef = ref(null)
const forgotForm = reactive({ email: '', code: '', newPassword: '', confirmPassword: '' })
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== forgotForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}
const forgotRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 128, message: '密码6-128个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
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
      currentView.value = 'verify'
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

  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    await authStore.register(registerForm.email, registerForm.username, registerForm.password)
    await authStore.verify(registerForm.email, registerForm.code)
    connect()
    ElMessage.success('注册并验证成功')
    router.push(route.query.redirect || '/groups')
  } catch (err) {
    const data = err.response?.data
    ElMessage.error(data?.message || '注册失败')
  } finally {
    loading.value = false
  }
}

async function onSendRegisterCode() {
  if (!registerForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  resending.value = true
  try {
    await authApi.resendCode({ email: registerForm.email, type: 'REGISTER' })
    ElMessage.success('验证码已发送')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '发送失败')
  } finally {
    resending.value = false
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
  const valid = await forgotFormRef.value?.validate().catch(() => false)
  if (!valid) return

  if (forgotForm.newPassword !== forgotForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
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
    currentView.value = 'login'
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
  background: var(--color-card-bg);
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.login-brand {
  text-align: center;
  margin-bottom: 24px;
}

.login-logo {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  margin-bottom: 12px;
}

.login-brand h1 {
  font-size: 28px;
  color: #667eea;
  margin-bottom: 4px;
}

.view-title {
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
}

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: -8px;
  font-size: 13px;
  color: var(--color-text-secondary);
}
</style>
