import * as THREE from 'three'

/**
 * 依据后端返回的 sceneConfig 程序化构建写实风格户型 3D 场景。
 * 通过 PBR 材质 + RoomEnvironment 环境光照 + 柔和阴影 + 细节家具，
 * 让程序化内景尽量接近真实装修效果（"离线写实化"）。
 * 返回 THREE.Group（整体居中到原点），并在 group.userData 上挂载房间中心与连接信息，供 VR 全景页使用。
 */
export function buildProceduralApartment(config) {
  const group = new THREE.Group()
  const wallHeight = config.wallHeight || 2.8
  const wallThickness = 0.12
  const palette = config.palette || {}

  const rooms = config.rooms || []
  const furniture = config.furniture || []

  // 计算整体包围盒用于居中
  let minX = Infinity, minZ = Infinity, maxX = -Infinity, maxZ = -Infinity
  rooms.forEach((r) => {
    minX = Math.min(minX, r.x)
    minZ = Math.min(minZ, r.z)
    maxX = Math.max(maxX, r.x + r.width)
    maxZ = Math.max(maxZ, r.z + r.depth)
  })
  if (!isFinite(minX)) {
    minX = 0; minZ = 0; maxX = 6; maxZ = 6
  }
  const centerX = (minX + maxX) / 2
  const centerZ = (minZ + maxZ) / 2
  const offset = new THREE.Vector3(centerX, 0, centerZ)

  // 地板底座
  const baseGeo = new THREE.BoxGeometry(maxX - minX + 0.6, 0.15, maxZ - minZ + 0.6)
  const baseMat = new THREE.MeshStandardMaterial({ color: 0xe8e8e8, roughness: 0.95 })
  const base = new THREE.Mesh(baseGeo, baseMat)
  base.position.set((minX + maxX) / 2 - centerX, -0.075, (minZ + maxZ) / 2 - centerZ)
  base.receiveShadow = true
  group.add(base)

  // 记录每个房间中心（供 VR 相机与热点使用）
  const roomCenters = {}

  rooms.forEach((room, idx) => {
    const rid = room.id != null ? room.id : idx
    const rx = room.x - centerX
    const rz = room.z - centerZ
    const w = room.width
    const d = room.depth
    const h = room.height || wallHeight
    const floorColor = room.floorColor || palette.floor || '#d9c7a3'
    const wallColor = room.wallColor || config.wallColor || palette.wall || '#f5f1ea'

    roomCenters[rid] = {
      id: rid,
      x: rx + w / 2,
      z: rz + d / 2,
      name: room.name,
      connections: room.connections || [],
      width: w,
      depth: d
    }

    // 地板（略带光泽，模拟瓷砖/木地板）
    const floorMat = new THREE.MeshStandardMaterial({
      color: new THREE.Color(floorColor),
      roughness: 0.55,
      metalness: 0.05
    })
    const floor = new THREE.Mesh(new THREE.BoxGeometry(w, 0.04, d), floorMat)
    floor.position.set(rx + w / 2, 0.02, rz + d / 2)
    floor.receiveShadow = true
    group.add(floor)

    // 天花板
    const ceilMat = new THREE.MeshStandardMaterial({ color: 0xfbfbfb, roughness: 0.95 })
    const ceil = new THREE.Mesh(new THREE.BoxGeometry(w, 0.06, d), ceilMat)
    ceil.position.set(rx + w / 2, h, rz + d / 2)
    ceil.receiveShadow = true
    group.add(ceil)

    // 墙面（不透明，营造封闭内景）
    const wallMat = new THREE.MeshStandardMaterial({
      color: new THREE.Color(wallColor),
      roughness: 0.92,
      metalness: 0
    })
    const walls = [
      { x: rx + w / 2, z: rz, w: w, d: wallThickness, face: 'back' },
      { x: rx + w / 2, z: rz + d, w: w, d: wallThickness, face: 'front' },
      { x: rx, z: rz + d / 2, w: wallThickness, d: d, face: 'left' },
      { x: rx + w, z: rz + d / 2, w: wallThickness, d: d, face: 'right' }
    ]
    walls.forEach((wl) => {
      const g = new THREE.BoxGeometry(wl.w, h, wl.d)
      const m = new THREE.Mesh(g, wallMat)
      m.position.set(wl.x, h / 2, wl.z)
      m.castShadow = true
      m.receiveShadow = true
      group.add(m)
      // 踢脚线
      const skirt = new THREE.Mesh(
        new THREE.BoxGeometry(wl.w + 0.001, 0.1, wl.d + 0.001),
        new THREE.MeshStandardMaterial({ color: 0xf0f0f0, roughness: 0.6 })
      )
      skirt.position.set(wl.x, 0.05, wl.z)
      group.add(skirt)
    })

    // 窗户（后墙，带日光感）—— 提升写实氛围
    addWindow(group, rx + w / 2, rz + 0.02, h)

    // 吸顶灯（发光体，提供氛围光源提示）
    const lamp = new THREE.Mesh(
      new THREE.SphereGeometry(0.12, 16, 16),
      new THREE.MeshStandardMaterial({ color: 0xffffff, emissive: 0xfff2cc, emissiveIntensity: 1.2 })
    )
    lamp.position.set(rx + w / 2, h - 0.18, rz + d / 2)
    group.add(lamp)
  })

  // 家具
  furniture.forEach((f) => {
    const obj = buildFurniture(f.type, f.color, palette)
    if (obj) {
      obj.position.set(f.x - centerX, 0, f.z - centerZ)
      obj.rotation.y = (f.rotation || 0) * (Math.PI / 180)
      group.add(obj)
    }
  })

  group.userData.size = Math.max(maxX - minX, maxZ - minZ)
  group.userData.roomCenters = roomCenters
  group.userData.offset = offset
  return group
}

