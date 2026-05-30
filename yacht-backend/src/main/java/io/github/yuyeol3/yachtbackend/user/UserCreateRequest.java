package io.github.yuyeol3.yachtbackend.user;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Size(min=6, max=20) String loginId,
        @NotBlank @Size(min=6, max=20) String password,
        @NotBlank @Size(min=1, max=20) String nickname
) {
}
