<template>
  <div class="min-h-screen flex items-center justify-center p-4"
    :style="{ backgroundColor: 'var(--color-bg)' }">
    <div class="w-full max-w-sm p-8 rounded-2xl shadow-lg"
      :style="{ backgroundColor: 'var(--color-surface)', border: '1px solid var(--color-border)' }">
      <div class="text-center mb-8">
        <h1 class="font-display text-3xl font-bold" :style="{ color: 'var(--color-accent)' }">MindVault</h1>
        <p class="text-sm mt-2" :style="{ color: 'var(--color-text-secondary)' }">知忆 · 你的AI增强第二大脑</p>
      </div>

      <form @submit.prevent="handleLogin" class="space-y-4">
        <div>
          <label class="block text-sm font-medium mb-1" :style="{ color: 'var(--color-text)' }">用户名</label>
          <input v-model="username" type="text" required
            class="w-full px-3 py-2.5 rounded-lg border text-sm outline-none transition-colors"
            :style="{
              backgroundColor: 'var(--color-bg)',
              borderColor: 'var(--color-border)',
              color: 'var(--color-text)'
            }"
            @focus="$event.target.style.borderColor = 'var(--color-accent)'"
            @blur="$event.target.style.borderColor = 'var(--color-border)'">
        </div>
        <div>
          <label class="block text-sm font-medium mb-1" :style="{ color: 'var(--color-text)' }">密码</label>
          <input v-model="password" type="password" required
            class="w-full px-3 py-2.5 rounded-lg border text-sm outline-none transition-colors"
            :style="{
              backgroundColor: 'var(--color-bg)',
              borderColor: 'var(--color-border)',
              color: 'var(--color-text)'
            }"
            @focus="$event.target.style.borderColor = 'var(--color-accent)'"
            @blur="$event.target.style.borderColor = 'var(--color-border)'">
        </div>
        <p v-if="error" class="text-sm" style="color: #ef4444">{{ error }}</p>
        <button type="submit" :disabled="loading"
          class="w-full py-2.5 rounded-lg text-sm font-medium transition-all duration-150"
          :style="{
            backgroundColor: loading ? 'var(--color-muted)' : 'var(--color-accent)',
            color: 'white',
            opacity: loading ? 0.7 : 1
          }">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  loading.value = true
  error.value = ''
  try {
    await auth.login(username.value, password.value)
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    error.value = e.response?.data?.message || '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>
