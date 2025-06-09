-- 用户表
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    department VARCHAR(20) NOT NULL,
    organization_level VARCHAR(100),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 更新工单表结构
DROP TABLE IF EXISTS work_order;
CREATE TABLE work_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wo_code VARCHAR(32) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(20) NOT NULL,
    priority VARCHAR(10) NOT NULL,
    sla_minutes INT,
    status VARCHAR(20) NOT NULL,
    creator_id BIGINT,
    creator_name VARCHAR(100),
    creator_dept VARCHAR(100),
    creator_level VARCHAR(100),
    approver_id BIGINT,
    approver_name VARCHAR(100),
    approved_at TIMESTAMP NULL,
    assignee_id BIGINT,
    assignee_name VARCHAR(100),
    assigned_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deadline TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    INDEX idx_creator_id (creator_id),
    INDEX idx_assignee_id (assignee_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_deadline (deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 工单操作日志表
CREATE TABLE work_order_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    work_order_id BIGINT NOT NULL,
    user_id BIGINT,
    user_name VARCHAR(100),
    action VARCHAR(20) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_work_order_id (work_order_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (work_order_id) REFERENCES work_order(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 审批流程表
CREATE TABLE approval_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    work_order_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    approver_id BIGINT NOT NULL,
    approver_name VARCHAR(100),
    approver_role VARCHAR(50),
    approver_dept VARCHAR(100),
    status VARCHAR(20) DEFAULT 'PENDING',
    comment TEXT,
    approved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_work_order_id (work_order_id),
    INDEX idx_approver_id (approver_id),
    INDEX idx_status (status),
    FOREIGN KEY (work_order_id) REFERENCES work_order(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化管理员用户
INSERT INTO users (username, password, real_name, email, role, department, organization_level, enabled) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9P1SHA.fkumdB9e', '系统管理员', 'admin@example.com', 'ADMIN', 'IT_DEPT', '省级', TRUE),
('manager1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9P1SHA.fkumdB9e', '部门经理', 'manager1@example.com', 'DEPT_MANAGER', 'IT_DEPT', '市级', TRUE),
('operator1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9P1SHA.fkumdB9e', '运维人员', 'operator1@example.com', 'OPERATOR', 'MAINTENANCE', '区县级', TRUE),
('user1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9P1SHA.fkumdB9e', '普通用户', 'user1@example.com', 'USER', 'BUSINESS_DEPT', '市级', TRUE);

-- 插入示例工单数据
INSERT INTO work_order (wo_code, title, description, category, priority, sla_minutes, status, creator_id, creator_name, creator_dept, creator_level, created_at, deadline) VALUES
('WO-SYS001', '服务器维护任务', '需要对生产服务器进行定期维护，包括系统更新、日志清理等操作', 'MAINTENANCE', 'MEDIUM', 480, 'SUBMITTED', 4, '普通用户', '业务部门', '市级', NOW(), DATE_ADD(NOW(), INTERVAL 480 MINUTE)),
('WO-NET002', '网络故障处理', '办公网络出现间歇性断网问题，需要紧急处理', 'INCIDENT', 'HIGH', 120, 'APPROVED', 4, '普通用户', '业务部门', '市级', NOW(), DATE_ADD(NOW(), INTERVAL 120 MINUTE)),
('WO-SEC003', '安全漏洞修复', '发现系统存在安全漏洞，需要及时修复', 'CHANGE', 'HIGH', 240, 'ASSIGNED', 4, '普通用户', '业务部门', '市级', NOW(), DATE_ADD(NOW(), INTERVAL 240 MINUTE));
