<template>
  <div class="viewer-root" ref="rootEl">
    <div ref="canvasWrap" class="canvas-wrap"></div>

    <!-- 多房间写实照片（按真实比例平面展示，不变形） -->
    <img v-if="isTourMode" :key="currentRoomId" class="tour-img" :class="{ rotating: autoRotate }" :src="tourCurrentUrl" alt="装修效果图" />

    <!-- 顶部信息栏 -->
    <div class="topbar">
      <el-button circle @click="goBack"><el-icon><Back /></el-icon></el-button>
      <div class="title">
        <span class="dot" :class="{ ok: ready }"></span>
        {{ isTourMode ? '装修效果漫游' : '720° 全景漫游' }} · {{ styleText || '装修效果' }}
        <el-tag v-if="currentRoomName" size="small" effect="dark" type="warning" round style="margin-left: 6px">
          {{ currentRoomName }}
        </el-tag>
      </div>
    </div>

    <!-- 控制面板 -->
    <div class="control-panel" v-if="ready">
      <div class="panel-row">
        <span>自动旋转</span>
        <el-switch v-model="autoRotate" />
      </div>
      <template v-if="!isTourMode">
        <div class="panel-row" v-if="isPanorama">
          <span>氛围亮度</span>
          <el-slider v-model="brightness" :min="0.4" :max="1.6" :step="0.1" style="width: 120px" />
        </div>
        <el-button size="small" style="width: 100%" @click="resetView">重置视角</el-button>
      </template>
      <el-button size="small" style="width: 100%" @click="toggleFullscreen">
        <el-icon><FullScreen /></el-icon> 全屏
      </el-button>
      <p class="hint text-muted">
        {{ isTourMode ? '自动旋转：VR 式镜头缓缓扫视房间 · 点击下方缩略图切换房间' : '拖拽环视四周 · 点击门口热点或下方缩略图切换房间' }}
      </p>
    </div>

    <!-- 底部多房间缩略图（多房间切换） -->
    <div class="room-bar" v-if="ready && roomList.length > 1">
      <button
        v-for="r in roomList"
        :key="r.id"
        class="room-thumb"
        :class="{ active: r.id === currentRoomId }"
        @click="teleportTo(r.id)"
      >
        <span class="swatch" :style="r.thumb ? { backgroundImage: 'url(' + r.thumb + ')', backgroundSize: 'cover' } : { background: r.swatch }"></span>
        <span class="rname">{{ r.name }}</span>
      </button>
    </div>

    <!-- 加载 / 错误状态 -->
    <div v-if="loading" class="overlay">
      <el-icon class="loading-icon is-loading"><Loading /></el-icon>
      <p>正在加载场景…</p>
    </div>
    <div v-if="errorMsg" class="overlay">
      <el-icon class="err-icon"><WarningFilled /></el-icon>
      <p>{{ errorMsg }}</p>
      <el-button @click="goBack">返回</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { RoomEnvironment } from 'three/examples/jsm/environments/RoomEnvironment.js'
import { generationApi } from '@/api'
import { buildProceduralApartment } from '@/three/apartmentScene'

const route = useRoute()
const router = useRouter()

const rootEl = ref(null)
const canvasWrap = ref(null)
const loading = ref(true)
const ready = ref(false)
const errorMsg = ref('')
const autoRotate = ref(true)
const brightness = ref(1.0)
const isPanorama = ref(false)
const isTourMode = ref(false)
const styleText = ref('')
const currentRoomId = ref(0)
const currentRoomName = ref('')
const roomList = ref([])
const tourCurrentUrl = ref('')

let renderer, scene, camera, controls, animId
let contentGroup = null
let sphereMesh = null
let ambient, hemi, dirLight
const hotspots = []
const tourTextures = {}
const tourRooms = {}
let envTex = null

// 全景环视状态
let yaw = 0
let pitch = 0
let dragging = false
let lastX = 0
let lastY = 0
let moved = false
const raycaster = new THREE.Raycaster()
const pointer = new THREE.Vector2()

function goBack() {
  router.back()
}

