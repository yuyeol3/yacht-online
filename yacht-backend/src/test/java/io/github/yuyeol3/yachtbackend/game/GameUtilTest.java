package io.github.yuyeol3.yachtbackend.game;

import io.github.yuyeol3.yachtbackend.game.dto.UserScoreBoard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameUtilTest {

    private final GameUtil gameUtil = new GameUtil(3, 4);

    @Test
    void calcRanks_assigns_same_rank_for_tied_scores() {
        GameUtil.UserScore[] userScores = new GameUtil.UserScore[] {
                new GameUtil.UserScore(1L, 100),
                new GameUtil.UserScore(2L, 100),
                new GameUtil.UserScore(3L, 80),
                new GameUtil.UserScore(4L, 70),
                new GameUtil.UserScore(5L, 70)
        };

        int[] ranks = gameUtil.calcRanks(userScores, 5);

        assertThat(ranks).containsExactly(1, 1, 2, 3, 3);
    }

    @Test
    void calcIsDraw_returns_true_when_all_users_are_rank1_and_multiple_left() {
        boolean isDraw = gameUtil.calcIsDraw(new int[] {1, 1, 1}, 3, 3);

        assertThat(isDraw).isTrue();
    }

    @Test
    void calcIsDraw_returns_false_when_only_one_user_left() {
        boolean isDraw = gameUtil.calcIsDraw(new int[] {1, 1}, 2, 1);

        assertThat(isDraw).isFalse();
    }

    @Test
    void calcIsDraw_returns_false_when_any_user_is_not_rank1() {
        boolean isDraw = gameUtil.calcIsDraw(new int[] {1, 2, 3}, 3, 3);

        assertThat(isDraw).isFalse();
    }

    @Test
    void getFirstEmptyCategory_returns_ones_for_empty_board() {
        String category = gameUtil.getFirstEmptyCategory(UserScoreBoard.empty());

        assertThat(category).isEqualTo("ONES");
    }

    @Test
    void getFirstEmptyCategory_returns_first_null_category_in_order() {
        UserScoreBoard board = UserScoreBoard.builder()
                .ones(1)
                .threes(3)
                .upperScore(4)
                .total(4)
                .build();

        String category = gameUtil.getFirstEmptyCategory(board);

        assertThat(category).isEqualTo("TWOS");
    }

    @Test
    void getFirstEmptyCategory_returns_yacht_when_all_categories_filled() {
        UserScoreBoard board = UserScoreBoard.builder()
                .ones(1).twos(2).threes(3).fours(4).fives(5).sixes(6)
                .choice(20).fourOfAKind(18).fullHouse(15).sStraight(15).lStraight(30).yacht(50)
                .bonus(0).upperScore(21).total(149)
                .build();

        String category = gameUtil.getFirstEmptyCategory(board);

        assertThat(category).isEqualTo("YACHT");
    }

    @Test
    void calculateScore_returns_sum_for_choice() {
        int score = gameUtil.calculateScore(List.of(1, 2, 3, 4, 5), "CHOICE");

        assertThat(score).isEqualTo(15);
    }

    @Test
    void calculateScore_returns_score_for_four_of_a_kind_when_present() {
        int score = gameUtil.calculateScore(List.of(2, 2, 2, 2, 5), "FOUR_OF_A_KIND");

        assertThat(score).isEqualTo(13);
    }

    @Test
    void calculateScore_returns_zero_for_four_of_a_kind_when_absent() {
        int score = gameUtil.calculateScore(List.of(1, 2, 2, 3, 5), "FOUR_OF_A_KIND");

        assertThat(score).isZero();
    }

    @Test
    void calculateScore_returns_score_for_full_house_when_present() {
        int score = gameUtil.calculateScore(List.of(3, 3, 3, 5, 5), "FULL_HOUSE");

        assertThat(score).isEqualTo(19);
    }

    @Test
    void calculateScore_returns_zero_for_full_house_when_absent() {
        int score = gameUtil.calculateScore(List.of(1, 1, 1, 1, 2), "FULL_HOUSE");

        assertThat(score).isZero();
    }

    @Test
    void calculateScore_returns_fixed_score_for_small_straight() {
        int score = gameUtil.calculateScore(List.of(1, 2, 3, 4, 6), "S_STRAIGHT");

        assertThat(score).isEqualTo(15);
    }

    @Test
    void calculateScore_returns_zero_for_small_straight_when_absent() {
        int score = gameUtil.calculateScore(List.of(1, 2, 2, 4, 6), "S_STRAIGHT");

        assertThat(score).isZero();
    }

    @Test
    void calculateScore_returns_fixed_score_for_large_straight() {
        int score = gameUtil.calculateScore(List.of(2, 3, 4, 5, 6), "L_STRAIGHT");

        assertThat(score).isEqualTo(30);
    }

    @Test
    void calculateScore_returns_zero_for_large_straight_when_absent() {
        int score = gameUtil.calculateScore(List.of(1, 2, 3, 4, 6), "L_STRAIGHT");

        assertThat(score).isZero();
    }

    @Test
    void calculateScore_returns_50_for_yacht_when_all_dice_match() {
        int score = gameUtil.calculateScore(List.of(6, 6, 6, 6, 6), "YACHT");

        assertThat(score).isEqualTo(50);
    }

    @Test
    void calculateScore_returns_zero_for_unknown_category() {
        int score = gameUtil.calculateScore(List.of(1, 2, 3, 4, 5), "UNKNOWN");

        assertThat(score).isZero();
    }
}
