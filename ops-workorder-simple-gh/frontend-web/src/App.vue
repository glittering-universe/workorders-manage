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
  submittedOrders?: number
  approvedOrders?: number
  rejectedOrders?: number
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

// 审批相关状态
const pendingApprovals = ref<WorkOrder[]>([])

// 过滤状态
const statusFilter = ref<string>('')
const isOverdueFilter = ref(false)

// 登录表单
const loginForm = reactive({
  username: '',
  password: ''
})

// 新用户表单
const newUser = reactive({
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  role: 'USER',
  department: 'IT_DEPT',
  organizationLevel: '市级'
})

// 控制创建用户表单显示
const showCreateUserForm = ref(false)

// 分派中心
const showDispatchCenter = ref(false)
const selectedWorkOrderIds = ref<number[]>([])
const dispatchAssigneeId = ref<number | null>(null)

// 工单详情
const selectedWorkOrder = ref<WorkOrder | null>(null)
const showWorkOrderDetail = ref(false)

const viewWorkOrderDetail = (workOrder: WorkOrder) => {
  selectedWorkOrder.value = workOrder
  showWorkOrderDetail.value = true
}

const closeWorkOrderDetail = () => {
  selectedWorkOrder.value = null
  showWorkOrderDetail.value = false
}

// 分派中心功能
const openDispatchCenter = () => {
  showDispatchCenter.value = true
  selectedWorkOrderIds.value = []
  dispatchAssigneeId.value = null
}

const closeDispatchCenter = () => {
  showDispatchCenter.value = false
  selectedWorkOrderIds.value = []
  dispatchAssigneeId.value = null
}

const toggleWorkOrderSelection = (workOrderId: number) => {
  const index = selectedWorkOrderIds.value.indexOf(workOrderId)
  if (index > -1) {
    selectedWorkOrderIds.value.splice(index, 1)
  } else {
    selectedWorkOrderIds.value.push(workOrderId)
  }
}

const executeDispatch = async () => {
  if (selectedWorkOrderIds.value.length === 0 || !dispatchAssigneeId.value) {
    alert('请选择工单和分派对象')
    return
  }
  
  await batchAssignWorkOrders(selectedWorkOrderIds.value, dispatchAssigneeId.value)
  closeDispatchCenter()
}

// 编辑草稿工单
const editingWorkOrder = ref<WorkOrder | null>(null)
const showEditForm = ref(false)

const editWorkOrder = (workOrder: WorkOrder) => {
  editingWorkOrder.value = { ...workOrder }
  showEditForm.value = true
}

const updateWorkOrder = async () => {
  if (!editingWorkOrder.value) return
  
  loading.value = true
  try {
    const payload = {
      title: editingWorkOrder.value.title,
      description: editingWorkOrder.value.description,
      category: editingWorkOrder.value.category,
      priority: editingWorkOrder.value.priority,
      slaMinutes: editingWorkOrder.value.slaMinutes
    }
    
    const response = await axios.put(`${apiBase}/orders/${editingWorkOrder.value.id}`, payload)
    const index = workOrders.value.findIndex(wo => wo.id === editingWorkOrder.value!.id)
    if (index !== -1) {
      workOrders.value[index] = response.data
    }
    
    showEditForm.value = false
    editingWorkOrder.value = null
    alert('工单更新成功！')
  } catch (error) {
    console.error('更新工单失败:', error)
    alert('更新工单失败')
  } finally {
    loading.value = false
  }
}

const cancelEdit = () => {
  showEditForm.value = false
  editingWorkOrder.value = null
}

// 仪表板点击过滤
const filterByStatus = (status: string) => {
  activeTab.value = 'workorders'
  statusFilter.value = status
}

const filterOverdue = () => {
  activeTab.value = 'workorders'
  isOverdueFilter.value = true
  statusFilter.value = ''
}

// Remove duplicate - using the role-based filteredWorkOrders below

