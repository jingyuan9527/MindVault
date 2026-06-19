import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useToastStore = defineStore('toast', () => {
  const messages = ref([])
  let id = 0

  function add(text, type = 'info', duration = 4000) {
    const toast = { id: ++id, text, type }
    messages.value.push(toast)
    setTimeout(() => remove(toast.id), duration)
  }

  function remove(toastId) {
    messages.value = messages.value.filter(m => m.id !== toastId)
  }

  function success(text) { add(text, 'success') }
  function error(text) { add(text, 'error', 6000) }
  function info(text) { add(text, 'info') }

  return { messages, add, remove, success, error, info }
})
