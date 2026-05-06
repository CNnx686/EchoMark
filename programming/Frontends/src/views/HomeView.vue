<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from "vue";
import { useI18n } from "vue-i18n";
import Router from "@/router";
// 导入录音组件
import RecordingModal from "@/components/RecordingModal.vue";
import { AudioResponseDto } from "@/types/Audio";

const { t } = useI18n();

// 地图实例引用
const mapInstance = ref<any>(null);
const mapContainerRef = ref<HTMLElement | null>(null);

// 用户位置相关状态
const userLongitude = ref(116.404);
const userLatitude = ref(39.915);

// 确认框相关状态
const showConfirmDialog = ref(false);
const showRecordingModal = ref(false);
const clickedLongitude = ref(0);
const clickedLatitude = ref(0);

// 附近音频点相关状态
const nearbyAudios = ref<AudioResponseDto[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const currentZoom = ref(15);
const currentDistance = ref(20000); // 固定距离20000米

// 搜索相关状态
const searchQuery = ref("");
const searchResults = ref<any[]>([]);
const showSearchResults = ref(false);
let debounceTimer: any = null;

// mapvgl相关实例
const view = ref<any>(null);
const pointLayer = ref<any>(null);

// 创建地图实例
const CreateMap = (centerLng: number = 116.404, centerLat: number = 39.915) => {
  // 防止重复初始化地图
  if (mapInstance.value) {
    return;
  }

  if (!mapContainerRef.value || !(window as any).BMapGL) {
    return;
  }

  try {
    const BMapGL = (window as any).BMapGL;

    // 创建地图实例
    mapInstance.value = new BMapGL.Map(mapContainerRef.value, {
      enableRotate: false, // 禁用旋转
      enableTilt: false, // 禁用倾斜
      enableDoubleClickZoom: true,
      enableKeyboard: true,
    });

    // 设置中心点和缩放级别
    const point = new BMapGL.Point(centerLng, centerLat);
    mapInstance.value.centerAndZoom(point, 15);

    // 启用鼠标滚轮缩放
    mapInstance.value.enableScrollWheelZoom(true);

    // 添加缩放控件（右下角）
    const zoomCtrl = new BMapGL.ZoomControl();
    mapInstance.value.addControl(zoomCtrl);

    // 监听缩放事件
    mapInstance.value.addEventListener("zoomend", () => {
      const newZoom = mapInstance.value.getZoom();
      currentZoom.value = newZoom;
      // 当缩放级别变化时，重新获取附近的音频点
      fetchNearbyAudios();
    });

    // 监听拖动结束事件
    mapInstance.value.addEventListener("dragend", () => {
      // 当地图拖动结束时，重新获取附近的音频点
      fetchNearbyAudios();
    });
  } catch (error) {
    // 静默处理地图初始化错误
  }
};

// 初始化地图
const initMap = () => {
  // 尝试获取用户当前位置
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        // 获取成功，使用用户位置创建地图
        const userLng = position.coords.longitude;
        const userLat = position.coords.latitude;

        // 更新用户位置的响应式变量
        userLongitude.value = userLng;
        userLatitude.value = userLat;

        CreateMap(userLng, userLat);

        // 初始化mapvgl相关组件
        if (mapInstance.value && (window as any).mapvgl) {
          initMapVglComponents();
          // 地图初始化完成后，获取附近的音频点
          fetchNearbyAudios();
        }
      },
      (error) => {
        // 获取失败，使用默认位置
        CreateMap();

        // 初始化mapvgl相关组件
        if (mapInstance.value && (window as any).mapvgl) {
          initMapVglComponents();
          // 地图初始化完成后，获取附近的音频点
          fetchNearbyAudios();
        }
      }
    );
  } else {
    // 浏览器不支持地理位置API，使用默认位置
    CreateMap();

    // 初始化mapvgl相关组件
    if (mapInstance.value && (window as any).mapvgl) {
      initMapVglComponents();
      // 地图初始化完成后，获取附近的音频点
      fetchNearbyAudios();
    }
  }
};

// 初始化mapvgl相关组件
const initMapVglComponents = () => {
  // 创建 View
  view.value = new (window as any).mapvgl.View({
    map: mapInstance.value,
  });

  // 创建 PointLayer
  pointLayer.value = new (window as any).mapvgl.PointLayer({
    color: "rgba(50, 50, 200, 1)",
    size: 24,
    blend: "normal",
    enablePicked: true,
    selectedColor: "#ff0000",
    autoSelect: true,
    onClick: (e: any) => {
      // 添加空值检查，避免访问undefined的属性
      if (e && e.dataItem && e.dataItem.properties && e.dataItem.properties.id) {
        const audioId = e.dataItem.properties.id;
        Router.push(`audio/${audioId}`);
      }
    },
  });

  // 添加到视图
  view.value.addLayer(pointLayer.value);
};

