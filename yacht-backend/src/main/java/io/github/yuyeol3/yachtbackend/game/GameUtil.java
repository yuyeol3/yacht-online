package io.github.yuyeol3.yachtbackend.game;

import io.github.yuyeol3.yachtbackend.game.dto.UserScoreBoard;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Getter
public class GameUtil {

    private final int turnLimitMinutes;
    private final int userLimitPerRoom;

    public GameUtil(
            @Value("${game.rule.turn_limit_minutes}") int turnLimitMinutes,
            @Value("${game.rule.user_limit_per_room}") int userLimitPerRoom
    ) {
        this.turnLimitMinutes = turnLimitMinutes;
        this.userLimitPerRoom = userLimitPerRoom;
    }

    public static class UserScore {
        Long userId;
        int score;

        public UserScore(Long userId, int score) {
            this.userId = userId;
            this.score = score;
        }
    }

    // 내림차순 정렬된 userScores 리스트 받아 등수 배열을 반환하는 함수
    public int[] calcRanks(UserScore[] userScores, int totalUser) {
        int[] ranks =  new int[totalUser];
        for (int i = 0; i < totalUser; i++) {
            if (i == 0) ranks[i] = 1;
            else
                ranks[i] = ranks[i - 1] + (userScores[i].score == userScores[i - 1].score ? 0 : 1);
        }
        return ranks;
    }

    public boolean calcIsDraw(int[] ranks, int totalUser, int leftUser) {
        boolean isDraw = true;

        for (int i = 0; i < totalUser; i++) {
            isDraw = isDraw && (ranks[i] == 1); // 모든 사람이 1등이면 모두 무승부
        }
        // 방에 사람이 혼자 남아있으면 무승부 아님
        isDraw = isDraw && !(leftUser == 1);
        return isDraw;
    }

    public String getFirstEmptyCategory(UserScoreBoard board) {
        if (board.ones() == null) return "ONES";
        if (board.twos() == null) return "TWOS";
        if (board.threes() == null) return "THREES";
        if (board.fours() == null) return "FOURS";
        if (board.fives() == null) return "FIVES";
        if (board.sixes() == null) return "SIXES";
        if (board.choice() == null) return "CHOICE";
        if (board.fourOfAKind() == null) return "FOUR_OF_A_KIND";
        if (board.fullHouse() == null) return "FULL_HOUSE";
        if (board.sStraight() == null) return "S_STRAIGHT";
        if (board.lStraight() == null) return "L_STRAIGHT";
        return "YACHT";
    }

    public int calculateScore(List<Integer> dice, String category) {

        int[] scores = new int[7];

        for (int i = 0; i < dice.size(); i++) {
            scores[dice.get(i)]++;
        }

        return switch (category) {
            case "ONES" -> scores[1];
            case "TWOS" -> scores[2] * 2;
            case "THREES" -> scores[3] * 3;
            case "FOURS" -> scores[4] * 4;
            case "FIVES" ->  scores[5] * 5;
            case "SIXES" ->  scores[6] * 6;
            case "CHOICE" -> dice.stream().mapToInt(i->i).sum();
            case "FOUR_OF_A_KIND" -> calcFourOfAKind(dice, scores);
            case "FULL_HOUSE" -> calcFullHouse(dice, scores);
            case "S_STRAIGHT" -> calcSStraight(dice, scores);
            case "L_STRAIGHT" -> calcLStraight(dice, scores);
            case "YACHT" -> calcYacht(dice, scores);
            default -> 0;
        };
    }

    private int calcFourOfAKind(List<Integer> dice, int[] scores) {
        for (int i = 1; i <= 6; i++) {
            if (scores[i] >= 4) {
                return dice.stream().mapToInt(n->n).sum();
            }
        }

        return 0;
    }

    private int calcFullHouse(List<Integer> dice, int[] scores) {
        for (int i = 1;  i <= 6; i++) {
            for (int j = i+1; j <= 6; j++) {
                if (scores[i] == 2 && scores[j] == 3 ||
                    scores[i] == 3 && scores[j] == 2
                ) return dice.stream().mapToInt(n->n).sum();
            }
        }
        return 0;
    }

    private int calcSStraight(List<Integer> dice, int[] scores) {
        for (int i = 1; i <= 3; i++) {
            boolean flag = true;
            for (int j = i; j < 4 + i; j++) {
                flag = flag && (scores[j] >= 1);
            }
            if (flag) {
                return 15;
            }
        }

        return 0;
    }

    private int calcLStraight(List<Integer> dice, int[] scores) {
        for (int i = 1; i <= 2; i++) {
            boolean flag = true;
            for (int j = i; j < 5 + i; j++) {
                flag = flag && (scores[j] >= 1);
            }
            if (flag) {
                return 30;
            }
        }

        return 0;
    }

    private int calcYacht(List<Integer> dice, int[] scores) {
        for (int i = 1; i <= 6; i++) {
            if (scores[i] == 5)
                return 50;
        }

        return 0;
    }

}
