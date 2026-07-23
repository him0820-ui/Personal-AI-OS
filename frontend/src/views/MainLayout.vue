<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useSessionStore } from '@/stores/session'
import { NMenu, NButton, NPopconfirm } from 'naive-ui'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const sessionStore = useSessionStore()

const menuOptions = [
  { label: '聊天', key: '/main/chat' },
  { label: '记忆管理', key: '/main/memory' },
  { label: '记忆审核', key: '/main/review' },
  { label: '每日总结', key: '/main/planner' }
]

const handleMenuClick = (key: string) => {
  if (key === '/logout') {
    authStore.logout()
    router.push('/')
  } else {
    router.push(key)
  }
}

const switchSession = (sessionId: number) => {
  sessionStore.switchSession(sessionId)
  router.push(`/main/chat/${sessionId}`)
}

const createNewSession = async () => {
  const session = await sessionStore.createNewSession()
  if (session) {
    router.push(`/main/chat/${session.id}`)
  }
}

const handleDeleteSession = async (sessionId: number) => {
  const success = await sessionStore.deleteSession(sessionId)
  if (success) {
    if (!sessionStore.currentSessionId && sessionStore.sessions.length > 0) {
      router.push(`/main/chat/${sessionStore.sessions[0].id}`)
    } else if (sessionStore.sessions.length === 0) {
      await createNewSession()
    }
  }
}

onMounted(() => {
  sessionStore.fetchSessions()
  
  const sessionId = route.params.sessionId as string
  if (sessionId) {
    sessionStore.switchSession(parseInt(sessionId))
  }
})

watch(() => route.params.sessionId, (newSessionId) => {
  if (newSessionId) {
    sessionStore.switchSession(parseInt(newSessionId as string))
  }
})
</script>

<template>
  <div class="layout-container">
    <div class="sidebar">
      <div class="sidebar-header">
        <div class="logo">
          <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="32" height="32" rx="10" fill="url(#sidebarLogoGradient)"/>
            <circle cx="16" cy="11" r="5" fill="white"/>
            <path d="M11 18L16 26L21 18" stroke="white" stroke-width="3" fill="none" stroke-linecap="round"/>
            <defs>
              <linearGradient id="sidebarLogoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#D3373A"/>
                <stop offset="100%" stop-color="#FF6B6B"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <span class="app-name">AI OS</span>
      </div>
      
      <div class="menu-section">
        <NMenu 
          :options="menuOptions" 
          @update:value="handleMenuClick"
          :value="route.path"
          mode="vertical"
        />
      </div>
      
      <div class="user-card">
        <div class="user-avatar">
          <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="20" cy="20" r="18" fill="url(#avatarGradient)"/>
            <circle cx="20" cy="16" r="6" fill="white"/>
            <path d="M12 30 Q20 35 28 30" stroke="white" stroke-width="2" fill="none" stroke-linecap="round"/>
            <defs>
              <linearGradient id="avatarGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#D3373A"/>
                <stop offset="100%" stop-color="#FF6B6B"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div class="user-info">
          <p class="user-name">{{ authStore.username }}</p>
          <p class="user-status">在线</p>
        </div>
      </div>
      
      <div class="session-section">
        <div class="section-header">
          <span class="section-title">最近会话</span>
          <NButton size="tiny" type="primary" @click="createNewSession" class="new-session-btn">
            +
          </NButton>
        </div>
        
        <div class="session-list">
          <div 
            v-for="session in sessionStore.sessions" 
            :key="session.id"
            class="session-item"
            :class="{ active: sessionStore.currentSessionId === session.id && route.path.includes('/chat') }"
            @click="switchSession(session.id)"
          >
            <div class="session-content">
              <svg viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg" class="session-icon">
                <rect x="2" y="4" width="16" height="12" rx="2" :fill="sessionStore.currentSessionId === session.id && route.path.includes('/chat') ? '#D3373A' : '#e8e8e8'"/>
                <rect x="6" y="2" width="8" height="4" rx="1" :fill="sessionStore.currentSessionId === session.id && route.path.includes('/chat') ? '#FF6B6B' : '#d0d0d0'"/>
                <circle :cx="sessionStore.currentSessionId === session.id && route.path.includes('/chat') ? '9' : '7'" cy="10" r="1.5" :fill="sessionStore.currentSessionId === session.id && route.path.includes('/chat') ? 'white' : '#999'"/>
                <circle :cx="sessionStore.currentSessionId === session.id && route.path.includes('/chat') ? '13' : '11'" cy="10" r="1.5" :fill="sessionStore.currentSessionId === session.id && route.path.includes('/chat') ? 'white' : '#999'"/>
                <path :d="sessionStore.currentSessionId === session.id && route.path.includes('/chat') ? 'M7 14 L10 17 L15 12' : 'M5 13 L9 16 L15 11'" stroke-width="1.5" :stroke="sessionStore.currentSessionId === session.id && route.path.includes('/chat') ? 'white' : '#999'" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span class="session-title">{{ session.title }}</span>
            </div>
            <NPopconfirm 
              title="确认删除?" 
              description="删除会话将同时删除该会话的所有消息和记忆提取记录，此操作不可恢复！"
              negative-text="取消"
              positive-text="确认删除"
              @positive-click="(e) => { e.stopPropagation(); handleDeleteSession(session.id); }"
            >
              <template #trigger>
                <NButton 
                  size="tiny" 
                  type="default" 
                  text
                  @click.stop
                  class="delete-btn"
                >
                  ✕
                </NButton>
              </template>
            </NPopconfirm>
          </div>
          
          <div v-if="sessionStore.sessions.length === 0" class="empty-session">
            <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg" class="empty-icon">
              <circle cx="24" cy="24" r="20" stroke="#e8e8e8" stroke-width="2"/>
              <path d="M20 30 L24 26 L30 20" stroke="#e8e8e8" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="24" cy="24" r="8" stroke="#e8e8e8" stroke-width="2"/>
            </svg>
            <p>暂无会话</p>
            <NButton size="small" type="primary" @click="createNewSession">
              创建新会话
            </NButton>
          </div>
        </div>
      </div>
      
      <div class="sidebar-footer">
        <NButton size="small" block @click="authStore.logout(); router.push('/')" class="logout-btn">
          退出登录
        </NButton>
      </div>
    </div>
    
    <div class="main-content">
      <router-view />
    </div>
  </div>
