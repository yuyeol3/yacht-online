package io.github.yuyeol3.yachtbackend.gameroom.dto;

import io.github.yuyeol3.yachtbackend.server.GameServerInfo;
import lombok.Builder;

@Builder
public record GameRoomCreateResponse(
        Long roomId,
        String roomName,
        GameServerInfo gameServer
) {
}
