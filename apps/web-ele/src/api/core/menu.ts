import type { RouteRecordStringComponent } from '@vben/types';

/**
 * 获取用户所有菜单。
 *
 * 后端尚未提供菜单接口，前端 `preferences.app.accessMode` 默认为 `frontend`，
 * 此时菜单由前端静态路由生成，下方返回空数组即可避免阻塞；
 * 若将来切换到 `backend` 模式，请接入后端 `/menu/all` 接口。
 */
export async function getAllMenusApi(): Promise<RouteRecordStringComponent[]> {
  return [];
}
