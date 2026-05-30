package io.github.yuyeol3.yachtbackend.user;

import io.github.yuyeol3.yachtbackend.error.BusinessException;
import io.github.yuyeol3.yachtbackend.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void create_find_delete_flow() {
        UserCreateRequest req = new UserCreateRequest("login123", "pw123456", "nick");

        Long id = userService.create(req);

        assertThat(id).isNotNull();
        assertThat(userRepository.findById(id)).isPresent();
        assertThat(userService.findByNickname("nick").nickname()).isEqualTo("nick");

        Long deleted = userService.delete(id);
        assertThat(deleted).isEqualTo(id);
        assertThat(userRepository.findById(id)).isEmpty();
    }

    @Test
    void create_duplicate_login_is_rejected() {
        UserCreateRequest req = new UserCreateRequest("login123", "pw123456", "nick");
        userService.create(req);

        assertThatThrownBy(() -> userService.create(new UserCreateRequest("login123", "pw123456", "nick2")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ID_OCCUPIED);
    }
}
