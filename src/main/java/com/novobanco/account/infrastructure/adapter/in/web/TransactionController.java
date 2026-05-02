package com.novobanco.account.infrastructure.adapter.in.web;

import com.novobanco.account.application.port.in.TransactionPort;
import com.novobanco.account.infrastructure.adapter.in.web.dto.request.DepositRequest;
import com.novobanco.account.infrastructure.adapter.in.web.dto.request.TransferRequest;
import com.novobanco.account.infrastructure.adapter.in.web.dto.request.WithdrawRequest;
import com.novobanco.account.infrastructure.adapter.in.web.dto.response.PagedResponse;
import com.novobanco.account.infrastructure.adapter.in.web.dto.response.TransactionResponse;
import com.novobanco.account.infrastructure.adapter.in.web.dto.response.TransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Transactions", description = "Operaciones de depósito, retiro y transferencia")
public class TransactionController {

    private final TransactionPort transactionPort;

    public TransactionController(TransactionPort transactionPort) {
        this.transactionPort = transactionPort;
    }

    @PostMapping("/accounts/{accountNumber}/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Depositar", description = "Realiza un depósito en la cuenta indicada")
    public TransactionResponse deposit(@PathVariable String accountNumber,
                                       @Valid @RequestBody DepositRequest request) {
        return TransactionResponse.from(
                transactionPort.deposit(accountNumber, request.amount(), request.reference(), request.description()));
    }

    @PostMapping("/accounts/{accountNumber}/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Retirar", description = "Realiza un retiro de la cuenta indicada")
    public TransactionResponse withdraw(@PathVariable String accountNumber,
                                        @Valid @RequestBody WithdrawRequest request) {
        return TransactionResponse.from(
                transactionPort.withdraw(accountNumber, request.amount(), request.reference(), request.description()));
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transferir", description = "Transfiere fondos entre dos cuentas")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        return TransferResponse.from(
                transactionPort.transfer(request.sourceAccountNumber(), request.destinationAccountNumber(),
                        request.amount(), request.reference(), request.description()));
    }

    @GetMapping("/transactions/reference/{reference}")
    @Operation(summary = "Buscar por referencia", description = "Busca una transacción por su referencia única. Útil para detectar duplicados o rastrear pagos.")
    public TransactionResponse findByReference(@PathVariable UUID reference) {
        return TransactionResponse.from(transactionPort.findByReference(reference));
    }

    @GetMapping("/accounts/{accountNumber}/transactions")
    @Operation(summary = "Historial de transacciones", description = "Obtiene el historial paginado de transacciones de una cuenta")
    public PagedResponse<TransactionResponse> getHistory(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return PagedResponse.from(transactionPort.getHistory(accountNumber, pageable), TransactionResponse::from);
    }
}
