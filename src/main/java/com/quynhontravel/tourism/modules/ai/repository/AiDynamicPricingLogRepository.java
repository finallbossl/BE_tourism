package com.quynhontravel.tourism.modules.ai.repository;

import com.quynhontravel.tourism.modules.ai.entity.AiDynamicPricingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiDynamicPricingLogRepository extends JpaRepository<AiDynamicPricingLog, UUID> {
    
    List<AiDynamicPricingLog> findAllByScheduleIdOrderByAppliedAtDesc(UUID scheduleId);
}
