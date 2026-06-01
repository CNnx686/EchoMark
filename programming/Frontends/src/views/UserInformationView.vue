<script setup lang="ts">
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import { ref, onMounted } from "vue";
import type { ApiResponse } from "@/types/Audio";
import { UserProfileDTO, UserProfileUpdateRequestDTO } from "@/types/User";
import type { AudioResponseDto } from "@/types/Audio";

// 资源服务器
const GetAudioUrlPrefix = "http://localhost:5000";

const exitConfirm = ref(false);

const router = useRouter();

const { t } = useI18n();

const UserInfo = ref<UserProfileDTO>();

// 音频列表数据
const audioList = ref<AudioResponseDto[]>([]);
const pageLoading = ref(true);
const error = ref<string | null>(null);

// 用于编辑时的临时数据
const EditingData = ref<UserProfileUpdateRequestDTO>({
  nickname: "",
  avatarUrl: "",
  bio: "",
  selfDescription: "",
});

const getAuthToken = () => localStorage.getItem("token");

// 获取用户音频列表
const fetchUserAudios = async () => {
  pageLoading.value = true;
  error.value = null;
  try {
    const token = getAuthToken();
    if (!UserInfo.value?.userId) {
      throw new Error(t("userInformation.userIdNotFound"));
    }
    const url = `/api/audio/all/${UserInfo.value.userId}`;
    const response = await fetch(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const result: ApiResponse<AudioResponseDto[]> = await response.json();
    if (result.code === 200 && Array.isArray(result.data)) {
      audioList.value = result.data;
    } else {
      error.value = t("userInformation.loadAudioFailed");
    }
  } catch (err) {
    console.error("Failed to load user audios:", err);
    error.value = t("userInformation.networkError");
  } finally {
    pageLoading.value = false;
  }
};

// 跳转到音频详情页
const goToDetail = (id: number) => {
  router.push(`/audio/${id}`);
};

// 获取用户信息
const GetUserInformation = async () => {
  pageLoading.value = true;
  try {
    const token = getAuthToken();
    if (!token) {
      router.push("/login");
      return;
    }
    
    // 先尝试从localStorage获取缓存的用户信息
    const cachedUserInfo = localStorage.getItem("userInfo");
    if (cachedUserInfo) {
      try {
        const parsedUserInfo = JSON.parse(cachedUserInfo) as UserProfileDTO;
        // 缓存有效，直接使用
        UserInfo.value = parsedUserInfo;
        return;
      } catch (e) {
        localStorage.removeItem("userInfo");
      }
    }
    
    // 没有缓存或缓存无效，从API获取
    const url = `/api/users/profile`;
    const response = await fetch(url, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const result: ApiResponse<UserProfileDTO> = await response.json();
    if (result.code === 200) {
      UserInfo.value = result.data;
      // 更新缓存
      localStorage.setItem("userInfo", JSON.stringify(result.data));
    } else {
      router.push("/login");
      localStorage.removeItem("userInfo");
    }
  } catch (err) {
    router.push("/login");
    localStorage.removeItem("userInfo");
  } finally {
    pageLoading.value = false;
  }
};

// 隐藏的文件输入框引用
const avatarFileInput = ref<HTMLInputElement | null>(null);

// 更换头像函数
const changeAvatar = () => {
  // 触发文件选择对话框
  avatarFileInput.value?.click();
};

// 处理头像文件选择
const handleAvatarFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (!input.files || input.files.length === 0) return;

  const file = input.files[0];

  // 验证文件类型
  if (!file.type.startsWith("image/")) {
    alert(t("userInformation.invalidImageType"));
    return;
  }

  // 验证文件大小 (限制为5MB)
  const maxSize = 5 * 1024 * 1024; // 5MB
  if (file.size > maxSize) {
    alert(t("userInformation.imageTooLarge"));
    return;
  }

  try {
    const token = getAuthToken();
    if (!token) {
      alert(t("userInformation.loginExpired"));
      router.push("/login");
      return;
    }

    // 构建FormData
    const formData = new FormData();

    // 如果UserInfo存在，填充现有信息
    if (UserInfo.value) {
      formData.append("nickname", UserInfo.value.nickname || "");
      formData.append("bio", UserInfo.value.bio || "");
      formData.append("selfDescription", UserInfo.value.selfDescription || "");
    }

    // 添加头像文件
    formData.append("avatar", file);

    // 发送请求
    pageLoading.value = true;
    const url = "/api/users/profile";
    const response = await fetch(url, {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
      },
      body: formData,
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const result: ApiResponse<UserProfileDTO> = await response.json();
    if (result.code === 200 && result.data) {
      // 更新用户信息
      UserInfo.value = result.data;
      // 更新localStorage缓存
      localStorage.setItem("userInfo", JSON.stringify(result.data));
      alert(t("userInformation.avatarUpdateSuccess"));
    } else {
      alert(t("userInformation.avatarUpdateFailed"));
    }
  } catch (err) {
    console.error("Failed to update avatar:", err);
    alert(t("userInformation.avatarUpdateFailed"));
  } finally {
    // 清空文件输入，允许重新选择同一文件
    if (input) {
      input.value = "";
    }
    pageLoading.value = false;
  }
};

const exit = async () => {
  const url = "/api/auth/logout";
  const response = await fetch(url, {});
  // 清理用户信息和头像资源
  UserInfo.value = undefined;
  localStorage.clear();
  router.push("/");
};

const showEditModal = ref(false);

const EditProfile = () => {
  if (UserInfo.value) {
    // 初始化编辑数据为当前用户信息
    EditingData.value = {
      nickname: UserInfo.value.nickname || "",
      avatarUrl: "",
      bio: UserInfo.value.bio || "",
      selfDescription: UserInfo.value.selfDescription || "",
    };
    showEditModal.value = true;
  }
};

const updateProfile = async () => {
  const token = getAuthToken();
  const url = "/api/users/profile";
  if (!token) {
    alert(t("userInformation.loginExpired"));
    router.push("/login");
    return;
  }
  const data = EditingData.value;
  if (data.nickname?.trim() == "" || data.bio?.trim() == "" || data.selfDescription?.trim() == "") {
    alert(t("userInformation.emptyFieldsError"));
    return;
  }
  const formData = new FormData();

  formData.append("nickname", EditingData.value.nickname?.trim() || "");
  formData.append("bio", EditingData.value.bio?.trim() || "");
  formData.append("selfDescription", EditingData.value.selfDescription?.trim() || "");

  try {
    pageLoading.value = true;
    const response = await fetch(url, {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
      },
      body: formData,
    });

    if (response.ok) {
      const result: ApiResponse<UserProfileDTO> = await response.json();
      if (result.code === 200) {
        UserInfo.value = result.data;
        // 更新localStorage缓存
        localStorage.setItem("userInfo", JSON.stringify(result.data));
        alert(t("userInformation.updateSuccess"));
        showEditModal.value = false;
      } else {
        alert("更新失败：" + (result.msg || "未知错误"));
      }
    } else {
      const errorText = await response.text();
      console.error("Server error:", errorText);
      alert(t("userInformation.requestFailed"));
    }
  } catch (err) {
    console.error(err);
    alert(t("userInformation.networkError"));
  } finally {
    pageLoading.value = false;
  }
};

const handleAvatarError = (e: Event) => {
  const img = e.target as HTMLImageElement;
  if (img.src !== window.location.origin + "/DefaultAvatar.jpeg") {
    img.src = "/DefaultAvatar.jpeg"; // 切换到默认头像
  }
};

// 初始化加载
onMounted(async () => {
  pageLoading.value = true;
  try {
    await GetUserInformation();
    await fetchUserAudios();
  } finally {
    pageLoading.value = false;
  }
});
</script>

<template>
  <div class="userInformation">
    <!-- 加载遮罩 -->
    <div
      v-if="pageLoading"
      class="loading-overlay"
    >
      <div class="loading-spinner" />
    </div>
    <div class="content-avatar">
      <img
        :src="UserInfo?.avatarUrl || '/DefaultAvatar.jpeg'"
        alt="Avatar"
        class="user-avatar"
        @error="handleAvatarError"
      >
      <!-- 隐藏的文件输入框 -->
      <input
        ref="avatarFileInput"
        type="file"
        accept="image/*"
        style="display: none"
        @change="handleAvatarFileChange"
      >

      <div class="info-section-wrapper">
        <div class="info-section">
          <!-- 编辑按钮（右上角） -->
          <div class="edit-btn-container">
            <button
              class="btn btn-sm btn-outline-primary edit-btn-container"
              @click="EditProfile"
            >
              <img
                class="pic"
                src="/editProfile.jpg"
              >
            </button>
          </div>

          <div class="info-item">
            <label>{{ t("userInformation.userId") }}:</label>
            <span>{{ UserInfo?.userId }}</span>
          </div>
          <div class="info-item">
            <label>{{ t("userInformation.nickname") }}:</label>
            <span>{{ UserInfo?.nickname }}</span>
          </div>
          <div class="info-item">
            <label>{{ t("userInformation.bio") }}:</label>
            <span>{{ UserInfo?.bio }}</span>
          </div>
          <div class="info-item bio-item">
            <label>{{ t("userInformation.description") }}:</label>
            <p>{{ UserInfo?.selfDescription }}</p>
          </div>
        </div>
      </div>

      <div class="btns">
        <button
          class="btn btn-info user-avatar-btn"
          @click="changeAvatar"
        >
          {{ t("userInformation.changeAvatar") }}
        </button>
        <button
          class="btn btn-info user-avatar-btn"
          @click="exitConfirm = true"
        >
          {{ t("userInformation.exit") }}
        </button>
      </div>
    </div>

    <div
      v-if="exitConfirm"
      class="modal-backdrop fade show"
    />
    <div
      v-if="exitConfirm"
      class="modal d-block"
      aria-modal="true"
    >
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content decorated-dialog">
          <div class="modal-header">
            <h5 class="modal-title">
              {{ t("userInformation.logoutConfirmTitle") }}
            </h5>
            <button
              type="button"
              class="btn-close"
              @click="exitConfirm = false"
            >
              x
            </button>
          </div>
          <div class="modal-body">
            <p>{{ t("userInformation.logoutConfirmMessage") }}</p>
          </div>
          <div class="modal-footer">
            <button
              class="btn btn-outline-secondary"
              @click="exitConfirm = false"
            >
              {{ t("userInformation.cancel") }}
            </button>
            <button
              class="btn btn-danger"
              @click="exit"
            >
              {{ t("userInformation.confirm") }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 音频列表区域 -->
    <div class="audio-list-section mt-8">
      <h3 class="text-center mb-4 fw-bold">
        {{ t("userInformation.myAudioPosts") }}
      </h3>

      <!-- 错误提示 -->
      <div
        v-if="error"
        class="alert alert-warning text-center"
        role="alert"
      >
        {{ error }}
        <button
          class="btn btn-sm btn-outline-primary mt-2"
          @click="fetchUserAudios"
        >
          重试
        </button>
      </div>

      <!-- 音频列表 -->
      <div
        v-else
        class="container"
      >
        <div class="row g-4">
          <div
            v-for="audio in audioList"
            :key="audio.id"
            class="col-12"
            style="cursor: pointer"
            @click="goToDetail(audio.id)"
          >
            <div class="card shadow-sm border-0 rounded-3 h-100">
              <div class="row g-0">
                <!-- 左侧图片 -->
                <div class="col-md-3 col-12">
                  <img
                    :src="GetAudioUrlPrefix + audio.photoUrl"
                    class="img-fluid object-fit-cover"
                    alt="Audio cover"
                  >
                </div>

                <!-- 右侧内容 -->
                <div class="col-md-9 col-12">
                  <div class="card-body d-flex flex-column h-100">
                    <!-- 标题 + 用户名 -->
                    <div class="d-flex justify-content-between align-items-start mb-2">
                      <h5
                        class="card-title mb-0 text-truncate"
                        style="max-width: 70%"
                      >
                        {{ audio.title || t("userInformation.untitled") }}
                      </h5>
                      <small class="text-muted">{{ audio.userName }}</small>
                    </div>

                    <!-- 描述 -->
                    <p class="card-text text-muted flex-grow-1 mb-3">
                      {{ audio.description || t("userInformation.noDescription") }}
                    </p>

                    <!-- 音频播放器 -->
                    <div class="mt-auto">
                      <audio
                        controls
                        :src="GetAudioUrlPrefix + audio.audioUrl"
                        class="w-100"
                        @click.stop
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div
            v-if="!pageLoading && audioList.length === 0"
            class="text-center py-5"
          >
            <p class="text-muted">
              {{ t("userInformation.noAudioPosts") }}
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 编辑资料模态框 -->
  <div
    v-if="showEditModal"
    class="modal-backdrop fade show"
  />
  <div
    v-if="showEditModal"
    class="modal d-block"
    aria-modal="true"
  >
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content decorated-dialog">
        <div class="modal-header">
          <h5 class="modal-title">
            {{ t("userInformation.editProfile") }}
          </h5>
          <button
            type="button"
            class="btn-close"
            @click="showEditModal = false"
          >
            x
          </button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label">{{ t("userInformation.nickname") }}</label>
            <input
              v-model="EditingData.nickname"
              type="text"
              class="form-control"
              maxlength="30"
            >
          </div>
          <div class="mb-3">
            <label class="form-label">{{ t("userInformation.bio") }}</label>
            <input
              v-model="EditingData.bio"
              type="text"
              class="form-control"
              maxlength="100"
            >
          </div>
          <div class="mb-3">
            <label class="form-label">{{ t("userInformation.description") }}</label>
            <textarea
              v-model="EditingData.selfDescription"
              class="form-control"
              rows="4"
              maxlength="500"
            />
          </div>
        </div>
        <div class="modal-footer">
          <button
            class="btn btn-outline-secondary"
            @click="showEditModal = false"
          >
            {{ t("userInformation.cancel") }}
          </button>
          <button
            class="btn btn-primary"
            @click="updateProfile"
          >
            {{ t("userInformation.confirm") }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.userInformation {
  width: 100%;
  padding: 2rem;
  box-sizing: border-box;
}

.user-avatar {
  height: 10rem;
  width: 10rem;
  border-radius: 50%;
  object-fit: cover;
  object-position: center;
}

.content-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3rem;
  margin: 0 auto;
}

.btns {
  display: flex;
  flex-direction: row;
  gap: 5%;
  width: 100%;
  object-position: center;
  justify-content: center;
  align-items: center;
}

.user-avatar-btn {
  width: 15%;
  padding: 0.75rem;
  font-size: 1rem;
}

.info-section-wrapper {
  width: 100%;
  max-width: 28rem;
}

.info-section {
  background: #f9f9f9;
  padding: 1.5rem;
  border-radius: 0.75rem;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
  position: relative;
}

.edit-btn-container {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  width: 2.5rem;
  height: 2.5rem;
  border: white;
}
.edit-btn-container:hover {
  transform: scale(1.1); /* 悬停后按钮大小 */
  background-color: transparent;
}
.pic {
  object-fit: cover;
  height: 100%;
  width: 100%;
}

.info-item {
  margin-bottom: 1rem;
  display: flex;
  flex-direction: column;
}

.info-item label {
  font-weight: bold;
  color: #555;
  font-size: 0.95rem;
  margin-bottom: 0.25rem;
}

.info-item span,
.info-item p {
  font-size: 1rem;
  color: #333;
  word-break: break-word;
}

.bio-item p {
  white-space: pre-line; /* 保留换行符 */
  margin: 0;
}

/* 音频列表区域样式 */
.audio-list-section {
  padding-top: 2rem;
  padding-bottom: 2rem;
}

.audio-list-section .card {
  transition: transform 0.2s, box-shadow 0.2s;
}

.audio-list-section .card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.audio-list-section .object-fit-cover {
  height: auto;
  max-height: 23.5vh; /* 限制高度 */
  width: auto;
}

.mt-8 {
  margin-top: 2rem;
}

/* 加载遮罩样式 */
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 10000;
  display: flex;
  justify-content: center;
  align-items: center;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 5px solid rgba(255, 255, 255, 0.3);
  border-left-color: #fff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
