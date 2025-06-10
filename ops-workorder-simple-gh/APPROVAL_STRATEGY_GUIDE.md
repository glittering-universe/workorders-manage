# 审批分配策略和权限说明

## 📋 审批分配策略

### 🎯 **分配逻辑**

系统根据工单优先级自动分配审批人：

#### **高优先级工单 (HIGH)**
```
工单优先级: HIGH
↓
优先分配给: DEPT_MANAGER (部门经理)
↓
如果没有部门经理，则分配给: ADMIN (管理员)
```

#### **普通优先级工单 (MEDIUM/LOW)**
```
工单优先级: MEDIUM 或 LOW
↓
优先分配给: APPROVER (审批员)
↓
如果没有审批员，则分配给: DEPT_MANAGER (部门经理)
↓
如果没有部门经理，则分配给: ADMIN (管理员)
```

## 👥 **角色权限体系**

### **1. ADMIN (管理员)**
- **超级审批权限**: 可以审批任何待审批工单，不受分配限制
- **可见范围**: 审批中心显示所有待审批工单
- **权限等级**: 最高

### **2. DEPT_MANAGER (部门经理)**
- **上级审批权限**: 可以审批任何待审批工单，包括分配给下级的工单
- **可见范围**: 审批中心显示所有待审批工单
- **权限等级**: 高

### **3. APPROVER (审批员)**
- **基础审批权限**: 只能审批分配给自己的工单
- **可见范围**: 审批中心只显示分配给自己的待审批工单
- **权限等级**: 普通

## 🔧 **权限检查逻辑**

后端审批权限检查代码：
```java
// 权限检查：
// 1. Admin用户具有超级审批权限，可以审批任何工单
// 2. 部门经理可以审批任何工单（上级权限）
// 3. 当前指定审批人可以审批
boolean hasPermission = currentStep.getApproverId().equals(approverId) ||
                       approver.getRole() == User.Role.ADMIN ||
                       approver.getRole() == User.Role.DEPT_MANAGER;
```

## 📱 **前端显示逻辑**

### **审批中心工单显示**
```javascript
if (currentUser.value.role === 'ADMIN' || currentUser.value.role === 'DEPT_MANAGER') {
  // Admin和部门经理用户可以看到所有待审批工单
  const response = await axios.get(`${apiBase}/orders`)
  pendingApprovals.value = response.data.filter((wo: WorkOrder) => wo.status === 'SUBMITTED')
} else {
  // 其他用户只能看到分配给自己的待审批工单
  const response = await axios.get(`${apiBase}/orders/pending-approvals?approverId=${currentUser.value.id}`)
  pendingApprovals.value = response.data
}
```

## 🎯 **用户角色示例**

### **当前系统用户**
- **ID=1**: admin (ADMIN角色) - 超级权限
- **ID=2**: manager1 (DEPT_MANAGER角色) - 上级权限  
- **ID=6**: approver1 (APPROVER角色) - 基础权限

### **实际分配示例**

#### **例子1: 普通优先级工单**
```
工单: "部门经理权限测试" (MEDIUM优先级)
↓
自动分配给: approver1 (ID=6, APPROVER角色)
↓
可以审批的用户:
- ✅ approver1 (ID=6) - 原分配审批人
- ✅ manager1 (ID=2) - 部门经理上级权限
- ✅ admin (ID=1) - 管理员超级权限
```

#### **例子2: 高优先级工单**
```
工单: "Admin审批测试工单" (HIGH优先级)
↓
自动分配给: manager1 (ID=2, DEPT_MANAGER角色)
↓
可以审批的用户:
- ✅ manager1 (ID=2) - 原分配审批人
- ✅ admin (ID=1) - 管理员超级权限
- ❌ approver1 (ID=6) - 审批员无上级权限
```

## 🔒 **安全考虑**

1. **权限分层**: 确保上级可以处理下级的工作，避免审批瓶颈
2. **日志记录**: 所有审批操作都有完整的操作日志
3. **角色验证**: 后端严格验证用户角色和权限
4. **状态一致性**: 前后端状态同步，避免显示不一致

## 📊 **测试验证**

### **测试用例1: Admin超级权限** ✅
- Admin用户可以审批分配给审批员的工单
- Admin用户可以审批分配给部门经理的工单

### **测试用例2: 部门经理上级权限** ✅  
- 部门经理可以审批分配给审批员的工单
- 部门经理可以审批分配给自己的工单

### **测试用例3: 审批员基础权限** ✅
- 审批员只能审批分配给自己的工单
- 审批员不能审批分配给其他人的工单

## 🚀 **业务优势**

1. **灵活性**: 上级可以代替下级审批，避免流程卡住
2. **效率**: Admin和部门经理可以看到所有待审批工单，便于统一管理
3. **安全性**: 严格的权限控制，确保只有有权限的用户才能审批
4. **可扩展性**: 权限体系设计清晰，便于future扩展

---

**文档更新时间**: 2025年6月10日  
**系统状态**: ✅ 所有角色权限测试通过  
**审批功能**: ✅ 完全正常运行
