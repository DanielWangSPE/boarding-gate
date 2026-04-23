/**
 * 业务请求客户端。
 *
 * 关键约定（与后端 design-docs/coding/01-认证与会话管理/01-01-登录认证.spec.md §2 对齐）：
 *   - 所有响应均为 HTTP 200，通过外壳中的 String `code` 判定业务结果
 *   - 成功固定为 "200"；失败为 "A0210" / "A0232" 等业务码
 *   - Access Token 放在 Authorization: Bearer <token>
 *   - Refresh Token 放在 HttpOnly Cookie（/auth/refresh），前端不感知
 */
import type { RequestClientOptions } from '@vben/request';

import { useAppConfig } from '@vben/hooks';
import { preferences } from '@vben/preferences';
import { errorMessageResponseInterceptor, RequestClient } from '@vben/request';
import { useAccessStore } from '@vben/stores';

import { ElMessage } from 'element-plus';

import { useAuthStore } from '#/store';

import { refreshTokenApi } from './core';

const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);

/** 业务成功码。 */
const SUCCESS_CODE = '200';

/** 需要前端静默重登录的业务码集合（Access Token 本体或会话已失效）。 */
const ACCESS_INVALID_CODES = new Set(['A0231', 'A0232', 'A0240']);

/** Access Token 过期、需要尝试 refresh 续期的业务码。 */
const ACCESS_EXPIRED_CODE = 'A0232';

/** Refresh Token 本身也已失效、只能跳回登录页的业务码。 */
const REFRESH_INVALID_CODES = new Set(['A0230', 'A0231']);

function formatToken(token: null | string) {
  return token ? `Bearer ${token}` : null;
}

function createRequestClient(baseURL: string, options?: RequestClientOptions) {
  const client = new RequestClient({
    ...options,
    baseURL,
  });

  /** 触发重新登录（Refresh 也失效，或已明确未登录）。 */
  async function doReAuthenticate() {
    const accessStore = useAccessStore();
    const authStore = useAuthStore();
    accessStore.setAccessToken(null);
    if (
      preferences.app.loginExpiredMode === 'modal' &&
      accessStore.isAccessChecked
    ) {
      accessStore.setLoginExpired(true);
    } else {
      await authStore.logout();
    }
  }

  /** 走 /auth/refresh 换新 Access Token。 */
  async function doRefreshToken() {
    const accessStore = useAccessStore();
    const { accessToken } = await refreshTokenApi();
    accessStore.setAccessToken(accessToken);
    return accessToken;
  }

  //  请求头处理：Authorization + Accept-Language（项目仅中文，写死 zh-CN）
  client.addRequestInterceptor({
    fulfilled: async (config) => {
      const accessStore = useAccessStore();
      config.headers.Authorization = formatToken(accessStore.accessToken);
      config.headers['Accept-Language'] = 'zh-CN';
      //  refresh / logout 接口需要带上 Cookie（HttpOnly 的 refreshToken）
      if (
        config.url?.includes('/auth/refresh') ||
        config.url?.includes('/auth/logout')
      ) {
        config.withCredentials = true;
      }
      return config;
    },
  });

  //  业务响应处理：根据 String code 分支
  client.addResponseInterceptor({
    fulfilled: async (response) => {
      const { config, data: responseData, status } = response;
      //  axios config 被 vben 类型收窄，动态扩展字段在这里统一访问
      const mutableConfig = config as typeof config & {
        __isRetryRequest?: boolean;
      };

      if (config.responseReturn === 'raw' || status < 200 || status >= 400) {
        return response;
      }
      if (config.responseReturn === 'body') {
        return responseData;
      }

      const bizCode = responseData?.code;
      //  透传给 client.request 的 url，兜底空串（正常情况下不会为空）
      const retryUrl = config.url ?? '';

      //  正常成功：返回 data 字段
      if (bizCode === SUCCESS_CODE) {
        return responseData.data;
      }

      //  Access Token 过期：走刷新并重放
      if (bizCode === ACCESS_EXPIRED_CODE) {
        if (
          !preferences.app.enableRefreshToken ||
          mutableConfig.__isRetryRequest
        ) {
          await doReAuthenticate();
          throw Object.assign({}, response, { response });
        }
        if (client.isRefreshing) {
          return new Promise((resolve) => {
            client.refreshTokenQueue.push((newToken: string) => {
              config.headers.Authorization = formatToken(newToken);
              resolve(client.request(retryUrl, { ...config }));
            });
          });
        }
        client.isRefreshing = true;
        mutableConfig.__isRetryRequest = true;
        try {
          const newToken = await doRefreshToken();
          client.refreshTokenQueue.forEach((cb) => cb(newToken));
          client.refreshTokenQueue = [];
          return client.request(retryUrl, { ...config });
        } catch (refreshError) {
          client.refreshTokenQueue.forEach((cb) => cb(''));
          client.refreshTokenQueue = [];
          await doReAuthenticate();
          throw refreshError;
        } finally {
          client.isRefreshing = false;
        }
      }

      //  Refresh Token 失效 / 会话被吊销 / 未登录：强制回登录
      if (
        REFRESH_INVALID_CODES.has(bizCode) ||
        ACCESS_INVALID_CODES.has(bizCode)
      ) {
        await doReAuthenticate();
        throw Object.assign({}, response, {
          response,
          message: responseData?.message,
          bizCode,
        });
      }

      //  其余业务错误：交给下游 errorMessageResponseInterceptor 或调用方处理
      throw Object.assign({}, response, {
        response,
        message: responseData?.message,
        bizCode,
      });
    },
  });

  //  通用错误提示：真正的 HTTP 异常或未被上面吞掉的业务错误
  client.addResponseInterceptor(
    errorMessageResponseInterceptor((msg: string, error) => {
      const responseData = error?.response?.data ?? {};
      //  后端业务失败：优先显示 message 字段
      const errorMessage = responseData?.message ?? responseData?.error ?? '';
      ElMessage.error(errorMessage || msg);
    }),
  );

  return client;
}

export const requestClient = createRequestClient(apiURL, {
  responseReturn: 'data',
});

export const baseRequestClient = new RequestClient({ baseURL: apiURL });
