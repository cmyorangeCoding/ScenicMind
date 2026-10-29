// utils/appState.js
// 全局状态管理（内存存储，退出APP后清除）

// 使用响应式对象（Vue 3 reactive）
import { reactive } from "vue";

const appState = reactive({
  // 当前导览路线（景点名称数组）
  targetRoute: null,

  // 路线哈希（用于判断路线是否变化）
  routeHash: null,
});

// 设置目标路线
export const setTargetRoute = (route) => {
  appState.targetRoute = route;
  appState.routeHash = route ? JSON.stringify(route) : null;
  console.log("[全局状态] 设置路线:", route);
};

// 获取目标路线
export const getTargetRoute = () => {
  return appState.targetRoute;
};

// 获取路线哈希
export const getRouteHash = () => {
  return appState.routeHash;
};

// 清除目标路线
export const clearTargetRoute = () => {
  appState.targetRoute = null;
  appState.routeHash = null;
  console.log("[全局状态] 清除路线");
};

// 检查路线是否变化
export const isRouteChanged = (newRoute) => {
  const newHash = newRoute ? JSON.stringify(newRoute) : null;
  return newHash !== appState.routeHash;
};

export default appState;