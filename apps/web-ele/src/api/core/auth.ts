import { requestClient } from '#/api/request';

import { encryptPasswordBySm4 } from './crypto';

export namespace AuthApi {
  /** 前端登录入参（明文）。内部会先走 /auth/crypto/login 拿 key/iv 再 SM4 加密。 */
  export interface LoginParams {
    password: string;
    username: string;
  }

  /** SM4 加密参数（后端 spec §2.1）。 */
  export interface CryptoParams {
    algorithm: string;
    cryptoId: string;
    iv: string;
    key: string;
    mode: string;
    ttlSeconds: number;
  }

  /** 登录成功返回（后端 spec §2.2）。 */
  export interface LoginResult {
    accessToken: string;
    expiresIn: number;
    forceChangePassword: boolean;
    tokenType: string;
  }

  /** /auth/refresh 返回。 */
  export interface RefreshResult {
    accessToken: string;
    expiresIn: number;
    tokenType: string;
  }
}

/**
 * 获取 SM4 加密参数（一次性，单次消费）。
 */
export async function fetchCryptoParamsApi() {
  return requestClient.get<AuthApi.CryptoParams>('/auth/crypto/login');
}

/**
 * 账号密码登录：
 *   1) 先拿 key/iv/cryptoId
 *   2) 前端 SM4-CBC/PKCS7 加密密码 → Hex
 *   3) 提交 /auth/crypto/login
 */
export async function loginApi(data: AuthApi.LoginParams) {
  const crypto = await fetchCryptoParamsApi();
  const cipherHex = encryptPasswordBySm4(data.password, crypto.key, crypto.iv);
  return requestClient.post<AuthApi.LoginResult>('/auth/crypto/login', {
    cryptoId: crypto.cryptoId,
    password: cipherHex,
    username: data.username,
  });
}

/**
 * 刷新 accessToken。Refresh Token 在 HttpOnly Cookie 中，
 * 由 request 拦截器统一附加 withCredentials。
 */
export async function refreshTokenApi() {
  return requestClient.post<AuthApi.RefreshResult>('/auth/refresh');
}

/** 退出登录。 */
export async function logoutApi() {
  return requestClient.post('/auth/logout');
}

/**
 * 获取用户权限码。
 *
 * 后端尚未提供该接口，前端 access mode 默认为 `frontend`，不会强依赖权限码内容。
 * 返回空数组保证 Promise.all 链路不断。
 */
export async function getAccessCodesApi(): Promise<string[]> {
  return [];
}
