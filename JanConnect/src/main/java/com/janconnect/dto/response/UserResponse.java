package com.janconnect.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.janconnect.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private final Long          id;
    private final String        username;
    private final String        email;
    private final String        fullName;
    private final String        mobileNumber;
    private final Set<String>   roles;
    private final boolean       enabled;
    private final LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .mobileNumber(user.getMobileNumber())
                .roles(user.getRoles().stream()
                        .map(r -> r.getRoleName())
                        .collect(Collectors.toSet()))
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
