package com.novobanco.account.infrastructure.adapter.in.web.dto.request;

import com.novobanco.account.domain.enums.AccountStatus;
import com.novobanco.account.domain.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank(message = "La identificación del cliente es requerida")
        String identification,

        @NotNull(message = "El tipo de cuenta es requerido (SAVINGS o CHECKING)")
        AccountType type,

        @NotNull(message = "El estado inicial de la cuenta es requerido")
        AccountStatus initialStatus,

        @PositiveOrZero(message = "El balance inicial no puede ser negativo")
        BigDecimal initialBalance
) {}
