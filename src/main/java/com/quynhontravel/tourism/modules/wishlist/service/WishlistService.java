package com.quynhontravel.tourism.modules.wishlist.service;

import com.quynhontravel.tourism.common.exception.ResourceNotFoundException;
import com.quynhontravel.tourism.modules.tour.dto.TourResponse;
import com.quynhontravel.tourism.modules.tour.entity.Tour;
import com.quynhontravel.tourism.modules.tour.repository.TourRepository;
import com.quynhontravel.tourism.modules.tour.service.TourService;
import com.quynhontravel.tourism.modules.user.repository.UserRepository;
import com.quynhontravel.tourism.modules.wishlist.dto.WishlistToggleResponse;
import com.quynhontravel.tourism.modules.wishlist.entity.Wishlist;
import com.quynhontravel.tourism.modules.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final TourService tourService;

    /**
     * Thêm hoặc xóa tour khỏi danh sách yêu thích (Toggle)
     */
    @Transactional
    public WishlistToggleResponse toggleWishlist(UUID customerId, UUID tourId) {
        // Kiểm tra khách hàng tồn tại
        if (!userRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Không tìm thấy thông tin người dùng.");
        }

        Optional<Wishlist> existingWishlist = wishlistRepository.findByCustomerIdAndTourId(customerId, tourId);
        
        if (existingWishlist.isPresent()) {
            // Đã tồn tại -> Thực hiện XÓA
            wishlistRepository.delete(existingWishlist.get());
            log.info("Khách hàng {} đã xóa tour {} khỏi danh sách yêu thích.", customerId, tourId);
            return WishlistToggleResponse.builder()
                    .tourId(tourId)
                    .saved(false)
                    .message("Đã xóa tour khỏi danh sách yêu thích.")
                    .build();
        } else {
            // Chưa tồn tại -> Kiểm tra Tour hợp lệ -> Thực hiện THÊM
            Tour tour = tourRepository.findById(tourId)
                    .filter(t -> !t.getIsDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Tour du lịch không tồn tại hoặc đã bị xóa."));

            Wishlist wishlist = Wishlist.builder()
                    .customerId(customerId)
                    .tourId(tour.getId())
                    .build();
            wishlistRepository.save(wishlist);
            log.info("Khách hàng {} đã thêm tour {} vào danh sách yêu thích.", customerId, tourId);

            return WishlistToggleResponse.builder()
                    .tourId(tourId)
                    .saved(true)
                    .message("Đã thêm tour vào danh sách yêu thích thành công.")
                    .build();
        }
    }

    /**
     * Lấy danh sách toàn bộ Tour yêu thích của khách hàng
     */
    @Transactional(readOnly = true)
    public List<TourResponse> getWishlist(UUID customerId) {
        // Kiểm tra khách hàng tồn tại
        if (!userRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Không tìm thấy thông tin người dùng.");
        }

        List<Wishlist> wishlists = wishlistRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId);
        List<TourResponse> responses = new ArrayList<>();

        for (Wishlist item : wishlists) {
            tourRepository.findById(item.getTourId())
                    .filter(tour -> !tour.getIsDeleted())
                    .map(tourService::mapToResponse)
                    .ifPresent(responses::add);
        }

        return responses;
    }
}
