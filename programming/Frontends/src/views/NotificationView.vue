<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useI18n } from "vue-i18n";
import type { NotificationDTO } from "@/types/Notification";
import { TargetType } from "@/types/Notification";
import { useRouter } from "vue-router";
import { UserProfileDTO, UserSettingDTO } from "@/types/User";
import { ApiResponse } from "@/types/Audio";

const { t } = useI18n();
const router = useRouter();

const notifications = ref<NotificationDTO[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);

const nicknameMap = ref<Record<number, string>>({});

const getAuthToken = () => localStorage.getItem("token");

// 已读全部消息
const markAllAsRead = async () => {
  if (notifications.value.length === 0) return;

  const token = getAuthToken();
  if (!token) {
    router.push("/login");
    return;
  }

  try {
    loading.value = true;
    const notificationIds = notifications.value.map((n) => n.id);
    const response = await fetch("/api/notifications/read", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(notificationIds),
    });

    const result: ApiResponse<boolean> = await response.json();
    if (result.code === 200 && result.data) {
      // 更新本地通知状态为已读
      notifications.value.forEach((notification) => {
        notification.isRead = true;
      });
      // 清除本地存储中的未读通知ID
      localStorage.removeItem("pendingNotificationIds");
      // 触发本地存储变化事件，通知Header组件更新红点
      window.dispatchEvent(new Event("pendingNotificationChange"));
    }
  } catch (err) {
    console.error("Failed to mark all notifications as read:", err);
  } finally {
    loading.value = false;
  }
};

// 标记单个通知为已读
const markAsRead = async (notificationId: number) => {
  const token = getAuthToken();
  if (!token) {
    router.push("/login");
    return;
  }

  try {
    loading.value = true;
    const response = await fetch("/api/notifications/read", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify([notificationId]),
    });

    const result: ApiResponse<boolean> = await response.json();
    if (result.code === 200 && result.data) {
      // 更新本地通知状态为已读
      const notification = notifications.value.find((n) => n.id === notificationId);
      if (notification) {
        notification.isRead = true;
      }
      // 从本地存储中移除该通知ID
      const pendingIds = localStorage.getItem("pendingNotificationIds");
      if (pendingIds) {
        const ids = JSON.parse(pendingIds);
        const updatedIds = ids.filter((id: number) => id !== notificationId);
        localStorage.setItem("pendingNotificationIds", JSON.stringify(updatedIds));
        // 触发本地存储变化事件，通知Header组件更新红点
        window.dispatchEvent(new Event("pendingNotificationChange"));
      }
    }
  } catch (err) {
    console.error(`Failed to mark notification ${notificationId} as read:`, err);
  } finally {
    loading.value = false;
  }
};

// 点击通知处理函数
const handleNotificationClick = async (notification: NotificationDTO) => {
  // 标记为已读
  if (!notification.isRead) {
    await markAsRead(notification.id);
  }

  // 根据 targetType 决定如何跳转
  if (notification.targetId) {
    if (notification.targetType === TargetType.AUDIO) {
      // 如果目标类型是音频，使用 targetId 跳转
      router.push(`/audio/${notification.targetId}`);
    } else if (notification.targetType === TargetType.COMMENT) {
      // 如果目标类型是评论，先获取评论信息得到音频 ID
      try {
        loading.value = true;
        const token = getAuthToken();
        const response = await fetch(`/api/social/comment/${notification.targetId}`, {
          headers: token ? { Authorization: `Bearer ${token}` } : {},
        });

        if (response.ok) {
          const result = await response.json();
          if (result.code === 200 && result.data && result.data.audioId) {
            router.push(`/audio/${result.data.audioId}`);
          }
        }
      } catch (err) {
        console.error("Failed to get comment:", err);
      } finally {
        loading.value = false;
      }
    } else if (notification.targetType === TargetType.REPLY) {
      // 如果目标类型是回复，先获取回复信息得到音频 ID
      try {
        loading.value = true;
        const token = getAuthToken();
        const response = await fetch(`/api/social/reply/${notification.targetId}`, {
          headers: token ? { Authorization: `Bearer ${token}` } : {},
        });

        if (response.ok) {
          const result = await response.json();
          if (result.code === 200 && result.data && result.data.audioId) {
            router.push(`/audio/${result.data.audioId}`);
          }
        }
      } catch (err) {
        console.error("Failed to get reply:", err);
      } finally {
        loading.value = false;
      }
    }
  }
};

// 静默通知开关
const notificationMute = ref(false);

