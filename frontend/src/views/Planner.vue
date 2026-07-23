<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { plannerApi, type DailySummary, type TomorrowPlan, type TaskRecommendation } from '@/api'
import { 
  NCard, NButton, NProgress, useMessage, NMessageProvider, 
  NTabs, NTabPane, NBadge, NSpace, NText
} from 'naive-ui'

const router = useRouter()
const message = useMessage()

const summary = ref<DailySummary | null>(null)
const tomorrowPlan = ref<TomorrowPlan | null>(null)
const recommendations = ref<TaskRecommendation | null>(null)

const summaryLoading = ref(false)
const planLoading = ref(false)
const recLoading = ref(false)

const fetchSummary = async () => {
  summaryLoading.value = true
  try {
    const response = await plannerApi.getSummary()
    summary.value = response.data
  } catch (error: any) {
    message.error(error.response?.data?.error || '获取总结失败')
  } finally {
    summaryLoading.value = false
  }
}

const fetchTomorrowPlan = async () => {
  planLoading.value = true
  try {
    const response = await plannerApi.getTomorrowPlan()
    tomorrowPlan.value = response.data
  } catch (error: any) {
    message.error(error.response?.data?.error || '获取明日计划失败')
  } finally {
    planLoading.value = false
  }
}

const fetchRecommendations = async () => {
  recLoading.value = true
  try {
    const response = await plannerApi.getRecommendations()
    recommendations.value = response.data
  } catch (error: any) {
    message.error(error.response?.data?.error || '获取任务推荐失败')
  } finally {
    recLoading.value = false
  }
}

const handleGenerateSummary = async () => {
  summaryLoading.value = true
  try {
    const response = await plannerApi.generateSummary()
    summary.value = response.data
    message.success('总结生成成功')
  } catch (error: any) {
    message.error(error.response?.data?.error || '生成失败')
  } finally {
    summaryLoading.value = false
  }
}

const handleGeneratePlan = async () => {
  planLoading.value = true
  try {
    const response = await plannerApi.generateTomorrowPlan()
    tomorrowPlan.value = response.data
    message.success('明日计划生成成功')
  } catch (error: any) {
    message.error(error.response?.data?.error || '生成失败')
  } finally {
    planLoading.value = false
  }
}

const handleGenerateRecommendations = async () => {
  recLoading.value = true
  try {
    const response = await plannerApi.generateRecommendations()
    recommendations.value = response.data
    message.success('任务推荐生成成功')
  } catch (error: any) {
    message.error(error.response?.data?.error || '生成失败')
  } finally {
    recLoading.value = false
  }
}

const getPriorityColor = (priority: string): 'default' | 'error' | 'warning' | 'success' | 'info' | undefined => {
  const colors: Record<string, 'default' | 'error' | 'warning' | 'success' | 'info'> = {
    '高': 'error',
    '中': 'warning',
    '低': 'success'
  }
  return colors[priority] || 'default'
}

const getPriorityLabel = (priority: string) => {
  const labels: Record<string, string> = {
    'high': '高',
    'medium': '中',
    'low': '低',
    '高': '高',
    '中': '中',
    '低': '低'
  }
  return labels[priority] || priority
}

onMounted(() => {
  fetchSummary()
  fetchTomorrowPlan()
  fetchRecommendations()
})
</script>

