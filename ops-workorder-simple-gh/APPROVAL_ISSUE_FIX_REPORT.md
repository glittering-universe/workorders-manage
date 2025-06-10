# 审批功能问题修复报告

## 问题描述
用户反馈无法审批通过工单，经过分析发现这是前端和后端的参数不匹配问题。

## 发现的问题

### 1. 前端参数名称错误
**问题位置**: `frontend-web/src/App.vue` line 303
**问题描述**: 前端调用待审批工单API时使用了错误的参数名
```javascript
// 错误的参数名
const response = await axios.get(`${apiBase}/orders/pending-approvals?userId=${currentUser.value.id}`)

// 正确的参数名  
const response = await axios.get(`${apiBase}/orders/pending-approvals?approverId=${currentUser.value.id}`)
```

### 2. 审批中心显示逻辑错误
**问题位置**: `frontend-web/src/App.vue` line 1230
**问题描述**: 审批中心页面显示的是所有已提交工单，而不是当前用户的待审批工单
```vue
<!-- 错误的过滤逻辑 -->
<div v-for="order in workOrders.filter(wo => wo.status === 'SUBMITTED')" :key="order.id">

<!-- 正确的显示逻辑 -->
<div v-for="order in pendingApprovals" :key="order.id">
```

### 3. 拒绝按钮缺少点击事件
**问题位置**: `frontend-web/src/App.vue` line 1253
**问题描述**: 拒绝按钮没有绑定点击事件处理函数

### 4. 后端查询逻辑不严格
**问题位置**: `backend/src/main/java/.../WorkOrderService.java` line 57
**问题描述**: 待审批工单查询包含了已拒绝的工单

## 修复内容

### 1. 修复前端API调用参数
✅ 将 `userId` 参数改为 `approverId`

### 2. 修复审批中心显示逻辑
✅ 使用 `pendingApprovals` 数据而不是过滤所有工单
✅ 添加空状态提示

### 3. 实现拒绝功能
✅ 添加 `rejectWorkOrder` 函数
✅ 为拒绝按钮添加点击事件
✅ 审批/拒绝后重新加载待审批列表

### 4. 优化后端查询逻辑
✅ 只返回状态为 `SUBMITTED` 的工单

## 测试验证

### API测试结果
```bash
# 待审批工单查询 ✅
curl -X GET "http://localhost:8080/api/orders/pending-approvals?approverId=6"
# 返回: 只包含SUBMITTED状态的工单

# 审批功能 ✅  
curl -X POST "http://localhost:8080/api/orders/26/approve?approverId=6" \
  -H "Content-Type: application/json" \
  -d '{"comment": "审批通过"}'
# 返回: 工单状态变为APPROVED

# 拒绝功能 ✅
curl -X POST "http://localhost:8080/api/orders/24/reject?approverId=6" \
  -H "Content-Type: application/json" \
  -d '{"comment": "审批拒绝"}'
# 返回: 工单状态变为REJECTED
```

### 前端功能测试
1. ✅ 审批中心正确显示待审批工单
2. ✅ 批准按钮正常工作
3. ✅ 拒绝按钮正常工作
4. ✅ 审批后工单从待审批列表中移除
5. ✅ 权限控制正确（只有ADMIN、DEPT_MANAGER、APPROVER可以审批）

## 影响范围
- ✅ 修复不影响现有功能
- ✅ 向下兼容
- ✅ 无需数据库变更

## 测试工单
- 工单ID 24: 测试拒绝功能 ✅
- 工单ID 25: 测试审批功能 ✅  
- 工单ID 26: 测试前端界面 ✅

## 修复完成状态
🎉 **审批功能问题已完全修复！**

- 前端参数匹配问题 ✅ 已解决
- 审批中心显示问题 ✅ 已解决  
- 拒绝功能缺失问题 ✅ 已解决
- 后端查询逻辑问题 ✅ 已解决

用户现在可以正常使用审批功能进行工单的批准和拒绝操作。
