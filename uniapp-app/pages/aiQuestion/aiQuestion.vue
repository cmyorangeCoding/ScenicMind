<script setup>
import { ref, nextTick, onMounted, onUnmounted } from "vue";
import { onShow, onHide } from "@dcloudio/uni-app";
import request from "../../request.js";
import {
  getUserId,
  getQaSessionId,
  getAvatarUrl,
  getDigitalHumanAvatar,
} from "../../auth.js";
import UniIcons from "@dcloudio/uni-ui/lib/uni-icons/uni-icons.vue";
import wsUtil from "@/utils/websocket.js";
import { speechToText, clearTokenCache } from "@/utils/aliyunAsr.js";
import permission from "@/utils/permission.js";
import storage from "@/utils/storage.js";

// 用户信息
const userId = ref(getUserId());
const sessionId = ref("");
const userAvatar = ref(getAvatarUrl() || "/static/user-avatar.png");
const digitalHumanAvatar = ref(getDigitalHumanAvatar());

// 聊天消息
const messages = ref([
  {
    role: "ai",
    content:
      "你好呀！我是灵山胜境AI导游，你可以向我提问景区知识、景区历史等问题哦~",
    chatId: "",
  },
]);

const question = ref("");
const scrollTop = ref("");
const scrollTopValue = ref(0);
let scrollRequestId = 0; // 滚动请求计数器，确保每次滚动都能触发

// 猜你想问
const allHotQuestions = ref([]);
const hotQuestions = ref([]);
const isVoiceMode = ref(false);
const keyboardHeight = ref(0);

// 导览状态相关
const isGuiding = ref(false);
const visitedSpots = ref([]);

// 流式播报开关（喇叭图标）
const isStreamSpeakEnabled = ref(true); // 默认开启

// 当前正在手动播报的消息索引（用于显示取消播报按钮）
const currentSpeakingIndex = ref(-1);

// 已收藏的 chatId 列表（从 storage 加载）
const collectedChatIds = ref([]);

let isAiThinking = false; // 锁
let thinkingTimeoutTimer = null; // 思考超时定时器（防止锁永远不释放）
const THINKING_TIMEOUT = 30000; // 30秒超时
let waitingTimer = null; // 等待下一段内容的定时器
let isFirstWaiting = false; // 第一次等待标记
const WAITING_DELAY = 500; // 停止接收数据500ms后显示等待中UI

// 初始等待阶段的思考文本切换
const thinkingTexts = ["", "思考中", "资料查询中", "回答生成中"];
const currentThinkingIndex = ref(0); // 当前显示的思考文本索引
let initialThinkingTimers = []; // 初始等待阶段的定时器数组

// 录音相关
const isRecording = ref(false);
const recorderManager = ref(null);
const recordStartTime = ref(0);
let startTouchX = 0; // 开始触摸时的X坐标
let startTouchY = 0; // 开始触摸时的Y坐标

// 拍照/相册相关
const showPhotoMenu = ref(false);

// ========== 数字人相关 ==========
const isDigitalHumanLoading = ref(true);
const digitalHumanWebview = ref(null);
const htmlPath = "/static/xfyun-digital-human/web-api.html";

// 流式播报控制器
let streamBuffer = ""; // 待播报文本缓冲（过滤后的纯文本）
let isSpeaking = false; // 是否正在播报
let speakQueue = []; // 播报队列
let speakEndPollTimer = null; // 播报结束轮询定时器（作为speakEnd事件的后备）
let forceNextSentenceTimer = null; // 强制超时定时器（防止播报卡住）

// 标签过滤状态
let isFilteringTag = false; // 是否正在过滤标签（如[[ROUTE_RECOMMENDATION]]）
let isPendingBracket = false; // 是否正在等待第二个[（用于检测[[）
let tagBuffer = ""; // 标签缓冲区，用于累积标签内容直到找到]]

// 过滤函数：只保留纯文本，用于数字人播报
const filterPureText = (content) => {
  if (!content) return "";

  let text = content;

  // ========== 基础标签过滤 ==========
  // 过滤 ROUTE_RECOMMENDATION 标签
  text = text.replace(/\[\[ROUTE_RECOMMENDATION\]\]/gi, "");

  // 过滤 CHAT_ID 标签
  text = text.replace(/\[?\[?CHAT_ID\]?\]:?\d*/gi, "");

  // ========== Markdown 格式过滤 ==========
  // 过滤加粗 **内容**
  text = text.replace(/\*\*(.+?)\*\*/g, "$1");
  // 过滤斜体 *内容*
  text = text.replace(/\*(.+?)\*/g, "$1");
  // 过滤代码 `内容`
  text = text.replace(/`(.+?)`/g, "$1");
  // 过滤链接 [文本](url)
  text = text.replace(/\[([^\]]+)\]\([^)]+\)/g, "$1");

  // ========== Emoji 过滤（增强版）==========
  // 过滤基本多文种平面的 emoji
  text = text.replace(/[\u2600-\u26FF]/g, "");
  // 过滤杂项符号
  text = text.replace(/[\u2700-\u27BF]/g, "");
  // 过滤 Dingbats
  text = text.replace(/[\u27C0-\u27EF]/g, "");
  // 过滤几何形状
  text = text.replace(/[\u25A0-\u25FF]/g, "");
  // 过滤箭头符号
  text = text.replace(/[\u2190-\u21FF]/g, "");
  // 过滤数学运算符
  text = text.replace(/[\u2200-\u22FF]/g, "");
  // 过滤代理对（表情符号常用）
  text = text.replace(/[\uD800-\uDBFF][\uDC00-\uDFFF]/g, "");
  // 过滤扩展表情符号区块
  text = text.replace(/[\u{1F000}-\u{1FFFF}]/gu, "");
  // 过滤特殊符号（如 →、←、↑、↓ 等）
  text = text.replace(/[→←↑↓↔↕↖↗↘↙⇒⇐⇑⇓⇔⇕⇖⇗⇘⇙]/g, "");

  // ========== 特殊标记过滤 ==========
  // 过滤特殊标记（如 [微笑]、[开心] 等）
  text = text.replace(/\[.*?\]/g, "");

  // 过滤 HTML 标签
  text = text.replace(/<[^>]*>/g, "");

  // 过滤多余空白
  text = text.replace(/\s+/g, " ").trim();

  return text;
};

// 发送文本给数字人播报
const sendToDigitalHuman = (text) => {
  console.log("[数字人] 尝试发送文本:", text);
  console.log("[数字人] webview状态:", {
    exists: !!digitalHumanWebview.value,
    webview: digitalHumanWebview.value,
  });

  if (!digitalHumanWebview.value) {
    console.error("[数字人] ❌ webview 不存在，无法播报");
    return;
  }

  const pureText = filterPureText(text);
  console.log("[数字人] 二次过滤后文本:", pureText);

  if (!pureText) {
    console.log("[数字人] 过滤后无内容，跳过播报");
    return;
  }

  console.log("[数字人] ✅ 发送播报文本:", pureText);
  try {
    // 使用 JSON.stringify 确保安全转义（处理所有特殊字符）
    const escapedText = JSON.stringify(pureText);
    digitalHumanWebview.value.evalJS(`window.sendTextInteract(${escapedText})`);
    console.log("[数字人] ✅ 发送成功");

    // ===== 注释掉强制超时机制 =====
    // 设置强制超时定时器（防止播报卡住）
    // const timeoutMs = 500 + pureText.length * 500;
    // setupForceTimeout(timeoutMs);
  } catch (e) {
    console.error("[数字人] ❌ 发送失败:", e);
  }
};

// ===== 注释掉强制超时机制 =====
// // 设置强制超时定时器
// const setupForceTimeout = (timeoutMs) => {
//   // 清除之前的定时器
//   if (forceNextSentenceTimer) {
//     clearTimeout(forceNextSentenceTimer);
//   }

//   console.log("[流式播报] 设置强制超时定时器，超时时间:", timeoutMs, "ms");

//   forceNextSentenceTimer = setTimeout(() => {
//     console.log("[流式播报] ⚠️ 强制超时触发，尝试继续播报");
//     // 如果数字人还在"播报中"状态但长时间没有响应，强制结束当前播报
//     if (isSpeaking && (speakQueue.length > 0 || streamBuffer.length > 0)) {
//       console.log("[流式播报] ⚠️ 强制结束当前播报状态");
//       isSpeaking = false;
//       // 处理队列或缓冲中的内容
//       onSpeakEnd();
//     }
//     forceNextSentenceTimer = null;
//   }, timeoutMs);
// };

// // 清除强制超时定时器
// const clearForceTimeout = () => {
//   if (forceNextSentenceTimer) {
//     clearTimeout(forceNextSentenceTimer);
//     forceNextSentenceTimer = null;
//     console.log("[流式播报] 清除强制超时定时器");
//   }
// };

// 处理流式文本，按句子分割后发送给数字人（利用SDK追加模式）
const processStreamText = (newText) => {
  console.log("[流式播报] 收到新文本:", newText);

  // 1. 过滤特殊字符后加入缓冲
  const pureText = filterPureText(newText);
  console.log("[流式播报] 过滤后文本:", pureText);

  if (!pureText) {
    console.log("[流式播报] 过滤后无内容，跳过");
    return;
  }

  streamBuffer += pureText;
  console.log("[流式播报] 当前缓冲:", streamBuffer);

  // 2. 检查缓冲中是否有完整句子，有则发送给数字人
  sendCompleteSentences();
};

