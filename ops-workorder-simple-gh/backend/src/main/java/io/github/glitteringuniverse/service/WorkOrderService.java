package io.github.glitteringuniverse.service;

import io.github.glitteringuniverse.entity.WorkOrder;
import io.github.glitteringuniverse.entity.WorkOrderLog;
import io.github.glitteringuniverse.entity.ApprovalFlow;
import io.github.glitteringuniverse.entity.User;
import io.github.glitteringuniverse.repository.WorkOrderRepository;
import io.github.glitteringuniverse.repository.WorkOrderLogRepository;
import io.github.glitteringuniverse.repository.ApprovalFlowRepository;
import io.github.glitteringuniverse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderLogRepository logRepository;
    private final ApprovalFlowRepository approvalFlowRepository;
    private final UserRepository userRepository;

    public List<WorkOrder> getAll() {
        return workOrderRepository.findAll();
    }
    
    /**
     * 根据ID获取工单
     */
    public WorkOrder getById(Long id) {
        return workOrderRepository.findById(id).orElse(null);
    }
    
    /**
     * 获取用户的待办工单
     */
    public List<WorkOrder> getPendingTasks(Long userId) {
        return workOrderRepository.findPendingTasksByAssigneeId(userId);
    }
    
    /**
     * 获取用户创建的工单
     */
    public List<WorkOrder> getMyOrders(Long userId) {
        return workOrderRepository.findByCreatorId(userId);
    }
    
    /**
     * 获取待审批的工单
     */
    public List<WorkOrder> getPendingApprovals(Long approverId) {
        List<ApprovalFlow> pendingApprovals = approvalFlowRepository.findPendingApprovalsByApproverId(approverId);
        return pendingApprovals.stream()
                .map(af -> workOrderRepository.findById(af.getWorkOrderId()).orElse(null))
                .filter(wo -> wo != null && wo.getStatus() == WorkOrder.Status.SUBMITTED)
                .toList();
    }

    @Transactional
    public WorkOrder create(WorkOrder dto, Long creatorId) {
        // 设置工单基本信息
        dto.setWoCode("WO-" + UUID.randomUUID().toString().substring(0,8));
        dto.setStatus(WorkOrder.Status.DRAFT);
        dto.setCreatedAt(LocalDateTime.now());
        
        // 设置创建人信息
        User creator = userRepository.findById(creatorId).orElseThrow();
        dto.setCreatorId(creatorId);
        dto.setCreatorName(creator.getRealName());
        dto.setCreatorDept(creator.getDepartment().name());
        dto.setCreatorLevel(creator.getOrganizationLevel());
        
        // 计算截止时间
        if (dto.getSlaMinutes() != null) {
            dto.setDeadline(dto.getCreatedAt().plusMinutes(dto.getSlaMinutes()));
        }
        
        WorkOrder saved = workOrderRepository.save(dto);
        
        // 记录操作日志
        logWorkOrderAction(saved.getId(), creatorId, creator.getRealName(), 
                WorkOrderLog.Action.CREATE, null, saved.getStatus().name(), "创建工单");
        
        return saved;
    }

    @Transactional
    public WorkOrder update(Long id, WorkOrder dto) {
        WorkOrder existingWorkOrder = workOrderRepository.findById(id).orElseThrow();
        
        // 只有草稿状态的工单才能编辑
        if (existingWorkOrder.getStatus() != WorkOrder.Status.DRAFT) {
            throw new IllegalStateException("只有草稿状态的工单才能编辑");
        }
        
        // 更新基本信息
        existingWorkOrder.setTitle(dto.getTitle());
        existingWorkOrder.setDescription(dto.getDescription());
        existingWorkOrder.setCategory(dto.getCategory());
        existingWorkOrder.setPriority(dto.getPriority());
        existingWorkOrder.setSlaMinutes(dto.getSlaMinutes());
        existingWorkOrder.setUpdatedAt(LocalDateTime.now());
        
        // 重新计算截止时间
        if (dto.getSlaMinutes() != null) {
            existingWorkOrder.setDeadline(existingWorkOrder.getCreatedAt().plusMinutes(dto.getSlaMinutes()));
        }
        
        return workOrderRepository.save(existingWorkOrder);
    }

    @Transactional
    public WorkOrder submit(Long id, Long userId) {
        WorkOrder wo = workOrderRepository.findById(id).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        
        if (wo.getStatus() != WorkOrder.Status.DRAFT) {
            throw new IllegalStateException("只有草稿状态的工单才能提交");
        }
        
        String fromStatus = wo.getStatus().name();
        wo.setStatus(WorkOrder.Status.SUBMITTED);
        wo.setUpdatedAt(LocalDateTime.now());
        
        WorkOrder saved = workOrderRepository.save(wo);
        
        // 创建审批流程
        createApprovalFlow(saved);
        
        // 记录操作日志
        logWorkOrderAction(id, userId, user.getRealName(), 
                WorkOrderLog.Action.SUBMIT, fromStatus, saved.getStatus().name(), "提交工单");
        
        return saved;
    }
    
    @Transactional
    public WorkOrder approve(Long id, Long approverId, String comment) {
        WorkOrder wo = workOrderRepository.findById(id).orElseThrow();
        User approver = userRepository.findById(approverId).orElseThrow();
        
        // 查找当前待审批步骤
        List<ApprovalFlow> pendingApprovals = approvalFlowRepository.findPendingApprovalsByWorkOrderId(id);
        if (pendingApprovals.isEmpty()) {
            throw new IllegalStateException("没有待审批的流程");
        }
        
        ApprovalFlow currentStep = pendingApprovals.get(0);
        // Admin用户具有超级审批权限，可以审批任何工单
        if (!currentStep.getApproverId().equals(approverId) && approver.getRole() != User.Role.ADMIN) {
            throw new IllegalStateException("不是当前审批人");
        }
        
        String fromStatus = wo.getStatus().name();
        
        // 更新审批流程
        currentStep.setStatus(ApprovalFlow.Status.APPROVED);
        currentStep.setComment(comment);
        currentStep.setApprovedAt(LocalDateTime.now());
        approvalFlowRepository.save(currentStep);
        
        // 检查是否还有待审批步骤
        List<ApprovalFlow> remainingApprovals = approvalFlowRepository.findPendingApprovalsByWorkOrderId(id);
        if (remainingApprovals.isEmpty()) {
            // 所有审批完成，更新工单状态
            wo.setStatus(WorkOrder.Status.APPROVED);
            wo.setApproverId(approverId);
            wo.setApproverName(approver.getRealName());
            wo.setApprovedAt(LocalDateTime.now());
        } else {
            wo.setStatus(WorkOrder.Status.APPROVING);
        }
        
        wo.setUpdatedAt(LocalDateTime.now());
        WorkOrder saved = workOrderRepository.save(wo);
        
        // 记录操作日志
        logWorkOrderAction(id, approverId, approver.getRealName(), 
                WorkOrderLog.Action.APPROVE, fromStatus, saved.getStatus().name(), comment);
        
        return saved;
    }
    
    @Transactional
    public WorkOrder reject(Long id, Long approverId, String comment) {
        WorkOrder wo = workOrderRepository.findById(id).orElseThrow();
        User approver = userRepository.findById(approverId).orElseThrow();
        
        String fromStatus = wo.getStatus().name();
        wo.setStatus(WorkOrder.Status.REJECTED);
        wo.setUpdatedAt(LocalDateTime.now());
        
        WorkOrder saved = workOrderRepository.save(wo);
        
        // 记录操作日志
        logWorkOrderAction(id, approverId, approver.getRealName(), 
                WorkOrderLog.Action.REJECT, fromStatus, saved.getStatus().name(), comment);
        
        return saved;
    }
    
    @Transactional
    public WorkOrder assign(Long id, Long assigneeId, Long assignerId) {
        WorkOrder wo = workOrderRepository.findById(id).orElseThrow();
        User assignee = userRepository.findById(assigneeId).orElseThrow();
        User assigner = userRepository.findById(assignerId).orElseThrow();
        
        if (wo.getStatus() != WorkOrder.Status.APPROVED) {
            throw new IllegalStateException("只有已审批的工单才能分派");
        }
        
        String fromStatus = wo.getStatus().name();
        wo.setStatus(WorkOrder.Status.ASSIGNED);
        wo.setAssigneeId(assigneeId);
        wo.setAssigneeName(assignee.getRealName());
        wo.setAssignedAt(LocalDateTime.now());
        wo.setUpdatedAt(LocalDateTime.now());
        
        WorkOrder saved = workOrderRepository.save(wo);
        
        // 记录操作日志
        logWorkOrderAction(id, assignerId, assigner.getRealName(), 
                WorkOrderLog.Action.ASSIGN, fromStatus, saved.getStatus().name(), 
                "分派给: " + assignee.getRealName());
        
        return saved;
    }
    
    @Transactional
    public WorkOrder complete(Long id, Long operatorId, String comment) {
        WorkOrder wo = workOrderRepository.findById(id).orElseThrow();
        User operator = userRepository.findById(operatorId).orElseThrow();
        
        if (!List.of(WorkOrder.Status.ASSIGNED, WorkOrder.Status.IN_PROGRESS).contains(wo.getStatus())) {
            throw new IllegalStateException("工单状态不允许完成操作");
        }
        
        String fromStatus = wo.getStatus().name();
        wo.setStatus(WorkOrder.Status.COMPLETED);
        wo.setCompletedAt(LocalDateTime.now());
        wo.setUpdatedAt(LocalDateTime.now());
        
        WorkOrder saved = workOrderRepository.save(wo);
        
        // 记录操作日志
        logWorkOrderAction(id, operatorId, operator.getRealName(), 
                WorkOrderLog.Action.COMPLETE, fromStatus, saved.getStatus().name(), comment);
        
        return saved;
    }
    
    /**
     * 获取工单操作日志
     */
    public List<WorkOrderLog> getWorkOrderLogs(Long workOrderId) {
        return logRepository.findByWorkOrderIdOrderByCreatedAtDesc(workOrderId);
    }
    
    private void createApprovalFlow(WorkOrder workOrder) {
        // 简化的审批流程：根据工单优先级和类别确定审批人
        List<User> approvers = getApproversForWorkOrder(workOrder);
        
        for (int i = 0; i < approvers.size(); i++) {
            User approver = approvers.get(i);
            ApprovalFlow flow = ApprovalFlow.builder()
                    .workOrderId(workOrder.getId())
                    .stepOrder(i + 1)
                    .approverId(approver.getId())
                    .approverName(approver.getRealName())
                    .approverRole(approver.getRole().name())
                    .approverDept(approver.getDepartment().name())
                    .status(ApprovalFlow.Status.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            approvalFlowRepository.save(flow);
        }
    }
    
    private List<User> getApproversForWorkOrder(WorkOrder workOrder) {
        // 优先级审批逻辑：高优先级需要部门经理或管理员审批
        if (workOrder.getPriority() == WorkOrder.Priority.HIGH) {
            // 高优先级：优先部门经理，如无则使用管理员
            List<User> deptManagers = userRepository.findByRole(User.Role.DEPT_MANAGER);
            if (!deptManagers.isEmpty()) {
                return deptManagers;
            }
            return userRepository.findByRole(User.Role.ADMIN);
        } else {
            // 普通优先级：审批员 -> 部门经理 -> 管理员
            List<User> approvers = userRepository.findByRole(User.Role.APPROVER);
            if (!approvers.isEmpty()) {
                return approvers;
            }
            
            List<User> deptManagers = userRepository.findByRole(User.Role.DEPT_MANAGER);
            if (!deptManagers.isEmpty()) {
                return deptManagers;
            }
            
            // 最后使用管理员作为兜底
            return userRepository.findByRole(User.Role.ADMIN);
        }
    }
    
    private void logWorkOrderAction(Long workOrderId, Long userId, String userName,
                                  WorkOrderLog.Action action, String fromStatus, String toStatus, String comment) {
        WorkOrderLog log = WorkOrderLog.builder()
                .workOrderId(workOrderId)
                .userId(userId)
                .userName(userName)
                .action(action)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build();
        logRepository.save(log);
    }
}
