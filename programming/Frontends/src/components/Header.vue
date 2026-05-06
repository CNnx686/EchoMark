<script setup lang="ts">
import { onMounted, ref, onBeforeUnmount } from "vue";
import { useRouter } from "vue-router";
import { UserProfileDTO } from "@/types/User";
import i18n, { languageOptions } from "../i18n";
import type { ApiResponse } from "@/types/Audio";

const router = useRouter();
const { t } = i18n.global;

const getAuthToken = () => localStorage.getItem("token");

// 语言相关
const languages = languageOptions;
const currentLanguage = ref(i18n.global.locale.value);
const isDropdownOpen = ref(false);

const toggleDropdown = () => (isDropdownOpen.value = !isDropdownOpen.value);

const selectLanguage = (code: string) => {
  currentLanguage.value = code;
  i18n.global.locale.value = code;
  isDropdownOpen.value = false;
};

// 用户信息
const userInfo = ref<UserProfileDTO | null>(null);

// 用户设置
const userSettings = ref({ notificationMute: false });

// 路由跳转
const goToHome = () => router.push("/");

// 发现页面
const goToDiscovery = async () => {
  const isAuthenticated = await checkAuth();
  router.push(isAuthenticated ? "/discovery" : "/");
};

// 未读通知数量响应式变量
const unreadNotificationCount = ref(0);

const goToNotification = async () => {
  const isAuthenticated = await checkAuth();
  if (isAuthenticated) {
    router.push("/notification");
  } else {
    router.push("/");
  }
};

const gotoFavourite = async () => {
  const isAuthenticated = await checkAuth();
  router.push(isAuthenticated ? "/MyFavourite" : "/");
};

