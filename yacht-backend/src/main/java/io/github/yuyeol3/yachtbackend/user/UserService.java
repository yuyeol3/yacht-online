package io.github.yuyeol3.yachtbackend.user;

import io.github.yuyeol3.yachtbackend.error.BusinessException;
import io.github.yuyeol3.yachtbackend.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long create(UserCreateRequest userCreateRequest) {
        if (userRepository.findByLoginId(userCreateRequest.loginId()).isPresent()) {
            throw new BusinessException(ErrorCode.ID_OCCUPIED);
        }

        if (userRepository.findByNickname(userCreateRequest.nickname()).isPresent()) {
            throw new BusinessException(ErrorCode.NICKNAME_OCCUPIED);
        }

        if (userCreateRequest.nickname().equals(userCreateRequest.loginId())) {
            throw new BusinessException(ErrorCode.ID_EQUALS_NICKNAME);
        }

        try {
            String passwordHash = passwordEncoder.encode(userCreateRequest.password());
            User user = User.builder()
                    .loginId(userCreateRequest.loginId())
                    .password(passwordHash)
                    .nickname(userCreateRequest.nickname())
                    .role(Role.USER)
                    .build();
            return userRepository.save(user).getId();
        }
        catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.USER_CREATE_CONFLICT);
        }

    }

    // TODO : 게임 이력 등 정보 추가
    public UserResponse findByNickname(String nickname) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));
        return new UserResponse(user.getNickname());
    }

    @Transactional
    public Long delete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));

        userRepository.delete(user);
        return user.getId();
    }

}
