<script setup lang="ts">
import { RouterView } from "vue-router";
import Header from "./components/Header.vue";
</script>

<template>
  <Header />
  <RouterView />
</template>

<style>
/* 全局样式重置 */
* {
  box-sizing: border-box;
}

/* 确保页面没有不必要的滚动条 */
html,
body {
  margin: 0;
  padding: 0;
  overflow-x: hidden;
}

#app {
  min-height: 100vh;
  overflow-x: hidden;
}

/* 全局按钮美化 */
button,
.btn {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

button:not(:disabled):active,
.btn:not(:disabled):active {
  transform: scale(0.95);
}

button:not(:disabled):hover,
.btn:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

button:disabled,
.btn:disabled {
  opacity: 0.65;
  cursor: not-allowed !important;
}

/* 主要操作按钮增强 */
.btn-primary,
.confirm-btn {
  background: linear-gradient(135deg, #1890ff 0%, #1677cc 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
}

.btn-primary:hover:not(:disabled),
.confirm-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%);
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.4);
}

/* 危险操作按钮增强 */
.btn-danger {
  background: linear-gradient(135deg, #ff4d4f 0%, #d9363e 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(255, 77, 79, 0.3);
}

.btn-danger:hover:not(:disabled) {
  box-shadow: 0 4px 16px rgba(255, 77, 79, 0.4);
}

/* 卡片增强 */
.card {
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.2s ease;
}

.card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
}

/* 输入框聚焦增强 */
input:focus,
textarea:focus,
select:focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.12);
  outline: none;
}
</style>