<template>
  <NMessageProvider>
    <div class="planner-container">
      <div class="page-header">
        <NButton size="small" @click="router.back()" class="back-btn">
          <svg viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M15 10L5 10M5 10L11 4M5 10L11 16" stroke="#D3373A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          返回
        </NButton>
        <h2>智能规划</h2>
      </div>

      <NTabs type="card" size="medium" class="planner-tabs">
        <NTabPane name="summary" tab="每日总结">
          <NCard class="action-card">
            <div class="action-bar">
              <span class="action-title">今日总结</span>
              <NButton type="primary" :loading="summaryLoading" @click="handleGenerateSummary" class="generate-btn">
                <svg viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M10 3L10 7M10 13L10 17M17 10L13 10M7 10L3 10" stroke="white" stroke-width="2" stroke-linecap="round"/>
                </svg>
                重新生成
              </NButton>
            </div>
          </NCard>

          <div v-if="summary" class="summary-grid">
            <NCard class="summary-card" title="今日总结">
              <p class="summary-text">{{ summary.summary }}</p>
            </NCard>

            <NCard class="summary-card" title="明日建议">
              <p class="summary-text">{{ summary.suggestions }}</p>
            </NCard>

            <NCard class="summary-card progress-card" title="待办完成率">
              <div class="progress-wrapper">
                <NProgress :percentage="summary.completionRate" showIndicator size="large" class="main-progress" />
              </div>
              <p class="progress-label">今日待办完成率：{{ summary.completionRate.toFixed(1) }}%</p>
            </NCard>

            <NCard class="summary-card date-card" title="日期">
              <div class="date-display">
                <p class="date-full">{{ new Date(summary.date).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }) }}</p>
              </div>
            </NCard>
          </div>

          <div v-else-if="!summaryLoading" class="empty-state">
            <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="32" cy="32" r="28" stroke="#e8e8e8" stroke-width="2"/>
              <path d="M22 32H42" stroke="#e8e8e8" stroke-width="2" stroke-linecap="round"/>
              <path d="M32 22V42" stroke="#e8e8e8" stroke-width="2" stroke-linecap="round"/>
              <circle cx="32" cy="32" r="6" stroke="#e8e8e8" stroke-width="2"/>
            </svg>
            <p>暂无总结数据</p>
            <p class="empty-hint">点击上方按钮生成今日总结</p>
          </div>
        </NTabPane>

        <NTabPane name="tomorrow-plan" tab="明日计划">
          <NCard class="action-card">
            <div class="action-bar">
              <span class="action-title">明日计划</span>
              <NButton type="primary" :loading="planLoading" @click="handleGeneratePlan" class="generate-btn">
                <svg viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M10 3L10 7M10 13L10 17M17 10L13 10M7 10L3 10" stroke="white" stroke-width="2" stroke-linecap="round"/>
                </svg>
                重新生成
              </NButton>
            </div>
          </NCard>

          <div v-if="tomorrowPlan">
            <NCard class="plan-card overview-card" title="计划概览">
              <p class="overview-text">{{ tomorrowPlan.overview }}</p>
              
              <div v-if="tomorrowPlan.focusAreas && tomorrowPlan.focusAreas.length > 0" class="focus-areas">
                <NText strong class="areas-label">重点领域：</NText>
                <NSpace class="areas-list">
                  <NBadge v-for="(area, index) in tomorrowPlan.focusAreas" :key="index" type="info" :value="area" class="area-badge" />
                </NSpace>
              </div>
            </NCard>

            <NCard class="plan-card tasks-card" title="计划任务">
              <div v-if="tomorrowPlan.tasks && tomorrowPlan.tasks.length > 0" class="task-list">
                <div v-for="(task, index) in tomorrowPlan.tasks" :key="index" class="task-item">
                  <div class="task-index">{{ index + 1 }}</div>
                  <div class="task-content">
                    <div class="task-header">
                      <div class="task-title-row">
                        <NText class="task-title">{{ task.title }}</NText>
                        <NBadge :type="getPriorityColor(getPriorityLabel(task.priority))" :value="getPriorityLabel(task.priority)" class="priority-badge" />
                      </div>
                      <div class="task-meta-row">
                        <span class="task-time">{{ task.timeSlot }}</span>
                        <span class="task-hours">{{ task.estimatedHours }}小时</span>
                      </div>
                    </div>
                    
                    <div v-if="task.description" class="task-description">
                      <NText>{{ task.description }}</NText>
                    </div>
                    
                    <div v-if="task.relatedGoal" class="task-meta">
                      <NText class="meta-label">关联目标：</NText>
                      <NText class="meta-value">{{ task.relatedGoal }}</NText>
                    </div>
                  </div>
                </div>
              </div>
              
              <div v-else class="no-content">
                暂无计划任务
              </div>
            </NCard>

            <NCard class="plan-card suggestions-card" title="额外建议">
              <p class="suggestions-text">{{ tomorrowPlan.suggestions }}</p>
            </NCard>
          </div>

          <div v-else-if="!planLoading" class="empty-state">
            <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="32" cy="32" r="28" stroke="#e8e8e8" stroke-width="2"/>
              <path d="M22 28L30 36L42 24" stroke="#e8e8e8" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <p>暂无明日计划</p>
            <p class="empty-hint">点击上方按钮生成明日计划</p>
          </div>
        </NTabPane>

        <NTabPane name="recommendations" tab="任务推荐">
          <NCard class="action-card">
            <div class="action-bar">
              <span class="action-title">智能任务推荐</span>
              <NButton type="primary" :loading="recLoading" @click="handleGenerateRecommendations" class="generate-btn">
                <svg viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M10 3L10 7M10 13L10 17M17 10L13 10M7 10L3 10" stroke="white" stroke-width="2" stroke-linecap="round"/>
                </svg>
                重新推荐
              </NButton>
            </div>
          </NCard>

          <div v-if="recommendations">
            <NCard class="rec-card" title="推荐任务">
              <div v-if="recommendations.recommendations && recommendations.recommendations.length > 0" class="recommendation-list">
                <div v-for="(rec, index) in recommendations.recommendations" :key="index" class="recommendation-item">
                  <div class="rec-rank" :class="{ top: rec.rank <= 3 }">{{ rec.rank }}</div>
                  <div class="rec-content">
                    <div class="rec-header">
                      <NText class="rec-task">{{ rec.task }}</NText>
                      <NBadge :type="getPriorityColor(getPriorityLabel(rec.priority))" :value="getPriorityLabel(rec.priority)" class="priority-badge" />
                    </div>
                    <div class="rec-reason">
                      <NText>{{ rec.reason }}</NText>
                    </div>
                    <div class="rec-meta">
                      <NText class="meta-item">预计用时：{{ rec.estimatedHours }}小时</NText>
                      <NText v-if="rec.relatedGoal" class="meta-item">关联目标：{{ rec.relatedGoal }}</NText>
                    </div>
                  </div>
                </div>
              </div>
              
              <div v-else class="no-content">
                暂无任务推荐
              </div>
            </NCard>

            <NCard class="rec-card message-card" title="鼓励语">
              <div class="message-content">
                <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg" class="message-icon">
                  <circle cx="24" cy="24" r="20" fill="rgba(211, 55, 58, 0.1)"/>
                  <path d="M16 26L22 32L34 20" stroke="#D3373A" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <p class="message-text">{{ recommendations.message }}</p>
              </div>
            </NCard>
          </div>

          <div v-else-if="!recLoading" class="empty-state">
            <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="32" cy="32" r="28" stroke="#e8e8e8" stroke-width="2"/>
              <rect x="22" y="28" width="20" height="12" rx="2" stroke="#e8e8e8" stroke-width="2"/>
              <path d="M26 34L30 38L40 28" stroke="#e8e8e8" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <p>暂无任务推荐</p>
            <p class="empty-hint">点击上方按钮获取智能推荐</p>
          </div>
        </NTabPane>
      </NTabs>
    </div>
  </NMessageProvider>
