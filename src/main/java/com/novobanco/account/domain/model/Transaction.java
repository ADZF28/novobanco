package com.novobanco.account.domain.model;

import com.novobanco.account.domain.enums.TransactionStatus;
import com.novobanco.account.domain.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private Long id;
    private UUID uuid;
    private Long accountId;
    private TransactionType type;
    private BigDecimal amount;
    private UUID reference;
    private TransactionStatus status;
    private String description;
    private UUID transferReference;
    private LocalDateTime createdAt;

    public static Transaction createDeposit(Long accountId, BigDecimal amount, UUID reference, String description) {
        return Transaction.builder()
                .accountId(accountId)
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .reference(reference)
                .status(TransactionStatus.SUCCESS)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Transaction createWithdrawal(Long accountId, BigDecimal amount, UUID reference, String description) {
        return Transaction.builder()
                .accountId(accountId)
                .type(TransactionType.WITHDRAWAL)
                .amount(amount)
                .reference(reference)
                .status(TransactionStatus.SUCCESS)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Transaction createTransferDebit(Long accountId, BigDecimal amount, UUID reference,
                                                   UUID transferReference, String description) {
        return Transaction.builder()
                .accountId(accountId)
                .type(TransactionType.TRANSFER_DEBIT)
                .amount(amount)
                .reference(reference)
                .status(TransactionStatus.SUCCESS)
                .description(description)
                .transferReference(transferReference)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Transaction createTransferCredit(Long accountId, BigDecimal amount, UUID reference,
                                                    UUID transferReference, String description) {
        return Transaction.builder()
                .accountId(accountId)
                .type(TransactionType.TRANSFER_CREDIT)
                .amount(amount)
                .reference(reference)
                .status(TransactionStatus.SUCCESS)
                .description(description)
                .transferReference(transferReference)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
