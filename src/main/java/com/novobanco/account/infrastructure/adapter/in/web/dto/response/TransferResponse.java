package com.novobanco.account.infrastructure.adapter.in.web.dto.response;

import com.novobanco.account.domain.model.Transaction;

import java.util.List;

public record TransferResponse(
        TransactionResponse debit,
        TransactionResponse credit
) {
    public static TransferResponse from(List<Transaction> transactions) {
        return new TransferResponse(
                TransactionResponse.from(transactions.get(0)),
                TransactionResponse.from(transactions.get(1))
        );
    }
}
