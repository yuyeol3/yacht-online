package io.github.yuyeol3.yachtbackend.user;

import io.github.yuyeol3.yachtbackend.error.BusinessException;
import io.github.yuyeol3.yachtbackend.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void create_success() {
        UserCreateRequest req = new UserCreateRequest("login123", "pw123456", "nick");

        when(userRepository.findByLoginId(req.loginId())).thenReturn(Optional.empty());
        when(userRepository.findByNickname(req.nickname())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(req.password())).thenReturn("hashed");

        User saved = mock(User.class);
        when(saved.getId()).thenReturn(1L);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        Long id = userService.create(req);

        assertThat(id).isEqualTo(1L);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getLoginId()).isEqualTo(req.loginId());
        assertThat(userCaptor.getValue().getNickname()).isEqualTo(req.nickname());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void create_rejects_duplicate_login() {
        UserCreateRequest req = new UserCreateRequest("login123", "pw123456", "nick");
        when(userRepository.findByLoginId(req.loginId())).thenReturn(Optional.of(mock(User.class)));

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ID_OCCUPIED);
    }

    @Test
    void create_rejects_duplicate_nickname() {
        UserCreateRequest req = new UserCreateRequest("login123", "pw123456", "nick");
        when(userRepository.findByLoginId(req.loginId())).thenReturn(Optional.empty());
        when(userRepository.findByNickname(req.nickname())).thenReturn(Optional.of(mock(User.class)));

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NICKNAME_OCCUPIED);
    }

    @Test
    void create_rejects_same_id_and_nickname() {
        UserCreateRequest req = new UserCreateRequest("same", "pw123456", "same");
        when(userRepository.findByLoginId(req.loginId())).thenReturn(Optional.empty());
        when(userRepository.findByNickname(req.nickname())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ID_EQUALS_NICKNAME);
    }

    @Test
    void create_wraps_data_integrity_violation() {
        UserCreateRequest req = new UserCreateRequest("login123", "pw123456", "nick");
        when(userRepository.findByLoginId(req.loginId())).thenReturn(Optional.empty());
        when(userRepository.findByNickname(req.nickname())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(req.password())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_CREATE_CONFLICT);
    }

    @Test
    void delete_success() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        Long deleted = userService.delete(5L);

        assertThat(deleted).isEqualTo(5L);
        verify(userRepository).delete(user);
    }

    @Test
    void delete_missing_user() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(5L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
