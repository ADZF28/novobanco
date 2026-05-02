package com.novobanco.account.infrastructure.adapter.in.web;

import com.novobanco.account.application.port.in.AccountPort;
import com.novobanco.account.application.port.in.ClientPort;
import com.novobanco.account.application.port.in.TransactionPort;
import com.novobanco.account.domain.model.Account;
import com.novobanco.account.domain.model.Client;
import com.novobanco.account.domain.model.Transaction;
import com.novobanco.account.infrastructure.adapter.in.web.dto.request.CreateClientRequest;
import com.novobanco.account.infrastructure.adapter.in.web.dto.response.ClientDetailResponse;
import com.novobanco.account.infrastructure.adapter.in.web.dto.response.ClientResponse;
import com.novobanco.account.infrastructure.adapter.in.web.dto.response.OutgoingTransfersResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@Tag(name = "Clients", description = "Gestión de clientes")
public class ClientController {

    private final ClientPort clientPort;
    private final AccountPort accountPort;
    private final TransactionPort transactionPort;

    public ClientController(ClientPort clientPort, AccountPort accountPort, TransactionPort transactionPort) {
        this.clientPort = clientPort;
        this.accountPort = accountPort;
        this.transactionPort = transactionPort;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear cliente", description = "Registra un nuevo cliente en el sistema")
    public ClientResponse createClient(@Valid @RequestBody CreateClientRequest request) {
        return ClientResponse.from(clientPort.createClient(
                request.fullName(),
                request.email(),
                request.identification(),
                request.typeIdentification(),
                request.phone(),
                request.address(),
                request.gender()
        ));
    }

    @GetMapping("/{identification}")
    @Operation(summary = "Consultar cliente", description = "Obtiene los datos del cliente junto con la cantidad de cuentas. Use ?includeAccounts=true para ver el detalle de cada cuenta.")
    public ClientDetailResponse getClient(@PathVariable String identification,
                                          @RequestParam(defaultValue = "false") boolean includeAccounts) {
        Client client = clientPort.getClient(identification);
        List<Account> accounts = accountPort.getAccountsByClientIdentification(identification);
        return ClientDetailResponse.from(client, accounts, includeAccounts);
    }

    @GetMapping("/{identification}/transfers/outgoing")
    @Operation(summary = "Transferencias salientes", description = "Lista las transferencias salientes de un cliente en un rango de fechas junto con el total.")
    public OutgoingTransfersResponse getOutgoingTransfers(
            @PathVariable String identification,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Transaction> transfers = transactionPort.getOutgoingTransfers(identification, startDate, endDate);
        return OutgoingTransfersResponse.from(identification, startDate, endDate, transfers);
    }
}
