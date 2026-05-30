package io.github.yuyeol3.yachtbackend.gameroom;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {


    Slice<GameRoom> findAllBy(Pageable pageable);

}
