package org.tongji.sse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tongji.sse.entity.UserSetting;

/**
 * 用户设置仓库接口
 */
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
}
