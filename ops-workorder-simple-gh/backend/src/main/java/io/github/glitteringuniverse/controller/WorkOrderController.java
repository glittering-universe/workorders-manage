package io.github.glitteringuniverse.controller;

import io.github.glitteringuniverse.entity.WorkOrder;
import io.github.glitteringuniverse.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService svc;

    @PostMapping
    public WorkOrder create(@RequestBody WorkOrder dto) {
        return svc.create(dto);
    }

    @PostMapping("/<built-in function id>/submit")
    public WorkOrder submit(@PathVariable Long id) {
        return svc.submit(id);
    }
}
