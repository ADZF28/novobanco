package com.novobanco.account.infrastructure.adapter.in.web.dto.request;

import com.novobanco.account.domain.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @NotNull(message = "El estado de la cuenta es requerido")
        AccountStatus status
) {}
