package io.github.glitteringuniverse.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity @Data @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false, length = 100)
    private String realName;
    
    @Column(length = 100)
    private String email;
    
    @Column(length = 20)
    private String phone;
    
    @Enumerated(EnumType.STRING)
    private Role role;
    
    @Enumerated(EnumType.STRING)
    private Department department;
    
    @Column(length = 100)
    private String organizationLevel; // 省/市/区县
    
    private Boolean enabled = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum Role {
        ADMIN,          // 系统管理员
        DEPT_MANAGER,   // 部门经理
        APPROVER,       // 审批人员
        OPERATOR,       // 操作人员
        USER            // 普通用户
    }
    
    public enum Department {
        IT_DEPT,        // IT部门
        NETWORK_DEPT,   // 网络部门
        SECURITY_DEPT,  // 安全部门
        MAINTENANCE,    // 维护部门
        BUSINESS_DEPT   // 业务部门
    }
}
