package io.github.glitteringuniverse.service;

import io.github.glitteringuniverse.entity.WorkOrder;
import io.github.glitteringuniverse.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository repo;

    public List<WorkOrder> getAll() {
        return repo.findAll();
    }

    @Transactional
    public WorkOrder create(WorkOrder dto) {
        dto.setWoCode("WO-" + UUID.randomUUID().toString().substring(0,8));
        dto.setStatus(WorkOrder.Status.DRAFT);
        return repo.save(dto);
    }

    @Transactional
    public WorkOrder submit(Long id) {
        WorkOrder wo = repo.findById(id).orElseThrow();
        if (wo.getStatus() != WorkOrder.Status.DRAFT)
            throw new IllegalStateException("Only draft can be submitted");
        wo.setStatus(WorkOrder.Status.SUBMITTED);
        return wo;
    }
}
