<template>
  <el-container class="app-layout">
    <el-aside :width="collapsed ? '64px' : '210px'" class="app-aside">
      <div class="logo">
        <el-icon :size="22"><Reading /></el-icon>
        <span v-show="!collapsed" class="logo-text">智能出题批改</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        background-color="#20222a"
        text-color="#c0c4cc"
        active-text-color="#ffd04b"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="20" @click="collapsed = !collapsed">
            <Fold v-if="!collapsed" />
            <Expand v-else />
          </el-icon>
          <span class="page-title">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <el-tag v-if="roleLabel" size="small" type="info" effect="plain">
            {{ roleLabel }}
          </el-tag>
          <el-dropdown @command="onCommand">
            <span class="user-trigger">
              <el-avatar :size="30" :src="auth.user?.avatar">
                {{ auth.nickname?.charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ auth.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <keep-alive :max="6">
            <component :is="Component" :key="route.path" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)

// 从路由表提取侧边栏菜单（Layout 的 children）
const menuItems = computed(() => {
  const layoutRoute = router.options.routes.find((r) => r.path === '/')
  return (layoutRoute?.children || [])
    .filter((c) => !c.meta?.hidden)
    .map((c) => ({
      path: '/' + c.path,
      title: c.meta?.title,
      icon: c.meta?.icon
    }))
})

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '')
const roleLabel = computed(() => ({ 0: '学生', 1: '管理员' })[auth.role] || '')

async function onCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        type: 'warning',
        confirmButtonText: '退出',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
    await auth.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
.app-layout {
  height: 100vh;
}
.app-aside {
  background-color: #20222a;
  transition: width 0.2s;
  overflow-x: hidden;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  color: #ffd04b;
  border-bottom: 1px solid #2c2e36;
  white-space: nowrap;
}
.logo-text {
  font-weight: 700;
  font-size: 15px;
}
.app-aside :deep(.el-menu) {
  border-right: none;
}
.app-header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e6e8eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.collapse-btn {
  cursor: pointer;
  color: #606266;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}
.user-name {
  font-size: 14px;
  color: #303133;
}
.app-main {
  background: #f5f7fa;
  padding: 0;
  overflow-y: auto;
}
</style>
