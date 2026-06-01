<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import i18n from '@/i18n';
import type { ApiResponse } from '@/types/Audio';
import type { AudioResponseDto } from '@/types/Audio';
import type { UserProfileDTO } from '@/types/User';

const GetAssetPrefix = "http://localhost:5000";

const getAuthToken = () => localStorage.getItem('token');

const { t } = i18n.global;
const router = useRouter();

const loading = ref(false);
const error = ref<string | null>(null);
const audios = ref<AudioResponseDto[]>([]);
const userProfiles = ref<Map<number, UserProfileDTO>>(new Map());

const formatDuration = (isoDuration: string): string => {
  if (!isoDuration || !isoDuration.startsWith('PT')) return '0:00';
  const timePart = isoDuration.substring(2);
  const hourMatch = timePart.match(/(\d+)H/);
  const minuteMatch = timePart.match(/(\d+)M/);
  const secondMatch = timePart.match(/(\d+)S/);
  const hours = hourMatch ? parseInt(hourMatch[1], 10) : 0;
  const minutes = minuteMatch ? parseInt(minuteMatch[1], 10) : 0;
  const seconds = secondMatch ? parseInt(secondMatch[1], 10) : 0;

  if (hours > 0) {
    return `${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
};

const goToDetail = (id: number | string) => {
  router.push(`/audio/${id}`);
};

const fetchUserProfiles = async (userIds: number[]) => {
  const token = getAuthToken();
  if (!token) return;

  const uniqueUserIds = [...new Set(userIds)];
  const profileMap = new Map<number, UserProfileDTO>();

  const promises = uniqueUserIds.map(async (userId) => {
    try {
      const response = await fetch(`/api/users/${userId}/profile`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (response.ok) {
        const data: ApiResponse<UserProfileDTO> = await response.json();
        if (data.code === 200) {
          profileMap.set(userId, data.data);
        }
      }
    } catch (err) {
      console.warn(`Failed to load profile for user ${userId}:`, err);
    }
  });

  await Promise.all(promises);
  userProfiles.value = profileMap;
};

const fetchFavorites = async () => {
  const token = getAuthToken();
  if (!token) {
    router.push('/login');
    return;
  }

  loading.value = true;
  error.value = null;

  try {
    const favRes = await fetch('/api/users/favorites', {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (!favRes.ok) {
      if (favRes.status === 401) {
        router.push('/login');
        return;
      }
      throw new Error(`Failed to load favorites: ${favRes.status}`);
    }

    const favData: ApiResponse<number[]> = await favRes.json();
    if (favData.code !== 200 || !Array.isArray(favData.data)) {
      throw new Error(t('favorites.loadError'));
    }

    const ids = favData.data;
    if (ids.length === 0) {
      audios.value = [];
      loading.value = false;
      return;
    }

    const audioRes = await fetch('/api/audio/list', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(ids),
    });

    if (!audioRes.ok) {
      throw new Error(`Failed to load audio details: ${audioRes.status}`);
    }

    const audioData: ApiResponse<AudioResponseDto[]> = await audioRes.json();
    if (audioData.code === 200 && Array.isArray(audioData.data)) {
      audios.value = audioData.data;

      const userIds = audios.value.map(a => a.userId).filter(id => id != null) as number[];
      await fetchUserProfiles(userIds);
    } else {
      throw new Error(t('favorites.loadDetailError'));
    }
  } catch (err: any) {
    console.error('Fetch favorites failed:', err);
    error.value = err.message || t('common.unknownError');
  } finally {
    loading.value = false;
  }
};

const getUsername = (userId: number) => {
  const profile = userProfiles.value.get(userId);
  return profile?.nickname || String(userId);
};

onMounted(() => {
  fetchFavorites();
});
</script>

<template>
  <div class="favorites-page">
    <h1>{{ t("favorites.title") }}</h1>

    <div
      v-if="loading"
      class="loading"
    >
      {{ t("common.loading") }}...
    </div>
    <div
      v-else-if="error"
      class="error"
    >
      {{ error }}
    </div>
    <div
      v-else-if="audios.length === 0"
      class="empty"
    >
      {{ t("favorites.empty") }}
    </div>

    <div
      v-else
      class="audio-list"
    >
      <div
        v-for="audio in audios"
        :key="audio.id"
        class="audio-item"
        @click="goToDetail(audio.id)"
      >
        <img
          v-if="audio.photoUrl"
          :src="GetAssetPrefix + audio.photoUrl"
          :alt="audio.title"
          class="cover"
        >
        <div class="info">
          <h3 class="title">
            {{ audio.title }}
          </h3>
          <!-- ✅ 显示用户名 -->
          <p class="author">
            {{ t("audio.author") }}: {{ getUsername(audio.userId) }}
          </p>
          <!-- ⚠️ 注意：publishTime 应该是发布时间，不是时长！ -->
          <!-- 如果 audio.duration 是 ISO 字符串，才用 formatDuration -->
          <p class="duration">
            {{ formatDuration(audio.publishTime) }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>



<style scoped>
.favorites-page {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.favorites-page h1 {
  text-align: center;
  margin-bottom: 24px;
  color: #333;
}

.loading,
.error,
.empty {
  text-align: center;
  padding: 40px 20px;
  color: #666;
}

.error {
  color: #d32f2f;
}

.audio-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.audio-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.audio-item:hover {
  background-color: #f5f5f5;
}

.cover {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  margin-right: 16px;
}

.info {
  flex: 1;
}

.title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 6px 0;
  color: #1a1a1a;
}

.author,
.duration {
  font-size: 14px;
  color: #555;
  margin: 2px 0;
}
</style>