// 发送缓冲中所有完整句子（利用数字人SDK的追加模式）
const sendCompleteSentences = () => {
  console.log("[流式播报] 检查缓冲中完整句子");

  // 循环检查并发送所有完整句子
  while (streamBuffer.length > 0) {
    // 查找完整句子结束位置
    const endIndex = findSentenceEnd(streamBuffer);

    if (endIndex === -1) {
      console.log("[流式播报] 缓冲中无完整句子，保留缓冲继续等待");
      return;
    }

    // 提取完整句子
    const completeSentence = streamBuffer.substring(0, endIndex + 1);
    const remaining = streamBuffer.substring(endIndex + 1);

    console.log("[流式播报] 提取完整句子:", completeSentence);
    console.log("[流式播报] 剩余缓冲:", remaining);

    // 直接发送给数字人（SDK的追加模式会自动排队）
    if (digitalHumanWebview.value) {
      const escapedText = JSON.stringify(completeSentence);
      digitalHumanWebview.value.evalJS(
        `window.sendTextInteract(${escapedText})`,
      );
      console.log("[流式播报] ✅ 句子已发送（追加模式）");

      // 标记正在播报
      if (!isSpeaking) {
        isSpeaking = true;
      }
    } else {
      console.error("[流式播报] ❌ webview不存在");
    }

    // 更新缓冲
    streamBuffer = remaining;
  }

  console.log("[流式播报] 缓冲已清空，等待新数据");
};

// 启动播报结束轮询（作为speakEnd事件的后备机制）
const startSpeakEndPolling = () => {
  // 清除之前的定时器
  if (speakEndPollTimer) {
    clearInterval(speakEndPollTimer);
  }

  console.log("[流式播报] 启动播报结束轮询");

  // 每800ms检查一次数字人是否还在播报（根据语速调整）
  speakEndPollTimer = setInterval(() => {
    // 始终检查数字人状态，而不仅仅是队列有内容时
    if (isSpeaking) {
      checkDigitalHumanSpeakingStatus();
    }

    // 如果数字人已停止且队列有内容，继续播放
    if (!isSpeaking && speakQueue.length > 0) {
      console.log("[流式播报] 轮询检测到数字人已停止，队列有内容，继续播放");
      clearInterval(speakEndPollTimer);
      speakEndPollTimer = null;
      const nextText = speakQueue.shift();
      isSpeaking = true;
      sendToDigitalHuman(nextText);
      startSpeakEndPolling();
    } else if (!isSpeaking && speakQueue.length === 0) {
      // 播报结束且队列为空，停止轮询
      console.log("[流式播报] 轮询检测到播报结束，队列为空，停止轮询");
      clearInterval(speakEndPollTimer);
      speakEndPollTimer = null;
    }
  }, 800);
};

// 检查数字人播报状态
const checkDigitalHumanSpeakingStatus = () => {
  if (!digitalHumanWebview.value) return;

  try {
    // 尝试通过evalJS查询数字人状态
    digitalHumanWebview.value.evalJS(`
      (function() {
        var isSpeaking = false;
        if (window.avatarDriver && typeof window.avatarDriver.isSpeaking === 'boolean') {
          isSpeaking = window.avatarDriver.isSpeaking;
        } else if (window.AIUI && typeof window.AIUI.isSpeaking === 'boolean') {
          isSpeaking = window.AIUI.isSpeaking;
        } else if (window.digitalHuman && typeof window.digitalHuman.isSpeaking === 'boolean') {
          isSpeaking = window.digitalHuman.isSpeaking;
        }
        window.postMessage({ type: 'speakingStatus', data: isSpeaking });
      })();
    `);
  } catch (e) {
    console.log("[数字人状态检查] 检查失败:", e);
  }
};

// 在文本中查找句子结束位置（跳过引号内的标点）
const findSentenceEnd = (text) => {
  let inSingleQuote = false; // 是否在单引号内
  let inDoubleQuote = false; // 是否在双引号内
  let inChineseQuote = false; // 是否在中文引号「」内

  for (let i = 0; i < text.length; i++) {
    const char = text[i];

    // 处理引号状态
    if (char === "'" && !inDoubleQuote && !inChineseQuote) {
      inSingleQuote = !inSingleQuote;
    } else if (char === '"' && !inSingleQuote && !inChineseQuote) {
      inDoubleQuote = !inDoubleQuote;
    } else if (char === "「" && !inSingleQuote && !inDoubleQuote) {
      inChineseQuote = true;
    } else if (char === "」" && !inSingleQuote && !inDoubleQuote) {
      inChineseQuote = false;
    }

    // 如果不在任何引号内，检查是否是句子结束符
    if (!inSingleQuote && !inDoubleQuote && !inChineseQuote) {
      if (
        char === "。" ||
        char === "！" ||
        char === "？" ||
        char === "…" ||
        char === "～"
      ) {
        return i;
      }
      // 逗号作为句子分隔符，但仅在后面有更多内容时
      if (char === "，" || char === "、") {
        // 检查后面是否还有内容（至少3个字符，避免单独的逗号）
        if (i + 1 < text.length && text.substring(i + 1).trim().length > 2) {
          // 检查逗号后是否是引号开头，如果是则不分割
          const nextChar = text[i + 1];
          if (nextChar !== '"' && nextChar !== "'" && nextChar !== "「") {
            return i;
          }
        }
      }
    }
  }
  return -1;
};

// 数字人播报结束回调（由 webview 消息触发）
const onSpeakEnd = () => {
  console.log("[流式播报] 数字人播报结束");
  isSpeaking = false;

  // 清除手动播报状态 - 这会将"取消播报"按钮变回"播报"
  currentSpeakingIndex.value = -1;

  // ===== 注释掉强制超时机制 =====
  // clearForceTimeout();

  // 数字人SDK已处理队列，不需要前端处理
  console.log("[流式播报] 数字人SDK已处理队列，无需前端干预");
};

// 流式播报结束（数字人SDK已自动处理所有追加的文本）
const finishStreamSpeak = () => {
  console.log("[流式播报] 流结束");
  console.log("[流式播报] 数字人状态:", {
    isSpeaking,
  });

  // 数字人SDK的追加模式会自动处理所有已发送的文本
  // 不需要额外处理剩余内容
  console.log("[流式播报] 数字人SDK已自动处理所有追加文本");
};

