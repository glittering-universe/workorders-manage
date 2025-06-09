package io.github.glitteringuniverse.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Data @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "work_order_log")
public class WorkOrderLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "work_order_id", nullable = false)
    private Long workOrderId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "user_name", length = 100)
    private String userName;
    
    @Enumerated(EnumType.STRING)
    private Action action;
    
    @Column(name = "from_status", length = 20)
    private String fromStatus;
    
    @Column(name = "to_status", length = 20)
    private String toStatus;
    
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum Action {
        CREATE,         // 创建
        SUBMIT,         // 提交
        APPROVE,        // 审批通过
        REJECT,         // 审批拒绝
        ASSIGN,         // 分派
        EXECUTE,        // 执行
        COMPLETE,       // 完成
        CLOSE,          // 关闭
        COMMENT         // 添加备注
    }
}
