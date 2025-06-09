<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import axios from 'axios'

interface User {
  id: number
  username: string
  realName: string
  email: string
  role: string
  department: string
  organizationLevel: string
  enabled: boolean
}

interface WorkOrder {
  id?: number
  woCode?: string
  title: string
  description?: string
  category: string
  priority: string
  slaMinutes: number
  status?: string
  creatorId?: number
  creatorName?: string
  creatorDept?: string
  approverId?: number
  approverName?: string
  assigneeId?: number
  assigneeName?: string
  createdAt?: string
  updatedAt?: string
  deadline?: string
  completedAt?: string
}

interface Statistics {
  date: string
  totalOrders: number
  createdOrders: number
  completedOrders: number
  overdueOrders: number
  averageProcessingTime: number
  onTimeCompletionRate: number
}

// 全局状态
const currentUser = ref<User | null>(null)
const workOrders = ref<WorkOrder[]>([])
const users = ref<User[]>([])
const statistics = ref<Statistics | null>(null)
const loading = ref(false)
const activeTab = ref('dashboard')
const showCreateForm = ref(false)
const showLoginModal = ref(true)

// 登录表单
const loginForm = reactive({
  username: '',
  password: ''
})

// 新工单表单
const newWorkOrder = reactive<WorkOrder>({
  title: '',
  description: '',
  category: 'MAINTENANCE',
  priority: 'MEDIUM',
  slaMinutes: 240
})

const apiBase = 'http://localhost:8080/api'

