package com.quynhontravel.tourism.modules.tour.controller;

import com.quynhontravel.tourism.common.response.ApiResponse;
import com.quynhontravel.tourism.modules.tour.dto.CategoryResponse;
import com.quynhontravel.tourism.modules.tour.dto.TourDetailResponse;
import com.quynhontravel.tourism.modules.tour.dto.TourResponse;
import com.quynhontravel.tourism.modules.tour.service.CategoryService;
import com.quynhontravel.tourism.modules.tour.service.TourService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tours")
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;
    private final CategoryService categoryService;

    /**
     * API lấy danh sách toàn bộ Tour hoạt động
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TourResponse>>> getAllTours() {
        return ResponseEntity.ok(ApiResponse.success(tourService.getAllTours(), "Lấy danh sách tour thành công."));
    }

    /**
     * API lấy danh sách toàn bộ Danh mục hoạt động
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories(), "Lấy danh sách danh mục thành công."));
    }

    /**
     * API lấy chi tiết Tour cùng các lịch khởi hành sắp tới qua URL Slug
     */
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<TourDetailResponse>> getTourDetail(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(tourService.getTourDetailBySlug(slug), "Lấy chi tiết tour thành công."));
    }
}