</template>

<style scoped>
.layout-container {
  display: flex;
  min-height: 100vh;
  position: relative;
}

.sidebar {
  width: 260px;
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(40px) saturate(180%);
  -webkit-backdrop-filter: blur(40px) saturate(180%);
  border-right: 1px solid rgba(255, 255, 255, 0.5);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 24px 20px;
  box-shadow: 8px 0 40px rgba(139, 0, 0, 0.1);
  position: relative;
  overflow: hidden;
}

.sidebar::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 80% 20%, rgba(222, 41, 16, 0.1) 0%, transparent 50%);
  pointer-events: none;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
  padding: 10px 4px;
  position: relative;
  z-index: 1;
}

.logo {
  width: 40px;
  height: 40px;
  border-radius: 14px;
}

.app-name {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #D3373A 0%, #FF6B6B 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.menu-section {
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  background: linear-gradient(135deg, rgba(222, 41, 16, 0.12) 0%, rgba(255, 222, 0, 0.12) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 20px;
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
  box-shadow: 0 4px 20px rgba(222, 41, 16, 0.1);
  transition: all 0.3s ease;
}

.user-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 30px rgba(222, 41, 16, 0.18);
}

.user-avatar {
  width: 44px;
  height: 44px;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.user-status {
  font-size: 12px;
  color: #666;
  margin: 2px 0 0;
}

.session-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.new-session-btn {
  background: linear-gradient(135deg, #DE2910 0%, #8B0000 100%) !important;
  border: none !important;
  width: 30px;
  height: 30px;
  padding: 0 !important;
  border-radius: 10px !important;
  box-shadow: 0 2px 8px rgba(222, 41, 16, 0.3) !important;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 6px;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  margin-bottom: 10px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  position: relative;
  z-index: 1;
  border: 1px solid rgba(255, 255, 255, 0.4);
}

.session-item:hover {
  background: rgba(255, 255, 255, 0.85);
  transform: translateX(6px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
}

.session-item.active {
  background: linear-gradient(135deg, #DE2910 0%, #8B0000 100%);
  color: white;
  box-shadow: 0 8px 24px rgba(222, 41, 16, 0.35);
  transform: translateX(4px);
  border: 1px solid rgba(255, 222, 0, 0.3);
}

.session-content {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  overflow: hidden;
}

.session-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.session-title {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delete-btn {
  opacity: 0.6;
  transition: opacity 0.2s;
}

.delete-btn:hover {
  opacity: 1;
}

.empty-session {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #999;
}

.empty-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 12px;
}

.empty-session p {
  margin-bottom: 16px;
  font-size: 14px;
}

.sidebar-footer {
  margin-top: 16px;
}

.logout-btn {
  background: rgba(255, 255, 255, 0.6) !important;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  color: #666 !important;
  border-radius: 12px !important;
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.9) !important;
  color: #DE2910 !important;
  box-shadow: 0 4px 12px rgba(222, 41, 16, 0.15) !important;
}

.main-content {
  flex: 1;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(30px) saturate(180%);
  -webkit-backdrop-filter: blur(30px) saturate(180%);
  position: relative;
  overflow: hidden;
}

.main-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  background:
    radial-gradient(circle at 10% 10%, rgba(255, 222, 0, 0.15) 0%, transparent 40%),
    radial-gradient(circle at 90% 90%, rgba(222, 41, 16, 0.15) 0%, transparent 40%);
  z-index: 0;
}

.main-content > * {
  position: relative;
  z-index: 1;
}
</style>