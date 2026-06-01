-- ============================================================================
-- SoundMap 数据库建表脚本
-- 基于 JPA 实体模型 和 ER 图
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- ============================================================================

CREATE DATABASE IF NOT EXISTS sound_map
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE sound_map;

-- ============================================================================
-- 1. users — 用户认证账户 (对应 ER 图 AuthenticationAccount)
-- ============================================================================
CREATE TABLE users (
    id              BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    username        VARCHAR(255)    NOT NULL,
    password        VARCHAR(255)    NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    phone_number    VARCHAR(255)    DEFAULT NULL,
    registration_time DATETIME      DEFAULT NULL,

    UNIQUE KEY uk_username     (username),
    UNIQUE KEY uk_email        (email),
    UNIQUE KEY uk_phone_number (phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 2. user_profiles — 用户个人信息 (对应 ER 图 UserProfile)
-- ============================================================================
CREATE TABLE user_profiles (
    user_id             BIGINT          PRIMARY KEY,
    nickname            VARCHAR(255)    NOT NULL,
    avatar_url          VARCHAR(255)    DEFAULT NULL,
    bio                 VARCHAR(255)    DEFAULT NULL,
    self_description    VARCHAR(2000)   DEFAULT NULL,

    CONSTRAINT fk_profile_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 3. user_settings — 用户设置
-- ============================================================================
CREATE TABLE user_settings (
    user_id             BIGINT          PRIMARY KEY,
    notification_mute   TINYINT(1)      NOT NULL DEFAULT 0,

    CONSTRAINT fk_settings_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 4. audios — 音频记录 (对应 ER 图 AudioRecord)
-- ============================================================================
CREATE TABLE audios (
    id              BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    user_id         BIGINT          DEFAULT NULL,
    audio_url       VARCHAR(255)    DEFAULT NULL,
    photo_url       VARCHAR(255)    DEFAULT NULL,
    latitude        DOUBLE          DEFAULT NULL,
    longitude       DOUBLE          DEFAULT NULL,
    title           VARCHAR(255)    DEFAULT NULL,
    description     VARCHAR(200)    DEFAULT NULL,
    upload_time     TIMESTAMP       NULL DEFAULT NULL,
    publish_time    TIMESTAMP       NULL DEFAULT NULL,
    status          VARCHAR(255)    DEFAULT NULL,
    deleted         TINYINT(1)      DEFAULT 0,
    visit_count     BIGINT          DEFAULT 0,

    INDEX idx_audios_user_id    (user_id),
    INDEX idx_audios_status     (status),
    INDEX idx_audios_location   (latitude, longitude),

    CONSTRAINT fk_audio_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 5. audio_tags — 标签 (对应 ER 图 Tag, JPA @ElementCollection)
-- ============================================================================
CREATE TABLE audio_tags (
    audio_id    BIGINT          NOT NULL,
    tag         VARCHAR(50)     NOT NULL,

    PRIMARY KEY (audio_id, tag),

    CONSTRAINT fk_tag_audio
        FOREIGN KEY (audio_id) REFERENCES audios(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 6. audio_comment — 评论 (对应 ER 图 Comment)
--    ER 图使用 ParentCommentId 自引用, 实际实现拆分为 audio_comment_reply
-- ============================================================================
CREATE TABLE audio_comment (
    id          BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    audio_id    BIGINT          NOT NULL,
    user_id     BIGINT          NOT NULL,
    content     VARCHAR(1000)   NOT NULL,
    create_time DATETIME        NOT NULL,
    is_deleted  TINYINT(1)      NOT NULL DEFAULT 0,

    INDEX idx_comment_audio_id (audio_id),

    CONSTRAINT fk_audio_comment_audio
        FOREIGN KEY (audio_id) REFERENCES audios(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_audio_comment_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 7. audio_comment_reply — 评论回复 (对应 ER 图 Comment 自引用)
--    实际实现中 Comment 的父评论关系由此表承载
-- ============================================================================
CREATE TABLE audio_comment_reply (
    id          BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    comment_id  BIGINT          NOT NULL,
    user_id     BIGINT          NOT NULL,
    content     VARCHAR(1000)   NOT NULL,
    create_time DATETIME        NOT NULL,
    is_deleted  TINYINT(1)      NOT NULL DEFAULT 0,

    INDEX idx_reply_comment_id (comment_id),

    CONSTRAINT fk_reply_comment
        FOREIGN KEY (comment_id) REFERENCES audio_comment(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_reply_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 8. audio_like — 音频点赞 (对应 ER 图 Like, TargetType=RECORD)
-- ============================================================================
CREATE TABLE audio_like (
    id          BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    audio_id    BIGINT          NOT NULL,
    user_id     BIGINT          NOT NULL,

    UNIQUE KEY uk_audio_like (audio_id, user_id),

    CONSTRAINT fk_audio_like_audio
        FOREIGN KEY (audio_id) REFERENCES audios(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_audio_like_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 9. comment_reply_like — 评论/回复点赞 (对应 ER 图 Like, TargetType=COMMENT)
--    target_type 枚举: AUDIO/COMMENT/REPLY
--    UNIQUE (target_id, user_id, target_type) 防止重复点赞
-- ============================================================================
CREATE TABLE comment_reply_like (
    id          BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    target_id   BIGINT          NOT NULL,
    user_id     BIGINT          NOT NULL,
    target_type VARCHAR(10)     NOT NULL,

    UNIQUE KEY uk_comment_reply_like (target_id, user_id, target_type),

    CONSTRAINT fk_comment_reply_like_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 10. favorites — 收藏 (对应 ER 图 Favorite)
--     UNIQUE (user_id, record_id) 防止重复收藏
-- ============================================================================
CREATE TABLE favorites (
    favorite_id BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    user_id     BIGINT          NOT NULL,
    record_id   BIGINT          NOT NULL,
    created_at  DATETIME        DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_favorite (user_id, record_id),

    CONSTRAINT fk_favorite_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_favorite_audio
        FOREIGN KEY (record_id) REFERENCES audios(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 11. notification — 通知 (对应 ER 图 Notification)
--     type 枚举: LIKE / COMMENT / REPLY
--     target_type 枚举: AUDIO / COMMENT
--     event_id UNIQUE 用于幂等性保证
-- ============================================================================
CREATE TABLE notification (
    id                  BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    event_id            VARCHAR(255)    NOT NULL,
    receiver_user_id    BIGINT          NOT NULL,
    actor_user_id       BIGINT          NOT NULL,
    type                VARCHAR(20)     NOT NULL,
    content             VARCHAR(255)    NOT NULL,
    target_type         VARCHAR(10)     NOT NULL,
    target_id           BIGINT          NOT NULL,
    created_at          TIMESTAMP       NOT NULL,
    is_read             TINYINT(1)      NOT NULL DEFAULT 0,

    UNIQUE KEY uk_event_id (event_id),
    INDEX idx_notification_receiver (receiver_user_id),

    CONSTRAINT fk_notification_receiver
        FOREIGN KEY (receiver_user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_notification_actor
        FOREIGN KEY (actor_user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 12. sse_pending_notification — SSE 待推送通知
--     UNIQUE (user_id, notification_id) 防止重复推送
-- ============================================================================
CREATE TABLE sse_pending_notification (
    id                  BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    notification_id     BIGINT          NOT NULL,
    pushed              TINYINT(1)      NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL,
    pushed_at           TIMESTAMP       NULL DEFAULT NULL,

    UNIQUE KEY uk_user_notification (user_id, notification_id),

    CONSTRAINT fk_pending_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_pending_notification
        FOREIGN KEY (notification_id) REFERENCES notification(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 13. user_persona — 用户 AI 画像 (LLM 生成)
--     persona_json 存储为 JSON 类型
-- ============================================================================
CREATE TABLE user_persona (
    user_id         BIGINT          PRIMARY KEY,
    persona_json    JSON            NOT NULL,
    version         INT             DEFAULT NULL,
    updated_at      TIMESTAMP       NULL DEFAULT NULL,

    CONSTRAINT fk_persona_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