function initThree() {
  const wrap = canvasWrap.value
  const width = wrap.clientWidth
  const height = wrap.clientHeight

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0xeef2f7)

  camera = new THREE.PerspectiveCamera(72, width / height, 0.05, 1000)
  camera.rotation.order = 'YXZ'
  camera.position.set(0, 1.6, 0)

  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  renderer.outputColorSpace = THREE.SRGBColorSpace
  wrap.appendChild(renderer.domElement)

  // 环境光照（RoomEnvironment 让 PBR 材质更真实）
  const pmrem = new THREE.PMREMGenerator(renderer)
  envTex = pmrem.fromScene(new RoomEnvironment(), 0.04).texture
  scene.environment = envTex

  ambient = new THREE.AmbientLight(0xffffff, 0.35)
  scene.add(ambient)

  hemi = new THREE.HemisphereLight(0xffffff, 0x9099a3, 0.5)
  scene.add(hemi)

  dirLight = new THREE.DirectionalLight(0xffffff, 1.0)
  dirLight.position.set(6, 14, 8)
  dirLight.castShadow = true
  dirLight.shadow.mapSize.set(2048, 2048)
  dirLight.shadow.camera.near = 1
  dirLight.shadow.camera.far = 60
  dirLight.shadow.camera.left = -25
  dirLight.shadow.camera.right = 25
  dirLight.shadow.camera.top = 25
  dirLight.shadow.camera.bottom = -25
  dirLight.shadow.bias = -0.0004
  scene.add(dirLight)

  window.addEventListener('resize', onResize)
  bindLookControls(renderer.domElement)
  animate()
}

function onResize() {
  if (!renderer || !canvasWrap.value) return
  const width = canvasWrap.value.clientWidth
  const height = canvasWrap.value.clientHeight
  camera.aspect = width / height
  camera.updateProjectionMatrix()
  renderer.setSize(width, height)
}

function animate() {
  animId = requestAnimationFrame(animate)
  if (isPanorama.value && !isTourMode.value) {
    if (autoRotate.value && !dragging) yaw += 0.0016
    camera.rotation.set(pitch, yaw, 0, 'YXZ')
  } else if (controls) {
    controls.update()
  }
  renderer.render(scene, camera)
}

/* ---------- 全景环视交互 ---------- */
function bindLookControls(dom) {
  dom.style.cursor = 'grab'
  dom.addEventListener('pointerdown', (e) => {
    dragging = true
    moved = false
    lastX = e.clientX
    lastY = e.clientY
    dom.style.cursor = 'grabbing'
  })
  dom.addEventListener('pointermove', (e) => {
    if (!dragging) return
    const dx = e.clientX - lastX
    const dy = e.clientY - lastY
    if (Math.abs(dx) + Math.abs(dy) > 3) moved = true
    lastX = e.clientX
    lastY = e.clientY
    yaw -= dx * 0.0026
    pitch -= dy * 0.0026
    pitch = Math.max(-1.35, Math.min(1.35, pitch))
  })
  const end = (e) => {
    if (dragging && !moved) tryHotspotClick(e)
    dragging = false
    dom.style.cursor = 'grab'
  }
  dom.addEventListener('pointerup', end)
  dom.addEventListener('pointerleave', () => { dragging = false; dom.style.cursor = 'grab' })
}

function tryHotspotClick(e) {
  const rect = renderer.domElement.getBoundingClientRect()
  pointer.x = ((e.clientX - rect.left) / rect.width) * 2 - 1
  pointer.y = -((e.clientY - rect.top) / rect.height) * 2 + 1
  raycaster.setFromCamera(pointer, camera)
  const hits = raycaster.intersectObjects(hotspots, false)
  if (hits.length > 0) {
    const target = hits[0].object.userData.targetId
    if (target != null) teleportTo(target)
  }
}