// API 函数
const login = async () => {
  loading.value = true
  try {
    const response = await axios.post(`${apiBase}/users/login`, loginForm)
    if (response.data) {
      currentUser.value = response.data
      // 保存用户信息到localStorage
      localStorage.setItem('currentUser', JSON.stringify(response.data))
      showLoginModal.value = false
      await loadData()
      alert(`欢迎回来，${response.data.realName}！`)
    } else {
      alert('用户名或密码错误')
    }
  } catch (error) {
    console.error('登录失败:', error)
    alert('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

const logout = () => {
  currentUser.value = null
  workOrders.value = []
  users.value = []
  statistics.value = null
  showLoginModal.value = true
  activeTab.value = 'dashboard'
  // 清除localStorage中的用户信息
  localStorage.removeItem('currentUser')
}

const loadData = async () => {
  await Promise.all([
    loadWorkOrders(),
    loadUsers(),
    loadStatistics()
  ])
}

const loadWorkOrders = async () => {
  loading.value = true
  try {
    const response = await axios.get(`${apiBase}/orders`)
    workOrders.value = response.data
  } catch (error) {
    console.error('加载工单失败:', error)
  } finally {
    loading.value = false
  }
}

const loadUsers = async () => {
  try {
    const response = await axios.get(`${apiBase}/users`)
    users.value = response.data
  } catch (error) {
    console.error('加载用户失败:', error)
  }
}

const loadStatistics = async () => {
  try {
    // 使用总体统计数据获取更准确的信息
    const response = await axios.get(`${apiBase}/statistics/overall`)
    statistics.value = response.data
  } catch (error) {
    console.error('加载统计数据失败:', error)
    // 如果总体统计失败，回退到今日统计
    try {
      const today = new Date().toISOString().split('T')[0]
      const fallbackResponse = await axios.get(`${apiBase}/statistics/daily?date=${today}`)
      statistics.value = fallbackResponse.data
    } catch (fallbackError) {
      console.error('加载今日统计数据也失败:', fallbackError)
    }
  }
}

const createWorkOrder = async () => {
  loading.value = true
  try {
    const payload = {
      ...newWorkOrder,
      creatorId: currentUser.value?.id
    }
    const response = await axios.post(`${apiBase}/orders`, payload)
    workOrders.value.push(response.data)
    
    // 重置表单
    Object.assign(newWorkOrder, {
      title: '',
      description: '',
      category: 'MAINTENANCE',
      priority: 'MEDIUM',
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

const approveWorkOrder = async (id: number) => {
  loading.value = true
  try {
    const payload = {
      approverId: currentUser.value?.id,
      comment: '审批通过'
    }
    const response = await axios.post(`${apiBase}/orders/${id}/approve`, payload)
    const index = workOrders.value.findIndex(wo => wo.id === id)
    if (index !== -1) {
      workOrders.value[index] = response.data
    }
    alert('工单审批成功！')
  } catch (error) {
    console.error('审批工单失败:', error)
    alert('审批工单失败')
  } finally {
    loading.value = false
  }
}

const assignWorkOrder = async (id: number, operatorId: number) => {
  loading.value = true
  try {
    const response = await axios.post(`${apiBase}/orders/${id}/assign/${operatorId}`)
    const index = workOrders.value.findIndex(wo => wo.id === id)
    if (index !== -1) {
      workOrders.value[index] = response.data
    }
    alert('工单分派成功！')
  } catch (error) {
    console.error('分派工单失败:', error)
    alert('分派工单失败')
  } finally {
    loading.value = false
  }
}

const completeWorkOrder = async (id: number) => {
  loading.value = true
  try {
    const response = await axios.post(`${apiBase}/orders/${id}/complete`)
    const index = workOrders.value.findIndex(wo => wo.id === id)
    if (index !== -1) {
      workOrders.value[index] = response.data
    }
    alert('工单完成！')
  } catch (error) {
    console.error('完成工单失败:', error)
    alert('完成工单失败')
  } finally {
    loading.value = false
  }
}

// 工具函数
const getStatusColor = (status: string) => {
  switch (status) {
    case 'DRAFT': return '#6c757d'
    case 'SUBMITTED': return '#007bff'
    case 'APPROVING': return '#ffc107'
    case 'APPROVED': return '#28a745'
    case 'ASSIGNED': return '#17a2b8'
    case 'IN_PROGRESS': return '#fd7e14'
    case 'COMPLETED': return '#28a745'
    case 'CLOSED': return '#6c757d'
    case 'REJECTED': return '#dc3545'
    default: return '#6c757d'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'DRAFT': return '草稿'
    case 'SUBMITTED': return '已提交'
    case 'APPROVING': return '审批中'
    case 'APPROVED': return '已审批'
    case 'ASSIGNED': return '已分派'
    case 'IN_PROGRESS': return '执行中'
    case 'COMPLETED': return '已完成'
    case 'CLOSED': return '已关闭'
    case 'REJECTED': return '已拒绝'
    default: return '未知'
  }
}

const getPriorityText = (priority: string) => {
  switch (priority) {
    case 'HIGH': return '高'
    case 'MEDIUM': return '中'
    case 'LOW': return '低'
    default: return '未知'
  }
}

const getCategoryText = (category: string) => {
  switch (category) {
    case 'MAINTENANCE': return '维护'
    case 'INCIDENT': return '故障'
    case 'REQUEST': return '请求'
    case 'CHANGE': return '变更'
    case 'EMERGENCY': return '紧急'
    default: return '未知'
  }
}

const getRoleText = (role: string) => {
  switch (role) {
    case 'ADMIN': return '系统管理员'
    case 'DEPT_MANAGER': return '部门经理'
    case 'APPROVER': return '审批人员'
    case 'OPERATOR': return '运维人员'
    case 'USER': return '普通用户'
    default: return '未知'
  }
}

const getDepartmentText = (department: string) => {
  switch (department) {
    case 'IT_DEPT': return 'IT部门'
    case 'BUSINESS_DEPT': return '业务部门'
    case 'MAINTENANCE': return '维护部门'
    case 'SECURITY': return '安全部门'
    case 'FINANCE': return '财务部门'
    default: return department || '未知'
  }
}

// 计算属性
const filteredWorkOrders = computed(() => {
  if (!currentUser.value) return []
  
  const userRole = currentUser.value.role
  const userId = currentUser.value.id
  
  switch (userRole) {
    case 'ADMIN':
      return workOrders.value
    case 'DEPT_MANAGER':
    case 'APPROVER':
      return workOrders.value.filter(wo => 
        wo.creatorId === userId || 
        wo.approverId === userId ||
        wo.status === 'SUBMITTED' ||
        wo.status === 'APPROVING'
      )
    case 'OPERATOR':
      return workOrders.value.filter(wo => 
        wo.assigneeId === userId || 
        wo.status === 'APPROVED' ||
        wo.status === 'ASSIGNED'
      )
    case 'USER':
    default:
      return workOrders.value.filter(wo => wo.creatorId === userId)
  }
})

onMounted(() => {
  // 检查localStorage中是否有保存的用户会话
  const savedUser = localStorage.getItem('currentUser')
  if (savedUser) {
    try {
      currentUser.value = JSON.parse(savedUser)
      showLoginModal.value = false
      loadData()
    } catch (error) {
      console.error('恢复用户会话失败:', error)
      localStorage.removeItem('currentUser')
      showLoginModal.value = true
    }
  } else {
    showLoginModal.value = true
  }
})
</script>

<template>
  <div class="app">
    <!-- 登录模态框 -->
    <div v-if="showLoginModal" class="modal-overlay">
      <div class="modal">
        <div class="modal-header">
          <h2>🔐 运维工单系统登录</h2>
        </div>
        <div class="modal-body">
          <form @submit.prevent="login">
            <div class="form-group">
              <label>用户名:</label>
              <input 
                v-model="loginForm.username" 
                type="text" 
                required 
                placeholder="请输入用户名"
                class="form-control"
              />
            </div>
            <div class="form-group">
              <label>密码:</label>
              <input 
                v-model="loginForm.password" 
                type="password" 
                required 
                placeholder="请输入密码"
                class="form-control"
              />
            </div>
            <button type="submit" :disabled="loading" class="btn btn-primary btn-block">
              {{ loading ? '登录中...' : '登录' }}
            </button>
          </form>
          <div class="login-help">
            <p><strong>测试账号:</strong></p>
            <p>管理员: admin / password123</p>
            <p>部门经理: manager1 / password123</p>
            <p>运维人员: operator1 / password123</p>
            <p>普通用户: user1 / password123</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 主界面 -->
    <div v-if="currentUser" class="main-app">
      <!-- 顶部导航 -->
      <header class="header">
        <div class="header-left">
          <h1>🔧 运维工单管理系统</h1>
        </div>
        <div class="header-right">
          <span class="user-info">
            {{ currentUser.realName }} ({{ getRoleText(currentUser.role) }})
          </span>
          <button @click="logout" class="btn btn-secondary btn-sm">退出登录</button>
        </div>
      </header>

      <!-- 导航标签 -->
      <nav class="nav-tabs">
        <button 
          :class="['nav-tab', { active: activeTab === 'dashboard' }]"
          @click="activeTab = 'dashboard'"
        >
          📊 仪表板
        </button>
        <button 
          :class="['nav-tab', { active: activeTab === 'workorders' }]"
          @click="activeTab = 'workorders'"
        >
          📋 工单管理
        </button>
        <button 
          v-if="['ADMIN', 'DEPT_MANAGER'].includes(currentUser.role)"
          :class="['nav-tab', { active: activeTab === 'users' }]"
          @click="activeTab = 'users'"
        >
          👥 用户管理
        </button>
        <button 
          v-if="['ADMIN', 'DEPT_MANAGER', 'APPROVER'].includes(currentUser.role)"
          :class="['nav-tab', { active: activeTab === 'approval' }]"
          @click="activeTab = 'approval'"
        >
          ✅ 审批中心
        </button>
        <button 
          :class="['nav-tab', { active: activeTab === 'statistics' }]"
          @click="activeTab = 'statistics'"
        >
          📈 统计报告
        </button>
      </nav>

      <!-- 仪表板 -->
      <div v-if="activeTab === 'dashboard'" class="tab-content">
        <div class="dashboard">
          <h2>系统概览</h2>
          <div class="stats-grid" v-if="statistics">
            <div class="stat-card">
              <div class="stat-number">{{ statistics.totalOrders }}</div>
              <div class="stat-label">总工单数</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ statistics.completedOrders }}</div>
              <div class="stat-label">已完成工单</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ statistics.submittedOrders + statistics.approvedOrders + statistics.rejectedOrders }}</div>
              <div class="stat-label">处理中工单</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ statistics.overdueOrders }}</div>
              <div class="stat-label">逾期工单</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ statistics.onTimeCompletionRate.toFixed(1) }}%</div>
              <div class="stat-label">按时完成率</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ statistics.averageProcessingTime.toFixed(1) }}h</div>
              <div class="stat-label">平均处理时间</div>
            </div>
          </div>
          
          <div class="recent-orders">
            <h3>最近工单</h3>
            <div class="order-list">
              <div 
                v-for="order in filteredWorkOrders.slice(0, 5)" 
                :key="order.id" 
                class="order-item"
              >
                <span class="order-code">{{ order.woCode }}</span>
                <span class="order-title">{{ order.title }}</span>
                <span 
                  class="order-status"
                  :style="{ backgroundColor: getStatusColor(order.status || '') }"
                >
                  {{ getStatusText(order.status || '') }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 工单管理 -->
      <div v-if="activeTab === 'workorders'" class="tab-content">
        <div class="workorder-management">
          <div class="section-header">
            <h2>工单管理</h2>
            <button @click="showCreateForm = !showCreateForm" class="btn btn-primary">
              {{ showCreateForm ? '取消' : '创建工单' }}
            </button>
          </div>

          <!-- 创建工单表单 -->
          <div v-if="showCreateForm" class="create-form">
            <h3>创建新工单</h3>
            <form @submit.prevent="createWorkOrder">
              <div class="form-row">
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
                    <option value="EMERGENCY">紧急</option>
                  </select>
                </div>
              </div>
              
              <div class="form-row">
                <div class="form-group">
                  <label>优先级:</label>
                  <select v-model="newWorkOrder.priority" class="form-control">
                    <option value="HIGH">高</option>
                    <option value="MEDIUM">中</option>
                    <option value="LOW">低</option>
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
              </div>
              
              <div class="form-group">
                <label>详细描述:</label>
                <textarea 
                  v-model="newWorkOrder.description" 
                  rows="4" 
                  placeholder="请详细描述工单内容..."
                  class="form-control"
                ></textarea>
              </div>
              
              <button type="submit" :disabled="loading" class="btn btn-success">
                {{ loading ? '创建中...' : '创建工单' }}
              </button>
            </form>
          </div>

          <!-- 工单列表 -->
          <div class="workorder-list">
            <div v-if="filteredWorkOrders.length === 0" class="empty-state">
              <p>暂无工单，点击上方按钮创建第一个工单</p>
            </div>
            
            <div v-for="workOrder in filteredWorkOrders" :key="workOrder.id" class="workorder-card">
              <div class="card-header">
                <div class="card-title">
                  <span class="wo-code">{{ workOrder.woCode }}</span>
                  <h4>{{ workOrder.title }}</h4>
                </div>
                <span 
                  class="status-badge" 
                  :style="{ backgroundColor: getStatusColor(workOrder.status || '') }"
                >
                  {{ getStatusText(workOrder.status || '') }}
                </span>
              </div>
              
              <div class="card-body">
                <div class="workorder-details">
                  <div class="detail-row">
                    <span class="detail-item">分类: {{ getCategoryText(workOrder.category) }}</span>
                    <span class="detail-item">优先级: {{ getPriorityText(workOrder.priority) }}</span>
                    <span class="detail-item">SLA: {{ workOrder.slaMinutes }}分钟</span>
                  </div>
                  <div class="detail-row">
                    <span class="detail-item">创建人: {{ workOrder.creatorName || '未知' }}</span>
                    <span class="detail-item">创建时间: {{ workOrder.createdAt ? new Date(workOrder.createdAt).toLocaleString() : '未知' }}</span>
                  </div>
                  <div v-if="workOrder.description" class="description">
                    <strong>描述:</strong> {{ workOrder.description }}
                  </div>
                </div>
                
                <div class="actions">
                  <!-- 提交工单 -->
                  <button 
                    v-if="workOrder.status === 'DRAFT' && workOrder.creatorId === currentUser.id"
                    @click="submitWorkOrder(workOrder.id!)" 
                    :disabled="loading"
                    class="btn btn-primary btn-sm"
                  >
                    提交工单
                  </button>
                  
                  <!-- 审批工单 -->
                  <button 
                    v-if="workOrder.status === 'SUBMITTED' && ['ADMIN', 'DEPT_MANAGER', 'APPROVER'].includes(currentUser.role)"
                    @click="approveWorkOrder(workOrder.id!)" 
                    :disabled="loading"
                    class="btn btn-success btn-sm"
                  >
                    审批通过
                  </button>
                  
                  <!-- 分派工单 -->
                  <select 
                    v-if="workOrder.status === 'APPROVED' && ['ADMIN', 'DEPT_MANAGER'].includes(currentUser.role)"
                    @change="assignWorkOrder(workOrder.id!, parseInt($event.target.value))"
                    class="form-control btn-sm assign-select"
                  >
                    <option value="">选择运维人员</option>
                    <option 
                      v-for="user in users.filter(u => u.role === 'OPERATOR')" 
                      :key="user.id" 
                      :value="user.id"
                    >
                      {{ user.realName }}
                    </option>
                  </select>
                  
                  <!-- 完成工单 -->
                  <button 
                    v-if="(workOrder.status === 'ASSIGNED' || workOrder.status === 'IN_PROGRESS') && 
                           (workOrder.assigneeId === currentUser.id || ['ADMIN'].includes(currentUser.role))"
                    @click="completeWorkOrder(workOrder.id!)" 
                    :disabled="loading"
                    class="btn btn-warning btn-sm"
                  >
                    标记完成
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 用户管理 -->
      <div v-if="activeTab === 'users'" class="tab-content">
        <div class="user-management">
          <div class="section-header">
            <h2>用户管理</h2>
            <span class="user-count">共 {{ users.length }} 个用户</span>
          </div>
          
          <div v-if="users.length === 0" class="empty-state">
            <p>暂无用户数据，请检查系统连接</p>
          </div>
          
          <div v-else class="user-list">
            <div v-for="user in users" :key="user.id" class="user-card">
              <div class="user-info">
                <h4>{{ user.realName }}</h4>
                <p><strong>用户名:</strong> {{ user.username }}</p>
                <p><strong>邮箱:</strong> {{ user.email }}</p>
                <p><strong>角色:</strong> {{ getRoleText(user.role) }}</p>
                <p><strong>部门:</strong> {{ getDepartmentText(user.department) }}</p>
                <p><strong>级别:</strong> {{ user.organizationLevel }}</p>
              </div>
              <div class="user-status">
                <span :class="['status-indicator', user.enabled ? 'enabled' : 'disabled']"></span>
                <span>{{ user.enabled ? '启用' : '禁用' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 审批中心 -->
      <div v-if="activeTab === 'approval'" class="tab-content">
        <div class="approval-center">
          <h2>审批中心</h2>
          <div class="pending-approvals">
            <div v-for="order in workOrders.filter(wo => wo.status === 'SUBMITTED')" :key="order.id" class="approval-card">
              <div class="approval-header">
                <span class="wo-code">{{ order.woCode }}</span>
                <span class="priority" :class="order.priority.toLowerCase()">{{ getPriorityText(order.priority) }}</span>
              </div>
              <div class="approval-body">
                <h4>{{ order.title }}</h4>
                <p><strong>创建人:</strong> {{ order.creatorName }}</p>
                <p><strong>分类:</strong> {{ getCategoryText(order.category) }}</p>
                <p><strong>创建时间:</strong> {{ order.createdAt ? new Date(order.createdAt).toLocaleString() : '未知' }}</p>
                <div class="approval-actions">
                  <button 
                    @click="approveWorkOrder(order.id!)" 
                    :disabled="loading"
                    class="btn btn-success btn-sm"
                  >
                    批准
                  </button>
                  <button class="btn btn-danger btn-sm">拒绝</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 统计报告 -->
      <div v-if="activeTab === 'statistics'" class="tab-content">
        <div class="statistics">
          <h2>统计报告</h2>
          <div v-if="statistics" class="detailed-stats">
            <div class="stats-section">
              <h3>工单统计</h3>
              <div class="stats-grid">
                <div class="stat-item">
                  <label>总工单数:</label>
                  <span>{{ statistics.totalOrders }}</span>
                </div>
                <div class="stat-item">
                  <label>今日新建:</label>
                  <span>{{ statistics.createdOrders }}</span>
                </div>
                <div class="stat-item">
                  <label>今日完成:</label>
                  <span>{{ statistics.completedOrders }}</span>
                </div>
                <div class="stat-item">
                  <label>逾期工单:</label>
                  <span>{{ statistics.overdueOrders }}</span>
                </div>
              </div>
            </div>
            
            <div class="stats-section">
              <h3>性能指标</h3>
              <div class="stats-grid">
                <div class="stat-item">
                  <label>按时完成率:</label>
                  <span>{{ statistics.onTimeCompletionRate.toFixed(1) }}%</span>
                </div>
                <div class="stat-item">
                  <label>平均处理时间:</label>
                  <span>{{ statistics.averageProcessingTime.toFixed(1) }} 小时</span>
                </div>
              </div>
            </div>
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

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 15px;
}

.stat-card {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.recent-orders {
  margin-top: 30px;
}

.order-list {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  padding: 15px;
}

.order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #e0e0e0;
}

.order-item:last-child {
  border-bottom: none;
}

.order-code {
  font-family: 'Courier New', monospace;
  font-weight: bold;
  color: #007bff;
}

.order-title {
  flex: 1;
  margin: 0 10px;
  color: #333;
}

.order-status {
  padding: 5px 10px;
  border-radius: 15px;
  color: white;
  font-size: 12px;
  font-weight: bold;
}

.user-management {
  background: white;
  padding: 25px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.user-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.user-card {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-info {
  flex: 1;
  margin-right: 10px;
}

.user-status {
  min-width: 80px;
  text-align: center;
}

.status-indicator {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 5px;
}

.enabled {
  background: #28a745;
}

.disabled {
  background: #dc3545;
}

.approval-center {
  background: white;
  padding: 25px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.pending-approvals {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.approval-card {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.approval-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.priority {
  padding: 3px 8px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: bold;
}

.approval-body {
  margin-top: 10px;
}

.statistics {
  background: white;
  padding: 25px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.detailed-stats {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-section {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 15px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-radius: 5px;
  background: #fff;
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
}

.stat-item label {
  font-weight: 600;
  color: #333;
}

.stat-item span {
  font-size: 14px;
  color: #666;
}

.user-count {
  background: #e9ecef;
  padding: 5px 10px;
  border-radius: 15px;
  font-size: 14px;
  color: #666;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal {
  background: white;
  padding: 30px;
  border-radius: 15px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  max-width: 400px;
  width: 90%;
}

.modal-header {
  text-align: center;
  margin-bottom: 25px;
}

.modal-header h2 {
  margin: 0;
  color: #333;
  font-size: 24px;
}

.modal-body {
  display: flex;
  flex-direction: column;
}

.login-help {
  margin-top: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.4;
}

.login-help p {
  margin: 5px 0;
}

.btn-block {
  width: 100%;
  margin-top: 15px;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-warning {
  background: #ffc107;
  color: #212529;
}

.user-info {
  margin-right: 15px;
  font-weight: 500;
  color: white;
}

.nav-tabs {
  display: flex;
  margin-bottom: 30px;
  border-bottom: 2px solid #e0e0e0;
  background: white;
  border-radius: 10px 10px 0 0;
  overflow: hidden;
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
}

.nav-tab {
  flex: 1;
  padding: 15px 20px;
  border: none;
  background: #f8f9fa;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  border-bottom: 3px solid transparent;
}

.nav-tab:hover {
  background: #e9ecef;
  color: #333;
}

.nav-tab.active {
  background: white;
  color: #007bff;
  border-bottom-color: #007bff;
  font-weight: 600;
}

.tab-content {
  background: white;
  padding: 30px;
  border-radius: 0 0 10px 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  min-height: 500px;
}

.dashboard {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.dashboard h2 {
  margin: 0;
  color: #333;
  font-size: 28px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.workorder-details {
  margin-bottom: 15px;
}

.detail-row {
  display: flex;
  gap: 15px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.description {
  margin-top: 15px;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 5px;
  border-left: 4px solid #007bff;
}

.actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.assign-select {
  width: auto;
  min-width: 150px;
}

.approval-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.priority.high {
  background: #dc3545;
  color: white;
}

.priority.medium {
  background: #ffc107;
  color: #212529;
}

.priority.low {
  background: #28a745;
  color: white;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-title h4 {
  margin: 0;
  color: #333;
}

/* Responsive Design */
@media (max-width: 768px) {
  .header {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }
  
  .nav-tabs {
    flex-direction: column;
  }
  
  .nav-tab {
    border-bottom: 1px solid #e0e0e0;
    border-right: none;
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
  
  .stats-grid {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  }
  
  .user-list {
    grid-template-columns: 1fr;
  }
  
  .actions {
    flex-direction: column;
    align-items: stretch;
  }
  
  .detail-row {
    flex-direction: column;
    gap: 5px;
  }
}
</style>
