import request from './request'

// 认证
export const authApi = {
  register: (data) => request.post('/auth/register', data),
  login: (data) => request.post('/auth/login', data),
  me: () => request.get('/auth/me'),
  update: (data) => request.put('/auth/me', data)
}

// 项目
export const projectApi = {
  create: (formData) =>
    request.post('/projects', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }),
  list: () => request.get('/projects'),
  get: (id) => request.get(`/projects/${id}`),
  remove: (id) => request.delete(`/projects/${id}`)
}

// 生成
export const generationApi = {
  start: (projectId) => request.post(`/projects/${projectId}/generate`),
  get: (id) => request.get(`/generations/${id}`),
  listByProject: (projectId) => request.get(`/projects/${projectId}/generations`),
  listByUser: () => request.get('/generations')
}

// 装修小圈
export const communityApi = {
  list: (mine = false) => request.get('/posts', { params: { mine } }),
  create: (data) => request.post('/posts', data),
  remove: (id) => request.delete(`/posts/${id}`),
  toggleLike: (id) => request.post(`/posts/${id}/like`),
  comment: (id, content) => request.post(`/posts/${id}/comments`, { content }),
  removeComment: (id) => request.delete(`/comments/${id}`)
}

// 通用文件上传（返回可访问 URL）
export const fileApi = {
  upload: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return request.post('/files/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
