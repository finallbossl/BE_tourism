package com.quynhontravel.tourism.modules.wishlist.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistToggleResponse {
    private UUID tourId;
    private boolean saved;
    private String message;
}
