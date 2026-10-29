// utils/websocket.js
import { BASE_URL } from "../config.js";

let socketTask = null;
let messageCallback = null;
let isStreamFinished = false; // 标记流是否结束
let reconnectTimer = null; // 重连定时器
let connectAttempts = 0; // 连接尝试次数
const MAX_RECONNECT_ATTEMPTS = 5; // 最大重连次数
const RECONNECT_DELAY = 3000; // 重连延迟（毫秒）

// 从 BASE_URL 解析 host 和 port（兼容 uni-app 环境）
const parseUrl = (url) => {
  const match = url.match(/^(?:http|ws):\/\/([^:/]+)(?::(\d+))?/);
  if (match) {
    return {
      hostname: match[1],
      port: match[2] || "8080",
    };
  }
  return { hostname: "127.0.0.1", port: "8080" };
};

const { hostname: defaultWsHost, port: defaultWsPort } = parseUrl(BASE_URL);

// 获取WebSocket地址（支持环境变量配置，默认从BASE_URL解析）
const getWsUrl = (userId, sessionId, token) => {
  const wsHost = process.env.VUE_APP_WS_HOST || defaultWsHost;
  const wsPort = process.env.VUE_APP_WS_PORT || defaultWsPort;
  return `ws://${wsHost}:${wsPort}/ws/chat?userId=${userId}&sessionId=${sessionId}&token=${encodeURIComponent(token)}`;
};

export default {
  // 1. 建立连接（支持重连）
  connect(userId, sessionId) {
    // 如果正在重连中，先清除定时器
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }

    // 如果已有连接，先关闭
    if (socketTask) {
      try {
        socketTask.close();
      } catch (e) {
        console.warn("关闭旧连接时出错:", e);
      }
      socketTask = null;
    }

    connectAttempts = 0;
    this.attemptConnect(userId, sessionId);
  },

  // 尝试连接
  attemptConnect(userId, sessionId) {
    if (connectAttempts >= MAX_RECONNECT_ATTEMPTS) {
      console.error(
        `WebSocket 连接失败，已尝试 ${MAX_RECONNECT_ATTEMPTS} 次，停止重连`,
      );
      return;
    }

    connectAttempts++;

    // 获取 token
    const token = uni.getStorageSync("token") || "";
    console.log(
      "🔑 [WebSocket Token] Token:",
      token ? `${token.substring(0, 20)}...` : "未找到",
    );

    const wsUrl = getWsUrl(userId, sessionId, token);
    console.log("🌐 [WebSocket URL]", wsUrl);

    socketTask = uni.connectSocket({
      url: wsUrl,
      success: () => {
        console.log("✅ [WebSocket] 连接请求已发送");
      },
      fail: (err) => {
        console.error(`❌ [WebSocket] 连接失败:`, err);
        this.scheduleReconnect(userId, sessionId);
      },
    });

    // 监听连接打开
    socketTask.onOpen(() => {
      console.log("✅ [WebSocket] 连接已建立");
      connectAttempts = 0; // 重置连接尝试次数
      isStreamFinished = false;
    });

    // 监听消息接收（转发给页面处理）
    socketTask.onMessage((res) => {
      if (messageCallback) {
        const result = messageCallback(res.data);
        // 如果回调返回true，表示需要终止流
        if (result === true) {
          this.close();
        }
      }
    });

    // 监听错误
    socketTask.onError((err) => {
      console.error("❌ [WebSocket] 发生错误:", err);
      isStreamFinished = true;
      // 尝试重连
      this.scheduleReconnect(userId, sessionId);
    });

    // 监听关闭
    socketTask.onClose((code, reason) => {
      console.log(`🔌 [WebSocket] 连接断开，代码: ${code}, 原因: ${reason}`);
      socketTask = null;
      isStreamFinished = false;

      // 只有非正常关闭才重连
      if (code !== 1000) {
        this.scheduleReconnect(userId, sessionId);
      }
    });
  },

  // 调度重连
  scheduleReconnect(userId, sessionId) {
    if (connectAttempts >= MAX_RECONNECT_ATTEMPTS) {
      return;
    }

    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
    }

    reconnectTimer = setTimeout(() => {
      this.attemptConnect(userId, sessionId);
    }, RECONNECT_DELAY * connectAttempts); // 指数退避策略

    console.log(
      `将在 ${(RECONNECT_DELAY * connectAttempts) / 1000} 秒后尝试第 ${connectAttempts + 1} 次重连`,
    );
  },

  // 2. 发送消息
  send(data) {
    console.log("准备发送数据，当前 socketTask:", socketTask);

    if (socketTask) {
      socketTask.send({
        data: data,
        success: () => console.log("数据发送成功"),
        fail: (err) => {
          console.error("数据发送失败:", err);
          // 发送失败时也尝试重连
          console.log("WebSocket 发送失败");
        },
      });
      isStreamFinished = false;
    } else {
      console.error("❌ 发送失败：Socket 未连接");
      console.log("WebSocket 网络连接已断开");
    }
  },

  // 3. 注册消息监听器
  onMessage(cb) {
    messageCallback = cb;
  },

  // 4. 关闭连接（主动关闭，不重连）
  close() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }

    if (socketTask) {
      try {
        socketTask.close({ code: 1000, reason: "主动关闭" });
      } catch (e) {
        console.warn("关闭连接时出错:", e);
      }
      socketTask = null;
    }
    isStreamFinished = true;
    connectAttempts = 0;
  },

  // 5. 获取流状态
  isFinished() {
    return isStreamFinished;
  },

  // 6. 检查连接状态
  isConnected() {
    return socketTask !== null;
  },

  // 7. 重置连接（强制重新连接）
  reconnect(userId, sessionId) {
    this.close();
    this.connect(userId, sessionId);
  },
};
