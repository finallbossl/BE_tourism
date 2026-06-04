package com.quynhontravel.tourism.modules.wishlist;

import com.quynhontravel.tourism.modules.tour.dto.TourResponse;
import com.quynhontravel.tourism.modules.tour.entity.Category;
import com.quynhontravel.tourism.modules.tour.entity.Tour;
import com.quynhontravel.tourism.modules.tour.repository.TourRepository;
import com.quynhontravel.tourism.modules.tour.service.TourService;
import com.quynhontravel.tourism.modules.user.repository.UserRepository;
import com.quynhontravel.tourism.modules.wishlist.dto.WishlistToggleResponse;
import com.quynhontravel.tourism.modules.wishlist.entity.Wishlist;
import com.quynhontravel.tourism.modules.wishlist.repository.WishlistRepository;
import com.quynhontravel.tourism.modules.wishlist.service.WishlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;
    @Mock
    private TourRepository tourRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TourService tourService;

    @InjectMocks
    private WishlistService wishlistService;

    private UUID userId;
    private UUID tourId;
    private Tour tour;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tourId = UUID.randomUUID();
        tour = Tour.builder()
                .id(tourId)
                .title("Kỳ Co Eo Gió")
                .isDeleted(false)
                .category(Category.builder().name("Biển Đảo").slug("bien-dao").build())
                .build();
    }

    @Test
    void toggleWishlist_ShouldAdd_WhenNotExists() {
        when(userRepository.existsById(userId)).thenReturn(true);
        when(wishlistRepository.findByCustomerIdAndTourId(userId, tourId)).thenReturn(Optional.empty());
        when(tourRepository.findById(tourId)).thenReturn(Optional.of(tour));

        WishlistToggleResponse response = wishlistService.toggleWishlist(userId, tourId);

        assertNotNull(response);
        assertTrue(response.isSaved());
        assertEquals("Đã thêm tour vào danh sách yêu thích thành công.", response.getMessage());
        verify(wishlistRepository, times(1)).save(any(Wishlist.class));
    }

    @Test
    void toggleWishlist_ShouldRemove_WhenExists() {
        Wishlist wishlist = Wishlist.builder()
                .id(UUID.randomUUID())
                .customerId(userId)
                .tourId(tourId)
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(wishlistRepository.findByCustomerIdAndTourId(userId, tourId)).thenReturn(Optional.of(wishlist));

        WishlistToggleResponse response = wishlistService.toggleWishlist(userId, tourId);

        assertNotNull(response);
        assertFalse(response.isSaved());
        assertEquals("Đã xóa tour khỏi danh sách yêu thích.", response.getMessage());
        verify(wishlistRepository, times(1)).delete(wishlist);
    }

    @Test
    void getWishlist_ShouldReturnTours() {
        Wishlist wishlist = Wishlist.builder()
                .id(UUID.randomUUID())
                .customerId(userId)
                .tourId(tourId)
                .build();

        TourResponse tourResponse = TourResponse.builder()
                .id(tourId)
                .title("Kỳ Co Eo Gió")
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(wishlistRepository.findAllByCustomerIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(wishlist));
        when(tourRepository.findById(tourId)).thenReturn(Optional.of(tour));
        when(tourService.mapToResponse(tour)).thenReturn(tourResponse);

        List<TourResponse> result = wishlistService.getWishlist(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Kỳ Co Eo Gió", result.get(0).getTitle());
    }
}
