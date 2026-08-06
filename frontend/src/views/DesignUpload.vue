<template>
  <div class="upload-page">
    <AppHeader />

    <div class="page-container narrow">
      <!-- 步骤指示器 -->
      <div class="stepper">
        <div class="step" :class="{ active: step >= 1, done: step > 1 }">
          <div class="step-circle">1</div>
          <span class="step-label">基本信息</span>
        </div>
        <div class="step-connector" :class="{ active: step > 1 }"></div>
        <div class="step" :class="{ active: step >= 2, done: step > 2 }">
          <div class="step-circle">2</div>
          <span class="step-label">选择风格</span>
        </div>
        <div class="step-connector" :class="{ active: step > 2 }"></div>
        <div class="step" :class="{ active: step >= 3 }">
          <div class="step-circle">3</div>
          <span class="step-label">上传设计图</span>
        </div>
      </div>

      <div class="upload-card">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <!-- Step 1: 基本信息 -->
          <div v-show="step === 1" class="step-panel animate-fade-in">
            <h3 class="panel-title">项目信息</h3>
            <el-form-item label="项目名称" prop="name">
              <el-input
                v-model="form.name"
                placeholder="例如：三居室 · 现代简约"
                size="large"
                :prefix-icon="Edit"
                clearable
              />
            </el-form-item>
            <el-form-item label="项目描述（选填）">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="4"
                placeholder="记录你的设计想法、户型情况或特殊需求…"
              />
            </el-form-item>
            <div class="step-actions">
              <el-button size="large" disabled style="visibility: hidden">上一步</el-button>
              <el-button type="primary" size="large" @click="nextStep(1)">下一步</el-button>
            </div>
          </div>

          <!-- Step 2: 选择风格 -->
          <div v-show="step === 2" class="step-panel animate-fade-in">
            <h3 class="panel-title">选择装修风格</h3>
            <p class="panel-desc text-muted">AI 将根据你选择的风格生成对应的 3D 效果</p>
            <el-form-item prop="style">
              <div class="style-grid">
                <div
                  v-for="s in styles"
                  :key="s.code"
                  class="style-card"
                  :class="{ active: form.style === s.code }"
                  @click="form.style = s.code"
                >
                  <div class="style-swatches">
                    <span
                      v-for="(c, i) in s.colors"
                      :key="i"
                      class="swatch"
                      :style="{ background: c }"
                    ></span>
                  </div>
                  <div class="style-name">{{ s.name }}</div>
                  <div class="style-desc">{{ s.desc }}</div>
                  <div class="style-check" v-if="form.style === s.code">
                    <el-icon><CircleCheckFilled /></el-icon>
                  </div>
                </div>
              </div>
            </el-form-item>
            <div class="step-actions">
              <el-button size="large" @click="step = 1">上一步</el-button>
              <el-button type="primary" size="large" @click="nextStep(2)">下一步</el-button>
            </div>
          </div>

          <!-- Step 3: 上传并提交 -->
          <div v-show="step === 3" class="step-panel animate-fade-in">
            <h3 class="panel-title">上传设计图</h3>
            <p class="panel-desc text-muted">支持户型平面图、CAD 导出图或手绘稿，jpg / png 格式，≤20MB</p>
            <el-form-item prop="file">
              <el-upload
                class="uploader"
                drag
                :auto-upload="false"
                :show-file-list="false"
                accept="image/*"
                :on-change="onFileChange"
              >
                <div v-if="!previewUrl" class="upload-empty">
                  <div class="upload-icon-wrap">
                    <el-icon class="upload-icon"><UploadFilled /></el-icon>
                  </div>
                  <div class="upload-text">将设计图拖到此处，或<span class="upload-link">点击上传</span></div>
                  <div class="upload-tip">支持 jpg / png，≤20MB</div>
                </div>
                <div v-else class="preview-wrap">
                  <img :src="previewUrl" class="preview" />
                  <div class="preview-overlay">
                    <el-icon class="preview-replace"><Refresh /></el-icon>
                    <span>重新选择</span>
                  </div>
                </div>
              </el-upload>
            </el-form-item>

            <div class="summary-card" v-if="previewUrl">
              <div class="summary-row">
                <span class="summary-label">项目名称</span>
                <span class="summary-value">{{ form.name }}</span>
              </div>
              <div class="summary-row">
                <span class="summary-label">装修风格</span>
                <el-tag size="small" effect="light" type="warning" round>{{ currentStyleName }}</el-tag>
              </div>
            </div>

            <div class="step-actions">
              <el-button size="large" @click="step = 2">上一步</el-button>
              <el-button
                type="primary"
                size="large"
                :loading="submitting"
                @click="onSubmit"
                class="submit-btn"
              >
                <span v-if="!submitting">创建项目并提交</span>
              </el-button>
            </div>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import { projectApi } from '@/api'
