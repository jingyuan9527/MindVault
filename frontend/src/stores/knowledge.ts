import { defineStore } from 'pinia'
import { knowledgeApi } from '@/api/knowledge'

export const useKnowledgeStore = defineStore('knowledge', {
  state: () => ({
    items: [] as any[],
    searchResults: [] as any[],
    isLoading: false,
  }),

  actions: {
    async loadItems() {
      this.isLoading = true
      try {
        const res = await knowledgeApi.list()
        this.items = res.data.data || []
      } finally {
        this.isLoading = false
      }
    },
    async search(query) {
      const res = await knowledgeApi.search(query)
      this.searchResults = res.data.data || []
    },
    async add(data) {
      const res = await knowledgeApi.add(data)
      this.items.unshift(res.data.data)
      return res.data.data
    },
    async remove(id) {
      await knowledgeApi.delete(id)
      this.items = this.items.filter(i => i.id !== id)
    }
  }
})