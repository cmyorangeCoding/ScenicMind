const TOKEN_KEY = "token";
const TOKEN_EXPIRE_KEY = "tokenExpire";
const USER_ID_KEY = "userId";
const SESSION_ID_KEY = "sessionId";
const NICKNAME_KEY = "nickname";
const AVATAR_URL_KEY = "avatarUrl";

const genSessionId = () =>
  Math.random().toString(36).substring(2, 12) + Date.now();

const safeSetStorage = (key, value) => {
  if (value === undefined || value === null || value === "") return;
  uni.setStorageSync(key, value);
};

const decodeBase64 = (str) => {
  try {
    const normalized = str.replace(/-/g, "+").replace(/_/g, "/");
    const decoded = atob(normalized);
    return decodeURIComponent(
      decoded
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join(""),
    );
  } catch (error) {
    return null;
  }
};

const parseJwtPayload = (token) => {
  if (!token) return null;
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  const payload = decodeBase64(parts[1]);
  if (!payload) return null;
  try {
    return JSON.parse(payload);
  } catch (error) {
    return null;
  }
};

export const getToken = () => uni.getStorageSync(TOKEN_KEY) || "";

const getTokenExpire = () => {
  const raw = uni.getStorageSync(TOKEN_EXPIRE_KEY);
  if (!raw) return 0;
  const numberValue = Number(raw);
  if (!Number.isNaN(numberValue) && numberValue > 0) return numberValue;
  const dateValue = Date.parse(raw);
  return Number.isNaN(dateValue) ? 0 : dateValue;
};

// ✅ 修复：从 token 正确获取 userId，永不丢失精度
const getUserIdFromToken = (token) => {
  const payload = parseJwtPayload(token);
  return payload?.sub || payload?.userId || "";
};

// ✅ 修复：永远返回正确的 userId
export const getUserId = () => {
  const token = getToken();
  const idFromToken = getUserIdFromToken(token);
  if (idFromToken) return idFromToken;

  const stored = uni.getStorageSync(USER_ID_KEY);
  return stored || "";
};

export const getNickname = () => uni.getStorageSync(NICKNAME_KEY) || "";
export const getAvatarUrl = () => uni.getStorageSync(AVATAR_URL_KEY) || "";

// 获取用户当前使用的数字人头像
export const getDigitalHumanAvatar = () => {
  try {
    const config = uni.getStorageSync("user_avatar_config");
    if (config && config.avatarImage) {
      return config.avatarImage;
    }
  } catch (e) {
    console.error("获取数字人头像失败:", e);
  }
  // 默认头像
  return "/static/digitalHuman.jpg";
};

export const getQaSessionId = () => {
  let sid = uni.getStorageSync("session_qa");
  if (!sid) {
    sid = genSessionId();
    uni.setStorageSync("session_qa", sid);
  }
  return sid;
};

export const getRouteSessionId = () => {
  let sid = uni.getStorageSync("session_route");
  if (!sid) {
    sid = genSessionId();
    uni.setStorageSync("session_route", sid);
  }
  return sid;
};

export const isTokenValid = () => {
  const token = getToken();
  if (!token) return false;
  const expireTime = getTokenExpire();
  if (!expireTime) return false;
  return Date.now() < expireTime;
};

export const setAuth = ({
  token,
  userId,
  expiresAt,
  tokenExpire,
  expiresIn,
  expires_in,
  nickname,
  avatarUrl,
  phone,
}) => {
  if (token) safeSetStorage(TOKEN_KEY, token);

  const finalUserId = userId || getUserIdFromToken(token);
  // 防止 Long 类型精度丢失，存储为字符串
  if (finalUserId) safeSetStorage(USER_ID_KEY, String(finalUserId));

  if (nickname) safeSetStorage(NICKNAME_KEY, nickname);
  if (avatarUrl) safeSetStorage(AVATAR_URL_KEY, avatarUrl);
  if (phone) safeSetStorage("phone", phone);

  const expireValue = expiresAt || tokenExpire;
  if (expireValue) {
    const expiry = Number(expireValue);
    if (!Number.isNaN(expiry) && expiry > 0) {
      safeSetStorage(TOKEN_EXPIRE_KEY, expiry);
    } else {
      const parsed = Date.parse(expireValue);
      if (!Number.isNaN(parsed)) {
        safeSetStorage(TOKEN_EXPIRE_KEY, parsed);
      }
    }
  } else if (expiresIn || expires_in) {
    const seconds = Number(expiresIn || expires_in);
    if (!Number.isNaN(seconds) && seconds > 0) {
      safeSetStorage(TOKEN_EXPIRE_KEY, Date.now() + seconds * 1000);
    }
  }
};

export const clearAuth = () => {
  uni.removeStorageSync(TOKEN_KEY);
  uni.removeStorageSync(TOKEN_EXPIRE_KEY);
  uni.removeStorageSync(USER_ID_KEY);
  uni.removeStorageSync(NICKNAME_KEY);
  uni.removeStorageSync(AVATAR_URL_KEY);
};

export { genSessionId };
