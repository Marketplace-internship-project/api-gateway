package io.hohichh.marketplace.gateway.dto.in;

import jakarta.validation.constraints.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

public record UserDataWithCredentialsDto(
        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 255)
        String surname,

        @Past(message = "birthDate must be in the past")
        LocalDate birthDate,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotNull
        UUID userId,

        @NotBlank
        String login,

        @NotBlank
        String password
) implements Serializable {
}
