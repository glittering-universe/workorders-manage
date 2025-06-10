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

// 搜索和筛选状态
const searchQuery = ref<string>('')
const priorityFilter = ref<string>('')
const categoryFilter = ref<string>('')
const creatorFilter = ref<string>('')

// 通知系统
const notifications = ref<Array<{
  id: number
  type: 'success' | 'error' | 'warning' | 'info'
  title: string
  message: string
  timestamp: Date
}>>([])

// 实时刷新状态
const autoRefresh = ref(true)
const refreshInterval = ref<number | null>(null)

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

// 用户详情
const selectedUser = ref<User | null>(null)
const showUserDetail = ref(false)

const showUserDetailModal = (user: User) => {
  selectedUser.value = user
  showUserDetail.value = true
}

const closeUserDetail = () => {
  selectedUser.value = null
  showUserDetail.value = false
}

// 详细统计视图
const showDetailedStats = ref(false)
const selectedStatType = ref('')

const openDetailedStats = (statType: string) => {
  selectedStatType.value = statType
  showDetailedStats.value = true
}

const closeDetailedStats = () => {
  showDetailedStats.value = false
  selectedStatType.value = ''
}

// 效率等级评定辅助函数
const getEfficiencyGrade = (avgProcessingTime: number): string => {
  if (avgProcessingTime <= 2) return 'A+'
  if (avgProcessingTime <= 4) return 'A'
  if (avgProcessingTime <= 8) return 'B'
  if (avgProcessingTime <= 16) return 'C'
  return 'D'
}

const getEfficiencyGradeText = (grade: string): string => {
  const gradeTexts = {
    'A+': '优秀',
    'A': '良好',
    'B': '一般',
    'C': '待改进',
    'D': '需优化'
  }
  return gradeTexts[grade] || '未知'
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
    showNotification('warning', '批量分派', '请选择工单和分派对象')
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
  searchQuery.value = ''
  priorityFilter.value = ''
  categoryFilter.value = ''
  creatorFilter.value = ''
}

const apiBase = 'http://localhost:8080/api'

// 通知系统函数
let notificationId = 0
const showNotification = (type: 'success' | 'error' | 'warning' | 'info', title: string, message: string) => {
  const id = ++notificationId
  const notification = {
    id,
    type,
    title,
    message,
    timestamp: new Date()
  }
  notifications.value.push(notification)
  
  // 自动移除通知
  setTimeout(() => {
    removeNotification(id)
  }, type === 'error' ? 5000 : 3000)
}

const removeNotification = (id: number) => {
  const index = notifications.value.findIndex(n => n.id === id)
  if (index > -1) {
    notifications.value.splice(index, 1)
  }
}

// 实时刷新功能
const startAutoRefresh = () => {
  if (refreshInterval.value) return
  
  refreshInterval.value = setInterval(async () => {
    if (currentUser.value && !loading.value) {
      await loadData()
    }
  }, 30000) // 每30秒刷新一次
}

