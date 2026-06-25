import { defineStore } from 'pinia'
import { knowledgeApi } from '@/api/knowledge'

export const useKnowledgeStore = defineStore('knowledge', {
  state: () => ({
    items: [] as any[],
    total: 0,
    isLoading: false,
  }),

  actions: {
    async fetchItems(params: {
      page?: number
      size?: number
      keyword?: string
      tags?: string[]
      sortBy?: string
      sortOrder?: string
    } = {}) {
      this.isLoading = true
      try {
        const res = await knowledgeApi.list({
          page: params.page ?? 0,
          size: params.size ?? 20,
          keyword: params.keyword || undefined,
          tags: params.tags?.length ? params.tags : undefined,
          sortBy: params.sortBy || 'createdAt',
          sortOrder: params.sortOrder || 'desc',
        })
        const data = res.data.data || { records: [], total: 0 }
        this.items = data.records || []
        this.total = data.total || 0
      } finally {
        this.isLoading = false
      }
    },
    async add(data) {
      const res = await knowledgeApi.add(data)
      return res.data.data
    },
    async remove(id) {
      await knowledgeApi.delete(id)
    }
  }
})