package io.github.yuyeol3.yachtbackend.user;

import lombok.Builder;

@Builder
public record UserResponse(
        String nickname
) {
    static UserResponse from(User user) {
        return UserResponse.builder().nickname(user.getNickname()).build();
    }
}
