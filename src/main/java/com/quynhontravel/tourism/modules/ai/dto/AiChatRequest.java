package com.quynhontravel.tourism.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiChatRequest {

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String message;
}