import { DESIGN_STYLES, DEFAULT_STYLE } from '@/constants/styles'

const router = useRouter()
const formRef = ref()
const step = ref(1)
const submitting = ref(false)
const previewUrl = ref('')
const selectedFile = ref(null)
const styles = DESIGN_STYLES

const form = reactive({ name: '', description: '', style: DEFAULT_STYLE })
const rules = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  style: [{ required: true, message: '请选择装修风格', trigger: 'change' }],
  file: [{ validator: (r, v, cb) => { selectedFile.value ? cb() : cb(new Error('请上传设计图')) }, trigger: 'change' }]
}

const currentStyleName = computed(() => {
  const s = styles.find((x) => x.code === form.style)
  return s ? s.name : ''
})

function nextStep(current) {
  formRef.value.validateField(current === 1 ? 'name' : 'style', (valid) => {
    if (valid) step.value++
  })
}

function onFileChange(file) {
  const raw = file.raw
  if (!raw) return
  if (raw.size > 20 * 1024 * 1024) {
    ElMessage.error('文件不能超过 20MB')
    return
  }
  selectedFile.value = raw
  previewUrl.value = URL.createObjectURL(raw)
}

async function onSubmit() {
  await formRef.value.validate()
  const fd = new FormData()
  fd.append('name', form.name)
  fd.append('description', form.description || '')
  fd.append('style', form.style)
  fd.append('designImage', selectedFile.value)

  submitting.value = true
  try {
    const project = await projectApi.create(fd)
    ElMessage.success('项目创建成功！AI 正在分析设计图…')
    router.push(`/projects/${project.id}`)
  } catch (e) {
    /* 错误提示已由拦截器处理 */
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.upload-page {
  min-height: 100vh;
  background: var(--color-bg, #f5f7fa);
}

.narrow {
  max-width: 680px;
  padding-top: 24px;
  padding-bottom: 48px;
}

/* ---- 步骤指示器 ---- */
.stepper {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32px;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.step-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 15px;
  background: #f0f2f5;
  color: var(--color-text-placeholder, #c0c4cc);
  transition: all var(--el-transition-duration-normal, 0.3s);
}

.step.active .step-circle {
  background: var(--color-primary, #409eff);
  color: #fff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.step.done .step-circle {
  background: var(--color-success, #67c23a);
  color: #fff;
}

.step-label {
  font-size: 12px;
  color: var(--color-text-placeholder, #c0c4cc);
  white-space: nowrap;
  transition: color var(--el-transition-duration-fast);
}

.step.active .step-label {
  color: var(--color-text-primary, #303133);
  font-weight: 500;
}

.step-connector {
  width: 60px;
  height: 2px;
  background: #f0f2f5;
  margin: 0 12px;
  margin-bottom: 22px;
  border-radius: 2px;
  transition: background var(--el-transition-duration-normal);
}

.step-connector.active {
  background: var(--color-success, #67c23a);
}

/* ---- 上传卡片 ---- */
.upload-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06), 0 0 0 1px rgba(255, 255, 255, 0.5);
}

.step-panel {
  animation: fadeIn 0.35s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.panel-title {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
}

.panel-desc {
  margin: 0 0 20px;
  font-size: 13px;
}

.step-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 24px;
  gap: 12px;
}

.submit-btn {
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.3);
  transition: all var(--el-transition-duration-fast);
  padding: 12px 28px;
  font-weight: 600;
}

.submit-btn:hover {
  box-shadow: 0 8px 28px rgba(64, 158, 255, 0.4);
  transform: translateY(-1px);
}

/* ---- 风格选择 ---- */
.style-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 12px;
}

.style-card {
  position: relative;
  border: 2px solid var(--color-border-lighter, #ebeef5);
  border-radius: 14px;
  padding: 14px;
  cursor: pointer;
  transition: all var(--el-transition-duration-fast);
}

.style-card:hover {
  border-color: #c6d4ff;
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.1);
}

.style-card.active {
  border-color: var(--color-primary, #409eff);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.18);
}

.style-swatches {
  display: flex;
  gap: 4px;
  margin-bottom: 10px;
}

.swatch {
  flex: 1;
  height: 24px;
  border-radius: 4px;
  transition: transform var(--el-transition-duration-fast);
}

.style-card:hover .swatch {
  transform: scaleY(1.15);
}

.style-name {
  font-weight: 600;
  font-size: 14px;
  line-height: 1.3;
}

.style-desc {
  font-size: 12px;
  margin-top: 4px;
  line-height: 1.4;
  color: var(--color-text-secondary, #909399);
}

.style-check {
  position: absolute;
  top: 8px;
  right: 8px;
  color: var(--color-primary, #409eff);
  font-size: 20px;
  animation: scaleIn 0.25s ease;
}

@keyframes scaleIn {
  from { transform: scale(0); }
  to { transform: scale(1); }
}

/* ---- 上传区域 ---- */
.uploader {
  width: 100%;
}

:deep(.el-upload),
:deep(.el-upload-dragger) {
  width: 100%;
}

:deep(.el-upload-dragger) {
  border-radius: 14px;
  border: 2px dashed var(--color-border, #dcdfe6);
  transition: all var(--el-transition-duration-normal, 0.3s);
}

:deep(.el-upload-dragger:hover) {
  border-color: var(--color-primary, #409eff);
  background: rgba(64, 158, 255, 0.02);
}

:deep(.el-upload-dragger.is-dragover) {
  border-color: var(--color-primary, #409eff);
  background: rgba(64, 158, 255, 0.06);
}

.upload-empty {
  padding: 36px 20px;
  text-align: center;
}

.upload-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #e8f3ff, #c8e0ff);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
}

.upload-icon {
  font-size: 28px;
  color: var(--color-primary, #409eff);
}

.upload-text {
  font-size: 15px;
  color: var(--color-text-regular, #606266);
  margin-bottom: 8px;
}

.upload-link {
  color: var(--color-primary, #409eff);
  font-weight: 500;
}

.upload-tip {
  font-size: 12px;
  color: var(--color-text-placeholder, #c0c4cc);
}

.preview-wrap {
  position: relative;
  overflow: hidden;
}

.preview {
  max-width: 100%;
  max-height: 360px;
  border-radius: 10px;
  display: block;
  margin: 0 auto;
}

.preview-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #fff;
  font-size: 14px;
  opacity: 0;
  transition: opacity var(--el-transition-duration-normal, 0.3s);
  border-radius: 10px;
  cursor: pointer;
}

.preview-wrap:hover .preview-overlay {
  opacity: 1;
}

.preview-replace {
  font-size: 24px;
}

/* ---- 摘要 ---- */
.summary-card {
  background: #f7f8fa;
  border-radius: 12px;
  padding: 16px;
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
}

.summary-label {
  color: var(--color-text-secondary, #909399);
}

.summary-value {
  font-weight: 500;
}

@media (max-width: 560px) {
  .style-grid { grid-template-columns: 1fr 1fr; }
  .stepper { gap: 4px; }
  .step-connector { width: 30px; }
}
</style>
