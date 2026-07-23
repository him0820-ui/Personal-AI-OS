<script setup lang="ts">
import { RouterView } from 'vue-router'
import { NMessageProvider, NConfigProvider } from 'naive-ui'
import NotificationListener from '@/components/NotificationListener.vue'
</script>

<template>
  <NConfigProvider>
    <NMessageProvider>
      <RouterView />
      <NotificationListener />
    </NMessageProvider>
  </NConfigProvider>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: linear-gradient(135deg, #DE2910 0%, #8B0000 100%);
  min-height: 100vh;
  overflow-x: hidden;
}

#app {
  min-height: 100vh;
  position: relative;
}

/* 背景装饰元素 */
#app::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  background:
    radial-gradient(circle at 20% 80%, rgba(255, 222, 0, 0.25) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255, 0, 0, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 40% 40%, rgba(255, 222, 0, 0.15) 0%, transparent 40%);
  z-index: -1;
}

/* 浮动背景圆圈动画 */
@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(5deg);
  }
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

/* 全局按钮样式 */
button {
  transition: all 0.25s ease;
}

button:hover {
  transform: translateY(-1px);
}

button:active {
  transform: translateY(0);
}
</style>
