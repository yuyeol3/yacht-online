package io.github.yuyeol3.yachtbackend.gameroom;

import io.github.yuyeol3.yachtbackend.GenericDataResponse;
import io.github.yuyeol3.yachtbackend.config.role.ApiServerOnly;
import io.github.yuyeol3.yachtbackend.error.BusinessException;
import io.github.yuyeol3.yachtbackend.error.ErrorCode;
import io.github.yuyeol3.yachtbackend.gameroom.dto.GameRoomCreateRequest;
import io.github.yuyeol3.yachtbackend.gameroom.dto.GameRoomCreateResponse;
import io.github.yuyeol3.yachtbackend.gameroom.dto.GameRoomResponse;
import io.github.yuyeol3.yachtbackend.gameroom.dto.GameRoomResponseDetail;
import io.github.yuyeol3.yachtbackend.gameroom.dto.ParticipatedState;
import io.github.yuyeol3.yachtbackend.server.GameServerInfo;
import io.github.yuyeol3.yachtbackend.server.GameServerRegistryService;
import io.github.yuyeol3.yachtbackend.user.User;
import io.github.yuyeol3.yachtbackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@ApiServerOnly
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomApiService {
    private final GameRoomRepository gameRoomRepository;
    private final ParticipatedRepository participatedRepository;
    private final UserRepository userRepository;
    private final GameServerRegistryService gameServerRegistryService;

    @Transactional
    public GenericDataResponse<GameRoomCreateResponse> createRoom(GameRoomCreateRequest gameRoomCreateRequest,
                                                                  UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        User user = userRepository.findById(userId).orElseThrow();

        GameServerInfo assignedServer = gameServerRegistryService.selectLeastLoadedServer();
        GameRoom gameRoom = new GameRoom(user, gameRoomCreateRequest.roomName(), assignedServer.serverId());
        gameRoom = gameRoomRepository.save(gameRoom);
        gameServerRegistryService.bindCreatedRoom(gameRoom.getId(), assignedServer.serverId());

        return new GenericDataResponse<>(GameRoomCreateResponse.builder()
                .roomId(gameRoom.getId())
                .roomName(gameRoom.getRoomName())
                .gameServer(assignedServer)
                .build());
    }

    public Slice<GameRoomResponse> getRooms(Pageable pageable) {
        Slice<GameRoom> gameRoomSlice = gameRoomRepository.findAllBy(pageable);
        return gameRoomSlice.map(gameRoom -> GameRoomResponse.builder()
                .id(gameRoom.getId())
                .roomName(gameRoom.getRoomName())
                .hostNickName(gameRoom.getHost().getNickname())
                .participatedUsers((long) participatedRepository.count(gameRoom.getId()))
                .status(gameRoom.getStatus())
                .gameServer(gameServerRegistryService.findServerForRoom(gameRoom.getId(), gameRoom.getServerId()))
                .build()
        );
    }

    public GameRoomResponseDetail getRoomById(Long roomId) {
        GameRoom gameRoom = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

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
                .status(gameRoom.getStatus())
                .gameServer(gameServerRegistryService.findServerForRoom(roomId, gameRoom.getServerId()))
                .host(host)
                .participatedUsers((long) participants.size())
                .participants(participants)
                .build();
    }
}
