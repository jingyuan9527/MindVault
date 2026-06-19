import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('mindvault_token') || '')
  const user = ref(null)
  const loading = ref(false)

  async function login(username, password) {
    loading.value = true
    try {
      const res = await authApi.login({ username, password })
      token.value = res.data.data.token
      user.value = res.data.data
      localStorage.setItem('mindvault_token', token.value)
    } finally {
      loading.value = false
    }
  }

  async function fetchMe() {
    try {
      const res = await authApi.me()
      user.value = res.data.data
    } catch {
      user.value = null
      token.value = ''
      localStorage.removeItem('mindvault_token')
    }
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('mindvault_token')
    authApi.logout().catch(() => {})
  }

  function isLoggedIn() {
    return !!token.value
  }

  return { token, user, loading, login, fetchMe, logout, isLoggedIn }
})
