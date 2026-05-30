package io.github.yuyeol3.yachtbackend.server;

import lombok.Builder;

@Builder
public record GameServerInfo(
        String serverId,
        String wsUrl
) {
}
