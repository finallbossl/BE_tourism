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

    @Value("${app.gemini.model:gemini-2.5-flash}")
    private String defaultModel;

    @Value("${app.gemini.models-fallback:gemini-3.1-flash-lite,gemini-2.5-flash-lite,gemma-4-31b-it,gemini-2.5-flash}")
    private List<String> fallbackModels;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Gửi yêu cầu sinh nội dung văn bản hoặc JSON đến Gemini API bằng các model trong danh sách fallback từ trên xuống.
     * Khi model trước đó gặp lỗi (giới hạn hạn mức/kết nối), hệ thống tự động thử model tiếp theo để giảm thiểu gián đoạn và chi phí.
     */
    public String generateContent(String prompt, boolean forceJson) {
        List<String> modelsToTry = fallbackModels != null && !fallbackModels.isEmpty()
            ? fallbackModels
            : List.of(defaultModel);

        List<String> errors = new ArrayList<>();
        for (String model : modelsToTry) {
            try {
                log.info("Đang gọi model AI: {}", model);
                return generateContent(model, prompt, forceJson);
            } catch (Exception e) {
                log.warn("Gọi model {} thất bại. Lỗi: {}. Đang chuyển sang model tiếp theo trong danh sách...", model, e.getMessage());
                errors.add(model + " (" + e.getMessage() + ")");
            }
        }
        throw new RuntimeException("Tất cả các model AI trong cấu hình đều thất bại hoặc hết hạn mức. Chi tiết: " + String.join(" | ", errors));
    }

    /**
     * Gửi yêu cầu sinh nội dung văn bản hoặc JSON đến Gemini API bằng model được chỉ định
     */
    public String generateContent(String model, String prompt, boolean forceJson) {
        try {
            String url;
            if (apiUrl.contains("/models/")) {
                url = apiUrl.replaceAll("/models/[^:]+:", "/models/" + model + ":");
            } else {
                url = apiUrl + "/models/" + model + ":generateContent";
            }
            url = url + "?key=" + apiKey;
            
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
                            String text = (String) firstPart.get("text");
                            if (forceJson && text != null) {
                                return extractJson(text);
                            }
                            return text;
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

    /**
     * Trích xuất chuỗi JSON thô từ văn bản phản hồi của LLM (đề phòng model trả về giải thích hoặc markdown)
     */
    private String extractJson(String text) {
        if (text == null) {
            return null;
        }
        text = text.trim();
        
        // Loại bỏ block markdown code ```json ... ```
        if (text.startsWith("```json")) {
            text = text.substring(7);
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();
        } else if (text.startsWith("```")) {
            text = text.substring(3);
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();
        }
        
        // Tìm vị trí mở/đóng ngoặc nhọn hoặc ngoặc vuông đầu và cuối cùng
        int firstBrace = text.indexOf('{');
        int firstBracket = text.indexOf('[');
        int lastBrace = text.lastIndexOf('}');
        int lastBracket = text.lastIndexOf(']');
        
        int startIdx = -1;
        int endIdx = -1;
        
        if (firstBrace != -1 && (firstBracket == -1 || firstBrace < firstBracket)) {
            startIdx = firstBrace;
        } else if (firstBracket != -1) {
            startIdx = firstBracket;
        }
        
        if (lastBrace != -1 && (lastBracket == -1 || lastBrace > lastBracket)) {
            endIdx = lastBrace;
        } else if (lastBracket != -1) {
            endIdx = lastBracket;
        }
        
        if (startIdx != -1 && endIdx != -1 && endIdx >= startIdx) {
            return text.substring(startIdx, endIdx + 1);
        }
        
        return text;
    }

    /**
     * Lấy vector embedding (3072 chiều) từ văn bản sử dụng model gemini-embedding-001
     */
    @SuppressWarnings("unchecked")
    public List<Double> getEmbedding(String text) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=" + apiKey;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "models/gemini-embedding-001");
            
            Map<String, Object> part = new HashMap<>();
            part.put("text", text);
            
            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(part));
            
            requestBody.put("content", content);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<?, ?> body = response.getBody();
                Map<?, ?> embedding = (Map<?, ?>) body.get("embedding");
                if (embedding != null) {
                    List<Number> values = (List<Number>) embedding.get("values");
                    if (values != null) {
                        List<Double> doubleValues = new ArrayList<>();
                        for (Number val : values) {
                            doubleValues.add(val.doubleValue());
                        }
                        return doubleValues;
                    }
                }
            }
            throw new RuntimeException("Phản hồi rỗng hoặc không hợp lệ từ Gemini Embedding API.");
        } catch (Exception e) {
            log.error("Lỗi khi kết nối gửi yêu cầu đến Gemini Embedding API", e);
            throw new RuntimeException("Không thể lấy embedding cho văn bản. Chi tiết: " + e.getMessage());
        }
    }
}
