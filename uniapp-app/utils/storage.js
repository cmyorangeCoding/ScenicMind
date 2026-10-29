// utils/storage.js
export default {
  // --- 路线相关 ---
  setTargetRoute(route) {
    uni.setStorageSync("target_route", JSON.stringify(route));
  },
  getTargetRoute() {
    try {
      return JSON.parse(uni.getStorageSync("target_route") || "null");
    } catch {
      return null;
    }
  },
  setRealRoute(route) {
    uni.setStorageSync("real_route", JSON.stringify(route));
  },
  getRealRoute() {
    try {
      return JSON.parse(uni.getStorageSync("real_route") || "[]");
    } catch {
      return [];
    }
  },

  // --- 导览状态与位置 ---
  setIsGuiding(status) {
    uni.setStorageSync("is_guiding", status);
  },
  getIsGuiding() {
    return uni.getStorageSync("is_guiding") === true;
  },
  setCurrentLocation(loc) {
    uni.setStorageSync("current_loc", JSON.stringify(loc));
  },
  getCurrentLocation() {
    try {
      return JSON.parse(uni.getStorageSync("current_loc") || "null");
    } catch {
      return null;
    }
  },
  setUserPosition(pos) {
    uni.setStorageSync("user_position", JSON.stringify(pos));
  },
  getUserPosition() {
    try {
      return JSON.parse(uni.getStorageSync("user_position") || "null");
    } catch {
      return null;
    }
  },
  setLastUpdateTime(time) {
    uni.setStorageSync("last_update_time", time);
  },
  getLastUpdateTime() {
    return uni.getStorageSync("last_update_time") || null;
  },

  // --- 景点游览记录 (用于生成游记) ---
  addVisitRecord(record) {
    let records = this.getVisitRecords();
    records.push(record);
    uni.setStorageSync("visit_records", JSON.stringify(records));
  },
  updateVisitEndTime(spotName, endTime) {
    let records = this.getVisitRecords();
    const index = records.findIndex(
      (r) => r.spotName === spotName && !r.endTime,
    );
    if (index !== -1) {
      records[index].endTime = endTime;
      uni.setStorageSync("visit_records", JSON.stringify(records));
    }
  },
  getVisitRecords() {
    try {
      return JSON.parse(uni.getStorageSync("visit_records") || "[]");
    } catch {
      return [];
    }
  },
  clearVisitRecords() {
    uni.removeStorageSync("visit_records");
  },

  // --- 用户信息 ---
  getUserId() {
    return uni.getStorageSync("userId") || "";
  },

  clearAllGuideData() {
    this.setTargetRoute(null);
    this.setRealRoute([]);
    this.setIsGuiding(false);
    this.clearVisitRecords();
    this.setCurrentLocation(null);
    this.setUserPosition(null);
  },

  // --- 路线 chatId 列表（带状态和 routeId）---
  // 数据结构: [{ chatId: "xxx", isCollected: false, routeId: null }, ...]
  setCollectedChatIds(chatIdsWithStatus) {
    uni.setStorageSync("collected_chat_ids", JSON.stringify(chatIdsWithStatus));
  },
  getCollectedChatIds() {
    try {
      return JSON.parse(uni.getStorageSync("collected_chat_ids") || "[]");
    } catch {
      return [];
    }
  },

  // 更新 chatId 的收藏状态
  updateChatIdStatus(chatId, isCollected) {
    const list = this.getCollectedChatIds();
    const item = list.find((item) => String(item.chatId) === String(chatId));
    if (item) {
      item.isCollected = isCollected;
      this.setCollectedChatIds(list);
    }
  },

  // 获取 chatId 的收藏状态
  getChatIdStatus(chatId) {
    const list = this.getCollectedChatIds();
    const item = list.find((item) => String(item.chatId) === String(chatId));
    return item ? item.isCollected : false;
  },

  // 更新 chatId 的 routeId
  updateChatIdRouteId(chatId, routeId) {
    const list = this.getCollectedChatIds();
    const item = list.find((item) => String(item.chatId) === String(chatId));
    if (item) {
      item.routeId = routeId;
      this.setCollectedChatIds(list);
    }
  },

  // 获取 chatId 的 routeId
  getChatIdRouteId(chatId) {
    const list = this.getCollectedChatIds();
    const item = list.find((item) => String(item.chatId) === String(chatId));
    return item ? item.routeId : null;
  },

  // --- 流式播报开关 ---
  // true = 开启流式播报，false = 关闭流式播报
  setStreamSpeakEnabled(enabled) {
    uni.setStorageSync("stream_speak_enabled", enabled);
  },
  getStreamSpeakEnabled() {
    // 默认开启
    const stored = uni.getStorageSync("stream_speak_enabled");
    return stored === false ? false : true;
  },

  // --- 导览页播报开关 ---
  // true = 开启自动播报，false = 关闭自动播报
  setGuideCompanionSpeakEnabled(enabled) {
    uni.setStorageSync("guide_companion_speak_enabled", enabled);
  },
  getGuideCompanionSpeakEnabled() {
    // 默认开启
    const stored = uni.getStorageSync("guide_companion_speak_enabled");
    return stored === false ? false : true;
  },
};
