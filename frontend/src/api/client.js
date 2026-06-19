import axios from 'axios'

const client = axios.create({ baseURL: '/api/v1' })

client.interceptors.request.use(config => {
  const token = localStorage.getItem('mindvault_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let toastStore = null
let retryCount = 0

client.interceptors.response.use(
  response => {
    retryCount = 0
    return response
  },
  async error => {
    const status = error.response?.status
    const data = error.response?.data

    if (!toastStore) {
      try {
        const { useToastStore } = await import('@/stores/toast')
        toastStore = useToastStore()
      } catch {}
    }

    if (status === 401) {
      localStorage.removeItem('mindvault_token')
      const auth = (await import('@/stores/auth')).useAuthStore()
      auth.user = null
      if (window.location.pathname !== '/login') {
        toastStore?.error('登录已过期，请重新登录')
        setTimeout(() => { window.location.href = '/login' }, 1000)
      }
    } else if (status >= 500) {
      toastStore?.error(data?.message || '服务器内部错误，请稍后重试')
    } else if (!error.response) {
      toastStore?.error('网络连接失败，请检查网络')
    } else if (status === 400) {
      toastStore?.error(data?.message || '请求参数错误')
    } else if (status === 404) {
      toastStore?.error(data?.message || '资源不存在')
    } else if (status === 429) {
      retryCount++
      if (retryCount <= 3) {
        toastStore?.info('请求过于频繁，正在重试...')
        await new Promise(r => setTimeout(r, retryCount * 1000))
        return client(error.config)
      }
      toastStore?.error('请求过于频繁，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default client