/** 在后墙添加一扇带日光感的窗户（发光玻璃 + 窗框） */
function addWindow(group, cx, z, h) {
  const w = 1.8, wh = 1.4
  const y = 1.5
  // 发光玻璃
  const sky = makeSkyTexture()
  const glassMat = new THREE.MeshStandardMaterial({
    map: sky, emissive: 0xffffff, emissiveMap: sky, emissiveIntensity: 0.9,
    roughness: 0.1, metalness: 0
  })
  const glass = new THREE.Mesh(new THREE.PlaneGeometry(w, wh), glassMat)
  glass.position.set(cx, y, z + 0.07)
  group.add(glass)
  // 窗框
  const frameMat = new THREE.MeshStandardMaterial({ color: 0xffffff, roughness: 0.5 })
  const ft = 0.06
  const bar = (bw, bh, bx, by) => {
    const m = new THREE.Mesh(new THREE.BoxGeometry(bw, bh, 0.1), frameMat)
    m.position.set(bx, by, z + 0.09)
    group.add(m)
  }
  bar(w + ft * 2, ft, cx, y + wh / 2)        // 上
  bar(w + ft * 2, ft, cx, y - wh / 2)        // 下
  bar(ft, wh + ft * 2, cx - w / 2, y)        // 左
  bar(ft, wh + ft * 2, cx + w / 2, y)        // 右
  bar(ft, wh, cx, y)                          // 中竖
  bar(w, ft, cx, y)                          // 中横
}

function makeSkyTexture() {
  const c = document.createElement('canvas')
  c.width = 256; c.height = 256
  const ctx = c.getContext('2d')
  const g = ctx.createLinearGradient(0, 0, 0, 256)
  g.addColorStop(0, '#bfe3ff')
  g.addColorStop(0.55, '#e8f4ff')
  g.addColorStop(1, '#fbfdff')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, 256, 256)
  // 远处云带
  ctx.fillStyle = 'rgba(255,255,255,0.6)'
  for (let i = 0; i < 5; i++) {
    const y = 60 + i * 30
    ctx.beginPath()
    ctx.ellipse(60 + i * 40, y, 50, 12, 0, 0, Math.PI * 2)
    ctx.fill()
  }
  const tex = new THREE.CanvasTexture(c)
  tex.colorSpace = THREE.SRGBColorSpace
  return tex
}

/* ---------------- 材质与家具 ---------------- */

