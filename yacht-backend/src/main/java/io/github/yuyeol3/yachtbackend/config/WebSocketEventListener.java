package io.github.yuyeol3.yachtbackend.config;

import io.github.yuyeol3.yachtbackend.error.BusinessException;
import io.github.yuyeol3.yachtbackend.game.MessageType;
import io.github.yuyeol3.yachtbackend.game.dto.SocketResponse;
import io.github.yuyeol3.yachtbackend.gameroom.GameRoomService;
import io.github.yuyeol3.yachtbackend.gameroom.ParticipatedRepository;
import io.github.yuyeol3.yachtbackend.gameroom.dto.GameRoomEnterQuit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessagingTemplate template;
    private final ParticipatedRepository participatedRepository;
    private final GameRoomService gameRoomService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        if (accessor.getUser() != null) {
            Long userId = Long.parseLong(accessor.getUser().getName());
            Long roomId = participatedRepository.findRoomIdByUserId(userId);

            if (roomId != null) {
                // 강제퇴장 처리

                try {
                    GameRoomEnterQuit result = gameRoomService.removeParticipant(roomId, userId);

                    template.convertAndSend("/sub/rooms/" + roomId,
                            new SocketResponse<>(MessageType.QUIT, result)
                    );
                }
                catch (BusinessException be) {
                    log.warn("웹소켓 강제퇴장 처리 중 비즈니스 오류 발생", be);
                }
                catch (Exception e) {
                    log.error("웹소켓 강제퇴장 처리 중 에러 발생", e);
                }
            }
        }
    }

}
