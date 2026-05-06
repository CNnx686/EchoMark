<script setup lang="ts">
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import { computed, reactive, ref } from "vue";
import { UserRegisterRequestDTO} from "@/types/Register";
import { ApiResponse} from "@/types/Audio";

const { t } = useI18n();
const router = useRouter();

const goToLogin = () => {
  router.push("/login");
};

const formData = reactive({
  phone: '',
  email: '',
  username: '',
  password: '',
  confirmPassword: '',
  code: '' // 新增：验证码
});

const loading = ref(false);
const sendingCode = ref(false); // 是否正在发送验证码
const countdown = ref(0); // 倒计时（秒）

// 计算属性：密码不匹配
const passwordMismatch = computed(() => {
  return formData.password !== '' && formData.confirmPassword !== '' && formData.password !== formData.confirmPassword;
});

// 计算属性：是否可点击“获取验证码”
const canSendCode = computed(() => {
  return !sendingCode.value && countdown.value === 0 && formData.email.trim() !== '';
});

// 发送验证码
const sendVerificationCode = async () => {
  if(!canSendCode.value){
    return;
  }
  sendingCode.value = true;
  countdown.value = 60; // 60秒倒计时
  // 启动倒计时
  const timer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--;
    } else {
      clearInterval(timer);
      sendingCode.value = false;
    }
  }, 1000);
  try {
    // const CodeRequest = ref<RegisterCodeRequestDTO>();
    // CodeRequest.value = {email : formData.email};
    const CodeRequest = ref<string>();
    CodeRequest.value = formData.email;
    const url = `/api/auth/register/code`;
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        email: CodeRequest.value
      })
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const result: ApiResponse<VoidFunction> = await response.json();
    console.log(result);
    if (result.code === 200) {
      console.log("验证码发送成功");
      alert(t("register.codeSent"))
    }
    else {
      console.log("验证码发送失败");
      alert(t("register.sendCodeFailed"))
    }
  }
  catch (err) {
    console.log(err)
    alert(t("register.sendCodeFailed"))
  }
};

// 提交注册
const InfoSubmit = async () => {
  if (!formData.phone || !formData.email || !formData.password || !formData.confirmPassword || !formData.code) {
    alert(t('register.pleaseFillRequired'));
    return;
  }

  if (passwordMismatch.value) {
    alert(t('register.passwordNotMatchAlert'));
    return;
  }
  const SubmitInfo = ref<UserRegisterRequestDTO>({
    username: formData.username,
    password: formData.password,
    email: formData.email,
    phoneNumber: formData.phone,
    code: formData.code,
  });
  const url = "api/auth/register"
  loading.value = true;
  console.log('提交注册数据:', SubmitInfo.value);
  const response = await fetch(url,{
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(
      SubmitInfo.value
    )
  });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  const result: ApiResponse<VoidFunction> = await response.json();
  console.log(result);
  if (result.code === 409) {
    alert(t("register.fieldAlreadyExists"));
  }
  else if(result.code === 400){
    alert(t("register.codeExpired"));
  }
  else if(result.code === 500){
    alert("HTTP 500");
  }
  else{
    alert(t("register.success"));
    router.push("/login");
  }
  loading.value=false;
};
</script>

<template>
  <div class="register-page d-flex align-items-center min-vh-100 bg-light">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-md-6 col-lg-5">
          <div class="card shadow-sm border-0 rounded-3">
            <div class="card-body p-4 p-md-5">
              <h2 class="text-center mb-4 fw-bold">
                {{ t('register.title') }}
              </h2>

              <form @submit.prevent="InfoSubmit">
                <!-- 手机号 -->
                <div class="mb-3">
                  <label
                    for="phone"
                    class="form-label"
                  >{{ t('register.phone') }}</label>
                  <input
                    id="phone"
                    v-model="formData.phone"
                    type="tel"
                    class="form-control"
                    :placeholder="t('register.phonePlaceholder')"
                  >
                </div>

                <!-- 验证码 -->
                <div class="mb-3">
                  <label
                    for="code"
                    class="form-label"
                  >{{ t('register.verificationCode') }}</label>
                  <div class="d-flex gap-2">
                    <input
                      id="code"
                      v-model="formData.code"
                      type="text"
                      class="form-control"
                      :placeholder="t('register.codePlaceholder')"
                      maxlength="6"
                    >
                    <button
                      type="button"
                      class="btn btn-outline-secondary"
                      :disabled="!canSendCode"
                      @click="sendVerificationCode"
                    >
                      {{ countdown > 0 ? `${countdown}s` : t('register.sendCode') }}
                    </button>
                  </div>
                  <div
                    v-if="!formData.code && formData.phone && !/^\d{6}$/.test(formData.code)"
                    class="text-danger mt-1 small"
                  >
                    {{ t('register.codeRequired') }}
                  </div>
                </div>

                <!-- 邮箱 -->
                <div class="mb-3">
                  <label
                    for="email"
                    class="form-label"
                  >{{ t('register.email') }}</label>
                  <input
                    id="email"
                    v-model="formData.email"
                    type="email"
                    class="form-control"
                    :placeholder="t('register.emailPlaceholder')"
                  >
                </div>

                <!-- 用户名 -->
                <div class="mb-3">
                  <label
                    for="username"
                    class="form-label"
                  >{{ t('register.username') }}
                    <small class="text-muted">({{ t('register.optional') }})</small>
                  </label>
                  <input
                    id="username"
                    v-model="formData.username"
                    type="text"
                    class="form-control"
                    :placeholder="t('register.usernamePlaceholder')"
                  >
                </div>

                <!-- 密码 -->
                <div class="mb-3">
                  <label
                    for="password"
                    class="form-label"
                  >{{ t('register.password') }}</label>
                  <input
                    id="password"
                    v-model="formData.password"
                    type="password"
                    class="form-control"
                    :placeholder="t('register.passwordPlaceholder')"
                  >
                </div>

                <!-- 确认密码 -->
                <div class="mb-4">
                  <label
                    for="confirmPassword"
                    class="form-label"
                  >{{ t('register.confirmPassword') }}</label>
                  <input
                    id="confirmPassword"
                    v-model="formData.confirmPassword"
                    type="password"
                    class="form-control"
                    :placeholder="t('register.confirmPasswordPlaceholder')"
                  >
                  <div
                    v-if="passwordMismatch"
                    class="text-danger mt-1 small"
                  >
                    {{ t('register.passwordNotMatch') }}
                  </div>
                </div>

                <!-- 提交按钮 -->
                <div class="d-grid">
                  <button
                    type="submit"
                    class="btn btn-primary btn-lg"
                    :disabled="loading"
                  >
                    {{ loading ? t('register.registering') : t('register.registerButton') }}
                  </button>
                </div>

                <!-- 已有账号？登录 -->
                <div class="text-center mt-3">
                  <p class="mb-0">
                    {{ t('register.alreadyHaveAccount') }}
                    <a
                      class="text-decoration-none fw-medium"
                      @click="goToLogin"
                    >
                      {{ t('register.loginNow') }}
                    </a>
                  </p>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.d-grid {
  display: flex;
  justify-content: center;
  padding-top: 1rem;
}
.btn {
  padding: 0.5rem;
  width: 50%;
}

.gap-2 {
  flex-direction: row;
  gap:10%
}
.gap-2 > :first-child {
  flex: 0 0 60%; /* 不伸缩，基础宽度 60% */
}

.gap-2 > :last-child {
  flex: 0 0 30%; /* 不伸缩，基础宽度 30% */
}

</style>