/* ---------- 内容加载 ---------- */
async function loadContent() {
  const id = route.params.generationId
  let gen
  try {
    gen = await generationApi.get(id)
  } catch (e) {
    errorMsg.value = '无法获取生成结果'
    loading.value = false
    return
  }
  if (gen.status !== 'SUCCESS') {
    errorMsg.value = '该结果尚未生成完成'
    loading.value = false
    return
  }

  try {
    if (gen.sceneConfig) {
      try {
        const cfg = JSON.parse(gen.sceneConfig)
        if ((cfg.type === 'panorama-tour' || cfg.type === 'photo-tour') && Array.isArray(cfg.rooms) && cfg.rooms.length) {
          // 真实 AI 多房间写实照片（智谱生成）：按真实比例平面展示
          isPanorama.value = true
          isTourMode.value = true
          await buildPanoramaTour(cfg)
          ready.value = true
          loading.value = false
          return
        }
      } catch (e) {
        // 不是 tour 配置，继续走其它分支
      }
    }
    if (gen.panoramaUrl) {
      // 真实 AI 产出的等距柱状全景图：球体内部贴图
      isPanorama.value = true
      isTourMode.value = false
      await loadPanoramaImage(gen.panoramaUrl)
    } else if (gen.sceneConfig) {
      // 程序化户型 -> 720° 内景漫游
      isPanorama.value = true
      isTourMode.value = false
      buildPanorama(gen.sceneConfig)
    } else if (gen.modelUrl) {
      // 真实 3D 模型：轨道环绕查看
      isPanorama.value = false
      isTourMode.value = false
      await loadGlb(gen.modelUrl)
    } else {
      errorMsg.value = '结果数据为空'
      loading.value = false
      return
    }
    ready.value = true
  } catch (e) {
    console.error(e)
    errorMsg.value = '场景渲染失败: ' + (e.message || e)
  } finally {
    loading.value = false
  }
}

function clearContent() {
  if (contentGroup) {
    scene.remove(contentGroup)
    contentGroup.traverse((o) => {
      if (o.isMesh) {
        o.geometry?.dispose()
        const mats = Array.isArray(o.material) ? o.material : [o.material]
        mats.forEach((m) => m?.dispose())
      }
    })
    contentGroup = null
  }
  sphereMesh = null
  for (const k in tourTextures) {
    tourTextures[k]?.dispose()
    delete tourTextures[k]
  }
  for (const k in tourRooms) delete tourRooms[k]
  tourCurrentUrl.value = ''
  isTourMode.value = false
  while (hotspots.length) {
    const s = hotspots.pop()
    scene.remove(s)
    s.material.map?.dispose()
    s.material.dispose()
  }
}

function buildPanorama(sceneConfigStr) {
  clearContent()
  const config = JSON.parse(sceneConfigStr)
  styleText.value = config.styleLabel || ''
  contentGroup = buildProceduralApartment(config)
  contentGroup.traverse((c) => { if (c.isMesh) { c.castShadow = true; c.receiveShadow = true } })
  scene.add(contentGroup)

  const centers = contentGroup.userData.roomCenters || {}
  roomList.value = Object.values(centers).map((r) => ({
    id: r.id ?? r.name,
    name: r.name,
    swatch: r.floorColor || '#cccccc'
  }))

  // 默认进入第一个房间
  const firstId = roomList.value.length ? roomList.value[0].id : 0
  teleportTo(firstId, true)
}

function loadPanoramaImage(url) {
  return new Promise((resolve, reject) => {
    clearContent()
    new THREE.TextureLoader().load(url, (tex) => {
      tex.colorSpace = THREE.SRGBColorSpace
      tex.mapping = THREE.EquirectangularReflectionMapping
      const geo = new THREE.SphereGeometry(50, 60, 40)
      const matl = new THREE.MeshBasicMaterial({ map: tex, side: THREE.BackSide })
      contentGroup = new THREE.Mesh(geo, matl)
      sphereMesh = contentGroup
      scene.add(contentGroup)
      isPanorama.value = true
      yaw = 0; pitch = 0
      camera.position.set(0, 0, 0.01)
      resolve()
    }, undefined, reject)
  })
}

