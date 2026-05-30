package io.github.yuyeol3.yachtbackend.game.dto;

import io.github.yuyeol3.yachtbackend.game.MessageType;


public record  SocketResponse<T>(
    MessageType type,
    T data
) {
}
