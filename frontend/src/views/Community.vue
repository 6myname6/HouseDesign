<template>
  <div class="community">
    <AppHeader />

    <div class="page-container">
      <div class="feed-head">
        <div class="feed-head-left">
          <h1 class="page-title">装修小圈</h1>
          <p class="page-desc text-muted">分享装修经验，交流设计心得</p>
        </div>
        <el-radio-group v-model="activeTab" size="small" class="tab-group">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="mine">我的</el-radio-button>
        </el-radio-group>
      </div>

      <div v-loading="loading" class="feed-list">
        <el-empty v-if="!loading && posts.length === 0" :image-size="120">
          <template #image>
            <div class="empty-icon">📝</div>
          </template>
          <p class="empty-text">还没有内容，去发布第一条分享吧</p>
        </el-empty>

        <article
          v-for="(post, i) in posts"
          :key="post.id"
          class="post-card"
          :style="{ animationDelay: `${i * 0.05}s` }"
        >
          <!-- 顶部：头像 & 作者 -->
          <div class="post-top">
            <el-avatar :size="42" :style="{ background: avatarColor(post.authorName) }" class="post-avatar">
              {{ initial(post.authorName) }}
            </el-avatar>
            <div class="post-meta">
              <div class="author-name">{{ post.authorName }}</div>
              <div class="post-time">{{ formatTime(post.createdAt) }}</div>
            </div>
            <el-dropdown v-if="post.userId === currentUserId" trigger="click" class="post-more">
              <el-button text class="more-btn">
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="deletePost(post)">
                    <span style="color: #f56c6c">
                      <el-icon><Delete /></el-icon> 删除
                    </span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <!-- 内容 -->
          <p v-if="post.content" class="post-content">{{ post.content }}</p>

          <!-- 图片 -->
          <div v-if="post.images && post.images.length" class="post-images">
            <el-image
              v-for="(img, i) in post.images"
              :key="i"
              :src="img"
              :preview-src-list="post.images"
              :initial-index="i"
              fit="cover"
              class="post-image"
              :class="{ 'img-full': post.images.length === 1 }"
            />
          </div>

          <!-- 交互按钮 -->
          <div class="post-actions">
            <button
              class="action-btn like-btn"
              :class="{ liked: post.likedByMe }"
              @click="toggleLike(post)"
            >
              <span class="like-icon" :class="{ 'heart-beat': post._justLiked }">
                <el-icon v-if="post.likedByMe"><StarFilled /></el-icon>
                <el-icon v-else><Star /></el-icon>
              </span>
              <span>{{ post.likeCount || 0 }}</span>
              <span class="action-label">赞</span>
            </button>
            <button
              class="action-btn comment-btn"
              :class="{ active: post._showComments }"
              @click="toggleComments(post)"
            >
              <el-icon><ChatRound /></el-icon>
              <span>{{ post.commentCount || 0 }}</span>
              <span class="action-label">评论</span>
            </button>
          </div>

          <!-- 评论区 -->
          <transition name="comments-slide">
            <div v-if="post._showComments" class="comment-box">
              <div v-if="post.comments && post.comments.length" class="comment-list">
                <div v-for="c in post.comments" :key="c.id" class="comment-item">
                  <el-avatar :size="30" :style="{ background: avatarColor(c.authorName), fontSize: '12px' }">
                    {{ initial(c.authorName) }}
                  </el-avatar>
                  <div class="comment-body">
                    <div class="comment-head">
                      <span class="comment-author">{{ c.authorName }}</span>
                      <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
                      <button
                        v-if="c.userId === currentUserId"
                        class="comment-del"
                        title="删除"
                        @click="deleteComment(post, c)"
                      >
                        <el-icon><Delete /></el-icon>
                      </button>
                    </div>
                    <div class="comment-text">{{ c.content }}</div>
                  </div>
                </div>
              </div>
              <div class="comment-input">
                <el-input
                  v-model="post._draft"
                  placeholder="说点什么…"
                  size="small"
                  :prefix-icon="EditPen"
                  @keyup.enter="submitComment(post)"
                />
                <el-button
                  type="primary"
                  size="small"
                  round
                  :disabled="!post._draft || !post._draft.trim()"
                  @click="submitComment(post)"
                >
                  发送
                </el-button>
              </div>
            </div>
          </transition>
        </article>
      </div>
    </div>

    <!-- 发布浮窗 -->
    <div class="fab" title="发布分享" @click="openPublish">
      <el-icon><Plus /></el-icon>
      <span class="fab-label">发布</span>
    </div>

    <!-- 发布弹窗 -->
    <el-dialog v-model="showPublish" title="发布分享" width="560px" top="6vh" @closed="resetPublish" class="publish-dialog">
      <div class="publish-avatar">
        <el-avatar :size="36" :style="{ background: avatarColor(userStore.userInfo?.nickname || userStore.userInfo?.username) }">
          {{ initial(userStore.userInfo?.nickname || userStore.userInfo?.username) }}
        </el-avatar>
        <span class="publish-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
      </div>
      <el-input
        v-model="publishContent"
        type="textarea"
        :rows="5"
        maxlength="1000"
        show-word-limit
        placeholder="分享你的装修经验、避坑心得，或晒一晒效果美图…"
        class="publish-input"
      />
      <div class="uploader">
        <el-upload
          list-type="picture-card"
          :http-request="customUpload"
          :on-success="onUploadSuccess"
          :on-remove="onUploadRemove"
          :before-upload="beforeImage"
          v-model:file-list="publishImages"
          accept="image/*"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
      </div>
      <template #footer>
        <el-button @click="showPublish = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="publish" round class="pub-btn">
          发布
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import { useUserStore } from '@/store/user'
import { communityApi, fileApi } from '@/api'