function loadTexture(url) {
  return new Promise((resolve, reject) => {
    new THREE.TextureLoader().load(url, (tex) => {
      tex.colorSpace = THREE.SRGBColorSpace
      tex.mapping = THREE.EquirectangularReflectionMapping
      resolve(tex)
    }, undefined, reject)
  })
}

/* ---------- 多房间写实照片（智谱等真实 AI 产出，按真实比例平面展示） ---------- */
async function buildPanoramaTour(cfg) {
  clearContent()
  isTourMode.value = true
  styleText.value = cfg.styleLabel || ''
  // 记录各房间信息（平面照片展示，按图片真实比例，不变形）
  for (const r of cfg.rooms) {
    tourRooms[r.id] = r
  }

  roomList.value = cfg.rooms.map((r) => ({
    id: r.id,
    name: r.name,
    swatch: '#cfc8bd',
    thumb: r.panoramaUrl
  }))

  const first = cfg.rooms[0]
  teleportTo(first.id, true)
}

function teleportTo(roomId, silent) {
  if (isTourMode.value) {
    const room = tourRooms[String(roomId)] || Object.values(tourRooms)[0]
    if (!room) return
    currentRoomId.value = room.id
    currentRoomName.value = room.name
    tourCurrentUrl.value = room.panoramaUrl || ''
    return
  }
  if (!contentGroup || !contentGroup.userData.roomCenters) return
  const centers = contentGroup.userData.roomCenters
  const key = String(roomId)
  const cur = centers[key] || Object.values(centers)[0]
  if (!cur) return
  currentRoomId.value = cur.id ?? key
  currentRoomName.value = cur.name
  camera.position.set(cur.x, 1.6, cur.z)
  yaw = 0
  pitch = 0
  buildHotspots(cur)
}

function buildHotspots(cur) {
  // 清旧
  while (hotspots.length) {
    const s = hotspots.pop()
    scene.remove(s)
    s.material.map?.dispose()
    s.material.dispose()
  }
  if (isTourMode.value) {
    // 全景漫游：在相机正前方扇形布置相邻房间热点
    const conns = cur.connections || []
    const n = conns.length
    conns.forEach((nid, i) => {
      const nb = tourRooms[String(nid)]
      if (!nb) return
      const spread = 0.55
      const a = (n === 1) ? 0 : (-spread + (2 * spread) * (i / (n - 1)))
      const radius = 6
      const sprite = makeHotspotSprite(nb.name)
      sprite.position.set(Math.sin(a) * radius, 1.2, -Math.cos(a) * radius)
      sprite.userData.targetId = nb.id
      scene.add(sprite)
      hotspots.push(sprite)
    })
    return
  }
  const centers = contentGroup.userData.roomCenters
  const conns = cur.connections || []
  const curV = new THREE.Vector3(cur.x, 1.4, cur.z)
  conns.forEach((nid) => {
    const nb = centers[String(nid)]
    if (!nb) return
    const nbV = new THREE.Vector3(nb.x, 1.4, nb.z)
    const mid = curV.clone().add(nbV).multiplyScalar(0.5)
    // 往当前房间内缩一点，确保门口热点可见
    const dir = curV.clone().sub(mid).normalize()
    const pos = mid.add(dir.multiplyScalar(0.7))
    const sprite = makeHotspotSprite(nb.name)
    sprite.position.copy(pos)
    sprite.userData.targetId = nb.id ?? nid
    scene.add(sprite)
    hotspots.push(sprite)
  })
}

function makeHotspotSprite(label) {
  const c = document.createElement('canvas')
  c.width = 256; c.height = 128
  const ctx = c.getContext('2d')
  ctx.fillStyle = 'rgba(64,158,255,0.92)'
  ctx.beginPath(); ctx.roundRect(8, 28, 240, 72, 36); ctx.fill()
  ctx.fillStyle = '#fff'
  ctx.font = 'bold 40px sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText('进入 ' + label, 128, 64)
  const tex = new THREE.CanvasTexture(c)
  tex.colorSpace = THREE.SRGBColorSpace
  const matl = new THREE.SpriteMaterial({ map: tex, transparent: true, depthTest: false })
  const sp = new THREE.Sprite(matl)
  sp.scale.set(1.4, 0.7, 1)
  sp.renderOrder = 999
  return sp
}

