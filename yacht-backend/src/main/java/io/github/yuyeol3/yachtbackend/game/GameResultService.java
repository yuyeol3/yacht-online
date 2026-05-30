package io.github.yuyeol3.yachtbackend.game;

import io.github.yuyeol3.yachtbackend.game.dto.PlayedResponse;
import io.github.yuyeol3.yachtbackend.gameroom.GameRoom;
import io.github.yuyeol3.yachtbackend.gameroom.GameRoomRepository;
import io.github.yuyeol3.yachtbackend.gameroom.ParticipatedRepository;
import io.github.yuyeol3.yachtbackend.gameroom.dto.ParticipatedState;
import io.github.yuyeol3.yachtbackend.user.User;
import io.github.yuyeol3.yachtbackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameResultService {
    private final ParticipatedRepository participatedRepository;
    private final GameRepository gameRepository;
    private final GameRoomRepository gameRoomRepository;
    private final PlayedRepository playedRepository;
    private final UserRepository userRepository;
    private final GameUtil gameUtil;


    @Transactional
    public List<PlayedResponse> saveGameResults(GameState state) {
        // 게임 기본 정보 저장
        Game game = Game.builder()
                .startedAt(state.startedAt())
                .endedAt(LocalDateTime.now())
                .build();

        game = gameRepository.save(game);

        // [[userId, score],...]
        int totalUser = state.turnList().size();
        int leftUser = participatedRepository.findMembersByRoomId(state.roomId()).size();
        GameUtil.UserScore[] userScores = new GameUtil.UserScore[totalUser];
        int idx = 0;
        for (Long userId : state.turnList()) {
            Optional<ParticipatedState> ps = participatedRepository.findByMemberIdAndRoomId(state.roomId(), userId);
            if (ps.isEmpty())
                userScores[idx] = new GameUtil.UserScore(userId, -1);
            else
                userScores[idx] = new GameUtil.UserScore(userId, state.scores().get(userId).total());
            idx++;
        }

        // 점수 기준으로 내림차순 정렬
        Arrays.sort(userScores, (a, b)->Long.compare(b.score, a.score));

        // 등수 구하기
        int[] ranks = gameUtil.calcRanks(userScores, totalUser);
        boolean isDraw = gameUtil.calcIsDraw(ranks, totalUser, leftUser);

        // 사용자별 played 결과 생성 및 저장
        List<PlayedResponse> savePlayedResults = savePlayedResults(
                totalUser, userScores, ranks, game, isDraw
        );
        gameRoomRepository.findById(state.roomId()).ifPresent(GameRoom::end);
        return savePlayedResults;
    }

    private List<PlayedResponse> savePlayedResults(
            int totalUser,
            GameUtil.UserScore[] userScores,
            int[] ranks,
            Game game,
            boolean isDraw
    ) {
        List<PlayedResponse> playedResponses = new ArrayList<>();
        // 사용자별 played 결과 생성 및 저장
        for (int i = 0; i < totalUser; i++) {
            int rank = ranks[i];
            Long userId = userScores[i].userId;
            int score = userScores[i].score;

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) continue;

            Played.PlayedBuilder pb = Played.builder()
                    .game(game)
                    .user(user)
                    .rank(rank);
            Played p;
            if (score == -1) {
                p = pb.score(0).gameResult(GameResult.LOSE).build();
            }
            else {
                if (isDraw) {
                    pb = pb.gameResult(GameResult.DRAW);
                }
                else {
                    pb = pb.gameResult(
                            rank <= (totalUser / 2) ? GameResult.WIN : GameResult.LOSE
                    );
                }

                p = pb.score(score).build();
            }

            playedRepository.save(p);
            playedResponses.add(PlayedResponse.from(p));
        }

        return playedResponses;
    }

}
