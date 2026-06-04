package com.quynhontravel.tourism.modules.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculateDiscountRequest {

    @NotNull(message = "Lịch khởi hành không được để trống")
    private UUID scheduleId;

    @NotNull(message = "Số lượng người lớn không được để trống")
    @Min(value = 1, message = "Số lượng người lớn tối thiểu là 1")
    private Integer quantityAdults;

    @NotNull(message = "Số lượng trẻ em không được để trống")
    @Min(value = 0, message = "Số lượng trẻ em không được âm")
    private Integer quantityChildren;

    @NotNull(message = "Số điểm sử dụng không được để trống")
    @Min(value = 0, message = "Số điểm sử dụng không được âm")
    private Integer pointsUsed;
}
