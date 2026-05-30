package io.github.yuyeol3.yachtbackend.gameroom;


import io.github.yuyeol3.yachtbackend.TimeEntity;
import io.github.yuyeol3.yachtbackend.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "game_rooms")
public class GameRoom extends TimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false)
    private GameStatus status;

    @Column(name="room_name", nullable = false)
    private String roomName;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private User host;

    public GameRoom(User host, String roomName) {
        this.host = host;
        this.roomName = roomName;
        this.status = GameStatus.WAITING;
    }


    public void start() {
        this.status = GameStatus.PLAYING;
    }

    public void end() {
        this.status = GameStatus.WAITING;
    }

    public void updateHost(User newHost) {
        this.host = newHost;
    }


}
