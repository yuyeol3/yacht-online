package io.github.yuyeol3.yachtbackend.server;

import io.github.yuyeol3.yachtbackend.error.BusinessException;
import io.github.yuyeol3.yachtbackend.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GameServerRegistryService {
    private static final String GAME_SERVERS_KEY = "game_servers";
    private static final String ROOM_SERVER_KEY_PREFIX = "room:";
    private static final String SERVER_KEY_PREFIX = "game_server:";

    private final StringRedisTemplate redisTemplate;
    private final GameServerProperties properties;

    @Value("${yacht.game-server.registry.enabled:false}")
    private boolean registryEnabled;

    public GameServerInfo selectLeastLoadedServer() {
        return findLeastLoadedServer();
    }

    public void bindCreatedRoom(Long roomId, String serverId) {
        bindRoom(roomId, serverId);
        incrementRoomCount(serverId);
    }

    public GameServerInfo findServerForRoom(Long roomId, String fallbackServerId) {
        String serverId = registryEnabled
                ? redisTemplate.opsForValue().get(roomServerKey(roomId))
                : fallbackServerId;

        if (serverId == null || serverId.isBlank()) {
            serverId = fallbackServerId;
        }
        return findServer(serverId);
    }

    public void registerCurrentServer() {
        if (!registryEnabled) {
            return;
        }

        String serverKey = serverKey(properties.getServerId());
        redisTemplate.opsForHash().putAll(serverKey, Map.of(
                "serverId", properties.getServerId(),
                "wsUrl", properties.getWsUrl(),
                "lastHeartbeat", Instant.now().toString()
        ));
        redisTemplate.expire(serverKey, Duration.ofSeconds(properties.getHeartbeatTtlSeconds()));
        Double currentScore = redisTemplate.opsForZSet().score(GAME_SERVERS_KEY, properties.getServerId());
        if (currentScore == null) {
            redisTemplate.opsForZSet().add(GAME_SERVERS_KEY, properties.getServerId(), 0);
        }
    }

    public void releaseRoom(Long roomId, String serverId) {
        if (!registryEnabled) {
            return;
        }

        redisTemplate.delete(roomServerKey(roomId));
        if (serverId != null && !serverId.isBlank()) {
            redisTemplate.opsForZSet().incrementScore(GAME_SERVERS_KEY, serverId, -1);
        }
    }

    public boolean isCurrentServerOwner(Long roomId, String fallbackServerId) {
        if (!registryEnabled) {
            return true;
        }

        GameServerInfo owner = findServerForRoom(roomId, fallbackServerId);
        return Objects.equals(owner.serverId(), properties.getServerId());
    }

    private GameServerInfo findLeastLoadedServer() {
        if (!registryEnabled) {
            return currentServer();
        }

        Set<ZSetOperations.TypedTuple<String>> candidates =
                redisTemplate.opsForZSet().rangeWithScores(GAME_SERVERS_KEY, 0, -1);

        if (candidates == null || candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        for (ZSetOperations.TypedTuple<String> candidate : candidates) {
            String serverId = candidate.getValue();
            if (serverId != null && Boolean.TRUE.equals(redisTemplate.hasKey(serverKey(serverId)))) {
                return findServer(serverId);
            }
            redisTemplate.opsForZSet().remove(GAME_SERVERS_KEY, serverId);
        }

        throw new BusinessException(ErrorCode.NOT_FOUND);
    }

    private GameServerInfo findServer(String serverId) {
        if (!registryEnabled || Objects.equals(serverId, properties.getServerId())) {
            return currentServer();
        }

        Object wsUrl = redisTemplate.opsForHash().get(serverKey(serverId), "wsUrl");
        if (wsUrl == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        return new GameServerInfo(serverId, wsUrl.toString());
    }

    private void bindRoom(Long roomId, String serverId) {
        if (!registryEnabled) {
            return;
        }
        redisTemplate.opsForValue().set(roomServerKey(roomId), serverId);
    }

    private void incrementRoomCount(String serverId) {
        if (!registryEnabled) {
            return;
        }
        redisTemplate.opsForZSet().incrementScore(GAME_SERVERS_KEY, serverId, 1);
    }

    private GameServerInfo currentServer() {
        return new GameServerInfo(properties.getServerId(), properties.getWsUrl());
    }

    private String roomServerKey(Long roomId) {
        return ROOM_SERVER_KEY_PREFIX + roomId + ":server";
    }

    private String serverKey(String serverId) {
        return SERVER_KEY_PREFIX + serverId;
    }
}
