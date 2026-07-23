<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api'
import { NCard, NButton, NInput, NModal, NForm, NFormItem, NSelect, useMessage, NMessageProvider, NInputNumber, NSwitch } from 'naive-ui'

interface Fact {
  id: number
  key: string
  value: string
  importance: number
  confidence: number
  accessCount: number
  memoryType: string
  status: string
  userId: number
  createdAt: string
  updatedAt: string
  source: 'fact' | 'attribute'  // 数据来源：fact=MemoryFact表，attribute=MemoryAttribute表
}

interface Timeline {
  id: number
  title: string
  description: string
  timestamp: string
  userId: number
  createdAt: string
}

interface Goal {
  id: number
  title: string
  description: string
  progress: number
  priority: number
  userId: number
  createdAt: string
  updatedAt: string
}

interface Todo {
  id: number
  title: string
  completed: boolean
  priority: number
  userId: number
  createdAt: string
}

const router = useRouter()
const message = useMessage()

const activeTab = ref('fact')
const facts = ref<Fact[]>([])
const timelines = ref<Timeline[]>([])
const goals = ref<Goal[]>([])
const todos = ref<Todo[]>([])

const showModal = ref(false)
const editMode = ref(false)
const currentItem = ref<Fact | Timeline | Goal | Todo | null>(null)

const formData = ref({
  key: '',
  value: '',
  importance: 50,
  title: '',
  description: '',
  timestamp: '',
  progress: 0,
  priority: 1,
  completed: false
})

const tabOptions = [
  { label: '事实常识', value: 'fact' },
  { label: '时间线', value: 'timeline' },
  { label: '目标计划', value: 'goal' },
  { label: '待办事项', value: 'todo' }
]

const fetchData = async () => {
  try {
    const [factRes, timelineRes, goalRes, todoRes, attrRes] = await Promise.all([
      api.get('/memory/fact'),
      api.get('/memory/timeline'),
      api.get('/memory/goal'),
      api.get('/memory/todo'),
      api.get('/memory/attribute')
    ])

    const factList = Array.isArray(factRes.data) ? [...factRes.data] : (factRes.data?.data || [])
    const attrList = Array.isArray(attrRes.data) ? [...attrRes.data] : (attrRes.data?.data || [])

    // 给 factList 添加 source 标记
    const factListWithSource = factList.map(fact => ({
      ...fact,
      source: 'fact' as const
    }))

    const attributeFacts = attrList.map(attr => ({
      id: attr.id,
      key: `${attr.category}-${attr.entity}-${attr.attribute}`,
      value: attr.value,
      importance: attr.importance || 50,
      confidence: attr.confidence || 50,
      accessCount: attr.accessCount || 0,
      memoryType: attr.category,
      status: attr.status || 'ACTIVE',
      userId: attr.userId,
      createdAt: attr.createdAt,
      updatedAt: attr.updatedAt,
      source: 'attribute' as const
    }))

    facts.value = [...factListWithSource, ...attributeFacts]
    timelines.value = Array.isArray(timelineRes.data) ? [...timelineRes.data] : (timelineRes.data?.data || [])
    goals.value = Array.isArray(goalRes.data) ? [...goalRes.data] : (goalRes.data?.data || [])
    todos.value = Array.isArray(todoRes.data) ? [...todoRes.data] : (todoRes.data?.data || [])
  } catch (error) {
    message.error('获取数据失败')
  }
}

const openAddModal = () => {
  editMode.value = false
  currentItem.value = null
  formData.value = {
    key: '',
    value: '',
    importance: 50,
    title: '',
    description: '',
    timestamp: new Date().toISOString().slice(0, 16),
    progress: 0,
    priority: 1,
    completed: false
  }
  showModal.value = true
}

const openEditModal = (item: Fact | Timeline | Goal | Todo) => {
  editMode.value = true
  currentItem.value = item
  
  if (activeTab.value === 'fact') {
    const fact = item as Fact
    formData.value = { key: fact.key, value: fact.value, importance: fact.importance ?? 50, title: '', description: '', timestamp: '', progress: 0, priority: 0, completed: false }
  } else if (activeTab.value === 'timeline') {
    const tl = item as Timeline
    formData.value = { key: '', value: '', importance: 50, title: tl.title, description: tl.description, timestamp: tl.timestamp.slice(0, 16), progress: 0, priority: 0, completed: false }
  } else if (activeTab.value === 'goal') {
    const goal = item as Goal
    formData.value = { key: '', value: '', importance: 50, title: goal.title, description: goal.description, timestamp: '', progress: goal.progress, priority: goal.priority, completed: false }
  } else if (activeTab.value === 'todo') {
    const todo = item as Todo
    formData.value = { key: '', value: '', importance: 50, title: todo.title, description: '', timestamp: '', progress: 0, priority: todo.priority, completed: todo.completed }
  }
  
  showModal.value = true
}

