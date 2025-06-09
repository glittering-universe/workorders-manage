package io.github.glitteringuniverse.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;

@Data @Builder
public class WorkOrderStatistics {
    private LocalDate date;
    private String department;
    private String organizationLevel;
    
    // 工单数量统计
    private Long totalOrders;
    private Long createdOrders;
    private Long submittedOrders;
    private Long approvedOrders;
    private Long completedOrders;
    private Long closedOrders;
    private Long rejectedOrders;
    
    // 按分类统计
    private Long maintenanceOrders;
    private Long incidentOrders;
    private Long requestOrders;
    private Long changeOrders;
    private Long emergencyOrders;
    
    // 按优先级统计
    private Long highPriorityOrders;
    private Long mediumPriorityOrders;
    private Long lowPriorityOrders;
    
    // 时效统计
    private Long overdueOrders;
    private Long soonToExpireOrders; // 24小时内到期
    private Double averageProcessingTime; // 平均处理时间(小时)
    private Double onTimeCompletionRate; // 按时完成率
}
