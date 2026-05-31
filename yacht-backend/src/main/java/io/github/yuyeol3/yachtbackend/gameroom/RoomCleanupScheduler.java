package io.github.yuyeol3.yachtbackend.gameroom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomCleanupScheduler {

    private final RoomCleanupService roomCleanupService;

    @Scheduled(fixedDelay = 3600 * 1000)
    public void cleanupEmptyRooms(){
        log.info("Cleaning up empty rooms");
        try {
            List<GameRoom> zombies = roomCleanupService.getZombieRooms();
            int deleted = 0;
            if (!zombies.isEmpty())
                deleted = roomCleanupService.deleteRooms(zombies);

            log.info("{} Orphaned Rooms cleanup completed", deleted);
        }
        catch (Exception e) {
            log.error("게임 방 정리 중 오류 발생", e);
        }
    }
}
