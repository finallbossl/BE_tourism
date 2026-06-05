package com.quynhontravel.tourism;

import com.quynhontravel.tourism.integration.gemini.GeminiService;
import com.quynhontravel.tourism.modules.ai.service.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class TourismApplicationTests {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private AiService aiService;

    @Test
    void contextLoads() {
    }

    @Test
    void testGeminiRateLimit() {
        int maxRequests = 40;
        int successCount = 0;
        int rateLimitCount = 0;
        int errorCount = 0;
        String rateLimitErrorMessage = "";

        System.out.println("=== STARTING GEMINI RATE LIMIT TEST (Making " + maxRequests + " requests) ===");
        for (int i = 1; i <= maxRequests; i++) {
            try {
                // Keep the prompt short to save tokens and request time
                String response = geminiService.generateContent("Respond with exactly one word: ping" + i, false);
                successCount++;
                System.out.println("Request #" + i + ": SUCCESS");
                // Sleep 100ms to avoid instant connection issues
                Thread.sleep(100);
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                if (errorMsg.contains("429") || errorMsg.contains("Too Many Requests") || errorMsg.contains("quota") || errorMsg.contains("Quota")) {
                    rateLimitCount++;
                    rateLimitErrorMessage = errorMsg;
                    System.out.println("Request #" + i + ": RATE LIMITED (429)");
                } else {
                    errorCount++;
                    System.out.println("Request #" + i + ": FAILED with error: " + errorMsg);
                }
            }
        }
        System.out.println("=== RATE LIMIT TEST RESULTS ===");
        System.out.println("Total Requests Attempted: " + maxRequests);
        System.out.println("Successful Requests: " + successCount);
        System.out.println("Rate Limited (429) Requests: " + rateLimitCount);
        System.out.println("Other Failed Requests: " + errorCount);
        if (rateLimitCount > 0) {
            System.out.println("Rate Limit Error Message: " + rateLimitErrorMessage);
        }
        System.out.println("=================================");
    }

    @Test
    void testVariousModelsAndLimits() {
        String[] models = {
            "gemini-3.1-flash-lite",
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-pro"
        };
        
        System.out.println("=== TESTING GEMINI KEY ACCESSIBILITY FOR VARIOUS MODELS ===");
        for (String model : models) {
            try {
                long start = System.currentTimeMillis();
                String response = geminiService.generateContent(model, "Respond with exactly one word: hello", false);
                long duration = System.currentTimeMillis() - start;
                System.out.println("Model: " + model + " -> SUCCESS (Time: " + duration + "ms). Response: " + response.trim());
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                System.out.println("Model: " + model + " -> FAILED. Error: " + errorMsg);
            }
        }
        System.out.println("==========================================================");
    }

    @Test
    void testTravelPlanWithGemini31FlashLite() {
        String prompt = "Hãy đóng vai là một chuyên gia du lịch địa phương Quy Nhơn. Hãy lập lịch trình chi tiết dạng JSON cho chuyến đi Quy Nhơn với các thông số sau:\n" +
                "- Ngân sách: 5,000,000 VNĐ\n" +
                "- Số ngày: 3 ngày\n" +
                "- Số người: 2 người\n" +
                "- Sở thích: Tắm biển, ăn hải sản\n" +
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
                "Chỉ trả về chuỗi JSON thô, không thêm dấu nháy ngược markdown ```json hay bất kỳ văn bản nào khác.";

        System.out.println("=== TESTING TRAVEL PLAN GENERATION WITH GEMINI 3.1 FLASH LITE ===");
        try {
            long start = System.currentTimeMillis();
            String response = geminiService.generateContent("gemini-3.1-flash-lite", prompt, true);
            long duration = System.currentTimeMillis() - start;
            System.out.println("Status: SUCCESS (Time: " + duration + "ms)");
            System.out.println("Response:\n" + response);
            assertNotNull(response);
        } catch (Exception e) {
            System.out.println("Status: FAILED. Error: " + e.getMessage());
        }
        System.out.println("=================================================================");
    }

    @Test
    void listAvailableModels() {
        System.out.println("=== LISTING ALL AVAILABLE MODELS FOR THE API KEY ===");
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String url = "https://generativelanguage.googleapis.com/v1beta/models?key=AIzaSyCvmebg-F1rbAayeGBBrsd_3qP1d3NU2po";
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("RESPONSE:\n" + response);
        } catch (Exception e) {
            System.out.println("FAILED TO LIST MODELS: " + e.getMessage());
        }
        System.out.println("=====================================================");
    }

    @Test
    void testUserRequestedModels() {
        String[] models = {
            "gemma-4-26b",
            "gemma-4-31b",
            "gemma-4-26b-it",
            "gemma-4-31b-it",
            "gemini-2.5-flash-lite",
            "text-embedding-004",
            "embedding-001"
        };
        
        System.out.println("=== TESTING ACCESS FOR USER REQUESTED MODELS ===");
        for (String model : models) {
            try {
                long start = System.currentTimeMillis();
                String response = geminiService.generateContent(model, "Respond with exactly one word: test", false);
                long duration = System.currentTimeMillis() - start;
                System.out.println("Model: " + model + " -> SUCCESS (Time: " + duration + "ms). Response: " + response.trim());
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                System.out.println("Model: " + model + " -> FAILED. Error: " + errorMsg);
            }
        }
        System.out.println("================================================");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGeminiFallbackFailover() throws Exception {
        System.out.println("=== TESTING GEMINI FALLBACK/FAILOVER LOGIC ===");
        
        // We will use reflection to set fallbackModels to include an invalid model followed by a valid one
        java.lang.reflect.Field field = GeminiService.class.getDeclaredField("fallbackModels");
        field.setAccessible(true);
        
        // Save original list
        List<String> originalFallback = (List<String>) field.get(geminiService);
        
        try {
            // Set fallback list to: ["invalid-model-name", "gemini-3.1-flash-lite"]
            field.set(geminiService, List.of("invalid-model-name", "gemini-3.1-flash-lite"));
            
            long start = System.currentTimeMillis();
            String response = geminiService.generateContent("Respond with exactly one word: failover", false);
            long duration = System.currentTimeMillis() - start;
            
            System.out.println("Status: SUCCESS (Time: " + duration + "ms)");
            System.out.println("Response: " + response.trim());
            assertNotNull(response);
        } finally {
            // Restore original list
            field.set(geminiService, originalFallback);
        }
        System.out.println("===============================================");
    }

    @Test
    void testGeminiEmbeddingModel() {
        System.out.println("=== TESTING GEMINI EMBEDDING 1 MODEL ===");
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=AIzaSyCvmebg-F1rbAayeGBBrsd_3qP1d3NU2po";
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            
            java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("model", "models/gemini-embedding-001");
            
            java.util.Map<String, Object> part = new java.util.HashMap<>();
            part.put("text", "Du lịch Quy Nhơn có gì đẹp?");
            
            java.util.Map<String, Object> content = new java.util.HashMap<>();
            content.put("parts", java.util.List.of(part));
            
            requestBody.put("content", content);
            
            org.springframework.http.HttpEntity<java.util.Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(requestBody, headers);
            
            org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.postForEntity(url, entity, java.util.Map.class);
            
            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                java.util.Map<?, ?> body = response.getBody();
                java.util.Map<?, ?> embedding = (java.util.Map<?, ?>) body.get("embedding");
                if (embedding != null) {
                    java.util.List<?> values = (java.util.List<?>) embedding.get("values");
                    System.out.println("Status: SUCCESS");
                    System.out.println("Embedding Vector Size: " + (values != null ? values.size() : 0));
                    assertNotNull(values);
                    return;
                }
            }
            System.out.println("Status: FAILED. Invalid response structure.");
        } catch (Exception e) {
            System.out.println("Status: FAILED. Error: " + e.getMessage());
        }
        System.out.println("=========================================");
    }

    @Test
    void testChatbotResponse() {
        String question = "Bánh xèo tôm nhảy Quy Nhơn ăn ở đâu ngon nhất và Kỳ Co có gì đẹp không?";
        System.out.println("=== TESTING CHATBOT CONSULTATION ===");
        System.out.println("Question: " + question);
        try {
            long start = System.currentTimeMillis();
            String response = aiService.chat(question);
            long duration = System.currentTimeMillis() - start;
            System.out.println("Status: SUCCESS (Time: " + duration + "ms)");
            System.out.println("AI Response:\n" + response);
            assertNotNull(response);
        } catch (Exception e) {
            System.out.println("Status: FAILED. Error: " + e.getMessage());
        }
        System.out.println("=====================================");
    }

    @Test
    void testTravelPlanWithAllWorkingModels() {
        String[] models = {
            "gemini-3.1-flash-lite",
            "gemini-2.5-flash-lite",
            "gemma-4-31b-it",
            "gemma-4-26b-a4b-it"
        };

        String prompt = "Hãy đóng vai là một chuyên gia du lịch địa phương Quy Nhơn. Hãy lập lịch trình chi tiết dạng JSON cho chuyến đi Quy Nhơn với các thông số sau:\n" +
                "- Ngân sách: 5,000,000 VNĐ\n" +
                "- Số ngày: 3 ngày\n" +
                "- Số người: 2 người\n" +
                "- Sở thích: Tắm biển, ăn hải sản\n" +
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
                "Chỉ trả về chuỗi JSON thô, không thêm dấu nháy ngược markdown ```json hay bất kỳ văn bản nào khác.";

        System.out.println("=== TESTING TRAVEL PLAN GENERATION WITH ALL WORKING MODELS ===");
        for (String model : models) {
            try {
                System.out.println("\nTesting model: " + model);
                long start = System.currentTimeMillis();
                String response = geminiService.generateContent(model, prompt, true);
                long duration = System.currentTimeMillis() - start;
                System.out.println("Status: SUCCESS (Time: " + duration + "ms)");
                System.out.println("Response:\n" + response);
                assertNotNull(response);
            } catch (Exception e) {
                System.out.println("Status: FAILED. Error: " + e.getMessage());
            }
        }
        System.out.println("==============================================================");
    }
}
