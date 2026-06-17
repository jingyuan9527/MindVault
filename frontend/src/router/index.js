import { createRouter, createWebHistory } from 'vue-router'
import KnowledgeView from '@/views/KnowledgeView.vue'
import ReviewView from '@/views/ReviewView.vue'
import SettingsView from '@/views/SettingsView.vue'

const routes = [
  { path: '/', name: 'knowledge', component: KnowledgeView },
  { path: '/review', name: 'review', component: ReviewView },
  { path: '/settings', name: 'settings', component: SettingsView }
]

export default createRouter({
  history: createWebHistory(),
  routes
})