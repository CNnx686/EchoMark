package org.tongji.sse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tongji.sse.entity.Audio;

import java.util.List;

public interface AudioRepository extends JpaRepository<Audio, Long> {
    List<Audio> findAllByIdIn(List<Long> ids);
    boolean existsByIdAndDeletedFalseAndStatus(Long id, String status);


    @Query(value = """
        SELECT a FROM Audio a
        WHERE a.deleted = false
        AND a.status = 'PUBLISHED'
        AND (6371000 * 2 * ASIN(SQRT(
            POWER(SIN((:latitude - a.latitude) * PI()/180 / 2), 2) +
            COS(:latitude * PI()/180) * COS(a.latitude * PI()/180) *
            POWER(SIN((:longitude - a.longitude) * PI()/180 / 2), 2)
        ))) <= :distance
    """)
    List<Audio> findNearby(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("distance") Double distance
    );

    @Query("SELECT DISTINCT a FROM Audio a LEFT JOIN a.tags t WHERE (a.title LIKE %:keyword% OR t LIKE %:keyword%) AND a.deleted = false AND a.status = 'PUBLISHED'")
    List<Audio> searchByKeyword(@Param("keyword") String keyword);

    @Query(value = "SELECT audio_id, COUNT(*) FROM audio_like GROUP BY audio_id", nativeQuery = true)
    List<Object[]> countLikesPerAudio();
}
