package org.tongji.sse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tongji.sse.entity.AudioCommentReply;

import java.util.List;

@Repository
public interface AudioCommentReplyRepository extends JpaRepository<AudioCommentReply, Long> {

    List<AudioCommentReply> findByCommentIdAndIsDeletedFalseOrderByCreateTimeAsc(Long commentId);

    List<AudioCommentReply> findByCommentIdInAndIsDeletedFalse(List<Long> commentIds);
}
