<template>
  <div class="min-h-screen flex items-center justify-center p-4"
    style="background-color: var(--color-bg)">
    <div class="w-full max-w-sm p-8 rounded-2xl shadow-lg scale-in-enter card"
      style="border: 1px solid var(--color-border)">
      <div class="text-center mb-8">
        <h1 class="font-display text-3xl font-bold" style="color: var(--color-accent)">MindVault</h1>
        <p class="text-sm mt-2" style="color: var(--color-text-secondary)">知忆 · 你的AI增强第二大脑</p>
      </div>

      <form @submit.prevent="handleLogin" class="space-y-4">
        <div>
          <label class="block text-sm font-medium mb-1" style="color: var(--color-text)">用户名</label>
          <input v-model="username" type="text" required
            class="input-field" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1" style="color: var(--color-text)">密码</label>
          <input v-model="password" type="password" required
            class="input-field" />
        </div>
        <p v-if="error" class="text-sm" style="color: var(--color-danger)">{{ error }}</p>
        <button type="submit" :disabled="loading"
          class="btn-primary w-full py-2.5 flex items-center justify-center">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
    </div>
  </div>
</template>
