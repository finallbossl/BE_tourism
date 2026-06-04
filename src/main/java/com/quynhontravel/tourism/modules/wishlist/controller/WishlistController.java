package com.quynhontravel.tourism.modules.wishlist.controller;

import com.quynhontravel.tourism.common.response.ApiResponse;
import com.quynhontravel.tourism.modules.tour.dto.TourResponse;
import com.quynhontravel.tourism.modules.wishlist.dto.WishlistRequest;
import com.quynhontravel.tourism.modules.wishlist.dto.WishlistToggleResponse;
import com.quynhontravel.tourism.modules.wishlist.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    /**
     * API thêm/xóa tour vào danh sách yêu thích (Toggle)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WishlistToggleResponse>> toggleWishlist(
            @Valid @RequestBody WishlistRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID customerId = UUID.fromString(jwt.getClaimAsString("userId"));
        WishlistToggleResponse response = wishlistService.toggleWishlist(customerId, request.getTourId());
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    /**
     * API lấy danh sách tour yêu thích của khách hàng
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TourResponse>>> getWishlist(@AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getClaimAsString("userId"));
        List<TourResponse> wishlist = wishlistService.getWishlist(customerId);
        return ResponseEntity.ok(ApiResponse.success(wishlist, "Lấy danh sách tour yêu thích thành công."));
    }
}