// 获取用户设置
const fetchUserSettings = async (): Promise<UserSettingDTO> => {
  // 定义默认设置
  const defaultSettings = { notificationMute: false };

  // 先尝试从localStorage获取缓存的用户设置
  const cachedSettings = localStorage.getItem("userSettings");
  if (cachedSettings) {
    try {
      const parsedSettings = JSON.parse(cachedSettings) as UserSettingDTO;
      // 缓存有效，更新响应式变量并返回
      notificationMute.value = parsedSettings.notificationMute;
      return parsedSettings;
    } catch (e) {
      localStorage.removeItem("userSettings");
    }
  }

  // 没有缓存或缓存无效，检查是否有token
  const token = getAuthToken();
  if (!token) {
    // 如果没有token，使用默认设置
    notificationMute.value = defaultSettings.notificationMute;
    return defaultSettings;
  }

  // 有token，从API获取
  try {
    const response = await fetch("/api/users/settings", {
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      // API请求失败，使用默认设置
      notificationMute.value = defaultSettings.notificationMute;
      return defaultSettings;
    }

    const result: ApiResponse<UserSettingDTO> = await response.json();
    if (result.code === 200 && result.data) {
      // 更新本地存储
      localStorage.setItem("userSettings", JSON.stringify(result.data));
      // 更新响应式变量
      notificationMute.value = result.data.notificationMute;
      return result.data;
    }

    // API返回但数据无效，使用默认设置
    notificationMute.value = defaultSettings.notificationMute;
    return defaultSettings;
  } catch (e) {
    console.warn("Failed to fetch user settings", e);
    // 异常情况下使用默认设置
    notificationMute.value = defaultSettings.notificationMute;
    return defaultSettings;
  } finally {
  }
};

// 更新静默通知设置
const updateNotificationMute = async () => {
  const token = getAuthToken();
  if (!token) {
    router.push("/login");
    return;
  }

  const newSettings: UserSettingDTO = { notificationMute: notificationMute.value };
  const originalValue = !newSettings.notificationMute;

  try {
    loading.value = true;

    // 发送API请求更新设置
    const response = await fetch("/api/users/settings", {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(newSettings),
    });

    const result: ApiResponse<boolean> = await response.json();
    if (result.code !== 200) {
      // 如果更新失败，恢复原来的设置
      notificationMute.value = originalValue;
      throw new Error(result.msg || "更新设置失败");
    }

    // API请求成功后，再更新本地存储
    localStorage.setItem("userSettings", JSON.stringify(newSettings));
    // 触发用户设置变化事件，通知Header组件更新静默设置
    window.dispatchEvent(new Event("userSettingsChange"));
  } catch (err) {
    console.error("Failed to update notification mute setting:", err);
    // 如果更新失败，恢复原来的设置
    notificationMute.value = originalValue;
    // 如果本地存储已经被修改，恢复原来的设置
    localStorage.setItem("userSettings", JSON.stringify({ notificationMute: originalValue }));
  } finally {
    loading.value = false;
  }
};

// 获取昵称
const fetchNicknameByUserId = async (userId: number): Promise<string> => {
  if (nicknameMap.value[userId]) {
    return nicknameMap.value[userId];
  }

  const token = getAuthToken();
  try {
    const response = await fetch(`/api/users/${userId}/profile`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });

    if (!response.ok) {
      return `User_${userId}`;
    }
    const result: ApiResponse<UserProfileDTO> = await response.json();
    const nickname = result.data.nickname || `User_${userId}`;
    nicknameMap.value[userId] = nickname;
    return nickname;
  } catch (err) {
    console.warn(`Failed to load nickname for user ${userId}:`, err);
    return `User_${userId}`;
  }
};

// 加载昵称
const loadActorUsernames = async () => {
  const userIds = notifications.value
    .map((n) => n.actorUserId)
    .filter((id) => id != null && !isNaN(id)) as number[];

  const uniqueUserIds = [...new Set(userIds)];
  if (uniqueUserIds.length === 0) return;

  await Promise.all(uniqueUserIds.map((id) => fetchNicknameByUserId(id)));
};

