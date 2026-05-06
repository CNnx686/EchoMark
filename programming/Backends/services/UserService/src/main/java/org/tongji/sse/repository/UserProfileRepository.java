package org.tongji.sse.repository;

import org.tongji.sse.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 用户个人资料仓库接口
 * 继承自 JpaRepository，提供基本的 CRUD 操作
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}

