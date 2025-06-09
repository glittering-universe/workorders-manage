package io.github.glitteringuniverse.repository;

import io.github.glitteringuniverse.entity.WorkOrderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkOrderLogRepository extends JpaRepository<WorkOrderLog, Long> {
    
    List<WorkOrderLog> findByWorkOrderIdOrderByCreatedAtDesc(Long workOrderId);
    
    List<WorkOrderLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT wl FROM WorkOrderLog wl WHERE wl.createdAt BETWEEN :startDate AND :endDate ORDER BY wl.createdAt DESC")
    List<WorkOrderLog> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT wl FROM WorkOrderLog wl WHERE wl.action = :action AND wl.createdAt >= :since")
    List<WorkOrderLog> findByActionSince(@Param("action") WorkOrderLog.Action action, @Param("since") LocalDateTime since);
}
