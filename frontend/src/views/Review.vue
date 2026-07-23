﻿﻿﻿﻿﻿<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api'
import { 
  NCard, NButton, NMessageProvider, useMessage, NSelect, NBadge, 
  NSpace, NDivider, NText, NH3, NAlert, NTabs, NTabPane, 
  NInput, NInputNumber, NPopconfirm, NIcon, NEllipsis
} from 'naive-ui'

interface MemoryConflict {
  id: number
  userId: number
  factId: number
  key: string
  oldValue: string
  newValue: string
  conflictScore: number
  conflictType: string
  reviewStatus: string
  reviewResult: string
  aiAnalysis: string
  createdAt: string
  reviewedAt: string
}

interface MemoryAttribute {
  id: number
  userId: number
  category: string
  entity: string
  attribute: string
  value: string
  importance: number
  confidence: number
  sourceQuote: string
  accessCount: number
  lastAccessTime: string
  status: string
  createdAt: string
  updatedAt: string
}

interface FactHistory {
  id: number
  factId: number
  key: string
  value: string
  version: number
  changeReason: string
  createdAt: string
}

interface ReviewSummary {
  pendingConflicts: number
  lowConfidenceCount: number
  sourceAnomalyCount: number
  recentCount: number
}

const router = useRouter()
const message = useMessage()

const conflicts = ref<MemoryConflict[]>([])
const selectedConflict = ref<MemoryConflict | null>(null)
const factHistory = ref<FactHistory[]>([])
const activeFilter = ref('pending')
const pendingCount = ref(0)

const lowConfidenceAttributes = ref<MemoryAttribute[]>([])
const sourceAnomalyAttributes = ref<MemoryAttribute[]>([])
const recentAttributes = ref<MemoryAttribute[]>([])
const reviewSummary = ref<ReviewSummary>({
  pendingConflicts: 0,
  lowConfidenceCount: 0,
  sourceAnomalyCount: 0,
  recentCount: 0
})

const editingAttribute = ref<MemoryAttribute | null>(null)
const editForm = ref({
  value: '',
  importance: 50,
  confidence: 50,
  sourceQuote: ''
})

const filterOptions = [
  { label: '待审核', value: 'pending' },
  { label: '已解决', value: 'resolved' },
  { label: '全部', value: 'all' }
]

const filteredConflicts = computed(() => {
  if (activeFilter.value === 'pending') {
    return conflicts.value.filter(c => c.reviewStatus === 'PENDING')
  } else if (activeFilter.value === 'resolved') {
    return conflicts.value.filter(c => c.reviewStatus === 'RESOLVED')
  }
  return conflicts.value
})

const fetchConflicts = async () => {
  try {
    const res = await api.get('/memory/review/conflicts/all')
    conflicts.value = res.data || []
    pendingCount.value = conflicts.value.filter(c => c.reviewStatus === 'PENDING').length
  } catch (error) {
    message.error('获取冲突列表失败')
  }
}

const fetchPendingCount = async () => {
  try {
    const res = await api.get('/memory/review/conflicts/count')
    pendingCount.value = res.data?.pendingCount || 0
  } catch (error) {
    console.error('Failed to fetch pending count')
  }
}

const fetchFactHistory = async (factId: number) => {
  try {
    const res = await api.get(`/memory/review/fact/${factId}/history`)
    factHistory.value = res.data || []
  } catch (error) {
    message.error('获取历史记录失败')
  }
}

const fetchLowConfidence = async () => {
  try {
    const res = await api.get('/memory/review/low-confidence', { params: { threshold: 50 } })
    lowConfidenceAttributes.value = res.data?.attributes || []
  } catch (error) {
    message.error('获取低置信度记忆失败')
  }
}

const fetchSourceAnomaly = async () => {
  try {
    const res = await api.get('/memory/review/source-anomaly')
    sourceAnomalyAttributes.value = res.data?.attributes || []
  } catch (error) {
    message.error('获取来源异常记忆失败')
  }
}

const fetchRecent = async () => {
  try {
    const res = await api.get('/memory/review/recent', { params: { hours: 24 } })
    recentAttributes.value = res.data?.attributes || []
  } catch (error) {
    message.error('获取最近记忆失败')
  }
}

