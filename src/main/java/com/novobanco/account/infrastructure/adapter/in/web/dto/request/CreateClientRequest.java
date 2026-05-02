package com.novobanco.account.infrastructure.adapter.in.web.dto.request;

import com.novobanco.account.domain.enums.IdentificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(
        @NotBlank(message = "El nombre completo es requerido")
        @Size(max = 200)
        String fullName,

        @NotBlank(message = "El email es requerido")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 200)
        String email,

        @NotBlank(message = "La identificación es requerida")
        @Size(max = 50)
        String identification,

        @NotNull(message = "El tipo de identificación es requerido (NATIONAL_ID, RUC, PASSPORT)")
        IdentificationType typeIdentification,

        @Size(max = 20)
        String phone,

        @Size(max = 500)
        String address,

        @Size(max = 30)
        String gender
) {}
