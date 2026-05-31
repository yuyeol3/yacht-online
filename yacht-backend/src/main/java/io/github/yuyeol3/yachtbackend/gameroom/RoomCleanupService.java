package io.github.yuyeol3.yachtbackend.gameroom;

import io.github.yuyeol3.yachtbackend.server.GameServerRegistryService;
import io.github.yuyeol3.yachtbackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoomCleanupService {
    private final GameRoomRepository gameRoomRepository;
    private final ParticipatedRepository participatedRepository;
    private final GameServerRegistryService gameServerRegistryService;

    public List<GameRoom> getZombieRooms() {
        LocalDateTime criteria = LocalDateTime.now().minusMinutes(5);
        // 5분 이전에 만들어진 방 중 참여자가 0인 방
        List<GameRoom> targets = gameRoomRepository
                .findAllByCreatedAtBefore(criteria)
                .stream()
//                .map(GameRoom::getId)
                .filter(room -> participatedRepository.count(room.getId()) == 0)
                .toList();

        targets.forEach(room -> gameServerRegistryService.releaseRoom(room.getId(), room.getServerId()));
        return targets;
    }
    @Transactional
    public int deleteRooms(List<GameRoom> ids) {
        List<Long> targetIds = ids.stream().map(GameRoom::getId).toList();
        return gameRoomRepository.deleteAllByIdIn(targetIds);
    }

}
