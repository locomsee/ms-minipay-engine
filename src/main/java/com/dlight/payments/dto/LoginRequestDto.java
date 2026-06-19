package com.dlight.payments.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(

        @Schema(example = "dlightadmin")
        @NotBlank(message = "username is required")
        String username,

        @Schema(example = "bJopnie@uu")
        @NotBlank(message = "password is required")
        String password
) {
}
