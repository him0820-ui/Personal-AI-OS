import { onMounted, onUnmounted, ref } from 'vue'
import { notificationApi, type Notification } from '@/api'
import { useMessage } from 'naive-ui'

export function useNotification() {
  const notifications = ref<Notification[]>([])
  const message = useMessage()
  let reader: ReadableStreamDefaultReader<Uint8Array> | null = null
  let reconnectTimeout: number | null = null

  const startListening = async () => {
    try {
      if (reader) {
        await reader.cancel()
        reader = null
      }
      
      const token = localStorage.getItem('token')
      if (!token) {
        console.log('No token, skipping notification connection')
        return
      }
      
      const response = await fetch('/api/notifications/stream', {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Accept': 'text/event-stream'
        }
      })
      
      if (!response.ok) {
        console.error('Notification connection failed:', response.status)
        throw new Error('Connection failed')
      }
      
      const stream = response.body
      if (!stream) {
        throw new Error('No stream')
      }
      
      reader = stream.getReader()
      const decoder = new TextDecoder()
      
      let buffer = ''
      
      while (true) {
        const { done, value } = await reader.read()
        
        if (done) {
          break
        }
        
        buffer += decoder.decode(value)
        
        const messages = buffer.split('\n\n')
        buffer = messages.pop() || ''
        
        for (const msg of messages) {
          const lines = msg.split('\n')
          let eventName = ''
          let data = ''
          
          for (const line of lines) {
            if (line.startsWith('event:')) {
              eventName = line.slice(7).trim()
            } else if (line.startsWith('data:')) {
              data += line.slice(6)
            }
          }
          
          if (eventName === 'notification' && data) {
            try {
              const parsed = JSON.parse(data)
              const notification: Notification = {
                type: parsed.type || 'reminder',
                title: parsed.title,
                message: parsed.message,
                timestamp: parsed.timestamp
              }
              
              notifications.value.unshift(notification)
              if (notifications.value.length > 50) {
                notifications.value.pop()
              }
              
              message.info(notification.message, { duration: 8000 })
              
              console.log('Received notification:', notification)
            } catch (e) {
              console.error('Failed to parse notification:', e)
            }
          }
        }
      }
    } catch (e) {
      console.error('Notification connection error:', e)
      
      if (reconnectTimeout) {
        clearTimeout(reconnectTimeout)
      }
      
      reconnectTimeout = window.setTimeout(() => {
        startListening()
      }, 5000)
    }
  }

  const stopListening = async () => {
    if (reader) {
      await reader.cancel()
      reader = null
    }
    if (reconnectTimeout) {
      clearTimeout(reconnectTimeout)
      reconnectTimeout = null
    }
    console.log('Notification listener stopped')
  }

  const testNotification = async (title?: string, msg?: string) => {
    try {
      await notificationApi.test({ title, message: msg })
    } catch (e) {
      console.error('Failed to send test notification:', e)
    }
  }

  onMounted(() => {
    startListening()
  })

  onUnmounted(() => {
    stopListening()
  })

  return {
    notifications,
    startListening,
    stopListening,
    testNotification
  }
}
