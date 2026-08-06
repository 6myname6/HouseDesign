<template>
  <div class="home">
    <AppHeader />

    <!-- ===== Hero — 快速开始 ===== -->
    <section class="hero">
      <!-- 浮动装饰元素 -->
      <div class="floating-elements">
        <span class="float-el el-1">🛋️</span>
        <span class="float-el el-2">💡</span>
        <span class="float-el el-3">🪴</span>
        <span class="float-el el-4">✨</span>
        <span class="float-el el-5">🏗️</span>
      </div>

      <div class="hero-inner">
        <div class="hero-text fade-in-up">
          <div class="hero-badge">
            <span class="badge-dot"></span>
            AI 3D 智能装修设计
          </div>
          <h1>
            <span class="h-line">准备好，</span>
            <span class="h-line gradient-text">开启你的理想家</span>
          </h1>
          <p class="hero-subtitle">
            上传一张设计图，AI 帮你识别户型与布局，<br />
            生成可旋转、可漫游的 3D 施工效果，装修前先看见未来的家。
          </p>
          <div class="hero-actions">
            <el-button type="primary" size="large" class="hero-btn" @click="goStart">
              快速开始
              <el-icon class="el-icon--right"><Right /></el-icon>
            </el-button>
            <el-button size="large" class="hero-btn-ghost" @click="scrollToFeed">
              了解更多
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
          </div>
          <div class="hero-meta">
            <div class="meta-item">
              <el-icon class="meta-icon"><Document /></el-icon>
              <span>支持户型图 / CAD / 手绘稿</span>
            </div>
            <div class="meta-divider"></div>
            <div class="meta-item">
              <el-icon class="meta-icon"><MagicStick /></el-icon>
              <span>AI 自动生成 3D 效果</span>
            </div>
            <div class="meta-divider"></div>
            <div class="meta-item">
              <el-icon class="meta-icon"><VideoCamera /></el-icon>
              <span>720° 全景漫游</span>
            </div>
          </div>
        </div>
        <div class="hero-visual fade-in-up" style="animation-delay: 0.2s">
          <div class="visual-card">
            <div class="vc-header">
              <span class="vc-dot red"></span>
              <span class="vc-dot yellow"></span>
              <span class="vc-dot green"></span>
            </div>
            <div class="vc-content">
              <div class="vc-floorplan">
                <div class="room living-room">
                  <span class="room-label">客厅</span>
                  <div class="furniture sofa"></div>
                  <div class="furniture tv"></div>
                </div>
                <div class="room bedroom">
                  <span class="room-label">卧室</span>
                  <div class="furniture bed"></div>
                  <div class="furniture nightstand"></div>
                </div>
                <div class="room kitchen">
                  <span class="room-label">厨房</span>
                  <div class="furniture counter"></div>
                </div>
                <div class="room bathroom">
                  <span class="room-label">卫浴</span>
                  <div class="furniture bathtub"></div>
                </div>
              </div>
            </div>
            <div class="vc-footer">
              <div class="scanline"></div>
              <span class="vc-status">
                <span class="status-dot"></span>
                AI 分析完成 · 3D 生成中
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 工作流程 ===== -->
    <section class="workflow">
      <div class="page-container">
        <div class="section-header fade-in-up">
          <span class="section-tag">流程</span>
          <h2 class="section-title">三步搞定装修预览</h2>
          <p class="section-desc text-muted">从设计图到 3D 沉浸式体验，只需几分钟</p>
        </div>
        <div class="steps-grid">
          <div
            v-for="(s, i) in steps"
            :key="i"
            class="step-card fade-in-up"
            :style="{ animationDelay: `${0.1 + i * 0.12}s` }"
          >
            <div class="step-number">{{ String(i + 1).padStart(2, '0') }}</div>
            <div class="step-icon-wrap">
              <el-icon class="step-icon"><component :is="s.icon" /></el-icon>
            </div>
            <h3 class="step-title">{{ s.title }}</h3>
            <p class="step-desc">{{ s.desc }}</p>
            <div class="step-line" v-if="i < steps.length - 1"></div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 亮点特性 ===== -->
    <section class="features">
      <div class="page-container">
        <div class="section-header fade-in-up">
          <span class="section-tag">亮点</span>
          <h2 class="section-title">为什么选择筑梦家</h2>
        </div>
        <div class="features-grid">
          <div
            v-for="(f, i) in features"
            :key="i"
            class="feature-card fade-in-up"
            :style="{ animationDelay: `${0.1 + i * 0.1}s` }"
          >
            <div class="feature-icon-wrap" :style="{ background: f.bg }">
              <span class="feature-emoji">{{ f.emoji }}</span>
            </div>
            <h3 class="feature-title">{{ f.title }}</h3>
            <p class="feature-desc">{{ f.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 装修干货 / 经验 ===== -->
    <section class="feed page-container" ref="feedRef">
      <div class="feed-head fade-in-up">
        <div class="feed-head-left">
          <span class="section-tag">干货</span>
          <h2 class="section-title">装修干货 · 避坑指南</h2>
        </div>
        <el-tabs v-model="activeCat" class="feed-tabs">
          <el-tab-pane v-for="c in categories" :key="c.key" :label="c.label" :name="c.key" />
        </el-tabs>
      </div>

      <div class="feed-grid stagger-enter" :key="activeCat">
        <article
          v-for="(post, i) in filteredPosts"
          :key="post.id"
          class="feed-card"
          :style="{ animationDelay: `${i * 0.06}s` }"
          @click="openPost(post)"
        >
          <div class="cover" :style="{ background: post.cover }">
            <span class="cover-emoji">{{ post.emoji }}</span>
            <div class="cover-overlay"></div>
          </div>
          <div class="card-body">
            <div class="card-tag-row">
              <el-tag size="small" :type="tagMeta(post.type).type" effect="light" round>
                {{ tagMeta(post.type).label }}
              </el-tag>
            </div>
            <h3 class="card-title">{{ post.title }}</h3>
            <p class="excerpt">{{ post.excerpt }}</p>
            <div class="card-footer">
              <span class="author">
                <el-icon><User /></el-icon>
                {{ post.author }}
              </span>
              <span class="read-count">
                <el-icon><View /></el-icon>
                {{ post.readCount >= 1000 ? (post.readCount / 1000).toFixed(1) + 'k' : post.readCount }}
              </span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <!-- ===== CTA 底部横幅 ===== -->
    <section class="cta-banner fade-in-up">
      <div class="cta-inner">
        <h2>现在就试试，看见你未来的家</h2>
        <p>上传设计图 → AI 快速生成 → 沉浸式 3D 漫游</p>
        <el-button type="primary" size="large" class="cta-btn" @click="goStart">
          免费开始设计
          <el-icon class="el-icon--right"><Right /></el-icon>
        </el-button>
      </div>
    </section>

    <!-- ===== 详情弹窗 ===== -->
    <el-dialog v-model="dialogVisible" :title="activePost?.title" width="640px" top="8vh" class="post-dialog">
      <div v-if="activePost" class="post-detail">
        <div class="post-tags">
          <el-tag size="small" :type="tagMeta(activePost.type).type" effect="light" round>
            {{ tagMeta(activePost.type).label }}
          </el-tag>
          <span class="text-muted">{{ activePost.author }} · {{ activePost.date }}</span>
        </div>
        <div class="post-content">
          <p v-for="(p, i) in contentLines" :key="i">{{ p }}</p>
        </div>
      </div>
    </el-dialog>

    <footer class="site-footer">
      <div class="footer-inner">
        <span>🏠 筑梦家 · AI 3D 房屋装修设计</span>
        <span class="text-muted">让每个家都值得被看见</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import AppHeader from '@/components/AppHeader.vue'
import { FEED_CATEGORIES, FEED_POSTS, FEED_TAG_META } from '@/constants/homeFeed'

const router = useRouter()
const userStore = useUserStore()

const feedRef = ref(null)

const steps = [
  { icon: 'Upload', title: '上传设计图', desc: '户型平面图、CAD 导出图或手绘稿，拖拽上传即可。' },
  { icon: 'MagicStick', title: 'AI 生成 3D', desc: '自动识别房间、墙体与布局，智能匹配装修风格。' },
  { icon: 'VideoCamera', title: '漫游查看', desc: '在 3D 网页中自由旋转、缩放，沉浸式预览未来家。' }
]

const features = [
  { emoji: '🤖', title: 'AI 智能识别', desc: '自动识别户型图中的墙体、门窗、房间布局。', bg: 'linear-gradient(135deg, #e8f3ff, #c8e0ff)' },
  { emoji: '🎨', title: '多种装修风格', desc: '现代简约、奶油法式、意式轻奢、新中式等风格一键切换。', bg: 'linear-gradient(135deg, #fef3e8, #fde8c8)' },
  { emoji: '🌐', title: '720° 沉浸漫游', desc: '自由旋转视角，支持房间间跳转，如同身临其境。', bg: 'linear-gradient(135deg, #e8faf0, #c8f0dc)' },
  { emoji: '📱', title: '多端适配', desc: 'PC、平板、手机均可流畅体验，随时随地查看设计。', bg: 'linear-gradient(135deg, #f3e8ff, #e0c8ff)' }
]

function goStart() {
  if (userStore.isLoggedIn) {
    router.push('/projects/new')
  } else {
    router.push('/login')
  }
}

function scrollToFeed() {
  feedRef.value?.scrollIntoView({ behavior: 'smooth' })
}

const categories = FEED_CATEGORIES
const activeCat = ref('all')
const filteredPosts = computed(() =>
  activeCat.value === 'all'
    ? FEED_POSTS
    : FEED_POSTS.filter((p) => p.type === activeCat.value)
)

function tagMeta(type) {
  return FEED_TAG_META[type] || { label: '干货', type: 'info' }
}

const dialogVisible = ref(false)
const activePost = ref(null)
const contentLines = computed(() =>
  activePost.value ? activePost.value.content.split('\n') : []
)
function openPost(post) {
  activePost.value = post
  dialogVisible.value = true
}

/* 滚动入场 */
onMounted(() => {
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('in-view')
          observer.unobserve(entry.target)
        }
      })
    },
    { threshold: 0.1 }
  )
  document.querySelectorAll('.fade-in-up').forEach((el) => observer.observe(el))
})
</script>

