import { createRouter, createWebHistory } from 'vue-router'
import KnowledgeView from '@/views/KnowledgeView.vue'
import ReviewView from '@/views/ReviewView.vue'
import FlashCardView from '@/views/FlashCardView.vue'
import WritingView from '@/views/WritingView.vue'
import DailyReviewView from '@/views/DailyReviewView.vue'
import OperationLogView from '@/views/OperationLogView.vue'
import SettingsView from '@/views/SettingsView.vue'

const routes = [
  { path: '/', name: 'knowledge', component: KnowledgeView },
  { path: '/review', name: 'review', component: ReviewView },
  { path: '/flashcards', name: 'flashcards', component: FlashCardView },
  { path: '/writing', name: 'writing', component: WritingView },
  { path: '/daily-review', name: 'dailyReview', component: DailyReviewView },
  { path: '/operation-logs', name: 'operationLogs', component: OperationLogView },
  { path: '/settings', name: 'settings', component: SettingsView }
]

export default createRouter({
  history: createWebHistory(),
  routes
})