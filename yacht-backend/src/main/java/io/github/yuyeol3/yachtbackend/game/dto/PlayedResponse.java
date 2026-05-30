package io.github.yuyeol3.yachtbackend.game.dto;

import io.github.yuyeol3.yachtbackend.game.GameResult;
import io.github.yuyeol3.yachtbackend.game.Played;
import lombok.Builder;

@Builder
public record PlayedResponse(
        Long id,
        String nickname,
        Integer score,
        Integer rank,
        GameResult result
) {
    public static PlayedResponse from(Played result) {
        return PlayedResponse.builder()
                .id(result.getId())
                .nickname(result.getUser().getNickname())
                .score(result.getScore())
                .rank(result.getRank())
                .result(result.getGameResult())
                .build();
    }
}
