package org.tongji.sse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tongji.sse.entity.LlmAudio;

import java.util.List;
import java.util.Set;

@Repository
public interface LlmAudioRepository extends JpaRepository<LlmAudio, Long> {
    List<LlmAudio> findDistinctByTagsIn(Set<String> tags);
    
    List<LlmAudio> findDistinctByTagsInAndStatusAndUserIdNot(Set<String> tags, String status, Long userId);
}
