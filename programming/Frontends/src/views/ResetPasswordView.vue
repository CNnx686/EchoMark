<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
const router = useRouter();

const newPassword = ref('');
const confirmPassword = ref('');
const loading = ref(false);

// 密码是否一致
const passwordMismatch = computed(() => {
  return newPassword.value !== '' && confirmPassword.value !== '' && newPassword.value !== confirmPassword.value;
});

// 是否可以提交（非空 + 一致）
const canSubmit = computed(() => {
  return (
    confirmPassword.value.trim() === newPassword.value.trim() &&
    confirmPassword.value.trim()!='' && newPassword.value.trim()!=''
  );
});

// 提交重置
const handleSubmit = () => {
  if (!canSubmit.value) {
    alert(t('resetPassword.pleaseCheckForm'));
    return;
  }

  loading.value = true;

  alert(t('resetPassword.success'));

  router.push("/login");
};
</script>

<template>
  <div class="reset-password-page d-flex align-items-center min-vh-100 bg-light">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-md-6 col-lg-5">
          <div class="card shadow-sm border-0 rounded-3">
            <div class="card-body p-4 p-md-5">
              <h2 class="text-center mb-4 fw-bold">
                {{ t('resetPassword.title') }}
              </h2>

              <form @submit.prevent="handleSubmit">
                <!-- 新密码 -->
                <div class="mb-3">
                  <label
                    for="newPassword"
                    class="form-label"
                  >{{ t('resetPassword.newPassword') }}</label>
                  <input
                    id="newPassword"
                    v-model="newPassword"
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
                    v-model="confirmPassword"
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
                    class="btn btn-primary btn-lg btn-reset-confirm"
                    :disabled="loading || !canSubmit"
                  >
                    {{ loading ? t('resetPassword.resetting') : t('resetPassword.resetButton') }}
                  </button>
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
.btn-reset-confirm{
  width: 50%;
}
</style>
