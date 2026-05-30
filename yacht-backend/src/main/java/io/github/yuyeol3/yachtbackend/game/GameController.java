package io.github.yuyeol3.yachtbackend.game;

import io.github.yuyeol3.yachtbackend.error.BusinessException;
import io.github.yuyeol3.yachtbackend.game.dto.GameAction;
import io.github.yuyeol3.yachtbackend.game.dto.SocketResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@Slf4j
@RequiredArgsConstructor
public class GameController {

    private final SimpMessagingTemplate template;
    private final GameService gameService;

    @MessageMapping("/games/{roomId}/action")
    public void handleGameAction(
            @DestinationVariable Long roomId,
            @Payload GameAction action,
            Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());
        log.info("Action received : Room={}, User={}, Type={}", roomId, userId, action.type());

        GameState state = gameService.processAction(roomId, userId, action);

        template.convertAndSend("/sub/rooms/" + roomId,
                new SocketResponse<>(state.round() >= 13 ? MessageType.GAME_OVER : action.type(), state)
        );

        log.info("Action replied : Room={}, User={}, Type={}", roomId, userId, action.type());
    }
}

