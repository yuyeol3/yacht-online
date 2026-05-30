package io.github.yuyeol3.yachtbackend.gameroom.dto;

import lombok.Builder;

@Builder
public record GameRoomResponse(
        Long id,
        String roomName,
        String hostNickName,
        Long participatedUsers
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
