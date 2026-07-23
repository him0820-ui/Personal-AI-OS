<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useSessionStore } from '@/stores/session'
import { api } from '@/api'
import { NLayoutFooter, NInput, NButton, NScrollbar, NMessageProvider, useMessage, NCard, NModal, NForm, NFormItem } from 'naive-ui'

interface Message {
  id: string
  content: string
  think: string
  sender: string
  timestamp: string
  showThink: boolean
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const sessionStore = useSessionStore()
const message = useMessage()

const messages = ref<Message[]>([])
const inputMessage = ref('')
const loadingCount = ref(0)
const messagesContainer = ref<InstanceType<typeof NScrollbar> | null>(null)
const abortController = ref<AbortController | null>(null)
const showEditTitleModal = ref(false)
const editingSession = ref<number | null>(null)
const editTitleValue = ref('')
const userScrolledUp = ref(false)
const isStreaming = ref(false)

const currentSession = computed(() => {
  return sessionStore.getCurrentSession()
})

const fetchSessions = async () => {
  try {
    await sessionStore.fetchSessions()
    
    const sessionId = route.params.sessionId as string
    if (sessionId) {
      const session = sessionStore.sessions.find(s => s.id === parseInt(sessionId))
      if (session) {
        await switchSession(session)
        return
      }
    }
    
    if (sessionStore.sessions.length === 0) {
      await createNewSession()
    } else if (!sessionStore.currentSessionId) {
      await switchSession(sessionStore.sessions[0])
    }
  } catch (error) {
    message.error('获取会话列表失败')
  }
}

const createNewSession = async () => {
  try {
    const session = await sessionStore.createNewSession()
    if (session) {
      await switchSession(session)
    }
  } catch (error) {
    message.error('创建会话失败')
  }
}

const switchSession = async (session: { id: number }) => {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
  sessionStore.switchSession(session.id)
  messages.value = []
  await fetchHistory(session.id)
}

const fetchHistory = async (sessionId: number) => {
  try {
    const response = await api.get(`/chat/history?sessionId=${sessionId}`)
    messages.value = response.data.map((msg: any) => ({
      id: msg.id ? msg.id.toString() : Math.random().toString(36).substr(2, 9),
      content: msg.content,
      think: msg.think || '',
      sender: msg.sender,
      timestamp: msg.timestamp,
      showThink: false
    }))
    scrollToBottom()
  } catch (error) {
    message.error('获取聊天记录失败')
  }
}

const updateSessionTitle = async () => {
  if (!editingSession.value || !editTitleValue.value.trim()) return
  
  try {
    await sessionStore.updateSessionTitle(editingSession.value, editTitleValue.value.trim())
    showEditTitleModal.value = false
    editTitleValue.value = ''
    message.success('标题更新成功')
  } catch (error) {
    message.error('更新标题失败')
  }
}

const openEditTitleModal = (sessionId: number) => {
  editingSession.value = sessionId
  const session = sessionStore.sessions.find(s => s.id === sessionId)
  editTitleValue.value = session?.title || ''
  showEditTitleModal.value = true
}

const generateId = () => {
  return Date.now().toString(36) + Math.random().toString(36).substr(2, 9)
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || !currentSession.value) return

  loadingCount.value++
  isStreaming.value = true
  userScrolledUp.value = false

  const messageId = generateId()

  const userMessage: Message = {
    id: generateId(),
    content: inputMessage.value,
    think: '',
    sender: 'user',
    timestamp: new Date().toISOString(),
    showThink: false
  }
  messages.value.push(userMessage)
  inputMessage.value = ''
  scrollToBottom(true)
  
  const aiMessage: Message = {
    id: messageId,
    content: '',
    think: '',
    sender: 'ai',
    timestamp: new Date().toISOString(),
    showThink: false
  }
  messages.value.push(aiMessage)
  
