package org.tongji.sse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tongji.sse.entity.UserPersona;

@Repository
public interface UserPersonaRepository extends JpaRepository<UserPersona, Long> {
}
