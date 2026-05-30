package com.quynhontravel.tourism.modules.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AiPlannerRequest {

    @NotNull(message = "Ngân sách không được để trống")
    @Min(value = 100000, message = "Ngân sách tối thiểu là 100,000 VNĐ")
    private BigDecimal budget;

    @NotNull(message = "Số ngày đi không được để trống")
    @Min(value = 1, message = "Số ngày đi tối thiểu là 1 ngày")
    private Integer days;

    @NotNull(message = "Số lượng người không được để trống")
    @Min(value = 1, message = "Số lượng người tối thiểu là 1")
    private Integer guests;

    private String preferences = "Tự do khám phá Quy Nhơn";
}
