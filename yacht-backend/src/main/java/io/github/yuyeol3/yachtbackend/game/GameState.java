package io.github.yuyeol3.yachtbackend.game;

import io.github.yuyeol3.yachtbackend.game.dto.PlayedResponse;
import io.github.yuyeol3.yachtbackend.game.dto.UserScoreBoard;
import lombok.Builder;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
public record GameState(
        LocalDateTime startedAt,
        LocalDateTime turnTimeoutTime,
        Long roomId,
        Long curTurnUserId,
        int leftRollCnt,
        int round,
        int turn,
        List<Long> turnList,
        List<Integer> dice,
        List<Boolean> kept,
        Map<Long, UserScoreBoard> scores,
        List<PlayedResponse> playedResults,
        Long leftMillis
) {
    public GameState {
        if (turnTimeoutTime == null)
            leftMillis = 0L;
        else
            leftMillis = Duration.between(LocalDateTime.now(), turnTimeoutTime).toMillis();
    }
}
