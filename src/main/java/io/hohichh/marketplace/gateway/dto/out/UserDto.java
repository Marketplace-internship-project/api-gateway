package io.hohichh.marketplace.gateway.dto.out;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record UserDto(
        @NotNull UUID id,
        @NotBlank
        @Size(max = 255)
        String name,
        @Size(max = 255)
        String surname,
        @Past LocalDate birthDate,
        @Email @NotBlank @Size(max = 255) String email) implements Serializable {
}