</template>

<style scoped>
.planner-container {
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #DE2910 !important;
  padding: 8px 14px !important;
  border-radius: 12px !important;
  background: rgba(255, 255, 255, 0.6) !important;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
}

.back-btn svg {
  width: 16px;
  height: 16px;
}

.page-header h2 {
  font-size: 26px;
  color: #333;
  margin: 0;
  font-weight: 700;
}

.planner-tabs {
  --n-tabs-tab-color: #666;
  --n-tabs-tab-active-color: #D3373A;
  --n-tabs-tab-active-background: rgba(211, 55, 58, 0.1);
  --n-tabs-card-border-radius: 12px;
}

.action-card {
  margin-bottom: 20px;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-title {
  font-size: 16px;
  color: #666;
  font-weight: 500;
}

.generate-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #D3373A 0%, #FF6B6B 100%) !important;
  border: none !important;
  border-radius: 10px !important;
  padding: 10px 20px !important;
}

.generate-btn svg {
  width: 16px;
  height: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 40px;
  color: #999;
}

.empty-state svg {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
}

.empty-state p {
  margin: 0;
  font-size: 16px;
}

.empty-hint {
  font-size: 14px !important;
  margin-top: 8px !important;
  opacity: 0.7;
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.summary-card {
  padding: 24px !important;
  border-radius: 20px !important;
  background: rgba(255, 255, 255, 0.6) !important;
  backdrop-filter: blur(24px) saturate(180%) !important;
  -webkit-backdrop-filter: blur(24px) saturate(180%) !important;
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  box-shadow: 0 8px 28px rgba(139, 0, 0, 0.06) !important;
}

.summary-card :deep(.n-card-header) {
  margin-bottom: 16px !important;
}

.summary-card :deep(.n-card-header-title) {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.summary-text {
  font-size: 15px;
  color: #333;
  line-height: 1.8;
  margin: 0;
}

.progress-card {
  display: flex;
  flex-direction: column;
}

.progress-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
}

.main-progress {
  width: 100%;
}

.progress-label {
  margin-top: 16px;
  font-size: 14px;
  color: #666;
  text-align: center;
}

.date-card {
  background: linear-gradient(135deg, rgba(222, 41, 16, 0.05) 0%, rgba(255, 222, 0, 0.05) 100%) !important;
}

.date-display {
  text-align: center;
}

.date-full {
  font-size: 20px;
  color: #333;
  margin: 0;
  font-weight: 500;
}

.plan-card {
  margin-bottom: 20px;
  padding: 24px !important;
  border-radius: 20px !important;
  background: rgba(255, 255, 255, 0.6) !important;
  backdrop-filter: blur(24px) saturate(180%) !important;
  -webkit-backdrop-filter: blur(24px) saturate(180%) !important;
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  box-shadow: 0 8px 28px rgba(139, 0, 0, 0.06) !important;
}

.plan-card :deep(.n-card-header) {
  margin-bottom: 16px !important;
}

.plan-card :deep(.n-card-header-title) {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.overview-card {
  background: linear-gradient(135deg, rgba(222, 41, 16, 0.05) 0%, rgba(255, 222, 0, 0.05) 100%) !important;
}

.overview-text {
  font-size: 15px;
  color: #333;
  line-height: 1.8;
  margin: 0;
}

.focus-areas {
  margin-top: 20px;
}

.areas-label {
  font-size: 14px;
  color: #666;
}

.areas-list {
  margin-top: 8px;
}

.area-badge {
  border-radius: 20px !important;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.03);
  transition: all 0.2s ease;
}

.task-item:hover {
  background: rgba(211, 55, 58, 0.03);
  transform: translateX(4px);
}

.task-index {
  width: 28px;
  height: 28px;
  background: rgba(211, 55, 58, 0.1);
  color: #D3373A;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}

.task-content {
  flex: 1;
}

.task-header {
  margin-bottom: 8px;
}

.task-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.task-title {
  font-weight: 600;
  font-size: 15px;
  color: #333;
}

.priority-badge {
  border-radius: 20px !important;
}

.task-meta-row {
  display: flex;
  gap: 12px;
}

.task-time {
  font-size: 13px;
  color: #999;
}

.task-hours {
  font-size: 12px;
  color: #D3373A;
  background: rgba(211, 55, 58, 0.1);
  padding: 3px 8px;
  border-radius: 6px;
}

.task-description {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.task-meta {
  display: flex;
  align-items: center;
  font-size: 12px;
}

.meta-label {
  color: #999;
}

.meta-value {
  color: #D3373A;
}

.no-content {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 14px;
}

.suggestions-card {
  background: rgba(82, 196, 26, 0.05) !important;
}

.suggestions-text {
  font-size: 14px;
  color: #666;
  margin: 0;
  line-height: 1.6;
}

.rec-card {
  margin-bottom: 20px;
  padding: 24px !important;
  border-radius: 20px !important;
  background: rgba(255, 255, 255, 0.6) !important;
  backdrop-filter: blur(24px) saturate(180%) !important;
  -webkit-backdrop-filter: blur(24px) saturate(180%) !important;
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  box-shadow: 0 8px 28px rgba(139, 0, 0, 0.06) !important;
}

.rec-card :deep(.n-card-header) {
  margin-bottom: 16px !important;
}

.rec-card :deep(.n-card-header-title) {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.recommendation-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recommendation-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.03);
  transition: all 0.2s ease;
}

.recommendation-item:hover {
  background: rgba(211, 55, 58, 0.03);
  transform: translateX(4px);
}

.rec-rank {
  width: 32px;
  height: 32px;
  background: rgba(211, 55, 58, 0.1);
  color: #D3373A;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 16px;
  flex-shrink: 0;
}

.rec-rank.top {
  background: linear-gradient(135deg, #D3373A 0%, #FF6B6B 100%);
  color: white;
}

.rec-content {
  flex: 1;
}

.rec-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.rec-task {
  font-weight: 600;
  font-size: 15px;
  color: #333;
}

.rec-reason {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.rec-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}

.meta-item {
  display: flex;
  align-items: center;
}

.message-card {
  background: linear-gradient(135deg, rgba(222, 41, 16, 0.08) 0%, rgba(255, 222, 0, 0.08) 100%) !important;
}

.message-content {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.message-icon {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
}

.message-text {
  font-size: 16px;
  color: #333;
  line-height: 1.8;
  margin: 0;
  font-style: italic;
}
</style>