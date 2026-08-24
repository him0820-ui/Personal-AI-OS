<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import * as Stomp from '@stomp/stompjs'

const authStore = useAuthStore()
const notifications = ref<Array<{ id: number; type: string; title: string; message: string; timestamp: string }>>([])
let stompClient: any = null
let reconnectTimeout: number | null = null
let notificationIdCounter = 0

// 从JWT token中解析用户ID
const getUserIdFromToken = (token: string): number | null => {
  try {
    const payload = token.split('.')[1]
    const decoded = atob(payload)
    const data = JSON.parse(decoded)
    return data.userId || data.sub || null
  } catch (e) {
    console.error('[NotificationListener] Failed to parse token:', e)
    return null
  }
}

const showNotification = (notification: { type: string; title: string; message: string; timestamp: string }) => {
  console.log('[NotificationListener] Displaying notification:', notification)
  
  const newNotification = {
    ...notification,
    id: ++notificationIdCounter
  }
  
  notifications.value.unshift(newNotification)
  if (notifications.value.length > 50) {
    notifications.value.pop()
  }
  
  setTimeout(() => {
    removeNotification(newNotification.id)
  }, 8000)
  
  if ('Notification' in window && Notification.permission === 'granted') {
    try {
      new Notification(notification.title, {
        body: notification.message,
        icon: '/favicon.ico'
      })
      console.log('[NotificationListener] Browser notification shown')
    } catch (browserError) {
      console.error('[NotificationListener] Failed to show browser notification:', browserError)
    }
  } else if ('Notification' in window && Notification.permission !== 'denied') {
    Notification.requestPermission().then(permission => {
      if (permission === 'granted') {
        new Notification(notification.title, {
          body: notification.message,
          icon: '/favicon.ico'
        })
      }
    })
  }
}

const removeNotification = (id: number) => {
  const index = notifications.value.findIndex(n => n.id === id)
  if (index > -1) {
    notifications.value.splice(index, 1)
  }
}

const connect = () => {
  const token = localStorage.getItem('token')
  console.log('[NotificationListener] Token from localStorage:', token ? '***' : 'null')
  
  if (!token) {
    console.log('[NotificationListener] No token, skipping WebSocket connection')
    return
  }

  console.log('[NotificationListener] Connecting to WebSocket...')
  
  const protocol =
      location.protocol === 'https:' ? 'wss:' : 'ws:'

  const wsUrl = `${protocol}//${window.location.host}/ws`
  console.log('[NotificationListener] WebSocket URL:', wsUrl)
  
  const wsFactory = () => {
    const socket = new WebSocket(wsUrl)
    socket.onopen = () => console.log('[NotificationListener] WebSocket opened')
    socket.onclose = (event) => console.log('[NotificationListener] WebSocket closed:', event.code, event.reason)
    socket.onerror = (e: any) => console.error('[NotificationListener] WebSocket error:', e)
    return socket
  }
  
  stompClient = new Stomp.Client({
    webSocketFactory: wsFactory,
    connectHeaders: { Authorization: `Bearer ${token}` },
    debug: (str: string) => console.debug('[Stomp]', str),
    reconnectDelay: 5000
  })

  stompClient.onConnect = (frame: any) => {
    console.log('[NotificationListener] WebSocket connected:', frame)
    
    // 优先使用authStore的userId，如果为空则从token解析
    let userId = authStore.userId
    if (!userId) {
      const token = localStorage.getItem('token')
      if (token) {
        userId = getUserIdFromToken(token)
      }
    }
    
    console.log('[NotificationListener] User ID for subscription:', userId)
    
    if (userId) {
      stompClient.subscribe(`/topic/notifications/${userId}`, (message: any) => {
        try {
          const parsed = JSON.parse(message.body)
          console.log('[NotificationListener] Received WebSocket notification:', parsed)
          
          const notification = {
            type: parsed.type || 'reminder',
            title: parsed.title,
            message: parsed.message,
            timestamp: parsed.timestamp
          }
          
          showNotification(notification)
        } catch (e) {
          console.error('[NotificationListener] Failed to parse notification:', e, 'raw data:', message.body)
        }
      })
      
      stompClient.publish({
        destination: '/app/subscribe-notifications',
        body: JSON.stringify({ userId })
      })
      
      console.log('[NotificationListener] Subscribed to topic /topic/notifications/' + userId)
    } else {
      console.warn('[NotificationListener] No userId found, cannot subscribe to notifications')
    }
  }

  stompClient.onWebSocketError = (error: any) => {
    console.error('[NotificationListener] WebSocket error:', error)
  }

  stompClient.onStompError = (frame: any) => {
    console.error('[NotificationListener] STOMP error:', frame)
  }

  stompClient.activate()

}

const disconnect = () => {
  if (stompClient) {
    try {
      stompClient.deactivate()
      console.log('[NotificationListener] WebSocket disconnected')
    } catch (e) {
      console.error('[NotificationListener] Error disconnecting:', e)
    }
    stompClient = null
  }
  if (reconnectTimeout) {
    clearTimeout(reconnectTimeout)
    reconnectTimeout = null
  }
}

const formatTime = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit',
    second: '2-digit'
  })
}

watch(() => authStore.isAuthenticated(), (isLoggedIn) => {
  console.log('[NotificationListener] Auth status changed:', isLoggedIn)
  if (isLoggedIn) {
    setTimeout(() => {
      connect()
    }, 1000)
  } else {
    disconnect()
  }
})

onMounted(() => {
  console.log('[NotificationListener] mounted')
  if (authStore.isAuthenticated()) {
    connect()
  }
})

onUnmounted(() => {
  disconnect()
})
</script>

<template>
  <div class="notification-container">
    <transition-group name="notification-fade">
      <div 
        v-for="notification in notifications.slice(0, 3)" 
        :key="notification.id"
        class="notification-item"
      >
        <button class="notification-close" @click="removeNotification(notification.id)">
          ×
        </button>
        <div class="notification-title">{{ notification.title }}</div>
        <div class="notification-message">{{ notification.message }}</div>
        <div class="notification-time">{{ formatTime(notification.timestamp) }}</div>
      </div>
    </transition-group>
  </div>
</template>

<style scoped>
.notification-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-item {
  background: linear-gradient(135deg, rgba(222, 41, 16, 0.85) 0%, rgba(139, 0, 0, 0.85) 100%);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  color: white;
  padding: 18px 22px;
  border-radius: 18px;
  box-shadow: 0 8px 32px rgba(222, 41, 16, 0.4);
  min-width: 300px;
  max-width: 420px;
  position: relative;
  border: 1px solid rgba(255, 222, 0, 0.3);
}

.notification-close {
  position: absolute;
  top: 10px;
  right: 14px;
  background: rgba(255, 255, 255, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  font-size: 20px;
  cursor: pointer;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: all 0.2s ease;
}

.notification-close:hover {
  background: rgba(255, 255, 255, 0.4);
  transform: rotate(90deg);
}
</style>