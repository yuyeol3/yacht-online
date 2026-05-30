package io.github.yuyeol3.yachtbackend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(min=6, max=20) String loginId,
        @NotBlank @Size(min=6, max=20) String password
) {
}
