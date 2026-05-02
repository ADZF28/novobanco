package com.novobanco.account.infrastructure.adapter.in.web.dto.response;

import com.novobanco.account.domain.model.Transaction;

import java.time.LocalDate;
import java.util.List;

public record OutgoingTransfersResponse(
        String identification,
        LocalDate startDate,
        LocalDate endDate,
        long count,
        List<TransactionResponse> transfers
) {
    public static OutgoingTransfersResponse from(String identification, LocalDate startDate,
                                                  LocalDate endDate, List<Transaction> transactions) {
        List<TransactionResponse> transfers = transactions.stream()
                .map(TransactionResponse::from)
                .toList();
        return new OutgoingTransfersResponse(identification, startDate, endDate, transfers.size(), transfers);
    }
}
