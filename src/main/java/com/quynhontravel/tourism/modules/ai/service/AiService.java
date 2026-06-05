package com.quynhontravel.tourism.modules.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quynhontravel.tourism.common.enums.SentimentType;
import com.quynhontravel.tourism.integration.gemini.GeminiService;
import com.quynhontravel.tourism.modules.ai.entity.AiDynamicPricingLog;
import com.quynhontravel.tourism.modules.ai.entity.AiTravelPlan;
import com.quynhontravel.tourism.modules.ai.repository.AiDynamicPricingLogRepository;
import com.quynhontravel.tourism.modules.ai.repository.AiTravelPlanRepository;
import com.quynhontravel.tourism.modules.review.entity.Review;
import com.quynhontravel.tourism.modules.review.repository.ReviewRepository;
import com.quynhontravel.tourism.modules.tour.entity.Tour;
import com.quynhontravel.tourism.modules.tour.entity.TourSchedule;
import com.quynhontravel.tourism.modules.tour.repository.TourRepository;
import com.quynhontravel.tourism.modules.tour.repository.TourScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final GeminiService geminiService;
    private final AiTravelPlanRepository aiTravelPlanRepository;
    private final AiDynamicPricingLogRepository aiDynamicPricingLogRepository;
    private final ReviewRepository reviewRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final TourRepository tourRepository;
    private final ObjectMapper objectMapper;

    // Cache để lưu trữ embedding của các tour, tránh tạo lại nhiều lần gây tốn token và tăng độ trễ
    private final Map<UUID, List<Double>> tourEmbeddingCache = new ConcurrentHashMap<>();

    /**
     * Tạo lịch trình du lịch thông minh bằng AI (Có lưu cache trên Redis qua @Cacheable)
     */
    @Cacheable(value = "ai_plans", key = "T(java.util.Objects).hash(#budget, #days, #guests, #preferences)", unless = "#result == null")
    @Transactional
    public String createTravelPlan(BigDecimal budget, Integer days, Integer guests, String preferences, UUID customerId) {
        String prompt = String.format(
                "Hãy đóng vai là một chuyên gia du lịch địa phương Quy Nhơn. Hãy lập lịch trình chi tiết dạng JSON cho chuyến đi Quy Nhơn với các thông số sau:\n" +
                "- Ngân sách: %,.0f VNĐ\n" +
                "- Số ngày: %d ngày\n" +
                "- Số người: %d người\n" +
                "- Sở thích: %s\n" +
                "Yêu cầu trả về cấu trúc JSON nghiêm ngặt như sau:\n" +
                "{\n" +
                "  \"days\": [\n" +
                "    {\n" +
                "      \"day\": 1,\n" +
                "      \"activities\": [\n" +
                "        { \"time\": \"Morning\", \"location\": \"Tên địa điểm\", \"description\": \"Mô tả hoạt động\", \"costEstimate\": 100000 }\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"totalEstimatedCost\": 1500000\n" +
                "}\n" +
                "Chỉ trả về chuỗi JSON thô, không thêm dấu nháy ngược markdown ```json hay bất kỳ văn bản nào khác.",
                budget, days, guests, preferences
        );

        log.info("Bắt đầu gọi Gemini API lập lịch trình du lịch Quy Nhơn...");
        String responseJson = geminiService.generateContent(prompt, true);

        // Lưu thông tin lịch trình vào PostgreSQL
        AiTravelPlan plan = AiTravelPlan.builder()
                .customerId(customerId)
                .inputBudget(budget)
                .inputDays(days)
                .inputGuests(guests)
                .inputPreferences(preferences)
                .aiResponseJson(responseJson)
                .build();
        aiTravelPlanRepository.save(plan);
        log.info("Lập lịch trình AI thành công và lưu vào PostgreSQL cho khách hàng: {}", customerId);

        return responseJson;
    }

    /**
     * Tư vấn du lịch Quy Nhơn trực tuyến qua Trợ lý ảo Chatbot (Sử dụng RAG / Tìm kiếm ngữ nghĩa bằng Gemini Embedding 1)
     */
    public String chat(String message) {
        String systemInstruction = 
                "Bạn là một trợ lý ảo hỗ trợ khách hàng của công ty du lịch Quy Nhơn Travel.\n" +
                "Bạn chỉ trả lời các thông tin liên quan đến du lịch Quy Nhơn, danh thắng địa phương (Kỳ Co, Eo Gió, Tháp Đôi, Ghềnh Ráng Tiên Sa, Hòn Khô, Cù Lao Xanh),\n" +
                "ẩm thực đặc sản (bánh hỏi lòng heo, chả cá, tré, bánh xèo tôm nhảy), thời tiết và tư vấn các tour Quy Nhơn phù hợp.\n" +
                "Hãy luôn trả lời bằng tiếng Việt, giọng điệu thân thiện, mến khách và chuyên nghiệp. Không trả lời câu hỏi không liên quan đến du lịch hoặc Quy Nhơn.\n";

        // Tìm kiếm các tour liên quan ngữ nghĩa bằng Gemini Embedding 1 (RAG)
        String tourContext = retrieveRelevantToursContext(message);

        String prompt = systemInstruction + tourContext + "\nCâu hỏi khách hàng: " + message;
        return geminiService.generateContent(prompt, false);
    }

    /**
     * Tìm kiếm ngữ nghĩa các tour du lịch liên quan đến tin nhắn của khách hàng
     */
    private String retrieveRelevantToursContext(String message) {
        try {
            log.info("Đang tạo embedding truy vấn cho chatbot bằng gemini-embedding-001...");
            List<Double> queryEmbedding = geminiService.getEmbedding(message);
            List<Tour> tours = tourRepository.findAllByIsDeletedFalse();
            
            List<Map.Entry<Tour, Double>> matches = new ArrayList<>();
            for (Tour tour : tours) {
                List<Double> tourEmbedding = getCachedTourEmbedding(tour);
                if (tourEmbedding != null) {
                    double similarity = calculateCosineSimilarity(queryEmbedding, tourEmbedding);
                    // Ngưỡng lọc độ tương đồng tối thiểu
                    if (similarity >= 0.35) {
                        matches.add(new AbstractMap.SimpleEntry<>(tour, similarity));
                    }
                }
            }
            
            // Sắp xếp các tour phù hợp nhất lên đầu
            matches.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            
            if (matches.isEmpty()) {
                return "";
            }
            
            // Lấy tối đa 2 tour liên quan nhất cung cấp làm ngữ cảnh cho LLM
            StringBuilder context = new StringBuilder("\n[Ngữ cảnh hệ thống: Các tour du lịch Quy Nhơn liên quan được tìm thấy bằng AI (Gemini Embedding)]:\n");
            int count = 0;
            for (Map.Entry<Tour, Double> entry : matches) {
                Tour t = entry.getKey();
                context.append(String.format("- Tour: %s\n  Mô tả: %s\n  Giá gốc: %,.0f VNĐ\n  Thời gian: %d ngày %d đêm\n", 
                        t.getTitle(), t.getDescription(), t.getBasePrice(), t.getDurationDays(), t.getDurationNights()));
                count++;
                if (count >= 2) break;
            }
            return context.toString();
        } catch (Exception e) {
            log.warn("Lỗi khi tìm kiếm ngữ cảnh tour (RAG): {}. Trả lời mà không có ngữ cảnh.", e.getMessage());
            return "";
        }
    }

    /**
     * Tạo hoặc lấy từ cache vector embedding của tour du lịch
     */
    private List<Double> getCachedTourEmbedding(Tour tour) {
        return tourEmbeddingCache.computeIfAbsent(tour.getId(), id -> {
            String tourText = "Tour: " + tour.getTitle() + ". Mô tả: " + tour.getDescription();
            try {
                log.info("Tạo embedding mới cho tour: {}", tour.getTitle());
                return geminiService.getEmbedding(tourText);
            } catch (Exception e) {
                log.warn("Không thể tạo embedding cho tour: {}. Lỗi: {}", tour.getTitle(), e.getMessage());
                return null;
            }
        });
    }

    /**
     * Tính toán độ tương đồng Cosine giữa 2 vector embedding
     */
    private double calculateCosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.size() != vectorB.size()) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Phân tích cảm xúc của bình luận đánh giá (Xử lý bất đồng bộ ngầm @Async)
     */
    @Async
    @Transactional
    public void analyzeReviewSentiment(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            log.warn("Không tìm thấy review ID {} để phân tích cảm xúc", reviewId);
            return;
        }

        String prompt = String.format(
                "Phân tích bình luận sau của khách hàng đi tour Quy Nhơn và phân loại cảm xúc thành một trong ba nhãn duy nhất: POSITIVE, NEUTRAL, NEGATIVE.\n" +
                "Bình luận: \"%s\"\n" +
                "Chỉ trả về duy nhất từ khóa nhãn (POSITIVE hoặc NEUTRAL hoặc NEGATIVE), không thêm bất kỳ văn bản nào khác.",
                review.getComment()
        );

        try {
            log.info("Chạy ngầm phân tích cảm xúc cho Review ID: {}", reviewId);
            String sentimentResult = geminiService.generateContent(prompt, false).trim().toUpperCase();
            
            SentimentType sentiment = SentimentType.NEUTRAL;
            if (sentimentResult.contains("POSITIVE")) {
                sentiment = SentimentType.POSITIVE;
            } else if (sentimentResult.contains("NEGATIVE")) {
                sentiment = SentimentType.NEGATIVE;
            }
            
            review.setAiSentiment(sentiment);
            reviewRepository.save(review);
            log.info("Phân tích cảm xúc Review {} thành công: Nhãn {}", reviewId, sentiment);
        } catch (Exception e) {
            log.error("Lỗi phân tích cảm xúc AI cho Review ID: {}", reviewId, e);
        }
    }

    /**
     * Phân tích và tự động cập nhật giá động cho các lịch trình tour (Scheduler gọi)
     */
    @Transactional
    public void suggestDynamicPricing() {
        log.info("Bắt đầu quét và điều chỉnh giá động tự động bằng AI...");
        List<TourSchedule> schedules = tourScheduleRepository.findAll();
        
        OffsetDateTime now = OffsetDateTime.now();
        for (TourSchedule schedule : schedules) {
            // Chỉ định giá động cho các tour chưa khởi hành
            if (schedule.getStartDate().isAfter(now)) {
                long daysToDeparture = Duration.between(now, schedule.getStartDate()).toDays();
                double occupancyRate = (double) (schedule.getMaxSlots() - schedule.getAvailableSlots()) / schedule.getMaxSlots();
                
                String prompt = String.format(
                        "Hãy đóng vai chuyên gia định giá du lịch thông minh. Phân tích các thông số sau của lịch khởi hành tour và đề xuất giá bán mới tối ưu bằng định dạng JSON:\n" +
                        "{\n" +
                        "  \"tourTitle\": \"%s\",\n" +
                        "  \"basePrice\": %s,\n" +
                        "  \"currentPrice\": %s,\n" +
                        "  \"maxSlots\": %d,\n" +
                        "  \"availableSlots\": %d,\n" +
                        "  \"daysToDeparture\": %d\n" +
                        "}\n" +
                        "Yêu cầu phản hồi bằng 1 chuỗi JSON duy nhất, định dạng:\n" +
                        "{\n" +
                        "  \"newPrice\": 1250000,\n" +
                        "  \"reason\": \"Giải thích lý do đề xuất ngắn gọn bằng tiếng Việt\"\n" +
                        "}\n" +
                        "Không thêm bất kỳ văn bản nào khác ngoài JSON này.",
                        schedule.getTour().getTitle(),
                        schedule.getTour().getBasePrice(),
                        schedule.getCurrentPrice(),
                        schedule.getMaxSlots(),
                        schedule.getAvailableSlots(),
                        daysToDeparture
                );

                try {
                    String response = geminiService.generateContent(prompt, true);
                    Map<?, ?> result = objectMapper.readValue(response, Map.class);
                    
                    Number newPriceNum = (Number) result.get("newPrice");
                    BigDecimal newPrice = BigDecimal.valueOf(newPriceNum.doubleValue());
                    String reason = (String) result.get("reason");
                    
                    if (newPrice.compareTo(schedule.getCurrentPrice()) != 0) {
                        BigDecimal oldPrice = schedule.getCurrentPrice();
                        
                        // Cập nhật giá mới cho Tour Schedule
                        schedule.setCurrentPrice(newPrice);
                        tourScheduleRepository.save(schedule);
                        
                        // Ghi log biến động giá động
                        AiDynamicPricingLog pricingLog = AiDynamicPricingLog.builder()
                                .scheduleId(schedule.getId())
                                .oldPrice(oldPrice)
                                .newPrice(newPrice)
                                .triggerReason(reason)
                                .build();
                        aiDynamicPricingLogRepository.save(pricingLog);
                        
                        log.info("Lịch trình {} đã cập nhật giá động: {} -> {} (Lý do: {})", 
                                schedule.getId(), oldPrice, newPrice, reason);
                    }
                } catch (Exception e) {
                    log.error("Lỗi khi xử lý tính giá động cho Schedule ID: {}", schedule.getId(), e);
                }
            }
        }
    }
}
