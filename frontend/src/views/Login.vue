<template>
  <div class="auth-page">
    <!-- 背景装饰 -->
    <div class="bg-shapes">
      <div class="shape s1"></div>
      <div class="shape s2"></div>
      <div class="shape s3"></div>
      <div class="shape s4"></div>
    </div>

    <div class="auth-card-wrapper">
      <div class="auth-card">
        <div class="auth-brand">
          <span class="logo-icon">🏠</span>
          <span class="logo-text">筑梦家</span>
        </div>
        <h2 class="auth-title">欢迎回来</h2>
        <p class="auth-subtitle">登录以管理你的房屋设计项目</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="onSubmit" class="auth-form">
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
            />
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="loading"
            @click="onSubmit"
          >
            <span v-if="!loading">登录</span>
            <el-icon class="el-icon--right" v-if="!loading"><Right /></el-icon>
          </el-button>
        </el-form>

        <div class="auth-footer">
          还没有账号？
          <router-link to="/register" class="link">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { authApi } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await authApi.login(form)
    userStore.setAuth(data)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/projects')
  } catch (e) {
    /* 错误提示已由拦截器处理 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f3ff 0%, #f0faf0 50%, #eafaef 100%);
  position: relative;
  overflow: hidden;
}

/* ---- 背景装饰 ---- */
.bg-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.3;
}

.shape.s1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.25), transparent);
  top: -100px;
  right: -80px;
  animation: floatShape 8s ease-in-out infinite;
}

.shape.s2 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(103, 194, 58, 0.2), transparent);
  bottom: -60px;
  left: -60px;
  animation: floatShape 10s ease-in-out infinite reverse;
}

.shape.s3 {
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.15), transparent);
  top: 30%;
  left: 5%;
  animation: floatShape 12s ease-in-out infinite 2s;
}

.shape.s4 {
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(230, 162, 60, 0.12), transparent);
  bottom: 20%;
  right: 10%;
  animation: floatShape 9s ease-in-out infinite 1s;
}

@keyframes floatShape {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(20px, -30px) scale(1.05); }
  66% { transform: translate(-15px, 20px) scale(0.95); }
}

/* ---- 卡片 ---- */
.auth-card-wrapper {
  position: relative;
  z-index: 1;
  animation: cardEnter 0.6s ease;
}

@keyframes cardEnter {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.auth-card {
  width: 420px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 44px 40px 36px;
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.06),
    0 0 0 1px rgba(255, 255, 255, 0.6);
  transition: transform var(--el-transition-duration-normal, 0.3s);
}

.auth-card:hover {
  transform: translateY(-2px);
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}

.logo-icon {
  font-size: 28px;
}

.logo-text {
  font-weight: 700;
  font-size: 22px;
  background: linear-gradient(135deg, #409eff, #67c23a);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.auth-title {
  margin: 0 0 4px;
  font-size: 24px;
  font-weight: 700;
}

.auth-subtitle {
  margin: 0 0 28px;
  color: var(--color-text-secondary, #909399);
  font-size: 14px;
}

.auth-form {
  margin-bottom: 4px;
}

/* 输入框样式覆盖 */
.auth-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 12px;
  box-shadow: 0 0 0 1px var(--el-input-border-color, #dcdfe6) inset;
  transition: box-shadow var(--el-transition-duration-fast);
}

.auth-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--color-primary-light, #79bbff) inset;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--color-primary, #409eff) inset;
}

.auth-form :deep(.el-input__prefix) {
  margin-right: 6px;
}

.auth-form :deep(.el-input__prefix-inner) {
  color: var(--color-text-placeholder, #c0c4cc);
}

.submit-btn {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  margin-top: 4px;
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.3);
  transition: all var(--el-transition-duration-fast);
}

.submit-btn:hover {
  box-shadow: 0 8px 28px rgba(64, 158, 255, 0.4);
  transform: translateY(-1px);
}

.auth-footer {
  margin-top: 24px;
  text-align: center;
  color: var(--color-text-secondary, #909399);
  font-size: 14px;
}

.link {
  color: var(--color-primary, #409eff);
  font-weight: 500;
  transition: color var(--el-transition-duration-fast);
}

.link:hover {
  color: var(--color-primary-dark, #2c6bb5);
  text-decoration: underline;
}

@media (max-width: 480px) {
  .auth-card {
    width: calc(100vw - 32px);
    padding: 32px 24px 28px;
  }
}
</style>