const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.userId)

const posts = ref([])
const loading = ref(false)
const activeTab = ref('all')

async function fetchPosts() {
  loading.value = true
  try {
    posts.value = (await communityApi.list(activeTab.value === 'mine')) || []
  } catch (e) {
    posts.value = []
  } finally {
    loading.value = false
  }
}
watch(activeTab, fetchPosts)

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return String(t)
  const pad = (n) => String(n).padStart(2, '0')
  const diff = (Date.now() - d.getTime()) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  if (diff < 2592000) return Math.floor(diff / 86400) + ' 天前'
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const AVATAR_COLORS = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#9254de', '#13c2c2', '#eb2f96']
function avatarColor(name) {
  let h = 0
  const s = name || '?'
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0
  return AVATAR_COLORS[h % AVATAR_COLORS.length]
}
function initial(name) {
  return (name || '?').trim().charAt(0)
}

async function toggleLike(post) {
  if (post._liking) return
  post._liking = true
  try {
    const r = await communityApi.toggleLike(post.id)
    post.likedByMe = r.liked
    post.likeCount = r.likeCount
    if (r.liked) {
      post._justLiked = true
      setTimeout(() => { post._justLiked = false }, 600)
    }
  } finally {
    post._liking = false
  }
}

function toggleComments(post) {
  post._showComments = !post._showComments
  if (post._showComments && post._draft === undefined) post._draft = ''
}

async function submitComment(post) {
  const text = (post._draft || '').trim()
  if (!text) return
  try {
    const c = await communityApi.comment(post.id, text)
    if (!post.comments) post.comments = []
    post.comments.push(c)
    post.commentCount = (post.commentCount || 0) + 1
    post._draft = ''
  } catch (e) {
    /* 错误处理 */
  }
}

async function deleteComment(post, c) {
  try {
    await ElMessageBox.confirm('删除这条评论？', '提示', { type: 'warning' })
  } catch { return }
  await communityApi.removeComment(c.id)
  post.comments = (post.comments || []).filter((x) => x.id !== c.id)
  post.commentCount = Math.max(0, (post.commentCount || 1) - 1)
}

