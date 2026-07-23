import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

api.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      window.location.href = '/'
    }
    return Promise.reject(error)
  }
)

// Planner API
const plannerApi = {
  getSummary: () => api.get('/planner/summary'),
  generateSummary: () => api.post('/planner/generate'),
  getTomorrowPlan: () => api.get('/planner/tomorrow-plan'),
  generateTomorrowPlan: () => api.post('/planner/tomorrow-plan/generate'),
  getRecommendations: () => api.get('/planner/recommendations'),
  generateRecommendations: () => api.post('/planner/recommendations/generate')
}

export { api, plannerApi }

// Types - matching backend response structures
export interface DailySummary {
  date: string
  summary: string
  completionRate: number
  suggestions: string
}

export interface TomorrowPlan {
  date: string
  overview: string
  focusAreas: string[]
  tasks: {
    title: string
    priority: string
    estimatedHours: number
    relatedGoal: string
    description: string
    timeSlot: string
  }[]
  suggestions: string
}

export interface TaskRecommendation {
  recommendations: {
    rank: number
    task: string
    reason: string
    priority: string
    estimatedHours: number
    relatedGoal: string
  }[]
  totalTasks: number
  message: string
}
