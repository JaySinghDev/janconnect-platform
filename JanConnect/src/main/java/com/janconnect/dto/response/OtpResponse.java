package com.janconnect.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpResponse {

    private final String message;

    /** Exposed in DEV only. Remove / null this out when email/SMS delivery is wired. */
    private final String otp;

    private final long expiresInSeconds;
}
