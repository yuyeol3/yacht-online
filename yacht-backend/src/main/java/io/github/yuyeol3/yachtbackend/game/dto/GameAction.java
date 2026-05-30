package io.github.yuyeol3.yachtbackend.game.dto;

import io.github.yuyeol3.yachtbackend.game.MessageType;

import java.util.List;


public record GameAction(
        MessageType type,
        Long roomId,
        List<Integer> keepIndices,
        String scoreCategory
) {
}
