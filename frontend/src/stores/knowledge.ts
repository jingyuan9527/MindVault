import { defineStore } from 'pinia'
import { knowledgeApi } from '@/api/knowledge'

export const useKnowledgeStore = defineStore('knowledge', {
  state: () => ({
    items: [] as any[],
    total: 0,
    isLoading: false,
    searchResults: [] as any[],
    searchTotal: 0,
    isSearching: false,
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
    async search(params: { keyword: string; topN?: number; offset?: number; deep?: boolean; tag?: string }) {
      this.isSearching = true
      try {
        const api = params.deep ? knowledgeApi.searchRewrite : knowledgeApi.search
        const res = await api(params.keyword, {
          topN: params.topN ?? 20,
          offset: params.offset ?? 0,
          ...(params.tag ? { tag: params.tag } : {}),
        })
        const data = res.data.data || []
        this.searchResults = data
        this.searchTotal = data.length
      } finally {
        this.isSearching = false
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