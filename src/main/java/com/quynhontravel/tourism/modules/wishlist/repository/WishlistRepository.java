package com.quynhontravel.tourism.modules.wishlist.repository;

import com.quynhontravel.tourism.modules.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    
    Optional<Wishlist> findByCustomerIdAndTourId(UUID customerId, UUID tourId);
    
    List<Wishlist> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    
    boolean existsByCustomerIdAndTourId(UUID customerId, UUID tourId);
}
