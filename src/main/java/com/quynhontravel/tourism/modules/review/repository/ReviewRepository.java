package com.quynhontravel.tourism.modules.review.repository;

import com.quynhontravel.tourism.modules.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    
    List<Review> findAllByTourIdOrderByCreatedAtDesc(UUID tourId);
}
