package io.github.yuyeol3.yachtbackend.gameroom.dto;

import io.github.yuyeol3.yachtbackend.gameroom.GameStatus;
import io.github.yuyeol3.yachtbackend.server.GameServerInfo;
import lombok.Builder;

@Builder
public record GameRoomResponse(
        Long id,
        String roomName,
        String hostNickName,
        Long participatedUsers,
        GameStatus status,
        GameServerInfo gameServer
) {
//    static GameRoomResponse from(GameRoom gameRoom) {
//        return GameRoomResponse.builder()
//                .id(gameRoom.getId())
//                .roomName(gameRoom.getRoomName())
//                .hostNickName(gameRoom.getHost().getNickname())
//                .participatedUsers(gameRoom.getParticipants().stream().count())
//                .build();
//    }
}
