package com.quynhontravel.tourism.modules.tour.repository;

import com.quynhontravel.tourism.modules.tour.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    
    List<Category> findAllByIsDeletedFalse();
    
    Optional<Category> findBySlugAndIsDeletedFalse(String slug);
}
