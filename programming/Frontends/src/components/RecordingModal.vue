<template>
  <div
    v-if="show"
    class="recording-modal-overlay"
  >
    <div class="recording-modal">
      <!-- 顶部标题输入框 -->
      <div class="input-group">
        <label>{{ t("recording.titleLabel") }}</label>
        <input
          v-model="Title"
          type="text"
          :placeholder="t('recording.titlePlaceholder')"
          class="title-input"
          maxlength="100"
        >
      </div>

      <!-- 中部描述输入框 -->
      <div class="input-group">
        <label>{{ t("recording.descriptionLabel") }}</label>
        <textarea
          v-model="Description"
          :placeholder="t('recording.descriptionPlaceholder')"
          class="description-input"
          rows="4"
        />
        <div class="char-count">
          {{ DescriptionChars }}/600
        </div>
      </div>

      <!-- 图片上传 -->
      <div class="input-group">
        <label>{{ t("recording.imageLabel") }}</label>
        <div class="image-upload-container">
          <input
            id="image-upload"
            type="file"
            accept="image/*"
            class="image-upload-input"
            @change="handleImageUpload"
          >
          <label
            for="image-upload"
            class="image-upload-label"
          >
            <span v-if="!ImageUrl">{{ t("recording.uploadImage") }}</span>
            <span v-else>{{ t("recording.changeImage") }}</span>
          </label>
          <div
            v-if="ImageUrl"
            class="uploaded-image-preview"
          >
            <img
              :src="ImageUrl"
              alt="Uploaded image"
            >
            <button
              class="remove-image-btn"
              @click="removeImage"
            >
              ×
            </button>
          </div>
        </div>
      </div>

      <!-- 音频标签 -->
      <div class="input-group">
        <label>{{ t("recording.tagsLabel") }}</label>
        <div class="tags-container">
          <div class="tag-input-wrapper">
            <input
              v-model="newTag"
              type="text"
              :placeholder="t('recording.tagPlaceholder')"
              class="tag-input"
              :disabled="tags.length >= MAX_TAGS"
              @keydown.enter="addTag"
            >
            <button
              class="add-tag-btn"
              :disabled="!newTag.trim() || tags.length >= MAX_TAGS"
              @click="addTag"
            >
              {{ t("recording.addTag") }}
            </button>
          </div>
          <div class="tags-list">
            <span
              v-for="(tag, index) in tags"
              :key="index"
              class="tag-item"
            >
              {{ tag }}
              <button
                class="remove-tag-btn"
                @click="removeTag(index)"
              >×</button>
            </span>
          </div>
          <div
            v-if="tags.length >= MAX_TAGS"
            class="tag-limit-message"
          >
            {{ t("recording.maxTags") }}
          </div>
        </div>
      </div>

      <!-- 公开/私密状态 -->
      <div class="input-group">
        <label>{{ t("recording.statusLabel") }}</label>
        <div class="status-toggle">
          <label class="status-option">
            <input
              v-model="status"
              type="radio"
              value="PUBLIC"
              name="recording-status"
            >
            <span>{{ t("recording.public") }}</span>
          </label>
          <label class="status-option">
            <input
              v-model="status"
              type="radio"
              value="PRIVATE"
              name="recording-status"
            >
            <span>{{ t("recording.private") }}</span>
          </label>
        </div>
      </div>

      <!-- 录音控件 -->
      <div class="recording-section">
        <div class="recording-controls-full-width">
          <button
            class="record-button"
            :class="{ recording: IsRecording }"
            @click="ToggleRecording"
          >
            <span class="record-icon">●</span>
            {{ IsRecording ? t("recording.recording") : t("recording.startRecording") }}
          </button>

          <!-- 音频条 -->
          <div
            v-if="AudioUrl"
            class="audio-bar"
          >
            <audio
              :src="AudioUrl"
              controls
            />
          </div>

          <!-- AI按钮 -->
          <button
            class="ai-button"
            :disabled="aiSuggestionLoading"
            @click="HandleAIButton"
          >
            <span
              v-if="aiSuggestionLoading"
              class="spinner-border spinner-border-sm"
              role="status"
              aria-hidden="true"
            />
            <span v-else>{{ t("recording.aiButton") }}</span>
          </button>
        </div>
        <div
          v-if="IsRecording"
          class="recording-timer"
        >
          {{ FormatTime(RecordingTime) }}
        </div>
      </div>

      <!-- AI建议区域 -->
      <div
        v-if="aiSuggestion || aiSuggestionLoading || aiSuggestionError"
        class="ai-suggestion-section"
      >
        <h4 class="ai-suggestion-title">
          {{ t("recording.aiSuggestionTitle") }}
        </h4>
        
        <!-- 加载状态 -->
        <div
          v-if="aiSuggestionLoading"
          class="ai-suggestion-loading"
        >
          <div
            class="spinner-border"
            role="status"
          />
        </div>
        
        <!-- 错误信息 -->
        <div
          v-if="aiSuggestionError"
          class="ai-suggestion-error"
        >
          <p>{{ aiSuggestionError }}</p>
        </div>
        
        <!-- AI建议内容 -->
        <div
          v-if="aiSuggestion"
          class="ai-suggestion-content"
        >
          {{ aiSuggestion }}
        </div>
      </div>

      <!-- 底部按钮 -->
      <div class="modal-buttons">
        <button
          class="cancel-btn"
          @click="HandleCancel"
        >
          {{ t("recording.cancel") }}
        </button>
        <button
          class="confirm-btn"
          :disabled="!Title.trim()"
          @click="HandleConfirm"
        >
          {{ t("recording.confirm") }}
        </button>
      </div>
    </div>

    <!-- 加载UI -->
    <div
      v-if="loading"
      class="loading-overlay"
    >
      <div class="loading-spinner" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onUnmounted, watch } from "vue";
