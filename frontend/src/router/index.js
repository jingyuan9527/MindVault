import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import KnowledgeView from '@/views/KnowledgeView.vue'
import ChatView from '@/views/ChatView.vue'
import ReviewView from '@/views/ReviewView.vue'
import FlashCardView from '@/views/FlashCardView.vue'
import WritingView from '@/views/WritingView.vue'
import DailyReviewView from '@/views/DailyReviewView.vue'
import TokenUsageView from '@/views/TokenUsageView.vue'
import OperationLogView from '@/views/OperationLogView.vue'
import SettingsView from '@/views/SettingsView.vue'
import BackupView from '@/views/BackupView.vue'
import SystemMonitorView from '@/views/SystemMonitorView.vue'
import UserManagementView from '@/views/UserManagementView.vue'
import SystemConfigView from '@/views/SystemConfigView.vue'
import LoginView from '@/views/LoginView.vue'

const routes = [
  { path: '/login', name: 'login', component: LoginView, meta: { requiresAuth: false } },
  { path: '/', name: 'knowledge', component: KnowledgeView, meta: { requiresAuth: true } },
  { path: '/chat', name: 'chat', component: ChatView, meta: { requiresAuth: true } },
  { path: '/review', name: 'review', component: ReviewView, meta: { requiresAuth: true } },
  { path: '/flashcards', name: 'flashcards', component: FlashCardView, meta: { requiresAuth: true } },
  { path: '/writing', name: 'writing', component: WritingView, meta: { requiresAuth: true } },
  { path: '/daily-review', name: 'dailyReview', component: DailyReviewView, meta: { requiresAuth: true } },
  { path: '/token-usage', name: 'tokenUsage', component: TokenUsageView, meta: { requiresAuth: true } },
  { path: '/operation-logs', name: 'operationLogs', component: OperationLogView, meta: { requiresAuth: true } },
  { path: '/backups', name: 'backups', component: BackupView, meta: { requiresAuth: true } },
  { path: '/system', name: 'system', component: SystemMonitorView, meta: { requiresAuth: true } },
  { path: '/users', name: 'users', component: UserManagementView, meta: { requiresAuth: true } },
  { path: '/settings', name: 'settings', component: SettingsView, meta: { requiresAuth: true } },
  { path: '/system-config', name: 'systemConfig', component: SystemConfigView, meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth !== false && auth.isLoggedIn()) {
    if (!auth.user) {
      await auth.fetchMe()
    }
    if (!auth.isLoggedIn()) {
      return '/login'
    }
  } else if (to.meta.requiresAuth !== false && !auth.isLoggedIn()) {
    return '/login'
  }
  if (to.path === '/login' && auth.isLoggedIn()) {
    return '/'
  }
})

export default router
