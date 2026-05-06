<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import type {
  ApiResponse,
  AudioResponseDto,
  AudioResponseDetail,
  CommentWithRepliesResponse,
  ReplyResponse,
  CommentResponseData,
  ReplyResponseData,
} from "@/types/Audio";

const GetAudioUrlPrefix = "http://101.37.31.227:5000";
const Router = useRoute();
const AudioId = Number(Router.params.audioId);
const { t } = useI18n();

const AudioInfo = ref<AudioResponseDto | null>(null);
const AudioDetail = ref<AudioResponseDetail | null>(null);
const Favourite = ref<number[]>();
const NowAudioIsFavourite = ref<boolean>(false);

const loading = ref(true);
const error = ref<string | null>(null);
const newComment = ref("");
const submitting = ref(false);
const isPlaying = ref(false);
const audioElement = ref<HTMLAudioElement | null>(null);
const audioDuration = ref<number | null>(null);

// 修改功能相关状态
const isEditing = ref(false);
const newAudioTitle = ref("");
const newAudioDescription = ref("");

// 回复相关状态
const replyingTo = ref<number | null>(null); // 正在回复的顶级评论 ID
const replyingToReplyId = ref<number | null>(null); // 当前正在回复的 reply 的唯一 ID
const replyContent = ref("");
const submittingReply = ref(false);
const currentReplyTargetUser = ref<string>(""); // 用于 @前缀

// 音频事件
const onMetadataLoaded = () => {
  if (audioElement.value) {
    audioDuration.value = audioElement.value.duration;
  }
};

const onAudioEnded = () => {
  isPlaying.value = false;
};

const getAuthToken = () => localStorage.getItem("token");