const fetchReviewSummary = async () => {
  try {
    const res = await api.get('/memory/review/summary')
    reviewSummary.value = res.data || { pendingConflicts: 0, lowConfidenceCount: 0, sourceAnomalyCount: 0, recentCount: 0 }
  } catch (error) {
    console.error('Failed to fetch review summary')
  }
}

const openConflictDetail = (conflict: MemoryConflict) => {
  selectedConflict.value = conflict
  fetchFactHistory(conflict.factId)
}

const closeDetail = () => {
  selectedConflict.value = null
  factHistory.value = []
}

const handleResolve = async (conflictId: number, action: string) => {
  try {
    let endpoint = ''
    if (action === 'overwrite') {
      endpoint = `/memory/review/conflicts/${conflictId}/overwrite`
    } else if (action === 'keep-history') {
      endpoint = `/memory/review/conflicts/${conflictId}/keep-history`
    } else if (action === 'delete-new') {
      endpoint = `/memory/review/conflicts/${conflictId}/delete-new`
    }
    
    await api.post(endpoint)
    message.success('操作成功')
    closeDetail()
    fetchConflicts()
    fetchReviewSummary()
  } catch (error: any) {
    message.error(error.response?.data?.error || '操作失败')
  }
}

const handleConfirmAttribute = async (attrId: number) => {
  try {
    await api.post(`/memory/review/attribute/${attrId}/confirm`)
    message.success('已确认')
    fetchLowConfidence()
    fetchSourceAnomaly()
    fetchRecent()
    fetchReviewSummary()
  } catch (error: any) {
    message.error(error.response?.data?.error || '操作失败')
  }
}

const handleRejectAttribute = async (attrId: number) => {
  try {
    await api.post(`/memory/review/attribute/${attrId}/reject`)
    message.success('已拒绝')
    fetchLowConfidence()
    fetchSourceAnomaly()
    fetchRecent()
    fetchReviewSummary()
  } catch (error: any) {
    message.error(error.response?.data?.error || '操作失败')
  }
}

const openEditAttribute = (attr: MemoryAttribute) => {
  editingAttribute.value = attr
  editForm.value = {
    value: attr.value || '',
    importance: attr.importance || 50,
    confidence: attr.confidence || 0,
    sourceQuote: attr.sourceQuote || ''
  }
}

const saveEditAttribute = async () => {
  if (!editingAttribute.value) return
  
  try {
    await api.post(`/memory/review/attribute/${editingAttribute.value.id}/update`, editForm.value)
    message.success('已保存')
    editingAttribute.value = null
    fetchLowConfidence()
    fetchSourceAnomaly()
    fetchRecent()
  } catch (error: any) {
    message.error(error.response?.data?.error || '操作失败')
  }
}

const cancelEdit = () => {
  editingAttribute.value = null
}

const getConflictTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    CONTRADICTION: '语义冲突',
    CHANGE: '值变更',
    LOW_CONFIDENCE: '低可信度',
    SOURCE_ANOMALY: '来源异常'
  }
  return labels[type] || type
}

const getConflictTypeColor = (type: string): 'default' | 'error' | 'warning' | 'success' | 'info' | undefined => {
  const colors: Record<string, 'default' | 'error' | 'warning' | 'success' | 'info'> = {
    CONTRADICTION: 'error',
    CHANGE: 'warning',
    LOW_CONFIDENCE: 'warning',
    SOURCE_ANOMALY: 'info'
  }
  return colors[type] || undefined
}

const getReviewStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
    PENDING: '待审核',
    RESOLVED: '已解决',
    IGNORED: '已忽略'
  }
  return labels[status] || status
}

const getReviewResultLabel = (result: string) => {
  const labels: Record<string, string> = {
    OVERWRITE: '已覆盖',
    KEEP_HISTORY: '保留历史',
    DELETE_NEW: '删除新值',
    DELETE_OLD: '删除旧值'
  }
  return labels[result] || result
}

const getCategoryLabel = (category: string) => {
  const labels: Record<string, string> = {
    Person: '个人信息',
    Preference: '偏好喜好',
    Skill: '技能能力',
    Fact: '事实',
    Timeline: '事件',
    Goal: '目标',
    Todo: '待办',
    Relationship: '关系'
  }
  return labels[category] || category
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchConflicts()
  fetchPendingCount()
  fetchLowConfidence()
  fetchSourceAnomaly()
  fetchRecent()
  fetchReviewSummary()
})
</script>

