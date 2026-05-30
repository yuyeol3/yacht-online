package io.github.yuyeol3.yachtbackend.game;

import io.github.yuyeol3.yachtbackend.error.BusinessException;
import io.github.yuyeol3.yachtbackend.error.ErrorCode;
import io.github.yuyeol3.yachtbackend.game.dto.GameAction;
import io.github.yuyeol3.yachtbackend.game.dto.UserScoreBoard;
import io.github.yuyeol3.yachtbackend.gameroom.ParticipatedRepository;
import io.github.yuyeol3.yachtbackend.gameroom.dto.ParticipatedState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    private GameStateRepository gameStateRepository;

    @Mock
    private ParticipatedRepository participatedRepository;

    @Mock
    private GameResultService gameResultService;

    @Mock
    private GameUtil gameUtil;

    @Mock
    private GameTimerService gameTimerService;

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameStateRepository = new GameStateRepository();
        gameService = new GameService(
                gameStateRepository,
                participatedRepository,
                gameResultService,
                gameUtil,
                gameTimerService
        );
    }

    @Test
    void processAction_not_your_turn_throws() {
        GameState state = baseState();
        gameStateRepository.save(10L, state);

        GameAction action = new GameAction(MessageType.ROLL, 10L, List.of(), null);

        assertThatThrownBy(() -> gameService.processAction(10L, 99L, action))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_YOUR_TURN);
    }

    @Test
    void selectScore_updates_state_and_restarts_timer() {
        GameState state = baseState().toBuilder()
                .leftRollCnt(2)
                .dice(List.of(1, 1, 1, 1, 1))
                .build();
        gameStateRepository.save(10L, state);

        when(gameUtil.calculateScore(state.dice(), "ONES")).thenReturn(5);
        when(gameUtil.getTurnLimitMinutes()).thenReturn(3);
        when(participatedRepository.findByMemberIdAndRoomId(10L, 2L))
                .thenReturn(Optional.of(new ParticipatedState(2L, "u2", true)));

        GameAction action = new GameAction(MessageType.SELECT_SCORE, 10L, List.of(), "ONES");

        GameState updated = gameService.processAction(10L, 1L, action);

        assertThat(updated.curTurnUserId()).isEqualTo(2L);
        assertThat(updated.leftRollCnt()).isEqualTo(3);
        assertThat(updated.dice()).containsExactly(0, 0, 0, 0, 0);
        assertThat(updated.kept()).containsExactly(false, false, false, false, false);
        assertThat(updated.scores().get(1L).total()).isEqualTo(5);

        verify(gameTimerService).startTurnTimer(any(), eq(10L), eq(updated.round()), eq(updated.turn()));
    }

    private GameState baseState() {
        Map<Long, UserScoreBoard> scores = new HashMap<>();
        scores.put(1L, UserScoreBoard.empty());
        scores.put(2L, UserScoreBoard.empty());
        return GameState.builder()
                .startedAt(LocalDateTime.now())
                .turnTimeoutTime(LocalDateTime.now().plusMinutes(3))
                .roomId(10L)
                .curTurnUserId(1L)
                .leftRollCnt(3)
                .round(1)
                .turn(0)
                .turnList(List.of(1L, 2L))
                .dice(List.of(0, 0, 0, 0, 0))
                .kept(List.of(false, false, false, false, false))
                .scores(scores)
                .build();
    }
}
