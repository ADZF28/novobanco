package com.novobanco.account.infrastructure.adapter.in.web;

import com.novobanco.account.application.port.in.TransactionPort;
import com.novobanco.account.domain.enums.AccountStatus;
import com.novobanco.account.domain.enums.TransactionStatus;
import com.novobanco.account.domain.enums.TransactionType;
import com.novobanco.account.domain.exception.AccountNotOperableException;
import com.novobanco.account.domain.exception.DuplicateTransactionException;
import com.novobanco.account.domain.exception.InsufficientFundsException;
import com.novobanco.account.domain.exception.TransactionNotFoundException;
import com.novobanco.account.domain.model.Transaction;
import com.novobanco.account.infrastructure.adapter.in.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionPort transactionPort;

    private MockMvc mockMvc;

    private Transaction depositTx;
    private Transaction debitTx;
    private Transaction creditTx;
    private UUID fixedRef;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TransactionController(transactionPort))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        fixedRef = UUID.randomUUID();

        depositTx = Transaction.builder()
                .id(1L).uuid(UUID.randomUUID()).accountId(1L)
                .type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("500.00")).reference(fixedRef)
                .status(TransactionStatus.SUCCESS).description("Depósito test")
                .createdAt(LocalDateTime.now()).build();

        UUID transferRef = UUID.randomUUID();
        debitTx = Transaction.builder()
                .id(2L).uuid(UUID.randomUUID()).accountId(1L)
                .type(TransactionType.TRANSFER_DEBIT)
                .amount(new BigDecimal("200.00")).reference(UUID.randomUUID())
                .status(TransactionStatus.SUCCESS).transferReference(transferRef)
                .createdAt(LocalDateTime.now()).build();

        creditTx = Transaction.builder()
                .id(3L).uuid(UUID.randomUUID()).accountId(2L)
                .type(TransactionType.TRANSFER_CREDIT)
                .amount(new BigDecimal("200.00")).reference(UUID.randomUUID())
                .status(TransactionStatus.SUCCESS).transferReference(transferRef)
                .createdAt(LocalDateTime.now()).build();
    }

    @Test
    void deposit_shouldReturn201_whenValid() throws Exception {
        when(transactionPort.deposit(eq("NB-001"), any(), any(), any())).thenReturn(depositTx);

        mockMvc.perform(post("/api/v1/accounts/NB-001/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 500.00, "description": "Depósito test"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(500.00));
    }

    @Test
    void deposit_shouldReturn400_whenBodyInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/NB-001/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deposit_shouldReturn409_whenDuplicateReference() throws Exception {
        UUID ref = UUID.randomUUID();
        when(transactionPort.deposit(any(), any(), any(), any()))
                .thenThrow(new DuplicateTransactionException(ref));

        mockMvc.perform(post("/api/v1/accounts/NB-001/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 500.00, "reference": "%s"}""".formatted(ref)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void deposit_shouldReturn422_whenAccountBlocked() throws Exception {
        when(transactionPort.deposit(any(), any(), any(), any()))
                .thenThrow(new AccountNotOperableException("NB-001", AccountStatus.BLOCKED));

        mockMvc.perform(post("/api/v1/accounts/NB-001/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 100.00}"""))
                .andExpect(status().is(422));
    }

    @Test
    void withdraw_shouldReturn201_whenValid() throws Exception {
        Transaction withdrawal = Transaction.builder()
                .id(4L).uuid(UUID.randomUUID()).accountId(1L)
                .type(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("100.00")).reference(UUID.randomUUID())
                .status(TransactionStatus.SUCCESS).createdAt(LocalDateTime.now()).build();

        when(transactionPort.withdraw(eq("NB-001"), any(), any(), any())).thenReturn(withdrawal);

        mockMvc.perform(post("/api/v1/accounts/NB-001/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 100.00}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"));
    }

    @Test
    void withdraw_shouldReturn422_whenInsufficientFunds() throws Exception {
        when(transactionPort.withdraw(any(), any(), any(), any()))
                .thenThrow(new InsufficientFundsException("NB-001",
                        new BigDecimal("9999"), new BigDecimal("100")));

        mockMvc.perform(post("/api/v1/accounts/NB-001/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 9999.00}"""))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value(422));
    }

    @Test
    void transfer_shouldReturn201_withDebitAndCredit() throws Exception {
        when(transactionPort.transfer(any(), any(), any(), any(), any()))
                .thenReturn(List.of(debitTx, creditTx));

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccountNumber": "NB-001",
                                  "destinationAccountNumber": "NB-002",
                                  "amount": 200.00,
                                  "description": "Pago"
                                }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.debit.type").value("TRANSFER_DEBIT"))
                .andExpect(jsonPath("$.credit.type").value("TRANSFER_CREDIT"));
    }

    @Test
    void findByReference_shouldReturn200_whenFound() throws Exception {
        when(transactionPort.findByReference(fixedRef)).thenReturn(depositTx);

        mockMvc.perform(get("/api/v1/transactions/reference/" + fixedRef))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DEPOSIT"));
    }

    @Test
    void findByReference_shouldReturn404_whenNotFound() throws Exception {
        UUID ref = UUID.randomUUID();
        when(transactionPort.findByReference(ref)).thenThrow(new TransactionNotFoundException(ref));

        mockMvc.perform(get("/api/v1/transactions/reference/" + ref))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getHistory_shouldReturn200_withPagedResult() throws Exception {
        var page = new PageImpl<>(List.of(depositTx), PageRequest.of(0, 20), 1);
        when(transactionPort.getHistory(eq("NB-001"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/accounts/NB-001/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }
}