// 创建数字人 webview
const createDigitalHumanWebview = () => {
  console.log("========== 开始创建数字人 webview ==========");

  // 防止重复创建
  if (digitalHumanWebview.value) {
    console.log("⚠️ 数字人webview已存在，跳过创建");
    return;
  }

  // H5环境降级方案
  if (typeof plus === "undefined" || !plus.webview) {
    console.log("[数字人] H5环境，跳过webview创建");
    return;
  }

  // 获取当前页面实例（延迟确保页面webview已就绪）
  setTimeout(() => {
    const pages = getCurrentPages();
    const currentPage = pages[pages.length - 1];

    // 尝试多种方式获取当前webview
    let currentWebview = null;
    if (currentPage.$page && currentPage.$page.getWebview) {
      currentWebview = currentPage.$page.getWebview();
    }
    if (!currentWebview && plus.webview.currentWebview) {
      currentWebview = plus.webview.currentWebview();
    }

    // 如果获取到的是service webview，尝试其他方式
    if (
      currentWebview &&
      currentWebview.__uuid__ &&
      currentWebview.__uuid__.includes("service")
    ) {
      const allWebviews = plus.webview.all();
      for (const wv of allWebviews) {
        const uuid = wv.__uuid__ || "";
        const wvId = (wv.id || "").toString();
        if (!uuid.includes("service") && !wvId.includes("__uniapp__")) {
          currentWebview = wv;
          break;
        }
      }
    }

    if (!currentWebview) {
      console.error("[数字人] 无法获取当前页面webview");
      return;
    }

    // 使用 uni.createSelectorQuery 获取头像占位框的实际位置
    const query = uni.createSelectorQuery().in(currentPage);
    query
      .select(".avatar-section")
      .boundingClientRect((rect) => {
        if (!rect) {
          console.error("[数字人] 无法获取头像占位框位置，尝试延迟重试");
          setTimeout(() => {
            createDigitalHumanWebview();
          }, 500);
          return;
        }

        console.log("[数字人] 头像占位框实际位置:", rect);

        const systemInfo = uni.getSystemInfoSync();
        const statusBarHeight = systemInfo.statusBarHeight || 0;

        // 使用固定位置和尺寸（恢复原始尺寸）
        const width = 196; // 固定宽度
        const height = 275; // 固定高度（恢复原始值）
        const left = -85; // 固定左边距
        const top = -50; // 向下移动10rpx（约5px）

        console.log("[数字人] 状态栏高度:", statusBarHeight);
        console.log("[数字人] webview 尺寸:", width, "x", height);
        console.log("[数字人] webview 位置:", left, top);

        // 使用预加载的 webview（与companionGuide保持一致）
        let preloadedWebview = null;
        try {
          preloadedWebview = plus.webview.getWebviewById("preloadDigitalHuman");
          console.log(
            "[数字人] 尝试获取预加载webview，结果:",
            !!preloadedWebview,
          );
        } catch (e) {
          console.warn("[数字人] 获取预加载webview失败，将创建新的:", e);
        }

        if (preloadedWebview) {
          console.log("[数字人] 找到预加载的 webview:", preloadedWebview.id);
          digitalHumanWebview.value = preloadedWebview;

          // 调整位置和尺寸（与companionGuide完全一致）
          // 注意：不要调用 hide()，由 onShow 控制显示逻辑
          digitalHumanWebview.value.setStyle({
            left: left,
            top: top,
            width: width,
            height: height,
            opacity: 1,
            show: false, // 保持隐藏状态，由onShow控制显示
            scalable: false,
            bounce: "none",
            background: "transparent",
            webkitOverflowScrolling: "touch",
            allowZoom: false,
            showScrollbar: false,
            zindex: 1, // 与companionGuide保持一致，避免遮挡导航栏
            popGesture: "none",
            hardwareAccelerated: true,
            ignoretouch: true, // 忽略触摸事件，让点击穿透到下面的输入框
            hitTest: false, // 不进行点击测试
            pointerEvents: "none", // CSS pointer-events设为none，允许点击穿透
          });

          // 不要调用show()，完全由onShow控制
          console.log("[数字人] 样式设置完成，等待onShow显示");

          // 切换到半身模式（确保是半身模式，因为首页可能切换成了全身模式）
          setTimeout(() => {
            digitalHumanWebview.value.evalJS("window.switchToHalfBody()");
          }, 200);
        } else {
          // 创建新的webview（备用）
          console.log("[数字人] 未找到预加载webview，创建新的");
          digitalHumanWebview.value = plus.webview.create(
            "_www/static/xfyun-digital-human/web-api.html",
            "digitalHumanAiQuestion",
            {
              left: left,
              top: top,
              width: width,
              height: height,
              scalable: false,
              bounce: "none",
              background: "transparent",
              webkitOverflowScrolling: "touch",
              allowZoom: false,
              showScrollbar: false,
              zindex: 1,
              popGesture: "none",
              hardwareAccelerated: true,
              ignoretouch: true, // 忽略触摸事件，让点击穿透到下面的输入框
              hitTest: false,
              pointerEvents: "none",
            },
          );

          // 显式显示新创建的webview
          digitalHumanWebview.value.show();
          console.log("[数字人] 新webview已创建并调用 show()");
        }

        console.log("[数字人] webview 设置完成");

        // 监听消息
        digitalHumanWebview.value.addEventListener("message", (e) => {
          const data = e.data && e.data[0];
          if (data) {
            console.log("[aiQuestion webview 消息] 收到消息:", data);

            if (data.type === "ready") {
              isDigitalHumanLoading.value = false;
              console.log("[数字人] 数字人已就绪");
            } else if (data.type === "speakEnd") {
              onSpeakEnd();
            } else if (data.type === "speakingStatus") {
              // 处理数字人播报状态查询结果
              console.log("[数字人状态] 播报中:", data.data);
              if (!data.data && isSpeaking) {
                // 数字人已停止播报
                console.log("[数字人状态] 检测到播报结束");
                isSpeaking = false;
                // 立即处理队列或缓冲中的内容
                onSpeakEnd();
              }
            }
          }
        });

        // 发送初始化消息给数字人（与companionGuide保持一致，使用window.postMessage）
        setTimeout(() => {
          const msgData = {
            type: "setMerchantInfo",
            data: {
              merchantName: "智能问答助手",
              introduction: "我是您的智能问答助手，有什么问题可以问我。",
            },
          };
          const script = `window.postMessage(${JSON.stringify(msgData)});`;
          digitalHumanWebview.value.evalJS(script);
          console.log("[数字人] 已发送初始化消息");

          // 触发数字人播放（处理移动端自动播放限制）
          setTimeout(() => {
            const playScript = `window.postMessage(${JSON.stringify({ type: "play" })});`;
            digitalHumanWebview.value.evalJS(playScript);
            console.log("[数字人] 已发送播放消息");
          }, 300);
        }, 500);

        console.log("[数字人] webview 创建完成");
      })
      .exec();
  }, 500); // 延迟500ms确保页面webview已准备好
};

// 销毁数字人 webview
const destroyDigitalHumanWebview = () => {
  console.log("========== 销毁数字人webview ==========");
  console.log("当前digitalHumanWebview:", digitalHumanWebview.value);

  if (digitalHumanWebview.value) {
    try {
      console.log("正在销毁数字人webview:", digitalHumanWebview.value.id);

      // 检查是否是预加载的webview（与companionGuide保持一致）
      const isPreloaded =
        digitalHumanWebview.value.id === "preloadDigitalHuman" ||
        digitalHumanWebview.value.__uuid__ === "preloadDigitalHuman";

      if (isPreloaded) {
        // 预加载的webview不销毁，只是移回隐藏位置
        console.log("✅ 预加载webview，移回隐藏位置");
        digitalHumanWebview.value.setStyle({
          left: -9999,
          top: -9999,
          width: 1,
          height: 1,
          opacity: 0,
          show: false,
          zindex: 1, // 恢复默认z-index
          ignoretouch: true, // 保持忽略触摸事件，避免拦截焦点
          hitTest: false, // 保持不进行点击测试
          pointerEvents: "none", // 保持允许点击穿透
        });
      } else {
        // 先隐藏
        if (digitalHumanWebview.value.hide) {
          console.log("步骤1: 隐藏webview");
          digitalHumanWebview.value.hide();
        }
        // 然后关闭
        if (digitalHumanWebview.value.close) {
          console.log("步骤2: 关闭webview");
          digitalHumanWebview.value.close("none");
        } else if (digitalHumanWebview.value.parentNode) {
          console.log("步骤2: 移除DOM节点");
          digitalHumanWebview.value.parentNode.removeChild(
            digitalHumanWebview.value,
          );
        }
      }
      digitalHumanWebview.value = null;
      console.log("✅ 数字人webview处理成功");
    } catch (e) {
      console.error("❌ 销毁数字人webview失败:", e);
      digitalHumanWebview.value = null;
    }
  } else {
    console.log("⚠️ 没有需要销毁的数字人webview");
  }
};
// 图片相关
const selectedImage = ref(null);
const imagePreviewVisible = ref(false);

// 初始化会话ID
const initSession = () => {
  sessionId.value = getQaSessionId();
};

// 加载聊天历史（带重试机制）
const loadChatHistory = async (retryCount = 0) => {
  const maxRetries = 3;
  try {
    const res = await request.get("/user/ai/chatHistory", {
      userId: userId.value,
      sessionId: sessionId.value,
    });

    if (res.code === 200 && res.data?.length > 0) {
      // ========== 日志：对比 history 返回的 chatId 和本地存储的 chatId ==========
      console.log("========== 历史记录加载对比 ==========");

      // 提取本地存储的 chatId（兼容新老数据结构，统一转换为字符串类型）
      const storedChatIds = collectedChatIds.value
        .map((item) => {
          if (typeof item === "object" && item.chatId) {
            return String(item.chatId); // 统一转换为字符串
          } else if (typeof item === "string") {
            return item;
          }
          return null;
        })
        .filter(Boolean);

      // history 接口返回的 chatId 统一转换为字符串类型
      const historyChatIds = res.data
        .map((item) => String(item.chatId)) // 统一转换为字符串
        .filter(Boolean);

      // 找出匹配和不匹配的 chatId
      const matchedChatIds = [];
      const unmatchedChatIds = [];
      historyChatIds.forEach((chatId) => {
        const exists = storedChatIds.includes(chatId);
        if (exists) {
          matchedChatIds.push(chatId);
        } else {
          unmatchedChatIds.push(chatId);
        }
      });

      console.log(
        "✓ 匹配的 chatId（显示收藏按钮）:",
        matchedChatIds.length,
        "个",
      );
      console.log(
        "✗ 不匹配的 chatId（不显示收藏按钮）:",
        unmatchedChatIds.length,
        "个",
      );
      console.log("======================================");

      // ========== 构建消息列表 ==========
      const list = [];
      res.data.forEach((item) => {
        list.push({
          role: "user",
          content: item.question,
          imageUrl: item.imageUrl || null,
          chatId: null,
          isCollected: false,
        });
        // 只有本地存储中有的 chatId 才显示收藏按钮（即：只有通过 chat 接口收到的 chatId 才显示）
        const chatId = item.chatId || null;
        // 检查 chatId 是否在本地存储中（统一转换为字符串比较）
        const hasStoredChatId =
          chatId && storedChatIds.includes(String(chatId));

        list.push({
          role: "ai",
          content: item.answer,
          imageUrl: null,
          chatId: hasStoredChatId ? chatId : null, // 只有存储过的才显示收藏按钮
          isCollected: hasStoredChatId ? getChatIdStatus(chatId) : false, // 根据本地存储的状态判断是否收藏
        });
      });
      messages.value = list;
      scrollToBottom();
    }
  } catch (e) {
    console.error("加载历史失败", e);
    // 如果是网络错误，尝试重试
    if (
      retryCount < maxRetries &&
      e.errMsg &&
      e.errMsg.includes("Failed to connect")
    ) {
      console.log(`加载历史失败，第 ${retryCount + 1} 次重试...`);
      await new Promise((resolve) =>
        setTimeout(resolve, 1000 * (retryCount + 1)),
      );
      await loadChatHistory(retryCount + 1);
    }
  }
};

