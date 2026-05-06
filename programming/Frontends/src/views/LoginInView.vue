<script setup lang="ts">
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import { ref } from "vue";
import type { ApiResponse } from "@/types/Audio";
import type { LoginResponseDTO } from "@/types/Login";

const router = useRouter();
const loginInput = ref("");
const password = ref("");
const loading = ref(false);

const { t } = useI18n();

// 登录提交
const LoginSubmit = async () => {
  const identifier = loginInput.value.trim();
  const pwd = password.value.trim();

  if (!identifier || !pwd) {
    alert(t("login.pleaseFillAll"));
    return;
  }

  loading.value = true;

  try {
    const payload = {
      username: identifier, // 后端 DTO 字段是 username
      password: pwd,
    };

    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });
    const result: ApiResponse<LoginResponseDTO> = await response.json();

    if (result.code === 200 && result.data) {
      localStorage.setItem("token", result.data.token);

      // 获取用户信息以获取userId
      try {
        const userResponse = await fetch("/api/users/profile", {
          headers: {
            Authorization: `Bearer ${result.data.token}`
          }
        });
        if (userResponse.ok) {
          const userResult = await userResponse.json();
          if (userResult.code === 200 && userResult.data) {
            localStorage.setItem("userId", userResult.data.userId.toString());
            localStorage.setItem("userInfo", JSON.stringify(userResult.data));
          }
        }
      } catch (err) {
        console.error("Failed to fetch user info after login:", err);
      }

      alert(t("login.loginSuccess"));
      router.push("/"); // 跳转首页

      // 登录成功后触发一次推荐更新，不需等待结果
      fetch("/api/llm/recommendation", {
        headers: {
          Authorization: `Bearer ${result.data.token}`
        }
      }).catch(() => {});
    }
    else {
      alert(result.msg || t("login.loginFailed"));
    }
  } catch (err) {
    console.error("Login error:", err);
    alert(t("login.networkError"));
  } finally {
    loading.value = false;
  }
};

const ForgotPassword = () => {
  router.push("/forgetPassword");
};

const goToRegister = () => {
  router.push("/register");
};
</script>

<template>
  <div class="login-page d-flex align-items-center min-vh-100 bg-light">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-md-6 col-lg-5">
          <div class="card shadow-sm border-0 rounded-3">
            <div class="card-body p-4 p-md-5">
              <h2 class="text-center mb-4 fw-bold">
                {{ t("login.title") }}
              </h2>

              <form @submit.prevent="LoginSubmit">
                <div class="mb-3">
                  <label
                    for="loginInput"
                    class="form-label"
                  >{{ t("login.loginLabel") }}</label>
                  <input
                    id="loginInput"
                    v-model="loginInput"
                    type="text"
                    class="form-control"
                    :placeholder="t('login.loginPlaceholder')"
                  >
                </div>

                <div class="mb-3">
                  <div class="d-flex justify-content-between align-items-center">
                    <label
                      for="password"
                      class="form-label"
                    >{{ t('login.passwordLabel') }}</label>
                    <a
                      href="#"
                      class="text-decoration-none small"
                      @click.prevent="ForgotPassword"
                    >
                      {{ t('login.forgotPassword') }}
                    </a>
                  </div>
                  <input
                    id="password"
                    v-model="password"
                    type="password"
                    class="form-control"
                    :placeholder="t('login.passwordPlaceholder')"
                  >
                </div>

                <div class="d-grid mb-3 btns">
                  <button
                    type="submit"
                    class="btn btn-primary btn-lg"
                    :disabled="loading"
                  >
                    {{ loading ? t('login.loggingIn') : t('login.loginButton') }}
                  </button>
                  <button
                    class="btn btn-outline-primary btn-lg"
                    @click="goToRegister"
                  >
                    {{ t('login.registerButton') }}
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
.btns {
  padding-top: 1.5rem;
  display: flex;
  flex-direction: row;
  gap: 15%;
  width: 100%;
  justify-content: center;
  align-items: center;
}
.btn {
  width: 42.5%;
}
</style>
