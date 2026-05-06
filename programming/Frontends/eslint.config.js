import pluginVue from "eslint-plugin-vue";
import typescriptParser from "@typescript-eslint/parser";
import typescriptEslintPlugin from "@typescript-eslint/eslint-plugin";
import globals from "globals";

// ESLint v9 配置
export default [
  // Vue配置
  ...pluginVue.configs["flat/recommended"],

  // TypeScript配置
  {
    files: ["**/*.{ts,tsx,d.ts}"],
    languageOptions: {
      parser: typescriptParser,
      ecmaVersion: "latest",
      sourceType: "module",
    },
    plugins: {
      "@typescript-eslint": typescriptEslintPlugin,
    },
    rules: {
      ...typescriptEslintPlugin.configs.recommended.rules,
    },
  },

  // Vue + TypeScript 集成配置
  {
    files: ["**/*.vue"],
    languageOptions: {
      parser: pluginVue.parser,
      parserOptions: {
        ecmaVersion: "latest",
        sourceType: "module",
        parser: {
          ts: typescriptParser,
          js: "espree",
        },
        extraFileExtensions: [".vue"],
      },
      globals: {
        ...globals.browser,
        BMapGL: "readonly",
        VoidFunction: "readonly",
      },
    },
    plugins: {
      vue: pluginVue,
      "@typescript-eslint": typescriptEslintPlugin,
    },
    rules: {
      // 禁用组件名必须为多单词的规则，因为Header是常见的组件名
      "vue/multi-word-component-names": "off",
    },
  },

  // 全局配置
  {
    languageOptions: {
      globals: {
        ...globals.browser,
        BMapGL: "readonly",
        VoidFunction: "readonly",
      },
    },
  },

  // 忽略配置
  {
    ignores: ["**/dist/**", "**/dist-ssr/**", "**/coverage/**", ".eslintcache"],
  },
];