async function deletePost(post) {
  try {
    await ElMessageBox.confirm('删除这条分享？', '提示', { type: 'warning' })
  } catch { return }
  await communityApi.remove(post.id)
  posts.value = posts.value.filter((p) => p.id !== post.id)
  ElMessage.success('已删除')
}

// 发布
const showPublish = ref(false)
const publishContent = ref('')
const publishImages = ref([])
const publishing = ref(false)

function openPublish() { showPublish.value = true }
function resetPublish() {
  publishContent.value = ''
  publishImages.value = []
}
async function customUpload(options) {
  const url = await fileApi.upload(options.file)
  options.onSuccess(url, options.file)
}
function onUploadSuccess(response, file) {
  file.url = response
}
function onUploadRemove(file, fileList) {
  publishImages.value = fileList
}
function beforeImage(file) {
  if (!file.type.startsWith('image/')) { ElMessage.error('只能上传图片'); return false }
  if (file.size / 1024 / 1024 > 10) { ElMessage.error('单张图片不能超过 10MB'); return false }
  return true
}
async function publish() {
  const imgs = publishImages.value.map((f) => f.url || f.response).filter(Boolean)
  if (!publishContent.value.trim() && imgs.length === 0) {
    ElMessage.warning('请填写内容或上传图片')
    return
  }
  publishing.value = true
  try {
    const post = await communityApi.create({ content: publishContent.value.trim(), images: imgs })
    posts.value.unshift(post)
    showPublish.value = false
    ElMessage.success('发布成功')
  } finally {
    publishing.value = false
  }
}

fetchPosts()
</script>

<style scoped>
.community {
  min-height: 100vh;
  background: var(--color-bg, #f5f7fa);
  padding-bottom: 80px;
}

/* ---- 头部 ---- */
.feed-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
  gap: 16px;
  flex-wrap: wrap;
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

.tab-group :deep(.el-radio-button__inner) {
  border-radius: 8px !important;
  border: none !important;
  padding: 6px 16px;
  font-weight: 500;
}

.tab-group :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 8px !important;
}

.tab-group :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 8px !important;
}

/* ---- 空状态 ---- */
.empty-icon {
  font-size: 48px;
  margin-bottom: 8px;
}

.empty-text {
  font-size: 14px;
  color: var(--color-text-secondary, #909399);
  margin: 0;
}

/* ---- 帖子列表 ---- */
.feed-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 200px;
}

