package com.novobanco.account.domain.model;

import com.novobanco.account.domain.enums.AccountStatus;
import com.novobanco.account.domain.enums.AccountType;
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
public class Account {
    private Long id;
    private UUID uuid;
    private String accountNumber;
    private Client client;
    private AccountType type;
    private String currency;
    private BigDecimal balance;
    private AccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
