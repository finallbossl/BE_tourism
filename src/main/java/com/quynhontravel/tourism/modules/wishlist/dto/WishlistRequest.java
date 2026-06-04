package com.quynhontravel.tourism.modules.wishlist.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistRequest {

    @NotNull(message = "Mã tour không được để trống")
    private UUID tourId;
}
