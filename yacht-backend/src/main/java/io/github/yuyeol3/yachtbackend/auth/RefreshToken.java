package io.github.yuyeol3.yachtbackend.auth;


import io.github.yuyeol3.yachtbackend.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor
public class RefreshToken {
    @Id
    @Column(columnDefinition = "BINARY(32)")
    private byte[] id;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    public RefreshToken(byte[] id, LocalDateTime validUntil, User user) {
        this.id = id;
        this.validUntil = validUntil;
        this.user = user;
    }

    public boolean isValid() {
        return LocalDateTime.now().isBefore(validUntil);
    }
}
