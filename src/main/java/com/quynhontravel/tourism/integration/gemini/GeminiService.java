package com.quynhontravel.tourism.integration.gemini;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Gửi yêu cầu sinh nội dung văn bản hoặc JSON đến Gemini API
     */
    public String generateContent(String prompt, boolean forceJson) {
        try {
            String url = apiUrl + "?key=" + apiKey;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Xây dựng cấu trúc Request Body chuẩn cho Gemini API
            Map<String, Object> requestBody = new HashMap<>();
            
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);
            
            Map<String, Object> partContainer = new HashMap<>();
            partContainer.put("parts", List.of(textPart));
            
            requestBody.put("contents", List.of(partContainer));
            
            if (forceJson) {
                Map<String, Object> config = new HashMap<>();
                config.put("responseMimeType", "application/json");
                requestBody.put("generationConfig", config);
            }
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Phân tích cây dữ liệu JSON phản hồi của Gemini: candidates[0].content.parts[0].text
                Map<?, ?> body = response.getBody();
                List<?> candidates = (List<?>) body.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
                    Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
                    if (content != null) {
                        List<?> parts = (List<?>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
                            return (String) firstPart.get("text");
                        }
                    }
                }
            }
            throw new RuntimeException("Phản hồi rỗng hoặc không hợp lệ từ Gemini API.");
        } catch (Exception e) {
            log.error("Lỗi khi kết nối gửi yêu cầu đến Gemini API", e);
            throw new RuntimeException("Hệ thống xử lý AI đang bận, vui lòng thử lại sau. Chi tiết: " + e.getMessage());
        }
    }
}
