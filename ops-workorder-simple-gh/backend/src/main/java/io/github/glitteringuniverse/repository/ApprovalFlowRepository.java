package io.github.glitteringuniverse.repository;

import io.github.glitteringuniverse.entity.ApprovalFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalFlowRepository extends JpaRepository<ApprovalFlow, Long> {
    
    List<ApprovalFlow> findByWorkOrderIdOrderByStepOrder(Long workOrderId);
    
    List<ApprovalFlow> findByApproverIdAndStatus(Long approverId, ApprovalFlow.Status status);
    
    @Query("SELECT af FROM ApprovalFlow af WHERE af.workOrderId = :workOrderId AND af.status = 'PENDING' ORDER BY af.stepOrder")
    List<ApprovalFlow> findPendingApprovalsByWorkOrderId(@Param("workOrderId") Long workOrderId);
    
    @Query("SELECT af FROM ApprovalFlow af WHERE af.approverId = :approverId AND af.status = 'PENDING' ORDER BY af.createdAt")
    List<ApprovalFlow> findPendingApprovalsByApproverId(@Param("approverId") Long approverId);
}