// 清除过滤器
const clearFilters = () => {
  statusFilter.value = ''
  isOverdueFilter.value = false
}

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
    loadStatistics(),
    loadPendingApprovals()
  ])
}

const loadPendingApprovals = async () => {
  if (!currentUser.value || !['ADMIN', 'DEPT_MANAGER', 'APPROVER'].includes(currentUser.value.role)) {
    return
  }
  
  try {
    const response = await axios.get(`${apiBase}/orders/pending-approvals?userId=${currentUser.value.id}`)
    pendingApprovals.value = response.data
  } catch (error) {
    console.error('加载待审批工单失败:', error)
  }
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

// 新工单表单
const newWorkOrder = reactive<WorkOrder>({
  title: '',
  description: '',
  category: 'MAINTENANCE',
  priority: 'MEDIUM',
  slaMinutes: 240
})

const createUser = async () => {
  loading.value = true
  try {
    const userData = {
      ...newUser
    }
    const response = await axios.post(`${apiBase}/users`, userData)
    users.value.push(response.data)
    
    // 重置表单
    Object.assign(newUser, {
      username: '',
      password: '',
      realName: '',
      email: '',
      phone: '',
      role: 'USER',
      department: 'IT_DEPT',
      organizationLevel: '市级'
    })
    showCreateUserForm.value = false
    alert('用户创建成功！')
  } catch (error) {
    console.error('创建用户失败:', error)
    alert('创建用户失败，请检查输入或联系管理员')
  } finally {
    loading.value = false
  }
}

const createWorkOrder = async () => {
  loading.value = true
  try {
    const payload = {
      ...newWorkOrder
    }
    const response = await axios.post(`${apiBase}/orders?creatorId=${currentUser.value?.id}`, payload)
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
    const response = await axios.post(`${apiBase}/orders/${id}/submit?userId=${currentUser.value?.id}`)
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
      comment: '审批通过'
    }
    const response = await axios.post(`${apiBase}/orders/${id}/approve?approverId=${currentUser.value?.id}`, payload)
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
    const response = await axios.post(`${apiBase}/orders/${id}/assign?assigneeId=${operatorId}&assignerId=${currentUser.value?.id}`)
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
    const payload = {
      comment: '工单已完成'
    }
    const response = await axios.post(`${apiBase}/orders/${id}/complete?operatorId=${currentUser.value?.id}`, payload)
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

// 批量分派工单
const batchAssignWorkOrders = async (workOrderIds: number[], assigneeId: number) => {
  loading.value = true
  try {
    const promises = workOrderIds.map(id => 
      axios.post(`${apiBase}/orders/${id}/assign?assigneeId=${assigneeId}&assignerId=${currentUser.value?.id}`)
    )
    const responses = await Promise.all(promises)
    
    // 更新本地工单数据
    responses.forEach((response, index) => {
      const workOrderIndex = workOrders.value.findIndex(wo => wo.id === workOrderIds[index])
      if (workOrderIndex !== -1) {
        workOrders.value[workOrderIndex] = response.data
      }
    })
    
    alert(`成功分派 ${workOrderIds.length} 个工单！`)
  } catch (error) {
    console.error('批量分派工单失败:', error)
    alert('批量分派工单失败')
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
  
  // 首先基于角色过滤工单
  let roleFilteredOrders: WorkOrder[] = []
  switch (userRole) {
    case 'ADMIN':
      roleFilteredOrders = workOrders.value
      break
    case 'DEPT_MANAGER':
    case 'APPROVER':
      roleFilteredOrders = workOrders.value.filter(wo => 
        wo.creatorId === userId || 
        wo.approverId === userId ||
        wo.status === 'SUBMITTED' ||
        wo.status === 'APPROVING'
      )
      break
    case 'OPERATOR':
      roleFilteredOrders = workOrders.value.filter(wo => 
        wo.assigneeId === userId || 
        wo.status === 'APPROVED' ||
        wo.status === 'ASSIGNED'
      )
      break
    case 'USER':
    default:
      roleFilteredOrders = workOrders.value.filter(wo => wo.creatorId === userId)
      break
  }
  
  // 然后应用状态和逾期过滤
  let filtered = roleFilteredOrders
  
  // 状态过滤
  if (statusFilter.value) {
    if (statusFilter.value === 'PROCESSING') {
      filtered = filtered.filter(wo => 
        ['SUBMITTED', 'APPROVING', 'APPROVED', 'ASSIGNED', 'IN_PROGRESS'].includes(wo.status || '')
      )
    } else {
      filtered = filtered.filter(wo => wo.status === statusFilter.value)
    }
  }
  
  // 逾期过滤
  if (isOverdueFilter.value) {
    const now = new Date()
    filtered = filtered.filter(wo => 
      wo.deadline && new Date(wo.deadline) < now && wo.status !== 'COMPLETED'
    )
  }
  
  return filtered
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
            {{ currentUser?.realName }} ({{ currentUser ? getRoleText(currentUser.role) : '' }})
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
          v-if="currentUser && ['ADMIN', 'DEPT_MANAGER'].includes(currentUser.role)"
          :class="['nav-tab', { active: activeTab === 'users' }]"
          @click="activeTab = 'users'"
        >
          👥 用户管理
        </button>
        <button 
          v-if="currentUser && ['ADMIN', 'DEPT_MANAGER', 'APPROVER'].includes(currentUser.role)"
          :class="['nav-tab', { active: activeTab === 'approval' }]"
          @click="activeTab = 'approval'"
        >
          ✅ 审批中心
        </button>
        <button 
          v-if="currentUser && ['ADMIN', 'DEPT_MANAGER'].includes(currentUser.role)"
          :class="['nav-tab', { active: activeTab === 'dispatch' }]"
          @click="activeTab = 'dispatch'"
        >
          🚀 分派中心
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
            <div class="stat-card clickable" @click="activeTab = 'workorders'">
              <div class="stat-number">{{ statistics.totalOrders }}</div>
              <div class="stat-label">总工单数</div>
            </div>
            <div class="stat-card clickable" @click="filterByStatus('COMPLETED')">
              <div class="stat-number">{{ statistics.completedOrders }}</div>
              <div class="stat-label">已完成工单</div>
            </div>
            <div class="stat-card clickable" @click="filterByStatus('PROCESSING')">
              <div class="stat-number">{{ (statistics.submittedOrders || 0) + (statistics.approvedOrders || 0) + (statistics.rejectedOrders || 0) }}</div>
              <div class="stat-label">处理中工单</div>
            </div>
            <div class="stat-card clickable warning" @click="filterOverdue()">
              <div class="stat-number">{{ statistics.overdueOrders }}</div>
              <div class="stat-label">逾期工单</div>
            </div>
            <div class="stat-card success">
              <div class="stat-number">{{ statistics.onTimeCompletionRate.toFixed(1) }}%</div>
              <div class="stat-label">按时完成率</div>
            </div>
            <div class="stat-card info">
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

          <!-- 过滤器状态栏 -->
          <div v-if="statusFilter || isOverdueFilter" class="filter-bar">
            <div class="active-filters">
              <span class="filter-label">当前过滤:</span>
              <span v-if="statusFilter" class="filter-tag">
                状态: {{ statusFilter === 'PROCESSING' ? '处理中' : getStatusText(statusFilter) }}
                <button @click="statusFilter = ''" class="filter-remove">&times;</button>
              </span>
              <span v-if="isOverdueFilter" class="filter-tag">
                逾期工单
                <button @click="isOverdueFilter = false" class="filter-remove">&times;</button>
              </span>
              <button @click="clearFilters" class="btn btn-sm btn-secondary">清空所有过滤器</button>
            </div>
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
            
            <div v-for="workOrder in filteredWorkOrders" :key="workOrder.id" 
                 class="workorder-card clickable-card" 
                 @click="viewWorkOrderDetail(workOrder)">
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
                
                <div class="actions" @click.stop>
                  <!-- 编辑草稿工单 -->
                  <button 
                    v-if="workOrder.status === 'DRAFT' && currentUser && workOrder.creatorId === currentUser.id"
                    @click="editWorkOrder(workOrder)" 
                    :disabled="loading"
                    class="btn btn-secondary btn-sm"
                  >
                    编辑
                  </button>
                  
                  <!-- 提交工单 -->
                  <button 
                    v-if="workOrder.status === 'DRAFT' && currentUser && workOrder.creatorId === currentUser.id"
                    @click="submitWorkOrder(workOrder.id!)" 
                    :disabled="loading"
                    class="btn btn-primary btn-sm"
                  >
                    提交工单
                  </button>
                  
                  <!-- 审批工单 -->
                  <button 
                    v-if="workOrder.status === 'SUBMITTED' && currentUser && ['ADMIN', 'DEPT_MANAGER', 'APPROVER'].includes(currentUser.role)"
                    @click="approveWorkOrder(workOrder.id!)" 
                    :disabled="loading"
                    class="btn btn-success btn-sm"
                  >
                    审批通过
                  </button>
                  
                  <!-- 分派工单 -->
                  <select 
                    v-if="workOrder.status === 'APPROVED' && currentUser && ['ADMIN', 'DEPT_MANAGER'].includes(currentUser.role)"
                    @change="assignWorkOrder(workOrder.id!, parseInt(($event.target as HTMLSelectElement).value))"
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
                           currentUser && (workOrder.assigneeId === currentUser.id || ['ADMIN'].includes(currentUser.role))"
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
            <div class="header-actions">
              <span class="user-count">共 {{ users.length }} 个用户</span>
              <button 
                v-if="currentUser && ['ADMIN', 'DEPT_MANAGER'].includes(currentUser.role)"
                @click="showCreateUserForm = true" 
                class="btn btn-primary btn-sm"
              >
                创建用户
              </button>
            </div>
          </div>

          <!-- 创建用户表单 -->
          <div v-if="showCreateUserForm" class="create-form">
            <h3>创建新用户</h3>
            <form @submit.prevent="createUser">
              <div class="form-row">
                <div class="form-group">
                  <label>用户名:</label>
                  <input 
                    v-model="newUser.username" 
                    type="text" 
                    required 
                    placeholder="请输入用户名"
                    class="form-control"
                  />
                </div>
                <div class="form-group">
                  <label>密码:</label>
                  <input 
                    v-model="newUser.password" 
                    type="password" 
                    required 
                    placeholder="请输入密码"
                    class="form-control"
                  />
                </div>
              </div>
              
              <div class="form-row">
                <div class="form-group">
                  <label>真实姓名:</label>
                  <input 
                    v-model="newUser.realName" 
                    type="text" 
                    required 
                    placeholder="请输入真实姓名"
                    class="form-control"
                  />
                </div>
                <div class="form-group">
                  <label>邮箱:</label>
                  <input 
                    v-model="newUser.email" 
                    type="email" 
                    placeholder="请输入邮箱"
                    class="form-control"
                  />
                </div>
              </div>
              
              <div class="form-row">
                <div class="form-group">
                  <label>电话:</label>
                  <input 
                    v-model="newUser.phone" 
                    type="tel" 
                    placeholder="请输入电话号码"
                    class="form-control"
                  />
                </div>
                <div class="form-group">
                  <label>角色:</label>
                  <select v-model="newUser.role" class="form-control">
                    <option value="USER">普通用户</option>
                    <option value="OPERATOR">运维人员</option>
                    <option value="APPROVER" v-if="currentUser && currentUser.role === 'ADMIN'">审批员</option>
                    <option value="DEPT_MANAGER" v-if="currentUser && currentUser.role === 'ADMIN'">部门经理</option>
                    <option value="ADMIN" v-if="currentUser && currentUser.role === 'ADMIN'">系统管理员</option>
                  </select>
                </div>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label>部门:</label>
                  <select v-model="newUser.department" class="form-control">
                    <option value="IT_DEPT">IT部门</option>
                    <option value="NETWORK_DEPT">网络部门</option>
                    <option value="SECURITY_DEPT">安全部门</option>
                    <option value="MAINTENANCE">维护部门</option>
                    <option value="BUSINESS_DEPT">业务部门</option>
                  </select>
                </div>
                <div class="form-group">
                  <label>组织级别:</label>
                  <select v-model="newUser.organizationLevel" class="form-control">
                    <option value="省级">省级</option>
                    <option value="市级">市级</option>
                    <option value="区县级">区县级</option>
                  </select>
                </div>
              </div>
              
              <div class="form-actions">
                <button type="submit" :disabled="loading" class="btn btn-success">
                  {{ loading ? '创建中...' : '创建用户' }}
                </button>
                <button type="button" @click="showCreateUserForm = false" class="btn btn-secondary">
                  取消
                </button>
              </div>
            </form>
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
                    v-if="currentUser && ['ADMIN', 'DEPT_MANAGER', 'APPROVER'].includes(currentUser.role)"
                    @click="approveWorkOrder(order.id!)" 
                    :disabled="loading"
                    class="btn btn-success btn-sm"
                  >
                    批准
                  </button>
                  <button 
                    v-if="currentUser && ['ADMIN', 'DEPT_MANAGER', 'APPROVER'].includes(currentUser.role)"
                    class="btn btn-danger btn-sm"
                  >
                    拒绝
                  </button>
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

      <!-- 分派中心 -->
      <div v-if="activeTab === 'dispatch'" class="tab-content">
        <div class="dispatch-center">
          <div class="section-header">
            <h2>🚀 分派中心</h2>
            <div class="dispatch-stats">
              <span class="stat-badge">
                待分派: {{ workOrders.filter(wo => wo.status === 'APPROVED').length }}
              </span>
              <span class="stat-badge">
                已分派: {{ workOrders.filter(wo => wo.status === 'ASSIGNED').length }}
              </span>
            </div>
          </div>

          <div class="dispatch-controls">
            <div class="bulk-actions">
              <select v-model="dispatchAssigneeId" class="form-control">
                <option value="">选择运维人员</option>
                <option 
                  v-for="user in users.filter(u => u.role === 'OPERATOR')" 
                  :key="user.id" 
                  :value="user.id"
                >
                  {{ user.realName }} ({{ getDepartmentText(user.department) }})
                </option>
              </select>
              <button 
                @click="executeDispatch" 
                :disabled="selectedWorkOrderIds.length === 0 || !dispatchAssigneeId || loading"
                class="btn btn-primary"
              >
                批量分派 ({{ selectedWorkOrderIds.length }})
              </button>
              <button 
                @click="selectedWorkOrderIds = []" 
                :disabled="selectedWorkOrderIds.length === 0"
                class="btn btn-secondary"
              >
                清空选择
              </button>
            </div>
            <div class="filter-info">
              <span v-if="selectedWorkOrderIds.length > 0" class="selection-info">
                已选择 {{ selectedWorkOrderIds.length }} 个工单
              </span>
            </div>
          </div>

          <div class="dispatch-workorders">
            <div v-if="workOrders.filter(wo => wo.status === 'APPROVED' || wo.status === 'ASSIGNED').length === 0" class="empty-state">
              <p>暂无需要分派的工单</p>
            </div>
            
            <div v-for="workOrder in workOrders.filter(wo => wo.status === 'APPROVED' || wo.status === 'ASSIGNED')" 
                 :key="workOrder.id" 
                 class="dispatch-card"
                 :class="{ selected: selectedWorkOrderIds.includes(workOrder.id!) }"
            >
              <div class="dispatch-header">
                <div class="selection-controls">
                  <input 
                    type="checkbox" 
                    :checked="selectedWorkOrderIds.includes(workOrder.id!)"
                    @change="toggleWorkOrderSelection(workOrder.id!)"
                    class="dispatch-checkbox"
                  />
                  <span class="wo-code">{{ workOrder.woCode }}</span>
                </div>
                <span 
                  class="status-badge" 
                  :style="{ backgroundColor: getStatusColor(workOrder.status || '') }"
                >
                  {{ getStatusText(workOrder.status || '') }}
                </span>
              </div>
              
              <div class="dispatch-body">
                <h4>{{ workOrder.title }}</h4>
                <div class="dispatch-details">
                  <div class="detail-row">
                    <span class="detail-item">分类: {{ getCategoryText(workOrder.category) }}</span>
                    <span class="detail-item">优先级: {{ getPriorityText(workOrder.priority) }}</span>
                    <span class="detail-item">SLA: {{ workOrder.slaMinutes }}分钟</span>
                  </div>
                  <div class="detail-row">
                    <span class="detail-item">创建人: {{ workOrder.creatorName || '未知' }}</span>
                    <span class="detail-item">
                      当前处理人: {{ workOrder.assigneeName || '未分派' }}
                    </span>
                  </div>
                  <div class="detail-row">
                    <span class="detail-item">创建时间: {{ workOrder.createdAt ? new Date(workOrder.createdAt).toLocaleString() : '未知' }}</span>
                  </div>
                </div>
                
                <div class="individual-actions">
                  <select 
                    @change="assignWorkOrder(workOrder.id!, parseInt(($event.target as HTMLSelectElement).value))"
                    class="form-control btn-sm assign-select"
                  >
                    <option value="">单独分派给...</option>
                    <option 
                      v-for="user in users.filter(u => u.role === 'OPERATOR')" 
                      :key="user.id" 
                      :value="user.id"
                    >
                      {{ user.realName }}
                    </option>
                  </select>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 工单详情模态框 -->
    <div v-if="showWorkOrderDetail && selectedWorkOrder" class="modal-overlay" @click="closeWorkOrderDetail">
      <div class="modal detail-modal" @click.stop>
        <div class="modal-header">
          <h2>📋 工单详情</h2>
          <button @click="closeWorkOrderDetail" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <div class="detail-section">
            <h3>基本信息</h3>
            <div class="detail-grid">
              <div class="detail-item">
                <label>工单编号:</label>
                <span>{{ selectedWorkOrder.woCode }}</span>
              </div>
              <div class="detail-item">
                <label>标题:</label>
                <span>{{ selectedWorkOrder.title }}</span>
              </div>
              <div class="detail-item">
                <label>状态:</label>
                <span class="status-badge" :style="{ backgroundColor: getStatusColor(selectedWorkOrder.status || '') }">
                  {{ getStatusText(selectedWorkOrder.status || '') }}
                </span>
              </div>
              <div class="detail-item">
                <label>分类:</label>
                <span>{{ getCategoryText(selectedWorkOrder.category) }}</span>
              </div>
              <div class="detail-item">
                <label>优先级:</label>
                <span>{{ getPriorityText(selectedWorkOrder.priority) }}</span>
              </div>
              <div class="detail-item">
                <label>SLA时间:</label>
                <span>{{ selectedWorkOrder.slaMinutes }}分钟</span>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <h3>人员信息</h3>
            <div class="detail-grid">
              <div class="detail-item">
                <label>创建人:</label>
                <span>{{ selectedWorkOrder.creatorName || '未知' }}</span>
              </div>
              <div class="detail-item">
                <label>审批人:</label>
                <span>{{ selectedWorkOrder.approverName || '待审批' }}</span>
              </div>
              <div class="detail-item">
                <label>处理人:</label>
                <span>{{ selectedWorkOrder.assigneeName || '未分派' }}</span>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <h3>时间信息</h3>
            <div class="detail-grid">
              <div class="detail-item">
                <label>创建时间:</label>
                <span>{{ selectedWorkOrder.createdAt ? new Date(selectedWorkOrder.createdAt).toLocaleString() : '未知' }}</span>
              </div>
              <div class="detail-item">
                <label>更新时间:</label>
                <span>{{ selectedWorkOrder.updatedAt ? new Date(selectedWorkOrder.updatedAt).toLocaleString() : '未知' }}</span>
              </div>
              <div class="detail-item">
                <label>截止时间:</label>
                <span>{{ selectedWorkOrder.deadline ? new Date(selectedWorkOrder.deadline).toLocaleString() : '未设置' }}</span>
              </div>
              <div class="detail-item">
                <label>完成时间:</label>
                <span>{{ selectedWorkOrder.completedAt ? new Date(selectedWorkOrder.completedAt).toLocaleString() : '未完成' }}</span>
              </div>
            </div>
          </div>

          <div v-if="selectedWorkOrder.description" class="detail-section">
            <h3>详细描述</h3>
            <div class="description-content">
              {{ selectedWorkOrder.description }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑工单模态框 -->
    <div v-if="showEditForm && editingWorkOrder" class="modal-overlay" @click="cancelEdit">
      <div class="modal edit-modal" @click.stop>
        <div class="modal-header">
          <h2>✏️ 编辑工单</h2>
          <button @click="cancelEdit" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="updateWorkOrder">
            <div class="form-group">
              <label>工单标题:</label>
              <input 
                v-model="editingWorkOrder.title" 
                type="text" 
                required 
                class="form-control"
              />
            </div>
            
            <div class="form-row">
              <div class="form-group">
                <label>分类:</label>
                <select v-model="editingWorkOrder.category" class="form-control">
                  <option value="MAINTENANCE">维护</option>
                  <option value="INCIDENT">故障</option>
                  <option value="REQUEST">请求</option>
                  <option value="CHANGE">变更</option>
                  <option value="EMERGENCY">紧急</option>
                </select>
              </div>
              <div class="form-group">
                <label>优先级:</label>
                <select v-model="editingWorkOrder.priority" class="form-control">
                  <option value="HIGH">高</option>
                  <option value="MEDIUM">中</option>
                  <option value="LOW">低</option>
                </select>
              </div>
            </div>
            
            <div class="form-group">
              <label>SLA时间 (分钟):</label>
              <input 
                v-model.number="editingWorkOrder.slaMinutes" 
                type="number" 
                min="1" 
                required 
                class="form-control"
              />
            </div>
            
            <div class="form-group">
              <label>详细描述:</label>
              <textarea 
                v-model="editingWorkOrder.description" 
                rows="4" 
                class="form-control"
              ></textarea>
            </div>
            
            <div class="form-actions">
              <button type="submit" :disabled="loading" class="btn btn-success">
                {{ loading ? '更新中...' : '保存更改' }}
              </button>
              <button type="button" @click="cancelEdit" class="btn btn-secondary">
                取消
              </button>
            </div>
          </form>
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
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.stat-card.clickable {
  cursor: pointer;
}

.stat-card.clickable:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(0,0,0,0.15);
  border-color: #007bff;
}

.stat-card.success {
  background: linear-gradient(135deg, #d4edda 0%, #c3e6cb 100%);
  border-color: #28a745;
}

.stat-card.warning {
  background: linear-gradient(135deg, #fff3cd 0%, #ffeeba 100%);
  border-color: #ffc107;
}

.stat-card.info {
  background: linear-gradient(135deg, #d1ecf1 0%, #bee5eb 100%);
  border-color: #17a2b8;
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
  background: #007bff;
  padding: 5px 10px;
  border-radius: 15px;
  font-size: 14px;
  color: white;
  font-weight: 600;
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
  backdrop-filter: blur(4px);
  animation: fadeIn 0.3s ease-out;
}

.modal {
  background: white;
  padding: 30px;
  border-radius: 15px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  max-width: 500px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  animation: slideIn 0.3s ease-out;
}

.detail-modal {
  max-width: 700px;
}

.edit-modal {
  max-width: 600px;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideIn {
  from { 
    opacity: 0;
    transform: translateY(-20px) scale(0.95);
  }
  to { 
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.modal-header h2 {
  margin: 0;
  color: #333;
  font-size: 24px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 30px;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.close-btn:hover {
  background: #f0f0f0;
  color: #333;
  transform: rotate(90deg);
}

.detail-section {
  margin-bottom: 25px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 10px;
  border-left: 4px solid #007bff;
}

.detail-section h3 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 18px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.detail-item label {
  font-weight: 600;
  color: #666;
  font-size: 14px;
}

.detail-item span {
  color: #333;
  font-size: 14px;
}

.description-content {
  background: white;
  padding: 15px;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  line-height: 1.6;
  color: #333;
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
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

/* Dispatch Center Styles */
.dispatch-center {
  background: white;
  padding: 25px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.dispatch-stats {
  display: flex;
  gap: 15px;
  align-items: center;
}

.stat-badge {
  background: #f8f9fa;
  padding: 8px 15px;
  border-radius: 15px;
  font-size: 14px;
  color: #666;
  border: 1px solid #e0e0e0;
}

.dispatch-controls {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 10px;
  margin: 20px 0;
  border: 1px solid #e0e0e0;
}

.bulk-actions {
  display: flex;
  gap: 15px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-info {
  margin-top: 10px;
}

.selection-info {
  background: #007bff;
  color: white;
  padding: 5px 10px;
  border-radius: 15px;
  font-size: 14px;
  font-weight: 500;
}

.dispatch-workorders {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.dispatch-card {
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  padding: 20px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.dispatch-card:hover {
  border-color: #007bff;
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
}

.dispatch-card.selected {
  border-color: #007bff;
  background: #f8f9ff;
  box-shadow: 0 4px 15px rgba(0,123,255,0.2);
}

.dispatch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.selection-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dispatch-checkbox {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.dispatch-body h4 {
  margin: 0 0 15px 0;
  color: #333;
}

.dispatch-details {
  margin-bottom: 15px;
}

.individual-actions {
  display: flex;
  justify-content: flex-end;
}

/* Clickable Card Styles */
.clickable-card {
  cursor: pointer;
  position: relative;
}

.clickable-card::before {
  content: '';
  position: absolute;
  top: 5px;
  right: 5px;
  width: 20px;
  height: 20px;
  background: rgba(0, 123, 255, 0.1);
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.clickable-card:hover::before {
  opacity: 1;
}

.clickable-card:hover {
  border-color: #007bff;
}

.clickable-card .actions {
  position: relative;
  z-index: 2;
}

/* Filter and Status Improvements */
.filter-bar {
  background: #e8f4f8;
  padding: 15px 20px;
  border-radius: 8px;
  margin: 20px 0;
  border-left: 4px solid #007bff;
}

.active-filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-label {
  font-weight: 600;
  color: #666;
}

.filter-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #007bff;
  color: white;
  padding: 5px 10px;
  border-radius: 15px;
  font-size: 14px;
}

.filter-remove {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 16px;
  padding: 0;
  margin-left: 5px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.3s ease;
}

.filter-remove:hover {
  background: rgba(255, 255, 255, 0.2);
}

.stat-card.clickable:hover .stat-number {
  color: #007bff;
}

.stat-card.clickable:hover .stat-label {
  color: #333;
}

@media (max-width: 768px) {
  .bulk-actions {
    flex-direction: column;
    align-items: stretch;
  }
  
  .bulk-actions select,
  .bulk-actions button {
    width: 100%;
  }
  
  .dispatch-stats {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .selection-controls {
    flex-wrap: wrap;
  }
}
</style>
