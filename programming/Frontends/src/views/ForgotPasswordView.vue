<script setup lang="ts">
import {ref, computed, reactive} from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import {ApiResponse} from "@/types/Audio";
import {PasswordResetRequestDTO} from "@/types/ForgotPassword";

const { t } = useI18n();
const router = useRouter();

const loading = ref(false);
const isCounting = ref(false);
const countdown = ref(60); // 倒计时60秒

const goToLogin = () => {
  router.push("/login");
}
const formData = reactive({
  Identifier: '',
  password: '',
  confirmPassword: '',
  code: '' // 新增：验证码
});
const sendingCode = ref(false); // 是否正在发送验证码
// 密码是否一致
const passwordMismatch = computed(() => {
  return formData.password !== '' && formData.confirmPassword !== '' && formData.password !== formData.confirmPassword;
});

// 是否可以提交（非空 + 一致）
const canSubmit = computed(() => {
  return (
    formData.confirmPassword.trim() === formData.password.trim() &&
    formData.confirmPassword.trim()!='' && formData.password.trim()!=''
  );
});

const canSendCode = computed(() => {
  return formData.Identifier.trim() !== '';
});

const sendCode = async () => {
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
    const CodeRequest = ref<string>();
    CodeRequest.value = formData.Identifier;
    const url = `api/auth/password/reset-request`;
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        CodeRequest
      })
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const result: ApiResponse<VoidFunction> = await response.json();
    console.log(result);
    if (result.code === 200) {
      console.log("验证码发送成功");
      alert(t("register.codeSent"))
    }
    else if(result.code === 400){
      console.log("用户未绑定邮箱/验证码过期");
      alert(t("register.sendCodeFailed"))
    }
    else if(result.code === 404){
      console.log("用户不存在");
      alert(t("register.sendCodeFailed"))
    }
  }
  catch (err) {
    console.log(err)
    alert(t("register.sendCodeFailed"))
  }
};

// 提交重置请求
const HandleSubmit = async () => {
  if (!formData.Identifier.length || !formData.password.length) {
    alert(t('forgotPassword.pleaseFillAll'));
    return;
  }
  if(formData.password != formData.confirmPassword){
    alert(t('resetPassword.passwordNotMatch'));
    return;
  }
  if (formData.code.length !== 6) {
    alert(t('forgotPassword.invalidCodeLength'));
    return;
  }

  loading.value = true;


  try {
    const ResetRequest = ref<PasswordResetRequestDTO>();
    ResetRequest.value = {
      identifier:formData.Identifier,
      code:formData.code,
      newPassword:formData.password
    }
    const url = "api/auth/password/reset";
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(
        ResetRequest.value
      )
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const result: ApiResponse<VoidFunction> = await response.json();
    console.log(result);
    if (result.code === 200) {
      console.log("重置成功");
    }
    else {
      console.log("重置失败");
    }
  }
  catch (err) {
    console.log(err)
  }
  loading.value = false;
};
</script>

<template>
  <div class="forgot-password-page d-flex align-items-center min-vh-100 bg-light">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-md-6 col-lg-5">
          <div class="card shadow-sm border-0 rounded-3">
            <div class="card-body p-4 p-md-5">
              <h2 class="text-center mb-4 fw-bold">
                {{ t('forgotPassword.title') }}
              </h2>
              <p class="text-center text-muted mb-4">
                {{ t('forgotPassword.description') }}
              </p>

              <form @submit.prevent="HandleSubmit">
                <!-- 手机号/邮箱 -->
                <div class="mb-3">
                  <label
                    for="contact"
                    class="form-label"
                  >{{ t('forgotPassword.contactLabel') }}</label>
                  <input
                    id="contact"
                    v-model="formData.Identifier"
                    type="text"
                    class="form-control"
                    :placeholder="t('forgotPassword.contactPlaceholder')"
                  >
                </div>

                <!-- 验证码输入 + 发送按钮 -->
                <div class="mb-4">
                  <div class="d-flex justify-content-between align-items-center mb-2">
                    <label
                      for="code"
                      class="form-label"
                    >{{ t('forgotPassword.verificationCode') }}</label>
                    <button
                      type="button"
                      class="btn btn-sm btn-outline-primary"
                      :disabled="isCounting || !canSendCode"
                      @click="sendCode"
                    >
                      {{ isCounting ? `${countdown}s` : t('forgotPassword.sendCode') }}
                    </button>
                  </div>
                  <input
                    id="code"
                    v-model="formData.code"
                    type="text"
                    class="form-control"
                    :placeholder="t('forgotPassword.codePlaceholder')"
                    maxlength="6"
                  >
                </div>
                <!-- 新密码 -->
                <div class="mb-3">
                  <label
                    for="newPassword"
                    class="form-label"
                  >{{ t('resetPassword.newPassword') }}</label>
                  <input
                    id="newPassword"
                    v-model="formData.password"
                    type="password"
                    class="form-control"
                    :placeholder="t('resetPassword.newPasswordPlaceholder')"
                  >
                </div>

                <!-- 确认密码 -->
                <div class="mb-4">
                  <label
                    for="confirmPassword"
                    class="form-label"
                  >{{ t('resetPassword.confirmPassword') }}</label>
                  <input
                    id="confirmPassword"
                    v-model="formData.confirmPassword"
                    type="password"
                    class="form-control"
                    :placeholder="t('resetPassword.confirmPasswordPlaceholder')"
                  >
                  <div
                    v-if="passwordMismatch"
                    class="text-danger mt-1 small"
                  >
                    {{ t('resetPassword.passwordNotMatch') }}
                  </div>
                </div>

                <!-- 确认重置按钮 -->
                <div class="d-grid mb-3">
                  <button
                    type="submit"
                    class="btn btn-primary btn-lg btn-reset"
                    :disabled="loading"
                  >
                    {{ loading ? t('forgotPassword.resetting') : t('forgotPassword.resetButton') }}
                  </button>
                </div>

                <!-- 返回登录 -->
                <div class="text-center">
                  <a
                    class="text-decoration-none small"
                    @click="goToLogin"
                  >
                    ← {{ t('forgotPassword.backToLogin') }}
                  </a>
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
.d-grid{
  padding-top: 1rem;
  display: flex;
  justify-content: center;
  align-items: center;
}
.btn-reset{
  width: 50%;
}
</style>
