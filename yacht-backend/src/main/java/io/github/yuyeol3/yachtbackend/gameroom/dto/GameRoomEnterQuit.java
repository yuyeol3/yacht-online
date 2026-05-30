package io.github.yuyeol3.yachtbackend.gameroom.dto;

import io.github.yuyeol3.yachtbackend.gameroom.EnterQuit;
import lombok.Builder;

@Builder
public record GameRoomEnterQuit(
        EnterQuit action,
        ParticipatedState user,
        ParticipatedState newHost
) {
}
