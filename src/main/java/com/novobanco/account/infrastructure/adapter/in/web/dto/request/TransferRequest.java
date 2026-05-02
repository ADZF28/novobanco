package com.novobanco.account.infrastructure.adapter.in.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotBlank(message = "La cuenta origen es requerida")
        String sourceAccountNumber,

        @NotBlank(message = "La cuenta destino es requerida")
        String destinationAccountNumber,

        @NotNull(message = "El monto es requerido")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        BigDecimal amount,

        UUID reference,

        String description
) {}
