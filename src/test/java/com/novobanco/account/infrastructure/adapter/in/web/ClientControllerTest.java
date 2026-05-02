package com.novobanco.account.infrastructure.adapter.in.web;

import com.novobanco.account.application.port.in.AccountPort;
import com.novobanco.account.application.port.in.ClientPort;
import com.novobanco.account.application.port.in.TransactionPort;
import com.novobanco.account.domain.enums.AccountStatus;
import com.novobanco.account.domain.enums.AccountType;
import com.novobanco.account.domain.enums.IdentificationType;
import com.novobanco.account.domain.enums.TransactionStatus;
import com.novobanco.account.domain.enums.TransactionType;
import com.novobanco.account.domain.exception.ClientNotFoundException;
import com.novobanco.account.domain.model.Account;
import com.novobanco.account.domain.model.Client;
import com.novobanco.account.domain.model.Transaction;
import com.novobanco.account.infrastructure.adapter.in.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    @Mock
    private ClientPort clientPort;

    @Mock
    private AccountPort accountPort;

    @Mock
    private TransactionPort transactionPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ClientController(clientPort, accountPort, transactionPort))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Client buildClient() {
        return Client.builder()
                .id(1L).uuid(UUID.randomUUID())
                .fullName("Ana García").email("ana@test.com")
                .identification("1234567890").typeIdentification(IdentificationType.NATIONAL_ID)
                .phone("0991234567").address("Av. Test 123").gender("FEMALE")
                .createdAt(LocalDateTime.now()).build();
    }

    private Account buildAccount(Client client) {
        return Account.builder()
                .id(1L).uuid(UUID.randomUUID())
                .accountNumber("NB-0000000001").client(client)
                .type(AccountType.SAVINGS).currency("USD")
                .balance(new BigDecimal("500.00")).status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    @Test
    void createClient_shouldReturn201_whenValid() throws Exception {
        when(clientPort.createClient(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(buildClient());

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Ana García",
                                  "email": "ana@test.com",
                                  "identification": "1234567890",
                                  "typeIdentification": "NATIONAL_ID",
                                  "phone": "0991234567",
                                  "address": "Av. Test 123",
                                  "gender": "FEMALE"
                                }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identification").value("1234567890"))
                .andExpect(jsonPath("$.email").value("ana@test.com"));
    }

    @Test
    void createClient_shouldReturn400_whenRequiredFieldsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createClient_shouldReturn400_whenIdentificationDuplicated() throws Exception {
        when(clientPort.createClient(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Ya existe un cliente con la identificación: 1234567890"));

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Otro",
                                  "email": "otro@test.com",
                                  "identification": "1234567890",
                                  "typeIdentification": "NATIONAL_ID"
                                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getClient_shouldReturn200_withoutAccounts_byDefault() throws Exception {
        Client client = buildClient();
        when(clientPort.getClient("1234567890")).thenReturn(client);
        when(accountPort.getAccountsByClientIdentification("1234567890")).thenReturn(List.of(buildAccount(client)));

        mockMvc.perform(get("/api/v1/clients/1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identification").value("1234567890"))
                .andExpect(jsonPath("$.accountCount").value(1))
                .andExpect(jsonPath("$.accounts").doesNotExist());
    }

    @Test
    void getClient_shouldReturn200_withAccounts_whenIncludeAccountsTrue() throws Exception {
        Client client = buildClient();
        when(clientPort.getClient("1234567890")).thenReturn(client);
        when(accountPort.getAccountsByClientIdentification("1234567890")).thenReturn(List.of(buildAccount(client)));

        mockMvc.perform(get("/api/v1/clients/1234567890")
                        .param("includeAccounts", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountCount").value(1))
                .andExpect(jsonPath("$.accounts").isArray())
                .andExpect(jsonPath("$.accounts[0].accountNumber").value("NB-0000000001"));
    }

    @Test
    void getClient_shouldReturn404_whenNotFound() throws Exception {
        when(clientPort.getClient("0000000000"))
                .thenThrow(new ClientNotFoundException("0000000000"));

        mockMvc.perform(get("/api/v1/clients/0000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getOutgoingTransfers_shouldReturn200_withCount() throws Exception {
        Transaction transfer = Transaction.builder()
                .id(1L).uuid(UUID.randomUUID()).accountId(1L)
                .type(TransactionType.TRANSFER_DEBIT)
                .amount(new BigDecimal("200.00")).reference(UUID.randomUUID())
                .status(TransactionStatus.SUCCESS)
                .transferReference(UUID.randomUUID()).createdAt(LocalDateTime.now()).build();

        when(transactionPort.getOutgoingTransfers(eq("1234567890"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(transfer));

        mockMvc.perform(get("/api/v1/clients/1234567890/transfers/outgoing")
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-05-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identification").value("1234567890"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.transfers").isArray())
                .andExpect(jsonPath("$.transfers[0].type").value("TRANSFER_DEBIT"));
    }

    @Test
    void getOutgoingTransfers_shouldReturn200_withEmptyList() throws Exception {
        when(transactionPort.getOutgoingTransfers(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/clients/1234567890/transfers/outgoing")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.transfers").isArray());
    }
}
