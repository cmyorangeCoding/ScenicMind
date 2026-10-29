// 讯飞开放平台配置（数字人 / TTS）
// 使用前请在 https://www.xfyun.cn/ 申请 AppId / AppKey / ApiSecret。
// 请勿把真实密钥提交到仓库：本地调试时临时填写，提交前务必清空。
export const XFYUN_CONFIG = {
  // 数字人服务配置
  virtualHuman: {
    appId: "",
    appKey: "",
    apiSecret: "",
    avatar: {
      modelId: "standard",
      style: "normal",
      avatarId: "201320001",
    },
  },
  // TTS语音合成配置（可选，如果需要单独使用语音）
  tts: {
    appId: "",
    apiKey: "",
    apiSecret: "",
  },
};

// 数字人HTML页面路径（Web SDK方案）
export const VIRTUAL_HUMAN_PAGE_PATH = "/static/xfyun-digital-human/index.html";

// Web API方案页面路径（WebSocket直接调用）
export const VIRTUAL_HUMAN_WEB_API_PATH =
  "/static/xfyun-digital-human/web-api.html";