const stopAutoRefresh = () => {
  if (refreshInterval.value) {
    clearInterval(refreshInterval.value)
    refreshInterval.value = null
  }
}

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
      showNotification('success', '登录成功', `欢迎回来，${response.data.realName}！`)
      startAutoRefresh()
    } else {
      showNotification('error', '登录失败', '用户名或密码错误')
    }
  } catch (error) {
    console.error('登录失败:', error)
    showNotification('error', '登录失败', '请检查用户名和密码')
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
  // 停止自动刷新
  stopAutoRefresh()
  showNotification('info', '已退出', '您已安全退出系统')
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
    if (currentUser.value.role === 'ADMIN' || currentUser.value.role === 'DEPT_MANAGER') {
      // Admin和部门经理用户可以看到所有待审批工单
      const response = await axios.get(`${apiBase}/orders`)
      pendingApprovals.value = response.data.filter((wo: WorkOrder) => wo.status === 'SUBMITTED')
    } else {
      // 其他用户只能看到分配给自己的待审批工单
      const response = await axios.get(`${apiBase}/orders/pending-approvals?approverId=${currentUser.value.id}`)
      pendingApprovals.value = response.data
    }
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
  if (!currentUser.value) return
  
  try {
    // 构建基于角色的统计查询参数
    let statisticsUrl = `${apiBase}/statistics/overall`
    const params = new URLSearchParams()
    
    // 根据用户角色和权限进行数据过滤
    const userRole = currentUser.value.role
    const userDept = currentUser.value.department
    const userLevel = currentUser.value.organizationLevel
    
    // 不是管理员的用户只能看到自己部门或级别的数据
    if (userRole !== 'ADMIN') {
      // 部门经理可以看到自己部门的数据
      if (userRole === 'DEPT_MANAGER') {
        params.append('department', userDept)
      }
      // 其他角色只能看到自己组织级别的数据
      else if (userRole === 'APPROVER' || userRole === 'OPERATOR' || userRole === 'USER') {
        params.append('organizationLevel', userLevel)
      }
    }
    
    if (params.toString()) {
      statisticsUrl += '?' + params.toString()
    }
    
    const response = await axios.get(statisticsUrl)
    statistics.value = response.data
  } catch (error) {
    console.error('加载统计数据失败:', error)
    // 如果总体统计失败，回退到今日统计
    try {
      const today = new Date().toISOString().split('T')[0]
      let fallbackUrl = `${apiBase}/statistics/daily?date=${today}`
      
      // 应用相同的角色过滤逻辑
      const params = new URLSearchParams([['date', today]])
      const userRole = currentUser.value?.role
      const userDept = currentUser.value?.department
      const userLevel = currentUser.value?.organizationLevel
      
      if (userRole !== 'ADMIN') {
        if (userRole === 'DEPT_MANAGER') {
          params.append('department', userDept)
        } else if (userRole === 'APPROVER' || userRole === 'OPERATOR' || userRole === 'USER') {
          params.append('organizationLevel', userLevel)
        }
      }
      
      fallbackUrl = `${apiBase}/statistics/daily?${params.toString()}`
      const fallbackResponse = await axios.get(fallbackUrl)
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
    showNotification('success', '创建成功', '用户创建成功！')
  } catch (error) {
    console.error('创建用户失败:', error)
    showNotification('error', '创建失败', '创建用户失败，请检查输入或联系管理员')
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
    showNotification('success', '创建成功', '工单创建成功！')
  } catch (error) {
    console.error('创建工单失败:', error)
    showNotification('error', '创建失败', '创建工单失败，请检查后端服务')
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
    showNotification('success', '提交成功', '工单提交成功！')
  } catch (error) {
    console.error('提交工单失败:', error)
    showNotification('error', '提交失败', '提交工单失败')
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
    // 重新加载待审批工单列表和所有工单
    await loadPendingApprovals()
    await loadWorkOrders()
    showNotification('success', '审批成功', '工单审批成功！')
  } catch (error) {
    console.error('审批工单失败:', error)
    showNotification('error', '审批失败', '审批工单失败')
  } finally {
    loading.value = false
  }
}

const rejectWorkOrder = async (id: number) => {
  loading.value = true
  try {
    const payload = {
      comment: '审批拒绝'
    }
    const response = await axios.post(`${apiBase}/orders/${id}/reject?approverId=${currentUser.value?.id}`, payload)
    const index = workOrders.value.findIndex(wo => wo.id === id)
    if (index !== -1) {
      workOrders.value[index] = response.data
    }
    // 重新加载待审批工单列表和所有工单
    await loadPendingApprovals()
    await loadWorkOrders()
    showNotification('warning', '已拒绝', '工单已拒绝！')
  } catch (error) {
    console.error('拒绝工单失败:', error)
    showNotification('error', '拒绝失败', '拒绝工单失败')
  } finally {
    loading.value = false
  }
}

const assignWorkOrder = async (id: number, assigneeId: number) => {
  loading.value = true
  try {
    const response = await axios.post(`${apiBase}/orders/${id}/assign?assigneeId=${assigneeId}&assignerId=${currentUser.value?.id}`)
    const index = workOrders.value.findIndex(wo => wo.id === id)
    if (index !== -1) {
      workOrders.value[index] = response.data
    }
    showNotification('success', '分派成功', '工单分派成功！')
  } catch (error) {
    console.error('分派工单失败:', error)
    showNotification('error', '分派失败', '分派工单失败')
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
    showNotification('success', '完成成功', '工单已完成！')
  } catch (error) {
    console.error('完成工单失败:', error)
    showNotification('error', '完成失败', '完成工单失败')
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
    
    showNotification('success', '批量分派成功', `成功分派 ${workOrderIds.length} 个工单！`)
  } catch (error) {
    console.error('批量分派工单失败:', error)
    showNotification('error', '批量分派失败', '批量分派工单失败')
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
  
  // 然后应用搜索和高级筛选
  let filtered = roleFilteredOrders
  
  // 搜索过滤 - 标题、描述、工单编号
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase().trim()
    filtered = filtered.filter(wo => 
      wo.title.toLowerCase().includes(query) ||
      (wo.description && wo.description.toLowerCase().includes(query)) ||
      (wo.woCode && wo.woCode.toLowerCase().includes(query))
    )
  }
  
  // 优先级过滤
  if (priorityFilter.value) {
    filtered = filtered.filter(wo => wo.priority === priorityFilter.value)
  }
  
  // 分类过滤
  if (categoryFilter.value) {
    filtered = filtered.filter(wo => wo.category === categoryFilter.value)
  }
  
  // 创建人过滤
  if (creatorFilter.value) {
    filtered = filtered.filter(wo => wo.creatorId?.toString() === creatorFilter.value)
  }
  
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
              <div class="stat-icon">📊</div>
              <div class="stat-number">{{ statistics.totalOrders }}</div>
              <div class="stat-label">总工单数</div>
            </div>
            <div class="stat-card clickable" @click="filterByStatus('COMPLETED')">
              <div class="stat-icon">✅</div>
              <div class="stat-number">{{ statistics.completedOrders }}</div>
              <div class="stat-label">已完成工单</div>
            </div>
            <div class="stat-card clickable" @click="filterByStatus('PROCESSING')">
              <div class="stat-icon">⏳</div>
              <div class="stat-number">{{ (statistics.submittedOrders || 0) + (statistics.approvedOrders || 0) + (statistics.rejectedOrders || 0) }}</div>
              <div class="stat-label">处理中工单</div>
            </div>
            <div class="stat-card clickable warning" @click="filterOverdue()">
              <div class="stat-icon">⚠️</div>
              <div class="stat-number">{{ statistics.overdueOrders }}</div>
              <div class="stat-label">逾期工单</div>
            </div>
            <div class="stat-card success clickable" @click="openDetailedStats('completion')">
              <div class="stat-icon">📈</div>
              <div class="stat-number">{{ statistics.onTimeCompletionRate.toFixed(1) }}%</div>
              <div class="stat-label">按时完成率</div>
              <div class="stat-detail">点击查看详情</div>
            </div>
            <div class="stat-card info clickable" @click="openDetailedStats('processing')">
              <div class="stat-icon">⏱️</div>
              <div class="stat-number">{{ statistics.averageProcessingTime.toFixed(1) }}h</div>
              <div class="stat-label">平均处理时间</div>
              <div class="stat-detail">点击查看详情</div>
            </div>
          </div>
          
          <!-- 权限说明 -->
          <div v-if="currentUser" class="permission-info">
            <small>
              📋 当前显示: 
              <span v-if="currentUser.role === 'ADMIN'">全系统数据</span>
              <span v-else-if="currentUser.role === 'DEPT_MANAGER'">{{ getDepartmentText(currentUser.department) }}数据</span>
              <span v-else>{{ currentUser.organizationLevel }}数据</span>
            </small>
          </div>
          
          <div class="recent-orders">
            <h3>最近工单</h3>
            <div class="order-list">
              <div 
                v-for="order in filteredWorkOrders.slice(0, 5)" 
                :key="order.id" 
                class="order-item clickable"
                @click="viewWorkOrderDetail(order)"
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

          <!-- 搜索和高级筛选 -->
          <div class="search-filter-section">
            <div class="search-bar">
              <div class="search-input-group">
                <span class="search-icon">🔍</span>
                <input 
                  v-model="searchQuery" 
                  type="text" 
                  placeholder="搜索工单标题、描述、编号..."
                  class="search-input"
                />
                <button 
                  v-if="searchQuery" 
                  @click="searchQuery = ''" 
                  class="clear-search-btn"
                >
                  ✕
                </button>
              </div>
            </div>
            
            <div class="advanced-filters">
              <div class="filter-group">
                <label>优先级:</label>
                <select v-model="priorityFilter" class="filter-select">
                  <option value="">全部优先级</option>
                  <option value="HIGH">高</option>
                  <option value="MEDIUM">中</option>
                  <option value="LOW">低</option>
                </select>
              </div>
              
              <div class="filter-group">
                <label>分类:</label>
                <select v-model="categoryFilter" class="filter-select">
                  <option value="">全部分类</option>
                  <option value="MAINTENANCE">维护</option>
                  <option value="INCIDENT">故障</option>
                  <option value="REQUEST">请求</option>
                  <option value="CHANGE">变更</option>
                  <option value="EMERGENCY">紧急</option>
                </select>
              </div>
              
              <div v-if="currentUser && ['ADMIN', 'DEPT_MANAGER'].includes(currentUser.role)" class="filter-group">
                <label>创建人:</label>
                <select v-model="creatorFilter" class="filter-select">
                  <option value="">全部创建人</option>
                  <option 
                    v-for="user in users" 
                    :key="user.id" 
                    :value="user.id.toString()"
                  >
                    {{ user.realName }}
                  </option>
                </select>
              </div>
              
              <button 
                v-if="searchQuery || priorityFilter || categoryFilter || creatorFilter || statusFilter || isOverdueFilter"
                @click="clearFilters" 
                class="btn btn-secondary btn-sm clear-filters-btn"
              >
                清空筛选
              </button>
            </div>
          </div>

          <!-- 过滤器状态栏 -->
          <div v-if="statusFilter || isOverdueFilter || searchQuery || priorityFilter || categoryFilter || creatorFilter" class="filter-bar">
            <div class="active-filters">
              <span class="filter-label">当前过滤:</span>
              <span v-if="searchQuery" class="filter-tag">
                搜索: "{{ searchQuery }}"
                <button @click="searchQuery = ''" class="filter-remove">&times;</button>
              </span>
              <span v-if="priorityFilter" class="filter-tag">
                优先级: {{ getPriorityText(priorityFilter) }}
                <button @click="priorityFilter = ''" class="filter-remove">&times;</button>
              </span>
              <span v-if="categoryFilter" class="filter-tag">
                分类: {{ getCategoryText(categoryFilter) }}
                <button @click="categoryFilter = ''" class="filter-remove">&times;</button>
              </span>
              <span v-if="creatorFilter" class="filter-tag">
                创建人: {{ users.find(u => u.id.toString() === creatorFilter)?.realName || '未知' }}
                <button @click="creatorFilter = ''" class="filter-remove">&times;</button>
              </span>
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
            <div 
              v-for="user in users" 
              :key="user.id" 
              class="user-card clickable"
              @click="showUserDetailModal(user)"
            >
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
            <div v-if="pendingApprovals.length === 0" class="empty-state">
              <p>暂无待审批工单</p>
            </div>
            <div v-for="order in pendingApprovals" :key="order.id" class="approval-card">
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
                    @click="rejectWorkOrder(order.id!)" 
                    :disabled="loading"
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

    <!-- 用户详情模态框 -->
    <div v-if="showUserDetail && selectedUser" class="modal-overlay" @click="closeUserDetail">
      <div class="modal detail-modal" @click.stop>
        <div class="modal-header">
          <h2>👤 用户详情</h2>
          <button @click="closeUserDetail" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <div class="detail-section">
            <h3>基本信息</h3>
            <div class="detail-grid">
              <div class="detail-item">
                <label>真实姓名:</label>
                <span>{{ selectedUser.realName }}</span>
              </div>
              <div class="detail-item">
                <label>用户名:</label>
                <span>{{ selectedUser.username }}</span>
              </div>
              <div class="detail-item">
                <label>邮箱:</label>
                <span>{{ selectedUser.email }}</span>
              </div>
              <div class="detail-item">
                <label>角色:</label>
                <span class="role-badge">{{ getRoleText(selectedUser.role) }}</span>
              </div>
              <div class="detail-item">
                <label>部门:</label>
                <span>{{ getDepartmentText(selectedUser.department) }}</span>
              </div>
              <div class="detail-item">
                <label>组织级别:</label>
                <span>{{ selectedUser.organizationLevel }}</span>
              </div>
              <div class="detail-item">
                <label>账户状态:</label>
                <span :class="['status-badge', selectedUser.enabled ? 'enabled' : 'disabled']">
                  {{ selectedUser.enabled ? '启用' : '禁用' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 详细统计模态框 -->
    <div v-if="showDetailedStats" class="modal-overlay" @click="closeDetailedStats">
      <div class="modal detail-modal stats-modal" @click.stop>
        <div class="modal-header">
          <h2>
            <span v-if="selectedStatType === 'completion'">📈 按时完成率详细分析</span>
            <span v-else-if="selectedStatType === 'processing'">⏱️ 处理时间详细分析</span>
            <span v-else>📊 统计分析</span>
          </h2>
          <button @click="closeDetailedStats" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          
          <!-- 按时完成率详细分析 -->
          <div v-if="selectedStatType === 'completion' && statistics" class="stats-detail-content">
            <!-- 关键指标概览 -->
            <div class="stats-overview">
              <div class="overview-card success">
                <div class="overview-icon">✅</div>
                <div class="overview-content">
                  <div class="overview-number">{{ statistics.onTimeCompletionRate.toFixed(1) }}%</div>
                  <div class="overview-label">当前按时完成率</div>
                </div>
              </div>
              <div class="overview-card info">
                <div class="overview-icon">📊</div>
                <div class="overview-content">
                  <div class="overview-number">{{ statistics.completedOrders || 0 }}</div>
                  <div class="overview-label">已完成工单总数</div>
                </div>
              </div>
              <div class="overview-card warning">
                <div class="overview-icon">⚠️</div>
                <div class="overview-content">
                  <div class="overview-number">{{ Math.round((statistics.completedOrders || 0) * (100 - statistics.onTimeCompletionRate) / 100) }}</div>
                  <div class="overview-label">逾期完成工单</div>
                </div>
              </div>
            </div>

            <!-- 完成率详细分析 -->
            <div class="detail-section">
              <h3>📈 完成率分析</h3>
              <div class="completion-analysis">
                <div class="progress-chart">
                  <div class="chart-title">按时完成率可视化</div>
                  <div class="progress-ring">
                    <svg viewBox="0 0 120 120" class="progress-svg">
                      <circle cx="60" cy="60" r="54" stroke="#e0e0e0" stroke-width="12" fill="none"/>
                      <circle 
                        cx="60" 
                        cy="60" 
                        r="54" 
                        stroke="#28a745" 
                        stroke-width="12" 
                        fill="none"
                        :stroke-dasharray="`${statistics.onTimeCompletionRate * 3.39} 339`"
                        stroke-linecap="round"
                        transform="rotate(-90 60 60)"
                      />
                    </svg>
                    <div class="progress-text">
                      <div class="progress-percent">{{ statistics.onTimeCompletionRate.toFixed(1) }}%</div>
                      <div class="progress-label">按时完成</div>
                    </div>
                  </div>
                </div>
                
                <div class="completion-metrics">
                  <div class="metric-item">
                    <div class="metric-icon">🎯</div>
                    <div class="metric-content">
                      <div class="metric-title">目标完成率</div>
                      <div class="metric-value">≥ 90%</div>
                      <div class="metric-status" :class="statistics.onTimeCompletionRate >= 90 ? 'good' : 'needs-improvement'">
                        {{ statistics.onTimeCompletionRate >= 90 ? '达标' : '需改进' }}
                      </div>
                    </div>
                  </div>
                  
                  <div class="metric-item">
                    <div class="metric-icon">📅</div>
                    <div class="metric-content">
                      <div class="metric-title">按时完成工单</div>
                      <div class="metric-value">{{ Math.round((statistics.completedOrders || 0) * statistics.onTimeCompletionRate / 100) }}</div>
                      <div class="metric-desc">/ {{ statistics.completedOrders || 0 }} 已完成</div>
                    </div>
                  </div>
                  
                  <div class="metric-item">
                    <div class="metric-icon">⏰</div>
                    <div class="metric-content">
                      <div class="metric-title">逾期工单</div>
                      <div class="metric-value">{{ Math.round((statistics.completedOrders || 0) * (100 - statistics.onTimeCompletionRate) / 100) }}</div>
                      <div class="metric-desc">需关注改进</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 改进建议 -->
            <div class="detail-section">
              <h3>💡 改进建议</h3>
              <div class="improvement-suggestions">
                <div v-if="statistics.onTimeCompletionRate < 90" class="suggestion-card priority">
                  <div class="suggestion-icon">🚨</div>
                  <div class="suggestion-content">
                    <div class="suggestion-title">紧急改进项</div>
                    <div class="suggestion-text">按时完成率低于90%，建议优化SLA时间设置或加强工单跟踪</div>
                  </div>
                </div>
                
                <div v-if="statistics.overdueOrders > 0" class="suggestion-card warning">
                  <div class="suggestion-icon">⚠️</div>
                  <div class="suggestion-content">
                    <div class="suggestion-title">逾期工单处理</div>
                    <div class="suggestion-text">当前有 {{ statistics.overdueOrders }} 个逾期工单，建议优先处理</div>
                  </div>
                </div>
                
                <div class="suggestion-card info">
                  <div class="suggestion-icon">📊</div>
                  <div class="suggestion-content">
                    <div class="suggestion-title">数据监控</div>
                    <div class="suggestion-text">建议定期监控完成率趋势，设置自动提醒机制</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 处理时间详细分析 -->
          <div v-if="selectedStatType === 'processing' && statistics" class="stats-detail-content">
            <!-- 处理时间概览 -->
            <div class="stats-overview">
              <div class="overview-card info">
                <div class="overview-icon">⏱️</div>
                <div class="overview-content">
                  <div class="overview-number">{{ statistics.averageProcessingTime.toFixed(1) }}h</div>
                  <div class="overview-label">平均处理时间</div>
                </div>
              </div>
              <div class="overview-card success">
                <div class="overview-icon">🚀</div>
                <div class="overview-content">
                  <div class="overview-number">{{ Math.max(0, 24 - statistics.averageProcessingTime).toFixed(1) }}h</div>
                  <div class="overview-label">比24h标准快</div>
                </div>
              </div>
              <div class="overview-card warning">
                <div class="overview-icon">📈</div>
                <div class="overview-content">
                  <div class="overview-number">{{ statistics.completedOrders || 0 }}</div>
                  <div class="overview-label">样本工单数</div>
                </div>
              </div>
            </div>

            <!-- 处理时间分析 -->
            <div class="detail-section">
              <h3>⏱️ 处理时间分析</h3>
              <div class="processing-analysis">
                <div class="time-chart">
                  <div class="chart-title">处理效率指标</div>
                  <div class="time-bars">
                    <div class="time-bar">
                      <div class="bar-label">当前平均时间</div>
                      <div class="bar-container">
                        <div 
                          class="bar-fill current" 
                          :style="{ width: Math.min(100, (statistics.averageProcessingTime / 48) * 100) + '%' }"
                        ></div>
                      </div>
                      <div class="bar-value">{{ statistics.averageProcessingTime.toFixed(1) }}h</div>
                    </div>
                    
                    <div class="time-bar">
                      <div class="bar-label">目标处理时间</div>
                      <div class="bar-container">
                        <div class="bar-fill target" style="width: 50%"></div>
                      </div>
                      <div class="bar-value">24.0h</div>
                    </div>
                    
                    <div class="time-bar">
                      <div class="bar-label">最佳实践时间</div>
                      <div class="bar-container">
                        <div class="bar-fill best" style="width: 25%"></div>
                      </div>
                      <div class="bar-value">12.0h</div>
                    </div>
                  </div>
                </div>
                
                <div class="time-metrics">
                  <div class="metric-item">
                    <div class="metric-icon">🎯</div>
                    <div class="metric-content">
                      <div class="metric-title">效率评级</div>
                      <div class="metric-value" :class="getEfficiencyGrade(statistics.averageProcessingTime)">
                        {{ getEfficiencyGradeText(getEfficiencyGrade(statistics.averageProcessingTime)) }}
                      </div>
                      <div class="metric-desc">基于行业标准</div>
                    </div>
                  </div>
                  
                  <div class="metric-item">
                    <div class="metric-icon">📊</div>
                    <div class="metric-content">
                      <div class="metric-title">相对表现</div>
                      <div class="metric-value">{{ statistics.averageProcessingTime <= 24 ? '良好' : '需改进' }}</div>
                      <div class="metric-desc">与24h标准对比</div>
                    </div>
                  </div>
                  
                  <div class="metric-item">
                    <div class="metric-icon">⚡</div>
                    <div class="metric-content">
                      <div class="metric-title">改进空间</div>
                      <div class="metric-value">{{ Math.max(0, statistics.averageProcessingTime - 12).toFixed(1) }}h</div>
                      <div class="metric-desc">距离最佳实践</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 优化建议 -->
            <div class="detail-section">
              <h3>🔧 优化建议</h3>
              <div class="optimization-suggestions">
                <div v-if="statistics.averageProcessingTime > 36" class="suggestion-card priority">
                  <div class="suggestion-icon">🚨</div>
                  <div class="suggestion-content">
                    <div class="suggestion-title">处理时间过长</div>
                    <div class="suggestion-text">平均处理时间超过36小时，建议检查流程瓶颈并优化资源配置</div>
                  </div>
                </div>
                
                <div v-else-if="statistics.averageProcessingTime > 24" class="suggestion-card warning">
                  <div class="suggestion-icon">⚠️</div>
                  <div class="suggestion-content">
                    <div class="suggestion-title">处理时间偏长</div>
                    <div class="suggestion-text">处理时间超过24小时标准，建议优化工作流程或增加人员配置</div>
                  </div>
                </div>
                
                <div v-else class="suggestion-card success">
                  <div class="suggestion-icon">✅</div>
                  <div class="suggestion-content">
                    <div class="suggestion-title">处理效率良好</div>
                    <div class="suggestion-text">处理时间在合理范围内，继续保持当前流程</div>
                  </div>
                </div>
                
                <div class="suggestion-card info">
                  <div class="suggestion-icon">📈</div>
                  <div class="suggestion-content">
                    <div class="suggestion-title">持续改进</div>
                    <div class="suggestion-text">建议定期分析处理时间趋势，识别季节性变化和流程优化机会</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 权限信息说明 -->
          <div v-if="currentUser" class="stats-permission-info">
            <small>
              📋 数据范围: 
              <span v-if="currentUser.role === 'ADMIN'">全系统统计数据</span>
              <span v-else-if="currentUser.role === 'DEPT_MANAGER'">{{ getDepartmentText(currentUser.department) }}统计数据</span>
              <span v-else>{{ currentUser.organizationLevel }}统计数据</span>
            </small>
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

    <!-- 用户详情模态框 -->
    <div v-if="showUserDetail && selectedUser" class="modal-overlay" @click="closeUserDetail">
      <div class="modal detail-modal" @click.stop>
        <div class="modal-header">
          <h2>👤 用户详情</h2>
          <button @click="closeUserDetail" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <div class="detail-section">
            <h3>基本信息</h3>
            <div class="detail-grid">
              <div class="detail-item">
                <label>真实姓名:</label>
                <span>{{ selectedUser.realName }}</span>
              </div>
              <div class="detail-item">
                <label>用户名:</label>
                <span>{{ selectedUser.username }}</span>
              </div>
              <div class="detail-item">
                <label>邮箱:</label>
                <span>{{ selectedUser.email }}</span>
              </div>
              <div class="detail-item">
                <label>角色:</label>
                <span class="role-badge">{{ getRoleText(selectedUser.role) }}</span>
              </div>
              <div class="detail-item">
                <label>部门:</label>
                <span>{{ getDepartmentText(selectedUser.department) }}</span>
              </div>
              <div class="detail-item">
                <label>组织级别:</label>
                <span>{{ selectedUser.organizationLevel }}</span>
              </div>
              <div class="detail-item">
                <label>账户状态:</label>
                <span :class="['status-badge', selectedUser.enabled ? 'enabled' : 'disabled']">
                  {{ selectedUser.enabled ? '启用' : '禁用' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 详细统计模态框 -->
    <div v-if="showDetailedStats" class="modal-overlay" @click="closeDetailedStats">
      <div class="modal detail-modal stats-modal" @click.stop>
        <div class="modal-header">
          <h2>
            <span v-if="selectedStatType === 'completion'">📈 按时完成率详细分析</span>
            <span v-else-if="selectedStatType === 'processing'">⏱️ 处理时间详细分析</span>
            <span v-else>📊 统计分析</span>
          </h2>
          <button @click="closeDetailedStats" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          
          <!-- 按时完成率详细分析 -->
          <div v-if="selectedStatType === 'completion' && statistics" class="stats-detail-content">
            <!-- 关键指标概览 -->
            <div class="stats-overview">
              <div class="overview-card success">
                <div class="overview-icon">✅</div>
                <div class="overview-content">
                  <div class="overview-number">{{ statistics.onTimeCompletionRate.toFixed(1) }}%</div>
                  <div class="overview-label">当前按时完成率</div>
                </div>
              </div>
              <div class="overview-card info">
                <div class="overview-icon">📊</div>
                <div class="overview-content">
                  <div class="overview-number">{{ statistics.completedOrders || 0 }}</div>
                  <div class="overview-label">已完成工单总数</div>
                </div>
              </div>
              <div class="overview-card warning">
                <div class="overview-icon">⚠️</div>
                <div class="overview-content">
                  <div class="overview-number">{{ Math.round((statistics.completedOrders || 0) * (100 - statistics.onTimeCompletionRate) / 100) }}</div>
                  <div class="overview-label">逾期完成工单</div>
                </div>
              </div>
            </div>

            <!-- 完成率详细分析 -->
            <div class="detail-section">
              <h3>📈 完成率分析</h3>
              <div class="completion-analysis">
                <div class="progress-chart">
                  <div class="chart-title">按时完成率可视化</div>
                  <div class="progress-ring">
                    <svg viewBox="0 0 120 120" class="progress-svg">
                      <circle cx="60" cy="60" r="54" stroke="#e0e0e0" stroke-width="12" fill="none"/>
                      <circle 
                        cx="60" 
                        cy="60" 
                        r="54" 
                        stroke="#28a745" 
                        stroke-width="12" 
                        fill="none"
                        :stroke-dasharray="`${statistics.onTimeCompletionRate * 3.39} 339`"
                        stroke-linecap="round"
                        transform="rotate(-90 60 60)"
                      />
                    </svg>
                    <div class="progress-text">
                      <div class="progress-percent">{{ statistics.onTimeCompletionRate.toFixed(1) }}%</div>
                      <div class="progress-label">按时完成</div>
                    </div>
                  </div>
                </div>
                
                <div class="completion-metrics">
                  <div class="metric-item">
                    <div class="metric-icon">🎯</div>
                    <div class="metric-content">
                      <div class="metric-title">目标完成率</div>
                      <div class="metric-value">≥ 90%</div>
                      <div class="metric-status" :class="statistics.onTimeCompletionRate >= 90 ? 'good' : 'needs-improvement'">
                        {{ statistics.onTimeCompletionRate >= 90 ? '达标' : '需改进' }}
                      </div>
                    </div>
                  </div>
                  
                  <div class="metric-item">
                    <div class="metric-icon">📅</div>
                    <div class="metric-content">
                      <div class="metric-title">按时完成工单</div>
                      <div class="metric-value">{{ Math.round((statistics.completedOrders || 0) * statistics.onTimeCompletionRate / 100) }}</div>
                      <div class="metric-desc">/ {{ statistics.completedOrders || 0 }} 已完成</div>
                    </div>
                  </div>
                  
                  <div class="metric-item">
                    <div class="metric-icon">⏰</div>
                    <div class="metric-content">
                      <div class="metric-title">逾期工单</div>
                      <div class="metric-value">{{ Math.round((statistics.completedOrders || 0) * (100 - statistics.onTimeCompletionRate) / 100) }}</div>
                      <div class="metric-desc">需关注改进</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 改进建议 -->
            <div class="detail-section">
              <h3>💡 改进建议</h3>
              <div class="improvement-suggestions">
                <div v-if="statistics.onTimeCompletionRate < 90" class="suggestion-card priority">
                  <div class="suggestion-icon">🚨</div>
                  <div class="suggestion-content">
                    <div class="suggestion-title">紧急改进项</div>
                    <div class="suggestion-text">按时完成率低于90%，建议优化SLA时间设置或加强工单跟踪</div>
                  </div>
                </div>
                
                <div v-if="statistics.overdueOrders > 0" class="suggestion-card warning">
                  <div class="suggestion-icon">⚠️</div>
                  <div class="suggestion-content">
                    <div class="suggestion-title">逾期工单处理</div>
                    <div class="suggestion-text">当前有 {{ statistics.overdueOrders }} 个逾期工单，建议优先处理</div>
                  </div>
                </div>
                
                <div class="suggestion-card info">
                  <div class="suggestion-icon">📊</div>
                  <div class="suggestion-content">
                    <div class="suggestion-title">数据监控</div>
                    <div class="suggestion-text">建议定期监控完成率趋势，设置自动提醒机制</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 处理时间详细分析 -->
          <div v-if="selectedStatType === 'processing' && statistics" class="stats-detail-content">
            <!-- 处理时间概览 -->
            <div class="stats-overview">
              <div class="overview-card info">
                <div class="overview-icon">⏱️</div>
                <div class="overview-content">
                  <div class="overview-number">{{ statistics.averageProcessingTime.toFixed(1) }}h</div>
                  <div class="overview-label">平均处理时间</div>
                </div>
              </div>
              <div class="overview-card success">
                <div class="overview-icon">🚀</div>
                <div class="overview-content">
                  <div class="overview-number">{{ Math.max(0, 24 - statistics.averageProcessingTime).toFixed(1) }}h</div>
                  <div class="overview-label">比24h标准快</div>
                </div>
              </div>
              <div class="overview-card warning">
                <div class="overview-icon">📈</div>
                <div class="overview-content">
                  <div class="overview-number">{{ statistics.completedOrders || 0 }}</div>
                  <div class="overview-label">样本工单数</div>
                </div>
              </div>
            </div>

            <!-- 处理时间分析 -->
            <div class="detail-section">
              <h3>⏱️ 处理时间分析</h3>
              <div class="processing-analysis">
                <div class="time-chart">
                  <div class="chart-title">处理效率指标</div>
                  <div class="time-bars">
                    <div class="time-bar">
                      <div class="bar-label">当前平均时间</div>
                      <div class="bar-container">
                        <div 
                          class="bar-fill current" 
                          :style="{ width: Math.min(100, (statistics.averageProcessingTime / 48) * 100) + '%' }"
                        ></div>
                      </div>
                      <div class="bar-value">{{ statistics.averageProcessingTime.toFixed(1) }}h</div>
                    </div>
                    
                    <div class="time-bar">
                      <div class="bar-label">目标处理时间</div>
                      <div class="bar-container">
                        <div class="bar-fill target" style="width: 50%"></div>
                      </div>
                      <div class="bar-value">24.0h</div>
                    </div>
                    
                    <div class="time-bar">
                      <div class="bar-label">最佳实践时间</div>
                      <div class="bar-container">
                        <div class="bar-fill best" style="width: 25%"></div>
                      </div>
                      <div class="bar-value">12.0h</div>
                    </div>
                  </div>
                </div>
                
                <div class="time-metrics">
                  <div class="metric-item">
                    <div class="metric-icon">🎯</div>
                    <div class="metric-content">
                      <div class="metric-title">效率评级</div>
                      <div class="metric-value" :class="getEfficiencyGrade(statistics.averageProcessingTime)">
                        {{ getEfficiencyGradeText(getEfficiencyGrade(statistics.averageProcessingTime)) }}
                      </div>
                      <div class="metric-desc">基于行业标准</div>
                    </div>
                  </div>
                  
                  <div class="metric-item">
                    <div class="metric-icon">📊</div>
                    <div class="metric-content">
                      <div class="metric-title">相对表现</div>
                      <div class="metric-value">{{ statistics.averageProcessingTime <= 24 ? '良好' : '需改进' }}</div>
                      <div class="metric-desc">与24h标准对比</div>
                    </div>
                  </div>
                  
                  <div class="metric-item">
                    <div class="metric-icon">⚡</div>
                    <div class="metric-content">
                      <div class="metric-title">改进空间</div>
                      <div class="metric-value">{{ Math.max(0, statistics.averageProcessingTime - 12).toFixed(1) }}h</div>
                      <div class="metric-desc">距离最佳实践</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 优化建议 -->
            <div class="detail-section">
              <h3>🔧 优化建议</h3>
              <div class="optimization-suggestions">
                <div v-if="statistics.averageProcessingTime > 36" class="suggestion-card priority">
                  <div class="suggestion-icon">🚨</div>
                  <div class="suggestion-content">
                    <div class="suggestion-title">处理时间过长</div>
                    <div class="suggestion-text">平均处理时间超过36小时，建议检查流程瓶颈并优化资源配置</div>
                  </div>
                </div>
                
                <div v-else-if="statistics.averageProcessingTime > 24" class="suggestion-card warning">
                  <div class="suggestion-icon">⚠️</div>
                  <div class="suggestion-content">
                    <div class="suggestion-title">处理时间偏长</div>
                    <div class="suggestion-text">处理时间超过24小时标准，建议优化工作流程或增加人员配置</div>
                  </div>
                </div>
                
                <div v-else class="suggestion-card success">
                  <div class="suggestion-icon">✅</div>
                  <div class="suggestion-content">
                    <div class="suggestion-title">处理效率良好</div>
                    <div class="suggestion-text">处理时间在合理范围内，继续保持当前流程</div>
                  </div>
                </div>
                
                <div class="suggestion-card info">
                  <div class="suggestion-icon">📈</div>
                  <div class="suggestion-content">
                    <div class="suggestion-title">持续改进</div>
                    <div class="suggestion-text">建议定期分析处理时间趋势，识别季节性变化和流程优化机会</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 权限信息说明 -->
          <div v-if="currentUser" class="stats-permission-info">
            <small>
              📋 数据范围: 
              <span v-if="currentUser.role === 'ADMIN'">全系统统计数据</span>
              <span v-else-if="currentUser.role === 'DEPT_MANAGER'">{{ getDepartmentText(currentUser.department) }}统计数据</span>
              <span v-else>{{ currentUser.organizationLevel }}统计数据</span>
            </small>
          </div>
        </div>
      </div>
    </div>

    <!-- 通知系统 -->
    <div class="notification-container">
      <transition-group name="notification" tag="div">
        <div 
          v-for="notification in notifications" 
          :key="notification.id" 
          :class="['notification-toast', notification.type]"
          @click="removeNotification(notification.id)"
        >
          <div class="notification-icon">
            <span v-if="notification.type === 'success'">✅</span>
            <span v-else-if="notification.type === 'error'">❌</span>
            <span v-else-if="notification.type === 'warning'">⚠️</span>
            <span v-else-if="notification.type === 'info'">ℹ️</span>
          </div>
          <div class="notification-content">
            <div class="notification-title">{{ notification.title }}</div>
            <div class="notification-message">{{ notification.message }}</div>
          </div>
          <button class="notification-close" @click.stop="removeNotification(notification.id)">
            ×
          </button>
        </div>
      </transition-group>
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
  transition: all 0.3s ease;
  cursor: pointer;
  border-radius: 5px;
  margin: 2px 0;
}

.order-item:last-child {
  border-bottom: none;
}

.order-item.clickable:hover {
  background: #f8f9fa;
  padding: 10px 15px;
  transform: translateX(5px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
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
  transition: all 0.3s ease;
  cursor: pointer;
}

.user-card:hover {
  background: #e3f2fd;
  box-shadow: 0 4px 15px rgba(0,0,0,0.15);
  transform: translateY(-2px);
}

.user-card.clickable:hover {
  background: #e3f2fd;
}

.user-info {
  flex: 1;
  margin-right: 10px;
}

.user-info h4 {
  margin: 0 0 8px 0;
  color: #2c3e50;
  font-weight: 600;
}

.user-info p {
  margin: 4px 0;
  font-size: 14px;
  color: #666;
  line-height: 1.4;
}

.user-status {
  min-width: 80px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
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

/* Detailed Statistics Modal Styles */
.stats-modal {
  max-width: 900px;
  max-height: 90vh;
}

.stats-detail-content {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

/* Statistics Overview Cards */
.stats-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 25px;
}

.overview-card {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
  border: 2px solid transparent;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 15px;
}

.overview-card.success {
  background: linear-gradient(135deg, #d4edda 0%, #c3e6cb 100%);
  border-color: #28a745;
}

.overview-card.info {
  background: linear-gradient(135deg, #d1ecf1 0%, #bee5eb 100%);
  border-color: #17a2b8;
}

.overview-card.warning {
  background: linear-gradient(135deg, #fff3cd 0%, #ffeeba 100%);
  border-color: #ffc107;
}

.overview-icon {
  font-size: 2.5em;
  opacity: 0.8;
}

.overview-content {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.overview-number {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  line-height: 1;
}

.overview-label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

/* Detailed Analysis Sections */
.detail-section {
  background: #f8f9fa;
  padding: 25px;
  border-radius: 12px;
  border-left: 4px solid #007bff;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.detail-section h3 {
  margin: 0 0 20px 0;
  color: #333;
  font-size: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

/* Completion Analysis Styles */
.completion-analysis {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 30px;
  align-items: start;
}

.progress-chart {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  text-align: center;
}

.progress-ring {
  position: relative;
  width: 150px;
  height: 150px;
}

.progress-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.progress-percent {
  font-size: 24px;
  font-weight: bold;
  color: #28a745;
  line-height: 1;
}

.progress-label {
  font-size: 12px;
  color: #666;
  margin-top: 5px;
}

.completion-metrics {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

/* Processing Analysis Styles */
.processing-analysis {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.time-chart {
  background: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.chart-header {
  margin-bottom: 20px;
}

.chart-title, .chart-subtitle {
  margin: 0;
  color: #333;
}

.chart-title {
  font-size: 18px;
  font-weight: 600;
}

.chart-subtitle {
  font-size: 14px;
  color: #666;
  margin-top: 5px;
}

.time-bars {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.time-bar {
  display: grid;
  grid-template-columns: 120px 1fr 60px;
  gap: 15px;
  align-items: center;
}

.bar-label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.bar-container {
  height: 20px;
  background: #e9ecef;
  border-radius: 10px;
  overflow: hidden;
  position: relative;
}

.bar-fill {
  height: 100%;
  border-radius: 10px;
  transition: width 0.8s ease;
  position: relative;
}

.bar-fill.current {
  background: linear-gradient(135deg, #007bff 0%, #0056b3 100%);
  animation: fillAnimation 1.5s ease-out;
}

.bar-fill.target {
  background: linear-gradient(135deg, #ffc107 0%, #e0a800 100%);
  animation: fillAnimation 1.5s ease-out 0.2s both;
}

.bar-fill.best {
  background: linear-gradient(135deg, #28a745 0%, #1e7e34 100%);
  animation: fillAnimation 1.5s ease-out 0.4s both;
}

@keyframes fillAnimation {
  from { width: 0 !important; }
}

.bar-value {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  text-align: right;
}

.time-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.metric-item {
  background: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  display: flex;
  align-items: center;
  gap: 15px;
  transition: transform 0.3s ease;
}

.metric-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
}

.metric-icon {
  font-size: 2em;
  opacity: 0.8;
}

.metric-content {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.metric-title {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.metric-value {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.metric-value.A\+ {
  color: #28a745;
}

.metric-value.A {
  color: #20c997;
}

.metric-value.B {
  color: #ffc107;
}

.metric-value.C {
  color: #fd7e14;
}

.metric-value.D {
  color: #dc3545;
}

.metric-desc {
  font-size: 12px;
  color: #999;
}

.metric-status {
  font-size: 14px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 15px;
  text-align: center;
}

.metric-status.good {
  background: #d4edda;
  color: #155724;
}

.metric-status.needs-improvement {
  background: #fff3cd;
  color: #856404;
}

/* Suggestion Cards */
.improvement-suggestions, .optimization-suggestions {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.suggestion-card {
  background: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  display: flex;
  align-items: flex-start;
  gap: 15px;
  border-left: 4px solid #e9ecef;
  transition: all 0.3s ease;
}

.suggestion-card:hover {
  transform: translateX(5px);
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
}

.suggestion-card.priority {
  border-left-color: #dc3545;
  background: linear-gradient(135deg, #fff5f5 0%, #fed7d7 100%);
}

.suggestion-card.warning {
  border-left-color: #ffc107;
  background: linear-gradient(135deg, #fffbf0 0%, #feebcb 100%);
}

.suggestion-card.success {
  border-left-color: #28a745;
  background: linear-gradient(135deg, #f0fff4 0%, #c6f6d5 100%);
}

.suggestion-card.info {
  border-left-color: #17a2b8;
  background: linear-gradient(135deg, #e6fffa 0%, #b2f5ea 100%);
}

.suggestion-icon {
  font-size: 1.5em;
  margin-top: 2px;
}

.suggestion-content {
  flex: 1;
}

.suggestion-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.suggestion-text {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}

/* Permission Info */
.stats-permission-info {
  background: #f8f9fa;
  padding: 10px 15px;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  margin-top: 20px;
  text-align: center;
}

.stats-permission-info small {
  color: #666;
  font-size: 13px;
}

/* Enhanced Dashboard Stat Cards */
.stat-detail {
  font-size: 12px;
  color: #666;
  margin-top: 5px;
  font-style: italic;
}

.permission-info {
  background: #f8f9fa;
  padding: 10px 15px;
  border-radius: 8px;
  margin-top: 20px;
  border: 1px solid #e9ecef;
}

.permission-info small {
  color: #666;
  font-size: 13px;
}

/* Responsive Design for Stats Modal */
@media (max-width: 768px) {
  .stats-modal {
    max-width: 95%;
    max-height: 95vh;
  }
  
  .stats-overview {
    grid-template-columns: 1fr;
  }
  
  .completion-analysis {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  
  .time-metrics {
    grid-template-columns: 1fr;
  }
  
  .time-bar {
    grid-template-columns: 1fr;
    gap: 8px;
    text-align: center;
  }
  
  .bar-container {
    order: 2;
  }
  
  .bar-label {
    order: 1;
    text-align: center;
  }
  
  .bar-value {
    order: 3;
    text-align: center;
  }
  
  .metric-item {
    flex-direction: column;
    text-align: center;
  }
  
  .suggestion-card {
    flex-direction: column;
    text-align: center;
  }
}

/* Search and Filter Styles */
.search-filter-section {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 10px;
  margin: 20px 0;
  border: 1px solid #e0e0e0;
}

.search-bar {
  margin-bottom: 15px;
}

.search-input-group {
  position: relative;
  max-width: 500px;
}

.search-icon {
  position: absolute;
  left: 15px;
  top: 50%;
  transform: translateY(-50%);
  color: #666;
  font-size: 16px;
}

.search-input {
  width: 100%;
  padding: 12px 15px 12px 45px;
  border: 2px solid #e0e0e0;
  border-radius: 25px;
  font-size: 14px;
  transition: all 0.3s ease;
  background: white;
}

.search-input:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
}

.clear-search-btn {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  background: #6c757d;
  color: white;
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  cursor: pointer;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.3s ease;
}

.clear-search-btn:hover {
  background: #5a6268;
}

.advanced-filters {
  display: flex;
  gap: 20px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 150px;
}

.filter-group label {
  font-size: 14px;
  font-weight: 600;
  color: #555;
}

.filter-select {
  padding: 8px 12px;
  border: 2px solid #e0e0e0;
  border-radius: 5px;
  font-size: 14px;
  background: white;
  transition: border-color 0.3s ease;
}

.filter-select:focus {
  outline: none;
  border-color: #007bff;
}

.clear-filters-btn {
  padding: 8px 16px;
  margin-left: auto;
}

/* Notification System Styles */
.notification-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  max-width: 400px;
  pointer-events: none;
}

.notification-toast {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  margin-bottom: 10px;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  pointer-events: all;
  position: relative;
  overflow: hidden;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.notification-toast:hover {
  transform: translateX(-5px);
  box-shadow: 0 6px 25px rgba(0, 0, 0, 0.2);
}

.notification-toast.success {
  background: linear-gradient(135deg, #d4edda 0%, #c3e6cb 100%);
  border: 1px solid #b8dabd;
  color: #155724;
}

.notification-toast.error {
  background: linear-gradient(135deg, #f8d7da 0%, #f1b0b7 100%);
  border: 1px solid #f5c6cb;
  color: #721c24;
}

.notification-toast.warning {
  background: linear-gradient(135deg, #fff3cd 0%, #fae19d 100%);
  border: 1px solid #ffeaa7;
  color: #856404;
}

.notification-toast.info {
  background: linear-gradient(135deg, #d1ecf1 0%, #bee5eb 100%);
  border: 1px solid #b8dadf;
  color: #0c5460;
}

.notification-icon {
  font-size: 20px;
  flex-shrink: 0;
  margin-top: 2px;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
  line-height: 1.2;
}

.notification-message {
  font-size: 13px;
  line-height: 1.3;
  opacity: 0.9;
}

.notification-close {
  position: absolute;
  top: 8px;
  right: 8px;
  background: none;
  border: none;
  font-size: 18px;
  color: currentColor;
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.3s ease;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.notification-close:hover {
  opacity: 1;
  background: rgba(0, 0, 0, 0.1);
}

/* Notification Transitions */
.notification-enter-active,
.notification-leave-active {
  transition: all 0.4s ease;
}

.notification-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.notification-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

.notification-move {
  transition: transform 0.4s ease;
}
</style>
