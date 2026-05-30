package io.github.yuyeol3.yachtbackend.error;

import lombok.Builder;
import org.springframework.http.ResponseEntity;

@Builder
public record ErrorResponse(
        String code,
        String message
) {
    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }
}
