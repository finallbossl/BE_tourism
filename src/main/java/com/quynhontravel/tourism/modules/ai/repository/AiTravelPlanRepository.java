package com.quynhontravel.tourism.modules.ai.repository;

import com.quynhontravel.tourism.modules.ai.entity.AiTravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiTravelPlanRepository extends JpaRepository<AiTravelPlan, UUID> {
    
    List<AiTravelPlan> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
