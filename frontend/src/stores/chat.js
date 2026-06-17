import { defineStore } from 'pinia'
import { chatApi } from '@/api/chat'

export const useChatStore = defineStore('chat', {
  state: () => ({
    sessions: [],
    currentSessionId: null,
    messages: [],
    isLoading: false,
  }),

  actions: {
    async loadSessions() {
      const res = await chatApi.listSessions()
      this.sessions = res.data.data || []
    },

    async createSession() {
      const res = await chatApi.createSession()
      const session = res.data.data
      this.sessions.unshift(session)
      this.currentSessionId = session.id
      this.messages = []
      return session
    },

    async loadMessages(sessionId) {
      this.currentSessionId = sessionId
      const res = await chatApi.getMessages(sessionId)
      this.messages = res.data.data || []
    },

    async sendMessage(content) {
      if (!this.currentSessionId) {
        await this.createSession()
      }
      this.isLoading = true
      try {
        await chatApi.sendMessage(this.currentSessionId, content)
        await this.loadMessages(this.currentSessionId)
      } finally {
        this.isLoading = false
      }
    }
  }
})