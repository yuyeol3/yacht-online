package io.github.yuyeol3.yachtbackend.gameroom.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record GameRoomCreateRequest(
        @NotBlank @Length(min = 1, max = 250) String roomName
) {
}
