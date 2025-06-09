package io.github.glitteringuniverse.controller;

import io.github.glitteringuniverse.entity.WorkOrder;
import io.github.glitteringuniverse.entity.WorkOrderLog;
import io.github.glitteringuniverse.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175"}, allowCredentials = "true")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @GetMapping
    public List<WorkOrder> getAll() {
        return workOrderService.getAll();
    }
    
    @GetMapping("/my")
    public List<WorkOrder> getMyOrders(@RequestParam Long userId) {
        return workOrderService.getMyOrders(userId);
    }
    
    @GetMapping("/pending-tasks")
    public List<WorkOrder> getPendingTasks(@RequestParam Long userId) {
        return workOrderService.getPendingTasks(userId);
    }
    
    @GetMapping("/pending-approvals")
    public List<WorkOrder> getPendingApprovals(@RequestParam Long approverId) {
        return workOrderService.getPendingApprovals(approverId);
    }

    @PostMapping
    public WorkOrder create(@RequestBody WorkOrder dto, @RequestParam Long creatorId) {
        return workOrderService.create(dto, creatorId);
    }

    @PostMapping("/{id}/submit")
    public WorkOrder submit(@PathVariable Long id, @RequestParam Long userId) {
        return workOrderService.submit(id, userId);
    }
    
    @PostMapping("/{id}/approve")
    public WorkOrder approve(@PathVariable Long id, @RequestParam Long approverId, @RequestBody Map<String, String> request) {
        String comment = request.get("comment");
        return workOrderService.approve(id, approverId, comment);
    }
    
    @PostMapping("/{id}/reject")
    public WorkOrder reject(@PathVariable Long id, @RequestParam Long approverId, @RequestBody Map<String, String> request) {
        String comment = request.get("comment");
        return workOrderService.reject(id, approverId, comment);
    }
    
    @PostMapping("/{id}/assign")
    public WorkOrder assign(@PathVariable Long id, @RequestParam Long assigneeId, @RequestParam Long assignerId) {
        return workOrderService.assign(id, assigneeId, assignerId);
    }
    
    @PostMapping("/{id}/complete")
    public WorkOrder complete(@PathVariable Long id, @RequestParam Long operatorId, @RequestBody Map<String, String> request) {
        String comment = request.get("comment");
        return workOrderService.complete(id, operatorId, comment);
    }
    
    @GetMapping("/{id}/logs")
    public List<WorkOrderLog> getWorkOrderLogs(@PathVariable Long id) {
        return workOrderService.getWorkOrderLogs(id);
    }
}