// 获取音频基本信息
const fetchAudioInfo = async (id: number) => {
  try {
    const token = getAuthToken();
    const url = `/api/audio/${id}`;
    const response = await fetch(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const result: ApiResponse<AudioResponseDto> = await response.json();
    if (result.code === 200) {
      AudioInfo.value = result.data;
    } else {
      throw new Error(result.msg || "获取音频信息失败");
    }
  } catch (err) {
    console.error("获取音频信息失败:", err);
    throw err;
  }
};

// 获取互动和评论数据
const fetchAudioDetail = async (id: number) => {
  try {
    const token = getAuthToken();
    const url = `/api/social/audio/${id}/detail`;
    const response = await fetch(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const result: ApiResponse<AudioResponseDetail> = await response.json();
    if (result.code === 200) {
      AudioDetail.value = result.data;
      // 确保每个评论的replies字段都被初始化为数组
      if (AudioDetail.value.comments) {
        AudioDetail.value.comments.forEach(comment => {
          if (!comment.replies) {
            comment.replies = [];
          }
        });
      }
    } else {
      throw new Error(result.msg || "获取互动数据失败");
    }
  } catch (err) {
    console.error("获取互动数据失败:", err);
    throw err;
  }
};

// 获取收藏状态
const fetchFavourite = async (id: number) => {
  try {
    const token = getAuthToken();
    const url = `/api/users/favorites`;
    const response = await fetch(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const result: ApiResponse<[]> = await response.json();
    if (result.code === 200) {
      Favourite.value = result.data;
      NowAudioIsFavourite.value = Favourite.value.includes(id);
    } else {
      throw new Error(result.msg || "获取收藏失败");
    }
  } catch (err) {
    console.error("获取收藏失败:", err);
    throw err;
  }
};

// 初始化
onMounted(async () => {
  if (isNaN(AudioId)) {
    error.value = t("audioDetail.invalidAudioId");
    loading.value = false;
    return;
  }

  try {
    await Promise.all([fetchAudioInfo(AudioId), fetchAudioDetail(AudioId)]);
    const token = getAuthToken();
    if (token) {
      await fetchFavourite(AudioId);
    }
  } catch (err) {
    error.value = t("audioDetail.loadFailed");
  } finally {
    loading.value = false;
  }
});

// 点赞
const toggleLike = async () => {
  if (!AudioDetail.value) return;
  loading.value = true;
  try {
    const res = await fetch(`/api/social/audio/${AudioId}/like`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
    });

    if (res.ok) {
      const result = await res.json();
      AudioDetail.value.userLiked = result.data.liked;
      AudioDetail.value.likes = result.data.likeCount;
    }
  } catch (err) {
    console.error("点赞失败:", err);
  } finally {
    loading.value = false;
  }
};

// 收藏
const toggleFavourite = async () => {
  try {
    const token = localStorage.getItem("token");
    if (!token) {
      alert("请先登录");
      return;
    }

    loading.value = true;
    let method = NowAudioIsFavourite.value ? "DELETE" : "POST";
    let url = `/api/users/favorites/${AudioId}`;

    const res = await fetch(url, {
      method: method,
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (res.ok) {
      NowAudioIsFavourite.value = !NowAudioIsFavourite.value;
    } else {
      const errorData = await res.json();
      alert(errorData.msg || "操作失败");
    }
  } catch (err) {
    console.error("收藏操作失败:", err);
    alert("操作失败，请稍后重试");
  } finally {
    loading.value = false;
  }
};

// 发送评论
const SubmitComment = async () => {
  if (!newComment.value.trim() || submitting.value) return;
  submitting.value = true;
  try {
    const token = getAuthToken();
    const formData = new URLSearchParams();
    formData.append("content", newComment.value.trim());
    const res = await fetch(`/api/social/audio/comment/${AudioId}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Authorization: `Bearer ${token}`,
      },
      body: formData.toString(),
    });
    if (res.ok) {
      const response: ApiResponse<CommentResponseData> = await res.json();
      const newCmtFromBackend = response.data;
      // 转换后端返回的数据结构为前端期望的格式
      const newCmt = {
        commentId: newCmtFromBackend.id, // 将id转换为commentId
        userId: newCmtFromBackend.userId,
        username: newCmtFromBackend.username,
        content: newCmtFromBackend.content,
        createTime: newCmtFromBackend.createTime,
      };
      if (AudioDetail.value) {
        AudioDetail.value.comments.unshift(newCmt);
      }
      newComment.value = "";
    } else {
      alert("评论失败");
    }
  } catch (err) {
    alert("评论失败，请稍后重试");
    console.error(err);
  } finally {
    submitting.value = false;
  }
};

// 提交回复
const SubmitReply = async (rootCommentId: number) => {
  if (!replyContent.value.trim() || submittingReply.value) return;

  // 统一设置提交状态
  submittingReply.value = true;
  try {
    const token = getAuthToken();
    const finalContent = `@${currentReplyTargetUser.value} ${replyContent.value.trim()}`;

    const formData = new URLSearchParams();
    formData.append("content", finalContent);

    const res = await fetch(`/api/social/audio/reply/${rootCommentId}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Authorization: `Bearer ${token}`,
      },
      body: formData.toString(),
    });

    if (res.ok) {
      const response: ApiResponse<ReplyResponseData> = await res.json();
      const newReplyFromBackend = response.data;
      // 转换后端返回的数据结构为前端期望的格式
      const newReply = {
        replyId: newReplyFromBackend.id, // 将id转换为replyId
        userId: getCurrentUserId() || 0, // 设置当前用户ID，用于删除按钮的实时更新
        username: newReplyFromBackend.username,
        content: newReplyFromBackend.content,
        createTime: newReplyFromBackend.createTime,
      };
      const comment = AudioDetail.value?.comments.find((c) => c.commentId === rootCommentId);
      if (comment) {
        // 确保replies数组已经初始化
        if (!comment.replies) {
          comment.replies = [];
        }
        comment.replies.push(newReply);
      }
      // 重置状态
      replyContent.value = "";
      replyingTo.value = null;
      replyingToReplyId.value = null;
    } else {
      const errMsg = await res.text();
      console.error("提交回复失败:", res.status, errMsg);
      alert("回复失败，请稍后重试");
    }
  } catch (err) {
    console.error("网络错误:", err);
    alert("网络错误，请稍后重试");
  } finally {
    submittingReply.value = false;
  }
};

// 切换评论点赞状态
const toggleCommentLike = async (comment: CommentWithRepliesResponse) => {
  try {
    loading.value = true;
    const token = getAuthToken();
    const res = await fetch(`/api/social/COMMENT/${comment.commentId}/like`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (res.ok) {
      // 更新本地状态
      comment.userLiked = !comment.userLiked;
      comment.likes = (comment.likes || 0) + (comment.userLiked ? 1 : -1);
    } else {
      console.error("评论点赞失败");
    }
  } catch (err) {
    console.error("网络错误:", err);
  } finally {
    loading.value = false;
  }
};

// 切换回复点赞状态
const toggleReplyLike = async (reply: ReplyResponse, comment: CommentWithRepliesResponse) => {
  try {
    loading.value = true;
    const token = getAuthToken();
    const res = await fetch(`/api/social/REPLY/${reply.replyId}/like`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (res.ok) {
      // 更新本地状态
      reply.userLiked = !reply.userLiked;
      reply.likes = (reply.likes || 0) + (reply.userLiked ? 1 : -1);
    } else {
      console.error("回复点赞失败");
    }
  } catch (err) {
    console.error("网络错误:", err);
  } finally {
    loading.value = false;
  }
};

// 格式化时间
const formatDate = (isoString: string): string => {
  return isoString.replace("T", " ").substring(0, 16);
};

// 获取当前用户ID
const getCurrentUserId = (): number | null => {
  const userIdStr = localStorage.getItem("userId");
  return userIdStr ? Number(userIdStr) : null;
};

// 检查当前用户是否是音频拥有者
const isAudioOwner = (): boolean => {
  if (!AudioInfo.value || !getCurrentUserId()) return false;
  return AudioInfo.value.userId === getCurrentUserId();
};

// 检查当前用户是否是评论或回复的所有者
const isCommentOrReplyOwner = (userId: number): boolean => {
  const currentUserId = getCurrentUserId();
  return currentUserId !== null && currentUserId === userId;
};

// 删除评论
const deleteComment = async (commentId: number) => {
  if (!confirm(t("audioDetail.deleteCommentConfirm"))) return;
  
  try {
    loading.value = true;
    const token = getAuthToken();
    const res = await fetch(`/api/social/COMMENT/${commentId}`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    
    if (res.ok) {
      // 更新本地状态，移除删除的评论
      if (AudioDetail.value) {
        AudioDetail.value.comments = AudioDetail.value.comments.filter(
          comment => comment.commentId !== commentId
        );
      }
      alert(t("audioDetail.deleteSuccess"));
    } else {
      alert(t("audioDetail.deleteFailed"));
    }
  } catch (err) {
    console.error("删除评论失败:", err);
    alert(t("audioDetail.deleteFailed"));
  } finally {
    loading.value = false;
  }
};

// 删除回复
const deleteReply = async (commentId: number, replyId: number) => {
  if (!confirm(t("audioDetail.deleteReplyConfirm"))) return;
  
  try {
    loading.value = true;
    const token = getAuthToken();
    const res = await fetch(`/api/social/REPLY/${replyId}`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    
    if (res.ok) {
      // 更新本地状态，移除删除的回复
      if (AudioDetail.value) {
        const comment = AudioDetail.value.comments.find(c => c.commentId === commentId);
        if (comment && comment.replies) {
          comment.replies = comment.replies.filter(reply => reply.replyId !== replyId);
        }
      }
      alert(t("audioDetail.deleteSuccess"));
    } else {
      alert(t("audioDetail.deleteFailed"));
    }
  } catch (err) {
    console.error("删除回复失败:", err);
    alert(t("audioDetail.deleteFailed"));
  } finally {
    loading.value = false;
  }
};

// 实现删除功能
const handleDelete = async () => {
  if (!confirm(t("audioDetail.deleteConfirm"))) return;

  try {
    loading.value = true;
    const token = getAuthToken();
    const response = await fetch(`/api/audio/${AudioId}`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (response.ok) {
      alert(t("audioDetail.deleteSuccess"));
      // 跳转到首页或其他页面
      window.location.href = "/";
    } else {
      alert(t("audioDetail.deleteFailed"));
    }
  } catch (error) {
    console.error("Delete error:", error);
    alert(t("audioDetail.deleteFailed"));
  } finally {
    loading.value = false;
  }
};

// 实现修改功能
const handleEdit = () => {
  // 设置编辑状态并初始化表单数据
  isEditing.value = true;
  newAudioTitle.value = AudioInfo.value?.title || "";
  newAudioDescription.value = AudioInfo.value?.description || "";
};

// 提交修改
const handleEditSubmit = async () => {
  if (!newAudioTitle.value.trim() || loading.value) return;

  try {
    loading.value = true;
    const token = getAuthToken();
    const response = await fetch(`/api/audio/${AudioId}/update`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        newTitle: newAudioTitle.value.trim(),
        newDescription: newAudioDescription.value.trim(),
      }),
    });

    if (response.ok) {
      const result = await response.json();
      if (result.code === 200 && result.data) {
        AudioInfo.value = result.data;
        alert(t("audioDetail.editSuccess"));
        isEditing.value = false;
      }
    } else {
      alert(t("audioDetail.editFailed"));
    }
  } catch (error) {
    console.error("Edit error:", error);
    alert(t("audioDetail.editFailed"));
  } finally {
    loading.value = false;
  }
};

// 取消编辑
const handleEditCancel = () => {
  isEditing.value = false;
  newAudioTitle.value = "";
  newAudioDescription.value = "";
};

// 实现隐藏功能
const handleHide = async () => {
  try {
    loading.value = true;
    const token = getAuthToken();
    const newHiddenStatus = AudioInfo.value?.status !== "HIDDEN";
    const response = await fetch(`/api/audio/${AudioId}/hide?hidden=${newHiddenStatus}`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (response.ok) {
      const result = await response.json();
      if (result.code === 200 && result.data) {
        AudioInfo.value = result.data;
        alert(newHiddenStatus ? t("audioDetail.hideSuccess") : t("audioDetail.showSuccess"));
      }
    } else {
      alert(t("audioDetail.hideFailed"));
    }
  } catch (error) {
    console.error("Hide error:", error);
    alert(t("audioDetail.hideFailed"));
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="container my-4">
    <!-- 数据加载遮罩 -->
    <div
      v-if="loading"
      class="loading-container"
    >
      <div class="loading-overlay">
        <div class="loading-spinner" />
      </div>
    </div>

    <div
      v-else-if="error"
      class="alert alert-danger"
      role="alert"
    >
      {{ error }}
    </div>

    <div
      v-else-if="AudioInfo && AudioDetail"
      class="row g-4"
    >
      <!-- 封面图（左侧） -->
      <div class="col-md-4 col-lg-3">
        <div class="bg-light rounded overflow-hidden shadow-sm">
          <img
            v-if="AudioInfo.photoUrl"
            :src="GetAudioUrlPrefix + AudioInfo.photoUrl"
            alt="音频封面"
            class="w-100"
            style="aspect-ratio: 1/1; object-fit: cover"
          >
          <div
            v-else
            class="d-flex align-items-center justify-content-center bg-secondary text-white"
            style="aspect-ratio: 1/1"
          >
            {{ t("audioDetail.noCover") }}
          </div>
        </div>
      </div>

      <!-- 右侧内容区 -->
      <div class="col-md-8 col-lg-9">
        <!-- 显示原始内容或编辑表单 -->
        <div v-if="!isEditing">
          <h1 class="mb-3">
            {{ AudioInfo.title }}
          </h1>
          <p class="text-muted mb-2">
            {{ AudioInfo.description }}
          </p>
          <p class="text-muted mb-3">
            by <strong>{{ AudioInfo.userName }}</strong>
          </p>
        </div>
        <!-- 编辑表单 -->
        <div
          v-else
          class="mb-3"
        >
          <textarea
            v-model="newAudioTitle"
            class="form-control form-control-lg mb-2"
            :placeholder="t('audioDetail.editTitlePlaceholder')"
            rows="1"
          />
          <textarea
            v-model="newAudioDescription"
            class="form-control mb-2"
            :placeholder="t('audioDetail.editDescriptionPlaceholder')"
            rows="2"
          />
          <div class="d-flex gap-2">
            <button
              class="btn btn-primary"
              :disabled="!newAudioTitle.trim() || loading"
              @click="handleEditSubmit"
            >
              {{ loading ? t("audioDetail.submitting") : t("audioDetail.send") }}
            </button>
            <button
              class="btn btn-secondary"
              :disabled="loading"
              @click="handleEditCancel"
            >
              {{ t("audioDetail.cancel") }}
            </button>
          </div>
        </div>

        <!-- 标签 -->
        <div class="mb-4 tags">
          <span
            v-for="tag in AudioInfo.tags"
            :key="tag"
            class="badge bg-secondary me-2 mb-2"
          >
            #{{ tag }}
          </span>
        </div>

        <!-- 音频播放器 -->
        <div class="card p-3 mb-4 shadow-sm">
          <audio
            ref="audioElement"
            :src="GetAudioUrlPrefix + AudioInfo.audioUrl"
            class="w-100 mb-3"
            controls
            @ended="onAudioEnded"
            @play="isPlaying = true"
            @pause="isPlaying = false"
            @loadedmetadata="onMetadataLoaded"
          />

          <div class="d-flex gap-3 mt-2 text-muted small">
            <span>👍 {{ AudioDetail.likes }}</span>
            <span>💬 {{ AudioDetail.comments.length }}</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="d-flex flex-wrap gap-2 mb-4">
          <button
            class="btn"
            :class="AudioDetail.userLiked ? 'btn-success' : 'btn-outline-primary'"
            @click="toggleLike"
          >
            👍 {{ AudioDetail.userLiked ? t("audioDetail.liked") : t("audioDetail.like") }}
          </button>
          <button
            class="btn btn-outline-secondary"
            @click="toggleFavourite"
          >
            ⭐ {{ NowAudioIsFavourite ? t("audioDetail.favorited") : t("audioDetail.favorite") }}
          </button>

          <!-- 管理按钮，仅当当前用户是音频拥有者时显示 -->
          <template v-if="isAudioOwner()">
            <button
              class="btn btn-outline-danger"
              :disabled="loading"
              @click="handleDelete"
            >
              🗑️ {{ loading ? t("audioDetail.deleting") : t("audioDetail.delete") }}
            </button>
            <button
              class="btn btn-outline-warning"
              :disabled="loading"
              @click="handleEdit"
            >
              ✏️ {{ loading ? t("audioDetail.editing") : t("audioDetail.edit") }}
            </button>
            <button
              class="btn btn-outline-secondary"
              :disabled="loading"
              @click="handleHide"
            >
              👁️
              {{
                loading
                  ? t("audioDetail.hiding")
                  : AudioInfo?.status === "HIDDEN"
                    ? t("audioDetail.show")
                    : t("audioDetail.hide")
              }}
            </button>
          </template>
        </div>

        <!-- 评论区 -->
        <div class="border-top pt-4">
          <h2 class="h4 mb-3">
            {{ t("audioDetail.comments") }} ({{ AudioDetail.comments.length }})
          </h2>

          <!-- 发表评论 -->
          <div class="mb-4">
            <textarea
              v-model="newComment"
              class="form-control mb-2"
              :placeholder="t('audioDetail.commentPlaceholder')"
              rows="3"
            />
            <button
              class="btn btn-primary"
              :disabled="!newComment.trim() || submitting"
              @click="SubmitComment"
            >
              {{ submitting ? t("audioDetail.submitting") : t("audioDetail.submitComment") }}
            </button>
          </div>

          <!-- 评论列表 -->
          <div class="list-group">
            <div
              v-for="comment in AudioDetail.comments"
              :key="comment.commentId"
              class="list-group-item border-0 px-0 py-3"
            >
              <div class="d-flex justify-content-between align-items-center">
                <strong>{{ comment.username }}</strong>
                <div class="d-flex align-items-center">
                  <small class="text-muted me-3">{{ formatDate(comment.createTime) }}</small>
                  <button
                    class="btn btn-sm p-0 text-decoration-none"
                    :class="comment.userLiked ? 'text-danger' : 'text-muted'"
                    :title="t('audioDetail.like')"
                    @click="toggleCommentLike(comment)"
                  >
                    <i class="bi bi-heart" />
                    <span class="ms-1">{{ t("audioDetail.like") }}</span>
                  </button>
                  <span class="ms-1 text-muted small">{{ comment.likes || 0 }}</span>
                  <!-- 删除按钮 -->
                  <button
                    v-if="isCommentOrReplyOwner(comment.userId)"
                    class="btn btn-sm p-0 text-decoration-none text-danger ms-3"
                    :title="t('audioDetail.delete')"
                    :disabled="loading"
                    @click="deleteComment(comment.commentId)"
                  >
                    <i class="bi bi-trash" />
                    <span class="ms-1">{{ t("audioDetail.delete") }}</span>
                  </button>
                </div>
              </div>
              <p class="mt-1 mb-2">
                {{ comment.content }}
              </p>

              <!-- 回复按钮（回复楼主） -->
              <button
                class="btn btn-sm btn-link p-0 text-decoration-none"
                @click="
                  {
                    replyingTo = comment.commentId;
                    replyingToReplyId = null;
                    currentReplyTargetUser = comment.username;
                    replyContent = '';
                  }
                "
              >
                {{ t("audioDetail.reply") }}
              </button>

              <!-- 回复此顶级评论的输入框 -->
              <div
                v-if="replyingTo === comment.commentId && !replyingToReplyId"
                class="mt-2 ms-3"
              >
                <textarea
                  v-model="replyContent"
                  class="form-control form-control-sm mb-1"
                  :placeholder="t('audioDetail.replyPlaceholder')"
                  rows="2"
                />
                <div>
                  <button
                    class="btn btn-sm btn-primary me-2"
                    :disabled="!replyContent.trim() || submittingReply"
                    @click="SubmitReply(comment.commentId)"
                  >
                    {{ submittingReply ? t("audioDetail.submitting") : t("audioDetail.send") }}
                  </button>
                  <button
                    class="btn btn-sm btn-secondary"
                    @click="
                      replyingTo = null;
                      replyingToReplyId = null;
                    "
                  >
                    {{ t("audioDetail.cancel") }}
                  </button>
                </div>
              </div>

              <!-- 子回复列表 -->
              <div
                v-if="comment.replies?.length"
                class="mt-2"
              >
                <div
                  v-for="reply in comment.replies"
                  :key="reply.replyId"
                  class="ms-4 ps-3 border-start border-secondary-subtle"
                >
                  <div class="d-flex justify-content-between align-items-start">
                    <strong class="small">{{ reply.username }}</strong>
                    <div class="d-flex align-items-center">
                      <small class="text-muted me-3">{{ formatDate(reply.createTime) }}</small>
                      <button
                        class="btn btn-sm p-0 text-decoration-none"
                        :class="reply.userLiked ? 'text-danger' : 'text-muted'"
                        :title="t('audioDetail.like')"
                        @click="toggleReplyLike(reply, comment)"
                      >
                        <i class="bi bi-heart" />
                        <span class="ms-1">{{ t("audioDetail.like") }}</span>
                      </button>
                      <span class="ms-1 text-muted small">{{ reply.likes || 0 }}</span>
                      <!-- 删除按钮 -->
                      <button
                        v-if="isCommentOrReplyOwner(reply.userId || 0)"
                        class="btn btn-sm p-0 text-decoration-none text-danger ms-3"
                        :title="t('audioDetail.delete')"
                        :disabled="loading"
                        @click="deleteReply(comment.commentId, reply.replyId)"
                      >
                        <i class="bi bi-trash" />
                        <span class="ms-1">{{ t("audioDetail.delete") }}</span>
                      </button>
                    </div>
                  </div>
                  <p
                    class="mt-1 mb-0 small text-muted"
                    style="line-height: 1.4"
                  >
                    {{ reply.content }}
                  </p>

                  <button
                    class="btn btn-sm btn-link p-0 text-decoration-none mt-1"
                    @click="
                      {
                        replyingTo = comment.commentId;
                        replyingToReplyId = reply.replyId;
                        currentReplyTargetUser = reply.username;
                        replyContent = '';
                      }
                    "
                  >
                    {{ t("audioDetail.reply") }}
                  </button>

                  <!-- 回复这条子回复的输入框 -->
                  <div
                    v-if="replyingToReplyId === reply.replyId"
                    class="mt-2 ms-3"
                  >
                    <textarea
                      v-model="replyContent"
                      class="form-control form-control-sm mb-1"
                      :placeholder="t('audioDetail.replyPlaceholder')"
                      rows="2"
                    />
                    <div>
                      <button
                        class="btn btn-sm btn-primary me-2"
                        :disabled="!replyContent.trim() || submittingReply"
                        @click="SubmitReply(comment.commentId)"
                      >
                        {{ submittingReply ? t("audioDetail.submitting") : t("audioDetail.send") }}
                      </button>
                      <button
                        class="btn btn-sm btn-secondary"
                        @click="
                          replyingTo = null;
                          replyingToReplyId = null;
                        "
                      >
                        {{ t("audioDetail.cancel") }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.container {
  max-width: 960px;
}
.badge {
  gap: 5%;
}

/* 加载UI样式 */
.loading-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100vh;
  z-index: 9999;
}

.loading-overlay {
  position: absolute;
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

/* 点赞按钮样式 */
.btn-sm.p-0 {
  padding: 0.25rem 0.5rem !important;
}

.btn-sm.p-0:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.text-danger {
  transition: color 0.2s ease;
}

.text-muted {
  transition: color 0.2s ease;
}
</style>