const gotoUserInformation = async () => {
  try {
    const token = getAuthToken();
    if (!token) {
      router.push("/login");
      return;
    }
    
    // 检查内存中的userInfo
    if (userInfo.value?.userId) {
      router.push(`/userInformation/${userInfo.value.userId}`);
      return;
    }
    
    // 尝试从localStorage获取缓存
    const cachedUserInfo = localStorage.getItem("userInfo");
    if (cachedUserInfo) {
      try {
        const parsedUserInfo = JSON.parse(cachedUserInfo) as UserProfileDTO;
        if (parsedUserInfo.userId) {
          userInfo.value = parsedUserInfo;
          router.push(`/userInformation/${parsedUserInfo.userId}`);
          return;
        }
      } catch (e) {
        localStorage.removeItem("userInfo");
      }
    }
    
    // 重新获取用户信息
    await fetchUserInfo();
    if (userInfo.value?.userId) {
      router.push(`/userInformation/${userInfo.value.userId}`);
    } else {
      // 如果获取失败，尝试使用原来的接口
      const response = await fetch(`/api/auth/me`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const result = await response.json();
      if (result.code === 200 && result.data?.userId) {
        router.push(`/userInformation/${result.data.userId}`);
      } else {
        router.push("/login");
      }
    }
  } catch (err) {
    router.push("/login");
  }
};

const checkAuth = async (): Promise<boolean> => {
  try {
    const token = getAuthToken();
    if (!token) return false;
    
    // 先检查内存中的userInfo
    if (userInfo.value) {
      return true;
    }
    
    // 尝试从localStorage获取缓存
    const cachedUserInfo = localStorage.getItem("userInfo");
    if (cachedUserInfo) {
      try {
        const parsedUserInfo = JSON.parse(cachedUserInfo) as UserProfileDTO;
        // 缓存有效，直接更新userInfo并返回已认证
        userInfo.value = parsedUserInfo;
        return true;
      } catch (e) {
        localStorage.removeItem("userInfo");
      }
    }
    
    // 没有缓存或缓存无效，请求API验证
    const response = await fetch(`/api/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!response.ok) return false;
    
    // 只需要知道用户是否已认证
    const result = await response.json();
    if (result.code === 200) {
      // 如果认证成功但没有用户信息，重新获取
      if (!userInfo.value) {
        await fetchUserInfo();
      }
      return true;
    }
    return false;
  } catch {
    return false;
  }
};

// 获取用户信息
const fetchUserInfo = async (): Promise<void> => {
  try {
    const token = getAuthToken();
    if (!token) {
      userInfo.value = null;
      localStorage.removeItem("userInfo");
      return;
    }
    
    // 先尝试从localStorage获取缓存的用户信息
    const cachedUserInfo = localStorage.getItem("userInfo");
    if (cachedUserInfo) {
      try {
        const parsedUserInfo = JSON.parse(cachedUserInfo) as UserProfileDTO;
        // 缓存有效，直接使用
        userInfo.value = parsedUserInfo;
        return;
      } catch (e) {
        localStorage.removeItem("userInfo");
      }
    }
    
    // 没有缓存或缓存无效，从API获取
    const response = await fetch(`/api/users/profile`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!response.ok) {
      userInfo.value = null;
      localStorage.removeItem("userInfo");
      return;
    }
    const result: ApiResponse<UserProfileDTO> = await response.json();
    if (result.code === 200) {
      userInfo.value = result.data;
      // 更新缓存
      localStorage.setItem("userInfo", JSON.stringify(result.data));
    } else {
      userInfo.value = null;
      localStorage.removeItem("userInfo");
    }
  } catch {
    userInfo.value = null;
    localStorage.removeItem("userInfo");
  }
};

// 获取用户设置
const fetchUserSettings = async (isManualToggle: boolean = false): Promise<{ notificationMute: boolean }> => {
  try {
    // 定义默认设置
    const defaultSettings = { notificationMute: false };
    
    // 先尝试从localStorage获取缓存的用户设置
    const cachedSettings = localStorage.getItem("userSettings");
    if (cachedSettings) {
      try {
        const parsedSettings = JSON.parse(cachedSettings);
        // 缓存有效，更新响应式变量并返回
        userSettings.value = parsedSettings;
        // 根据静默设置更新未读通知显示
        if (parsedSettings.notificationMute) {
          unreadNotificationCount.value = 0;
        } else if (isManualToggle) {
          // 只有在用户手动切换且关闭静默时，才重新获取未读数量
          fetchNotifications();
        }
        return parsedSettings;
      } catch (e) {
        localStorage.removeItem("userSettings");
      }
    }
    
    // 没有缓存或缓存无效，检查是否有token
    const token = getAuthToken();
    if (!token) {
      // 如果没有token，使用默认设置
      userSettings.value = defaultSettings;
      return defaultSettings;
    }
    
    // 有token，从API获取
    const response = await fetch("/api/users/settings", {
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });
    
    if (!response.ok) {
      // API请求失败，使用默认设置
      userSettings.value = defaultSettings;
      return defaultSettings;
    }
    
    const result: ApiResponse<{ notificationMute: boolean }> = await response.json();
    if (result.code === 200 && result.data) {
      // 更新本地存储
      localStorage.setItem("userSettings", JSON.stringify(result.data));
      // 更新响应式变量
      userSettings.value = result.data;
      // 根据静默设置更新未读通知显示
      if (result.data.notificationMute) {
        unreadNotificationCount.value = 0;
      } else if (isManualToggle) {
        // 只有在用户手动切换且关闭静默时，才重新获取未读数量
        fetchNotifications();
      }
      return result.data;
    }
    
    // API返回但数据无效，使用默认设置
    userSettings.value = defaultSettings;
    return defaultSettings;
  } catch (e) {
    console.warn("Failed to fetch user settings", e);
    // 异常情况下使用默认设置
    const defaultSettings = { notificationMute: false };
    userSettings.value = defaultSettings;
    return defaultSettings;
  }
};

// 从服务器获取所有通知并本地计算未读数量
const fetchNotifications = async () => {
  const token = getAuthToken();
  if (!token) {
    unreadNotificationCount.value = 0;
    return;
  }
  
  try {
    const response = await fetch("/api/notifications/me", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    
    const result: ApiResponse<{ id: number; isRead: boolean }[]> = await response.json();
    if (result.code === 200 && Array.isArray(result.data)) {
      // 本地计算未读通知数量
      const unreadCount = result.data.filter(notification => !notification.isRead).length;
      unreadNotificationCount.value = userSettings.value.notificationMute ? 0 : unreadCount;
      
      // 更新本地存储的未读通知ID列表
      const unreadIds = result.data.filter(notification => !notification.isRead).map(notification => notification.id);
      localStorage.setItem("pendingNotificationIds", JSON.stringify(unreadIds));
    }
  } catch (err) {
    console.warn("Failed to fetch notifications", err);
    // 如果获取失败，回退到本地存储的方式
    checkUnreadNotifications();
  }
};

// 未读通知检查（从本地存储）
const checkUnreadNotifications = () => {
  try {
    const raw = localStorage.getItem("pendingNotificationIds");
    if (!raw) {
      unreadNotificationCount.value = 0;
      return;
    }
    
    // 检查用户设置，如果开启了静默通知，则不显示未读标记
    if (userSettings.value.notificationMute) {
      unreadNotificationCount.value = 0;
      return;
    }
    
    const ids = JSON.parse(raw);
    const validCount = ids.filter(
      (id: unknown) =>
        (typeof id === "number" && !isNaN(id)) || (typeof id === "string" && !isNaN(Number(id)))
    ).length;
    unreadNotificationCount.value = validCount;
  } catch (e) {
    console.warn("Failed to parse pendingNotificationIds for unread check", e);
    unreadNotificationCount.value = 0;
  }
};

// SSE 相关
let notificationSseAbortController: AbortController | null = null;
let sseReconnectTimeout: ReturnType<typeof setTimeout> | null = null;
const MAX_RECONNECT_ATTEMPTS = 5;
let reconnectAttempt = 0;

// 定义通知创建事件类型
interface NotificationCreatedEvent {
  notificationId: number;
  receiverUserId: number;
  createdAt: string;
}

const mergeNotificationIds = (newIds: number[]) => {
  try {
    let existingIds: number[] = [];
    const raw = localStorage.getItem("pendingNotificationIds");
    if (raw) {
      existingIds = JSON.parse(raw)
        .filter(
          (id: unknown) => typeof id === "number" || (typeof id === "string" && !isNaN(Number(id)))
        )
        .map((id: number | string) => Number(id));
    }

    const idSet = new Set(existingIds);
    let newNotificationsCount = 0;
    
    // 统计新增的通知数量
    for (const id of newIds) {
      if (!isNaN(id) && !idSet.has(id)) {
        idSet.add(id);
        newNotificationsCount++;
      }
    }

    const merged = Array.from(idSet);
    localStorage.setItem("pendingNotificationIds", JSON.stringify(merged));

    // 如果有新增通知，增加未读数量（考虑静默设置）
    if (newNotificationsCount > 0 && !userSettings.value.notificationMute) {
      unreadNotificationCount.value += newNotificationsCount;
    }
  } catch (e) {
    console.error("Failed to merge notification IDs:", e);
    // 如果处理失败，回退到重新计算未读数量
    checkUnreadNotifications();
  }
};

const processSseEvent = (eventName: string, data: NotificationCreatedEvent) => {
  try {
    if (eventName === "notification-created") {
      // 处理通知创建事件
      const notificationId = data.notificationId;
      if (typeof notificationId === "number" && !isNaN(notificationId)) {
        mergeNotificationIds([notificationId]);
      }
    }
  } catch (e) {
    console.error("Failed to process SSE event:", eventName, data, e);
  }
};

const parseSseEvent = (eventText: string) => {
  const event: { name?: string; data?: string } = {};
  const lines = eventText.split("\n");

  for (const line of lines) {
    const trimmedLine = line.trim();
    if (!trimmedLine) continue;

    if (trimmedLine.startsWith("event:")) {
      event.name = trimmedLine.substring(6).trim();
    } else if (trimmedLine.startsWith("data:")) {
      const dataPart = trimmedLine.substring(5).trim();
      event.data = event.data ? `${event.data}\n${dataPart}` : dataPart;
    }
  }

  return event;
};

const startNotificationSSE = () => {
  const token = getAuthToken();
  if (!token) return;
  if (notificationSseAbortController) return;

  // 重置重连计数
  reconnectAttempt = 0;

  notificationSseAbortController = new AbortController();
  const { signal } = notificationSseAbortController;

  fetch("/api/sse/subscribe", {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: "text/event-stream",
    },
    signal,
  })
    .then((response) => {
      if (!response.ok || !response.body) {
       handleSseDisconnect();
        return;
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = "";

      const read = () => {
        reader
          .read()
          .then(({ done, value }) => {
            if (done) {
              handleSseDisconnect();
              return;
            }

            buffer += decoder.decode(value, { stream: true });
            const events = buffer.split("\n\n");
            buffer = events.pop() || "";

            for (const eventText of events) {
              if (!eventText.trim()) continue;
              const { name, data } = parseSseEvent(eventText);
              if (data) {
                try {
                  const parsedData = JSON.parse(data);
                  processSseEvent(name || "message", parsedData);
                } catch (e) {
                  console.error("Invalid SSE JSON data:", data, e);
                }
              }
            }

            read();
          })
          .catch((err) => {
            if (!signal.aborted) {
              console.error("Notification SSE read error:", err);
              handleSseDisconnect();
            }
          });
      };

      read();
    })
    .catch((err) => {
      if (!signal?.aborted) {
        console.error("Failed to establish notification SSE:", err);
        handleSseDisconnect();
      }
    });
};

const handleSseDisconnect = () => {
  cleanupNotificationSSE();
  
  // 尝试重连
  if (reconnectAttempt < MAX_RECONNECT_ATTEMPTS) {
    reconnectAttempt++;
    const delay = Math.min(1000 * Math.pow(2, reconnectAttempt), 30000); // 指数退避
    console.log(`Attempting to reconnect to SSE in ${delay}ms... (${reconnectAttempt}/${MAX_RECONNECT_ATTEMPTS})`);
    
    sseReconnectTimeout = setTimeout(() => {
      startNotificationSSE();
    }, delay);
  } else {
    console.error("Max SSE reconnection attempts reached");
  }
};

const cleanupNotificationSSE = () => {
  if (notificationSseAbortController) {
    notificationSseAbortController.abort();
    notificationSseAbortController = null;
  }
  
  if (sseReconnectTimeout) {
    clearTimeout(sseReconnectTimeout);
    sseReconnectTimeout = null;
  }
};

// 路由监听，用于在路由变化时检查用户登录状态
let routeChangeHandler: (() => void) | null = null;

const setupRouteListener = () => {
  routeChangeHandler = () => {
    const token = getAuthToken();
    
    // 根据token重新检查用户信息
    if (token) {
      // 有token，获取最新用户信息
      fetchUserInfo();
      // 启动通知SSE
      startNotificationSSE();
    } else {
      // 没有token，清空用户信息
      if (userInfo.value !== null) {
        userInfo.value = null;
        // 清理通知相关资源
        cleanupNotificationSSE();
      }
    }
  };

  // 监听路由变化
  window.addEventListener('popstate', routeChangeHandler);
  
  const originalPushState = history.pushState;
  history.pushState = function(...args) {
    originalPushState.apply(this, args);
    window.dispatchEvent(new Event('pushstate'));
  };
  
  const originalReplaceState = history.replaceState;
  history.replaceState = function(...args) {
    originalReplaceState.apply(this, args);
    window.dispatchEvent(new Event('replacestate'));
  };
  
  window.addEventListener('pushstate', routeChangeHandler);
  window.addEventListener('replacestate', routeChangeHandler);
};

const cleanupRouteListener = () => {
  if (routeChangeHandler) {
    window.removeEventListener('popstate', routeChangeHandler);
    window.removeEventListener('pushstate', routeChangeHandler);
    window.removeEventListener('replacestate', routeChangeHandler);
    routeChangeHandler = null;
  }
};

// 生命周期
onMounted(async () => {
  // 获取用户信息
  await fetchUserInfo();
  // 获取用户设置
  await fetchUserSettings();
  // 初始化未读状态（从服务器获取最新数据）
  await fetchNotifications();
  
  startNotificationSSE();
  // 设置路由监听
  setupRouteListener();
  
  // 添加localStorage变化监听，以便在其他标签页修改pendingNotificationIds或userSettings时更新红点
  window.addEventListener('storage', handleStorageChange);
  // 添加自定义事件监听，以便在同标签页修改pendingNotificationIds时更新红点
  window.addEventListener('pendingNotificationChange', handlePendingNotificationChange);
  // 添加自定义事件监听，以便在同标签页修改userSettings时更新红点
  window.addEventListener('userSettingsChange', handleUserSettingsChange);
});

onBeforeUnmount(() => {
  cleanupNotificationSSE();
  cleanupRouteListener();
  // 移除localStorage变化监听
  window.removeEventListener('storage', handleStorageChange);
  // 移除自定义事件监听
  window.removeEventListener('pendingNotificationChange', handlePendingNotificationChange);
  // 移除用户设置变化监听
  window.removeEventListener('userSettingsChange', handleUserSettingsChange);
});

// 处理用户设置变化事件
const handleUserSettingsChange = () => {
  // 用户手动切换设置，传递isManualToggle参数为true
  fetchUserSettings(true);
};

// 处理localStorage变化
const handleStorageChange = (event: StorageEvent) => {
  if (event.key === 'pendingNotificationIds' || event.key === 'userSettings') {
    // 如果是userSettings变化，需要重新获取用户设置
    if (event.key === 'userSettings') {
      fetchUserSettings();
    } else if (event.key === 'pendingNotificationIds') {
      // 如果是未读通知ID变化，重新从服务器获取最新数量
      fetchNotifications();
    }
  }
};

// 处理同标签页的通知变化事件
const handlePendingNotificationChange = () => {
  // 当标记已读时，重新从服务器获取最新的未读数量
  fetchNotifications();
};
</script>

<template>
  <header class="header">
    <!-- Logo -->
    <div
      class="logo-container"
      @click="goToHome"
    >
      <img
        src="/EchoMarkLogo.png"
        alt="EchoMark Logo"
        class="logo"
      >
    </div>
    <!-- Discovery Button -->
    <button
      class="discovery-button"
      @click="goToDiscovery"
    >
      {{ t("header.discover") }}
    </button>
    <!-- Notification Button with unread badge -->
    <div class="notification-wrapper">
      <button
        class="discovery-button"
        @click="goToNotification"
      >
        {{ t("header.notification") }}
      </button>
      <span
        v-if="unreadNotificationCount > 0"
        class="unread-badge"
      >
        {{ unreadNotificationCount }}
      </span>
    </div>
    <!-- Favourite Button -->
    <button
      class="discovery-button"
      @click="gotoFavourite"
    >
      {{ t("header.Favourite") }}
    </button>
    <!-- Spacer -->
    <div class="spacer" />
    <!-- Language Dropdown -->
    <div class="language-dropdown">
      <button
        class="lang-btn"
        @click="toggleDropdown"
      >
        ▼ 语言-language
      </button>
      <ul
        v-if="isDropdownOpen"
        class="lang-menu"
      >
        <li
          v-for="lang in languages"
          :key="lang.code"
          :class="{ active: currentLanguage === lang.code }"
          @click="selectLanguage(lang.code)"
        >
          {{ lang.label }}
        </li>
      </ul>
    </div>
    <!-- Spacer between language and user -->
    <div class="language-user-spacer" />
    <!-- User Avatar -->
    <button
      class="userInformation-button"
      @click="gotoUserInformation"
    >
      <img
        :src="userInfo?.avatarUrl || '/DefaultAvatar.jpeg'"
        alt="User Avatar"
        class="userAvatar"
      >
    </button>
  </header>
</template>

<style scoped>
.header {
  height: 60px;
  background-color: #f8f9fa;
  border-bottom: 1px solid #eaeaea;
  display: flex;
  align-items: center;
  padding: 0 20px;
  box-sizing: border-box;
  width: 100vw;
}

.logo-container {
  cursor: pointer;
  margin-right: 20px;
  transition: transform 0.3s ease;
}

.logo-container:hover {
  transform: scale(1.2);
}

.logo {
  height: 40px;
  display: block;
}

.discovery-button {
  padding: 8px 16px;
  background-color: transparent;
  color: #007bff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 20px;
  transition: all 0.3s ease;
}

.discovery-button:hover {
  transform: scale(1.2);
  background-color: transparent;
  color: #0056b3;
}

.userInformation-button {
  width: 40px;
  height: 40px;
  padding: 2px;
  border: none;
  cursor: pointer;
  background-color: transparent;
}

.spacer {
  flex-grow: 1;
}

.language-user-spacer {
  width: 10px;
}

.language-dropdown {
  position: relative;
  display: flex;
  align-items: center;
}

.lang-btn {
  padding: 4px 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
  background-color: rgb(248, 254, 255);
  font-size: 14px;
  color: #333;
  cursor: pointer;
  outline: none;
  transition: border-color 0.3s ease;
}

.lang-btn:hover {
  border-color: #007bff;
}

.lang-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background-color: white;
  border: 1px solid #ccc;
  border-radius: 4px;
  list-style: none;
  padding: 0;
  margin: 4px 0 0 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  min-width: 120px;
}

.lang-menu li {
  padding: 8px 12px;
  cursor: pointer;
  text-align: left;
}

.lang-menu li:hover {
  background-color: #f0f0f0;
}

.lang-menu li.active {
  color: #007bff;
  font-weight: bold;
}

.userAvatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.userInformation-button:hover {
  transform: scale(1.2);
  background-color: transparent;
}

/* 未读通知徽章样式 */
.notification-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.unread-badge {
  position: absolute;
  top: -4px;
  right: -12px;
  min-width: 20px;
  height: 20px;
  background-color: #ff4d4f; /* 鲜红色 */
  color: white;
  font-size: 12px;
  font-weight: bold;
  border-radius: 10px;
  border: 2px solid white;
  box-shadow: 0 0 2px rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}
</style>
