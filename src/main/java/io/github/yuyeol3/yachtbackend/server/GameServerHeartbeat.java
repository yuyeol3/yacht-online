package io.github.yuyeol3.yachtbackend.server;

import io.github.yuyeol3.yachtbackend.config.role.GameServerOnly;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@GameServerOnly
@ConditionalOnProperty(name = {
        "yacht.game-server.registry.enabled"
}, havingValue = "true")
public class GameServerHeartbeat {
    private final GameServerRegistryService gameServerRegistryService;

    @Scheduled(fixedDelayString = "${yacht.game-server.heartbeat-interval-ms:10000}")
    public void heartbeat() {
        gameServerRegistryService.registerCurrentServer();
    }
}
