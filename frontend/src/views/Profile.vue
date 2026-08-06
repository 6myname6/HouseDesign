<template>
  <div class="profile-page">
    <AppHeader />

    <!-- 封面区域 -->
    <div class="profile-cover">
      <div class="cover-bg"></div>
      <div class="cover-content">
        <div class="avatar-section">
          <div class="avatar-wrap">
            <img v-if="userInfo.avatar" :src="userInfo.avatar" class="avatar" alt="头像" />
            <div v-else class="avatar avatar-fallback" :style="{ background: avatarColor }">
              {{ avatarText }}
            </div>
            <div class="avatar-ring"></div>
          </div>
          <div class="profile-text">
            <h1 class="nickname">{{ userInfo.nickname || userInfo.username }}</h1>
            <p class="username">@{{ userInfo.username }}</p>
            <div class="profile-stats">
              <div class="stat-item">
                <span class="stat-value">{{ projectCount }}</span>
                <span class="stat-label">项目</span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <span class="stat-value">{{ generationCount }}</span>
                <span class="stat-label">生成</span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <span class="stat-value">{{ postCount }}</span>
                <span class="stat-label">分享</span>
              </div>
            </div>
          </div>
        </div>
        <el-button class="edit-profile-btn" @click="openEdit">
          <el-icon><Edit /></el-icon> 编辑资料
        </el-button>
      </div>
    </div>

    <!-- 项目列表 -->
    <section class="content-section">
      <div class="page-container">
        <div class="section-header">
          <div class="section-header-left">
            <h2 class="section-title">
              <el-icon><FolderOpened /></el-icon>
              我的项目
            </h2>
          </div>
          <el-button type="primary" @click="router.push('/projects/new')" class="new-btn">
            <el-icon><Plus /></el-icon> 新建项目
          </el-button>
        </div>

        <el-empty v-if="!loading && projects.length === 0" description="还没有项目" :image-size="80">
          <template #image>
            <div class="empty-icon">🏗️</div>
          </template>
          <el-button type="primary" @click="router.push('/projects/new')">上传设计图</el-button>
        </el-empty>

        <div v-loading="loading" class="project-grid stagger-enter">
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
                <el-icon><View /></el-icon>
              </div>
              <el-tag v-if="p.styleLabel" class="style-tag" size="small" effect="dark" round>
                {{ p.styleLabel }}
              </el-tag>
            </div>
            <div class="card-body">
              <div class="card-title-line">
                <h3>{{ p.name }}</h3>
              </div>
              <p class="card-desc">{{ p.description || '暂无描述' }}</p>
              <div class="card-footer">
                <span class="card-date">
                  <el-icon><Calendar /></el-icon>
                  {{ formatDate(p.createdAt) }}
                </span>
                <el-button size="small" text type="danger" @click.stop="onDelete(p)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="editVisible" title="编辑资料" width="440px" top="10vh" class="edit-dialog" @closed="resetEdit">
      <el-form label-width="60px">
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" maxlength="20" placeholder="请输入昵称" size="large" />
        </el-form-item>
        <el-form-item label="头像">
          <div class="avatar-edit-row">
            <img v-if="form.avatar" :src="form.avatar" class="avatar-preview" alt="头像预览" />
            <div v-else class="avatar-preview avatar-fallback-sm" :style="{ background: avatarColor }">
              {{ avatarText }}
            </div>
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :before-upload="beforeAvatar"
              :http-request="uploadAvatar"
              accept="image/*"
            >
              <el-button size="small">
                <el-icon><Upload /></el-icon> 上传图片
              </el-button>
            </el-upload>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile" round>保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import { authApi, projectApi, fileApi } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo || {})

const projects = ref([])
const loading = ref(false)
const editVisible = ref(false)
const saving = ref(false)
const projectCount = ref(0)
const generationCount = ref(0)
const postCount = ref(0)

const form = reactive({ nickname: '', avatar: '' })

const avatarText = computed(() => {
  const n = userInfo.value.nickname || userInfo.value.username || '?'
  return n.trim().charAt(0).toUpperCase()
})
const avatarColor = computed(() => {
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#9254de', '#13c2c2']
  const s = userInfo.value.username || '?'
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) % colors.length
  return colors[h]
})

async function load() {
  loading.value = true
  try {
    const list = await projectApi.list()
    projects.value = list
    projectCount.value = list.length
    generationCount.value = list.reduce((sum, p) => sum + (p.generationCount || 0), 0)
    postCount.value = list.reduce((sum, p) => sum + (p.postCount || 0), 0)
  } finally {
    loading.value = false
  }
}

async function onDelete(p) {
  try {
    await ElMessageBox.confirm(`确定删除项目「${p.name}」吗？`, '提示', { type: 'warning' })
  } catch { return }
  await projectApi.remove(p.id)
  ElMessage.success('已删除')
  load()
}

