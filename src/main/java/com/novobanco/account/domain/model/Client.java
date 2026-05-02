package com.novobanco.account.domain.model;

import com.novobanco.account.domain.enums.IdentificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Client {
    private Long id;
    private UUID uuid;
    private String fullName;
    private String email;
    private String identification;
    private IdentificationType typeIdentification;
    private String phone;
    private String address;
    private String gender;
    private LocalDateTime createdAt;
}
