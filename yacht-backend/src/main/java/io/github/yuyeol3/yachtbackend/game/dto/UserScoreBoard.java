package io.github.yuyeol3.yachtbackend.game.dto;

import lombok.Builder;

@Builder(toBuilder = true)
public record UserScoreBoard(
        Integer ones,
        Integer twos,
        Integer threes,
        Integer fours,
        Integer fives,
        Integer sixes,
        Integer choice,
        Integer fourOfAKind,
        Integer fullHouse,
        Integer sStraight,
        Integer lStraight,
        Integer yacht,

        Integer bonus,
        Integer upperScore,
        Integer total
) {
    public static UserScoreBoard empty() {
        return UserScoreBoard.builder()
                .upperScore(0).total(0)
                .build();
    }

    public static UserScoreBoard update(UserScoreBoard board, String category, int score) {

        return (switch (category) {
            case "ONES" -> board.toBuilder()
                    .ones(score)
                    .upperScore(board.upperScore() + score)
                    .build();
            case "TWOS" -> board.toBuilder()
                    .twos(score)
                    .upperScore(board.upperScore() + score)
                    .build();
            case "THREES" -> board.toBuilder()
                    .threes(score)
                    .upperScore(board.upperScore() + score)
                    .build();
            case "FOURS" -> board.toBuilder()
                    .fours(score)
                    .upperScore(board.upperScore() + score)
                    .build();
            case "FIVES" ->  board.toBuilder()
                    .fives(score)
                    .upperScore(board.upperScore() + score)
                    .build();
            case "SIXES" ->  board.toBuilder()
                    .sixes(score)
                    .upperScore(board.upperScore() + score)
                    .build();
            case "CHOICE" -> board.toBuilder().choice(score).build();
            case "FOUR_OF_A_KIND" -> board.toBuilder().fourOfAKind(score).build();
            case "FULL_HOUSE" -> board.toBuilder().fullHouse(score).build();
            case "S_STRAIGHT" -> board.toBuilder().sStraight(score).build();
            case "L_STRAIGHT" -> board.toBuilder().lStraight(score).build();
            case "YACHT" -> board.toBuilder().yacht(score).build();
            default -> board;
        }).toBuilder().total(board.total + score).build();
    }


    public boolean hasScore(String category) {
        return switch (category) {
            case "ONES" -> ones != null;
            case "TWOS" -> twos != null;
            case "THREES" -> threes != null;
            case "FOURS" -> fours != null;
            case "FIVES" ->  fives != null;
            case "SIXES" ->  sixes != null;
            case "CHOICE" -> choice != null;
            case "FOUR_OF_A_KIND" -> fourOfAKind != null;
            case "FULL_HOUSE" -> fullHouse != null;
            case "S_STRAIGHT" -> sStraight != null;
            case "L_STRAIGHT" -> lStraight != null;
            case "YACHT" -> yacht != null;
            default -> true;
        };
    }

}
