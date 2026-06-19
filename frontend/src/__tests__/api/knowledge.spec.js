import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('axios', () => {
  const mockHandlers = { use: vi.fn() }
  const mockAxios = {
    create: vi.fn(() => mockAxios),
    interceptors: { request: mockHandlers, response: mockHandlers },
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
  return { default: mockAxios }
})

import { knowledgeApi } from '@/api/knowledge'
import axios from 'axios'

describe('knowledgeApi', () => {
  const api = axios.create()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('add calls POST /knowledge', () => {
    const data = { title: 'Test', content: 'Hello' }
    knowledgeApi.add(data)
    expect(api.post).toHaveBeenCalledWith('/knowledge', data)
  })

  it('list calls GET /knowledge with query params', () => {
    knowledgeApi.list(0, 20)
    expect(api.get).toHaveBeenCalledWith('/knowledge?page=0&size=20')
  })

  it('getById calls GET /knowledge/:id', () => {
    knowledgeApi.getById(5)
    expect(api.get).toHaveBeenCalledWith('/knowledge/5')
  })

  it('search calls GET with encoded query', () => {
    knowledgeApi.search('test query')
    expect(api.get).toHaveBeenCalledWith('/knowledge/search?q=test%20query')
  })

  it('update calls PUT /knowledge/:id', () => {
    const data = { title: 'Updated' }
    knowledgeApi.update(1, data)
    expect(api.put).toHaveBeenCalledWith('/knowledge/1', data)
  })

  it('delete calls DELETE /knowledge/:id', () => {
    knowledgeApi.delete(1)
    expect(api.delete).toHaveBeenCalledWith('/knowledge/1')
  })

  it('parseUrl calls POST /knowledge/parse-url', () => {
    knowledgeApi.parseUrl('http://example.com')
    expect(api.post).toHaveBeenCalledWith('/knowledge/parse-url', { url: 'http://example.com' })
  })

  it('parsePdf calls POST with FormData', () => {
    const file = new File(['content'], 'test.pdf', { type: 'application/pdf' })
    knowledgeApi.parsePdf(file)
    expect(api.post).toHaveBeenCalledWith(
      '/knowledge/parse-pdf',
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } }
    )
    const formData = api.post.mock.calls[0][1]
    expect(formData.get('file')).toBe(file)
  })

  it('exportJson calls GET with blob responseType', () => {
    knowledgeApi.exportJson()
    expect(api.get).toHaveBeenCalledWith('/knowledge/export/json', { responseType: 'blob' })
  })

  it('exportCsv calls GET with blob responseType', () => {
    knowledgeApi.exportCsv()
    expect(api.get).toHaveBeenCalledWith('/knowledge/export/csv', { responseType: 'blob' })
  })

  it('getTags calls GET /knowledge/tags', () => {
    knowledgeApi.getTags()
    expect(api.get).toHaveBeenCalledWith('/knowledge/tags')
  })

  it('batchDelete calls POST /knowledge/batch/delete', () => {
    knowledgeApi.batchDelete([1, 2, 3])
    expect(api.post).toHaveBeenCalledWith('/knowledge/batch/delete', [1, 2, 3])
  })

  it('batchTag calls POST /knowledge/batch/tag', () => {
    knowledgeApi.batchTag([1, 2], 'java')
    expect(api.post).toHaveBeenCalledWith('/knowledge/batch/tag', { ids: [1, 2], tag: 'java' })
  })

  it('batchExport calls POST with blob responseType', () => {
    knowledgeApi.batchExport([1, 2])
    expect(api.post).toHaveBeenCalledWith('/knowledge/batch/export', [1, 2], { responseType: 'blob' })
  })

  it('getRelated calls GET /knowledge/:id/related', () => {
    knowledgeApi.getRelated(1, 5)
    expect(api.get).toHaveBeenCalledWith('/knowledge/1/related?limit=5')
  })

  it('previewImport calls POST /knowledge/import/preview', () => {
    const json = '{"items":[]}'
    knowledgeApi.previewImport(json)
    expect(api.post).toHaveBeenCalledWith('/knowledge/import/preview', json, {
      headers: { 'Content-Type': 'application/json' }
    })
  })

  it('importJson calls POST /knowledge/import with conflict param', () => {
    const json = '{"items":[]}'
    knowledgeApi.importJson(json, 'overwrite')
    expect(api.post).toHaveBeenCalledWith('/knowledge/import?conflict=overwrite', json, {
      headers: { 'Content-Type': 'application/json' }
    })
  })

  it('importJson defaults conflict to skip', () => {
    knowledgeApi.importJson('{}')
    expect(api.post).toHaveBeenCalledWith('/knowledge/import?conflict=skip', '{}', expect.any(Object))
  })
})