import { useI18n } from "vue-i18n";
import Recorder from "recorder-js";
import i18n from "../i18n/index"; // 导入i18n实例

const { t } = useI18n();

// 获取认证token的函数
const getAuthToken = () => {
  // 首先尝试从localStorage获取token
  const token = localStorage.getItem("token");
  if (token) {
    return token;
  }
  return "";
};

// 上传声印
const uploadAudio = async (
  audioBlob: Blob,
  latitude: number,
  longitude: number,
  title: string,
  description: string,
  tags: string[],
  imageBlob: Blob | null,
  status: string
) => {
  // 创建FormData对象
  const formData = new FormData();

  // 添加音频文件，确保使用正确的音频MIME类型
  const audioFile = new File([audioBlob], "recording.wav", { type: "audio/wav" });
  formData.append("file", audioFile);

  // 添加位置信息到表单数据
  formData.append("latitude", latitude.toString());
  formData.append("longitude", longitude.toString());

    // 获取认证token
    const token = getAuthToken();

    // 上传音频文件
    const uploadResponse = await fetch("/api/audio/upload", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
      },
      body: formData,
    });

    if (!uploadResponse.ok) {
      throw new Error(`上传失败: ${uploadResponse.status}`);
    }

    const uploadResult = await uploadResponse.json();

    if (uploadResult.code !== 200) {
      throw new Error(uploadResult.msg || "上传音频失败");
    }

    // 获取上传的音频ID
    const audioId = uploadResult.data.id;

    // 构建发布请求数据
    const publishData = {
      title,
      description,
      tags,
      photoUrl: "", // 添加照片URL
      isPublic: status === "PUBLIC", // 添加公开/私密状态
    };

    // 上传图片
    if (imageBlob) {
      const imageFormData = new FormData();
      const imageFile = new File([imageBlob], "image.jpg", { type: "image/jpeg" });
      imageFormData.append("file", imageFile);
      
      const imageResponse = await fetch("/api/audio/photo/upload", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: imageFormData,
      });
      
      if (!imageResponse.ok) {
        throw new Error(`图片上传失败: ${imageResponse.status}`);
      }
      
      const imageResult = await imageResponse.json();
      
      if (imageResult.code !== 200) {
        throw new Error(imageResult.msg || "图片上传失败");
      }
      
      // 获取图片URL并添加到发布数据中
      publishData.photoUrl = imageResult.data;
    }

    // 发布音频
    const publishResponse = await fetch(`/api/audio/${audioId}/publish`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(publishData),
    });

    if (!publishResponse.ok) {
      throw new Error(`发布失败: ${publishResponse.status}`);
    }

    const publishResult = await publishResponse.json();

    if (publishResult.code !== 200) {
      throw new Error(publishResult.msg || "发布音频失败");
    }

    return publishResult.data;
};

