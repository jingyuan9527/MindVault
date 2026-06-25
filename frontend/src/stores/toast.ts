import { defineStore } from 'pinia'
import { ref } from 'vue'

interface ToastMessage {
  id: number
  text: string
  type: string
}

export const useToastStore = defineStore('toast', () => {
  const messages = ref<ToastMessage[]>([])
  let id = 0

  function add(text: string, type = 'info', duration = 4000) {
    const toast: ToastMessage = { id: ++id, text, type }
    messages.value.push(toast)
    setTimeout(() => remove(toast.id), duration)
  }

  function remove(toastId: number) {
    messages.value = messages.value.filter(m => m.id !== toastId)
  }

  function success(text: string) { add(text, 'success') }
  function error(text: string) { add(text, 'error', 6000) }
  function info(text: string) { add(text, 'info') }

  return { messages, add, remove, success, error, info }
})