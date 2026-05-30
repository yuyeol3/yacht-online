package io.github.yuyeol3.yachtbackend.auth;

import io.github.yuyeol3.yachtbackend.error.BusinessException;
import io.github.yuyeol3.yachtbackend.error.ErrorCode;
import io.github.yuyeol3.yachtbackend.user.User;
import io.github.yuyeol3.yachtbackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;


    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByLoginId(loginRequest.loginId())
                .orElseThrow(()->new BusinessException(ErrorCode.INVALID_ID_OR_PWD));

        String hashedPassword = user.getPassword();
        if (BCrypt.checkpw(loginRequest.password(), hashedPassword)) {
            String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getNickname());
            return getLoginResponse(user, accessToken);
        }
        else throw new BusinessException(ErrorCode.INVALID_ID_OR_PWD);
    }

    private LoginResponse getLoginResponse(User user, String accessToken) {
        byte[] rawRefreshToken = jwtUtil.generateRefreshToken();
        byte[] hashedRefreshToken = jwtUtil.hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .id(hashedRefreshToken)
                .validUntil(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpiration()))
                .user(user)
                .build();
        refreshTokenRepository.save(refreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(Base64.getEncoder().encodeToString(rawRefreshToken))
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        try {
            byte[] rawToken = Base64.getDecoder().decode(refreshToken);
            byte[] hashedToken = jwtUtil.hashToken(rawToken);
            refreshTokenRepository.deleteById(hashedToken);
        }
        catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    @Transactional
    public LoginResponse refresh(String token) {
        try {
            byte[] rawToken = Base64.getDecoder().decode(token);
            byte[] hashedToken = jwtUtil.hashToken(rawToken);

            RefreshToken refreshToken = refreshTokenRepository.findById(hashedToken)
                    .orElseThrow(()->new BusinessException(ErrorCode.UNAUTHORIZED));

            if (!refreshToken.isValid()) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }

            User user = refreshToken.getUser();

            String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getNickname());

            // Rotate refresh token: delete old, issue new
            refreshTokenRepository.delete(refreshToken);

            return getLoginResponse(user, accessToken);
        }
        catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

    }
}
