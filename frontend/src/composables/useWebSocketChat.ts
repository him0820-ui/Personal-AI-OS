import { ref, onMounted, onUnmounted } from 'vue'
import * as Stomp from '@stomp/stompjs'

export interface WebSocketMessage {
  type: string
  content: string
}

export function useWebSocketChat(userId: number, sessionId: number) {
  const stompClient = ref<any>(null)
  const isConnected = ref(false)
  const messages = ref<WebSocketMessage[]>([])

  const connect = () => {
    const protocol =
      location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/ws`
    const wsFactory = () => new WebSocket(wsUrl)
    
    stompClient.value = new Stomp.Client({
      webSocketFactory: wsFactory,
      debug: (str: string) => console.debug('[Stomp]', str),
      reconnectDelay: 5000
    })

    stompClient.value.onConnect = (frame: any) => {
      isConnected.value = true
      console.log('WebSocket connected:', frame)

      stompClient.value.subscribe(`/topic/chat/${sessionId}`, (message: any) => {
        const data = JSON.parse(message.body)
        messages.value.push(data)
        console.log('Received message:', data)
      })

      stompClient.value.publish({
        destination: '/app/subscribe-notifications',
        body: JSON.stringify({ userId })
      })
    }

    stompClient.value.onWebSocketError = (error: any) => {
      console.error('WebSocket connection error:', error)
      isConnected.value = false
    }

    stompClient.value.activate()
  }

  const disconnect = () => {
    if (stompClient.value) {
      stompClient.value.deactivate()
      isConnected.value = false
    }
  }

  const sendMessage = (message: string) => {
    if (!stompClient.value || !isConnected.value) {
      console.error('WebSocket not connected')
      return
    }

    stompClient.value.send('/app/chat', {}, JSON.stringify({
      userId,
      sessionId,
      message
    }))
  }

  onMounted(() => {
    connect()
  })

  onUnmounted(() => {
    disconnect()
  })

  return {
    isConnected,
    messages,
    sendMessage,
    disconnect,
    connect
  }
}
