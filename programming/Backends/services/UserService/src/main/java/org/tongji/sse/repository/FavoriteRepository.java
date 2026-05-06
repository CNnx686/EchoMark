package org.tongji.sse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tongji.sse.entity.Favorite;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Favorite> findByUserIdAndRecordId(Long userId, Long recordId);
    boolean existsByUserIdAndRecordId(Long userId, Long recordId);
    void deleteByUserIdAndRecordId(Long userId, Long recordId);
}
