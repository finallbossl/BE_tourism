package com.quynhontravel.tourism.modules.tour.repository;

import com.quynhontravel.tourism.modules.tour.entity.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourRepository extends JpaRepository<Tour, UUID>, JpaSpecificationExecutor<Tour> {
    
    List<Tour> findAllByIsDeletedFalse();
    
    Optional<Tour> findBySlugAndIsDeletedFalse(String slug);
    
    List<Tour> findAllByCategoryIdAndIsDeletedFalse(UUID categoryId);
}
