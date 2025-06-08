package io.github.glitteringuniverse.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Data @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "work_order")
public class WorkOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 32)
    private String woCode;
    private String title;
    private String category;
    private Integer priority;
    private Integer slaMinutes;
    @Enumerated(EnumType.STRING)
    private Status status;
    public enum Status { DRAFT, SUBMITTED, APPROVED, CLOSED }
}
