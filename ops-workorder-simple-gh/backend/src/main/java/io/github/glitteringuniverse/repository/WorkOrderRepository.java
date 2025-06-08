package io.github.glitteringuniverse.repository;

import io.github.glitteringuniverse.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {}
