package io.github.yuyeol3.yachtbackend.auth;

import lombok.Builder;

@Builder
public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
