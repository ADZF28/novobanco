package com.novobanco.account.infrastructure.adapter.in.web.dto.response;

import com.novobanco.account.domain.enums.IdentificationType;
import com.novobanco.account.domain.model.Account;
import com.novobanco.account.domain.model.Client;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ClientDetailResponse(
        UUID uuid,
        String fullName,
        String email,
        String identification,
        IdentificationType typeIdentification,
        String phone,
        String address,
        String gender,
        LocalDateTime createdAt,
        long accountCount,
        List<AccountSummaryResponse> accounts
) {
    public static ClientDetailResponse from(Client client, List<Account> accounts, boolean includeAccounts) {
        return new ClientDetailResponse(
                client.getUuid(),
                client.getFullName(),
                client.getEmail(),
                client.getIdentification(),
                client.getTypeIdentification(),
                client.getPhone(),
                client.getAddress(),
                client.getGender(),
                client.getCreatedAt(),
                accounts.size(),
                includeAccounts ? accounts.stream().map(AccountSummaryResponse::from).toList() : null
        );
    }
}
