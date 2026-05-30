package io.github.yuyeol3.yachtbackend;


import io.github.yuyeol3.yachtbackend.error.BusinessException;
import io.github.yuyeol3.yachtbackend.error.ErrorCode;
import io.github.yuyeol3.yachtbackend.error.ErrorResponse;
import io.github.yuyeol3.yachtbackend.game.MessageType;
import io.github.yuyeol3.yachtbackend.game.dto.SocketResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.security.Principal;

@Slf4j
@ControllerAdvice
public class WebSocketExceptionHandler {
    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/errors")
    public SocketResponse<ErrorResponse> handleBusinessException(BusinessException e, Principal principal) {
        ErrorCode code = e.getErrorCode();
        log.warn("[웹소켓 예외] 유저: {}, 에러: {}", principal.getName(), e.getErrorCode().getMessage());
//        e.printStackTrace();
        return new SocketResponse<>(MessageType.ERROR, new ErrorResponse(code.getCode(), code.getMessage()));
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public SocketResponse<ErrorResponse> handleException(Exception e, Principal principal) {
        log.error("[치명적 에러] 유저: {}", principal.getName(), e);
//        e.printStackTrace();
        ErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;
        return new SocketResponse<>(MessageType.ERROR, new ErrorResponse(code.getCode(), code.getMessage()));
    }
}