const fetchNotifications = async () => {
  const token = getAuthToken();
  if (!token) {
    error.value = "请先登录";
    loading.value = false;
    router.push("/login");
    return;
  }

  try {
    loading.value = true;
    const response = await fetch("/api/notifications/me", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const result: ApiResponse<NotificationDTO[]> = await response.json();
    if (result.code === 200) {
      notifications.value = result.data || [];
      // 让用户昵称加载在后台进行，不阻塞按钮显示
      loadActorUsernames().catch((err) => console.warn("Failed to load actor usernames:", err));
    } else {
      error.value = result.msg || "加载失败";
    }
  } catch (err) {
    console.error(err);
    error.value = "网络错误，请稍后重试";
  } finally {
    loading.value = false;
  }
};

const formatTime = (isoString: string): string => {
  const date = new Date(isoString);
  return date
    .toLocaleString("zh-CN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    })
    .replace(/\//g, "-");
};

const enrichedNotifications = computed(() => {
  return notifications.value.map((item) => ({
    ...item,
    actorUsername: nicknameMap.value[item.actorUserId] || "加载中...",
  }));
});

onMounted(() => {
  fetchNotifications();
  fetchUserSettings();
});
</script>

<template>
  <div class="notification-page">
    <div class="page-header">
      <h1>{{ t("notification.title") }}</h1>
      <div class="header-actions">
        <div class="mute-switch">
          <label class="switch">
            <input
              v-model="notificationMute"
              type="checkbox"
              :disabled="loading"
              @change="updateNotificationMute"
            />
            <span class="slider round" />
          </label>
          <span>{{ t("notification.muteNotifications") }}</span>
        </div>
        <button class="mark-all-button" :disabled="loading" @click="markAllAsRead">
          {{ t("notification.markAllAsRead") }}
        </button>
      </div>
    </div>

    <!-- 加载遮罩 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner" />
    </div>

    <div v-else-if="error" class="error">
      {{ error }}
    </div>

    <div v-else-if="notifications.length === 0" class="empty">
      {{ t("notification.noNotifications") }}
    </div>

    <ul v-else class="notification-list">
      <!-- 未读通知 -->
      <template v-for="item in enrichedNotifications.filter((n) => !n.isRead)" :key="item.id">
        <li class="notification-item unread" @click="handleNotificationClick(item)">
          <!-- 显示发送通知的用户名 -->
          <div class="actor-info">
            <strong>{{ item.actorUsername }}</strong>
          </div>

          <div class="notification-type">
            <span class="type-badge">{{ t(`notification.type.${item.type}`) }}</span>
          </div>

          <div class="notification-content">
            {{ item.content }}
          </div>

          <div class="notification-meta">
            <span class="target-type">{{ t(`notification.target.${item.targetType}`) }}</span>
            <span class="time">{{ formatTime(item.createdAt) }}</span>
          </div>
        </li>
      </template>

      <!-- 已读未读分隔线 -->
      <li v-if="enrichedNotifications.filter((n) => n.isRead).length > 0" class="divider">
        <div class="divider-line" />
        <span class="divider-text">{{ t("notification.readSeparator") }}</span>
        <div class="divider-line" />
      </li>

      <!-- 已读通知 -->
      <template v-for="item in enrichedNotifications.filter((n) => n.isRead)" :key="item.id">
        <li class="notification-item read" @click="handleNotificationClick(item)">
          <!-- 显示发送通知的用户名 -->
          <div class="actor-info">
            <strong>{{ item.actorUsername }}</strong>
          </div>

          <div class="notification-type">
            <span class="type-badge">{{ t(`notification.type.${item.type}`) }}</span>
          </div>

          <div class="notification-content">
            {{ item.content }}
          </div>

          <div class="notification-meta">
            <span class="target-type">{{ t(`notification.target.${item.targetType}`) }}</span>
            <span class="time">{{ formatTime(item.createdAt) }}</span>
          </div>
        </li>
      </template>
    </ul>
  </div>
</template>

<style scoped>
.notification-page {
  padding: 1.5rem;
  max-width: 720px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.mute-switch {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.mute-switch span {
  font-size: 0.875rem;
}

/* Switch styles */
.switch {
  position: relative;
  display: inline-block;
  width: 48px;
  height: 24px;
  vertical-align: bottom;
  margin-bottom: -2px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: 0.4s;
  border-radius: 24px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.4s;
  border-radius: 50%;
}

input:checked + .slider {
  background-color: #007bff;
}

input:focus + .slider {
  box-shadow: 0 0 1px #007bff;
}

input:checked + .slider:before {
  transform: translateX(24px);
}

.mark-all-button {
  padding: 0.5rem 1rem;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
  transition: background-color 0.3s ease;
}

.mark-all-button:hover {
  background-color: #0056b3;
}

.notification-list {
  list-style: none;
  padding: 0;
}

.notification-item {
  border-bottom: 1px solid #eee;
  padding: 1rem 0;
}

.actor-info {
  margin-bottom: 0.5rem;
  font-size: 1.1rem;
  color: #2c3e50;
}

.notification-type {
  margin-bottom: 0.5rem;
}

.type-badge {
  background-color: #e9ecef;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.875rem;
  color: #495057;
}

.notification-content {
  margin: 0.5rem 0;
  color: #212529;
}

.notification-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.875rem;
  color: #6c757d;
}

.loading,
.empty,
.error {
  text-align: center;
  padding: 2rem;
  color: #6c757d;
}

.error {
  color: #dc3545;
}

/* 已读未读消息分隔线样式 */
.divider {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 1rem 0;
  padding: 0.5rem 0;
}

.divider-line {
  flex: 1;
  height: 1px;
  background-color: #e0e0e0;
}

.divider-text {
  padding: 0 1rem;
  color: #999;
  font-size: 0.875rem;
  font-weight: 500;
}

/* 已读和未读通知的不同样式 */
.notification-item {
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.notification-item:hover {
  background-color: #f5f5f5;
}

.notification-item.unread {
  font-weight: 500;
  background-color: rgba(0, 123, 255, 0.05);
}

.notification-item.read {
  color: #6c757d;
}

/* 加载遮罩 */
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(0, 0, 0, 0.1);
  border-left-color: #333;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
