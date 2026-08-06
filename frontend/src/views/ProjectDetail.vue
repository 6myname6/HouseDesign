<template>
  <div class="detail-page">
    <AppHeader />

    <div class="page-container" v-loading="loading" element-loading-text="加载项目…">
      <template v-if="project">
        <!-- 返回导航 -->
        <div class="breadcrumb">
          <el-button text @click="router.push('/projects')">
            <el-icon><ArrowLeft /></el-icon>
            返回项目列表
          </el-button>
        </div>

        <!-- 项目头部 -->
        <div class="detail-head">
          <div class="head-left">
            <div class="title-line">
              <h1 class="project-name">{{ project.name }}</h1>
              <el-tag v-if="project.styleLabel" effect="light" type="warning" round>
                {{ project.styleLabel }}
              </el-tag>
            </div>
            <p class="project-desc text-muted">{{ project.description || '暂无描述' }}</p>
            <span class="project-date text-muted">
              <el-icon><Calendar /></el-icon>
              创建于 {{ formatDate(project.createdAt) }}
            </span>
          </div>
          <div class="head-actions">
            <el-button
              type="primary"
              size="large"
              :loading="generating"
              @click="onGenerate"
              class="generate-btn"
            >
              <el-icon><MagicStick /></el-icon>
              <span>生成 3D 效果</span>
            </el-button>
          </div>
        </div>

        <div class="detail-grid">
          <!-- 左：设计图 -->
          <div class="panel design-panel">
            <div class="panel-header">
              <h3 class="panel-title">
                <el-icon><Picture /></el-icon>
                设计图
              </h3>
            </div>
            <div class="image-wrapper">
              <img :src="project.designImageUrl" class="design-image" />
              <div class="image-badge">原始设计图</div>
            </div>
          </div>

          <!-- 右：生成记录 -->
          <div class="panel gen-panel">
            <div class="panel-header">
              <h3 class="panel-title">
                <el-icon><Clock /></el-icon>
                生成记录
              </h3>
              <el-tag v-if="generations.length" size="small" effect="plain" round>
                {{ generations.length }} 条记录
              </el-tag>
            </div>

            <el-empty
              v-if="generations.length === 0"
              description="还没有生成记录"
              :image-size="80"
            >
              <template #image>
                <div class="empty-gen-icon">
                  <el-icon><MagicStick /></el-icon>
                </div>
              </template>
              <p class="empty-gen-text">点击右上角「生成 3D 效果」开始</p>
            </el-empty>

            <div v-else class="gen-list">
              <div v-for="g in generations" :key="g.id" class="gen-item" :class="[g.status.toLowerCase()]">
                <div class="gen-status-icon">
                  <el-icon v-if="g.status === 'SUCCESS'" class="icon-success"><CircleCheck /></el-icon>
                  <el-icon v-else-if="g.status === 'FAILED'" class="icon-failed"><CircleClose /></el-icon>
                  <el-icon v-else class="icon-pending is-loading"><Loading /></el-icon>
                </div>
                <div class="gen-info">
                  <div class="gen-top">
                    <el-tag :type="statusType(g.status)" size="small" effect="light" round>
                      {{ statusText(g.status) }}
                    </el-tag>
                    <span class="gen-time">{{ formatDate(g.createdAt) }}</span>
                  </div>
                  <div class="gen-provider" v-if="g.provider">
                    <el-icon><Cloudy /></el-icon> {{ g.provider }}
                  </div>
                </div>
                <div class="gen-actions">
                  <el-button
                    v-if="g.status === 'SUCCESS'"
                    type="primary"
                    size="small"
                    round
                    @click="openViewer(g)"
                  >
                    <el-icon><VideoCamera /></el-icon> 查看效果
                  </el-button>
                  <el-tooltip
                    v-else-if="g.status === 'FAILED'"
                    :content="g.errorMessage || '生成失败'"
                    placement="top"
                  >
                    <el-button size="small" text type="danger">失败详情</el-button>
                  </el-tooltip>
                  <span v-else class="processing-text">处理中…</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import { projectApi, generationApi } from '@/api'

const route = useRoute()
const router = useRouter()
const projectId = route.params.id

const project = ref(null)
const generations = ref([])
const loading = ref(false)
const generating = ref(false)
let timer = null

async function loadProject() {
  project.value = await projectApi.get(projectId)
}

async function loadGenerations() {
  generations.value = await generationApi.listByProject(projectId)
  const hasPending = generations.value.some((g) => g.status === 'PENDING' || g.status === 'PROCESSING')
  if (hasPending) {
    startPolling()
  } else {
    stopPolling()
  }
}

