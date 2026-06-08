package com.janconnect.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TokenResponse {

    private final String       accessToken;
    private final String       refreshToken;
    private final String       tokenType;
    private final long         expiresIn;
    private final String       username;
    private final List<String> roles;
}