// 检查导览状态
const checkGuidingStatus = () => {
  try {
    const guiding = storage.getIsGuiding();
    const spots = storage.getVisitRecords();
    isGuiding.value = guiding || false;
    visitedSpots.value = spots || [];
  } catch (e) {
    console.log("检查导览状态失败", e);
  }
};

// 加载已收藏的 chatId 列表（带状态）
// 数据结构: [{ chatId: "xxx", isCollected: false }, ...]
const loadCollectedChatIds = () => {
  try {
    const saved = storage.getCollectedChatIds();
    collectedChatIds.value = saved || [];
    console.log("已加载收藏的 chatId 列表:", collectedChatIds.value);
  } catch (e) {
    console.log("加载收藏列表失败", e);
    collectedChatIds.value = [];
  }
};

// 保存 chatId 到收藏列表（如果已存在则跳过）
const saveCollectedChatId = (chatId) => {
  if (!chatId) return;

  // 检查是否已存在
  const chatIdStr = String(chatId);
  const exists = collectedChatIds.value.find(
    (item) => String(item.chatId) === chatIdStr,
  );
  if (exists) {
    return;
  }

  // 添加新的 chatId，默认未收藏，routeId 为空
  const newItem = { chatId: chatIdStr, isCollected: false, routeId: null };
  collectedChatIds.value.push(newItem);
  try {
    storage.setCollectedChatIds(collectedChatIds.value);
  } catch (e) {
    console.log("保存收藏失败", e);
  }
};

// 获取 chatId 的收藏状态（统一字符串比较）
const getChatIdStatus = (chatId) => {
  const chatIdStr = String(chatId);
  const item = collectedChatIds.value.find(
    (item) => String(item.chatId) === chatIdStr,
  );
  return item ? item.isCollected : false;
};

// 更新 chatId 的收藏状态（统一字符串比较）
const updateChatIdStatus = (chatId, isCollected) => {
  const chatIdStr = String(chatId);
  const item = collectedChatIds.value.find(
    (item) => String(item.chatId) === chatIdStr,
  );
  if (item) {
    item.isCollected = isCollected;
    storage.updateChatIdStatus(chatIdStr, isCollected);
  }
};

// 更新 chatId 的 routeId
const updateChatIdRouteId = (chatId, routeId) => {
  const chatIdStr = String(chatId);
  const item = collectedChatIds.value.find(
    (item) => String(item.chatId) === chatIdStr,
  );
  if (item) {
    item.routeId = routeId;
    storage.updateChatIdRouteId(chatIdStr, routeId);
  }
};

// 获取 chatId 的 routeId
const getChatIdRouteId = (chatId) => {
  const chatIdStr = String(chatId);
  const item = collectedChatIds.value.find(
    (item) => String(item.chatId) === chatIdStr,
  );
  return item ? item.routeId : null;
};

const scrollToBottom = () => {
  nextTick(() => {
    // 使用递增计数器确保每次都能触发滚动
    scrollRequestId++;
    // 设置一个足够大的值，确保滚动到底部
    // 通过递增来确保每次值都不同，从而触发滚动
    scrollTopValue.value = 99999 + scrollRequestId;
  });
};

const initRecorder = () => {
  recorderManager.value = uni.getRecorderManager();

  recorderManager.value.onStart(() => {
    console.log("✅ 录音开始");
    isRecording.value = true;
    recordStartTime.value = Date.now();
    uni.showToast({ title: "正在录音...", icon: "none", duration: 30000 });
  });

  recorderManager.value.onStop((res) => {
    console.log("✅ 录音停止");
    console.log("📋 录音结果:", JSON.stringify(res));
    isRecording.value = false;
    uni.hideToast();
    const duration = Date.now() - recordStartTime.value;

    if (duration < 500) {
      uni.showToast({ title: "录音时间太短", icon: "none" });
      return;
    }

    console.log(
      `📁 录音文件: ${res.tempFilePath}, 大小: ${res.fileSize} bytes`,
    );

    handleVoiceToText(res.tempFilePath);
  });

  recorderManager.value.onError((err) => {
    console.error("❌ 录音错误:", err);
    isRecording.value = false;
    uni.hideToast();
    uni.showToast({ title: "录音失败", icon: "none" });
  });
};

// 语音转文字（调用阿里云 ASR）
const handleVoiceToText = async (tempFilePath) => {
  console.log("🎙️ 开始语音识别:", tempFilePath);

  try {
    const text = await speechToText(tempFilePath);
    console.log("✅ 语音识别结果:", text);

    if (!text || !text.trim()) {
      uni.showToast({
        title: "未识别到内容，请重试",
        icon: "none",
        duration: 2000,
      });
      return;
    }

    // 直接发送识别的文本（无需预览）
    question.value = text;
    await sendText();
  } catch (e) {
    console.error("❌ 语音转文字失败:", e);
    uni.showToast({
      title: "语音识别失败: " + (e.message || "未知错误"),
      icon: "none",
      duration: 3000,
    });
    if (e.message && e.message.includes("Token")) {
      clearTokenCache();
    }
  }
};

const startRecord = async (e) => {
  if (isAiThinking) {
    uni.showToast({ title: "AI 正在思考中...", icon: "none" });
    return;
  }

  // 记录开始触摸位置
  if (e && e.touches && e.touches[0]) {
    startTouchX = e.touches[0].clientX;
    startTouchY = e.touches[0].clientY;
  }

  try {
    const hasPermission = await permission.check("scope.record");
    if (!hasPermission) {
      const granted = await permission.request("scope.record", {
        title: "需要麦克风权限",
        content: "语音输入需要使用麦克风录制音频",
        showSetting: true,
      });
      if (!granted) return;
    }

    recorderManager.value.start({
      format: "mp3",
      sampleRate: 8000,
      numberOfChannels: 1,
      encodeBitRate: 48000,
    });

    console.log("✅ 录音参数已设置：MP3, 8kHz");
    uni.vibrateShort();
    uni.showToast({ title: "正在录音...", icon: "none", duration: 10000 });
  } catch (e) {
    console.error("❌ 录音权限检查失败", e);
    uni.showToast({ title: "权限检查失败", icon: "none" });
  }
};

const stopRecord = () => {
  if (isRecording.value) {
    uni.hideToast();
    const duration = Date.now() - recordStartTime.value;
    if (duration < 500) {
      uni.hideToast();
      uni.showToast({
        title: `录音太短（${(duration / 1000).toFixed(1)}秒），请至少 0.5 秒`,
        icon: "none",
        duration: 2500,
      });
      recorderManager.value.stop();
      return;
    }
    if (duration > 30000) {
      uni.hideToast();
      uni.showToast({ title: "录音已达到 30 秒上限", icon: "none" });
    }
    recorderManager.value.stop();
  }
};

const onVoiceTouchMove = (e) => {
  if (!isRecording.value) return;

  const touch = e.touches[0];
  const distance = Math.sqrt(
    Math.pow(touch.clientX - startTouchX, 2) +
      Math.pow(touch.clientY - startTouchY, 2),
  );

  if (distance > 250) {
    console.log("⚠️ 手指移动距离过大，停止录音");
    stopRecord();
  }
};

const onVoiceLongPress = () => {
  console.log("📱 长按事件触发");
};

const removeImage = () => {
  selectedImage.value = null;
  imagePreviewVisible.value = false;
};

const handleImageError = (e) => {
  console.error("图片加载失败", e);
};

// 图片转 Base64（APP 端用 plus.io）
const imageToBase64 = (filePath) => {
  return new Promise((resolve, reject) => {
    // #ifdef APP-PLUS
    plus.io.resolveLocalFileSystemURL(
      filePath,
      (entry) => {
        entry.file(
          (file) => {
            const fileReader = new plus.io.FileReader();
            fileReader.onloadend = (e) => {
              const dataUrl = e.target.result;
              const base64 = dataUrl.split(",")[1]; // 去掉 data:image/xxx;base64, 前缀
              resolve(base64);
            };
            fileReader.onerror = () => reject(new Error("文件读取失败"));
            fileReader.readAsDataURL(file);
          },
          (err) => reject(err),
        );
      },
      (err) => reject(err),
    );
    // #endif
  });
};

const chooseFromAlbum = async () => {
  showPhotoMenu.value = false;
  try {
    const hasPermission = await permission.check("scope.writePhotosAlbum");
    if (!hasPermission) {
      const granted = await permission.request("scope.writePhotosAlbum", {
        title: "需要相册权限",
        content: "选择图片需要访问您的相册",
        showSetting: true,
      });
      if (!granted) return;
    }
    uni.chooseImage({
      count: 1,
      sizeType: ["compressed"], // ✅ 优化：自动压缩图片，提升传输速度
      sourceType: ["album"],
      success: (res) => {
        const filePath = res.tempFilePaths[0];
        // #ifdef APP-PLUS
        plus.io.resolveLocalFileSystemURL(
          filePath,
          (entry) => {
            selectedImage.value = entry.toLocalURL();
            imagePreviewVisible.value = true;
          },
          () => {
            selectedImage.value = filePath;
            imagePreviewVisible.value = true;
          },
        );
        // #endif
        // #ifndef APP-PLUS
        selectedImage.value = filePath;
        imagePreviewVisible.value = true;
        // #endif
      },
      fail: (err) => console.error("选择图片失败", err),
    });
  } catch (e) {
    console.error("相册权限检查失败", e);
  }
};

