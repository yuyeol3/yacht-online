package io.github.yuyeol3.yachtbackend.gameroom;

import io.github.yuyeol3.yachtbackend.error.BusinessException;
import io.github.yuyeol3.yachtbackend.error.ErrorCode;
import io.github.yuyeol3.yachtbackend.gameroom.dto.ParticipatedState;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ParticipatedRepository {
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, ParticipatedState>> roomParticipants;
    private final ConcurrentHashMap<Long, Long> userCurrentRoom;

    public ParticipatedRepository() {
        this.roomParticipants = new ConcurrentHashMap<>();
        this.userCurrentRoom = new ConcurrentHashMap<>();

    }

    public void save(Long roomId, Long userId, String userNick) {
        if (userCurrentRoom.containsKey(userId)) {
            return;
        }

        roomParticipants.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .put(userId, new ParticipatedState(userId, userNick, false));

        userCurrentRoom.put(userId, roomId);
    }

    public void leave(Long roomId, Long userId) {
        Map<Long, ParticipatedState> participants = roomParticipants.get(roomId);
        if (participants != null) {
            participants.remove(userId);

            if (participants.isEmpty()) {
                roomParticipants.remove(roomId);
            }
        }

        userCurrentRoom.remove(userId);
    }

    public Optional<ParticipatedState> findByMemberId(Long userId) {
        Long roomId =  userCurrentRoom.get(userId);

        if (roomId == null) {
            return Optional.empty();
        }

        ConcurrentHashMap<Long, ParticipatedState> participants = roomParticipants.get(roomId);

        if (participants == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(participants.get(userId));
    }

    public Optional<ParticipatedState> findByMemberIdAndRoomId(Long roomId, Long userId) {
        Map<Long, ParticipatedState> participants = roomParticipants.get(roomId);
        if (participants == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(participants.get(userId));
    }


    public boolean toggleReady(Long roomId, Long userId) {
        Map<Long, ParticipatedState> participants = roomParticipants.get(roomId);
        if (participants == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        return participants.compute(userId, (key, oldState)->{
            if (oldState == null) throw new BusinessException(ErrorCode.NOT_FOUND);
            return oldState.toBuilder().isReady(!oldState.isReady()).build();
        }).isReady();
    }

    public void resetReadyAll(Long roomId) {
        Map<Long, ParticipatedState> participants = roomParticipants.get(roomId);
        if (participants == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        participants.replaceAll((userId, oldState)->
                oldState.toBuilder().isReady(false).build()
        );
    }


    // 불변 리스트 반환
    public List<ParticipatedState> findMembersByRoomId(Long roomId) {
        Map<Long, ParticipatedState> participants = roomParticipants.get(roomId);
        if (participants == null) {
            return List.of();
        }
        return List.copyOf(participants.values());
    }

    public Long findRoomIdByUserId(Long userId) {
        return userCurrentRoom.get(userId);
    }

    public int count(Long roomId) {
        Map<Long, ParticipatedState> participants = roomParticipants.get(roomId);
        return  participants == null ? 0 : participants.size();
    }

}
