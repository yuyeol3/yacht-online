package io.github.yuyeol3.yachtbackend.game;

import io.github.yuyeol3.yachtbackend.game.dto.SocketResponse;
import io.github.yuyeol3.yachtbackend.game.dto.UserScoreBoard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class GameTimerService {
    private final GameStateRepository gameStateRepository;
    private final TaskScheduler taskScheduler;
    private final SimpMessagingTemplate template;
    private final GameUtil gameUtil;
    private final GameResultService gameResultService;

    private final Map<Long, ScheduledFuture<?>> roomTimers = new ConcurrentHashMap<>();


    public void startTurnTimer(Function<FuncArgs, GameState> task, Long roomId, int expectedRound, int expectedTurn) {
        cancelTimer(roomId);

        Runnable timeoutTask = () -> handleTimeOut(task, roomId, expectedRound, expectedTurn);
        Instant executionTime = Instant.now().plus(gameUtil.getTurnLimitMinutes(), ChronoUnit.MINUTES);

        ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(timeoutTask, executionTime);
        roomTimers.put(roomId, scheduledFuture);
    }

    public void cancelTimer(Long roomId) {
        ScheduledFuture<?> prevTask = roomTimers.get(roomId);
        if (prevTask != null) {
            prevTask.cancel(false);
            roomTimers.remove(roomId);
        }
    }

    private void handleTimeOut(Function<FuncArgs, GameState> task, Long roomId, int expectedRound, int expectedTurn) {
        try {
            GameState newState = gameStateRepository.update(roomId, state ->{
                if (state.round() != expectedRound || state.turn() != expectedTurn || state.round() >= 13) {
                    return state;
                }

                UserScoreBoard myBoard = state.scores().get(state.curTurnUserId());
                String targetCategory = gameUtil.getFirstEmptyCategory(myBoard);

                return task.apply(new FuncArgs(state, state.curTurnUserId(), targetCategory, 0));
            });

            if (newState.round() >= 13) {
                cancelTimer(roomId);
                gameResultService.saveGameResults(newState);
                template.convertAndSend("/sub/rooms/" + roomId, new SocketResponse<>(
                        MessageType.GAME_OVER, newState
                ));
            }
            else {
                startTurnTimer(task, roomId, newState.round(), newState.turn());
                template.convertAndSend("/sub/rooms/" + roomId, new SocketResponse<>(MessageType.TIME_OUT, newState));
            }
        }
        catch (Exception e) {
            log.error("Timeout handling error", e);
        }
    }

    public record FuncArgs(
            GameState state,
            Long userId,
            String category,
            int score
    ) {}

}
