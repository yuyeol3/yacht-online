package io.github.yuyeol3.yachtbackend.gameroom;

import io.github.yuyeol3.yachtbackend.GenericDataResponse;
import io.github.yuyeol3.yachtbackend.game.GameState;
import io.github.yuyeol3.yachtbackend.game.MessageType;
import io.github.yuyeol3.yachtbackend.game.dto.SocketResponse;
import io.github.yuyeol3.yachtbackend.gameroom.dto.GameRoomEnterQuit;
import io.github.yuyeol3.yachtbackend.gameroom.dto.ToggleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;


@Slf4j
@Controller
@RequiredArgsConstructor
public class GameRoomWebSocketController {

    private final SimpMessagingTemplate template;
    private final GameRoomService gameRoomService;

    @MessageMapping("/rooms/{roomId}/enter")
    public void enterRoom(@DestinationVariable Long roomId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        GameRoomEnterQuit result = gameRoomService.addParticipant(roomId, userId);

        template.convertAndSend("/sub/rooms/" + roomId,
                new SocketResponse<>(MessageType.ENTER, result)
        );
    }

    @MessageMapping("/rooms/{roomId}/leave")
    public void leaveRoom(@DestinationVariable Long roomId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        GameRoomEnterQuit result = gameRoomService.removeParticipant(roomId, userId);

        template.convertAndSend("/sub/rooms/" + roomId,
            new SocketResponse<>(MessageType.QUIT, result)
        );
    }

    @MessageMapping("/rooms/{roomId}/start")
    public void startGame(@DestinationVariable Long roomId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        GameState state = gameRoomService.startGame(roomId, userId);

        template.convertAndSend("/sub/rooms/" + roomId,
                new SocketResponse<>(MessageType.START, state)
        );
    }

    @MessageMapping("/rooms/{roomId}/toggleReady")
    public void toggleReady(@DestinationVariable Long roomId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());


        ToggleResponse toggleResult = gameRoomService.toggleReady(roomId, userId);
        template.convertAndSend("/sub/rooms/" + roomId,
            new SocketResponse<>(MessageType.TOGGLE_READY, toggleResult)
        );
    }



}
