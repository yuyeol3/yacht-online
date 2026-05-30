package io.github.yuyeol3.yachtbackend.game;

import io.github.yuyeol3.yachtbackend.error.BusinessException;
import io.github.yuyeol3.yachtbackend.error.ErrorCode;
import io.github.yuyeol3.yachtbackend.game.dto.GameAction;
import io.github.yuyeol3.yachtbackend.game.dto.SocketResponse;
import io.github.yuyeol3.yachtbackend.game.dto.UserScoreBoard;
import io.github.yuyeol3.yachtbackend.gameroom.GameRoomRepository;
import io.github.yuyeol3.yachtbackend.gameroom.GameRoomService;
import io.github.yuyeol3.yachtbackend.gameroom.ParticipatedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameService {

    private final GameStateRepository gameStateRepository;
    private final ParticipatedRepository participatedRepository;
    private final GameResultService gameResultService;
    private final GameUtil gameUtil;
    private final GameTimerService gameTimerService;

    public Function<GameTimerService.FuncArgs, GameState> getTimerTask() {
        return t
                    -> applyScoreAndNextTurn(t.state(), t.userId(), t.category(), t.score());
    }

    public void startInitialTimer(Long roomId) {
        gameTimerService.startTurnTimer(getTimerTask(), roomId, 1, 0);
    }

    @Transactional
    public GameState processAction(Long roomId, Long userId, GameAction action) {
        GameState state = gameStateRepository.update(roomId, currentState-> {
            if (!currentState.curTurnUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.NOT_YOUR_TURN);
            }

            return switch (action.type()) {
                case ROLL -> rollDice(currentState, action);
                case SELECT_SCORE -> selectScore(currentState, userId, action);
                case KEEP_TOGGLE -> keepToggleDice(currentState, action);
                default -> currentState;
            };
        });

        if (state.round() >= 13) {
            gameTimerService.cancelTimer(roomId);
            participatedRepository.resetReadyAll(roomId);
            state = state.toBuilder()
                    .playedResults(gameResultService.saveGameResults(state))
                    .build();
        }
        else if (action.type() == MessageType.SELECT_SCORE) {
            gameTimerService.startTurnTimer(
                   getTimerTask(), roomId, state.round(), state.turn()
            );
        }
        return state;
    }



    private GameState keepToggleDice(GameState state, GameAction action) {
        if (state.leftRollCnt() == 3)
            throw new BusinessException(ErrorCode.NOT_ROLLED);


        List<Boolean> keep = new ArrayList<>(state.kept());
        for (int keepIdx : action.keepIndices()) {
            if (0 <= keepIdx && keepIdx < 5) {
                keep.set(keepIdx, !keep.get(keepIdx));
            }
        }

        return state
                .toBuilder()
                .kept(List.copyOf(keep))
                .build();
    }

    private GameState rollDice(GameState state, GameAction action) {
        if (state.leftRollCnt() <= 0) {
            throw new BusinessException(ErrorCode.ROLL_CHANCE_OVER);
        }

        List<Integer> newDice = new ArrayList<>(state.dice());

        for (int i = 0; i < 5; i++) {
            if (!state.kept().get(i))
                newDice.set(i, (int) (Math.random() * 6) + 1);
        }

        return state.toBuilder()
                .dice(List.copyOf(newDice))
                .leftRollCnt(state.leftRollCnt() - 1)
                .build();
    }

    private GameState selectScore(GameState state, Long userId, GameAction action) {
        if (state.leftRollCnt() == 3)
            throw new BusinessException(ErrorCode.NOT_ROLLED);

        String category = action.scoreCategory();
        int score = gameUtil.calculateScore(state.dice(), category);

        UserScoreBoard myBoard = state.scores().get(userId);
        if (myBoard.hasScore(category)) {
            throw new BusinessException(ErrorCode.FILLED_SCORE);
        }

        return applyScoreAndNextTurn(state, userId, category, score);
    }

    private GameState applyScoreAndNextTurn(GameState state, Long userId, String category, int score) {
        Map<Long, UserScoreBoard> newScores = new HashMap<>(state.scores());
        UserScoreBoard myBoard = newScores.get(userId);

        UserScoreBoard newBoard = UserScoreBoard.update(myBoard, category, score);
        // 보너스 판정 로직
        if (newBoard.upperScore() >= 63 && newBoard.bonus() == null) {
            newBoard = newBoard.toBuilder()
                    .bonus(35)
                    .total(newBoard.total() + 35)
                    .build();
        }
        newScores.put(userId, newBoard);
        int nextTurn = (state.turn() + 1) % state.turnList().size();
        int nextRound = nextTurn == 0 ? state.round() + 1 : state.round();


        return state.toBuilder()
                .scores(newScores)
                .curTurnUserId(state.turnList().get(nextTurn))
                .leftRollCnt(3)
                .dice(List.of(0,0,0,0,0))
                .kept(List.of(false, false, false, false, false))
                .turnTimeoutTime(LocalDateTime.now().plusMinutes(gameUtil.getTurnLimitMinutes()))
                .round(nextRound)
                .turn(nextTurn)
                .build();
    }

    @Transactional
    public void abortGame(Long roomId) {
        gameTimerService.cancelTimer(roomId);
        gameStateRepository.remove(roomId);
    }


}
