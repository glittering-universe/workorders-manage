package io.github.glitteringuniverse.repository;

import io.github.glitteringuniverse.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    
    List<WorkOrder> findByStatus(WorkOrder.Status status);
    
    List<WorkOrder> findByCreatorId(Long creatorId);
    
    List<WorkOrder> findByAssigneeId(Long assigneeId);
    
    List<WorkOrder> findByApproverId(Long approverId);
    
    List<WorkOrder> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<WorkOrder> findByDeadlineBeforeAndStatusNotIn(LocalDateTime deadline, List<WorkOrder.Status> statuses);
    
    List<WorkOrder> findByDeadlineBetweenAndStatusNotIn(LocalDateTime start, LocalDateTime end, List<WorkOrder.Status> statuses);
    
    @Query("SELECT wo FROM WorkOrder wo WHERE wo.assigneeId = :assigneeId AND wo.status IN ('ASSIGNED', 'IN_PROGRESS')")
    List<WorkOrder> findPendingTasksByAssigneeId(@Param("assigneeId") Long assigneeId);
    
    @Query("SELECT wo FROM WorkOrder wo WHERE wo.creatorDept = :department ORDER BY wo.createdAt DESC")
    List<WorkOrder> findByDepartment(@Param("department") String department);
    
    @Query("SELECT wo FROM WorkOrder wo WHERE wo.creatorLevel = :level ORDER BY wo.createdAt DESC")  
    List<WorkOrder> findByOrganizationLevel(@Param("level") String level);
}