function resetView() {
  if (isPanorama.value) {
    yaw = 0
    pitch = 0
  } else if (controls) {
    controls.reset()
  }
}

function toggleFullscreen() {
  const el = rootEl.value
  if (!document.fullscreenElement) {
    el.requestFullscreen?.()
  } else {
    document.exitFullscreen?.()
  }
}

function loadGlb(url) {
  return new Promise((resolve, reject) => {
    controls = new OrbitControls(camera, renderer.domElement)
    controls.enableDamping = true
    controls.dampingFactor = 0.08
    controls.autoRotate = autoRotate.value
    controls.autoRotateSpeed = 1.2
    controls.maxPolarAngle = Math.PI / 2.05
    const loader = new GLTFLoader()
    loader.load(
      url,
      (gltf) => {
        contentGroup = gltf.scene
        contentGroup.traverse((c) => { if (c.isMesh) { c.castShadow = true; c.receiveShadow = true } })
        scene.add(contentGroup)
        fitCameraTo(contentGroup)
        resolve()
      },
      undefined,
      (err) => reject(err)
    )
  })
}

function fitCameraTo(object) {
  const bbox = new THREE.Box3().setFromObject(object)
  const size = bbox.getSize(new THREE.Vector3())
  const center = bbox.getCenter(new THREE.Vector3())
  const maxDim = Math.max(size.x, size.y, size.z)
  const dist = maxDim * 1.6
  camera.position.set(center.x + dist, center.y + dist * 0.8, center.z + dist)
  controls.target.copy(center)
  controls.update()
}

watch(autoRotate, (v) => {
  if (controls) controls.autoRotate = v
})
watch(brightness, (v) => {
  if (ambient) ambient.intensity = 0.35 * v
  if (hemi) hemi.intensity = 0.5 * v
  if (sphereMesh && sphereMesh.material) {
    sphereMesh.material.color.setScalar(v)
  }
})

onMounted(async () => {
  initThree()
  await loadContent()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animId)
  window.removeEventListener('resize', onResize)
  if (controls) controls.dispose()
  if (renderer) {
    renderer.dispose()
    renderer.domElement?.remove()
  }
  envTex?.dispose()
  scene?.traverse((obj) => {
    if (obj.isMesh) {
      obj.geometry?.dispose()
      const mats = Array.isArray(obj.material) ? obj.material : [obj.material]
      mats.forEach((m) => m?.dispose())
    }
  })
})
</script>

<style scoped>
.viewer-root {
  position: fixed;
  inset: 0;
  overflow: hidden;
  background: #0f1115;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.canvas-wrap {
  width: 100%;
  height: 100%;
}

/* 平面照片：铺满 + VR 式扫视 + 切换淡入 */
.tour-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: #0f1115;
  z-index: 1;
  transform-origin: center center;
  animation: photoFade 0.55s ease;
}
.tour-img.rotating {
  animation: photoFade 0.55s ease, tourSway 22s ease-in-out infinite alternate;
}
@keyframes photoFade {
  from { opacity: 0; transform: scale(1.04); }
  to   { opacity: 1; transform: scale(1); }
}
@keyframes tourSway {
  0%   { transform: scale(1.16) translate(-8%, 0); }
  100% { transform: scale(1.16) translate(8%, 0); }
}

