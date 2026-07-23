import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/api'

export interface ChatSession {
  id: number
  userId: number
  title: string
  createdAt: string
  updatedAt: string
}

export const useSessionStore = defineStore('session', () => {
  const sessions = ref<ChatSession[]>([])
  const currentSessionId = ref<number | null>(null)

  const fetchSessions = async () => {
    try {
      const response = await api.get('/chat/sessions')
      sessions.value = response.data
      return sessions.value
    } catch (error) {
      console.error('获取会话列表失败')
      return []
    }
  }

  const createNewSession = async () => {
    try {
      const response = await api.post('/chat/session')
      const session = response.data as ChatSession
      sessions.value.unshift(session)
      currentSessionId.value = session.id
      return session
    } catch (error) {
      console.error('创建会话失败')
      return null
    }
  }

  const switchSession = (sessionId: number) => {
    currentSessionId.value = sessionId
  }

  const updateSessionTitle = async (sessionId: number, title: string) => {
    try {
      await api.put(`/chat/session/${sessionId}/title`, { title })
      const session = sessions.value.find(s => s.id === sessionId)
      if (session) {
        session.title = title
      }
      return true
    } catch (error) {
      console.error('更新会话标题失败')
      return false
    }
  }

  const deleteSession = async (sessionId: number) => {
    try {
      await api.delete(`/chat/session/${sessionId}`)
      const index = sessions.value.findIndex(s => s.id === sessionId)
      if (index !== -1) {
        sessions.value.splice(index, 1)
      }
      if (currentSessionId.value === sessionId) {
        currentSessionId.value = sessions.value.length > 0 ? sessions.value[0].id : null
      }
      return true
    } catch (error) {
      console.error('删除会话失败')
      return false
    }
  }

  const getCurrentSession = () => {
    return sessions.value.find(s => s.id === currentSessionId.value) || null
  }

  return {
    sessions,
    currentSessionId,
    fetchSessions,
    createNewSession,
    switchSession,
    updateSessionTitle,
    deleteSession,
    getCurrentSession
  }
})
