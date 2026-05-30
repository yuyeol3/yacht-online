package io.github.yuyeol3.yachtbackend.gameroom;


import io.github.yuyeol3.yachtbackend.GenericDataResponse;
import io.github.yuyeol3.yachtbackend.gameroom.dto.GameRoomCreateRequest;
import io.github.yuyeol3.yachtbackend.gameroom.dto.GameRoomResponse;
import io.github.yuyeol3.yachtbackend.gameroom.dto.GameRoomResponseDetail;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class GameRoomController {

    private final GameRoomService gameRoomService;

    @PostMapping
    public ResponseEntity<GenericDataResponse<Long>> create(@RequestBody @Valid GameRoomCreateRequest gameRoomCreateRequest,
                                                            @AuthenticationPrincipal UserDetails userDetails) {

        GenericDataResponse<Long> result = gameRoomService.createRoom(gameRoomCreateRequest, userDetails);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }


    @GetMapping
    public ResponseEntity<Slice<GameRoomResponse>> getRooms(
            @PageableDefault(
                    size = 15,
                    sort = "id",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {

        return new ResponseEntity<>(gameRoomService.getRooms(pageable), HttpStatus.OK);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<GameRoomResponseDetail> getRoom(@PathVariable Long roomId) {
        return new ResponseEntity<>(gameRoomService.getRoomById(roomId), HttpStatus.OK);
    }


}
