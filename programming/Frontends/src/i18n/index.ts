import { createI18n } from "vue-i18n";
import zhCN from "../locales/zh-CN.json";
import enUS from "../locales/en-US.json";

// 定义语言配置：单一事实来源
const languageConfig = {
  "zh-CN": { label: "中文", message: zhCN },
  "en-US": { label: "English", message: enUS },
};

// 导出供 UI 使用的选项列表
export const languageOptions = Object.entries(languageConfig).map(([code, config]) => ({
  code,
  label: config.label,
}));

// 生成 vue-i18n 需要的 messages 对象
const messages = Object.fromEntries(
  Object.entries(languageConfig).map(([code, config]) => [code, config.message])
);

const i18n = createI18n({
  locale: "zh-CN", // 默认语言
  fallbackLocale: "en-US", // 回退语言
  legacy:false,
  messages,
});

export default i18n;