<template>
  <NMessageProvider>
    <div style="padding: 28px;">
      <div style="display: flex; align-items: center; gap: 16px; margin-bottom: 28px;">
        <NButton size="small" @click="router.back()" style="border-radius: 12px;">← 返回</NButton>
        <h2 style="font-size: 26px; color: #333; margin: 0; font-weight: 700;">记忆审核</h2>
      </div>

      <NCard style="margin-bottom: 24px; background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(30px) saturate(180%); -webkit-backdrop-filter: blur(30px) saturate(180%); border: 1px solid rgba(255, 255, 255, 0.5); border-radius: 20px; box-shadow: 0 8px 32px rgba(139, 0, 0, 0.06);">
        <div class="summary-cards">
          <div class="summary-card">
            <div class="summary-count">{{ reviewSummary.pendingConflicts }}</div>
            <div class="summary-label">待处理冲突</div>
          </div>
          <div class="summary-card">
            <div class="summary-count warning">{{ reviewSummary.lowConfidenceCount }}</div>
            <div class="summary-label">低置信度</div>
          </div>
          <div class="summary-card">
            <div class="summary-count info">{{ reviewSummary.sourceAnomalyCount }}</div>
            <div class="summary-label">来源异常</div>
          </div>
          <div class="summary-card">
            <div class="summary-count success">{{ reviewSummary.recentCount }}</div>
            <div class="summary-label">最近新增</div>
          </div>
        </div>
      </NCard>

      <NTabs type="line" size="medium" style="margin-bottom: 20px;">
        <NTabPane name="conflicts" tab="冲突检测">
          <NCard style="background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(30px) saturate(180%); -webkit-backdrop-filter: blur(30px) saturate(180%); border: 1px solid rgba(255, 255, 255, 0.5); border-radius: 20px; box-shadow: 0 8px 32px rgba(139, 0, 0, 0.06);">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
              <NSelect v-model:value="activeFilter" style="width: 150px; border-radius: 12px;" :options="filterOptions" />
              <NText>待审核: {{ pendingCount }} 条</NText>
            </div>

            <div v-if="filteredConflicts.length === 0" style="text-align: center; padding: 40px; color: #999;">
              <NH3>暂无{{ activeFilter === 'pending' ? '待审核' : '已解决' }}的冲突</NH3>
              <p style="margin-top: 10px;">{{ activeFilter === 'pending' ? '所有记忆都很和谐~' : '去查看待审核的冲突吧' }}</p>
            </div>

            <div v-else class="conflict-list">
              <div 
                v-for="conflict in filteredConflicts" 
                :key="conflict.id" 
                class="conflict-item"
                @click="openConflictDetail(conflict)"
              >
                <div class="conflict-header">
                  <NSpace>
                    <NText class="conflict-key">{{ conflict.key }}</NText>
                    <NBadge :type="getConflictTypeColor(conflict.conflictType)" :value="getConflictTypeLabel(conflict.conflictType)" />
                    <NBadge v-if="conflict.reviewStatus === 'PENDING'" type="warning" value="待审核" />
                    <NBadge v-else type="success" :value="getReviewStatusLabel(conflict.reviewStatus)" />
                  </NSpace>
                  <NText class="conflict-date">{{ formatDate(conflict.createdAt) }}</NText>
                </div>
                
                <div class="conflict-content">
                  <div class="value-row">
                    <span class="value-label">旧值:</span>
                    <span class="old-value">{{ conflict.oldValue }}</span>
                  </div>
                  <div class="arrow">↓</div>
                  <div class="value-row">
                    <span class="value-label">新值:</span>
                    <span class="new-value">{{ conflict.newValue }}</span>
                  </div>
                </div>

                <div v-if="conflict.conflictScore > 0" class="conflict-score">
                  冲突分数: {{ conflict.conflictScore }}
                </div>
              </div>
            </div>
          </NCard>
        </NTabPane>

        <NTabPane name="low-confidence" tab="低置信度">
          <NCard style="background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(30px) saturate(180%); -webkit-backdrop-filter: blur(30px) saturate(180%); border: 1px solid rgba(255, 255, 255, 0.5); border-radius: 20px; box-shadow: 0 8px 32px rgba(139, 0, 0, 0.06);">
            <div v-if="lowConfidenceAttributes.length === 0" style="text-align: center; padding: 40px; color: #999;">
              <NH3>暂无低置信度记忆</NH3>
              <p style="margin-top: 10px;">所有记忆都经过验证~</p>
            </div>

            <div v-else class="attribute-list">
              <div v-for="attr in lowConfidenceAttributes" :key="attr.id" class="attribute-item">
                <div class="attr-header">
                  <NSpace>
                    <NBadge type="warning" :value="getCategoryLabel(attr.category)" />
                    <NText class="attr-entity">{{ attr.entity }}</NText>
                    <NText class="attr-attr">{{ attr.attribute }}</NText>
                  </NSpace>
                  <NText class="attr-date">{{ formatDate(attr.createdAt) }}</NText>
                </div>
                <div class="attr-content">
                  <NText>{{ attr.value }}</NText>
                </div>
                <div class="attr-meta">
                  <span class="meta-item">置信度: <span class="confidence-value">{{ attr.confidence }}</span></span>
                  <span class="meta-item">重要度: {{ attr.importance }}</span>
                  <span v-if="attr.sourceQuote" class="meta-item"><NEllipsis>{{ attr.sourceQuote }}</NEllipsis></span>
                </div>
                <div class="attr-actions">
                  <NButton size="small" type="primary" @click="handleConfirmAttribute(attr.id)">确认</NButton>
                  <NButton size="small" @click="openEditAttribute(attr)">编辑</NButton>
                  <NPopconfirm negative-text="取消" positive-text="确定" @positive-click="handleRejectAttribute(attr.id)">
                    <template #trigger>
                      <NButton size="small" type="error">拒绝</NButton>
                    </template>
                    确定要拒绝这条记忆吗？
                  </NPopconfirm>
                </div>
              </div>
            </div>
          </NCard>
        </NTabPane>

        <NTabPane name="source-anomaly" tab="来源异常">
          <NCard style="background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(30px) saturate(180%); -webkit-backdrop-filter: blur(30px) saturate(180%); border: 1px solid rgba(255, 255, 255, 0.5); border-radius: 20px; box-shadow: 0 8px 32px rgba(139, 0, 0, 0.06);">
            <div v-if="sourceAnomalyAttributes.length === 0" style="text-align: center; padding: 40px; color: #999;">
              <NH3>暂无来源异常记忆</NH3>
              <p style="margin-top: 10px;">所有记忆都有来源记录~</p>
            </div>

            <div v-else class="attribute-list">
              <div v-for="attr in sourceAnomalyAttributes" :key="attr.id" class="attribute-item">
                <div class="attr-header">
                  <NSpace>
                    <NBadge type="info" :value="getCategoryLabel(attr.category)" />
                    <NText class="attr-entity">{{ attr.entity }}</NText>
                    <NText class="attr-attr">{{ attr.attribute }}</NText>
                  </NSpace>
                  <NText class="attr-date">{{ formatDate(attr.createdAt) }}</NText>
                </div>
                <div class="attr-content">
                  <NText>{{ attr.value }}</NText>
                </div>
                <div class="attr-meta">
                  <span class="meta-item">置信度: {{ attr.confidence }}</span>
                  <span class="meta-item">重要度: {{ attr.importance }}</span>
                  <span class="meta-item" style="color: #ff9800;">⚠️ 无来源记录</span>
                </div>
                <div class="attr-actions">
                  <NButton size="small" type="primary" @click="handleConfirmAttribute(attr.id)">确认</NButton>
                  <NButton size="small" @click="openEditAttribute(attr)">编辑</NButton>
                  <NPopconfirm negative-text="取消" positive-text="确定" @positive-click="handleRejectAttribute(attr.id)">
                    <template #trigger>
                      <NButton size="small" type="error">拒绝</NButton>
                    </template>
                    确定要拒绝这条记忆吗？
                  </NPopconfirm>
                </div>
              </div>
            </div>
          </NCard>
        </NTabPane>

        <NTabPane name="recent" tab="最近新增">
          <NCard style="background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(30px) saturate(180%); -webkit-backdrop-filter: blur(30px) saturate(180%); border: 1px solid rgba(255, 255, 255, 0.5); border-radius: 20px; box-shadow: 0 8px 32px rgba(139, 0, 0, 0.06);">
            <div v-if="recentAttributes.length === 0" style="text-align: center; padding: 40px; color: #999;">
              <NH3>暂无最近新增记忆</NH3>
              <p style="margin-top: 10px;">最近没有新的记忆被添加~</p>
            </div>

            <div v-else class="attribute-list">
              <div v-for="attr in recentAttributes" :key="attr.id" class="attribute-item">
                <div class="attr-header">
                  <NSpace>
                    <NBadge type="success" :value="getCategoryLabel(attr.category)" />
                    <NText class="attr-entity">{{ attr.entity }}</NText>
                    <NText class="attr-attr">{{ attr.attribute }}</NText>
                  </NSpace>
                  <NText class="attr-date">{{ formatDate(attr.createdAt) }}</NText>
                </div>
                <div class="attr-content">
                  <NText>{{ attr.value }}</NText>
                </div>
                <div class="attr-meta">
                  <span class="meta-item">置信度: {{ attr.confidence }}</span>
                  <span class="meta-item">重要度: {{ attr.importance }}</span>
                  <span v-if="attr.sourceQuote" class="meta-item"><NEllipsis>{{ attr.sourceQuote }}</NEllipsis></span>
                </div>
                <div class="attr-actions">
                  <NButton size="small" type="primary" @click="handleConfirmAttribute(attr.id)">确认</NButton>
                  <NButton size="small" @click="openEditAttribute(attr)">编辑</NButton>
                  <NPopconfirm negative-text="取消" positive-text="确定" @positive-click="handleRejectAttribute(attr.id)">
                    <template #trigger>
                      <NButton size="small" type="error">拒绝</NButton>
                    </template>
                    确定要拒绝这条记忆吗？
                  </NPopconfirm>
                </div>
              </div>
            </div>
          </NCard>
        </NTabPane>
      </NTabs>

      <NCard v-if="selectedConflict" class="detail-card">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
          <NH3>冲突详情</NH3>
          <NButton size="small" @click="closeDetail">关闭</NButton>
        </div>

        <div class="detail-section">
          <NText strong>关键词: </NText>
          <NText>{{ selectedConflict.key }}</NText>
        </div>

        <NDivider />

        <div class="detail-section">
          <NH3>值对比</NH3>
          <div class="compare-box">
            <div class="compare-item old">
              <div class="compare-label">当前值</div>
              <div class="compare-value">{{ selectedConflict.oldValue }}</div>
            </div>
            <div class="compare-arrow">↔</div>
            <div class="compare-item new">
              <div class="compare-label">新值</div>
              <div class="compare-value">{{ selectedConflict.newValue }}</div>
            </div>
          </div>
        </div>

        <NDivider />

        <div class="detail-section">
          <NH3>AI分析</NH3>
          <NAlert :title="getConflictTypeLabel(selectedConflict.conflictType)" type="info" style="margin-bottom: 10px;" />
          <NText>{{ selectedConflict.aiAnalysis || '暂无分析' }}</NText>
        </div>

        <NDivider />

        <div class="detail-section">
          <NH3>历史记录</NH3>
          <div v-if="factHistory.length === 0" style="color: #999;">暂无历史记录</div>
          <div v-else class="history-list">
            <div v-for="history in factHistory" :key="history.id" class="history-item">
              <NText>v{{ history.version }}</NText>
              <NText>{{ history.value }}</NText>
              <NText class="history-reason">{{ history.changeReason || '自动更新' }}</NText>
              <NText class="history-date">{{ formatDate(history.createdAt) }}</NText>
            </div>
          </div>
        </div>

        <NDivider />

        <div v-if="selectedConflict.reviewStatus === 'PENDING'" class="action-section">
          <NH3>处理方式</NH3>
          <NSpace wrap>
            <NButton type="primary" @click="handleResolve(selectedConflict.id, 'overwrite')">
              覆盖旧值
            </NButton>
            <NButton type="info" @click="handleResolve(selectedConflict.id, 'keep-history')">
              保留历史
            </NButton>
            <NButton type="warning" @click="handleResolve(selectedConflict.id, 'delete-new')">
              忽略新值
            </NButton>
          </NSpace>
        </div>

        <div v-else class="action-section">
          <NH3>处理结果</NH3>
          <NText type="success">{{ getReviewResultLabel(selectedConflict.reviewResult) }}</NText>
          <NText style="margin-left: 10px;">{{ formatDate(selectedConflict.reviewedAt) }}</NText>
        </div>
      </NCard>

      <NCard v-if="editingAttribute" class="detail-card">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
          <NH3>编辑记忆</NH3>
          <NButton size="small" @click="cancelEdit">关闭</NButton>
        </div>

        <div class="detail-section">
          <NText strong>类别: </NText>
          <NText>{{ getCategoryLabel(editingAttribute.category) }}</NText>
        </div>

        <div class="detail-section">
          <NText strong>实体: </NText>
          <NText>{{ editingAttribute.entity }}</NText>
        </div>

        <div class="detail-section">
          <NText strong>属性: </NText>
          <NText>{{ editingAttribute.attribute }}</NText>
        </div>

        <NDivider />

        <div class="detail-section">
          <NText strong>值: </NText>
          <NInput v-model:value="editForm.value" placeholder="请输入值" />
        </div>

        <div class="detail-section">
          <NText strong>重要度: </NText>
          <NInputNumber v-model:value="editForm.importance" :min="0" :max="100" />
        </div>

        <div class="detail-section">
          <NText strong>置信度: </NText>
          <NInputNumber v-model:value="editForm.confidence" :min="0" :max="100" />
        </div>

        <div class="detail-section">
          <NText strong>来源引用: </NText>
          <NInput v-model:value="editForm.sourceQuote" placeholder="用户原话引用" />
        </div>

        <NDivider />

        <div class="action-section">
          <NSpace>
            <NButton type="primary" @click="saveEditAttribute">保存</NButton>
            <NButton @click="cancelEdit">取消</NButton>
          </NSpace>
        </div>
      </NCard>
    </div>
  </NMessageProvider>