function openEdit() {
  form.nickname = userInfo.value.nickname || ''
  form.avatar = userInfo.value.avatar || ''
  editVisible.value = true
}

function resetEdit() {
  form.nickname = ''
  form.avatar = ''
}

function beforeAvatar(file) {
  if (file.size / 1024 / 1024 > 5) { ElMessage.error('头像图片不能超过 5MB'); return false }
  return true
}

async function uploadAvatar({ file }) {
  try {
    const url = await fileApi.upload(file)
    form.avatar = url
    ElMessage.success('头像已上传')
  } catch {
    ElMessage.error('上传失败')
  }
}

async function saveProfile() {
  if (!form.nickname.trim()) { ElMessage.warning('昵称不能为空'); return }
  saving.value = true
  try {
    const res = await authApi.update({ nickname: form.nickname.trim(), avatar: form.avatar })
    userStore.setProfile({ nickname: res.nickname, avatar: res.avatar })
    ElMessage.success('资料已更新')
    editVisible.value = false
  } finally {
    saving.value = false
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
.profile-page {
  min-height: 100vh;
  background: var(--color-bg, #f5f7fa);
}

/* ---- 封面区域 ---- */
.profile-cover {
  position: relative;
  padding: 32px 24px;
  background: var(--color-bg-white, #fff);
  margin-bottom: 0;
  overflow: hidden;
}

.cover-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #e8f3ff 0%, #d4edda 50%, #e8f3ff 100%);
  opacity: 0.5;
}

.cover-bg::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 30%, rgba(64, 158, 255, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(103, 194, 58, 0.08) 0%, transparent 50%);
}

.cover-content {
  position: relative;
  max-width: 880px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 24px;
}

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: #fff;
  font-weight: 600;
  position: relative;
  z-index: 1;
}

.avatar-fallback {
  background: #409eff;
}

.avatar-ring {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 3px solid rgba(64, 158, 255, 0.2);
  animation: ringPulse 3s ease-in-out infinite;
}

@keyframes ringPulse {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.05); opacity: 1; }
}

.profile-text {
  min-width: 0;
}

.nickname {
  margin: 0 0 2px;
  font-size: 24px;
  font-weight: 700;
}

.username {
  margin: 0 0 16px;
  font-size: 14px;
  color: var(--color-text-secondary, #909399);
}

.profile-stats {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary, #303133);
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-secondary, #909399);
}

.stat-divider {
  width: 1px;
  height: 30px;
  background: var(--color-border-lighter, #ebeef5);
}

.edit-profile-btn {
  flex-shrink: 0;
  border-radius: 10px;
  padding: 10px 20px;
  font-weight: 500;
  background: rgba(64, 158, 255, 0.08);
  border: 1px solid rgba(64, 158, 255, 0.15);
  color: var(--color-primary, #409eff);
  transition: all var(--el-transition-duration-fast);
}

.edit-profile-btn:hover {
  background: var(--color-primary, #409eff);
  color: #fff;
  border-color: var(--color-primary, #409eff);
}

/* ---- 内容区域 ---- */
.content-section {
  padding: 24px 0 48px;
}

.page-container {
  max-width: 880px;
  margin: 0 auto;
  padding: 0 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.new-btn {
  border-radius: 10px;
  font-weight: 500;
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 4px;
}

/* ---- 项目网格 ---- */
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
  min-height: 200px;
}

.project-card {
  background: var(--color-bg-white, #fff);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.06));
  cursor: pointer;
  transition: all var(--el-transition-duration-normal, 0.3s);
  opacity: 0;
  transform: translateY(16px);
  animation: cardUp 0.4s ease forwards;
}

@keyframes cardUp {
  to { opacity: 1; transform: translateY(0); }
}

.project-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg, 0 12px 40px rgba(0,0,0,0.1));
}

.card-thumb {
  position: relative;
  height: 160px;
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
  transform: scale(1.05);
}

.thumb-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity var(--el-transition-duration-normal, 0.3s);
  color: #fff;
  font-size: 28px;
}

.project-card:hover .thumb-overlay {
  opacity: 1;
}

.style-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  border: none;
}

.card-body {
  padding: 14px 16px;
}

.card-title-line h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  margin: 6px 0 12px;
  font-size: 12px;
  color: var(--color-text-secondary, #909399);
  height: 32px;
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
  padding-top: 10px;
}

.card-date {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--color-text-placeholder, #c0c4cc);
}

/* ---- 编辑弹窗 ---- */
.avatar-edit-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-preview {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  font-size: 22px;
}

.avatar-fallback-sm {
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

@media (max-width: 640px) {
  .cover-content {
    flex-direction: column;
    align-items: flex-start;
  }
  .avatar-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  .edit-profile-btn {
    align-self: stretch;
    text-align: center;
  }
  .project-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 480px) {
  .project-grid {
    grid-template-columns: 1fr;
  }
}
</style>