function startPolling() {
  if (timer) return
  timer = setInterval(loadGenerations, 3000)
}

function stopPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

async function onGenerate() {
  generating.value = true
  try {
    await generationApi.start(projectId)
    ElMessage.success('已发起生成，请稍候…')
    await loadGenerations()
  } catch (e) {
    /* 错误提示已由拦截器处理 */
  } finally {
    generating.value = false
  }
}

function openViewer(g) {
  router.push(`/viewer/${g.id}`)
}

function statusType(s) {
  return { SUCCESS: 'success', FAILED: 'danger', PROCESSING: 'warning', PENDING: 'info' }[s] || 'info'
}
function statusText(s) {
  return { SUCCESS: '已完成', FAILED: '失败', PROCESSING: '生成中', PENDING: '排队中' }[s] || s
}
function formatDate(s) {
  if (!s) return ''
  const d = new Date(s)
  if (isNaN(d.getTime())) return s.replace('T', ' ').slice(0, 16)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(async () => {
  loading.value = true
  try {
    await loadProject()
    await loadGenerations()
  } finally {
    loading.value = false
  }
})

onUnmounted(stopPolling)
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: var(--color-bg, #f5f7fa);
}

/* ---- 面包屑 ---- */
.breadcrumb {
  margin-bottom: 8px;
}

/* ---- 头部 ---- */
.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  gap: 16px;
  background: var(--color-bg-white, #fff);
  border-radius: 16px;
  padding: 24px 28px;
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.06));
}

.title-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.project-name {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
}

.project-desc {
  margin: 6px 0 8px;
  font-size: 14px;
}

.project-date {
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.generate-btn {
  border-radius: 12px;
  padding: 12px 24px;
  font-weight: 600;
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.3);
  transition: all var(--el-transition-duration-fast);
}

.generate-btn:hover {
  box-shadow: 0 8px 28px rgba(64, 158, 255, 0.4);
  transform: translateY(-1px);
}

/* ---- 双栏布局 ---- */
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  padding-bottom: 48px;
}

.panel {
  background: var(--color-bg-white, #fff);
  border-radius: 16px;
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.06));
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--color-border-lighter, #ebeef5);
}

.panel-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.image-wrapper {
  padding: 16px;
  position: relative;
}

.design-image {
  width: 100%;
  border-radius: 10px;
  object-fit: contain;
  background: #f5f7fa;
  transition: transform var(--el-transition-duration-normal, 0.3s);
}

.design-image:hover {
  transform: scale(1.01);
}

.image-badge {
  position: absolute;
  top: 24px;
  left: 24px;
  padding: 2px 10px;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 11px;
  border-radius: 6px;
  backdrop-filter: blur(4px);
}

/* ---- 生成记录 ---- */
.gen-list {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.gen-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--color-border-lighter, #ebeef5);
  border-radius: 12px;
  transition: all var(--el-transition-duration-fast);
}

.gen-item:hover {
  border-color: var(--color-border-light, #e4e7ed);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.gen-item.success {
  border-left: 3px solid var(--color-success, #67c23a);
}

.gen-item.failed {
  border-left: 3px solid var(--color-danger, #f56c6c);
}

.gen-item.pending,
.gen-item.processing {
  border-left: 3px solid var(--color-warning, #e6a23c);
}

.gen-status-icon {
  flex-shrink: 0;
  font-size: 22px;
}

.icon-success { color: var(--color-success, #67c23a); }
.icon-failed { color: var(--color-danger, #f56c6c); }
.icon-pending { color: var(--color-warning, #e6a23c); }

.gen-info {
  flex: 1;
  min-width: 0;
}

.gen-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}

.gen-time {
  font-size: 12px;
  color: var(--color-text-placeholder, #c0c4cc);
}

.gen-provider {
  font-size: 12px;
  color: var(--color-text-secondary, #909399);
  display: flex;
  align-items: center;
  gap: 4px;
}

.gen-actions {
  flex-shrink: 0;
}

.processing-text {
  font-size: 13px;
  color: var(--color-warning, #e6a23c);
  white-space: nowrap;
}

/* ---- 空状态 ---- */
.empty-gen-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e8f3ff, #c8e0ff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: var(--color-primary, #409eff);
  margin: 0 auto;
}

.empty-gen-text {
  font-size: 13px;
  color: var(--color-text-secondary, #909399);
  margin: 8px 0 0;
}

@media (max-width: 768px) {
  .detail-grid { grid-template-columns: 1fr; }
  .detail-head { flex-direction: column; }
}
</style>
