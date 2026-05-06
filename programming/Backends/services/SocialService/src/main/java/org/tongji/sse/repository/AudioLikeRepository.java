package org.tongji.sse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tongji.sse.entity.AudioLike;

import java.util.Optional;

public interface AudioLikeRepository extends JpaRepository<AudioLike, Long> {

    Optional<AudioLike> findByAudio_IdAndUser_Id(Long audioId, Long userId);

    Long countByAudio_Id(Long audioId);

    Boolean existsByAudio_IdAndUser_Id(Long audioId, Long userId);
}
