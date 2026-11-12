<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <el-icon :size="34" color="#409eff"><Reading /></el-icon>
        <h1>智能出题与批改系统</h1>
        <p class="text-muted">AI 驱动的个性化练习与学习分析平台</p>
      </div>

      <el-tabs v-model="tab" stretch>
        <!-- 登录 -->
        <el-tab-pane label="登录" name="login">
          <el-form
            ref="loginRef"
            :model="loginForm"
            :rules="loginRules"
            label-position="top"
            @submit.prevent="onLogin"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                :prefix-icon="User"
                clearable
              />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
                @keyup.enter="onLogin"
              />
            </el-form-item>
            <el-button
              type="primary"
              class="submit-btn"
              :loading="loading"
              @click="onLogin"
            >
              登录
            </el-button>
          </el-form>
        </el-tab-pane>

        <!-- 注册 -->
        <el-tab-pane label="注册" name="register">
          <el-form
            ref="regRef"
            :model="regForm"
            :rules="regRules"
            label-position="top"
            @submit.prevent="onRegister"
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="regForm.username" placeholder="4-20 位字母数字" clearable />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="regForm.nickname" placeholder="选填，展示用名称" clearable />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="regForm.password" type="password" show-password placeholder="至少 6 位" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="regForm.confirmPassword"
                type="password"
                show-password
                placeholder="再次输入密码"
                @keyup.enter="onRegister"
              />
            </el-form-item>
            <el-button
              type="primary"
              class="submit-btn"
              :loading="loading"
              @click="onRegister"
            >
              注册并登录
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import { register } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const tab = ref('login')
const loading = ref(false)
const loginRef = ref()
const regRef = ref()

const loginForm = reactive({ username: '', password: '' })
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const regForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})
const regRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名 4-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, cb) =>
        value === regForm.password ? cb() : cb(new Error('两次密码不一致')),
      trigger: 'blur'
    }
  ]
}

async function onLogin() {
  const ok = await loginRef.value.validate().catch(() => false)
  if (!ok) return
  loading.value = true
  try {
    await auth.login({ username: loginForm.username, password: loginForm.password })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/dashboard'
    router.replace(redirect)
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function onRegister() {
  const ok = await regRef.value.validate().catch(() => false)
  if (!ok) return
  loading.value = true
  try {
    await register({ ...regForm })
    ElMessage.success('注册成功，正在登录…')
    await auth.login({ username: regForm.username, password: regForm.password })
    router.replace('/dashboard')
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
}
.login-card {
  width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 28px 32px 32px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);
}
.brand {
  text-align: center;
  margin-bottom: 12px;
}
.brand h1 {
  font-size: 20px;
  margin: 8px 0 4px;
}
.brand p {
  font-size: 12px;
  margin: 0 0 8px;
}
.submit-btn {
  width: 100%;
  margin-top: 4px;
}
</style>