  try {
    const token = authStore.token
    abortController.value = new AbortController()
    const response = await fetch('/api/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      body: JSON.stringify({ 
        message: userMessage.content, 
        messageId: messageId,
        sessionId: currentSession.value.id
      }),
      signal: abortController.value.signal
    })
    
    if (!response.ok) {
      throw new Error('发送失败')
    }
    
    const reader = response.body?.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    
    if (reader) {
      let currentEventName = messageId
      let dataBuffer = ''
      
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        
        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEventName = line.substring(6).trim()
          } else if (line.startsWith('data:')) {
            const dataLine = line.substring(5).replace(/^ /, '')
            if (dataBuffer) {
              dataBuffer += '\n'
            }
            dataBuffer += dataLine
          } else if (line.trim() === '') {
            if (dataBuffer) {
              const aiMsg = messages.value.find(m => m.id === currentEventName && m.sender === 'ai')
              if (aiMsg) {
                if (dataBuffer.startsWith('think:')) {
                  const thinkContent = dataBuffer.substring(6)
                  aiMsg.think += thinkContent
                  aiMsg.showThink = true
                  scrollToBottom()
                } else if (dataBuffer.startsWith('content:')) {
                  const content = dataBuffer.substring(8)
                  aiMsg.content += content
                  scrollToBottom()
                } else if (dataBuffer.startsWith('error:')) {
                  const errorContent = dataBuffer.substring(6)
                  aiMsg.content = errorContent
                  scrollToBottom()
                }
              }
              dataBuffer = ''
            }
          }
        }
      }
      
      if (dataBuffer) {
        const aiMsg = messages.value.find(m => m.id === currentEventName && m.sender === 'ai')
        if (aiMsg) {
          if (dataBuffer.startsWith('content:')) {
            aiMsg.content += dataBuffer.substring(8)
            scrollToBottom()
          } else if (dataBuffer.startsWith('error:')) {
            aiMsg.content = dataBuffer.substring(6)
            scrollToBottom()
          }
        }
      }
    }
    
    const finalAiMsg = messages.value.find(m => m.id === messageId && m.sender === 'ai')
    if (finalAiMsg) {
      finalAiMsg.timestamp = new Date().toISOString()
      
      const session = sessionStore.sessions.find(s => s.id === currentSession.value?.id)
      if (session && session.title === '新会话') {
        const firstMessage = messages.value.find(m => m.sender === 'user')
        if (firstMessage) {
          const newTitle = firstMessage.content.substring(0, 20) + (firstMessage.content.length > 20 ? '...' : '')
          try {
            await sessionStore.updateSessionTitle(session.id, newTitle)
          } catch (e) {
            console.error('Update title error:', e)
          }
        }
      }
      
      console.log('AI response received:', finalAiMsg.content.length, 'chars')
    }
    
    if (currentSession.value) {
      const session = sessionStore.sessions.find(s => s.id === currentSession.value?.id)
      if (session) session.updatedAt = new Date().toISOString()
    }
  } catch (error: any) {
    if (error.name === 'AbortError') {
      const aiMsgIndex = messages.value.findIndex(m => m.id === messageId && m.sender === 'ai')
      if (aiMsgIndex !== -1) {
        messages.value.splice(aiMsgIndex, 1)
      }
      return
    }
    console.error('Streaming error:', error)
    message.error(error.message || '发送失败')
    const aiMsgIndex = messages.value.findIndex(m => m.id === messageId && m.sender === 'ai')
    if (aiMsgIndex !== -1) {
      messages.value.splice(aiMsgIndex, 1)
    }
  } finally {
    loadingCount.value--
    isStreaming.value = false
  }
}

const scrollToBottom = async (force: boolean = false) => {
  if (!force && userScrolledUp.value) {
    return
  }
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTo({ top: 999999, behavior: 'smooth' })
  }
}

const handleScroll = (e: Event) => {
  const target = e.target as HTMLElement
  const { scrollTop, scrollHeight, clientHeight } = target
  const distanceToBottom = scrollHeight - scrollTop - clientHeight
  userScrolledUp.value = distanceToBottom > 100
}

const scrollToBottomButton = () => {
  userScrolledUp.value = false
  scrollToBottom(true)
}

const toggleThink = (index: number) => {
  messages.value[index].showThink = !messages.value[index].showThink
}

const formatTime = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

watch(() => route.params.sessionId, async (newSessionId) => {
  if (newSessionId && sessionStore.sessions.length > 0) {
    const session = sessionStore.sessions.find(s => s.id === parseInt(newSessionId as string))
    if (session) {
      await switchSession(session)
    }
  }
})

onMounted(() => {
  fetchSessions()
})

onUnmounted(() => {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
})
</script>

