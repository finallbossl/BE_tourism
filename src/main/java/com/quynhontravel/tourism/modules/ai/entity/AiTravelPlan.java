package com.quynhontravel.tourism.modules.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_travel_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiTravelPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "input_budget", nullable = false, precision = 12, scale = 2)
    private BigDecimal inputBudget;

    @Column(name = "input_days", nullable = false)
    private Integer inputDays;

    @Column(name = "input_guests", nullable = false)
    private Integer inputGuests;

    @Column(name = "input_preferences", columnDefinition = "TEXT")
    private String inputPreferences;

    @Column(name = "ai_response_json", nullable = false, columnDefinition = "jsonb")
    private String aiResponseJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
