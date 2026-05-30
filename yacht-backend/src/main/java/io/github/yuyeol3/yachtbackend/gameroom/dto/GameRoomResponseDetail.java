package io.github.yuyeol3.yachtbackend.gameroom.dto;

import io.github.yuyeol3.yachtbackend.gameroom.GameStatus;
import io.github.yuyeol3.yachtbackend.server.GameServerInfo;
import lombok.Builder;

import java.util.List;

@Builder
public record GameRoomResponseDetail(
        Long id,
        String roomName,
        GameStatus status,
        GameServerInfo gameServer,
        ParticipatedState host,
        Long participatedUsers,
        List<ParticipatedState> participants
) {
}
