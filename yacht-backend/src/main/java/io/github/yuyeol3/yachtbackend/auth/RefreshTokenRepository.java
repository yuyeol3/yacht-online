package io.github.yuyeol3.yachtbackend.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, byte[]> {

}