const handleSave = async () => {
  try {
    if (activeTab.value === 'fact') {
      if (editMode.value) {
        await api.put(`/memory/fact/${(currentItem.value as Fact).id}`, { key: formData.value.key, value: formData.value.value, score: formData.value.importance })
      } else {
        await api.post('/memory/fact', { key: formData.value.key, value: formData.value.value, score: formData.value.importance })
      }
    } else if (activeTab.value === 'timeline') {
      if (editMode.value) {
        await api.put(`/memory/timeline/${(currentItem.value as Timeline).id}`, { title: formData.value.title, description: formData.value.description, timestamp: formData.value.timestamp })
      } else {
        await api.post('/memory/timeline', { title: formData.value.title, description: formData.value.description, timestamp: formData.value.timestamp })
      }
    } else if (activeTab.value === 'goal') {
      if (editMode.value) {
        await api.put(`/memory/goal/${(currentItem.value as Goal).id}`, { title: formData.value.title, description: formData.value.description, progress: formData.value.progress, priority: formData.value.priority })
      } else {
        await api.post('/memory/goal', { title: formData.value.title, description: formData.value.description, progress: formData.value.progress, priority: formData.value.priority })
      }
    } else if (activeTab.value === 'todo') {
      if (editMode.value) {
        await api.put(`/memory/todo/${(currentItem.value as Todo).id}`, { title: formData.value.title, completed: formData.value.completed, priority: formData.value.priority })
      } else {
        await api.post('/memory/todo', { title: formData.value.title, completed: formData.value.completed, priority: formData.value.priority })
      }
    }
    
    message.success(editMode.value ? '更新成功' : '添加成功')
    showModal.value = false
    fetchData()
  } catch (error: any) {
    message.error(error.response?.data?.error || '操作失败')
  }
}

