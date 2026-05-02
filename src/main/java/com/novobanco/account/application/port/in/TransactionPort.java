package com.novobanco.account.application.port.in;

import com.novobanco.account.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionPort {
    Transaction deposit(String accountNumber, BigDecimal amount, UUID reference, String description);
    Transaction withdraw(String accountNumber, BigDecimal amount, UUID reference, String description);
    List<Transaction> transfer(String sourceAccountNumber, String destinationAccountNumber,
                               BigDecimal amount, UUID reference, String description);
    Page<Transaction> getHistory(String accountNumber, Pageable pageable);
    Transaction findByReference(UUID reference);
    List<Transaction> getOutgoingTransfers(String identification, LocalDate startDate, LocalDate endDate);
}