// 获取附近的音频点
const fetchNearbyAudios = async () => {
  if (!mapInstance.value) return;

  try {
    loading.value = true;
    error.value = null;

    // 获取当前地图中心点的经纬度
    const center = mapInstance.value.getCenter();
    const longitude = center.lng;
    const latitude = center.lat;

    // 获取当前距离
    const distance = currentDistance.value;

    // 调用API获取附近的音频点
    const response = await fetch(
      `/api/audio/nearby?latitude=${latitude}&longitude=${longitude}&distance=${distance}`
    );

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();
    if (result.code === 200 && result.data) {
      nearbyAudios.value = result.data;
      // 在地图上显示获取到的音频点
      showNearbyAudiosOnMap(nearbyAudios.value);
    } else {
      throw new Error(result.message || "获取附近音频点失败");
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : "获取附近音频点失败";
    console.error("获取附近音频点失败:", err);
  } finally {
    loading.value = false;
  }
};

// 在地图上显示附近的音频点
const showNearbyAudiosOnMap = (audios: AudioResponseDto[]) => {
  if (!pointLayer.value) {
    console.warn("PointLayer 尚未初始化");
    return;
  }

  // 构建所有点数据
  const points = audios.map((audio) => ({
    geometry: {
      type: "Point",
      coordinates: [audio.longitude, audio.latitude],
    },
    properties: {
      id: audio.id,
      title: audio.title,
      userName: audio.userName,
      audioUrl: audio.audioUrl,
    },
  }));

  // 一次性设置所有点
  pointLayer.value.setData(points);
};

// 发布按钮点击事件，使用用户当前位置
const showPublishConfirm = () => {
  // 设置点击位置为用户当前位置
  clickedLongitude.value = userLongitude.value;
  clickedLatitude.value = userLatitude.value;

  // 显示确认框
  showConfirmDialog.value = true;
};

// 处理确认按钮点击
const handleConfirm = () => {
  // 关闭确认框，打开录音模态框
  showConfirmDialog.value = false;
  showRecordingModal.value = true;
};

// 处理取消按钮点击
const handleCancel = () => {
  showConfirmDialog.value = false;
};

// 处理录音模态框取消
const handleRecordingCancel = () => {
  showRecordingModal.value = false;
};

// 处理录音模态框确认
const handleRecordingConfirm = () => {
  // 上传成功后关闭组件
  showRecordingModal.value = false;
};

// 搜索输入处理
const handleSearchInput = () => {
  // 防抖处理，避免频繁调用API
  if (debounceTimer) {
    clearTimeout(debounceTimer);
  }

  debounceTimer = setTimeout(() => {
    if (searchQuery.value.trim().length > 0) {
      fetchSearchSuggestions(searchQuery.value);
      showSearchResults.value = true;
    } else {
      searchResults.value = [];
      showSearchResults.value = false;
    }
  }, 300);
};

// 获取百度地图地点输入提示
const fetchSearchSuggestions = async (query: string) => {
  if (!query || !(window as any).BMapGL) return;

  try {
    const BMapGL = (window as any).BMapGL;

    // 使用百度地图JavaScript API的LocalSearch服务
    const localSearch = new BMapGL.LocalSearch(mapInstance.value, {
      renderOptions: { map: null }, // 不渲染到地图上
      pageCapacity: 10, // 最多返回10条结果，允许用户滚动查看更多
      onSearchComplete: (results: any) => {
        try {
          if (localSearch.getStatus() === 0) {
            // 0表示搜索成功
            // 转换结果格式
            let poiList = [];

            // 只处理百度地图API实际使用的_pois属性
            if (
              results &&
              typeof results === "object" &&
              results._pois &&
              Array.isArray(results._pois)
            ) {
              poiList = results._pois;
            }

            // 处理POI列表
            if (Array.isArray(poiList)) {
              searchResults.value = poiList
                .map((poi: any) => ({
                  name: poi.title || poi.name || "",
                  address: poi.address || poi.street || "",
                  location: {
                    lng: poi.point?.lng || poi.location?.lng || 0,
                    lat: poi.point?.lat || poi.location?.lat || 0,
                  },
                }))
                .filter((item) => item.location.lng !== 0 && item.location.lat !== 0);
            } else {
              searchResults.value = [];
            }
          } else {
            searchResults.value = [];
          }
        } catch (err) {
          searchResults.value = [];
        }
      },
    });

    // 执行搜索
    localSearch.search(query);
  } catch (err) {
    searchResults.value = [];
  }
};

// 处理搜索结果点击
const handleSearchResultClick = (result: any) => {
  if (!mapInstance.value || !result.location) return;

  // 关闭搜索结果
  showSearchResults.value = false;

  // 移动地图到选中的位置
  const BMapGL = (window as any).BMapGL;
  const point = new BMapGL.Point(result.location.lng, result.location.lat);
  mapInstance.value.centerAndZoom(point, 15);

  // 更新用户位置
  userLongitude.value = result.location.lng;
  userLatitude.value = result.location.lat;
};

// 点击外部关闭搜索结果
const handleClickOutside = (event: MouseEvent) => {
  const searchContainer = document.querySelector(".search-container");
  if (searchContainer && !searchContainer.contains(event.target as Node)) {
    showSearchResults.value = false;
  }
};

// 组件挂载时初始化地图
onMounted(() => {
  // 使用nextTick确保DOM已渲染
  nextTick(() => {
    initMap();
  });

  // 添加点击外部关闭搜索结果的事件监听
  document.addEventListener("click", handleClickOutside);
});

// 组件卸载前销毁地图和事件监听
onBeforeUnmount(() => {
  if (mapInstance.value) {
    mapInstance.value.destroy();
    mapInstance.value = null;
  }

  // 移除点击外部关闭搜索结果的事件监听
  document.removeEventListener("click", handleClickOutside);
});
</script>

<template>
  <div class="home">
    <!-- 搜索框组件 -->
    <div class="search-container">
      <div class="search-input-wrapper">
        <input
          v-model="searchQuery"
          type="text"
          class="search-input"
          :placeholder="t('home.searchPlaceholder')"
          @input="handleSearchInput"
          @click="showSearchResults = true"
        >
        <div class="search-icon">
          🔍
        </div>
      </div>

      <!-- 搜索结果下拉列表 -->
      <div
        v-if="showSearchResults && searchResults.length > 0"
        class="search-results"
      >
        <div
          v-for="(result, index) in searchResults"
          :key="index"
          class="search-result-item"
          @click="handleSearchResultClick(result)"
        >
          <div class="result-name">
            {{ result.name }}
          </div>
          <div class="result-address">
            {{ result.address }}
          </div>
        </div>
      </div>
    </div>

    <div
      ref="mapContainerRef"
      class="map-container"
    />

    <!-- 发布按钮 -->
    <button
      class="publish-btn"
      @click="showPublishConfirm"
    >
      <span class="publish-btn-plus">+</span>
    </button>

    <!-- 确认框 -->
    <div
      v-if="showConfirmDialog"
      class="confirm-dialog-overlay"
    >
      <div class="confirm-dialog">
        <div class="confirm-content">
          <p>
            {{
              t("home.confirmAddSoundMessage", {
                longitude: clickedLongitude.toFixed(6),
                latitude: clickedLatitude.toFixed(6),
              })
            }}
          </p>
        </div>
        <div class="confirm-buttons">
          <button
            class="cancel-btn"
            @click="handleCancel"
          >
            {{ t("home.cancel") }}
          </button>
          <button
            class="confirm-btn"
            @click="handleConfirm"
          >
            {{ t("home.confirm") }}
          </button>
        </div>
      </div>
    </div>

    <!-- 录音模态框 -->
    <RecordingModal
      :show="showRecordingModal"
      :latitude="clickedLatitude"
      :longitude="clickedLongitude"
      @cancel="handleRecordingCancel"
      @confirm="handleRecordingConfirm"
    />
  </div>
</template>

<style scoped>
.home {
  height: calc(100vh - 60px); /* 减去顶部栏高度 */
  overflow: hidden;
  position: relative;
}

.map-container {
  width: 100%;
  height: 100%;
  position: relative;
  background-color: #f0f0f0; /* 地图加载前的背景色 */
}

/* 自定义地图控件样式 */
:deep(.BMapGL_scaleCtrl) {
  left: 10px !important;
  bottom: 20px !important;
}

:deep(.BMapGL_zoomCtrl) {
  right: 10px !important;
  bottom: 20px !important;
}

/* 确认框样式 */
.confirm-dialog-overlay {
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

.confirm-dialog {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  min-width: 300px;
  max-width: 80%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.confirm-content {
  margin-bottom: 20px;
}

.confirm-content p {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.confirm-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.cancel-btn,
.confirm-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.cancel-btn {
  background-color: white;
  color: #333;
}

.confirm-btn {
  background-color: #1890ff; /* 蓝色 */
  color: white;
  border-color: #1890ff;
}

.cancel-btn:hover {
  background-color: #f5f5f5;
}

.confirm-btn:hover {
  background-color: #40a9ff;
  border-color: #40a9ff;
}

/* 发布按钮样式 */
.publish-btn {
  position: absolute;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background-color: #1890ff;
  color: white;
  border: none;
  font-size: 40px;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  z-index: 100;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
  margin: 0;
}

.publish-btn:hover {
  background-color: #40a9ff;
}

/* 发布按钮加号样式 */
.publish-btn-plus {
  display: inline-block;
  line-height: 1;
  margin-top: -10px;
}

/* 搜索框样式 */
.search-container {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 90%;
  max-width: 500px;
  z-index: 1000;
}

.search-input-wrapper {
  position: relative;
  width: 100%;
}

.search-input {
  width: 100%;
  padding: 12px 40px 12px 16px;
  border: none;
  border-radius: 25px;
  font-size: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  outline: none;
  background-color: white;
}

.search-icon {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 18px;
  color: #666;
}

/* 搜索结果样式 */
.search-results {
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  background-color: white;
  border-radius: 8px;
  margin-top: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  max-height: 200px;
  overflow-y: auto;
}

.search-result-item {
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-item:hover {
  background-color: #f5f5f5;
}

.result-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.result-address {
  font-size: 14px;
  color: #666;
  line-height: 1.4;
}
</style>
