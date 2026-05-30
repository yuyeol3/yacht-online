package io.github.yuyeol3.yachtbackend.auth;

import io.github.yuyeol3.yachtbackend.error.BusinessException;
import io.github.yuyeol3.yachtbackend.error.ErrorCode;
import io.github.yuyeol3.yachtbackend.user.User;
import io.github.yuyeol3.yachtbackend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_success_issues_tokens_and_saves_refresh() {
        String rawPassword = "pw123456";
        String hashed = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(user.getNickname()).thenReturn("nick");
        when(user.getPassword()).thenReturn(hashed);

        when(userRepository.findByLoginId("login")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(7L, "nick")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn(new byte[]{1, 2, 3});
        when(jwtUtil.hashToken(new byte[]{1, 2, 3})).thenReturn(new byte[]{9, 9, 9});
        when(jwtUtil.getRefreshExpiration()).thenReturn(3600L);

        LoginResponse response = authService.login(new LoginRequest("login", rawPassword));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_invalid_password_throws() {
        User user = mock(User.class);
        when(user.getPassword()).thenReturn(BCrypt.hashpw("other", BCrypt.gensalt()));
        when(userRepository.findByLoginId("login")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("login", "pw123456")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ID_OR_PWD);
    }

    @Test
    void logout_invalid_base64_throws() {
        assertThatThrownBy(() -> authService.logout("not-base64!!"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void refresh_rotates_refresh_token() {
        byte[] raw = new byte[]{4, 5, 6};
        String token = Base64.getEncoder().encodeToString(raw);
        byte[] hashed = new byte[]{7, 7, 7};

        User user = mock(User.class);
        when(user.getId()).thenReturn(8L);
        when(user.getNickname()).thenReturn("nick2");

        RefreshToken stored = RefreshToken.builder()
                .id(hashed)
                .validUntil(LocalDateTime.now().plusMinutes(5))
                .user(user)
                .build();

        when(jwtUtil.hashToken(raw)).thenReturn(hashed);
        when(refreshTokenRepository.findById(hashed)).thenReturn(Optional.of(stored));
        when(jwtUtil.generateAccessToken(8L, "nick2")).thenReturn("access2");
        when(jwtUtil.generateRefreshToken()).thenReturn(new byte[]{8, 8, 8});
        when(jwtUtil.hashToken(new byte[]{8, 8, 8})).thenReturn(new byte[]{1, 1, 1});
        when(jwtUtil.getRefreshExpiration()).thenReturn(3600L);

        LoginResponse response = authService.refresh(token);

        assertThat(response.accessToken()).isEqualTo("access2");
        assertThat(response.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).delete(stored);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }
}
