package io.github.yuyeol3.yachtbackend.game;

import io.github.yuyeol3.yachtbackend.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "played")
public class Played {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "score",  nullable = false)
    private int score;

    @Column(name = "game_rank", nullable = false)
    private int rank;

    @Enumerated(EnumType.STRING)
    @Column(name="game_result", nullable = false)
    private GameResult gameResult;


    @Builder
    public Played(Game game, User user, int score, int rank, GameResult gameResult) {
        this.game = game;
        this.user = user;
        this.score = score;
        this.rank = rank;
        this.gameResult = gameResult;
    }
}