function mat(color, roughness = 0.7, metalness = 0) {
  return new THREE.MeshStandardMaterial({
    color: new THREE.Color(color), roughness, metalness
  })
}

function box(w, h, d, color, rough, metal) {
  const m = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), mat(color, rough, metal))
  m.castShadow = true
  m.receiveShadow = true
  return m
}

function cyl(rt, rb, h, color, rough, seg = 16) {
  const m = new THREE.Mesh(new THREE.CylinderGeometry(rt, rb, h, seg), mat(color, rough))
  m.castShadow = true
  m.receiveShadow = true
  return m
}

/** 根据家具类型生成较写实的组合几何体 */
function buildFurniture(type, color, palette) {
  const g = new THREE.Group()
  const c = color || '#9e9e9e'
  const wood = palette.wood || '#8d6e63'
  switch (type) {
    case 'sofa': {
      const fabric = mat(c, 0.85)
      const base = new THREE.Mesh(new THREE.BoxGeometry(1.9, 0.35, 0.9), fabric)
      base.position.y = 0.25; base.castShadow = base.receiveShadow = true; g.add(base)
      const seat = new THREE.Mesh(new THREE.BoxGeometry(1.7, 0.18, 0.8), fabric)
      seat.position.set(0, 0.5, 0.02); seat.castShadow = true; g.add(seat)
      const back = new THREE.Mesh(new THREE.BoxGeometry(1.9, 0.5, 0.22), fabric)
      back.position.set(0, 0.62, -0.36); back.castShadow = true; g.add(back)
      for (const sx of [-0.85, 0.85]) {
        const arm = new THREE.Mesh(new THREE.BoxGeometry(0.22, 0.45, 0.9), fabric)
        arm.position.set(sx, 0.45, 0); arm.castShadow = true; g.add(arm)
      }
      for (const [lx, lz] of [[-0.8, -0.35], [0.8, -0.35], [-0.8, 0.35], [0.8, 0.35]]) {
        const leg = cyl(0.04, 0.04, 0.12, '#3a2c20', 0.6); leg.position.set(lx, 0.06, lz); g.add(leg)
      }
      break
    }
    case 'tv_cabinet': {
      const cab = box(1.7, 0.4, 0.42, wood, 0.6); cab.position.y = 0.2; g.add(cab)
      const tv = box(1.2, 0.7, 0.05, '#15171c', 0.4, 0.2); tv.position.set(0, 1.0, 0); g.add(tv)
      const screen = new THREE.Mesh(new THREE.PlaneGeometry(1.1, 0.62),
        new THREE.MeshStandardMaterial({ color: 0x0b0d12, emissive: 0x223044, emissiveIntensity: 0.5, roughness: 0.2 }))
      screen.position.set(0, 1.0, 0.03); g.add(screen)
      break
    }
    case 'coffee_table': {
      const top = box(1.0, 0.07, 0.55, wood, 0.45); top.position.y = 0.42; g.add(top)
      const shelf = box(0.9, 0.04, 0.45, wood, 0.5); shelf.position.y = 0.12; g.add(shelf)
      for (const [dx, dz] of [[-0.42, -0.22], [0.42, -0.22], [-0.42, 0.22], [0.42, 0.22]]) {
        const leg = box(0.06, 0.42, 0.06, wood, 0.5); leg.position.set(dx, 0.21, dz); g.add(leg)
      }
      break
    }
    case 'plant': {
      const pot = cyl(0.18, 0.14, 0.32, '#b08968', 0.7); pot.position.y = 0.16; g.add(pot)
      const leafMat = mat('#4f8f43', 0.8)
      for (let i = 0; i < 5; i++) {
        const leaf = new THREE.Mesh(new THREE.IcosahedronGeometry(0.22 + Math.random() * 0.1, 0), leafMat)
        leaf.position.set((Math.random() - 0.5) * 0.3, 0.55 + Math.random() * 0.35, (Math.random() - 0.5) * 0.3)
        leaf.castShadow = true
        g.add(leaf)
      }
      break
    }
    case 'rug': {
      const rug = new THREE.Mesh(new THREE.BoxGeometry(1.8, 0.02, 1.2),
        new THREE.MeshStandardMaterial({ color: new THREE.Color(c), roughness: 1 }))
      rug.position.y = 0.03; rug.receiveShadow = true; g.add(rug)
      const inner = new THREE.Mesh(new THREE.BoxGeometry(1.4, 0.022, 0.9),
        new THREE.MeshStandardMaterial({ color: new THREE.Color(c).offsetHSL(0, 0, 0.08), roughness: 1 }))
      inner.position.y = 0.035; inner.receiveShadow = true; g.add(inner)
      break
    }
    case 'bed': {
      const frame = box(1.7, 0.3, 2.1, wood, 0.6); frame.position.y = 0.15; g.add(frame)
      const mattress = box(1.55, 0.22, 2.0, '#f3efe9', 0.9); mattress.position.y = 0.43; g.add(mattress)
      const duvet = box(1.58, 0.1, 1.5, c, 0.95); duvet.position.set(0, 0.56, 0.25); g.add(duvet)
      for (const px of [-0.4, 0.4]) {
        const pillow = box(0.6, 0.16, 0.42, '#ffffff', 0.95); pillow.position.set(px, 0.6, -0.7); g.add(pillow)
      }
      const head = box(1.7, 0.8, 0.12, wood, 0.6); head.position.set(0, 0.55, -1.05); g.add(head)
      break
    }
    case 'wardrobe': {
      const body = box(1.5, 2.3, 0.62, wood, 0.5); body.position.y = 1.15; g.add(body)
      const doorMat = mat(new THREE.Color(wood).offsetHSL(0, 0, 0.05).getStyle(), 0.5)
      for (const dx of [-0.37, 0.37]) {
        const door = new THREE.Mesh(new THREE.BoxGeometry(0.7, 2.1, 0.04), doorMat)
        door.position.set(dx, 1.15, 0.32); g.add(door)
        const knob = cyl(0.02, 0.02, 0.06, '#caa84a', 0.3, 8); knob.position.set(dx + (dx < 0 ? 0.28 : -0.28), 1.15, 0.35); g.add(knob)
      }
      break
    }
    case 'nightstand': {
      const body = box(0.5, 0.5, 0.42, wood, 0.5); body.position.y = 0.25; g.add(body)
      const knob = cyl(0.02, 0.02, 0.05, '#caa84a', 0.3, 8); knob.position.set(0.15, 0.28, 0.22); g.add(knob)
      break
    }
    case 'kitchen_counter': {
      const base = box(2.3, 0.9, 0.62, wood, 0.5); base.position.y = 0.45; g.add(base)
      const top = box(2.36, 0.06, 0.66, '#5a5a5a', 0.3, 0.1); top.position.y = 0.92; g.add(top)
      const backsplash = box(2.3, 0.5, 0.04, '#e8e8e8', 0.4); backsplash.position.set(0, 1.25, -0.3); g.add(backsplash)
      const upper = box(2.0, 0.7, 0.35, wood, 0.5); upper.position.set(0, 2.0, -0.25); g.add(upper)
      const sink = box(0.5, 0.08, 0.4, '#cfd8dc', 0.3, 0.4); sink.position.set(0.4, 0.96, 0); g.add(sink)
      const faucet = cyl(0.02, 0.02, 0.3, '#b0bec5', 0.2, 8); faucet.position.set(0.4, 1.15, -0.1); g.add(faucet)
      break
    }
    case 'bathtub': {
      const tub = box(1.65, 0.55, 0.82, '#f3f3f3', 0.4); tub.position.y = 0.28; g.add(tub)
      const inner = new THREE.Mesh(new THREE.BoxGeometry(1.45, 0.42, 0.62),
        new THREE.MeshStandardMaterial({ color: 0xbfe3ff, roughness: 0.1, metalness: 0, transparent: true, opacity: 0.85 }))
      inner.position.y = 0.42; g.add(inner)
      const faucet = cyl(0.02, 0.02, 0.25, '#b0bec5', 0.2, 8); faucet.position.set(0.7, 0.55, -0.35); g.add(faucet)
      break
    }
    default: {
      const d = box(0.6, 0.6, 0.6, c, 0.7); d.position.y = 0.3; g.add(d)
    }
  }
  return g
}
