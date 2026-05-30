package io.github.yuyeol3.yachtbackend.gameroom;


import io.github.yuyeol3.yachtbackend.GenericDataResponse;
import io.github.yuyeol3.yachtbackend.error.BusinessException;
import io.github.yuyeol3.yachtbackend.error.ErrorCode;
import io.github.yuyeol3.yachtbackend.game.*;
import io.github.yuyeol3.yachtbackend.game.dto.UserScoreBoard;
import io.github.yuyeol3.yachtbackend.gameroom.dto.*;
import io.github.yuyeol3.yachtbackend.user.User;
import io.github.yuyeol3.yachtbackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameRoomService {

    private final GameRoomRepository gameRoomRepository;
    private final GameStateRepository  gameStateRepository;
    private final ParticipatedRepository participatedRepository;
    private final UserRepository userRepository;
    private final GameUtil gameUtil;

    private final GameService gameService;

    @Transactional
    public GenericDataResponse<Long> createRoom(GameRoomCreateRequest gameRoomCreateRequest,
                                                UserDetails userDetails
                                                ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        User user = userRepository.findById(userId).orElseThrow();

        GameRoom gameRoom = new GameRoom(user, gameRoomCreateRequest.roomName());

        return new GenericDataResponse<>(gameRoomRepository.save(gameRoom).getId());
    }

    public Slice<GameRoomResponse> getRooms(Pageable pageable) {
        Slice<GameRoom> gameRoomSlice = gameRoomRepository.findAllBy(pageable);
        return gameRoomSlice.map((gr)-> GameRoomResponse.builder()
                    .id(gr.getId())
                    .roomName(gr.getRoomName())
                    .hostNickName(gr.getHost().getNickname())
                    .participatedUsers(Integer.valueOf(participatedRepository.count(gr.getId())).longValue())
                    .build()
        );
    }

    // TODO : GameRoomResponseDetail 로 정보 제공하기 (플레이어 정보 및 방장 확인)
    public GameRoomResponseDetail getRoomById(Long roomId) {
        GameRoom gameRoom = gameRoomRepository.findById(roomId)
                .orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));

        ParticipatedState host = participatedRepository.findByMemberId(gameRoom.getHost().getId())
                .orElseGet(() -> new ParticipatedState(
                        gameRoom.getHost().getId(),
                        gameRoom.getHost().getNickname(),
                        false
                ));
        List<ParticipatedState> participants = participatedRepository.findMembersByRoomId(roomId);

        return GameRoomResponseDetail.builder()
                .id(gameRoom.getId())
                .roomName(gameRoom.getRoomName())
                .host(host)
                .participatedUsers(participants.stream().count())
                .participants(participants)
                .build();


//        return GameRoomResponse.from(gameRoom);

    }

    @Transactional
    public GameState startGame(Long roomId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                ()->new BusinessException(ErrorCode.UNAUTHORIZED)
        );

        GameRoom gameRoom = gameRoomRepository.findById(roomId).orElseThrow(
                ()->new BusinessException(ErrorCode.NOT_FOUND)
        );

        // 방장이 아닌 경우
        if (!gameRoom.getHost().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        List<ParticipatedState> participants = participatedRepository.findMembersByRoomId(roomId);
        boolean isAllReady = true;
        for (ParticipatedState p : participants) {
            isAllReady = isAllReady & p.isReady();
        }

        if (!isAllReady) {
            throw new BusinessException(ErrorCode.NOT_READY);
        }

        // 게임이 이미 시작된 경우
        if (gameRoom.getStatus().equals(GameStatus.PLAYING)) {
            throw new BusinessException(ErrorCode.ALREADY_PLAYING);
        }

        List<Long> turnList = participants.stream().map(ParticipatedState::userId).toList();
        Map<Long, UserScoreBoard> newScores = new HashMap<>();
        for (Long userIds : turnList) {
            newScores.put(userIds, UserScoreBoard.empty());
        }

        GameState gs = GameState.builder()
                .startedAt(LocalDateTime.now())
                .turnTimeoutTime(LocalDateTime.now().plusMinutes(gameUtil.getTurnLimitMinutes()))
                .roomId(roomId)
                .curTurnUserId(turnList.getFirst())
                .leftRollCnt(3)
                .round(1)
                .turn(0)
                .turnList(List.copyOf(turnList))
                .dice(List.of(0,0,0,0,0))
                .kept(List.of(false, false, false, false, false))
                .scores(newScores)
                .build();

        gameStateRepository.save(roomId, gs);
        gameService.startInitialTimer(roomId);
        gameRoom.start();
        return gs;
    }

    public GameRoomEnterQuit addParticipant(Long roomId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                ()->new BusinessException(ErrorCode.UNAUTHORIZED)
        );

        GameRoom gameRoom = gameRoomRepository.findById(roomId).orElseThrow(
                ()->new BusinessException(ErrorCode.NOT_FOUND)
        );

        if (gameRoom.getStatus().equals(GameStatus.PLAYING)) {
            throw new BusinessException(ErrorCode.ALREADY_PLAYING);
        }

        if (participatedRepository.findMembersByRoomId(roomId).size()
                >= gameUtil.getUserLimitPerRoom()) {
            throw new BusinessException(ErrorCode.GAME_ROOM_IS_FULL);
        }

        participatedRepository.save(roomId, userId, user.getNickname());
        ParticipatedState participant = participatedRepository.findByMemberIdAndRoomId(roomId, userId)
                .orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));

        return GameRoomEnterQuit.builder()
                .action(EnterQuit.ENTER)
                .user(participant)
                .newHost(null)
                .build();
    }

    // TODO : 나가는 사람이 방장이면, 방장 위임하도록 구현
    @Transactional
    public GameRoomEnterQuit removeParticipant(Long roomId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                ()->new BusinessException(ErrorCode.UNAUTHORIZED)
        );

        GameRoom gameRoom = gameRoomRepository.findById(roomId).orElseThrow(
                ()->new BusinessException(ErrorCode.NOT_FOUND)
        );
        ParticipatedState left = participatedRepository.findByMemberIdAndRoomId(roomId, userId)
                .orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
        participatedRepository.leave(roomId, userId);

        // 나가는 사람이 방장이면 아무에게나 위임
        ParticipatedState host = null;
        List<ParticipatedState> leftParticipants = participatedRepository.findMembersByRoomId(roomId);

        if (leftParticipants.isEmpty()) {
            if (gameRoom.getStatus().equals(GameStatus.PLAYING)) {
                gameService.abortGame(roomId);
            }
            gameRoomRepository.deleteById(roomId);
        }
        else {
            if (gameRoom.getHost().getId().equals(userId)) {

                User newHost = userRepository.findById(leftParticipants.getFirst().userId())
                        .orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
                host = participatedRepository.findByMemberIdAndRoomId(roomId, newHost.getId())
                        .orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));

                gameRoom.updateHost(newHost);

            }
        }

        return GameRoomEnterQuit.builder()
                .action(EnterQuit.QUIT)
                .user(left)
                .newHost(host)
                .build();
    }

    public ToggleResponse toggleReady(Long roomId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                ()->new BusinessException(ErrorCode.UNAUTHORIZED)
        );

        GameRoom gameRoom = gameRoomRepository.findById(roomId).orElseThrow(
                ()->new BusinessException(ErrorCode.NOT_FOUND)
        );

        boolean toggleResult = participatedRepository.toggleReady(roomId, userId);
        return new ToggleResponse(user.getId(), toggleResult);
    }

}
