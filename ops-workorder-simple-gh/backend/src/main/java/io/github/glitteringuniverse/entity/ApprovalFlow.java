package io.github.glitteringuniverse.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Data @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "approval_flow")
public class ApprovalFlow {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "work_order_id", nullable = false)
    private Long workOrderId;
    
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;
    
    @Column(name = "approver_id", nullable = false)
    private Long approverId;
    
    @Column(name = "approver_name", length = 100)
    private String approverName;
    
    @Column(name = "approver_role", length = 50)
    private String approverRole;
    
    @Column(name = "approver_dept", length = 100)
    private String approverDept;
    
    @Enumerated(EnumType.STRING)
    private Status status;
    
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum Status {
        PENDING,        // 待审批
        APPROVED,       // 已批准
        REJECTED,       // 已拒绝
        SKIPPED         // 已跳过
    }
}