.post-card {
  background: var(--color-bg-white, #fff);
  border-radius: 16px;
  padding: 20px 22px;
  box-shadow: var(--shadow-sm, 0 2px 12px rgba(0,0,0,0.04));
  transition: all var(--el-transition-duration-normal, 0.3s);
  opacity: 0;
  transform: translateY(12px);
  animation: postEnter 0.4s ease forwards;
}

@keyframes postEnter {
  to { opacity: 1; transform: translateY(0); }
}

.post-card:hover {
  box-shadow: var(--shadow-md, 0 4px 20px rgba(0,0,0,0.08));
}

/* ---- 顶部 ---- */
.post-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.post-avatar {
  flex-shrink: 0;
}

.post-meta {
  flex: 1;
  min-width: 0;
}

.author-name {
  font-weight: 600;
  font-size: 15px;
}

.post-time {
  font-size: 12px;
  color: var(--color-text-placeholder, #c0c4cc);
}

.more-btn {
  color: var(--color-text-placeholder, #c0c4cc);
  transition: color var(--el-transition-duration-fast);
}

.more-btn:hover {
  color: var(--color-text-secondary, #909399);
}

/* ---- 内容 ---- */
.post-content {
  margin: 12px 0;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 15px;
  color: var(--color-text-primary, #303133);
}

/* ---- 图片 ---- */
.post-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  margin-bottom: 12px;
}

.post-image {
  width: 100%;
  height: 160px;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform var(--el-transition-duration-fast);
}

.post-image:hover {
  transform: scale(1.02);
}

.post-image.img-full {
  height: 240px;
  max-width: 480px;
}

/* ---- 交互按钮 ---- */
.post-actions {
  display: flex;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border-lighter, #f2f3f5);
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--color-text-secondary, #909399);
  cursor: pointer;
  font-size: 14px;
  border: none;
  background: none;
  padding: 6px 14px;
  border-radius: 8px;
  transition: all var(--el-transition-duration-fast);
  font-family: inherit;
}

.action-btn:hover {
  background: #f5f7fa;
}

.action-label {
  font-size: 12px;
}

.like-btn.liked {
  color: #f56c6c;
}

.like-btn.liked:hover {
  background: rgba(245, 108, 108, 0.08);
}

.like-icon {
  display: flex;
  align-items: center;
  transition: transform var(--el-transition-duration-fast);
}

.like-icon.heart-beat {
  animation: heartBeat 0.5s ease;
}

@keyframes heartBeat {
  0% { transform: scale(1); }
  25% { transform: scale(1.35); }
  50% { transform: scale(1); }
  75% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

.comment-btn.active {
  color: var(--color-primary, #409eff);
}

/* ---- 评论区 ---- */
.comment-box {
  margin-top: 12px;
  background: #f7f8fa;
  border-radius: 12px;
  padding: 14px;
  overflow: hidden;
}

.comments-slide-enter-active {
  animation: slideDown 0.25s ease;
}
.comments-slide-leave-active {
  animation: slideDown 0.2s ease reverse;
}

@keyframes slideDown {
  from { opacity: 0; max-height: 0; }
  to { opacity: 1; max-height: 500px; }
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
}

.comment-item {
  display: flex;
  gap: 8px;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.comment-author {
  font-weight: 600;
  color: var(--color-text-primary, #303133);
}

.comment-time {
  font-size: 11px;
  color: var(--color-text-placeholder, #c0c4cc);
}

.comment-del {
  margin-left: auto;
  cursor: pointer;
  color: var(--color-text-placeholder, #c0c4cc);
  background: none;
  border: none;
  padding: 2px;
  display: flex;
  transition: color var(--el-transition-duration-fast);
}

.comment-del:hover {
  color: #f56c6c;
}

.comment-text {
  font-size: 14px;
  line-height: 1.6;
  margin-top: 2px;
  word-break: break-word;
  color: var(--color-text-regular, #606266);
}

.comment-input {
  display: flex;
  gap: 8px;
  align-items: center;
}

.comment-input .el-input {
  flex: 1;
}

/* ---- 底部浮动按钮 ---- */
.fab {
  position: fixed;
  right: 36px;
  bottom: 40px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 14px 22px;
  border-radius: 28px;
  background: linear-gradient(135deg, #409eff, #67c23a);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.4);
  transition: all var(--el-transition-duration-normal, 0.3s);
  z-index: 50;
  font-size: 20px;
  border: none;
  font-family: inherit;
}

.fab:hover {
  transform: translateY(-3px) scale(1.04);
  box-shadow: 0 12px 32px rgba(64, 158, 255, 0.5);
}

.fab:active {
  transform: scale(0.96);
}

.fab-label {
  font-size: 14px;
  font-weight: 600;
}

/* ---- 发布弹窗 ---- */
.publish-avatar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.publish-name {
  font-weight: 600;
  font-size: 15px;
}

.publish-input :deep(.el-textarea__inner) {
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.7;
}

.uploader {
  margin-top: 14px;
}

.pub-btn {
  padding: 8px 28px;
  font-weight: 600;
}

/* ---- 响应式 ---- */
@media (max-width: 560px) {
  .post-images {
    grid-template-columns: repeat(2, 1fr);
  }
  .post-image.img-full {
    max-width: 100%;
    height: 200px;
  }
  .fab {
    right: 20px;
    bottom: 24px;
    padding: 12px 18px;
  }
}
</style>
