package io.github.yuyeol3.yachtbackend.game;


import io.github.yuyeol3.yachtbackend.error.BusinessException;
import io.github.yuyeol3.yachtbackend.error.ErrorCode;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

@Repository
public class GameStateRepository {
    private final ConcurrentHashMap<Long, GameState> store;

    public GameStateRepository() {
        this.store = new ConcurrentHashMap<>();
    }

    public GameState findById(Long roomId) {
        return store.get(roomId);
    }

    public void save(Long roomId, GameState gameState) {
        store.put(roomId, gameState);
    }

    public GameState update(Long roomId, UnaryOperator<GameState> updater) {
        return store.compute(roomId, (key, oldState)->{
            if (oldState == null) throw new BusinessException(ErrorCode.NOT_FOUND);
            return updater.apply(oldState);
        });
    }

    public void remove(Long roomId) {
        store.remove(roomId);
    }

}
