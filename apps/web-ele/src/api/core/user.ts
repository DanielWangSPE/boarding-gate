import type { UserInfo } from '@vben/types';

import { preferences } from '@vben/preferences';

import { requestClient } from '#/api/request';

interface BackendMeResp {
  nickname?: string;
  sessionId?: string;
  status?: number;
  userId: number;
  username: string;
}

/**
 * 获取当前登录用户信息，并适配成前端 `UserInfo` 形态。
 */
export async function getUserInfoApi(): Promise<UserInfo> {
  const raw = await requestClient.get<BackendMeResp>('/user/me');
  return {
    avatar: '',
    desc: raw.username,
    homePath: preferences.app.defaultHomePath,
    realName: raw.nickname || raw.username,
    //  后端暂未下发角色信息；前端默认给一个兜底角色即可
    roles: ['user'],
    token: '',
    userId: String(raw.userId),
    username: raw.username,
  };
}
