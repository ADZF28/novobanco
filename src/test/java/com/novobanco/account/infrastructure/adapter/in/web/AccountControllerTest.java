package com.novobanco.account.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novobanco.account.application.port.in.AccountPort;
import com.novobanco.account.domain.enums.AccountStatus;
import com.novobanco.account.domain.enums.AccountType;
import com.novobanco.account.domain.enums.IdentificationType;
import com.novobanco.account.domain.exception.AccountNotFoundException;
import com.novobanco.account.domain.exception.InsufficientInitialDepositException;
import com.novobanco.account.domain.model.Account;
import com.novobanco.account.domain.model.Client;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountPort accountPort;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AccountController(accountPort))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Account buildAccount(String accountNumber, AccountStatus status, AccountType type, BigDecimal balance) {
        Client client = Client.builder()
                .id(1L).uuid(UUID.randomUUID())
                .fullName("Ana García").email("ana@test.com")
                .identification("1234567890").typeIdentification(IdentificationType.NATIONAL_ID)
                .createdAt(LocalDateTime.now()).build();
        return Account.builder()
                .id(1L).uuid(UUID.randomUUID())
                .accountNumber(accountNumber).client(client)
                .type(type).currency("USD")
                .balance(balance).status(status)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    @Test
    void createAccount_shouldReturn201_whenSavingsWithZeroBalance() throws Exception {
        Account account = buildAccount("NB-0000000001", AccountStatus.ACTIVE, AccountType.SAVINGS, BigDecimal.ZERO);
        when(accountPort.createAccount(any(), any(), any(), any())).thenReturn(account);

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identification": "1234567890",
                                  "type": "SAVINGS",
                                  "initialStatus": "ACTIVE",
                                  "initialBalance": 0
                                }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("NB-0000000001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.type").value("SAVINGS"));
    }

    @Test
    void createAccount_shouldReturn201_whenCheckingWithSufficientBalance() throws Exception {
        Account account = buildAccount("NB-0000000002", AccountStatus.ACTIVE, AccountType.CHECKING, new BigDecimal("100.00"));
        when(accountPort.createAccount(any(), eq(AccountType.CHECKING), any(), any())).thenReturn(account);

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identification": "1234567890",
                                  "type": "CHECKING",
                                  "initialStatus": "ACTIVE",
                                  "initialBalance": 100.00
                                }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CHECKING"));
    }

    @Test
    void createAccount_shouldReturn422_whenCheckingBelowMinimumDeposit() throws Exception {
        when(accountPort.createAccount(any(), any(), any(), any()))
                .thenThrow(new InsufficientInitialDepositException(new BigDecimal("50")));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identification": "1234567890",
                                  "type": "CHECKING",
                                  "initialStatus": "ACTIVE",
                                  "initialBalance": 30.00
                                }"""))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value(422));
    }

    @Test
    void createAccount_shouldReturn400_whenRequiredFieldsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAccount_shouldReturn200_whenFound() throws Exception {
        Account account = buildAccount("NB-0000000001", AccountStatus.ACTIVE, AccountType.SAVINGS, BigDecimal.ZERO);
        when(accountPort.getAccount("NB-0000000001")).thenReturn(account);

        mockMvc.perform(get("/api/v1/accounts/NB-0000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("NB-0000000001"))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void getAccount_shouldReturn404_whenNotFound() throws Exception {
        when(accountPort.getAccount("NB-9999999999"))
                .thenThrow(new AccountNotFoundException("NB-9999999999"));

        mockMvc.perform(get("/api/v1/accounts/NB-9999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void updateStatus_shouldReturn200_whenValidStatus() throws Exception {
        Account blocked = buildAccount("NB-0000000001", AccountStatus.BLOCKED, AccountType.SAVINGS, BigDecimal.ZERO);
        when(accountPort.updateStatus("NB-0000000001", AccountStatus.BLOCKED)).thenReturn(blocked);

        mockMvc.perform(patch("/api/v1/accounts/NB-0000000001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "BLOCKED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void updateStatus_shouldReturn404_whenAccountNotFound() throws Exception {
        when(accountPort.updateStatus(any(), any()))
                .thenThrow(new AccountNotFoundException("NB-9999999999"));

        mockMvc.perform(patch("/api/v1/accounts/NB-9999999999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "BLOCKED"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_shouldReturn400_whenBodyIsEmpty() throws Exception {
        mockMvc.perform(patch("/api/v1/accounts/NB-0000000001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
