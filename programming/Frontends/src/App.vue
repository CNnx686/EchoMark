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
</style>
