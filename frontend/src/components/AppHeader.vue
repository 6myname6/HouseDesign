<template>
  <header class="app-header" :class="{ scrolled: isScrolled }">
    <div class="header-inner">
      <div class="logo" @click="router.push('/')">
        <span class="logo-icon">🏠</span>
        <span class="logo-text">筑梦家</span>
      </div>
      <nav class="nav">
        <router-link to="/" class="nav-item" active-class="nav-active" exact-active-class="nav-active">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </router-link>
        <router-link to="/community" class="nav-item" active-class="nav-active">
          <el-icon><ChatRound /></el-icon>
          <span>装修小圈</span>
        </router-link>
        <router-link to="/projects" class="nav-item" active-class="nav-active">
          <el-icon><FolderOpened /></el-icon>
          <span>我的项目</span>
        </router-link>
        <router-link to="/me" class="nav-item" active-class="nav-active">
          <el-icon><UserFilled /></el-icon>
          <span>我的</span>
        </router-link>
      </nav>
      <div class="actions">
        <template v-if="userStore.isLoggedIn">
          <el-dropdown @command="onCommand" trigger="click">
            <span class="user-chip">
              <span class="avatar-small" :style="{ background: avatarColor }">
                {{ avatarText }}
              </span>
              <span class="user-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="projects">
                  <el-icon><FolderOpened /></el-icon> 我的项目
                </el-dropdown-item>
                <el-dropdown-item command="me">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button class="btn-login" text @click="router.push('/login')">登录</el-button>
          <el-button class="btn-register" type="primary" @click="router.push('/register')">
            注册
            <el-icon class="el-icon--right"><Right /></el-icon>
          </el-button>
        </template>
      </div>
    </div>
    <!-- 底部光晕装饰 -->
    <div class="header-glow"></div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const isScrolled = ref(false)

function onScroll() {
  isScrolled.value = window.scrollY > 20
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
})
onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
})

const avatarText = computed(() => {
  const n = userStore.userInfo?.nickname || userStore.userInfo?.username || '?'
  return n.trim().charAt(0).toUpperCase()
})

const avatarColor = computed(() => {
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#9254de', '#13c2c2']
  const s = userStore.userInfo?.username || '?'
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) % colors.length
  return colors[h]
})

function onCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  } else if (cmd === 'projects') {
    router.push('/projects')
  } else if (cmd === 'me') {
    router.push('/me')
  }
}
</script>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(235, 238, 245, 0.5);
  transition: background var(--el-transition-duration-fast), box-shadow var(--el-transition-duration-fast);
}

.app-header.scrolled {
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 1px 20px rgba(0, 0, 0, 0.06);
}

.header-glow {
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(64, 158, 255, 0.2), rgba(103, 194, 58, 0.2), transparent);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 8px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: 700;
  font-size: 20px;
  flex-shrink: 0;
  user-select: none;
  transition: opacity 0.2s;
}

.logo:active {
  opacity: 0.7;
}

.logo-icon {
  font-size: 26px;
  line-height: 1;
}

.logo-text {
  background: linear-gradient(135deg, #409eff, #67c23a);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.nav {
  display: flex;
  gap: 4px;
  flex: 1;
  justify-content: center;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  color: var(--color-text-secondary, #606266);
  font-size: 14px;
  font-weight: 500;
  transition: all var(--el-transition-duration-fast);
  position: relative;
}

.nav-item:hover {
  color: var(--color-primary, #409eff);
  background: rgba(64, 158, 255, 0.06);
}

.nav-active {
  color: var(--color-primary, #409eff) !important;
  background: rgba(64, 158, 255, 0.08);
}

.nav-active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  border-radius: 2px;
  background: var(--color-primary, #409eff);
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.btn-login {
  font-weight: 500;
}

.btn-register {
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  transition: all var(--el-transition-duration-fast);
}

.btn-register:hover {
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.4);
  transform: translateY(-1px);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--color-text-primary, #303133);
  outline: none;
  padding: 4px 12px 4px 4px;
  border-radius: 24px;
  transition: background var(--el-transition-duration-fast);
}

.user-chip:hover {
  background: rgba(64, 158, 255, 0.06);
}

.avatar-small {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  flex-shrink: 0;
}

.user-name {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
  font-size: 14px;
}

.arrow {
  font-size: 12px;
  color: var(--color-text-placeholder, #c0c4cc);
  transition: transform var(--el-transition-duration-fast);
}

.user-chip:hover .arrow {
  transform: rotate(180deg);
}

/* 下拉菜单覆盖 */
:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
</style>
