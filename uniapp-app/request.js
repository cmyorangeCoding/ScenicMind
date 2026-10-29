import { BASE_URL } from "./config.js";
import { getToken, isTokenValid, clearAuth } from "./auth.js";

// 防止重复跳转登录
let isRedirecting = false;

export default function request(options) {
  return new Promise((resolve, reject) => {
    const token = getToken();
    const skipAuthCheck = options.skipAuthCheck === true;

    // 1. 请求前检查：Token已过期（跳过鉴权检查的请求除外）
    if (!skipAuthCheck && token && !isTokenValid()) {
      handleTokenExpired();
      reject(new Error("Token expired"));
      return;
    }

    const header = {
      "Content-Type": "application/json",
      ...(options.header || options.headers || {}),
    };

    if (token) {
      header.Authorization = "Bearer " + token;
    }

    uni.request({
      url: BASE_URL + options.url,
      method: (options.method || "GET").toUpperCase(),
      data: options.data || {},
      header: header,
      timeout: 30000,
      success: (res) => {
        // 2. 响应后拦截：服务端返回401未授权（跳过鉴权检查的请求除外）
        if (!skipAuthCheck && res.statusCode === 401) {
          handleTokenExpired();
          reject(new Error("Unauthorized"));
          return;
        }

        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data);
        } else {
          reject(new Error(`Request failed with status ${res.statusCode}`));
        }
      },
      fail: reject,
    });
  });
}

// 统一处理Token过期
function handleTokenExpired() {
  if (isRedirecting) return;
  isRedirecting = true;

  clearAuth();

  uni.showToast({
    title: "登录已过期，请重新登录",
    icon: "none",
    duration: 2000,
  });

  setTimeout(() => {
    uni.reLaunch({
      url: "/pages/login/login",
      success: () => {
        isRedirecting = false;
      },
      fail: () => {
        isRedirecting = false;
      },
    });
  }, 1500);
}

// 兼容你现有写法：request.get / request.post
request.get = (url, data = {}, header = {}, skipAuthCheck = false) => {
  return request({ url, method: "GET", data, header, skipAuthCheck });
};

request.post = (url, data = {}, header = {}, skipAuthCheck = false) => {
  return request({ url, method: "POST", data, header, skipAuthCheck });
};
