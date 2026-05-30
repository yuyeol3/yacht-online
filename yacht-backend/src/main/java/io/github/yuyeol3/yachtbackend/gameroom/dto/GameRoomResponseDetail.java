package io.github.yuyeol3.yachtbackend.gameroom.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record GameRoomResponseDetail(
        Long id,
        String roomName,
        ParticipatedState host,
        Long participatedUsers,
        List<ParticipatedState> participants
) {
}
