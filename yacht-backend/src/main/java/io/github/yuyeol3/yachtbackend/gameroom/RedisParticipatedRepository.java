package io.github.yuyeol3.yachtbackend.gameroom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yuyeol3.yachtbackend.error.BusinessException;
import io.github.yuyeol3.yachtbackend.error.ErrorCode;
import io.github.yuyeol3.yachtbackend.gameroom.dto.ParticipatedState;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "yacht.participation-store", havingValue = "redis")
public class RedisParticipatedRepository implements ParticipatedRepository {
    private static final String ROOM_KEY_PREFIX = "room:";
    private static final String USER_KEY_PREFIX = "user:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void save(Long roomId, Long userId, String userNick) {
        if (findRoomIdByUserId(userId) != null) {
            return;
        }

        ParticipatedState state = new ParticipatedState(userId, userNick, false);
        redisTemplate.opsForHash().put(participantsKey(roomId), userId.toString(), serialize(state));
        redisTemplate.opsForValue().set(userRoomKey(userId), roomId.toString());
    }

    @Override
    public void leave(Long roomId, Long userId) {
        redisTemplate.opsForHash().delete(participantsKey(roomId), userId.toString());
        redisTemplate.delete(userRoomKey(userId));

        if (count(roomId) == 0) {
            redisTemplate.delete(participantsKey(roomId));
        }
    }

    @Override
    public Optional<ParticipatedState> findByMemberId(Long userId) {
        Long roomId = findRoomIdByUserId(userId);
        if (roomId == null) {
            return Optional.empty();
        }
        return findByMemberIdAndRoomId(roomId, userId);
    }

    @Override
    public Optional<ParticipatedState> findByMemberIdAndRoomId(Long roomId, Long userId) {
        Object raw = redisTemplate.opsForHash().get(participantsKey(roomId), userId.toString());
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.of(deserialize(raw.toString()));
    }

    @Override
    public boolean toggleReady(Long roomId, Long userId) {
        ParticipatedState oldState = findByMemberIdAndRoomId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        ParticipatedState newState = oldState.toBuilder()
                .isReady(!oldState.isReady())
                .build();
        redisTemplate.opsForHash().put(participantsKey(roomId), userId.toString(), serialize(newState));
        return newState.isReady();
    }

    @Override
    public void resetReadyAll(Long roomId) {
        Map<Object, Object> participants = redisTemplate.opsForHash().entries(participantsKey(roomId));
        if (participants.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        participants.forEach((userId, rawState) -> {
            ParticipatedState oldState = deserialize(rawState.toString());
            ParticipatedState newState = oldState.toBuilder().isReady(false).build();
            redisTemplate.opsForHash().put(participantsKey(roomId), userId, serialize(newState));
        });
    }

    @Override
    public List<ParticipatedState> findMembersByRoomId(Long roomId) {
        return redisTemplate.opsForHash()
                .values(participantsKey(roomId))
                .stream()
                .map(value -> deserialize(value.toString()))
                .toList();
    }

    @Override
    public Long findRoomIdByUserId(Long userId) {
        String rawRoomId = redisTemplate.opsForValue().get(userRoomKey(userId));
        return rawRoomId == null ? null : Long.valueOf(rawRoomId);
    }

    @Override
    public int count(Long roomId) {
        Long size = redisTemplate.opsForHash().size(participantsKey(roomId));
        return size == null ? 0 : size.intValue();
    }

    private String serialize(ParticipatedState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize participant state", e);
        }
    }

    private ParticipatedState deserialize(String value) {
        try {
            return objectMapper.readValue(value, ParticipatedState.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize participant state", e);
        }
    }

    private String participantsKey(Long roomId) {
        return ROOM_KEY_PREFIX + roomId + ":participants";
    }

    private String userRoomKey(Long userId) {
        return USER_KEY_PREFIX + userId + ":room";
    }
}