/* 顶栏 */
.topbar {
  position: absolute;
  top: 18px;
  left: 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 5;
}
.topbar :deep(.el-button) {
  background: rgba(20, 22, 28, 0.55);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.14);
  color: #fff;
  transition: background 0.2s, transform 0.2s;
}
.topbar :deep(.el-button):hover {
  background: rgba(64, 158, 255, 0.55);
  transform: scale(1.05);
}
.title {
  background: rgba(20, 22, 28, 0.55);
  backdrop-filter: blur(14px) saturate(140%);
  -webkit-backdrop-filter: blur(14px) saturate(140%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  padding: 9px 18px;
  border-radius: 22px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #f1f3f6;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.3);
}
.dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #e6a23c;
  animation: pulseWarn 1.8s infinite;
}
.dot.ok {
  background: #67c23a;
  animation: pulseOk 1.8s infinite;
}
@keyframes pulseWarn {
  0%   { box-shadow: 0 0 0 0 rgba(230, 162, 60, 0.6); }
  70%  { box-shadow: 0 0 0 8px rgba(230, 162, 60, 0); }
  100% { box-shadow: 0 0 0 0 rgba(230, 162, 60, 0); }
}
@keyframes pulseOk {
  0%   { box-shadow: 0 0 0 0 rgba(103, 194, 58, 0.6); }
  70%  { box-shadow: 0 0 0 8px rgba(103, 194, 58, 0); }
  100% { box-shadow: 0 0 0 0 rgba(103, 194, 58, 0); }
}

/* 控制面板 */
.control-panel {
  position: absolute;
  top: 18px;
  right: 18px;
  width: 234px;
  background: rgba(20, 22, 28, 0.55);
  backdrop-filter: blur(14px) saturate(140%);
  -webkit-backdrop-filter: blur(14px) saturate(140%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.35);
  display: flex;
  flex-direction: column;
  gap: 14px;
  z-index: 5;
  color: #f1f3f6;
  animation: panelIn 0.4s ease;
}
@keyframes panelIn {
  from { opacity: 0; transform: translateY(-10px); }
  to   { opacity: 1; transform: translateY(0); }
}
.panel-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}
.control-panel :deep(.el-button) {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: #f1f3f6;
  transition: all 0.2s;
}
.control-panel :deep(.el-button):hover {
  background: #409eff;
  border-color: #409eff;
}
.control-panel :deep(.el-slider__bar),
.control-panel :deep(.el-slider__button) {
  background: #409eff;
  border-color: #409eff;
}
.hint {
  font-size: 12px;
  margin: 4px 0 0;
  text-align: center;
  color: rgba(241, 243, 246, 0.7);
  line-height: 1.5;
}

/* 房间缩略图栏 */
.room-bar {
  position: absolute;
  bottom: 22px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 14px;
  padding: 12px 16px;
  background: rgba(20, 22, 28, 0.55);
  backdrop-filter: blur(14px) saturate(140%);
  -webkit-backdrop-filter: blur(14px) saturate(140%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.4);
  z-index: 5;
  max-width: 92vw;
  overflow-x: auto;
  animation: panelIn 0.4s ease;
}
.room-thumb {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  width: 94px;
  border: 2px solid transparent;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 14px;
  padding: 8px 6px;
  cursor: pointer;
  transition: transform 0.25s ease, background 0.25s, border-color 0.25s, box-shadow 0.25s;
  font: inherit;
  color: #f1f3f6;
}
.room-thumb:hover {
  background: rgba(255, 255, 255, 0.12);
  transform: translateY(-5px);
}
.room-thumb.active {
  border-color: #409eff;
  background: rgba(64, 158, 255, 0.18);
  box-shadow: 0 0 18px rgba(64, 158, 255, 0.45);
  transform: translateY(-5px);
}
.swatch {
  width: 100%;
  height: 58px;
  border-radius: 10px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.1);
  transition: transform 0.25s;
}
.room-thumb:hover .swatch {
  transform: scale(1.05);
}
.rname {
  font-size: 12px;
  color: #e6e9ef;
  font-weight: 500;
}
.room-thumb.active .rname {
  color: #fff;
}

/* 加载 / 错误 */
.overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  background: rgba(15, 17, 21, 0.82);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  color: #c9cdd4;
  z-index: 10;
  animation: photoFade 0.3s ease;
}
.overlay :deep(.el-button) {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}
.overlay p {
  max-width: 80vw;
  text-align: center;
  line-height: 1.6;
}
.loading-icon {
  font-size: 46px;
  color: #409eff;
}
.err-icon {
  font-size: 46px;
  color: #f56c6c;
}
</style>
