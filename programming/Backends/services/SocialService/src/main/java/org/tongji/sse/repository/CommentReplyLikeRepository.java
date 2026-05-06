package org.tongji.sse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tongji.sse.entity.CommentReplyLike;
import org.tongji.sse.enums.TargetType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface CommentReplyLikeRepository extends JpaRepository<CommentReplyLike, Long> {

    Optional<CommentReplyLike> findByTargetIdAndUserIdAndTargetType(Long targetId, Long userId, TargetType targetType);

    Long countByTargetIdAndTargetType(Long targetId, TargetType targetType);

    Boolean existsByTargetIdAndUserIdAndTargetType(Long targetId, Long userId, TargetType type);

    @Query("""
    SELECT crl.targetId, COUNT(crl)
    FROM CommentReplyLike crl
    WHERE crl.targetId IN :ids AND crl.targetType = :type
    GROUP BY crl.targetId
""")
    List<Object[]> countLikes(@Param("ids") List<Long> ids, @Param("type") TargetType type);

    default Map<Long, Long> countLikesGroupByTargetIdsAndType(List<Long> ids, TargetType type) {
        return countLikes(ids, type).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Query("""
    SELECT crl.targetId
    FROM CommentReplyLike crl
    WHERE crl.userId = :userId
      AND crl.targetType = :type
      AND crl.targetId IN :ids
""")
    Set<Long> findLikedTargetIdsByUserAndType(
            @Param("userId") Long userId,
            @Param("type") TargetType type,
            @Param("ids") List<Long> ids);
}
