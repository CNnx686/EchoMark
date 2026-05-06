<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import type { ApiResponse, AudioResponseDto, RecommendationResponse } from "@/types/Audio";
const GetAudioUrlPrefix = "http://101.37.31.227:5000";
const { t, locale } = useI18n();
const router = useRouter();

const audioList = ref<AudioResponseDto[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);

// 搜索相关
const searchKeyword = ref("");
const searchResults = ref<AudioResponseDto[]>([]);
const showSearchResults = ref(false);

// AI推荐相关
const aiRecommendationReason = ref("");
const aiRecommendationList = ref<AudioResponseDto[]>([]);
const showAiRecommendation = ref(false);
const aiLoading = ref(false); // AI推荐加载状态

// 获取认证token
const getAuthToken = () => localStorage.getItem("token");

// 获取推荐声音列表
const fetchRecommendations = async () => {
  loading.value = true;
  error.value = null;
  try {
    const response = await fetch("/api/audio/recommendation?limit=20");
    if (!response.ok) throw new Error(`HTTP ${response.status}`);

    const result: ApiResponse<AudioResponseDto[]> = await response.json();

    if (result.code === 200 && Array.isArray(result.data)) {
      audioList.value = result.data;
    } else {
      error.value = t("discovery.loadFailed");
    }
  } catch (err) {
    console.error("Failed to load recommendations:", err);
    error.value = t("discovery.networkError");
  } finally {
    loading.value = false;
  }
};

// 跳转到音频详情页
const goToDetail = (id: number) => {
  router.push(`/audio/${id}`);
};

// 搜索音频
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    showSearchResults.value = false;
    return;
  }

  loading.value = true;
  error.value = null;

  try {
    const response = await fetch(
      `/api/audio/search?keyword=${encodeURIComponent(searchKeyword.value.trim())}`
    );
    if (!response.ok) throw new Error(`HTTP ${response.status}`);

    const result: ApiResponse<AudioResponseDto[]> = await response.json();

    if (result.code === 200 && Array.isArray(result.data)) {
      searchResults.value = result.data;
      showSearchResults.value = true;
    } else {
      error.value = t("discovery.loadFailed");
    }
  } catch (err) {
    console.error("Failed to search audio:", err);
    error.value = t("discovery.networkError");
  } finally {
    loading.value = false;
  }
};

// 清除搜索结果
const clearSearch = () => {
  searchKeyword.value = "";
  showSearchResults.value = false;
};

// 切换推荐类型
const toggleRecommendation = async () => {
  if (showAiRecommendation.value) {
    // 如果当前是AI推荐，切换到热度推荐
    showAiRecommendation.value = false;
    // 重新获取热度推荐列表
    await fetchRecommendations();
  } else {
    // 如果当前是热度推荐，切换到AI推荐
    await fetchAiRecommendation();
  }
};

// 获取AI推荐
const fetchAiRecommendation = async () => {
  const token = getAuthToken();
  if (!token) {
    alert(t("discovery.loginRequired"));
    return;
  }

  aiLoading.value = true;
  aiRecommendationReason.value = "";
  aiRecommendationList.value = [];
  error.value = null;

  try {
    // 根据当前语言环境设置language参数
    const language = locale.value === 'en-US' ? 'English' : '简体中文';
    const response = await fetch(`/api/llm/recommendation?language=${encodeURIComponent(language)}`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) throw new Error(`HTTP ${response.status}`);

    const result: ApiResponse<RecommendationResponse> = await response.json();

    if (result.code === 200 && result.data) {
      aiRecommendationReason.value = result.data.reason;

      // 根据推荐的audioIds获取音频详情
      if (result.data.audioIds && result.data.audioIds.length > 0) {
        // 使用/api/audio/list批量获取音频详情
        const audioRes = await fetch("/api/audio/list", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify(result.data.audioIds),
        });

        if (audioRes.ok) {
          const audioData: ApiResponse<AudioResponseDto[]> = await audioRes.json();
          if (audioData.code === 200 && Array.isArray(audioData.data)) {
            aiRecommendationList.value = audioData.data;
          }
        }
      }

      showAiRecommendation.value = true;
      showSearchResults.value = false; // 隐藏搜索结果
    } else {
      error.value = t("discovery.aiRecommendFailed");
    }
  } catch (err) {
    console.error("Failed to get AI recommendation:", err);
    error.value = t("discovery.networkError");
  } finally {
    aiLoading.value = false;
  }
};

// 初始化加载
onMounted(() => {
  fetchRecommendations();
});
</script>

