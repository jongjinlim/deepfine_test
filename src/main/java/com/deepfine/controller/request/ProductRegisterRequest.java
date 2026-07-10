package com.deepfine.controller.request;

import jakarta.validation.constraints.NotBlank;

public record ProductRegisterRequest(
        @NotBlank(message = "상품명은 필수입니다.")
        String name
) {
}
