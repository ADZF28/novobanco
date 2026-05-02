package com.novobanco.account.infrastructure.adapter.in.web;

import com.novobanco.account.application.port.in.AccountPort;
import com.novobanco.account.infrastructure.adapter.in.web.dto.request.CreateAccountRequest;
import com.novobanco.account.infrastructure.adapter.in.web.dto.request.UpdateAccountStatusRequest;
import com.novobanco.account.infrastructure.adapter.in.web.dto.response.AccountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Gestión de cuentas bancarias")
public class AccountController {

    private final AccountPort accountPort;

    public AccountController(AccountPort accountPort) {
        this.accountPort = accountPort;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear cuenta", description = "Crea una nueva cuenta bancaria. Las cuentas corrientes requieren un depósito inicial mínimo de $50.")
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return AccountResponse.from(accountPort.createAccount(
                request.identification(),
                request.type(),
                request.initialStatus(),
                request.initialBalance()
        ));
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Obtener cuenta", description = "Obtiene los detalles de una cuenta por su número")
    public AccountResponse getAccount(@PathVariable String accountNumber) {
        return AccountResponse.from(accountPort.getAccount(accountNumber));
    }

    @PatchMapping("/{accountNumber}/status")
    @Operation(summary = "Cambiar estado de cuenta", description = "Actualiza únicamente el estado de una cuenta bancaria (ACTIVE, BLOCKED, CLOSED)")
    public AccountResponse updateStatus(@PathVariable String accountNumber,
                                        @Valid @RequestBody UpdateAccountStatusRequest request) {
        return AccountResponse.from(accountPort.updateStatus(accountNumber, request.status()));
    }
}