const takePhoto = async () => {
  showPhotoMenu.value = false;
  try {
    const hasPermission = await permission.check("scope.camera");
    if (!hasPermission) {
      const granted = await permission.request("scope.camera", {
        title: "需要相机权限",
        content: "拍照需要使用您的相机",
        showSetting: true,
      });
      if (!granted) return;
    }
    uni.chooseImage({
      count: 1,
      sizeType: ["compressed"], // ✅ 优化：自动压缩图片
      sourceType: ["camera"],
      success: (res) => {
        const filePath = res.tempFilePaths[0];
        // #ifdef APP-PLUS
        plus.io.resolveLocalFileSystemURL(
          filePath,
          (entry) => {
            selectedImage.value = entry.toLocalURL();
            imagePreviewVisible.value = true;
          },
          () => {
            selectedImage.value = filePath;
            imagePreviewVisible.value = true;
          },
        );
        // #endif
        // #ifndef APP-PLUS
        selectedImage.value = filePath;
        imagePreviewVisible.value = true;
        // #endif
      },
      fail: (err) => console.error("拍照失败", err),
    });
  } catch (e) {
    console.error("相机权限检查失败", e);
  }
};

// 发送消息（纯文字和图文都走 WebSocket Base64）
const sendText = async () => {
  // 1. 基础校验
  if (selectedImage.value && !question.value.trim()) {
    uni.showToast({ title: "请输入文字后再发送", icon: "none" });
    return;
  }
  if (!question.value.trim() && !selectedImage.value) return;
  if (isAiThinking) return;

  isAiThinking = true;

  // 设置思考超时定时器（防止锁永远不释放）
  if (thinkingTimeoutTimer) {
    clearTimeout(thinkingTimeoutTimer);
  }
  thinkingTimeoutTimer = setTimeout(() => {
    if (isAiThinking) {
      console.warn("[发送锁] 思考超时，强制释放锁");
      isAiThinking = false;
    }
    thinkingTimeoutTimer = null;
  }, THINKING_TIMEOUT);

  const userInput = question.value;
  const currentMsgId = "msg_" + Date.now();

  // 2. 立即渲染用户消息到界面
  messages.value.push({
    role: "user",
    content: userInput,
    imageUrl: selectedImage.value,
    id: currentMsgId,
  });

  // ✅ 关键修复：在清空之前，先备份图片路径
  const tempImagePath = selectedImage.value;

  // 3. 清空输入状态
  question.value = "";
  removeImage();
  scrollToBottom();

  // 4. 添加 AI 思考占位符
  const aiMsg = {
    role: "ai",
    content: "",
    id: "ai_" + currentMsgId,
    isThinking: true,
    isWaiting: false, // 等待下一段内容
  };
  messages.value.push(aiMsg);

  // 启动初始等待阶段的思考文本切换定时器
  startInitialThinkingTimers();

  try {
    let imageBase64 = null;

    // 5. 如果有图片（使用备份的路径），转为 Base64
    if (tempImagePath) {
      console.log("✅ 检测到已选图片，开始编码:", tempImagePath);
      uni.showLoading({ title: "图片编码中..." });

      try {
        imageBase64 = await new Promise((resolve, reject) => {
          // #ifdef APP-PLUS
          // 方案 A：直接使用 plus.io 读取（针对某些真机优化）
          plus.io.resolveLocalFileSystemURL(
            tempImagePath,
            (entry) => {
              entry.file(
                (file) => {
                  const reader = new plus.io.FileReader();
                  reader.onloadend = (e) => {
                    const result = e.target.result.split(",")[1];
                    resolve(result);
                  };
                  reader.onerror = (err) => reject(err);
                  reader.readAsDataURL(file);
                },
                (err) => reject(err),
              );
            },
            (err) => {
              // 方案 B：如果方案 A 失败，尝试使用 uni 的原生文件管理器（兼容性最好）
              const fs = uni.getFileSystemManager();
              fs.readFile({
                filePath: tempImagePath,
                encoding: "base64",
                success: (res) => {
                  resolve(res.data);
                },
                fail: (err) => {
                  reject(err);
                },
              });
            },
          );
          // #endif
        });
      } catch (e) {
        console.error("图片编码失败", e);
        uni.hideLoading();
        uni.showToast({ title: "图片处理失败", icon: "none" });
        isAiThinking = false;
        messages.value.pop();
        messages.value.pop();
        return;
      }
      uni.hideLoading();
    } else {
      console.log("⚠️ 未选择图片，直接发送文字");
    }

    // 在 try 块内部，构造 payloadObj 之前加这一句：
    // 6. 构造 JSON 负载并发送 (后端推荐方案)
    // 如果用户在游览中，把游玩列表的最后一个景点传给后端
    let currentAttraction = null;
    if (isGuiding.value && visitedSpots.value.length > 0) {
      currentAttraction =
        visitedSpots.value[visitedSpots.value.length - 1]?.spotName || null;
    }

    const payloadObj = {
      msgId: currentMsgId,
      userId: userId.value,
      sessionId: sessionId.value,
      message: userInput,
      imageBase64: imageBase64 || null, // 如果没有图片，传 null 即可
      currentAttraction: currentAttraction,
    };

    // 将对象转为 JSON 字符串
    const payloadJson = JSON.stringify(payloadObj);

    wsUtil.send(payloadJson);
  } catch (e) {
    console.error("发送流程异常", e);
    uni.showToast({ title: "发送失败", icon: "none" });
    isAiThinking = false;
    messages.value.pop(); // ✅ 修复：移除 AI 占位符
    messages.value.pop(); // ✅ 修复：移除用户消息
  }
};

// 手动点击播报按钮 - 直接发送完整内容给数字人
const playVoice = (content, index) => {
  // 如果当前正在播报这条消息，点击后取消播报
  if (currentSpeakingIndex.value === index) {
    console.log("[手动播报] 取消播报");
    resetDigitalHuman();
    currentSpeakingIndex.value = -1;
    uni.showToast({ title: "已取消播报", icon: "none" });
    return;
  }

  console.log("[手动播报] 点击播报按钮，内容:", content);

  if (!digitalHumanWebview.value) {
    console.error("[手动播报] ❌ 数字人webview不存在");
    uni.showToast({ title: "数字人未就绪", icon: "none" });
    return;
  }

  // 过滤特殊字符后发送（避免语法错误）
  const pureText = filterPureText(content);
  console.log("[手动播报] 过滤后内容:", pureText);

  if (!pureText) {
    console.log("[手动播报] 过滤后无内容");
    uni.showToast({ title: "无播报内容", icon: "none" });
    return;
  }

  // 如果正在播报其他消息，先取消
  if (currentSpeakingIndex.value !== -1) {
    resetDigitalHuman();
  }

  // 直接发送完整内容给数字人，让讯飞SDK自己处理分段和播放
  try {
    const escapedText = JSON.stringify(pureText);
    digitalHumanWebview.value.evalJS(`window.sendTextInteract(${escapedText})`);
    currentSpeakingIndex.value = index; // 记录当前播报的消息索引
    uni.showToast({ title: "开始播报", icon: "none" });
    console.log("[手动播报] ✅ 完整内容已发送给数字人");
  } catch (e) {
    console.error("[手动播报] ❌ 发送失败:", e);
    uni.showToast({ title: "播报失败", icon: "none" });
  }
};

