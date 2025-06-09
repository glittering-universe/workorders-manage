package io.github.glitteringuniverse.service;

import io.github.glitteringuniverse.dto.WorkOrderStatistics;
import io.github.glitteringuniverse.entity.WorkOrder;
import io.github.glitteringuniverse.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    
    private final WorkOrderRepository workOrderRepository;
    
    /**
     * 获取日统计数据
     */
    public WorkOrderStatistics getDailyStatistics(LocalDate date, String department, String organizationLevel) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        
        List<WorkOrder> dayOrders = workOrderRepository.findByCreatedAtBetween(startOfDay, endOfDay);
        
        if (department != null) {
            dayOrders = dayOrders.stream()
                    .filter(wo -> department.equals(wo.getCreatorDept()))
                    .toList();
        }
        
        if (organizationLevel != null) {
            dayOrders = dayOrders.stream()
                    .filter(wo -> organizationLevel.equals(wo.getCreatorLevel()))
                    .toList();
        }
        
        return buildStatistics(date, department, organizationLevel, dayOrders);
    }
    
    /**
     * 获取周统计数据
     */
    public WorkOrderStatistics getWeeklyStatistics(LocalDate weekStart, String department, String organizationLevel) {
        LocalDateTime startOfWeek = weekStart.atStartOfDay();
        LocalDateTime endOfWeek = weekStart.plusDays(7).atStartOfDay();
        
        List<WorkOrder> weekOrders = workOrderRepository.findByCreatedAtBetween(startOfWeek, endOfWeek);
        
        if (department != null) {
            weekOrders = weekOrders.stream()
                    .filter(wo -> department.equals(wo.getCreatorDept()))
                    .toList();
        }
        
        if (organizationLevel != null) {
            weekOrders = weekOrders.stream()
                    .filter(wo -> organizationLevel.equals(wo.getCreatorLevel()))
                    .toList();
        }
        
        return buildStatistics(weekStart, department, organizationLevel, weekOrders);
    }
    
    /**
     * 获取超时预警工单
     */
    public List<WorkOrder> getOverdueOrders() {
        LocalDateTime now = LocalDateTime.now();
        return workOrderRepository.findByDeadlineBeforeAndStatusNotIn(
            now,
            List.of(WorkOrder.Status.COMPLETED, WorkOrder.Status.CLOSED)
        );
    }
    
    /**
     * 获取即将到期工单（24小时内）
     */
    public List<WorkOrder> getSoonToExpireOrders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in24Hours = now.plusHours(24);
        return workOrderRepository.findByDeadlineBetweenAndStatusNotIn(
            now,
            in24Hours,
            List.of(WorkOrder.Status.COMPLETED, WorkOrder.Status.CLOSED)
        );
    }
    
    /**
     * 获取总体统计数据 - 包含所有工单的统计信息
     */
    public WorkOrderStatistics getOverallStatistics(String department, String organizationLevel) {
        List<WorkOrder> allOrders = workOrderRepository.findAll();
        
        if (department != null) {
            allOrders = allOrders.stream()
                    .filter(wo -> department.equals(wo.getCreatorDept()))
                    .toList();
        }
        
        if (organizationLevel != null) {
            allOrders = allOrders.stream()
                    .filter(wo -> organizationLevel.equals(wo.getCreatorLevel()))
                    .toList();
        }
        
        return buildStatistics(LocalDate.now(), department, organizationLevel, allOrders);
    }
    
    private WorkOrderStatistics buildStatistics(LocalDate date, String department, String organizationLevel, List<WorkOrder> orders) {
        WorkOrderStatistics.WorkOrderStatisticsBuilder builder = WorkOrderStatistics.builder()
                .date(date)
                .department(department)
                .organizationLevel(organizationLevel)
                .totalOrders((long) orders.size());
        
        // 按状态统计
        builder.createdOrders(countByStatus(orders, WorkOrder.Status.DRAFT))
                .submittedOrders(countByStatus(orders, WorkOrder.Status.SUBMITTED))
                .approvedOrders(countByStatus(orders, WorkOrder.Status.APPROVED))
                .completedOrders(countByStatus(orders, WorkOrder.Status.COMPLETED))
                .closedOrders(countByStatus(orders, WorkOrder.Status.CLOSED))
                .rejectedOrders(countByStatus(orders, WorkOrder.Status.REJECTED));
        
        // 按分类统计
        builder.maintenanceOrders(countByCategory(orders, WorkOrder.Category.MAINTENANCE))
                .incidentOrders(countByCategory(orders, WorkOrder.Category.INCIDENT))
                .requestOrders(countByCategory(orders, WorkOrder.Category.REQUEST))
                .changeOrders(countByCategory(orders, WorkOrder.Category.CHANGE))
                .emergencyOrders(countByCategory(orders, WorkOrder.Category.EMERGENCY));
        
        // 按优先级统计
        builder.highPriorityOrders(countByPriority(orders, WorkOrder.Priority.HIGH))
                .mediumPriorityOrders(countByPriority(orders, WorkOrder.Priority.MEDIUM))
                .lowPriorityOrders(countByPriority(orders, WorkOrder.Priority.LOW));
        
        // 时效统计
        LocalDateTime now = LocalDateTime.now();
        long overdueCount = orders.stream()
                .filter(wo -> wo.getDeadline() != null && wo.getDeadline().isBefore(now))
                .filter(wo -> !List.of(WorkOrder.Status.COMPLETED, WorkOrder.Status.CLOSED).contains(wo.getStatus()))
                .count();
        builder.overdueOrders(overdueCount);
        
        long soonToExpireCount = orders.stream()
                .filter(wo -> wo.getDeadline() != null)
                .filter(wo -> wo.getDeadline().isAfter(now) && wo.getDeadline().isBefore(now.plusHours(24)))
                .filter(wo -> !List.of(WorkOrder.Status.COMPLETED, WorkOrder.Status.CLOSED).contains(wo.getStatus()))
                .count();
        builder.soonToExpireOrders(soonToExpireCount);
        
        // 计算平均处理时间
        double avgProcessingTime = orders.stream()
                .filter(wo -> wo.getCompletedAt() != null)
                .mapToLong(wo -> ChronoUnit.HOURS.between(wo.getCreatedAt(), wo.getCompletedAt()))
                .average()
                .orElse(0.0);
        builder.averageProcessingTime(avgProcessingTime);
        
        // 计算按时完成率 - 修复计算逻辑
        long completedOrders = orders.stream()
                .filter(wo -> wo.getCompletedAt() != null)
                .count();
        long onTimeCompletedOrders = orders.stream()
                .filter(wo -> wo.getCompletedAt() != null && wo.getDeadline() != null)
                .filter(wo -> wo.getCompletedAt().isBefore(wo.getDeadline()) || wo.getCompletedAt().equals(wo.getDeadline()))
                .count();
        
        double onTimeRate = completedOrders > 0 ? (double) onTimeCompletedOrders / completedOrders * 100 : 0.0;
        builder.onTimeCompletionRate(onTimeRate);
        
        return builder.build();
    }
    
    private long countByStatus(List<WorkOrder> orders, WorkOrder.Status status) {
        return orders.stream().filter(wo -> wo.getStatus() == status).count();
    }
    
    private long countByCategory(List<WorkOrder> orders, WorkOrder.Category category) {
        return orders.stream().filter(wo -> wo.getCategory() == category).count();
    }
    
    private long countByPriority(List<WorkOrder> orders, WorkOrder.Priority priority) {
        return orders.stream().filter(wo -> wo.getPriority() == priority).count();
    }
}
