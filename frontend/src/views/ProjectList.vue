<template>
  <div class="project-list-page">
    <AppHeader />

    <div class="page-header">
      <div class="page-container">
        <div class="header-row">
          <div class="header-left">
            <h1 class="page-title">我的项目</h1>
            <p class="page-desc text-muted">管理你的所有房屋设计项目</p>
          </div>
          <el-button type="primary" size="large" class="create-btn" @click="router.push('/projects/new')">
            <el-icon><Plus /></el-icon>
            <span>新建项目</span>
          </el-button>
        </div>
      </div>
    </div>

    <div class="page-container">
      <!-- 空状态 -->
      <el-empty v-if="!loading && projects.length === 0" class="empty-state" :image-size="140">
        <template #image>
          <div class="empty-illustration">
            <span class="empty-emoji">🏗️</span>
          </div>
        </template>
        <template #description>
          <p class="empty-text">还没有项目，先上传一张设计图开始吧</p>
        </template>
        <el-button type="primary" size="large" @click="router.push('/projects/new')">
          <el-icon><Upload /></el-icon> 上传设计图
        </el-button>
      </el-empty>

      <!-- 项目列表 -->
      <div v-loading="loading" element-loading-text="加载中…" class="project-grid stagger-enter">
        <div
          v-for="(p, i) in projects"
          :key="p.id"
          class="project-card"
          :style="{ animationDelay: `${i * 0.06}s` }"
          @click="router.push(`/projects/${p.id}`)"
        >
          <div class="card-thumb">
            <img :src="p.designImageUrl" :alt="p.name" loading="lazy" />
            <div class="thumb-overlay">
              <span class="overlay-text">
                <el-icon><View /></el-icon> 查看详情
              </span>
            </div>
            <el-tag v-if="p.styleLabel" class="style-tag" size="small" effect="dark" round>
              {{ p.styleLabel }}
            </el-tag>
          </div>
          <div class="card-body">
            <h3 class="card-title">{{ p.name }}</h3>
            <p class="card-desc">{{ p.description || '暂无描述' }}</p>
            <div class="card-footer">
              <span class="card-date">
                <el-icon><Calendar /></el-icon>
                {{ formatDate(p.createdAt) }}
              </span>
              <el-button
                size="small"
                text
                type="danger"
                class="delete-btn"
                @click.stop="onDelete(p)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import { projectApi } from '@/api'

const router = useRouter()
const projects = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    projects.value = await projectApi.list()
  } finally {
    loading.value = false
  }
}

async function onDelete(p) {
  try {
    await ElMessageBox.confirm(
      `确定删除项目「${p.name}」吗？删除后无法恢复。`,
      '确认删除',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }
    )
    await projectApi.remove(p.id)
    ElMessage.success('已删除')
    load()
  } catch {
    /* 取消则不操作 */
  }
}

function formatDate(s) {
  if (!s) return ''
  const d = new Date(s)
  if (isNaN(d.getTime())) return s.replace('T', ' ').slice(0, 16)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(load)
</script>

<style scoped>
.project-list-page {
  min-height: 100vh;
  background: var(--color-bg, #f5f7fa);
}

/* ---- 页面标题 ---- */
.page-header {
  background: var(--color-bg-white, #fff);
  border-bottom: 1px solid var(--color-border-lighter, #ebeef5);
  padding: 28px 0 20px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
}

.page-desc {
  margin: 4px 0 0;
  font-size: 14px;
}

.create-btn {
  border-radius: 12px;
  padding: 12px 24px;
  font-weight: 600;
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.3);
  transition: all var(--el-transition-duration-fast);
}

.create-btn:hover {
  box-shadow: 0 8px 28px rgba(64, 158, 255, 0.4);
  transform: translateY(-1px);
}

/* ---- 空状态 ---- */
.empty-state {
  margin-top: 48px;
}

.empty-illustration {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e8f3ff, #eafaef);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
}

.empty-emoji {
  font-size: 48px;
}

.empty-text {
  font-size: 15px;
  margin: 0 0 16px;
}

/* ---- 项目卡片网格 ---- */
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  min-height: 200px;
  padding: 8px 0 48px;
}

.project-card {
  background: var(--color-bg-white, #fff);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.06));
  cursor: pointer;
  transition: all var(--el-transition-duration-normal, 0.3s);
  opacity: 0;
  transform: translateY(16px);
  animation: cardSlideUp 0.45s ease forwards;
}

@keyframes cardSlideUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.project-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-lg, 0 12px 40px rgba(0,0,0,0.1));
}

/* ---- 缩略图 ---- */
.card-thumb {
  position: relative;
  height: 180px;
  background: #f0f2f5;
  overflow: hidden;
}

.card-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--el-transition-duration-normal, 0.3s);
}

.project-card:hover .card-thumb img {
  transform: scale(1.06);
}

.thumb-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity var(--el-transition-duration-normal, 0.3s);
}

.project-card:hover .thumb-overlay {
  opacity: 1;
}

.overlay-text {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  padding: 8px 20px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(4px);
}

.style-tag {
  position: absolute;
  top: 12px;
  right: 12px;
  border: none;
}

/* ---- 卡片内容 ---- */
.card-body {
  padding: 16px 18px 14px;
}

.card-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  margin: 0 0 14px;
  font-size: 13px;
  color: var(--color-text-secondary, #909399);
  height: 36px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  border-top: 1px solid var(--color-border-lighter, #ebeef5);
  padding-top: 12px;
}

.card-date {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--color-text-placeholder, #c0c4cc);
}

.delete-btn {
  transition: color var(--el-transition-duration-fast);
}

.delete-btn:hover {
  transform: scale(1.1);
}
</style>