const collectRoute = async (chatId, index) => {
  console.log("========== 收藏/取消收藏路线开始 ==========");
  console.log("🚀 collectRoute 函数被调用！");
  console.log("📥 参数 chatId:", chatId, "类型:", typeof chatId);
  console.log("📥 参数 index:", index, "类型:", typeof index);
  console.log(
    "📊 当前收藏状态:",
    getChatIdStatus(chatId) ? "已收藏" : "未收藏",
  );

  // 检查 messages 数组
  console.log("📋 messages 数组长度:", messages.value.length);
  if (index !== undefined && messages.value[index]) {
    console.log(
      "📍 当前消息项:",
      JSON.stringify(messages.value[index], null, 2),
    );
  }

  if (!chatId) {
    console.log("❌ 操作失败：chatId 为空");
    uni.showToast({ title: "参数错误", icon: "none" });
    return;
  }

  // 获取当前收藏状态
  const currentStatus = getChatIdStatus(chatId);

  // ========== 乐观更新：只更新UI ==========
  const newStatus = !currentStatus;
  if (index !== undefined && messages.value[index]) {
    messages.value[index].isCollected = newStatus;
  }

  console.log("UI已更新，开始发送请求...");

  // ========== 异步发送请求 ==========
  try {
    if (currentStatus) {
      // 当前已收藏，执行取消收藏
      const routeId = getChatIdRouteId(chatId);
      console.log("🔄 取消收藏 - routeId:", routeId);

      let url;
      if (routeId) {
        url = `/user/route/collect/${routeId}?userId=${userId.value}`;
      } else {
        // 如果没有 routeId，尝试用 chatId 取消收藏
        console.log("⚠️ routeId 为空，尝试使用 chatId 取消收藏");
        url = `/user/route/collect/cancel?userId=${userId.value}&chatId=${Number(chatId)}`;
      }

      const res = await request({ url: url, method: "DELETE" });

      if (res.code !== 200) {
        console.log("❌ 取消收藏接口失败:", res.message);
        if (index !== undefined && messages.value[index]) {
          messages.value[index].isCollected = currentStatus;
        }
        uni.showToast({ title: "取消收藏失败", icon: "none" });
      } else {
        console.log("✅ 取消收藏成功");
        updateChatIdStatus(chatId, false);
        // 清空 routeId
        updateChatIdRouteId(chatId, null);
      }
    } else {
      // 当前未收藏，执行收藏
      updateChatIdRouteId(chatId, null);
      const url = `/user/route/collect?userId=${userId.value}&chatId=${Number(chatId)}`;
      const res = await request({ url: url, method: "POST" });

      if (res.code === 200 && res.data) {
        const routeId = res.data.routeId;
        console.log("收藏成功，routeId:", routeId);
        // 先保存 chatId 到列表（确保记录存在）
        saveCollectedChatId(chatId);
        // 再更新 routeId
        if (routeId) {
          updateChatIdRouteId(chatId, routeId);
        }
        // 最后更新收藏状态
        updateChatIdStatus(chatId, true);
      } else {
        console.log("收藏接口失败:", res.message);
        if (index !== undefined && messages.value[index]) {
          messages.value[index].isCollected = currentStatus;
        }
        uni.showToast({ title: "收藏失败", icon: "none" });
      }
    }
  } catch (e) {
    console.error(
      `${currentStatus ? "取消收藏" : "收藏"}请求异常:`,
      e.errMsg || e.message,
    );
    if (index !== undefined && messages.value[index]) {
      messages.value[index].isCollected = currentStatus;
    }
    uni.showToast({ title: "网络错误，请重试", icon: "none" });
  }
  console.log("========== 收藏/取消收藏路线结束 ==========");
};

const fetchHotQuestions = async () => {
  try {
    const res = await request.get("/user/ai/hot-questions");
    if (res.code === 200 && res.data) {
      allHotQuestions.value = res.data;
      randomShowThree();
    }
  } catch (e) {}
};

const randomShowThree = () => {
  const arr = [...allHotQuestions.value];
  const result = [];
  for (let i = 0; i < 3 && arr.length; i++) {
    const idx = Math.floor(Math.random() * arr.length);
    result.push(arr.splice(idx, 1)[0]);
  }
  hotQuestions.value = result;
};

const sendHotQuestion = (item) => {
  question.value = item.question;
  sendText();
};

const toggleInputMode = () => {
  isVoiceMode.value = !isVoiceMode.value;
  showPhotoMenu.value = false;
};

const togglePhotoMenu = () => {
  if (isVoiceMode.value) return;
  showPhotoMenu.value = !showPhotoMenu.value;
};

const handleKeyboardHeightChange = (e) => {
  if (e && e.detail && typeof e.detail.height === "number") {
    keyboardHeight.value = e.detail.height;
  }
};

const handleInputFocus = () => {
  showPhotoMenu.value = false;
};

// 启动初始等待阶段的思考文本切换定时器
const startInitialThinkingTimers = () => {
  // 先清除之前的定时器
  clearInitialThinkingTimers();

  // 重置索引为0，显示"..."
  currentThinkingIndex.value = 0;

  // 设置定时器，每4秒切换一次文本
  // 4秒后切换到"思考中..."
  initialThinkingTimers.push(
    setTimeout(() => {
      if (isAiThinking && currentThinkingIndex.value === 0) {
        currentThinkingIndex.value = 1;
        console.log("[思考阶段] 切换到: 思考中...");
      }
    }, 4000),
  );

  // 8秒后切换到"资料查询中..."
  initialThinkingTimers.push(
    setTimeout(() => {
      if (isAiThinking && currentThinkingIndex.value === 1) {
        currentThinkingIndex.value = 2;
        console.log("[思考阶段] 切换到: 资料查询中...");
      }
    }, 8000),
  );

  // 12秒后切换到"回答生成中..."
  initialThinkingTimers.push(
    setTimeout(() => {
      if (isAiThinking && currentThinkingIndex.value === 2) {
        currentThinkingIndex.value = 3;
        console.log("[思考阶段] 切换到: 回答生成中...");
      }
    }, 12000),
  );

  console.log("[思考阶段] 启动初始等待阶段定时器，当前显示: ...");
};

// 清除初始等待阶段的定时器
const clearInitialThinkingTimers = () => {
  initialThinkingTimers.forEach((timer) => clearTimeout(timer));
  initialThinkingTimers = [];
  console.log("[思考阶段] 清除初始等待阶段定时器");
};

// 切换流式播报开关
const toggleStreamSpeak = () => {
  const newState = !isStreamSpeakEnabled.value;
  isStreamSpeakEnabled.value = newState;

  // 持久化存储
  storage.setStreamSpeakEnabled(newState);

  // 如果关闭播报且当前正在播报，重置虚拟人（恢复静默状态，不断开连接）
  if (!newState && isSpeaking) {
    console.log("[流式播报] 开关关闭，重置虚拟人");
    resetDigitalHuman();
  }

  console.log("[流式播报] 开关状态:", newState ? "开启" : "关闭");
};

// 重置虚拟人（恢复静默推流状态）
const resetDigitalHuman = () => {
  if (digitalHumanWebview.value) {
    try {
      digitalHumanWebview.value.evalJS(
        "window.resetAvatar && window.resetAvatar()",
      );
      console.log("[数字人] 已发送重置指令");
    } catch (e) {
      console.error("[数字人] 重置失败:", e);
    }
  }
  isSpeaking = false;
  currentSpeakingIndex.value = -1; // 清除手动播报状态
};

// 点击页面空白处关闭拍照菜单（兼容 routeRecommendation 的实现）

