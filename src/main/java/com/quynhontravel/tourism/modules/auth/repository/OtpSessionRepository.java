package com.quynhontravel.tourism.modules.auth.repository;

import com.quynhontravel.tourism.common.enums.OtpPurpose;
import com.quynhontravel.tourism.modules.auth.entity.OtpSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpSessionRepository extends JpaRepository<OtpSession, UUID> {
    
    Optional<OtpSession> findFirstByEmailAndPurposeAndIsUsedOrderByCreatedAtDesc(
            String email, OtpPurpose purpose, Boolean isUsed);
}
