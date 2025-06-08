package io.github.glitteringuniverse.controller;

import io.github.glitteringuniverse.entity.WorkOrder;
import io.github.glitteringuniverse.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class WorkOrderController {

    private final WorkOrderService svc;

    @GetMapping
    public List<WorkOrder> getAll() {
        return svc.getAll();
    }

    @PostMapping
    public WorkOrder create(@RequestBody WorkOrder dto) {
        return svc.create(dto);
    }

    @PostMapping("/{id}/submit")
    public WorkOrder submit(@PathVariable Long id) {
        return svc.submit(id);
    }
}