onMounted(() => {
  initSession();
  loadCollectedChatIds(); // 先加载收藏列表
  loadChatHistory();
  fetchHotQuestions();
  scrollToBottom();
  initRecorder();
  checkGuidingStatus();

  // 加载流式播报开关状态
  isStreamSpeakEnabled.value = storage.getStreamSpeakEnabled();
  console.log("[流式播报] 加载开关状态:", isStreamSpeakEnabled.value);

  // 创建数字人 webview（内部已设置为隐藏状态，由onShow控制显示）
  createDigitalHumanWebview();

  // 监听键盘高度变化
  uni.onKeyboardHeightChange(handleKeyboardHeightChange);

  // 处理 URL 参数中的问题
  const pages = getCurrentPages();
  const currentPage = pages[pages.length - 1];
  const options = currentPage.$page?.options || currentPage.options || {};
  if (options.question) {
    const decodedQuestion = decodeURIComponent(options.question);
    console.log("📥 从 URL 获取到问题:", decodedQuestion);
    question.value = decodedQuestion;
    // 延迟发送，确保页面初始化完成
    setTimeout(() => {
      sendText();
    }, 500);
  }

  wsUtil.connect(userId.value, sessionId.value);

  wsUtil.onMessage((rawData) => {
    let cleanContent = rawData;
    let extractedChatId = null;
    let hasRouteRecommendation = false;
    const isError =
      cleanContent.includes("[ERROR]") || cleanContent.includes("error");
    const isDone = cleanContent.includes("[DONE]");

    // 等待下一段内容逻辑
    if (isDone) {
      // 流结束，清除定时器和等待状态
      if (waitingTimer) {
        clearTimeout(waitingTimer);
        waitingTimer = null;
      }
      const lastAiMsg = messages.value.find(
        (m) => m.role === "ai" && m.id && m.id.startsWith("ai_"),
      );
      if (lastAiMsg) {
        lastAiMsg.isWaiting = false;
        isFirstWaiting = false; // 流结束，重置第一次等待标记
      }
      console.log("[WebSocket] 流结束，清除等待状态");
    }
    // 收到data时清除之前的定时器（在渲染成功后会重新启动）

    console.log("收到原始消息:", rawData);

    // 检测 CHAT_ID 标签（匹配多种可能格式）
    const chatIdRegex = /\[?\[?CHAT_ID\]?\]:?(\d+)/gi;
    const match = cleanContent.match(chatIdRegex);
    if (match) {
      // 从匹配结果中提取数字
      const numMatch = match[0].match(/(\d+)/);
      if (numMatch) {
        extractedChatId = numMatch[1];
        cleanContent = cleanContent.replace(chatIdRegex, "").trim();
        console.log("已过滤 CHAT_ID 标签，提取到 chatId:", extractedChatId);
        // 只要提取到 chatId 就保存，不管是否有 ROUTE_RECOMMENDATION 标签
        saveCollectedChatId(extractedChatId);
      }
    }

    console.log("过滤后内容:", cleanContent);

    if (isError) {
      if (thinkingTimeoutTimer) {
        clearTimeout(thinkingTimeoutTimer);
        thinkingTimeoutTimer = null;
      }
      isAiThinking = false;
      const lastAiMsg = messages.value.filter((m) => m.role === "ai").pop();
      if (lastAiMsg) {
        lastAiMsg.isThinking = false;
        lastAiMsg.content = "抱歉，AI 服务暂时不可用，请稍后再试。";
      }
      uni.showToast({ title: "AI 回复失败", icon: "none", duration: 2000 });
      return;
    }
    if (isDone) {
      if (thinkingTimeoutTimer) {
        clearTimeout(thinkingTimeoutTimer);
        thinkingTimeoutTimer = null;
      }
      isAiThinking = false;
      const lastAiMsg = messages.value.filter((m) => m.role === "ai").pop();
      if (lastAiMsg) {
        // 设置 chatId（已在前面保存过了）
        if (extractedChatId) {
          lastAiMsg.chatId = extractedChatId;
        }
        lastAiMsg.isThinking = false;
      }
      // 流式播报结束，发送剩余内容
      finishStreamSpeak();
      return;
    }
    const delimiterIndex = cleanContent.indexOf("|");
    if (delimiterIndex !== -1) {
      const msgId = cleanContent
        .substring(0, delimiterIndex)
        .replace("msgId=", "")
        .trim();
      let content = cleanContent.substring(delimiterIndex + 1);

      let filteredContent = "";
      for (let i = 0; i < content.length; i++) {
        const char = content[i];
        if (isFilteringTag) {
          if (
            char === "]" &&
            i + 1 < content.length &&
            content[i + 1] === "]"
          ) {
            isFilteringTag = false;
            i++;
          }
        } else if (isPendingBracket) {
          if (char === "[") {
            isFilteringTag = true;
          } else {
            filteredContent += "[" + char;
          }
          isPendingBracket = false;
        } else {
          if (char === "[") {
            isPendingBracket = true;
          } else {
            filteredContent += char;
          }
        }
      }
      if (isPendingBracket) {
        filteredContent += "[";
        isPendingBracket = false;
      }

      if (filteredContent || extractedChatId) {
        console.log("[WebSocket] 解析到消息:", {
          msgId,
          content: filteredContent,
          chatId: extractedChatId,
        });

        // 收到第一个data，清除初始等待阶段的定时器
        if (filteredContent) {
          clearInitialThinkingTimers();
        }

        const targetMsg = messages.value.find((m) => m.id === "ai_" + msgId);
        if (targetMsg) {
          const newMessages = messages.value.map((msg) => {
            if (msg.id === "ai_" + msgId) {
              // 正常追加内容
              return {
                ...msg,
                isThinking: false,
                isWaiting: false,
                content: msg.content + (filteredContent || ""),
                chatId: extractedChatId || msg.chatId,
              };
            }
            return msg;
          });
          messages.value = newMessages;

          if (filteredContent) {
            console.log("[WebSocket] 更新UI内容，准备播报");

            // 更新成功后，启动等待定时器（500ms内没有新data就显示思考中）
            if (waitingTimer) {
              clearTimeout(waitingTimer);
              waitingTimer = null;
            }
            const currentMsg = newMessages.find((m) => m.id === "ai_" + msgId);
            if (currentMsg && currentMsg.content && !currentMsg.isWaiting) {
              waitingTimer = setTimeout(() => {
                const msg = messages.value.find((m) => m.id === currentMsg.id);
                if (msg && msg.content && isAiThinking) {
                  msg.isWaiting = true;
                  console.log("[WebSocket] 显示等待中UI（等待下一段内容）");
                }
                waitingTimer = null;
              }, WAITING_DELAY);
            }

            if (isStreamSpeakEnabled.value) {
              processStreamText(filteredContent);
            } else {
              console.log("[WebSocket] 流式播报已关闭，跳过播报");
            }
            scrollToBottom();
          }
        } else {
          console.warn("[WebSocket] 未找到对应消息:", "ai_" + msgId);
        }
      } else {
        console.log("[WebSocket] 过滤后无内容，跳过");
      }
    }
  });
});

// 页面显示时刷新收藏状态
onShow(() => {
  console.log("========== 页面显示，刷新收藏状态 ==========");
  loadCollectedChatIds();
  // 更新消息列表中的收藏状态
  messages.value.forEach((msg) => {
    if (msg.chatId) {
      msg.isCollected = getChatIdStatus(msg.chatId);
    }
  });

  // 加载流式播报开关状态
  isStreamSpeakEnabled.value = storage.getStreamSpeakEnabled();
  console.log("[流式播报] 加载开关状态:", isStreamSpeakEnabled.value);

  // 先隐藏数字人（确保焦点释放），再延迟显示
  const digitalHuman = plus.webview.getWebviewById("preloadDigitalHuman");
  console.log("[aiQuestion] onShow: 获取数字人webview:", !!digitalHuman);

  if (digitalHuman) {
    console.log("[aiQuestion] 页面显示，先隐藏数字人确保键盘能弹出");
    digitalHuman.hide();

    setTimeout(() => {
      // 如果键盘没有弹出（keyboardHeight为0或未初始化），显示数字人
      console.log(
        "[aiQuestion] 检查是否显示数字人，keyboardHeight:",
        keyboardHeight.value,
      );
      if (!keyboardHeight.value || keyboardHeight.value === 0) {
        console.log("[aiQuestion] 延迟显示数字人");
        // 使用 setStyle 确保显示，避免被之前的 setStyle({show: false}) 覆盖
        digitalHuman.setStyle({ show: true });
        digitalHuman.show();
        console.log("[aiQuestion] 数字人已显示");
      } else {
        console.log("[aiQuestion] 键盘已弹出，不显示数字人");
      }
    }, 600); // 增加延迟，确保 createDigitalHumanWebview 已完成
  } else {
    console.log("[aiQuestion] 未找到数字人webview");
  }
});

onHide(() => {
  // 退出页面时重置虚拟人，终止所有播报，但数字人保持存在
  console.log("[aiQuestion] 页面隐藏，重置虚拟人终止播报");
  resetDigitalHuman();
});

onUnmounted(() => {
  if (recorderManager.value) recorderManager.value.stop();
  // 清除定时器和锁
  if (thinkingTimeoutTimer) {
    clearTimeout(thinkingTimeoutTimer);
    thinkingTimeoutTimer = null;
  }
  isAiThinking = false;
  // 页面卸载时重置虚拟人并销毁webview
  resetDigitalHuman();
  destroyDigitalHumanWebview();
  uni.offKeyboardHeightChange(handleKeyboardHeightChange);
});
</script>

