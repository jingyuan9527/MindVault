import { defineStore } from 'pinia'
import { chatApi } from '@/api/chat'

export const useChatStore = defineStore('chat', {
  state: () => ({
    sessions: [],
    currentSessionId: null,
    messages: [],
    isLoading: false,
    streamingContent: '',
    streamingSources: [],
    streamingToolCall: null,
    streamingToolResults: [],
    cancelStream: null,
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

      const tempUserMsg = { id: Date.now(), role: 'USER', content, createdAt: new Date().toISOString() }
      this.messages.push(tempUserMsg)
      this.isLoading = true
      this.streamingContent = ''
      this.streamingSources = []
      this.streamingToolCall = null
      this.streamingToolResults = []

      const tempAgentMsgIndex = this.messages.length
      this.messages.push({ id: 'streaming', role: 'ASSISTANT', content: '', sources: '[]', toolResults: [], createdAt: new Date().toISOString() })

      this.cancelStream = chatApi.sendMessageStream(this.currentSessionId, content, {
        onToken: (token) => {
          this.streamingContent += token
          this.messages[tempAgentMsgIndex] = {
            ...this.messages[tempAgentMsgIndex],
            content: this.streamingContent,
            toolResults: this.streamingToolResults
          }
        },
        onSources: (sources) => {
          this.streamingSources = sources
          this.messages[tempAgentMsgIndex] = {
            ...this.messages[tempAgentMsgIndex],
            sources: JSON.stringify(sources)
          }
        },
        onToolCall: (data) => {
          this.streamingToolCall = data
        },
        onToolResult: (results) => {
          this.streamingToolCall = null
          this.streamingToolResults = results
          this.messages[tempAgentMsgIndex] = {
            ...this.messages[tempAgentMsgIndex],
            toolResults: results
          }
        },
        onBlocked: (msg) => {
          this.isLoading = false
          this.cancelStream = null
          this.messages.pop()
          this.messages.push({
            id: Date.now(), role: 'SYSTEM', content: msg,
            blocked: true, createdAt: new Date().toISOString()
          })
        },
        onDone: () => {
          this.isLoading = false
          this.streamingContent = ''
          this.streamingSources = []
          this.streamingToolCall = null
          this.streamingToolResults = []
          this.cancelStream = null
          this.loadMessages(this.currentSessionId)
        },
        onError: (error) => {
          this.isLoading = false
          this.streamingContent = ''
          this.streamingSources = []
          this.streamingToolCall = null
          this.streamingToolResults = []
          this.cancelStream = null
          this.messages[tempAgentMsgIndex] = {
            ...this.messages[tempAgentMsgIndex],
            content: `抱歉，处理消息时出错: ${error}`
          }
        }
      })
    }
  }
})