<template>
  <div class="discovery-page bg-light min-vh-100 py-4">
    <!-- 加载遮罩 -->
    <div
      v-if="loading"
      class="loading-overlay"
    >
      <div class="loading-spinner" />
    </div>
    <div class="container">
      <h2 class="text-center mb-4 fw-bold">
        {{ t("discovery.title") }}
      </h2>

      <!-- 搜索栏 -->
      <div class="mb-4">
        <div class="input-group">
          <input
            v-model="searchKeyword"
            type="text"
            class="form-control"
            :placeholder="t('discovery.searchPlaceholder')"
            @keyup.enter="handleSearch"
          >
          <button
            class="btn btn-primary"
            :disabled="loading"
            @click="handleSearch"
          >
            <span
              v-if="loading"
              class="spinner-border spinner-border-sm"
              role="status"
              aria-hidden="true"
            />
            {{ t("discovery.searchButton") }}
          </button>
          <button
            class="btn btn-info"
            :disabled="loading || aiLoading"
            @click="toggleRecommendation"
          >
            <span
              v-if="aiLoading"
              class="spinner-border spinner-border-sm"
              role="status"
              aria-hidden="true"
            />
            {{ showAiRecommendation
              ? t("discovery.hotRecommendButton")
              : t("discovery.aiRecommendButton")
            }}
          </button>
          <button
            v-if="searchKeyword"
            class="btn btn-outline-secondary"
            @click="clearSearch"
          >
            ×
          </button>
        </div>
        <!-- AI推荐等待提示 -->
        <small class="text-muted d-block mt-1 text-center">
          {{ t("discovery.aiRecommendWaitTime") }}
        </small>
      </div>

      <!-- 内容区域条件渲染 -->
      <div>
        <!-- 错误提示 -->
        <div
          v-if="error"
          class="alert alert-warning text-center"
          role="alert"
        >
          {{ error }}
          <button
            class="btn btn-sm btn-outline-primary mt-2"
            @click="fetchRecommendations"
          >
            {{ t("common.retry") }}
          </button>
        </div>

        <!-- 声音列表 -->
        <div
          v-else
          class="row g-4"
        >
          <!-- AI推荐结果 -->
          <template v-if="showAiRecommendation">
            <!-- AI推荐理由 -->
            <div class="col-12 mb-4">
              <div class="card bg-info bg-opacity-10 border-info">
                <div class="card-body">
                  <h5 class="card-title text-info">
                    🤖 {{ t("discovery.aiRecommendReason") }}
                  </h5>
                  <p class="card-text">
                    {{ aiRecommendationReason }}
                  </p>
                </div>
              </div>
            </div>

            <!-- 推荐的音频列表 -->
            <div
              v-for="audio in aiRecommendationList"
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
                          {{ audio.title || t("discovery.untitled") }}
                        </h5>
                        <small class="text-muted">{{ audio.userName }}</small>
                      </div>

                      <!-- 描述 -->
                      <p class="card-text text-muted flex-grow-1 mb-3">
                        {{ audio.description || t("discovery.noDescription") }}
                      </p>

                      <!-- 音频播放器 -->
                      <div class="mt-auto">
                        <audio
                          controls
                          preload="none"
                          :src="GetAudioUrlPrefix + audio.audioUrl"
                          class="w-100"
                          @click.stop
                        >
                          {{ t("discovery.audioNotSupported") }}
                        </audio>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- AI推荐空状态 -->
            <div
              v-if="!loading && aiRecommendationList.length === 0"
              class="text-center py-5"
            >
              <p class="text-muted">
                {{ t("discovery.noAiRecommendations") }}
              </p>
            </div>
          </template>

          <!-- 搜索结果 -->
          <template v-else-if="showSearchResults">
            <div
              v-for="audio in searchResults"
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
                          {{ audio.title || t("discovery.untitled") }}
                        </h5>
                        <small class="text-muted">{{ audio.userName }}</small>
                      </div>

                      <!-- 描述 -->
                      <p class="card-text text-muted flex-grow-1 mb-3">
                        {{ audio.description || t("discovery.noDescription") }}
                      </p>

                      <!-- 音频播放器 -->
                      <div class="mt-auto">
                        <audio
                          controls
                          preload="none"
                          :src="GetAudioUrlPrefix + audio.audioUrl"
                          class="w-100"
                          @click.stop
                        >
                          {{ t("discovery.audioNotSupported") }}
                        </audio>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 搜索空状态 -->
            <div
              v-if="!loading && searchResults.length === 0"
              class="text-center py-5"
            >
              <p class="text-muted">
                {{ t("discovery.noSearchResults") }}
              </p>
            </div>
          </template>

          <!-- 推荐列表 -->
          <template v-else-if="!showSearchResults && !showAiRecommendation">
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
                          {{ audio.title || t("discovery.untitled") }}
                        </h5>
                        <small class="text-muted">{{ audio.userName }}</small>
                      </div>

                      <!-- 描述 -->
                      <p class="card-text text-muted flex-grow-1 mb-3">
                        {{ audio.description || t("discovery.noDescription") }}
                      </p>

                      <!-- 音频播放器 -->
                      <div class="mt-auto">
                        <audio
                          controls
                          preload="none"
                          :src="GetAudioUrlPrefix + audio.audioUrl"
                          class="w-100"
                          @click.stop
                        >
                          {{ t("discovery.audioNotSupported") }}
                        </audio>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 推荐空状态 -->
            <div
              v-if="!loading && audioList.length === 0"
              class="text-center py-5"
            >
              <p class="text-muted">
                {{ t("discovery.noAudios") }}
              </p>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.discovery-page {
  padding-top: 1rem;
  padding-bottom: 2rem;
}

.card {
  transition: transform 0.2s, box-shadow 0.2s;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.object-fit-cover {
  height: auto;
  max-height: 23.5vh; /* 可选：限制高度 */
  width: auto;
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
