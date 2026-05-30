package io.github.yuyeol3.yachtbackend.gameroom.dto;

public record ToggleResponse(
    Long userId,
    boolean toggleResult
) {
}