<template>
  <view class="chat-page">
    <!-- 自定义导航栏 -->
    <view class="custom-nav-bar">
      <view class="nav-content">
        <text class="nav-title">智能问答</text>
        <!-- 流式播报开关 -->
        <view class="stream-speak-toggle" @click="toggleStreamSpeak">
          <uni-icons
            :type="isStreamSpeakEnabled ? 'sound' : 'sound'"
            :size="24"
            :color="isStreamSpeakEnabled ? '#b38c4f' : '#999'"
          />
        </view>
      </view>
    </view>

    <view class="chat-header">
      <!-- 数字人占位区域（数字人 webview 会渲染在这里） -->
      <view class="avatar-section"></view>
      <view class="header-right">
        <view class="hot-title">
          <text>猜您想问</text>
          <uni-icons
            type="refresh"
            size="20"
            color="#b38c4f"
            @click="randomShowThree"
          />
        </view>
        <view class="hot-list">
          <view
            class="hot-item"
            v-for="(item, index) in hotQuestions"
            :key="index"
            @tap="sendHotQuestion(item)"
          >
            <text>{{ item.question }}</text>
            <uni-icons type="right" size="16" color="#b38c4f" />
          </view>
        </view>
      </view>
    </view>

    <!-- 消息内容区域 -->
    <scroll-view
      class="chat-content"
      scroll-y
      :scroll-top="scrollTopValue"
      enhanced
      show-scrollbar
      :style="{ paddingBottom: `${keyboardHeight + 60}rpx` }"
    >
      <view
        class="message-item"
        :id="`msg-${index}`"
        v-for="(item, index) in messages"
        :key="index"
      >
        <view v-if="item.role === 'ai'" class="ai-message">
          <image class="avatar avatar-round" :src="digitalHumanAvatar" />
          <view class="bubble ai-bubble">
            <view v-if="item.isThinking" class="thinking-container">
              <text class="thinking-text">{{
                thinkingTexts[currentThinkingIndex]
              }}</text>
              <view class="thinking-dots"
                ><view class="dot"></view><view class="dot"></view
                ><view class="dot"></view
              ></view>
            </view>
            <!-- 主要回答内容 -->
            <view class="content-wrapper">
              <text>{{ item.content }}</text>
              <!-- 等待下一段内容时显示 -->
              <view v-if="item.isWaiting" class="waiting-inline">
                <text class="waiting-text">思考中</text>
                <view class="waiting-dots">
                  <view class="dot"></view>
                  <view class="dot"></view>
                  <view class="dot"></view>
                </view>
              </view>
            </view>
            <view
              v-if="!item.isThinking && item.content && !item.isWaiting"
              class="tool-row"
            >
              <view class="tool-btn" @click="playVoice(item.content, index)">
                <uni-icons type="sound" size="20" color="#b38c4f" /><text>{{
                  currentSpeakingIndex === index ? "取消播报" : "播报"
                }}</text>
              </view>
              <view
                v-if="item.chatId"
                class="tool-btn"
                @click="collectRoute(item.chatId, index)"
              >
                <uni-icons
                  :type="item.isCollected ? 'star-filled' : 'star'"
                  size="20"
                  :color="item.isCollected ? '#FFD700' : '#b38c4f'"
                /><text>{{ item.isCollected ? "已收藏" : "收藏" }}</text>
              </view>
            </view>
          </view>
        </view>

        <view v-else class="user-message">
          <view class="bubble user-bubble">
            <!-- 图片在上，文字在下，分开渲染 -->
            <image
              v-if="item.imageUrl"
              :src="item.imageUrl"
              mode="aspectFill"
              class="message-image"
            />
            <text v-if="item.content" class="message-text">{{
              item.content
            }}</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <view
      class="input-bar"
      :style="{ transform: `translateY(-${keyboardHeight}px)` }"
    >
      <!-- 图片预览区域（发送后自动删除） -->
      <view v-if="imagePreviewVisible && selectedImage" class="image-preview">
        <image
          :src="selectedImage"
          mode="aspectFill"
          class="preview-img"
          @error="handleImageError"
        />
        <view class="remove-btn" @click="removeImage">
          <uni-icons type="closeempty" size="16" color="#fff" />
        </view>
      </view>

      <view class="input-box">
        <view v-if="!isVoiceMode" class="photo-icon-wrapper">
          <view class="photo-menu" v-if="showPhotoMenu">
            <view class="menu-item" @click="chooseFromAlbum">
              <uni-icons type="image" size="20" color="#b38c4f" /><text
                >相册</text
              >
            </view>
            <view class="menu-item" @click="takePhoto">
              <uni-icons type="camera" size="20" color="#b38c4f" /><text
                >拍照</text
              >
            </view>
          </view>
          <view class="icon-left" @click="togglePhotoMenu">
            <uni-icons type="camera" size="24" color="#b38c4f" />
          </view>
        </view>

        <textarea
          v-if="!isVoiceMode"
          class="input input-textarea"
          v-model="question"
          placeholder="消息内容..."
          auto-height
          :max-height="200"
          :show-confirm-bar="false"
          @focus="handleInputFocus"
        ></textarea>

        <view
          v-else
          class="voice-input-box"
          @touchstart="startRecord"
          @touchend="stopRecord"
          @touchcancel="stopRecord"
          @touchmove="onVoiceTouchMove"
        >
          <text v-if="!isRecording">长按语音</text>
          <text v-else class="recording-text">松开结束</text>
        </view>

        <view class="icon-right-group">
          <view
            v-if="question?.trim() || selectedImage"
            class="send-icon"
            @click="sendText"
          >
            <uni-icons type="arrow-up" size="24" color="#fff" />
          </view>
          <view v-else class="icon-right" @click="toggleInputMode">
            <uni-icons
              v-if="!isVoiceMode"
              type="mic"
              size="24"
              color="#b38c4f"
            />
            <uni-icons v-else type="compose" size="24" color="#b38c4f" />
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
$gold: #b38c4f;
$gold-light: rgba(197, 166, 118, 0.1);
$bg-color: #fdfaf2;

.chat-page {
  width: 100vw;
  height: 100vh;
  background: $bg-color;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.custom-nav-bar {
  width: 100%;
  background: #f8f8f8;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 999;
  border-bottom: 1rpx solid #e8e8e8;
  flex-shrink: 0;
}

.nav-content {
  width: 100%;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: var(--status-bar-height, 44px);
  position: relative;
}

.nav-title {
  font-size: 34rpx;
  font-weight: 400;
  color: #333333;
}

.stream-speak-toggle {
  position: absolute;
  right: 30rpx;
  top: 100rpx;
  display: flex;
  width: 60rpx;
  height: 60rpx;
}

.mute-overlay {
  position: absolute;
  width: 70rpx;
  height: 4rpx;
  background: #999;
  border-radius: 2rpx;
  transform: rotate(-45deg);
}
.chat-header {
  position: fixed;
  top: calc(var(--status-bar-height, 44px) + 88rpx);
  left: 0;
  right: 0;
  height: 250rpx;
  padding: 20rpx 30rpx;
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1rpx solid $gold-light;
  z-index: 10;
}
.avatar-section {
  width: 140rpx;
  height: 200rpx;
}
.header-right {
  flex: 1;
  margin-left: 50rpx;
}
.hot-title {
  margin-bottom: 10rpx;
  display: flex;
  justify-content: space-between;
  font-size: 28rpx;
  font-weight: 600;
  color: #333333;
}
.hot-list {
  display: flex;
  flex-direction: column;
}
.hot-item {
  padding: 10rpx 14rpx;
  background: #fff;
  border: 1rpx solid $gold-light;
  border-radius: 16rpx;
  margin-bottom: 6rpx;
  display: flex;
  justify-content: space-between;
  font-size: 24rpx;
}
.chat-content {
  height: 1095rpx;
  padding: 0 16rpx 16rpx;
  margin-top: calc(var(--status-bar-height, 44px) + 380rpx);
  box-sizing: border-box;
  overflow-y: auto;
  border: 1rpx solid $gold-light;
}
.message-item {
  margin-bottom: 28rpx;
}
.ai-message,
.user-message {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
}
.ai-message {
  justify-content: flex-start;
}
.user-message {
  justify-content: flex-end;
}
.avatar {
  width: 80rpx;
  height: 80rpx;
  flex-shrink: 0;
}
.avatar-round {
  border-radius: 50%;
}
.bubble {
  max-width: 60%;
  padding: 22rpx 26rpx;
  border-radius: 26rpx;
  font-size: 28rpx;
  line-height: 1.5;
  word-break: break-all;
  display: flex;
  flex-direction: column;
}
.ai-bubble {
  background: #fff;
  border: 1rpx solid $gold-light;
}
.user-bubble {
  background: $gold;
  color: #fff;
}
.message-image {
  width: 200rpx;
  height: 200rpx;
  border-radius: 10rpx;
  margin-bottom: 10rpx;
}
.message-text {
  margin-top: 8rpx;
}
.tool-row {
  display: flex;
  gap: 24rpx;
  margin-top: 12rpx;
}
.tool-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  font-size: 24rpx;
  color: #b38c4f;
}
.thinking-container {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.thinking-text {
  color: #999;
  font-size: 28rpx;
}
.thinking-dots {
  display: flex;
  gap: 6rpx;
}
.dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: $gold;
  animation: thinking 1.4s infinite ease-in-out both;
}
.dot:nth-child(1) {
  animation-delay: -0.32s;
}
.dot:nth-child(2) {
  animation-delay: -0.16s;
}
@keyframes thinking {
  0%,
  80%,
  100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

// 内联等待中样式（在消息内容末尾显示）
.content-wrapper {
  display: inline;
  white-space: pre-wrap;
}
.waiting-inline {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  margin-left: 8rpx;
}
.waiting-text {
  color: #666;
  font-size: 28rpx;
}
.waiting-dots {
  display: inline-flex;
  gap: 4rpx;
  .dot {
    width: 8rpx;
    height: 8rpx;
    background: #666;
    border-radius: 50%;
    animation: thinking 1.4s infinite ease-in-out both;
    &:nth-child(1) {
      animation-delay: -0.32s;
    }
    &:nth-child(2) {
      animation-delay: -0.16s;
    }
  }
}

.input-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  margin-bottom: 10rpx;
  margin-left: 8rpx;
  width: 92%;
  background: #fff;
  padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid $gold-light;
  z-index: 100;
  will-change: transform;
}
.image-preview {
  position: relative;
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 10rpx;
  border-radius: 10rpx;
  overflow: hidden;
  .preview-img {
    width: 100%;
    height: 100%;
  }
  .remove-btn {
    position: absolute;
    top: 0;
    right: 0;
    width: 40rpx;
    height: 40rpx;
    background: rgba(0, 0, 0, 0.5);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
.input-box {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.photo-icon-wrapper {
  position: relative;
}
.icon-left {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: $gold-light;
}
.photo-menu {
  position: absolute;
  bottom: 90rpx;
  left: 0;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.15);
  padding: 10rpx 0;
  z-index: 100;
  min-width: 160rpx;
}
.menu-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 20rpx 24rpx;
  font-size: 28rpx;
  color: #333;
  &:active {
    background: $gold-light;
  }
}
.input {
  flex: 1;
  background: $gold-light;
  border-radius: 24rpx;
  padding: 20rpx 24rpx;
  font-size: 28rpx;
  border: none;
  pointer-events: auto;
  -webkit-user-select: text;
  user-select: text;
  cursor: text;
}
.input-textarea {
  max-height: 100rpx;
  overflow-y: auto;
  pointer-events: auto;
  -webkit-user-select: text;
  user-select: text;
}
.voice-input-box {
  flex: 1;
  background: $gold;
  color: #fff;
  text-align: center;
  border-radius: 24rpx;
  padding: 20rpx 24rpx;
  font-size: 28rpx;
  user-select: none;
}
.recording-text {
  animation: pulse 1s infinite;
}
@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}
.icon-right-group {
  display: flex;
  align-items: center;
}
.icon-right,
.send-icon {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.icon-right {
  background: $gold-light;
}
.send-icon {
  background: $gold;
}
</style>
