<template>
  <div class="page-container">
    <el-row :gutter="16">
      <!-- 基本信息 -->
      <el-col :md="14" :xs="24">
        <el-card shadow="never" class="mb-12">
          <template #header><span class="card-title">基本资料</span></template>
          <el-form ref="profileRef" :model="profile" :rules="profileRules" label-width="80px" v-loading="loading">
            <el-form-item label="用户名">
              <el-input :model-value="auth.user?.username" disabled />
            </el-form-item>
            <el-form-item label="角色">
              <el-tag effect="plain">{{ roleLabel }}</el-tag>
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="profile.nickname" placeholder="展示用名称" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profile.email" placeholder="选填" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profile.phone" placeholder="选填" />
            </el-form-item>
            <el-form-item label="头像地址" prop="avatar">
              <el-input v-model="profile.avatar" placeholder="选填，图片 URL" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingProfile" @click="onSaveProfile">保存资料</el-button>
              <el-button @click="resetProfile">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 修改密码 -->
      <el-col :md="10" :xs="24">
        <el-card shadow="never" class="mb-12">
          <template #header><span class="card-title">修改密码</span></template>
          <el-form ref="pwdRef" :model="pwd" :rules="pwdRules" label-width="90px">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="pwd.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwd.newPassword" type="password" show-password placeholder="至少 6 位" />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="pwd.confirmPassword" type="password" show-password @keyup.enter="onChangePwd" />
            </el-form-item>
            <el-form-item>
              <el-button type="warning" :loading="savingPwd" @click="onChangePwd">修改密码</el-button>
            </el-form-item>
          </el-form>
          <el-alert type="info" :closable="false" show-icon title="修改成功后需重新登录" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import { getUserInfo, updateProfile, updatePassword } from '@/api/user'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const savingProfile = ref(false)
const savingPwd = ref(false)
const profileRef = ref()
const pwdRef = ref()

const roleLabel = computed(() => ({ 0: '学生', 1: '管理员' })[auth.role] || '—')

const profile = reactive({ nickname: '', email: '', phone: '', avatar: '' })
const profileRules = {
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const pwd = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (r, v, cb) => (v === pwd.newPassword ? cb() : cb(new Error('两次密码不一致'))),
      trigger: 'blur'
    }
  ]
}

function fillProfile(vo) {
  profile.nickname = vo.nickname || ''
  profile.email = vo.email || ''
  profile.phone = vo.phone || ''
  profile.avatar = vo.avatar || ''
}

async function loadUser() {
  loading.value = true
  try {
    const vo = await getUserInfo()
    auth.setUser(vo)
    fillProfile(vo)
  } finally {
    loading.value = false
  }
}

function resetProfile() {
  if (auth.user) fillProfile(auth.user)
}

async function onSaveProfile() {
  const ok = await profileRef.value.validate().catch(() => false)
  if (!ok) return
  savingProfile.value = true
  try {
    const vo = await updateProfile({ ...profile })
    if (vo) auth.setUser(vo)
    ElMessage.success('资料已更新')
    loadUser()
  } finally {
    savingProfile.value = false
  }
}

async function onChangePwd() {
  const ok = await pwdRef.value.validate().catch(() => false)
  if (!ok) return
  savingPwd.value = true
  try {
    await updatePassword({ ...pwd })
    ElMessage.success('密码已修改，请重新登录')
    auth.clear()
    router.replace('/login')
  } finally {
    savingPwd.value = false
  }
}

onMounted(loadUser)
</script>