<style scoped>
/* =============================================
   HERO
   ============================================= */
.hero {
  position: relative;
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  background: linear-gradient(160deg, #eef5ff 0%, #f0faf0 50%, #f5fbf3 100%);
  overflow: hidden;
}

/* 浮动装饰 */
.floating-elements {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.float-el {
  position: absolute;
  font-size: 32px;
  opacity: 0.6;
  animation: floatAnim 6s ease-in-out infinite;
}
.float-el.el-1 { top: 12%; left: 6%; animation-delay: 0s; font-size: 40px; }
.float-el.el-2 { top: 20%; right: 10%; animation-delay: 0.8s; }
.float-el.el-3 { bottom: 20%; left: 8%; animation-delay: 1.6s; font-size: 28px; }
.float-el.el-4 { bottom: 15%; right: 6%; animation-delay: 2.4s; font-size: 24px; }
.float-el.el-5 { top: 50%; left: 3%; animation-delay: 3.2s; font-size: 36px; }

@keyframes floatAnim {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  25% { transform: translateY(-12px) rotate(4deg); }
  75% { transform: translateY(6px) rotate(-2deg); }
}

.hero-inner {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px 60px;
  width: 100%;
  display: flex;
  align-items: center;
  gap: 60px;
}

.hero-text {
  flex: 1;
  min-width: 0;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 14px;
  border-radius: 20px;
  background: rgba(64, 158, 255, 0.1);
  color: var(--color-primary, #409eff);
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 20px;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary, #409eff);
  animation: pulse 2s ease-in-out infinite;
}

.hero-text h1 {
  font-size: 48px;
  line-height: 1.2;
  margin: 0 0 16px;
  font-weight: 800;
}

.h-line {
  display: block;
}

.gradient-text {
  background: linear-gradient(135deg, #409eff, #67c23a);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-subtitle {
  font-size: 17px;
  color: var(--color-text-regular, #606266);
  max-width: 520px;
  line-height: 1.8;
  margin: 0 0 32px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-btn {
  padding: 14px 32px;
  font-size: 16px;
  border-radius: 12px;
  font-weight: 600;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.35);
  transition: all var(--el-transition-duration, 0.25s);
}

.hero-btn:hover {
  box-shadow: 0 12px 32px rgba(64, 158, 255, 0.45);
  transform: translateY(-2px);
}

.hero-btn-ghost {
  border-radius: 12px;
  font-weight: 500;
  transition: all var(--el-transition-duration, 0.25s);
}

.hero-btn-ghost:hover {
  background: rgba(64, 158, 255, 0.06);
  transform: translateY(-2px);
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  margin-top: 28px;
  align-items: center;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--color-text-secondary, #909399);
  font-size: 13px;
  padding: 6px 12px;
}

.meta-icon {
  font-size: 16px;
  color: var(--color-primary, #409eff);
}

.meta-divider {
  width: 1px;
  height: 16px;
  background: var(--color-border-lighter, #ebeef5);
}

/* ---- Hero 右侧可视化卡片 ---- */
.hero-visual {
  flex: 1;
  display: flex;
  justify-content: center;
}

.visual-card {
  width: 380px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(8px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08), 0 0 0 1px rgba(255, 255, 255, 0.5);
  overflow: hidden;
  transition: transform 0.4s ease, box-shadow 0.4s ease;
}

.visual-card:hover {
  transform: translateY(-6px) scale(1.01);
  box-shadow: 0 30px 80px rgba(64, 158, 255, 0.15), 0 0 0 1px rgba(255, 255, 255, 0.5);
}

.vc-header {
  display: flex;
  gap: 6px;
  padding: 14px 16px;
}

.vc-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.vc-dot.red { background: #ff5f57; }
.vc-dot.yellow { background: #febc2e; }
.vc-dot.green { background: #28c840; }

.vc-content {
  padding: 0 16px 16px;
}

.vc-floorplan {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 120px 100px;
  gap: 6px;
  background: #f0f2f5;
  border-radius: 12px;
  padding: 6px;
}

.room {
  border-radius: 8px;
  padding: 8px;
  position: relative;
  display: flex;
  flex-wrap: wrap;
  align-content: flex-end;
  gap: 3px;
  background: #fff;
  transition: transform 0.2s;
}

.room:hover {
  transform: scale(1.02);
  z-index: 2;
}

.room-label {
  position: absolute;
  top: 6px;
  left: 8px;
  font-size: 10px;
  font-weight: 600;
  color: var(--color-text-secondary, #909399);
  opacity: 0.7;
}

.living-room { grid-column: 1; grid-row: 1; }
.bedroom { grid-column: 2; grid-row: 1; }
.kitchen { grid-column: 1; grid-row: 2; }
.bathroom { grid-column: 2; grid-row: 2; }

.furniture {
  border-radius: 3px;
  opacity: 0.6;
}

.furniture.sofa { width: 40px; height: 10px; background: #409eff; border-radius: 4px; }
.furniture.tv { width: 14px; height: 8px; background: #606266; border-radius: 2px; }
.furniture.bed { width: 36px; height: 18px; background: #67c23a; border-radius: 4px; }
.furniture.nightstand { width: 10px; height: 10px; background: #909399; border-radius: 2px; }
.furniture.counter { width: 30px; height: 8px; background: #e6a23c; border-radius: 4px; }
.furniture.bathtub { width: 24px; height: 12px; background: #13c2c2; border-radius: 4px; }

.vc-footer {
  padding: 10px 16px;
  border-top: 1px solid rgba(235, 238, 245, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.scanline {
  flex: 1;
  height: 4px;
  border-radius: 2px;
  background: linear-gradient(90deg, #409eff 30%, transparent 80%);
  background-size: 200% 100%;
  animation: scanMove 1.5s linear infinite;
}

@keyframes scanMove {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.vc-status {
  font-size: 11px;
  color: var(--color-text-secondary, #909399);
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-success, #67c23a);
  animation: pulse 2s ease-in-out infinite;
}

/* =============================================
   工作流程
   ============================================= */
.workflow {
  padding: 64px 0 48px;
}

.steps-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 32px;
  margin-top: 40px;
}

.step-card {
  position: relative;
  text-align: center;
  padding: 40px 28px;
  border-radius: 20px;
  background: var(--color-bg-white, #fff);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.06));
  transition: all var(--el-transition-duration-normal, 0.3s);
}

.step-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-lg, 0 12px 40px rgba(0,0,0,0.12));
}

.step-number {
  position: absolute;
  top: 16px;
  right: 20px;
  font-size: 48px;
  font-weight: 800;
  color: rgba(64, 158, 255, 0.08);
  line-height: 1;
  user-select: none;
}

.step-icon-wrap {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: linear-gradient(135deg, #e8f3ff, #c8e0ff);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  font-size: 28px;
  color: var(--color-primary, #409eff);
  transition: transform var(--el-transition-duration-fast);
}

.step-card:hover .step-icon-wrap {
  transform: scale(1.1) rotate(-4deg);
}

.step-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 8px;
}

.step-desc {
  font-size: 14px;
  color: var(--color-text-secondary, #909399);
  margin: 0;
  line-height: 1.6;
}

.step-line {
  display: none;
}

/* =============================================
   亮点特性
   ============================================= */
.features {
  padding: 48px 0 64px;
  background: var(--color-bg-light, #f8fafc);
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-top: 40px;
}

.feature-card {
  text-align: center;
  padding: 36px 20px 28px;
  border-radius: 18px;
  background: var(--color-bg-white, #fff);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.06));
  transition: all var(--el-transition-duration-normal, 0.3s);
  cursor: default;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg, 0 12px 40px rgba(0,0,0,0.1));
}

.feature-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
  font-size: 26px;
  transition: transform var(--el-transition-duration-fast);
}

.feature-card:hover .feature-icon-wrap {
  transform: scale(1.15) rotate(4deg);
}

.feature-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px;
}

.feature-desc {
  font-size: 13px;
  color: var(--color-text-secondary, #909399);
  margin: 0;
  line-height: 1.6;
}

/* =============================================
   干货 Feed
   ============================================= */
.feed {
  padding-top: 56px;
  padding-bottom: 56px;
}

.feed-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}

.feed-head-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.feed-head .section-title {
  margin: 0;
}

.feed-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.feed-card {
  background: var(--color-bg-white, #fff);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.06));
  transition: all var(--el-transition-duration-normal, 0.3s);
  display: flex;
  flex-direction: column;
  opacity: 0;
  transform: translateY(16px);
  animation: slideUp 0.45s ease forwards;
}

.feed-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 40px rgba(64, 158, 255, 0.14);
}

.cover {
  height: 130px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.cover-emoji {
  font-size: 48px;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
  transition: transform var(--el-transition-duration-normal, 0.3s);
  z-index: 1;
}

.feed-card:hover .cover-emoji {
  transform: scale(1.2) rotate(-8deg);
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(0deg, rgba(0,0,0,0.03) 0%, transparent 50%);
}

.card-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.card-tag-row {
  display: flex;
  align-items: center;
}

.card-title {
  margin: 0;
  font-size: 15px;
  line-height: 1.4;
  font-weight: 600;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color var(--el-transition-duration-fast);
}

.feed-card:hover .card-title {
  color: var(--color-primary, #409eff);
}

.excerpt {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-secondary, #909399);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-placeholder, #c0c4cc);
  margin-top: 4px;
}

.author, .read-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* =============================================
   CTA 底部横幅
   ============================================= */
.cta-banner {
  background: linear-gradient(135deg, #409eff, #67c23a);
  padding: 0;
  margin: 0;
}

.cta-inner {
  max-width: 600px;
  margin: 0 auto;
  text-align: center;
  padding: 64px 24px;
  color: #fff;
}

.cta-inner h2 {
  font-size: 32px;
  margin: 0 0 12px;
  font-weight: 700;
}

.cta-inner p {
  font-size: 16px;
  opacity: 0.85;
  margin: 0 0 28px;
}

.cta-btn {
  padding: 14px 36px;
  font-size: 16px;
  border-radius: 12px;
  font-weight: 600;
  background: #fff;
  color: var(--color-primary, #409eff);
  border: none;
  transition: all var(--el-transition-duration-fast);
}

.cta-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
  background: #fff;
  color: var(--color-primary-dark, #2c6bb5);
}

/* =============================================
   Footer
   ============================================= */
.site-footer {
  background: var(--color-bg-white, #fff);
  border-top: 1px solid var(--color-border-lighter, #ebeef5);
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

/* =============================================
   公共组件
   ============================================= */
.section-header {
  text-align: center;
  margin-bottom: 8px;
}

.section-tag {
  display: inline-block;
  padding: 2px 12px;
  border-radius: 12px;
  background: rgba(64, 158, 255, 0.1);
  color: var(--color-primary, #409eff);
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 8px;
}

.section-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
}

.section-desc {
  font-size: 15px;
  margin: 0;
}

/* ---- Fade-in-up scroll animation ---- */
.fade-in-up {
  opacity: 0;
  transform: translateY(30px);
  transition: opacity 0.6s ease, transform 0.6s ease;
}

.fade-in-up.in-view {
  opacity: 1;
  transform: translateY(0);
}

/* ---- Shared anim keyframes ---- */
@keyframes slideUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.3); opacity: 0.6; }
}

/* =============================================
   弹窗
   ============================================= */
.post-detail {
  line-height: 1.8;
}

.post-tags {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.post-content p {
  color: var(--color-text-primary, #303133);
  margin: 0 0 12px;
  white-space: pre-wrap;
}

/* =============================================
   Responsive
   ============================================= */
@media (max-width: 992px) {
  .hero-inner { flex-direction: column; text-align: center; }
  .hero-subtitle { max-width: none; }
  .hero-actions { justify-content: center; }
  .hero-meta { justify-content: center; }
  .hero-text h1 { font-size: 36px; }
  .steps-grid { grid-template-columns: 1fr; max-width: 400px; margin-left: auto; margin-right: auto; }
  .features-grid { grid-template-columns: repeat(2, 1fr); }
  .feed-grid { grid-template-columns: repeat(2, 1fr); }
  .visual-card { width: 320px; }
}

@media (max-width: 560px) {
  .hero-text h1 { font-size: 28px; }
  .features-grid { grid-template-columns: 1fr; }
  .feed-grid { grid-template-columns: 1fr; }
  .hero-meta { flex-direction: column; align-items: center; gap: 8px; }
  .meta-divider { display: none; }
  .visual-card { width: 100%; max-width: 320px; }
  .footer-inner { flex-direction: column; gap: 6px; text-align: center; }
}
</style>
