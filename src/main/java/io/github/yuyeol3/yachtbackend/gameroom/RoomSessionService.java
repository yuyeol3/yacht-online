package io.github.yuyeol3.yachtbackend.gameroom;

import io.github.yuyeol3.yachtbackend.config.role.GameServerOnly;
import io.github.yuyeol3.yachtbackend.error.BusinessException;
import io.github.yuyeol3.yachtbackend.error.ErrorCode;
import io.github.yuyeol3.yachtbackend.game.GameService;
import io.github.yuyeol3.yachtbackend.game.GameState;
import io.github.yuyeol3.yachtbackend.game.GameStateRepository;
import io.github.yuyeol3.yachtbackend.game.GameUtil;
import io.github.yuyeol3.yachtbackend.game.dto.UserScoreBoard;
import io.github.yuyeol3.yachtbackend.gameroom.dto.GameRoomEnterQuit;
import io.github.yuyeol3.yachtbackend.gameroom.dto.ParticipatedState;
import io.github.yuyeol3.yachtbackend.gameroom.dto.ToggleResponse;
import io.github.yuyeol3.yachtbackend.server.GameServerRegistryService;
import io.github.yuyeol3.yachtbackend.user.User;
import io.github.yuyeol3.yachtbackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@GameServerOnly
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomSessionService {
    private final GameRoomRepository gameRoomRepository;
    private final GameStateRepository gameStateRepository;
    private final ParticipatedRepository participatedRepository;
    private final UserRepository userRepository;
    private final GameUtil gameUtil;
    private final GameService gameService;
    private final GameServerRegistryService gameServerRegistryService;

    @Transactional
    public GameState startGame(Long roomId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(ErrorCode.UNAUTHORIZED)
        );

        GameRoom gameRoom = gameRoomRepository.findById(roomId).orElseThrow(
                () -> new BusinessException(ErrorCode.NOT_FOUND)
        );
        assertCurrentServerOwns(gameRoom);

        if (!gameRoom.getHost().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        List<ParticipatedState> participants = participatedRepository.findMembersByRoomId(roomId);
        boolean isAllReady = true;
        for (ParticipatedState participant : participants) {
            isAllReady = isAllReady & participant.isReady();
        }

        if (!isAllReady) {
            throw new BusinessException(ErrorCode.NOT_READY);
        }

        if (gameRoom.getStatus().equals(GameStatus.PLAYING)) {
            throw new BusinessException(ErrorCode.ALREADY_PLAYING);
        }

        List<Long> turnList = participants.stream().map(ParticipatedState::userId).toList();
        Map<Long, UserScoreBoard> newScores = new HashMap<>();
        for (Long participantUserId : turnList) {
            newScores.put(participantUserId, UserScoreBoard.empty());
        }

        GameState gameState = GameState.builder()
                .startedAt(LocalDateTime.now())
                .turnTimeoutTime(LocalDateTime.now().plusMinutes(gameUtil.getTurnLimitMinutes()))
                .roomId(roomId)
                .curTurnUserId(turnList.getFirst())
                .leftRollCnt(3)
                .round(1)
                .turn(0)
                .turnList(List.copyOf(turnList))
                .dice(List.of(0, 0, 0, 0, 0))
                .kept(List.of(false, false, false, false, false))
                .scores(newScores)
                .build();

        gameStateRepository.save(roomId, gameState);
        gameService.startInitialTimer(roomId);
        gameRoom.start();
        return gameState;
    }

    public GameRoomEnterQuit addParticipant(Long roomId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(ErrorCode.UNAUTHORIZED)
        );

        GameRoom gameRoom = gameRoomRepository.findById(roomId).orElseThrow(
                () -> new BusinessException(ErrorCode.NOT_FOUND)
        );
        assertCurrentServerOwns(gameRoom);

        if (gameRoom.getStatus().equals(GameStatus.PLAYING)) {
            throw new BusinessException(ErrorCode.ALREADY_PLAYING);
        }

        if (participatedRepository.findMembersByRoomId(roomId).size()
                >= gameUtil.getUserLimitPerRoom()) {
            throw new BusinessException(ErrorCode.GAME_ROOM_IS_FULL);
        }

        participatedRepository.save(roomId, userId, user.getNickname());
        ParticipatedState participant = participatedRepository.findByMemberIdAndRoomId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        return GameRoomEnterQuit.builder()
                .action(EnterQuit.ENTER)
                .user(participant)
                .newHost(null)
                .build();
    }


    @Transactional
    public GameRoomEnterQuit removeParticipant(Long roomId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(ErrorCode.UNAUTHORIZED)
        );

        GameRoom gameRoom = gameRoomRepository.findById(roomId).orElseThrow(
                () -> new BusinessException(ErrorCode.NOT_FOUND)
        );
        assertCurrentServerOwns(gameRoom);

        ParticipatedState left = participatedRepository.findByMemberIdAndRoomId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        participatedRepository.leave(roomId, userId);

        ParticipatedState host = null;
        List<ParticipatedState> leftParticipants = participatedRepository.findMembersByRoomId(roomId);

        if (leftParticipants.isEmpty()) {
            if (gameRoom.getStatus().equals(GameStatus.PLAYING)) {
                gameService.abortGame(roomId);
            }
            gameServerRegistryService.releaseRoom(roomId, gameRoom.getServerId());
            gameRoomRepository.deleteById(roomId);
        } else if (gameRoom.getHost().getId().equals(userId)) {
            User newHost = userRepository.findById(leftParticipants.getFirst().userId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
            host = participatedRepository.findByMemberIdAndRoomId(roomId, newHost.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

            gameRoom.updateHost(newHost);
        }

        return GameRoomEnterQuit.builder()
                .action(EnterQuit.QUIT)
                .user(left)
                .newHost(host)
                .build();
    }

    public ToggleResponse toggleReady(Long roomId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(ErrorCode.UNAUTHORIZED)
        );

        GameRoom gameRoom = gameRoomRepository.findById(roomId).orElseThrow(
                () -> new BusinessException(ErrorCode.NOT_FOUND)
        );
        assertCurrentServerOwns(gameRoom);

        boolean toggleResult = participatedRepository.toggleReady(roomId, userId);
        return new ToggleResponse(user.getId(), toggleResult);
    }

    private void assertCurrentServerOwns(GameRoom gameRoom) {
        if (!gameServerRegistryService.isCurrentServerOwner(gameRoom.getId(), gameRoom.getServerId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
