package io.github.yuyeol3.yachtbackend.auth;

import io.github.yuyeol3.yachtbackend.GenericDataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<GenericDataResponse<String>> login(@RequestBody @Valid LoginRequest loginRequest){
        LoginResponse res = authService.login(loginRequest);
        ResponseCookie cookie = getRefreshTokenCookie(res.refreshToken())
                .maxAge(jwtUtil.getRefreshExpiration())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new GenericDataResponse<>(res.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue("refresh_token") String refreshToken) {
        authService.logout(refreshToken);

        // 클라이언트 쿠키 삭제용
        ResponseCookie cookie = getRefreshTokenCookie("")
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<GenericDataResponse<String>> refresh(@CookieValue("refresh_token") String refreshToken) {
        LoginResponse res = authService.refresh(refreshToken);
        ResponseCookie cookie = getRefreshTokenCookie(res.refreshToken())
                .maxAge(jwtUtil.getRefreshExpiration())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new GenericDataResponse<>(res.accessToken()));
    }

    private ResponseCookie.ResponseCookieBuilder getRefreshTokenCookie(String token) {
        return ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/");
    }

}
