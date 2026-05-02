package com.novobanco.account.infrastructure.adapter.in.web.dto.response;

public record ApiErrorResponse(
        int code,
        String message
) {
    public static ApiErrorResponse of(int code, String message) {
        return new ApiErrorResponse(code, message);
    }
}
