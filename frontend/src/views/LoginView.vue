<template>
  <div class="login-container">
    <div class="login-bg">
      <div class="bg-ornament top-right"></div>
      <div class="bg-ornament bottom-left"></div>
      <div class="bg-ornament center-glow"></div>
      <div class="bg-grid"></div>
    </div>
    <div class="login-content">
      <div class="login-brand">
        <div class="brand-icon">
          <svg viewBox="0 0 48 48" fill="none" class="w-10 h-10">
            <rect x="4" y="4" width="40" height="40" rx="10" stroke="currentColor" stroke-width="2"/>
            <path d="M16 32V20l8-6 8 6v12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M20 28h8v4h-8z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <h1 class="brand-title">MindVault</h1>
        <p class="brand-subtitle">知忆 · 你的AI增强第二大脑</p>
        <p class="brand-desc">智能管理知识，自动发现关联，让学习与思考事半功倍</p>
      </div>
      <div class="login-card">
        <n-form @submit.prevent="handleLogin" label-placement="top" :model="formData">
          <n-form-item label="用户名" path="username">
            <n-input v-model:value="formData.username" placeholder="请输入用户名" size="large">
              <template #prefix>
                <svg class="w-4 h-4" style="color: var(--color-text-secondary)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                </svg>
              </template>
            </n-input>
          </n-form-item>
          <n-form-item label="密码" path="password">
            <n-input v-model:value="formData.password" type="password" placeholder="请输入密码" size="large" @keyup.enter="handleLogin">
              <template #prefix>
                <svg class="w-4 h-4" style="color: var(--color-text-secondary)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                </svg>
              </template>
            </n-input>
          </n-form-item>

          <n-alert v-if="error" type="error" :show-icon="true" class="mb-4">
            {{ error }}
          </n-alert>

          <n-button attr-type="submit" :loading="loading" block size="large" class="login-btn">
            登录
          </n-button>
        </n-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const themeStore = useThemeStore()

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

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background-color: var(--color-bg);
}

.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-ornament {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
}
.bg-ornament.top-right {
  width: 500px;
  height: 500px;
  top: -200px;
  right: -150px;
  background: radial-gradient(circle, var(--color-ornament-primary) 0%, transparent 70%);
  animation: bg-shift 20s ease-in-out infinite;
}
.bg-ornament.bottom-left {
  width: 400px;
  height: 400px;
  bottom: -150px;
  left: -100px;
  background: radial-gradient(circle, var(--color-ornament-secondary) 0%, transparent 70%);
  animation: bg-shift 25s ease-in-out infinite reverse;
}
.bg-ornament.center-glow {
  width: 300px;
  height: 300px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, var(--color-ornament-accent) 0%, transparent 70%);
  animation: bg-shift 30s ease-in-out infinite;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(var(--color-border) 1px, transparent 1px),
    linear-gradient(90deg, var(--color-border) 1px, transparent 1px);
  background-size: 60px 60px;
  opacity: 0.12;
}

.login-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 48px;
  padding: 24px;
}

.login-brand {
  max-width: 320px;
}

.brand-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  color: white;
  background: var(--gradient-brand);
  box-shadow: 0 4px 20px var(--color-accent-light);
}

.brand-title {
  font-family: 'Outfit', ui-sans-serif, system-ui, sans-serif;
  font-size: 2.5rem;
  font-weight: 800;
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.03em;
  line-height: 1.1;
  margin-bottom: 4px;
}

.brand-subtitle {
  font-size: 0.95rem;
  color: var(--color-text-secondary);
  margin-bottom: 12px;
}

.brand-desc {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  opacity: 0.7;
  line-height: 1.6;
}

.login-card {
  width: 360px;
  padding: 32px;
  border-radius: 16px;
  background: var(--color-surface);
  backdrop-filter: blur(20px) saturate(1.4);
  -webkit-backdrop-filter: blur(20px) saturate(1.4);
  border: 1px solid var(--color-border);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

@media (max-width: 768px) {
  .login-content {
    flex-direction: column;
    gap: 32px;
    padding: 24px 16px;
    width: 100%;
  }
  .login-brand {
    max-width: 100%;
    text-align: center;
  }
  .brand-icon {
    margin: 0 auto 16px;
  }
  .login-card {
    width: 100%;
    max-width: 400px;
    padding: 24px;
  }
}

.login-btn {
  --n-color: var(--color-accent);
  --n-color-hover: var(--color-accent-hover);
  --n-color-pressed: var(--color-primary);
  --n-color-active: var(--color-accent);
  --n-ripple-color: var(--color-accent);
}
</style>