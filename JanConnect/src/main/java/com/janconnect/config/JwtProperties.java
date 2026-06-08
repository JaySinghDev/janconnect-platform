package com.janconnect.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    /** HMAC-SHA256 signing secret — must be at least 32 characters. Override via env var JWT_SECRET in prod. */
    private String secret;

    /** Access token lifetime in milliseconds. Default: 1 hour. */
    private long expirationMs = 3_600_000L;

    /** Refresh token lifetime in milliseconds. Default: 7 days. */
    private long refreshExpirationMs = 604_800_000L;
}