const handleDelete = async (item: Fact | Timeline | Goal | Todo) => {
  try {
    if (activeTab.value === 'fact') {
      const fact = item as Fact
      // 根据 source 调用不同的删除接口
      if (fact.source === 'attribute') {
        await api.delete(`/memory/attribute/${fact.id}`)
      } else {
        await api.delete(`/memory/fact/${fact.id}`)
      }
    } else if (activeTab.value === 'timeline') {
      await api.delete(`/memory/timeline/${(item as Timeline).id}`)
    } else if (activeTab.value === 'goal') {
      await api.delete(`/memory/goal/${(item as Goal).id}`)
    } else if (activeTab.value === 'todo') {
      await api.delete(`/memory/todo/${(item as Todo).id}`)
    }
    message.success('删除成功')
    fetchData()
  } catch (error: any) {
    message.error(error.response?.data?.error || '删除失败')
  }
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <NMessageProvider>
    <div style="padding: 28px;">
      <div style="display: flex; align-items: center; gap: 16px; margin-bottom: 28px;">
        <NButton size="small" @click="router.back()" style="border-radius: 12px;">← 返回</NButton>
        <h2 style="font-size: 26px; color: #333; margin: 0; font-weight: 700;">记忆管理</h2>
      </div>

      <NCard style="margin-bottom: 24px; background: rgba(255, 255, 255, 0.55); backdrop-filter: blur(30px) saturate(180%); -webkit-backdrop-filter: blur(30px) saturate(180%); border: 1px solid rgba(255, 255, 255, 0.5); border-radius: 20px; box-shadow: 0 8px 32px rgba(139, 0, 0, 0.08);">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <NSelect v-model:value="activeTab" style="width: 200px; border-radius: 12px;" :options="tabOptions" />
          <NButton type="primary" @click="openAddModal" style="border-radius: 12px;">添加</NButton>
        </div>
      </NCard>

      <NCard style="background: rgba(255, 255, 255, 0.55); backdrop-filter: blur(30px) saturate(180%); -webkit-backdrop-filter: blur(30px) saturate(180%); border: 1px solid rgba(255, 255, 255, 0.5); border-radius: 20px; box-shadow: 0 8px 32px rgba(139, 0, 0, 0.08);">
        <table v-if="activeTab === 'fact'" class="memory-table">
          <thead><tr><th>关键词</th><th>内容</th><th>类型</th><th>重要度</th><th>可信度</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="item in facts" :key="item.id">
              <td>{{ item.key }}</td>
              <td>{{ item.value }}</td>
              <td>{{ item.memoryType || 'Other' }}</td>
              <td>{{ item.importance ?? 0 }}</td>
              <td>{{ item.confidence ?? 0 }}</td>
              <td>{{ item.status || 'ACTIVE' }}</td>
              <td>
                <NButton size="small" type="primary" @click="openEditModal(item)">编辑</NButton>
                <NButton size="small" type="error" @click="handleDelete(item)">删除</NButton>
              </td>
            </tr>
          </tbody>
        </table>

        <table v-else-if="activeTab === 'timeline'" class="memory-table">
          <thead><tr><th>标题</th><th>描述</th><th>时间</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="item in timelines" :key="item.id">
              <td>{{ item.title }}</td>
              <td>{{ item.description }}</td>
              <td>{{ formatDate(item.timestamp) }}</td>
              <td>
                <NButton size="small" type="primary" @click="openEditModal(item)">编辑</NButton>
                <NButton size="small" type="error" @click="handleDelete(item)">删除</NButton>
              </td>
            </tr>
          </tbody>
        </table>

        <table v-else-if="activeTab === 'goal'" class="memory-table">
          <thead><tr><th>标题</th><th>描述</th><th>进度</th><th>优先级</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="item in goals" :key="item.id">
              <td>{{ item.title }}</td>
              <td>{{ item.description }}</td>
              <td>{{ item.progress }}%</td>
              <td>{{ item.priority }}</td>
              <td>
                <NButton size="small" type="primary" @click="openEditModal(item)">编辑</NButton>
                <NButton size="small" type="error" @click="handleDelete(item)">删除</NButton>
              </td>
            </tr>
          </tbody>
        </table>

        <table v-else-if="activeTab === 'todo'" class="memory-table">
          <thead><tr><th>标题</th><th>状态</th><th>优先级</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="item in todos" :key="item.id">
              <td>{{ item.title }}</td>
              <td>{{ item.completed ? '已完成' : '未完成' }}</td>
              <td>{{ item.priority }}</td>
              <td>
                <NButton size="small" type="primary" @click="openEditModal(item)">编辑</NButton>
                <NButton size="small" type="error" @click="handleDelete(item)">删除</NButton>
              </td>
            </tr>
          </tbody>
        </table>
      </NCard>
    </div>
    
    <NModal v-model:show="showModal" :title="editMode ? '编辑' : '添加'" preset="card">
      <NForm :model="formData">
        <template v-if="activeTab === 'fact'">
          <NFormItem label="关键词">
            <NInput v-model:value="formData.key" />
          </NFormItem>
          <NFormItem label="内容">
            <NInput v-model:value="formData.value" />
          </NFormItem>
          <NFormItem label="重要度">
            <NInputNumber v-model:value="formData.importance" :min="0" :max="100" />
          </NFormItem>
        </template>
        
        <template v-else-if="activeTab === 'timeline'">
          <NFormItem label="标题">
            <NInput v-model:value="formData.title" />
          </NFormItem>
          <NFormItem label="描述">
            <NInput v-model:value="formData.description" />
          </NFormItem>
          <NFormItem label="时间">
            <NInput v-model:value="formData.timestamp" />
          </NFormItem>
        </template>
        
        <template v-else-if="activeTab === 'goal'">
          <NFormItem label="标题">
            <NInput v-model:value="formData.title" />
          </NFormItem>
          <NFormItem label="描述">
            <NInput v-model:value="formData.description" />
          </NFormItem>
          <NFormItem label="进度">
            <NInputNumber v-model:value="formData.progress" :min="0" :max="100" />
          </NFormItem>
          <NFormItem label="优先级">
            <NInputNumber v-model:value="formData.priority" :min="1" :max="5" />
          </NFormItem>
        </template>
        
        <template v-else-if="activeTab === 'todo'">
          <NFormItem label="标题">
            <NInput v-model:value="formData.title" />
          </NFormItem>
          <NFormItem label="完成状态">
            <NSwitch v-model:value="formData.completed" />
          </NFormItem>
          <NFormItem label="优先级">
            <NInputNumber v-model:value="formData.priority" :min="1" :max="5" />
          </NFormItem>
        </template>
      </NForm>
      
      <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 20px;">
        <NButton @click="showModal = false">取消</NButton>
        <NButton type="primary" @click="handleSave">{{ editMode ? '更新' : '添加' }}</NButton>
      </div>
    </NModal>
  </NMessageProvider>
</template>

<style scoped>
.memory-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.memory-table th {
  background: linear-gradient(135deg, #DE2910 0%, #8B0000 100%);
  color: white;
  padding: 16px 22px;
  text-align: left;
  font-weight: 600;
  border-radius: 14px 14px 0 0;
  position: relative;
}

.memory-table th:first-child {
  border-radius: 14px 0 0 0;
}

.memory-table th:last-child {
  border-radius: 0 14px 0 0;
}

.memory-table td {
  padding: 16px 22px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.4);
  vertical-align: middle;
  transition: all 0.2s ease;
}

.memory-table tr {
  transition: all 0.2s ease;
}

.memory-table tr:hover {
  background-color: rgba(222, 41, 16, 0.06);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.memory-table tr:last-child td {
  border-bottom: none;
}

.memory-table tr:last-child td:first-child {
  border-radius: 0 0 0 14px;
}

.memory-table tr:last-child td:last-child {
  border-radius: 0 0 14px 0;
}
</style>