import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import i18n from "./i18n";
import "./css/bootstrap.min.4.6.0.css";

createApp(App).use(router).use(i18n).mount("#app");
