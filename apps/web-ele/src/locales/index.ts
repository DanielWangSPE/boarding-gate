import type { Language } from 'element-plus/es/locale';

import type { App } from 'vue';

import type { LocaleSetupOptions, SupportedLanguagesType } from '@vben/locales';

import { ref } from 'vue';

import {
  $t,
  setupI18n as coreSetup,
  loadLocalesMapFromDir,
} from '@vben/locales';

import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import defaultLocale from 'element-plus/es/locale/lang/zh-cn';

/**
 * 本项目仅提供中文文案，保留 $t() 机制用于对接 Vben 框架内部调用，
 * 所有语言相关参数一律按 zh-CN 处理，忽略外部传入的 lang。
 */

const elementLocale = ref<Language>(defaultLocale);

const modules = import.meta.glob('./langs/**/*.json');

const localesMap = loadLocalesMapFromDir(
  /\.\/langs\/([^/]+)\/(.*)\.json$/,
  modules,
);

async function loadMessages(_lang: SupportedLanguagesType) {
  const [appLocaleMessages] = await Promise.all([
    localesMap['zh-CN']?.(),
    loadThirdPartyMessage(),
  ]);
  return appLocaleMessages?.default;
}

async function loadThirdPartyMessage() {
  dayjs.locale('zh-cn');
  elementLocale.value = defaultLocale;
}

async function setupI18n(app: App, options: LocaleSetupOptions = {}) {
  await coreSetup(app, {
    defaultLocale: 'zh-CN',
    loadMessages,
    missingWarn: !import.meta.env.PROD,
    ...options,
  });
}

export { $t, elementLocale, setupI18n };
