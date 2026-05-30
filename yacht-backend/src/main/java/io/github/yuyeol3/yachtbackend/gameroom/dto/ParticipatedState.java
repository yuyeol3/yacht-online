package io.github.yuyeol3.yachtbackend.gameroom.dto;

import lombok.Builder;

@Builder(toBuilder = true)
public record ParticipatedState(
        long userId,
        String userNick,
        boolean isReady
) {
}