// 定义组件的props
interface Props {
  show: boolean;
  latitude: number;
  longitude: number;
}

const props = defineProps<Props>();

// 定义组件的事件
const emit = defineEmits<{
  cancel: [];
  confirm: [];
}>();

// 组件状态
const Title = ref("");
const Description = ref("");
const IsRecording = ref(false);
const RecordingTime = ref(0);
let RecordingStartTime: number = 0;
let RecordingInterval: ReturnType<typeof setInterval> | null = null;

const AudioUrl = ref<string | null>(null);
let CurrentStream: MediaStream | null = null;
let AudioBlobRef: Blob | null = null;
const ImageUrl = ref<string | null>(null);
let ImageBlobRef: Blob | null = null;
const tags = ref<string[]>([]);
const newTag = ref("");
const MAX_TAGS = 5;
const status = ref<string>("PUBLIC");
const loading = ref<boolean>(false);

// AI建议相关状态
const aiSuggestion = ref("");
const aiSuggestionLoading = ref(false);
const aiSuggestionError = ref<string | null>(null);

// 描述输入框的字符数
const DescriptionChars = ref(0);

// 监听描述变化，更新字符数
watch(
  () => Description.value,
  (newVal) => {
    DescriptionChars.value = newVal.length;

    // 如果超过600字符，截断到合适的长度
    if (DescriptionChars.value > 600) {
      Description.value = newVal.substring(0, 600);
      DescriptionChars.value = 600;
    }
  }
);

// 监听show属性变化，当组件显示时重置AI建议
watch(
  () => props.show,
  (newShow) => {
    if (newShow) {
      // 重置AI建议相关状态
      aiSuggestion.value = "";
      aiSuggestionLoading.value = false;
      aiSuggestionError.value = null;
    }
  }
);

// 开始/停止录音
const ToggleRecording = async () => {
  if (IsRecording.value) {
    StopRecording();
  } else {
    await StartRecording();
  }
};

// 开始录音
const StartRecording = async () => {
  try {
    // 确保之前的录音已停止
    if (RecordingInterval) {
      clearInterval(RecordingInterval);
      RecordingInterval = null;
    }

    // 如果已有音频URL，释放它以避免内存泄漏
    if (AudioUrl.value) {
      URL.revokeObjectURL(AudioUrl.value);
      AudioUrl.value = null;
    }

    // 使用recorder-js开始录音
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    CurrentStream = stream; // 保存流的引用

    // 创建AudioContext并初始化recorder
    const audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
    const recorder = new Recorder(audioContext);

    // 初始化recorder
    await recorder.init(stream);

    // 开始录音
    recorder.start();

    // 保存recorder实例用于停止录音
    (window as any).currentRecorder = recorder;
    (window as any).audioContext = audioContext;

    IsRecording.value = true;
    RecordingTime.value = 0;

    // 开始计时器
    RecordingStartTime = Date.now();
    RecordingInterval = setInterval(() => {
      RecordingTime.value = Math.floor((Date.now() - RecordingStartTime) / 1000);

      // 限制录音时间不超过60秒
      if (RecordingTime.value >= 60) {
        // 60秒
        StopRecording();
      }
    }, 1000);
  } catch (error) {
    alert(t("recording.error"));
  }
};

// 停止录音
const StopRecording = async () => {
  if (IsRecording.value) {
    // 先停止计时器，确保RecordingTime.value是最终的录音时长
    if (RecordingInterval) {
      clearInterval(RecordingInterval);
      RecordingInterval = null;
    }

    try {
      // 获取recorder实例
      const recorder = (window as any).currentRecorder;
      const audioContext = (window as any).audioContext;

      if (recorder) {
        // 停止录音并获取音频数据
        const { blob } = await recorder.stop();

        // 检查录音时长是否符合要求（不低于3秒）
        if (RecordingTime.value < 3) {
          // 3秒
          alert(t("recording.minRecordingTime"));
          AudioUrl.value = null; // 清除无效音频
          AudioBlobRef = null; // 清除音频Blob引用
        } else {
          const wavBlob = blob; // recorder-js 默认生成WAV格式
          AudioUrl.value = URL.createObjectURL(wavBlob);
          AudioBlobRef = wavBlob; // 保存音频Blob引用用于上传
        }

        // 清理recorder实例
        (window as any).currentRecorder = null;

        // 关闭AudioContext
        if (audioContext) {
          audioContext.close();
        }
      }
    } catch (error) {
    }

    // 无论录音是否有效，都停止所有音轨
    if (CurrentStream) {
      CurrentStream.getTracks().forEach((track) => track.stop());
      CurrentStream = null;
    }

    IsRecording.value = false;
  }
};

