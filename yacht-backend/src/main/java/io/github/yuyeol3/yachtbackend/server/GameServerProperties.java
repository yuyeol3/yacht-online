package io.github.yuyeol3.yachtbackend.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GameServerProperties {
    private final String serverId;
    private final String wsUrl;
    private final long heartbeatTtlSeconds;

    public GameServerProperties(
            @Value("${yacht.game-server.id:local}") String serverId,
            @Value("${yacht.game-server.ws-url:ws://localhost:8080/api/ws-stomp}") String wsUrl,
            @Value("${yacht.game-server.heartbeat-ttl-seconds:30}") long heartbeatTtlSeconds
    ) {
        this.serverId = serverId;
        this.wsUrl = wsUrl;
        this.heartbeatTtlSeconds = heartbeatTtlSeconds;
    }

    public String getServerId() {
        return serverId;
    }

    public String getWsUrl() {
        return wsUrl;
    }

    public long getHeartbeatTtlSeconds() {
        return heartbeatTtlSeconds;
    }
}
