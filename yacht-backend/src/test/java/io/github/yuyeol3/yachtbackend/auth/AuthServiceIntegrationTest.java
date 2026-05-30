package io.github.yuyeol3.yachtbackend.auth;

import io.github.yuyeol3.yachtbackend.user.UserCreateRequest;
import io.github.yuyeol3.yachtbackend.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void login_refresh_logout_flow() {
        userService.create(new UserCreateRequest("login123", "pw123456", "nick"));

        LoginResponse login = authService.login(new LoginRequest("login123", "pw123456"));

        assertThat(login.accessToken()).isNotBlank();
        assertThat(login.refreshToken()).isNotBlank();
        assertThat(jwtUtil.validateToken(login.accessToken())).isTrue();

        byte[] raw = Base64.getDecoder().decode(login.refreshToken());
        byte[] hashed = jwtUtil.hashToken(raw);
        assertThat(refreshTokenRepository.findById(hashed)).isPresent();

        LoginResponse refreshed = authService.refresh(login.refreshToken());
        assertThat(refreshed.accessToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isNotEqualTo(login.refreshToken());

        authService.logout(refreshed.refreshToken());
        byte[] raw2 = Base64.getDecoder().decode(refreshed.refreshToken());
        byte[] hashed2 = jwtUtil.hashToken(raw2);
        assertThat(refreshTokenRepository.findById(hashed2)).isEmpty();
    }
}
