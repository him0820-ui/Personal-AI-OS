<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { NForm, NFormItem, NInput, NButton, NCard, NSpace, useMessage } from 'naive-ui'

const router = useRouter()
const authStore = useAuthStore()
const message = useMessage()

const isLogin = ref(true)
const username = ref('')
const password = ref('')
const email = ref('')
const loading = ref(false)

const handleSubmit = async () => {
  loading.value = true
  try {
    if (isLogin.value) {
      await authStore.login(username.value, password.value)
      router.push('/main/chat')
    } else {
      await authStore.register(username.value, password.value, email.value || undefined)
      message.success('注册成功，请登录')
      isLogin.value = true
    }
  } catch (error: any) {
    message.error(error.response?.data?.error || '操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div style="min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; position: relative;">
    <!-- 背景装饰 -->
    <div style="position: absolute; top: 20%; left: 10%; width: 300px; height: 300px; background: radial-gradient(circle, rgba(255, 222, 0, 0.3) 0%, transparent 70%); border-radius: 50%;"></div>
    <div style="position: absolute; bottom: 20%; right: 10%; width: 400px; height: 400px; background: radial-gradient(circle, rgba(255, 0, 0, 0.3) 0%, transparent 70%); border-radius: 50%;"></div>
    <div style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); width: 500px; height: 500px; background: radial-gradient(circle, rgba(255, 222, 0, 0.15) 0%, transparent 70%); border-radius: 50%;"></div>
    
    <NCard style="width: 440px; background: rgba(255,255,255,0.6); backdrop-filter: blur(40px) saturate(180%); -webkit-backdrop-filter: blur(40px) saturate(180%); border-radius: 28px; box-shadow: 0 25px 80px rgba(139, 0, 0, 0.25); border: 1px solid rgba(255,255,255,0.6); position: relative; z-index: 1;">
      <div style="text-align: center; margin-bottom: 36px;">
        <div style="width: 88px; height: 88px; margin: 0 auto 18px; background: linear-gradient(135deg, #DE2910 0%, #8B0000 100%); border-radius: 24px; display: flex; align-items: center; justify-content: center; box-shadow: 0 10px 30px rgba(222, 41, 16, 0.35);">
          <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg" style="width: 44px; height: 44px;">
            <rect width="40" height="40" rx="12" fill="white" opacity="0.2"/>
            <circle cx="20" cy="14" r="6" fill="white"/>
            <path d="M14 24L20 32L26 24" stroke="white" stroke-width="3" fill="none" stroke-linecap="round"/>
          </svg>
        </div>
        <h1 style="font-size: 32px; color: #333; margin-bottom: 8px; font-weight: 700;">Personal AI OS</h1>
        <p style="color: #666; font-size: 14px;">你的个人AI操作系统</p>
      </div>
      
      <NForm :model="{ username, password, email }" @submit.prevent="handleSubmit">
        <NFormItem label="用户名" path="username">
          <NInput v-model:value="username" placeholder="请输入用户名" class="login-input" />
        </NFormItem>

        <NFormItem label="密码" path="password">
          <NInput type="password" v-model:value="password" placeholder="请输入密码" class="login-input" />
        </NFormItem>

        <NFormItem v-if="!isLogin" label="邮箱" path="email">
          <NInput v-model:value="email" placeholder="请输入邮箱（可选）" class="login-input" />
        </NFormItem>

        <NSpace vertical style="margin-top: 28px;">
          <NButton type="primary" block :loading="loading" @click="handleSubmit" class="submit-btn">
            {{ isLogin ? '登录' : '注册' }}
          </NButton>

          <NButton text block @click="isLogin = !isLogin" class="switch-btn">
            {{ isLogin ? '还没有账号？立即注册' : '已有账号？立即登录' }}
          </NButton>
        </NSpace>
      </NForm>
    </NCard>
  </div>
</template>

<style scoped>
/* 输入框毛玻璃 + 圆角风格 - 确保所有子层透明 */
.login-input :deep(.n-input__input-el) {
  font-size: 15px !important;
  border-radius: 12px !important;
  background: transparent !important;
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
  padding: 0 !important;
  margin: 0 !important;
  width: 100% !important;
  min-height: auto !important;
}

.login-input :deep(.n-input__input) {
  background: transparent !important;
  border: none !important;
  border-radius: 12px !important;
  padding: 0 !important;
  margin: 0 !important;
  min-height: auto !important;
}

/* 确保 n-input-wrapper 内所有元素都透明，只有 wrapper 本身有毛玻璃背景 */
.login-input :deep(.n-input-wrapper) > * {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

/* 覆盖 Naive UI 的默认颜色变量 */
.login-input {
  --n-color: transparent !important;
  --n-color-focus: transparent !important;
  --n-border: none !important;
  --n-border-focus: none !important;
  --n-border-hover: none !important;
}

/* 根元素设置圆角和溢出隐藏，确保子元素圆角被正确裁剪 */
.login-input {
  border-radius: 14px !important;
  overflow: hidden !important;
}

.login-input :deep(.n-input) {
  --n-border-radius: 14px !important;
  border-radius: 14px !important;
  overflow: hidden !important;
}

/* n-input__border 是底层边框层，需要透明背景 + 圆角 */
.login-input :deep(.n-input__border) {
  border-radius: 14px !important;
  border: none !important;
  background: transparent !important;
}

/* n-input__state-border 是上层边框层，设置圆角和边框 */
.login-input :deep(.n-input__state-border) {
  border-radius: 14px !important;
  border: 1px solid rgba(222, 41, 16, 0.2) !important;
  background: transparent !important;
  transition: all 0.3s ease;
}

/* wrapper 背景比父级卡片(0.6)稍亮，形成层次区分 */
.login-input :deep(.n-input-wrapper) {
  background: rgba(255, 255, 255, 0.75) !important;
  backdrop-filter: blur(16px) !important;
  -webkit-backdrop-filter: blur(16px) !important;
  border-radius: 14px !important;
  padding: 10px 16px !important;
  transition: all 0.3s ease;
}

/* 聚焦时仅轻微提亮 + 红色光晕，不改变圆角和边框宽度 */
.login-input.n-input--focus :deep(.n-input-wrapper) {
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 2px 12px rgba(222, 41, 16, 0.12);
}

.login-input.n-input--focus :deep(.n-input__state-border) {
  border: 1px solid #DE2910 !important;
  box-shadow: 0 0 0 3px rgba(222, 41, 16, 0.12);
}

.login-input:hover :deep(.n-input__state-border) {
  border: 1px solid rgba(222, 41, 16, 0.4) !important;
}

/* 表单标签美化 */
:deep(.n-form-item-label) {
  font-weight: 600 !important;
  color: #444 !important;
  font-size: 14px !important;
}

/* 提交按钮 - 红色渐变 + 圆角 + 阴影 */
.submit-btn {
  --n-border-radius: 14px !important;
  --n-color: transparent !important;
  --n-color-hover: transparent !important;
  --n-color-pressed: transparent !important;
  --n-color-focus: transparent !important;
  height: 48px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  background: linear-gradient(135deg, #DE2910 0%, #8B0000 100%) !important;
  border: none !important;
  border-radius: 14px !important;
  box-shadow: 0 8px 24px rgba(222, 41, 16, 0.35) !important;
  transition: all 0.3s ease !important;
}

.submit-btn:hover {
  transform: translateY(-2px) !important;
  box-shadow: 0 12px 32px rgba(222, 41, 16, 0.45) !important;
}

.submit-btn:active {
  transform: translateY(0) !important;
}

/* 切换按钮 */
.switch-btn {
  height: 40px !important;
  font-size: 14px !important;
  color: #666 !important;
  border-radius: 12px !important;
  transition: all 0.3s ease !important;
}

.switch-btn:hover {
  color: #DE2910 !important;
  background: rgba(222, 41, 16, 0.08) !important;
}
</style>

