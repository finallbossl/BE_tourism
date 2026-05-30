package com.quynhontravel.tourism.modules.tour.repository;

import com.quynhontravel.tourism.common.enums.TourScheduleStatus;
import com.quynhontravel.tourism.modules.tour.entity.TourSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourScheduleRepository extends JpaRepository<TourSchedule, UUID> {
    
    List<TourSchedule> findAllByTourIdAndStatus(UUID tourId, TourScheduleStatus status);
    
    List<TourSchedule> findAllByTourIdAndStartDateAfterOrderByStartDateAsc(UUID tourId, OffsetDateTime date);
    
    List<TourSchedule> findAllByGuideIdAndStatus(UUID guideId, TourScheduleStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TourSchedule s where s.id = :id")
    Optional<TourSchedule> findByIdWithLock(UUID id);
}