<template>
  <NMessageProvider>
    <div style="display: flex; flex-direction: column; height: 100vh;">
      <div style="padding: 20px 24px; background: rgba(255, 255, 255, 0.6); border-bottom: 1px solid rgba(255, 255, 255, 0.5); backdrop-filter: blur(30px) saturate(180%); -webkit-backdrop-filter: blur(30px) saturate(180%);">
        <div style="display: flex; align-items: center; gap: 16px;">
          <NButton size="small" @click="router.back()" style="border-radius: 12px;">← 返回</NButton>
          <h2 style="font-size: 20px; color: #333; margin: 0; font-weight: 600;">{{ currentSession?.title || '聊天' }}</h2>
          <NButton size="small" @click="openEditTitleModal(currentSession!.id)" v-if="currentSession" style="margin-left: auto; border-radius: 12px;">✏️ 编辑标题</NButton>
        </div>
      </div>

      <NCard style="flex: 1; border-radius: 0; box-shadow: none; overflow: hidden; position: relative; background: transparent;">
        <NScrollbar ref="messagesContainer" style="height: calc(100vh - 180px);" @scroll="handleScroll">
          <div style="padding: 28px 24px;">
            <div v-for="(msg, index) in messages" :key="msg.id"
                 :style="{ display: 'flex', marginBottom: '24px', justifyContent: msg.sender === 'user' ? 'flex-end' : 'flex-start', gap: '12px' }">
              <div v-if="msg.sender === 'ai'" style="width: 40px; height: 40px; flex-shrink: 0; background: linear-gradient(135deg, #DE2910 0%, #8B0000 100%); border-radius: 14px; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 12px rgba(222, 41, 16, 0.3);">
                <svg viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg" style="width: 18px; height: 18px;">
                  <circle cx="10" cy="7" r="3" fill="white"/>
                  <path d="M6 14L10 18L14 14" stroke="white" stroke-width="2" fill="none" stroke-linecap="round"/>
                </svg>
              </div>
              <div :style="{ maxWidth: '75%', padding: '16px 22px', borderRadius: msg.sender === 'user' ? '22px 22px 6px 22px' : '22px 22px 22px 6px', background: msg.sender === 'user' ? 'linear-gradient(135deg, #DE2910 0%, #8B0000 100%)' : 'rgba(255, 255, 255, 0.75)', backdropFilter: msg.sender === 'ai' ? 'blur(20px)' : 'none', WebkitBackdropFilter: msg.sender === 'ai' ? 'blur(20px)' : 'none', border: msg.sender === 'ai' ? '1px solid rgba(255, 255, 255, 0.5)' : 'none', color: msg.sender === 'user' ? 'white' : '#333', boxShadow: msg.sender === 'user' ? '0 6px 20px rgba(222, 41, 16, 0.3)' : '0 4px 18px rgba(0,0,0,0.06)', lineHeight: '1.65', position: 'relative' }">
                <div v-if="msg.sender === 'ai' && msg.think" style="margin-bottom: 10px;">
                  <div style="cursor: pointer; display: flex; align-items: center; gap: 6px; font-size: 12px; opacity: 0.6; transition: opacity 0.2s;">
                    <span @click="toggleThink(index)" :style="{ fontWeight: 'bold', fontSize: '14px' }">
                      {{ msg.showThink ? '▼' : '▲' }}
                    </span>
                    <span>思考过程</span>
                  </div>
                  <div v-if="msg.showThink" style="margin-top: 10px; padding: 14px; background: rgba(0,0,0,0.04); border-radius: 12px; font-size: 13px; color: #888; line-height: 1.7; border: 1px solid rgba(0,0,0,0.03);">
                    {{ msg.think }}
                  </div>
                </div>
                <p style="margin-bottom: 6px; font-size: 15px; word-break: break-word;">{{ msg.content }}</p>
                <p style="font-size: 12px; opacity: 0.6; margin: 0;">
                  {{ formatTime(msg.timestamp) }}
                </p>
              </div>
            </div>
          </div>
        </NScrollbar>
        <transition name="fade">
          <NButton
            v-if="userScrolledUp && isStreaming"
            @click="scrollToBottomButton"
            circle
            type="primary"
            style="position: absolute; bottom: 20px; left: 50%; transform: translateX(-50%); z-index: 10; box-shadow: 0 6px 18px rgba(222, 41, 16, 0.4); border-radius: 14px;"
          >
            ↓
          </NButton>
        </transition>
      </NCard>

      <NLayoutFooter style="background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(30px) saturate(180%); -webkit-backdrop-filter: blur(30px) saturate(180%); padding: 20px 24px; border-top: 1px solid rgba(255, 255, 255, 0.5);">
        <div style="display: flex; gap: 16px; align-items: center;">
          <NInput v-model:value="inputMessage" placeholder="输入消息..." style="flex: 1; border-radius: 14px;" @keyup.enter="sendMessage" />
          <NButton type="primary" :loading="loadingCount > 0" @click="sendMessage" style="border-radius: 14px; padding: 0 32px;">发送</NButton>
        </div>
      </NLayoutFooter>
    </div>
    
    <NModal v-model:show="showEditTitleModal" :title="'编辑标题'" preset="card" style="border-radius: 16px;">
      <NForm :model="{ title: editTitleValue }">
        <NFormItem label="标题" style="margin-bottom: 20px;">
          <NInput v-model:value="editTitleValue" placeholder="输入新标题" style="border-radius: 10px;" />
        </NFormItem>
      </NForm>
      
      <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 20px;">
        <NButton @click="showEditTitleModal = false" style="border-radius: 8px;">取消</NButton>
        <NButton type="primary" @click="updateSessionTitle" style="border-radius: 8px;">保存</NButton>
      </div>
    </NModal>
  </NMessageProvider>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(10px);
}
</style>
