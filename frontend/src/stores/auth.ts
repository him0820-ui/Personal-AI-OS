import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref('')
  const userId = ref<number | null>(null)

  const isAuthenticated = () => !!token.value

  const login = async (user: string, password: string) => {
    const response = await api.post('/auth/login', { username: user, password })
    token.value = response.data.token
    username.value = response.data.username
    userId.value = response.data.userId
    localStorage.setItem('token', token.value)
  }

  const register = async (user: string, password: string, email?: string) => {
    await api.post('/auth/register', { username: user, password, email })
  }

  const logout = () => {
    token.value = ''
    username.value = ''
    userId.value = null
    localStorage.removeItem('token')
  }

  return {
    token,
    username,
    userId,
    isAuthenticated,
    login,
    register,
    logout
  }
})
