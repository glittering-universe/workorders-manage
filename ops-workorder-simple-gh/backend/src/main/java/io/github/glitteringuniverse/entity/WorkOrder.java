package io.github.glitteringuniverse.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Data @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "work_order")
public class WorkOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 32)
    private String woCode;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    private Category category;
    
    @Enumerated(EnumType.STRING)
    private Priority priority;
    
    private Integer slaMinutes;
    
    @Enumerated(EnumType.STRING)
    private Status status;
    
    // 申请人信息
    @Column(name = "creator_id")
    private Long creatorId;
    
    @Column(name = "creator_name", length = 100)
    private String creatorName;
    
    @Column(name = "creator_dept", length = 100)
    private String creatorDept;
    
    @Column(name = "creator_level", length = 100)
    private String creatorLevel; // 省/市/区县级别
    
    // 审批人信息
    @Column(name = "approver_id")
    private Long approverId;
    
    @Column(name = "approver_name", length = 100)
    private String approverName;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    // 执行人信息
    @Column(name = "assignee_id")
    private Long assigneeId;
    
    @Column(name = "assignee_name", length = 100)
    private String assigneeName;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    // 时间戳
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deadline")
    private LocalDateTime deadline;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (slaMinutes != null) {
            deadline = createdAt.plusMinutes(slaMinutes);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum Status { 
        DRAFT,          // 草稿
        SUBMITTED,      // 已提交
        APPROVING,      // 审批中
        APPROVED,       // 已审批
        ASSIGNED,       // 已分派
        IN_PROGRESS,    // 执行中
        COMPLETED,      // 已完成
        CLOSED,         // 已关闭
        REJECTED        // 已拒绝
    }
    
    public enum Category {
        MAINTENANCE,    // 维护
        INCIDENT,       // 故障
        REQUEST,        // 请求
        CHANGE,         // 变更
        EMERGENCY       // 紧急
    }
    
    public enum Priority {
        HIGH(1, "高"),
        MEDIUM(2, "中"),
        LOW(3, "低");
        
        private final int level;
        private final String description;
        
        Priority(int level, String description) {
            this.level = level;
            this.description = description;
        }
        
        public int getLevel() { return level; }
        public String getDescription() { return description; }
    }
}
