# 筑梦家 · 房屋装修设计网站

上传房屋设计图，由 AI 生成可交互的 **3D 施工效果**（独立的三维动态网页）。

技术栈：**Spring Boot 3 + MySQL 8 + Vue 3 + Three.js**

---

## 功能概览

- 用户注册 / 登录（JWT 鉴权）
- 创建项目并上传房屋设计图（户型平面图 / CAD 导出图 / 手绘稿）
- 一键调用 AI 生成 3D 施工效果（异步任务 + 状态轮询）
- 独立的 **3D 动态效果网页**：自由旋转、缩放、平移、自动旋转、线框模式、光照调节
- AI 服务**可插拔**：配置了真实 API Key 走真实模型；未配置时自动降级为内置程序化户型重建，保证链路即开即用

---

## 目录结构

```
HouseDesign/
├── backend/            # Spring Boot 后端
│   ├── src/main/java/com/housedesign/
│   │   ├── config/     # 配置（CORS、静态资源、异步、属性）
│   │   ├── common/     # 统一响应、异常处理
│   │   ├── controller/ # REST 接口
│   │   ├── dto/        # 数据传输对象
│   │   ├── entity/     # JPA 实体
│   │   ├── repository/ # 数据访问
│   │   ├── security/   # JWT、鉴权、密码
│   │   └── service/    # 业务 + AI 图生3D 服务层
│   └── src/main/resources/application.yml
└── frontend/           # Vue3 前端
    └── src/
        ├── api/        # Axios 封装
        ├── router/     # 路由
        ├── store/      # Pinia 状态
        ├── three/      # Three.js 场景构建
        ├── components/ # 公共组件
        └── views/      # 页面
```

---

## 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.x（运行中）
- Node.js 18+

---

## 启动步骤

### 1. 数据库

无需手动建库，`application.yml` 中已配置 `createDatabaseIfNotExist=true`，会自动创建 `house_design` 库。
请根据本机情况修改数据库账号密码：

```yaml
# backend/src/main/resources/application.yml
spring:
  datasource:
    username: root
    password: root   # 改成你的 MySQL 密码
```

### 2. 后端

```bash
cd backend
mvn spring-boot:run
```

默认端口 `8080`。启动后会自动建表，并在 `backend/storage/` 下存放上传的设计图与生成结果。

### 3. 前端

```bash
cd frontend
npm install
npm run dev
```

默认端口 `5173`，已配置代理将 `/api`、`/files` 转发到后端 `8080`。
浏览器访问：http://localhost:5173

---

## 使用 AI 真实模型（可选）

默认 `provider: mock`，使用内置程序化生成（无需外部服务）。
如需接入真实的图生3D 服务（默认按 Meshy `image-to-3d` 接口契约实现）：

```yaml
# backend/src/main/resources/application.yml
app:
  ai:
    provider: meshy          # 非 mock 即走外部服务
    api-key: "你的API Key"    # 必填，否则自动回退到 mock
    base-url: "https://api.meshy.ai"
    timeout-seconds: 300
```

> 若要接入其它厂商（Tripo 等），实现 `ImageTo3DService` 接口新增一个实现，
> 并在 `ImageTo3DServiceFactory` 中按 `provider` 选择即可，其余流程无需改动。

---

## 核心接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录 |
| GET  | `/api/auth/me` | 当前用户 |
| POST | `/api/projects` | 创建项目并上传设计图（multipart） |
| GET  | `/api/projects` | 项目列表 |
| GET  | `/api/projects/{id}` | 项目详情 |
| DELETE | `/api/projects/{id}` | 删除项目 |
| POST | `/api/projects/{id}/generate` | 发起 3D 生成 |
| GET  | `/api/generations/{id}` | 查询生成任务状态 |
| GET  | `/api/projects/{id}/generations` | 项目的生成记录 |

---

## 生成流程说明

1. 用户上传设计图 → 落地到本地存储，创建 `DesignProject`
2. 点击“生成” → 创建 `GeneratedModel(PENDING)`，异步线程池处理
3. 处理器调用 `ImageTo3DServiceFactory` 选择实现：
   - **真实**：图片转 base64 → 创建任务 → 轮询 → 下载 glb 落地 → 返回 `modelUrl`
   - **降级**：分析图片 → 生成户型 `sceneConfig`（JSON）
4. 前端 3D 页面：有 `modelUrl` 用 `GLTFLoader` 加载；否则用 `sceneConfig` 程序化重建户型
```
