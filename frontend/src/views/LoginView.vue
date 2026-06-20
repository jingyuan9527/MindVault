<template>
  <div class="min-h-screen flex items-center justify-center p-4" style="background-color: var(--color-bg)">
    <n-card class="w-full max-w-sm" :bordered="true" size="large">
      <div class="text-center mb-6">
        <h1 class="font-display text-3xl font-bold" style="color: #4D6A4A">MindVault</h1>
        <p class="text-sm mt-2" style="color: var(--color-text-secondary)">知忆 · 你的AI增强第二大脑</p>
      </div>

      <n-form @submit.prevent="handleLogin" label-placement="top" :model="formData">
        <n-form-item label="用户名" path="username">
          <n-input v-model:value="formData.username" placeholder="请输入用户名" />
        </n-form-item>
        <n-form-item label="密码" path="password">
          <n-input v-model:value="formData.password" type="password" placeholder="请输入密码" @keyup.enter="handleLogin" />
        </n-form-item>

        <n-alert v-if="error" type="error" :show-icon="true" class="mb-4">
          {{ error }}
        </n-alert>

        <n-button type="primary" attr-type="submit" :loading="loading" block size="large">
          登录
        </n-button>
      </n-form>
    </n-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formData = reactive({ username: '', password: '' })
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  loading.value = true
  error.value = ''
  try {
    await auth.login(formData.username, formData.password)
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    error.value = e.response?.data?.message || '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>