// 格式化时间显示
const FormatTime = (seconds: number) => {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
};

// AI按钮处理
const HandleAIButton = async () => {
  // 清除之前的建议和错误
  aiSuggestion.value = "";
  aiSuggestionError.value = null;
  aiSuggestionLoading.value = true;

  // 获取认证token
  const token = getAuthToken();
  if (!token) {
    alert(t("recording.loginRequired"));
    aiSuggestionLoading.value = false;
    return;
  }

  try {
    // 获取当前语言
    const currentLocale = i18n.global.locale.value;
    // 设置AI输出语言
    const aiOutputLanguage = currentLocale.startsWith('zh') ? '中文' : '英文';
    
    // 构建请求参数
    const requestBody = {
      prompt: `请根据用户提供的信息，给予具体的录音建议。\n\n当前标题："${Title.value || '空'}"\n当前描述："${Description.value || '空'}"\n\n要求：\n1. 分析用户的标题和描述内容，理解用户的场景\n2. 给出具体的录音建议，例如：如果用户描述了在图书馆学习，建议录制翻书声或敲键盘的声音\n3. 建议应该具体、实用，与用户的场景相关\n4. 请用${aiOutputLanguage}回答\n\n如果用户没有提供任何信息，请给出一些通用的录音建议，比如记录日常生活中的声音、自然环境的声音等。注意，输出请尽可能简短，不要包含MarkDown代码块标记。`,
      enableThinking: false
    };

    // 发送POST请求
    const response = await fetch("/api/llm/generateStream", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`
      },
      body: JSON.stringify(requestBody)
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    // 处理SSE响应
    const reader = response.body?.getReader();
    if (!reader) {
      throw new Error("无法获取响应流");
    }

    const decoder = new TextDecoder("utf-8");
    let done = false;
    let buffer = "";

    // 在读取数据前先创建一个空的建议，确保UI能够显示
    aiSuggestion.value = "";

    while (!done) {
      const { value, done: doneReading } = await reader.read();
      done = doneReading;

      if (value) {
        // 解码数据，让解码器自动处理多字节字符
        const chunk = decoder.decode(value, { stream: !doneReading });
        buffer += chunk;
        
        // 按完整行处理数据
        while (true) {
          const newlineIndex = buffer.indexOf("\n");
          if (newlineIndex === -1) {
            break; // 没有完整的行，等待下一次数据
          }
          
          const line = buffer.substring(0, newlineIndex);
          buffer = buffer.substring(newlineIndex + 1);
          
          if (line.startsWith("data:")) {
            const data = line.substring(5).trim(); // 去掉 "data: " 前缀
            if (data) {
              // 过滤掉结束标记
              if (data.trim() === "[DONE]") {
                continue;
              }
              
              try {
                // 解析JSON格式的消息
                const jsonData = JSON.parse(data);
                // 只显示content字段的内容
                if (jsonData.content) {
                  aiSuggestion.value += jsonData.content;
                }
              } catch (e) {
                // 检查是否包含content字段
                if (data.includes('"content":"')) {
                  // 提取content内容，使用更精确的正则表达式
                  const contentMatch = data.match(/"content":"(.*?)("(?:,|}|$)|$)/s);
                  if (contentMatch && contentMatch[1]) {
                    aiSuggestion.value += contentMatch[1];
                  }
                }
                // 其他非JSON格式的数据跳过，不直接追加
              }
              // 使用Vue.nextTick确保UI能够及时更新
              await new Promise(resolve => setTimeout(resolve, 0));
            }
          }
        }
      }
    }
    
    // 处理剩余的缓冲数据
    if (buffer) {
      const dataLines = buffer.split("\n").filter(line => line.startsWith("data:"));
      for (const line of dataLines) {
        const data = line.substring(5).trim();
        if (data && data.trim() !== "[DONE]") {
          try {
            const jsonData = JSON.parse(data);
            if (jsonData.content) {
              aiSuggestion.value += jsonData.content;
            }
          } catch (e) {
            if (data.includes('"content":"')) {
              const contentMatch = data.match(/"content":"(.*?)("(?:,|}|$)|$)/s);
              if (contentMatch && contentMatch[1]) {
                aiSuggestion.value += contentMatch[1];
              }
            }
          }
        }
      }
    }
  } catch (error) {
    aiSuggestionError.value = t("recording.aiSuggestionError");
  } finally {
    aiSuggestionLoading.value = false;
  }
};

// 图片上传处理
const handleImageUpload = (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (input.files && input.files[0]) {
    const file = input.files[0];
    // 创建图片URL用于预览
    if (ImageUrl.value) {
      URL.revokeObjectURL(ImageUrl.value);
    }
    ImageUrl.value = URL.createObjectURL(file);
    ImageBlobRef = file;
  }
};

// 移除图片
const removeImage = () => {
  if (ImageUrl.value) {
    URL.revokeObjectURL(ImageUrl.value);
    ImageUrl.value = null;
    ImageBlobRef = null;
    // 清空文件输入
    const input = document.getElementById("image-upload") as HTMLInputElement;
    if (input) {
      input.value = "";
    }
  }
};

// 添加标签
const addTag = () => {
  const tag = newTag.value.trim();
  if (tag && !tags.value.includes(tag) && tags.value.length < MAX_TAGS) {
    tags.value.push(tag);
    newTag.value = "";
  }
};

// 移除标签
const removeTag = (index: number) => {
  tags.value.splice(index, 1);
};

// 取消按钮处理
const HandleCancel = () => {
  // 如果仍在录音，先停止录音
  if (IsRecording.value) {
    StopRecording();
  }

  // 重置所有状态
  ResetState();
  emit("cancel");
};

// 确认按钮处理
const HandleConfirm = async () => {
  if (!Title.value.trim()) {
    alert(t("recording.titleRequired"));
    return;
  }

  if (!AudioBlobRef) {
    alert(t("recording.noAudioError"));
    return;
  }

  // 如果仍在录音，先停止录音
  if (IsRecording.value) {
    StopRecording();
  }

  try {
    loading.value = true;
    // 上传音频
    const result = await uploadAudio(
      AudioBlobRef,
      props.latitude,
      props.longitude,
      Title.value,
      Description.value,
      tags.value,
      ImageBlobRef,
      status.value
    );

    alert(t("recording.uploadSuccess"));

    // 重置状态并发送确认事件
    ResetState();
    emit("confirm");
  } catch (error) {
    alert(t("recording.uploadFailed"));
  } finally {
    loading.value = false;
  }
};

// 重置组件状态
const ResetState = () => {
  // 如果仍在录音，先停止录音
  if (IsRecording.value) {
    StopRecording();
  }

  // 停止任何活动的音轨
  if (CurrentStream) {
    CurrentStream.getTracks().forEach((track) => track.stop());
    CurrentStream = null;
  }

  Title.value = "";
  Description.value = "";
  RecordingTime.value = 0;

  if (RecordingInterval) {
    clearInterval(RecordingInterval);
    RecordingInterval = null;
  }

  if (AudioUrl.value) {
    URL.revokeObjectURL(AudioUrl.value);
    AudioUrl.value = null;
  }

  if (ImageUrl.value) {
    URL.revokeObjectURL(ImageUrl.value);
    ImageUrl.value = null;
  }

  ImageBlobRef = null;
  tags.value = [];
  newTag.value = "";
  status.value = "PUBLIC";
};

// 组件卸载时清理资源
onUnmounted(() => {
  // 如果仍在录音，先停止录音
  if (IsRecording.value) {
    StopRecording();
  }

  // 停止任何活动的音轨
  if (CurrentStream) {
    CurrentStream.getTracks().forEach((track) => track.stop());
    CurrentStream = null;
  }

  if (RecordingInterval) {
    clearInterval(RecordingInterval);
  }

  if (AudioUrl.value) {
    URL.revokeObjectURL(AudioUrl.value);
  }
});
</script>

<style scoped>
.recording-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1001;
}

.recording-modal {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  min-width: 400px;
  max-width: 500px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.input-group {
  margin-bottom: 15px;
}

.input-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
  color: #333;
  font-size: 14px;
  line-height: 1.5;
}

.title-input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.description-input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  resize: vertical;
  min-height: 100px;
  box-sizing: border-box;
}

.char-count {
  text-align: right;
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

.image-upload-container {
  position: relative;
}

.image-upload-input {
  display: none;
}

.image-upload-label {
  display: inline-block;
  padding: 10px 15px;
  background-color: #f0f0f0;
  color: #333;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.image-upload-label:hover {
  background-color: #e0e0e0;
}

.uploaded-image-preview {
  margin-top: 10px;
  position: relative;
  max-width: 200px;
  max-height: 200px;
}

.uploaded-image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #ddd;
}

.remove-image-btn {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 24px;
  height: 24px;
  background-color: #ff4d4f;
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.3s;
}

.remove-image-btn:hover {
  background-color: #ff7875;
}

.tags-container {
  margin-top: 5px;
}

.tag-input-wrapper {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.tag-input {
  flex: 1;
  padding: 8px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.add-tag-btn {
  padding: 8px 15px;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.add-tag-btn:hover:not(:disabled) {
  background-color: #40a9ff;
}

.add-tag-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  background-color: #e6f7ff;
  color: #1890ff;
  border-radius: 12px;
  font-size: 13px;
}

.remove-tag-btn {
  margin-left: 5px;
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  padding: 0;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-tag-btn:hover {
  color: #ff4d4f;
}

.tag-limit-message {
  font-size: 12px;
  color: #ff4d4f;
  margin-top: 5px;
}

/* 状态切换样式 */
.status-toggle {
  display: flex;
  gap: 20px;
  margin-left: 10px;
}

/* 按钮 */
.status-option {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-size: 14px;
}

.status-option input[type="radio"] {
  accent-color: #1890ff;
}

.recording-section {
  margin: 20px 0;
}

.recording-controls-full-width {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  width: 100%;
}

.record-button {
  flex-shrink: 0; /* 录音按钮 */
}

.audio-bar {
  flex: 1; /* 音频条占据剩余空间 */
  min-width: 100px; /* 确保最小宽度 */
}

.audio-bar audio {
  width: 100%;
  max-width: none;
}

.ai-button {
  flex-shrink: 0; /* AI按钮 */
}

.record-button {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 10px 15px;
  background-color: #ff6b6b;
  color: white;
  border: none;
  border-radius: 25px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.record-button:hover {
  background-color: #ff5252;
}

.record-button.recording {
  background-color: #ff3b30;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
  100% {
    opacity: 1;
  }
}

.record-icon {
  font-size: 18px;
  line-height: 1;
}

.audio-controls {
  display: flex;
  flex-direction: row;
  flex: 1;
  gap: 10px;
  align-items: center;
}

.audio-bar {
  min-width: 100px; /* 确保最小宽度 */
}

.audio-bar audio {
  width: 100%;
  max-width: none;
}

.ai-button {
  padding: 6px 12px;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.ai-button:hover {
  background-color: #5a6268;
}

.recording-timer {
  text-align: center;
  font-size: 14px;
  color: #ff6b6b;
  font-weight: bold;
}

.modal-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.cancel-btn {
  padding: 10px 20px;
  background-color: white;
  color: #333;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
}

.cancel-btn:hover {
  background-color: #f5f5f5;
}

.confirm-btn {
  padding: 10px 20px;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.confirm-btn:hover:not(:disabled) {
  background-color: #40a9ff;
}

.confirm-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

/* AI建议区域样式 */
.ai-suggestion-section {
  margin: 15px 0;
  padding: 15px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background-color: #fafafa;
}

.ai-suggestion-title {
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.ai-suggestion-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 15px 0;
}

.ai-suggestion-error {
  color: #ff4d4f;
  font-size: 14px;
}

.ai-suggestion-content {
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  max-height: 300px;
  overflow-y: auto;
  overflow-x: hidden;
  white-space: pre-wrap;
  word-break: break-word;
  text-align: left;
}

/* 加载UI样式 */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1002;
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
