package io.hohichh.marketplace.gateway.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.UUID;

public record UserCredentialsCreateDto(
        @NotNull
        UUID userId,

        @NotBlank
        String login,

        @NotBlank
        String password
) implements Serializable {
}
