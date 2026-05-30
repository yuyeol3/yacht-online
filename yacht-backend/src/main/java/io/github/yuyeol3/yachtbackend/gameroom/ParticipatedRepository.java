package io.github.yuyeol3.yachtbackend.gameroom;

import io.github.yuyeol3.yachtbackend.gameroom.dto.ParticipatedState;

import java.util.List;
import java.util.Optional;

public interface ParticipatedRepository {
    void save(Long roomId, Long userId, String userNick);

    void leave(Long roomId, Long userId);

    Optional<ParticipatedState> findByMemberId(Long userId);

    Optional<ParticipatedState> findByMemberIdAndRoomId(Long roomId, Long userId);

    boolean toggleReady(Long roomId, Long userId);

    void resetReadyAll(Long roomId);

    List<ParticipatedState> findMembersByRoomId(Long roomId);

    Long findRoomIdByUserId(Long userId);

    int count(Long roomId);
}
