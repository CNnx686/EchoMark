package org.tongji.sse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tongji.sse.entity.AudioComment;

import java.util.List;

public interface AudioCommentRepository extends JpaRepository<AudioComment, Long> {

    // 查询某个音频的所有未删除评论
    List<AudioComment> findByAudioIdAndIsDeletedFalseOrderByCreateTimeDesc(Long audioId);

    List<AudioComment> findByAudioIdAndIsDeletedFalseOrderByCreateTimeAsc(Long audioId);
}
