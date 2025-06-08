<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'

interface WorkOrder {
  id?: number
  woCode?: string
  title: string
  category: string
  priority: number
  slaMinutes: number
  status?: string
}

const workOrders = ref<WorkOrder[]>([])
const loading = ref(false)
const showCreateForm = ref(false)

const newWorkOrder = reactive<WorkOrder>({
  title: '',
  category: 'MAINTENANCE',
  priority: 1,
  slaMinutes: 240
})

const apiBase = 'http://localhost:8080/api'

const loadWorkOrders = async () => {
  loading.value = true
  try {
    console.log('正在加载工单...')
    const response = await axios.get(`${apiBase}/orders`)
    console.log('工单加载成功:', response.data)
    workOrders.value = response.data
  } catch (error) {
    console.error('加载工单失败:', error)
    if (error.code === 'ERR_NETWORK') {
      alert('网络连接失败，请检查后端服务是否运行在 http://localhost:8080')
    } else {
      alert(`加载工单失败: ${error.message}`)
    }
  } finally {
    loading.value = false
  }
}

const createWorkOrder = async () => {
  loading.value = true
  try {
    const response = await axios.post(`${apiBase}/orders`, newWorkOrder)
    workOrders.value.push(response.data)
    
    // 重置表单
    Object.assign(newWorkOrder, {
      title: '',
      category: 'MAINTENANCE',
      priority: 1,
      slaMinutes: 240
    })
    showCreateForm.value = false
    alert('工单创建成功！')
  } catch (error) {
    console.error('创建工单失败:', error)
    alert('创建工单失败，请检查后端服务')
  } finally {
    loading.value = false
  }
}

const submitWorkOrder = async (id: number) => {
  loading.value = true
  try {
    const response = await axios.post(`${apiBase}/orders/${id}/submit`)
    const index = workOrders.value.findIndex(wo => wo.id === id)
    if (index !== -1) {
      workOrders.value[index] = response.data
    }
    alert('工单提交成功！')
  } catch (error) {
    console.error('提交工单失败:', error)
    alert('提交工单失败')
  } finally {
    loading.value = false
  }
}

const getStatusColor = (status: string) => {
  switch (status) {
    case 'DRAFT': return '#ffa500'
    case 'SUBMITTED': return '#007bff'
    case 'APPROVED': return '#28a745'
    case 'CLOSED': return '#6c757d'
    default: return '#6c757d'
  }
}

const getPriorityText = (priority: number) => {
  switch (priority) {
    case 1: return '高'
    case 2: return '中'
    case 3: return '低'
    default: return '未知'
  }
}

onMounted(() => {
  // 页面加载时获取现有工单列表
  loadWorkOrders()
})
</script>

<template>
  <div class="app">
    <header class="header">
      <h1>🔧 运维工单系统</h1>
      <button @click="showCreateForm = !showCreateForm" class="btn btn-primary">
        {{ showCreateForm ? '取消' : '创建工单' }}
      </button>
    </header>

    <!-- 创建工单表单 -->
    <div v-if="showCreateForm" class="create-form">
      <h3>创建新工单</h3>
      <form @submit.prevent="createWorkOrder">
        <div class="form-group">
          <label>工单标题:</label>
          <input 
            v-model="newWorkOrder.title" 
            type="text" 
            required 
            placeholder="请输入工单标题"
            class="form-control"
          />
        </div>
        
        <div class="form-group">
          <label>分类:</label>
          <select v-model="newWorkOrder.category" class="form-control">
            <option value="MAINTENANCE">维护</option>
            <option value="INCIDENT">故障</option>
            <option value="REQUEST">请求</option>
            <option value="CHANGE">变更</option>
          </select>
        </div>
        
        <div class="form-group">
          <label>优先级:</label>
          <select v-model="newWorkOrder.priority" class="form-control">
            <option :value="1">高</option>
            <option :value="2">中</option>
            <option :value="3">低</option>
          </select>
        </div>
        
        <div class="form-group">
          <label>SLA时间 (分钟):</label>
          <input 
            v-model.number="newWorkOrder.slaMinutes" 
            type="number" 
            min="1" 
            required 
            class="form-control"
          />
        </div>
        
        <button type="submit" :disabled="loading" class="btn btn-success">
          {{ loading ? '创建中...' : '创建工单' }}
        </button>
      </form>
    </div>

    <!-- 工单列表 -->
    <div class="workorder-list">
      <h3>工单列表</h3>
      
      <div v-if="workOrders.length === 0" class="empty-state">
        <p>暂无工单，点击上方按钮创建第一个工单</p>
      </div>
      
      <div v-for="workOrder in workOrders" :key="workOrder.id" class="workorder-card">
        <div class="card-header">
          <span class="wo-code">{{ workOrder.woCode }}</span>
          <span 
            class="status-badge" 
            :style="{ backgroundColor: getStatusColor(workOrder.status || '') }"
          >
            {{ workOrder.status }}
          </span>
        </div>
        
        <div class="card-body">
          <h4>{{ workOrder.title }}</h4>
          <div class="details">
            <span class="detail-item">分类: {{ workOrder.category }}</span>
            <span class="detail-item">优先级: {{ getPriorityText(workOrder.priority) }}</span>
            <span class="detail-item">SLA: {{ workOrder.slaMinutes }}分钟</span>
          </div>
          
          <div class="actions" v-if="workOrder.status === 'DRAFT'">
            <button 
              @click="submitWorkOrder(workOrder.id!)" 
              :disabled="loading"
              class="btn btn-primary btn-sm"
            >
              提交工单
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.app {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 10px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
}

.header h1 {
  margin: 0;
  font-size: 28px;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(0,0,0,0.2);
}

.btn-primary {
  background: #007bff;
  color: white;
}

.btn-success {
  background: #28a745;
  color: white;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
}

.create-form {
  background: white;
  padding: 25px;
  border-radius: 10px;
  margin-bottom: 30px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  border: 1px solid #e0e0e0;
}

.create-form h3 {
  margin-top: 0;
  color: #333;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 600;
  color: #555;
}

.form-control {
  width: 100%;
  padding: 10px;
  border: 2px solid #e0e0e0;
  border-radius: 5px;
  font-size: 14px;
  transition: border-color 0.3s ease;
}

.form-control:focus {
  outline: none;
  border-color: #007bff;
}

.workorder-list h3 {
  color: #333;
  margin-bottom: 20px;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #666;
  background: #f8f9fa;
  border-radius: 10px;
}

.workorder-card {
  background: white;
  border-radius: 10px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  border: 1px solid #e0e0e0;
  overflow: hidden;
  transition: transform 0.3s ease;
}

.workorder-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
}

.card-header {
  background: #f8f9fa;
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e0e0e0;
}

.wo-code {
  font-family: 'Courier New', monospace;
  font-weight: bold;
  color: #333;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 15px;
  color: white;
  font-size: 12px;
  font-weight: bold;
}

.card-body {
  padding: 20px;
}

.card-body h4 {
  margin: 0 0 15px 0;
  color: #333;
}

.details {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.detail-item {
  background: #f8f9fa;
  padding: 5px 10px;
  border-radius: 5px;
  font-size: 13px;
  color: #666;
}

.actions {
  margin-top: 15px;
}
</style>