</template>

<style scoped>
.summary-cards {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.summary-card {
  flex: 1;
  min-width: 150px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  text-align: center;
}

.summary-count {
  font-size: 30px;
  font-weight: bold;
  color: #DE2910;
}

.summary-count.warning {
  color: #ff9800;
}

.summary-count.info {
  color: #2196f3;
}

.summary-count.success {
  color: #4caf50;
}

.summary-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.conflict-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.conflict-item {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.conflict-item:hover {
  background: #f0f0f0;
  transform: translateX(4px);
}

.conflict-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.conflict-key {
  font-weight: 600;
  font-size: 16px;
  color: #333;
}

.conflict-date {
  color: #999;
  font-size: 12px;
}

.conflict-content {
  margin-bottom: 8px;
}

.value-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.value-label {
  font-size: 12px;
  color: #666;
  width: 40px;
}

.old-value {
  color: #666;
  font-style: italic;
}

.new-value {
  color: #e53935;
  font-weight: 500;
}

.arrow {
  color: #999;
  text-align: center;
  margin: 4px 0;
}

.conflict-score {
  font-size: 12px;
  color: #e53935;
}

.attribute-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.attribute-item {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.attr-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.attr-entity {
  font-weight: 600;
  font-size: 16px;
  color: #333;
}

.attr-attr {
  font-size: 14px;
  color: #666;
}

.attr-date {
  color: #999;
  font-size: 12px;
}

.attr-content {
  margin-bottom: 12px;
  font-size: 14px;
  color: #333;
}

.attr-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  font-size: 12px;
  color: #666;
}

.meta-item {
  display: flex;
  align-items: center;
}

.confidence-value {
  color: #ff9800;
  font-weight: 500;
}

.attr-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.detail-card {
  position: fixed;
  top: 20px;
  right: 20px;
  width: 450px;
  max-height: calc(100vh - 40px);
  overflow-y: auto;
  background: rgba(255, 255, 255, 0.7) !important;
  backdrop-filter: blur(30px) saturate(180%);
  -webkit-backdrop-filter: blur(30px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  border-radius: 20px !important;
  box-shadow: 0 12px 40px rgba(139, 0, 0, 0.15) !important;
}

.detail-section {
  margin-bottom: 16px;
}

.compare-box {
  display: flex;
  align-items: center;
  gap: 20px;
}

.compare-item {
  flex: 1;
  padding: 12px;
  border-radius: 8px;
}

.compare-item.old {
  background: #f5f5f5;
}

.compare-item.new {
  background: #fff3e0;
}

.compare-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.compare-value {
  font-size: 14px;
  font-weight: 500;
}

.compare-arrow {
  font-size: 24px;
  color: #999;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  background: #fafafa;
  border-radius: 4px;
  font-size: 13px;
}

.history-reason {
  color: #999;
  font-size: 12px;
}

.history-date {
  margin-left: auto;
  color: #bbb;
  font-size: 11px;
}

.action-section {
  padding-top: 16px;
}
</style>