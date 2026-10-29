<template>
  <view class="container">
    <!-- 自定义导航栏（完全复刻原生） -->
    <view class="custom-nav-bar">
      <view class="nav-content">
        <text class="nav-title">智能导览</text>
      </view>
    </view>

    <!-- 顶部模块（占页面22%） -->
    <view class="top-section">
      <!-- 数字人容器占位 -->
      <view class="avatar-section" @click="handleAvatarClick"></view>

      <!-- 中间：文字渲染框 -->
      <view class="info-section" @click="handleTextboxClick">
        <scroll-view class="info-scroll" scroll-y>
          <text class="info-text">{{ displayText }}</text>
        </scroll-view>
      </view>
    </view>

    <!-- 底部模块（占页面80%） -->
    <view class="bottom-section">
      <!-- 路线按钮（浮在地图上方，右上角） -->
      <view class="route-buttons">
        <view class="route-btn recommend-btn" @click="goToRecommend">
          <uni-icons type="navigate" size="28" color="#b38c4f" />
          <text class="route-btn-text">推荐路线</text>
        </view>
        <view class="route-btn collect-btn" @click="goToCollection">
          <uni-icons type="star" size="24" color="#b38c4f" />
          <text class="route-btn-text">收藏路线</text>
        </view>
      </view>

      <!-- 测试路线按钮 -->
      <view class="top-right-btn" @click="testRoute">
        <uni-icons type="location" size="20" color="#b38c4f" />
        <text class="btn-label">测试路线</text>
      </view>

      <!-- 获取位置/开始导览按钮 -->
      <view class="bottom-right-btn" @click="toggleGuide">
        <text class="loc-icon" :class="{ active: isGuiding }">📍</text>
        <text class="loc-text">{{ isGuiding ? "取消导览" : "获取位置" }}</text>
      </view>

      <!-- 地图缩放控制 -->
      <view class="zoom-controls">
        <view class="zoom-btn" @click="zoomIn">+</view>
        <view class="zoom-btn" @click="zoomOut">-</view>
      </view>

      <!-- 模拟走路按钮（测试用） -->
      <view class="walk-sim-btn" @click="simulateWalk" v-if="isGuiding">
        <text class="walk-text">模拟走一步</text>
      </view>

      <!-- 调试按钮 -->
      <view class="debug-btn" @click="toggleDebug">
        <text class="debug-icon">🔧</text>
        <text class="debug-text">调试</text>
      </view>

      <!-- 地图容器 -->
      <view
        class="map-container"
        @touchstart="onTouchStart"
        @touchmove="onTouchMove"
        @touchend="onTouchEnd"
        @click="onMapClick"
      >
        <view class="map-content">
          <view class="map-inner" :style="getMapInnerStyle()">
            <image
              src="/static/LingShan_map.jpg"
              class="hand-drawn-map"
              mode="aspectFill"
            ></image>

            <!-- 路线Canvas -->
            <canvas
              canvas-id="routeCanvas"
              class="route-canvas"
              :width="canvasWidth"
              :height="canvasHeight"
              :style="{
                width: canvasWidth + 'px',
                height: canvasHeight + 'px',
              }"
            ></canvas>

            <!-- 景点标记 -->
            <view class="map-overlay">
              <view
                v-for="(spot, index) in SPOTS"
                :key="index"
                class="spot-marker"
                :class="{ 'reverse-marker': isReverseMarker(spot.name) }"
                :style="getMarkerStyle(spot)"
                @click="onMarkerTap(index)"
                @longpress="onMarkerLongPress(index)"
              >
                <view class="marker-pin"></view>
                <view class="marker-label">{{ spot.name }}</view>
              </view>
            </view>

            <!-- 用户位置标记 -->
            <view
              v-if="isGuiding"
              class="user-marker"
              :style="getUserMarkerStyle()"
            >
              <view
                class="user-arrow"
                :style="{
                  transform: `rotate(${currentHeading - 180}deg)`,
                }"
              >
                <view class="user-pulse"></view>
                <view class="user-dot"></view>
                <view class="user-direction"></view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 景点详情弹窗 -->
    <view v-if="showSpotModal" class="modal-mask" @click="closeSpotModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">{{ currentSpotDetail.name }}</text>
          <view class="modal-close" @click="closeSpotModal">✕</view>
        </view>
        <scroll-view scroll-y class="modal-body">
          <image
            v-if="currentSpotDetail.image"
            :src="currentSpotDetail.image"
            class="modal-image"
            mode="aspectFit"
          ></image>
          <text class="modal-desc">{{ currentSpotDetail.description }}</text>
        </scroll-view>
      </view>
    </view>

    <!-- 调试面板 -->
    <view v-if="showDebugPanel" class="debug-panel-mask" @click="toggleDebug">
      <view class="debug-panel" @click.stop>
        <view class="debug-header">
          <text class="debug-title">参数调试</text>
          <view class="debug-close" @click="toggleDebug">✕</view>
        </view>
        <scroll-view scroll-y class="debug-body">
          <view class="debug-item">
            <view class="debug-label">旋转角度</view>
            <view class="debug-value">{{ rotationValue }}°</view>
            <slider
              :value="rotationValue"
              @change="
                (e) => {
                  rotationValue = e.detail.value;
                  updateTransform();
                }
              "
              min="0"
              max="90"
              activeColor="#b38c4f"
            />
          </view>
          <view class="debug-item">
            <view class="debug-label">缩放X</view>
            <view class="debug-value">{{ scaleXValue.toFixed(2) }}</view>
            <slider
              :value="scaleXValue * 10"
              @change="
                (e) => {
                  scaleXValue = e.detail.value / 10;
                  updateTransform();
                }
              "
              min="1"
              max="5"
              activeColor="#b38c4f"
            />
          </view>
          <view class="debug-item">
            <view class="debug-label">缩放Y</view>
            <view class="debug-value">{{ scaleYValue.toFixed(2) }}</view>
            <slider
              :value="scaleYValue * 10"
              @change="
                (e) => {
                  scaleYValue = e.detail.value / 10;
                  updateTransform();
                }
              "
              min="1"
              max="5"
              activeColor="#b38c4f"
            />
          </view>
          <view class="debug-item">
            <view class="debug-label">平移X</view>
            <view class="debug-value">{{ offsetXValue }}</view>
            <slider
              :value="offsetXValue + 50"
              @change="
                (e) => {
                  offsetXValue = e.detail.value - 50;
                  updateTransform();
                }
              "
              min="0"
              max="100"
              activeColor="#b38c4f"
            />
          </view>
          <view class="debug-item">
            <view class="debug-label">平移Y</view>
            <view class="debug-value">{{ offsetYValue }}</view>
            <slider
              :value="offsetYValue + 50"
              @change="
                (e) => {
                  offsetYValue = e.detail.value - 50;
                  updateTransform();
                }
              "
              min="0"
              max="100"
              activeColor="#b38c4f"
            />
          </view>
          <view class="debug-item">
            <view class="debug-label">旋转中心X</view>
            <view class="debug-value">{{ centerXValue.toFixed(1) }}</view>
            <slider
              :value="centerXValue"
              @change="
                (e) => {
                  centerXValue = e.detail.value;
                  updateTransform();
                }
              "
              min="0"
              max="100"
              activeColor="#b38c4f"
            />
          </view>
          <view class="debug-item">
            <view class="debug-label">旋转中心Y</view>
            <view class="debug-value">{{ centerYValue.toFixed(1) }}</view>
            <slider
              :value="centerYValue"
              @change="
                (e) => {
                  centerYValue = e.detail.value;
                  updateTransform();
                }
              "
              min="0"
              max="100"
              activeColor="#b38c4f"
            />
          </view>

          <view class="debug-divider"></view>
          <view class="debug-section-title">XY → 经纬度 反向验证</view>
          <view class="xy-input-row">
            <input
              v-model="inputX"
              type="number"
              placeholder="X像素(0-750)"
              class="xy-input"
              @input="convertedLngLat = null"
            />
            <text class="xy-separator">,</text>
            <input
              v-model="inputY"
              type="number"
              placeholder="Y像素(0-1200)"
              class="xy-input"
              @input="convertedLngLat = null"
            />
          </view>
          <view class="xy-actions">
            <view class="debug-btn-action" @click="convertXYToLngLat"
              >转换</view
            >
            <view class="debug-btn-action" @click="clearXYInput">清空</view>
          </view>
          <view v-if="convertedLngLat" class="xy-result">
            <text class="xy-result-label">经纬度：</text>
            <text class="xy-result-value"
              >{{ convertedLngLat.lng }}, {{ convertedLngLat.lat }}</text
            >
          </view>

          <view class="debug-divider"></view>
          <view class="debug-actions">
            <view class="debug-btn-action" @click="resetTransform"
              >重置参数</view
            >
            <view class="debug-btn-action apply-btn" @click="applyTransform"
              >应用并重绘路线</view
            >
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from "vue";
import { onShow, onHide } from "@dcloudio/uni-app";
import { AMAP_WEB_KEY } from "../../config.js";
import UniIcons from "@dcloudio/uni-ui/lib/uni-icons/uni-icons.vue";

// --- 讯飞数字人相关 ---
const isDigitalHumanLoading = ref(true);
const speakEnd = ref(false);
const digitalHumanUrl = computed(() => {
  return `/static/xfyun-digital-human/web-api.html`;
});

// 处理数字人播报结束
const handleSpeakEnd = () => {
  speakEnd.value = true;
  // 重置状态
  setTimeout(() => {
    speakEnd.value = false;
  }, 100);
};

// 数字人webview引用
let digitalHumanWebview = null;

// 创建数字人webview
const createDigitalHumanWebview = () => {
  try {
    // 防止重复创建
    if (digitalHumanWebview) {
      console.log("⚠️ 数字人webview已存在，跳过创建");
      return;
    }

    // H5环境降级方案
    if (typeof plus === "undefined" || !plus.webview) {
      createDigitalHumanIframe();
      return;
    }

    // 获取当前页面的webview - 使用延迟获取确保正确
    setTimeout(() => {
      try {
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
          const routeName = "companionGuide";

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
          createDigitalHumanIframe();
          return;
        }

        if (currentWebview && plus.webview) {
          // 使用 uni.createSelectorQuery 获取绿色占位框的实际位置
          uni
            .createSelectorQuery()
            .select(".avatar-section")
            .boundingClientRect((rect) => {
              if (!rect) {
                return;
              }

              const systemInfo = uni.getSystemInfoSync();

              // 放大后的webview尺寸（宽度更大）
              const width = 196;
              const height = 275;
              const left = -85;
              // 去掉导航栏，只加上状态栏高度，让数字人更靠上
              const top = -40;

              const htmlPath = "_www/static/xfyun-digital-human/web-api.html";
              const preloadedWebview = plus.webview.getWebviewById(
                "preloadDigitalHuman",
              );

              if (preloadedWebview) {
                digitalHumanWebview = preloadedWebview;

                // 调整位置和尺寸
                digitalHumanWebview.setStyle({
                  left: left,
                  top: top,
                  width: width,
                  height: height,
                  opacity: 1,
                  show: true,
                  scalable: false,
                  bounce: "none",
                  background: "transparent",
                  webkitOverflowScrolling: "touch",
                  allowZoom: false,
                  showScrollbar: false,
                  zindex: 1, // 降低z-index，避免遮挡导航栏
                  popGesture: "none",
                  hardwareAccelerated: true,
                  ignoretouch: false, // 允许点击事件，用于数字人交互
                  hitTest: true, // 允许点击测试
                  pointerEvents: "auto", // CSS pointer-events
                });
              } else {
                // 创建新的webview
                digitalHumanWebview = plus.webview.create(
                  htmlPath,
                  "digitalHuman",
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
                    zindex: 1, // 降低z-index，避免遮挡导航栏
                    popGesture: "none",
                    hardwareAccelerated: true,
                    ignoretouch: false, // 允许点击事件，用于数字人交互
                    hitTest: true, // 允许点击测试
                    pointerEvents: "none", // CSS pointer-events
                  },
                );
              }

              // 监听消息
              digitalHumanWebview.addEventListener("message", (e) => {
                const data = e.data && e.data[0];
                if (data) {
                  console.log("[webview 消息] 收到消息:", data);

                  if (data.type === "ready") {
                    isDigitalHumanLoading.value = false;
                  } else if (data.type === "speakEnd") {
                    handleSpeakEnd();
                  } else if (data.type === "updateText") {
                    console.log("[webview 消息] 更新文本框:", data.text);
                    displayText.value = data.text;
                  }
                }
              });

              // 监听数字人错误
              digitalHumanWebview.addEventListener("error", () => {});

              // 显示数字人webview
              digitalHumanWebview.show();

              // 发送给数字人消息（商家信息）
              setTimeout(() => {
                const msgData = JSON.stringify({
                  type: "setMerchantInfo",
                  data: {
                    merchantName: merchantDetail.value.name,
                    introduction: merchantDetail.value.introduction,
                  },
                });
                digitalHumanWebview.evalJS(
                  `window.postMessage(${JSON.stringify(msgData)});`,
                );
              }, 500);
            })
            .exec();

          // 提前返回，避免执行到后面的代码
          return;
        }
      } catch (e) {
        console.error("创建数字人webview失败:", e);
      }
    }, 500); // 延迟500ms确保页面webview已准备好
  } catch (e) {
    console.error("创建数字人webview失败:", e);
  }
};

// 让数字人说话
const speak = (text) => {
  if (!isDigitalHumanLoading.value) {
    sendMessageToWebView({ type: "speak", text: text });
  }
};

// 停止说话
const stopSpeak = () => {
  sendMessageToWebView({ type: "stop" });
};

// H5环境创建数字人iframe
const createDigitalHumanIframe = () => {
  try {
    // 创建iframe元素
    const iframe = document.createElement("iframe");
    iframe.src = "/static/xfyun-digital-human/index.html";
    iframe.style.width = "90px";
    iframe.style.height = "120px";
    iframe.style.position = "fixed";
    iframe.style.top = "88px";
    iframe.style.left = "24px";
    iframe.style.borderRadius = "8px";
    iframe.style.border = "2px solid rgba(255, 255, 255, 0.8)";
    iframe.style.boxShadow = "0 4px 12px rgba(0, 0, 0, 0.15)";
    iframe.style.zIndex = "100";
    iframe.style.overflow = "hidden";

    // 监听消息
    window.addEventListener("message", (e) => {
      console.log("收到数字人消息:", e.data);
      const data = e.data && e.data[0];
      if (data) {
        if (data.type === "ready") {
          isDigitalHumanLoading.value = false;
          console.log("数字人准备就绪");
        } else if (data.type === "speakEnd") {
          console.log("播报结束");
        }
      }
    });

    // 添加到页面
    document.body.appendChild(iframe);
    digitalHumanWebview = iframe;
    console.log("数字人iframe创建成功");
  } catch (e) {
    console.error("创建数字人iframe失败:", e);
  }
};

// 向数字人发送消息
const sendMessageToWebView = (data) => {
  if (digitalHumanWebview) {
    try {
      if (digitalHumanWebview.contentWindow) {
        // iframe方式
        digitalHumanWebview.contentWindow.postMessage(data, "*");
      } else if (digitalHumanWebview.evalJS) {
        // plus.webview方式
        const script = `window.postMessage(${JSON.stringify(data)});`;
        digitalHumanWebview.evalJS(script);
      }
    } catch (e) {
      console.error("发送消息失败:", e);
    }
  }
};

// 监听播报结束
watch(speakEnd, (newVal) => {
  if (newVal && digitalHumanWebview) {
    console.log("播报结束");
    if (digitalHumanWebview.evalJS) {
      digitalHumanWebview.evalJS("handleSpeakEnd()");
    }
  }
});

// 触发数字人播放（处理移动端自动播放限制）
const triggerDigitalHumanPlay = () => {
  sendMessageToWebView({ type: "play" });
};

// 销毁数字人webview
const destroyDigitalHumanWebview = () => {
  console.log("========== 销毁数字人webview ==========");
  console.log("当前digitalHumanWebview:", digitalHumanWebview);

  if (digitalHumanWebview) {
    try {
      console.log("正在销毁数字人webview:", digitalHumanWebview.id);

      // 检查是否是预加载的webview
      const isPreloaded =
        digitalHumanWebview.id === "preloadDigitalHuman" ||
        digitalHumanWebview.__uuid__ === "preloadDigitalHuman";

      if (isPreloaded) {
        // 预加载的webview不销毁，只是移回隐藏位置
        console.log("✅ 预加载webview，移回隐藏位置");
        digitalHumanWebview.setStyle({
          left: -9999,
          top: -9999,
          width: 1,
          height: 1,
          opacity: 0,
          show: false,
        });
      } else {
        // 先隐藏
        if (digitalHumanWebview.hide) {
          console.log("步骤1: 隐藏webview");
          digitalHumanWebview.hide();
        }
        // 然后关闭
        if (digitalHumanWebview.close) {
          console.log("步骤2: 关闭webview");
          // plus.webview方式
          digitalHumanWebview.close("none");
        } else if (digitalHumanWebview.parentNode) {
          console.log("步骤2: 移除DOM节点");
          // iframe方式
          digitalHumanWebview.parentNode.removeChild(digitalHumanWebview);
        }
      }
      digitalHumanWebview = null;
      console.log("✅ 数字人webview处理成功");
    } catch (e) {
      console.error("❌ 销毁数字人webview失败:", e);
    }
  } else {
    console.log("⚠️ 没有需要销毁的数字人webview");
  }
};

// --- 简化景点列表（只有你有经纬度的4个景点）---
// x, y 是底图上的百分比位置（你需要微调）
// lng, lat 是高德地图上对应的经纬度
const SPOTS = [
  {
    name: "灵山大照壁",
    x: 38,
    y: 83,
    lng: 120.102468,
    lat: 31.42145,
    range: 0.001,
    image: "/static/spots/lsdzb.jpg",
  },
  {
    name: "九龙灌浴",
    x: 35,
    y: 58,
    lng: 120.09992,
    lat: 31.42465,
    range: 0.001,
    image: "/static/spots/九龙灌浴.jpg",
  },
  {
    name: "祥符禅寺",
    x: 38,
    y: 33,
    lng: 120.09801,
    lat: 31.427971,
    range: 0.001,
    image: "/static/spots/祥符禅寺.jpg",
  },
  {
    name: "灵山大佛",
    x: 38,
    y: 17,
    lng: 120.096453,
    lat: 31.430265,
    range: 0.001,
    image: "/static/spots/灵山大佛.jpg",
  },
];

// 经纬度范围
const MAP_BOUNDS = {
  minLng: 120.096,
  maxLng: 120.105,
  minLat: 31.42,
  maxLat: 31.431,
};

// 手动调参的坐标变换参数（高德地图 → 手绘地图）
// 你可以根据路线效果微调这些参数
const MAP_TRANSFORM = {
  rotation: 38, // 旋转度数（顺时针）
  centerX: 45.2, // 旋转中心X（百分比）
  centerY: 50, // 旋转中心Y（百分比）
  scaleX: 0.3, // X方向缩放
  scaleY: 0.3, // Y方向缩放
  offsetX: -25, // X方向平移
  offsetY: -24.5, // Y方向平移
};

// 角度转弧度
const degToRad = (deg) => (deg * Math.PI) / 180;

// 坐标变换（经纬度 → 手绘地图百分比坐标）
const lngLatToXY = (lng, lat) => {
  // 1. 线性映射（经纬度 → 百分比坐标）
  let x =
    ((lng - MAP_BOUNDS.minLng) / (MAP_BOUNDS.maxLng - MAP_BOUNDS.minLng)) * 100;
  let y =
    100 -
    ((lat - MAP_BOUNDS.minLat) / (MAP_BOUNDS.maxLat - MAP_BOUNDS.minLat)) * 100;

  // 2. 应用旋转/缩放/平移变换
  const { rotation, centerX, centerY, scaleX, scaleY, offsetX, offsetY } =
    MAP_TRANSFORM;
  const angleRad = degToRad(rotation);
  const cos = Math.cos(angleRad);
  const sin = Math.sin(angleRad);

  let dx = x - centerX;
  let dy = y - centerY;

  dx *= scaleX;
  dy *= scaleY;

  const newX = dx * cos - dy * sin;
  const newY = dx * sin + dy * cos;

  x = newX + centerX + offsetX;
  y = newY + centerY + offsetY;

  return {
    x: Math.max(-30, Math.min(130, x)),
    y: Math.max(-30, Math.min(130, y)),
  };
};

// XY转经纬度（反向转换，用于调试）
const xyToLngLat = (x, y) => {
  const { rotation, centerX, centerY, scaleX, scaleY, offsetX, offsetY } =
    MAP_TRANSFORM;

  // 1. 逆向平移：减去中心和偏移
  let rx = x - centerX - offsetX;
  let ry = y - centerY - offsetY;

  // 2. 逆向旋转：顺时针旋转的逆操作是逆时针旋转
  const angleRad = degToRad(rotation);
  const cos = Math.cos(angleRad);
  const sin = Math.sin(angleRad);
  // 逆时针旋转公式：因为cos(-θ)=cos(θ), sin(-θ)=-sin(θ)
  const tempX = rx * cos + ry * sin;
  const tempY = -rx * sin + ry * cos;

  // 3. 逆向缩放
  rx = tempX / scaleX;
  ry = tempY / scaleY;

  // 4. 回到原始中心点（逆向平移）
  rx = rx + centerX;
  ry = ry + centerY;

  // 5. 线性映射回经纬度
  const lng =
    (rx / 100) * (MAP_BOUNDS.maxLng - MAP_BOUNDS.minLng) + MAP_BOUNDS.minLng;
  const lat =
    MAP_BOUNDS.maxLat - (ry / 100) * (MAP_BOUNDS.maxLat - MAP_BOUNDS.minLat);

  return { lng, lat };
};

// 简单线性插值转换（用于反向验证，不考虑旋转缩放）
const simpleXyToLngLat = (x, y) => {
  // 直接线性映射：不应用任何变换，直接从百分比坐标映射到经纬度
  const lng =
    (x / 100) * (MAP_BOUNDS.maxLng - MAP_BOUNDS.minLng) + MAP_BOUNDS.minLng;
  const lat =
    MAP_BOUNDS.maxLat - (y / 100) * (MAP_BOUNDS.maxLat - MAP_BOUNDS.minLat);
  return { lng, lat };
};

// --- 响应式数据 ---
// 调试面板状态
const showDebugPanel = ref(false);
const rotationValue = ref(MAP_TRANSFORM.rotation);
const scaleXValue = ref(MAP_TRANSFORM.scaleX);
const scaleYValue = ref(MAP_TRANSFORM.scaleY);
const offsetXValue = ref(MAP_TRANSFORM.offsetX);
const offsetYValue = ref(MAP_TRANSFORM.offsetY);
const centerXValue = ref(MAP_TRANSFORM.centerX);
const centerYValue = ref(MAP_TRANSFORM.centerY);

// XY坐标输入（反向验证）
const inputX = ref("");
const inputY = ref("");
const convertedLngLat = ref(null);

// XY转经纬度（使用简单线性插值，用于反向验证）
const convertXYToLngLat = () => {
  const x = parseFloat(inputX.value);
  const y = parseFloat(inputY.value);

  if (isNaN(x) || isNaN(y)) {
    uni.showToast({ title: "请输入有效的XY坐标", icon: "none" });
    return;
  }

  // 直接线性映射：从百分比坐标直接映射到经纬度
  // 不应用旋转/缩放/平移变换，用于验证景点数据的准确性
  const lng =
    (x / 100) * (MAP_BOUNDS.maxLng - MAP_BOUNDS.minLng) + MAP_BOUNDS.minLng;
  const lat =
    MAP_BOUNDS.maxLat - (y / 100) * (MAP_BOUNDS.maxLat - MAP_BOUNDS.minLat);

  convertedLngLat.value = {
    lng: lng.toFixed(6),
    lat: lat.toFixed(6),
  };

  console.log(
    `XY: (${x}, ${y}) -> 经纬度: ${lng.toFixed(6)}, ${lat.toFixed(6)}`,
  );
  return;
};

// 保留旧的转换函数（使用变换参数）
const convertXYToLngLatWithTransform = () => {
  const x = parseFloat(inputX.value);
  const y = parseFloat(inputY.value);

  if (isNaN(x) || isNaN(y)) {
    uni.showToast({ title: "请输入有效的XY坐标", icon: "none" });
    return;
  }

  const { rotation, centerX, centerY, scaleX, scaleY, offsetX, offsetY } =
    MAP_TRANSFORM;

  // 1. 逆向平移：减去中心和偏移
  let rx = x - centerX - offsetX;
  let ry = y - centerY - offsetY;

  // 2. 逆向旋转：顺时针旋转的逆操作是逆时针旋转
  const angleRad = degToRad(rotation);
  const cos = Math.cos(angleRad);
  const sin = Math.sin(angleRad);
  // 逆时针旋转公式：因为cos(-θ)=cos(θ), sin(-θ)=-sin(θ)
  const tempX = rx * cos + ry * sin;
  const tempY = -rx * sin + ry * cos;

  // 3. 逆向缩放
  rx = tempX / scaleX;
  ry = tempY / scaleY;

  rx = rx + centerX;
  ry = ry + centerY;

  const lng =
    (rx / 100) * (MAP_BOUNDS.maxLng - MAP_BOUNDS.minLng) + MAP_BOUNDS.minLng;
  const lat =
    MAP_BOUNDS.maxLat - (ry / 100) * (MAP_BOUNDS.maxLat - MAP_BOUNDS.minLat);

  convertedLngLat.value = {
    lng: lng.toFixed(6),
    lat: lat.toFixed(6),
  };

  console.log(
    `XY: (${x}, ${y}) -> 经纬度: ${lng.toFixed(6)}, ${lat.toFixed(6)}`,
  );
};

const clearXYInput = () => {
  inputX.value = "";
  inputY.value = "";
  convertedLngLat.value = null;
};

const longitude = ref(120.103637);
const latitude = ref(31.420211);
const isGuiding = ref(false);
const displayText = ref(
  '我是您的导览助手，当前页面为景区地图，点击"获取位置"开始导览，点击景点获取景点文化内涵介绍，点击"推荐路线"或"收藏路线"查看路线并导览',
);

// 用户位置（百分比坐标）
const userX = ref(-100);
const userY = ref(-100);

const showSpotModal = ref(false);
const currentSpotDetail = ref({ name: "", description: "" });
const currentSpot = ref(null);
const visitedSpots = ref([]);

// Canvas相关
const canvasWidth = ref(750);
const canvasHeight = ref(1200);

// 目标路线
const targetRoute = ref([]);

// 路线坐标点
const routePoints = ref([]);

// 当前路线索引
const currentRouteIndex = ref(0);

// 地图缩放相关
const mapScale = ref(1);
const minScale = 1;
const maxScale = 3;

// 地图平移相关
const offsetX = ref(0);
const offsetY = ref(0);

// 触摸相关变量
let touchStartX = 0;
let touchStartY = 0;
let lastTouchDistance = 0;
let isPinching = false;
let isPanning = false;

// PDR相关变量
let sensorTimer = null;
let lastAccel = { x: 0, y: 0, z: 0 };
let stepCount = 0;
let lastStepTime = 0;
const STEP_THRESHOLD = 3.5;
const STEP_INTERVAL = 300;
const STEP_LENGTH = 0.00006;

const currentHeading = ref(0);
let hasCompass = false;
let lastValidHeading = 0;
let lastStepHeading = 0;
let isTracking = false;

// --- 方法 ---

// 获取标记样式
const getMarkerStyle = (spot) => {
  return {
    left: `${spot.x}%`,
    top: `${spot.y}%`,
  };
};

// 获取用户标记样式
const getUserMarkerStyle = () => {
  return {
    left: `${userX.value}%`,
    top: `${userY.value}%`,
  };
};

// 获取地图内部样式
const getMapInnerStyle = () => {
  return {
    transform: `scale(${mapScale.value}) translate(${offsetX.value}px, ${offsetY.value}px)`,
  };
};

// 景点名称是否需要反转
const isReverseMarker = (name) => {
  return name.includes("佛") || name.includes("塔");
};

const handleTextboxClick = () => {
  displayText.value =
    '我是您的导览助手，当前页面为景区地图，点击"获取位置"开始导览，点击景点获取景点文化内涵介绍，点击"推荐路线"或"收藏路线"查看路线并导览';
};

// 通用方法：让数字人说话
const speakText = (text) => {
  console.log(`[数字人] 发送文本: "${text}"`);

  if (digitalHumanWebview) {
    digitalHumanWebview.evalJS(`window.sendTextInteract("${text}")`);
    console.log("[数字人] 消息已发送");
  } else {
    console.warn("[数字人] webview不存在");
  }
};

// 数字人点击处理
const handleAvatarClick = () => {
  console.log("数字人被点击");
  const welcomeText =
    "欢迎来到灵山胜境，我是你的智能导游，后续有文本点击播报后，我将为您播报";
  speakText(welcomeText);
  displayText.value = welcomeText;
};

// 供数字人webview调用的全局函数，用于更新文本框
const updateDigitalHumanText = (text) => {
  console.log("[全局函数] updateDigitalHumanText:", text);
  displayText.value = text;
};

// 在 onMounted 中注册全局函数
let registerGlobalFunction = () => {
  if (typeof window !== "undefined") {
    window.updateDigitalHumanText = updateDigitalHumanText;
  }
};

// 景点点击
const onMarkerTap = (index) => {
  const spot = SPOTS[index];
  currentSpotDetail.value = {
    name: spot.name,
    description: `这是 ${spot.name}，是灵山景区的重要景点之一。景点坐标: (${spot.x}%, ${spot.y}%)`,
    image: spot.image,
  };
  showSpotModal.value = true;
};

const onMarkerLongPress = (index) => {
  const spot = SPOTS[index];
  displayText.value = `您长按了 ${spot.name}，经纬度: (${spot.lng}, ${spot.lat})`;
};

// 景点详情
const closeSpotModal = () => {
  showSpotModal.value = false;
};

// 触摸事件
const onTouchStart = (e) => {
  if (e.touches.length === 1) {
    touchStartX = e.touches[0].clientX;
    touchStartY = e.touches[0].clientY;
    isPanning = true;
  } else if (e.touches.length === 2) {
    const dx = e.touches[0].clientX - e.touches[1].clientX;
    const dy = e.touches[0].clientY - e.touches[1].clientY;
    lastTouchDistance = Math.sqrt(dx * dx + dy * dy);
    isPinching = true;
    isPanning = false;
  }
};

const onTouchMove = (e) => {
  if (isPanning && e.touches.length === 1) {
    const dx = e.touches[0].clientX - touchStartX;
    const dy = e.touches[0].clientY - touchStartY;
    offsetX.value += dx / mapScale.value / 10;
    offsetY.value += dy / mapScale.value / 10;
    touchStartX = e.touches[0].clientX;
    touchStartY = e.touches[0].clientY;
  } else if (isPinching && e.touches.length === 2) {
    const dx = e.touches[0].clientX - e.touches[1].clientX;
    const dy = e.touches[0].clientY - e.touches[1].clientY;
    const distance = Math.sqrt(dx * dx + dy * dy);
    const scaleDiff = distance / lastTouchDistance;
    if (
      mapScale.value * scaleDiff >= minScale &&
      mapScale.value * scaleDiff <= maxScale
    ) {
      mapScale.value *= scaleDiff;
    }
    lastTouchDistance = distance;
  }
};

const onTouchEnd = () => {
  isPanning = false;
  isPinching = false;
};

// 缩放控制
const zoomIn = () => {
  if (mapScale.value < maxScale) {
    mapScale.value = Math.min(maxScale, mapScale.value * 1.2);
  }
};

const zoomOut = () => {
  if (mapScale.value > minScale) {
    mapScale.value = Math.max(minScale, mapScale.value / 1.2);
  }
};

// 导览开关
const toggleGuide = () => {
  isGuiding.value = !isGuiding.value;
  if (isGuiding.value) {
    displayText.value = "导览模式已开启，正在获取您的位置...";
    userX.value = SPOTS[0].x;
    userY.value = SPOTS[0].y;
    startSensor();
  } else {
    displayText.value = "导览已取消";
    userX.value = -100;
    userY.value = -100;
    stopSensor();
  }
};

// 模拟走路
const simulateWalk = () => {
  if (!isGuiding.value || targetRoute.value.length === 0) return;
  if (currentRouteIndex.value >= targetRoute.value.length - 1) {
    displayText.value = "您已到达终点！";
    return;
  }
  const nextSpot = targetRoute.value[currentRouteIndex.value + 1];
  const spot = SPOTS.find((s) => s.name === nextSpot);
  if (spot) {
    userX.value = spot.x;
    userY.value = spot.y;
    currentRouteIndex.value++;
    displayText.value = `已移动到 ${spot.name}`;
    checkArrival(spot);
  }
};

// 检查到达
const checkArrival = (spot) => {
  visitedSpots.value.push(spot.name);
  currentSpot.value = spot.name;
};

// 调试功能
const toggleDebug = () => {
  showDebugPanel.value = !showDebugPanel.value;
};

// 点击地图获取坐标
const onMapClick = (e) => {
  // 获取点击位置的百分比坐标
  const query = uni.createSelectorQuery();
  query
    .select(".map-inner")
    .boundingClientRect((rect) => {
      if (rect) {
        const touch = e.touches ? e.touches[0] : e;
        const x = ((touch.clientX - rect.left) / rect.width) * 100;
        const y = ((touch.clientY - rect.top) / rect.height) * 100;

        const latlng = xyToLngLat(x, y);

        console.log(`点击位置 XY: (${x.toFixed(2)}%, ${y.toFixed(2)}%)`);
        console.log(
          `对应的经纬度: ${latlng.lng.toFixed(6)}, ${latlng.lat.toFixed(6)}`,
        );

        uni.showToast({
          title: `XY: (${x.toFixed(1)}%, ${y.toFixed(1)}%)\nLng: ${latlng.lng.toFixed(6)}\nLat: ${latlng.lat.toFixed(6)}`,
          icon: "none",
          duration: 3000,
        });
      }
    })
    .exec();
};

// 更新变换参数
const updateTransform = () => {
  MAP_TRANSFORM.rotation = rotationValue.value;
  MAP_TRANSFORM.scaleX = scaleXValue.value;
  MAP_TRANSFORM.scaleY = scaleYValue.value;
  MAP_TRANSFORM.offsetX = offsetXValue.value;
  MAP_TRANSFORM.offsetY = offsetYValue.value;
  MAP_TRANSFORM.centerX = centerXValue.value;
  MAP_TRANSFORM.centerY = centerYValue.value;

  console.log("更新变换参数:", MAP_TRANSFORM);
};

// 重置参数
const resetTransform = () => {
  rotationValue.value = 38;
  scaleXValue.value = 0.3;
  scaleYValue.value = 0.3;
  offsetXValue.value = -29;
  offsetYValue.value = -24.5;
  centerXValue.value = 45.2;
  centerYValue.value = 50;
  updateTransform();

  uni.showToast({
    title: "参数已重置",
    icon: "success",
  });
};

// 应用并重绘路线
const applyTransform = () => {
  updateTransform();
  drawRouteOnMap();

  uni.showToast({
    title: "路线已重绘",
    icon: "success",
  });
};

// 传感器
const startSensor = () => {
  if (sensorTimer) return;
  sensorTimer = setInterval(() => {
    const accel = lastAccel;
    const magnitude = Math.sqrt(accel.x ** 2 + accel.y ** 2 + accel.z ** 2);
    const now = Date.now();
    if (magnitude > STEP_THRESHOLD && now - lastStepTime > STEP_INTERVAL) {
      stepCount++;
      lastStepTime = now;
      const headingRad = (currentHeading.value * Math.PI) / 180;
      const newLng = longitude.value + STEP_LENGTH * Math.sin(headingRad);
      const newLat = latitude.value + STEP_LENGTH * Math.cos(headingRad);
      longitude.value = newLng;
      latitude.value = newLat;
      const xy = lngLatToXY(newLng, newLat);
      userX.value = xy.x;
      userY.value = xy.y;
    }
  }, 50);
};

const stopSensor = () => {
  if (sensorTimer) {
    clearInterval(sensorTimer);
    sensorTimer = null;
  }
};

// --- 测试路线（简化版）---
const testRoute = async () => {
  uni.showLoading({ title: "规划路线..." });

  const routeNames = ["灵山大照壁", "九龙灌浴", "祥符禅寺", "灵山大佛"];
  const start = SPOTS.find((s) => s.name === routeNames[0]);
  const end = SPOTS.find((s) => s.name === routeNames[routeNames.length - 1]);
  const waypoints = routeNames.slice(1, -1).map((name) => {
    const spot = SPOTS.find((s) => s.name === name);
    return `${spot.lng},${spot.lat}`;
  });

  try {
    const res = await uni.request({
      url: "https://restapi.amap.com/v3/direction/walking",
      method: "GET",
      data: {
        origin: `${start.lng},${start.lat}`,
        destination: `${end.lng},${end.lat}`,
        waypoints: waypoints.join("|"),
        key: AMAP_WEB_KEY,
        output: "json",
      },
    });

    uni.hideLoading();

    if (res.data.status === "1" && res.data.route && res.data.route.paths) {
      const path = res.data.route.paths[0];
      const allPoints = [];

      if (path.steps) {
        path.steps.forEach((step) => {
          if (step.polyline) {
            step.polyline.split(";").forEach((pair) => {
              const [lng, lat] = pair.split(",").map(Number);
              allPoints.push({ lng, lat });
            });
          }
        });
      }

      console.log("========== 路线规划成功 ==========");
      console.log("解析到路径点:", allPoints.length);
      console.log("原始路径点示例:", allPoints.slice(0, 3));
      console.log(
        "转换后路径点示例:",
        allPoints.slice(0, 3).map((p) => lngLatToXY(p.lng, p.lat)),
      );

      routePoints.value = allPoints;
      targetRoute.value = routeNames;
      currentRouteIndex.value = 0;

      setTimeout(() => {
        drawRouteOnMap();
      }, 100);

      displayText.value = `路线规划成功！共 ${allPoints.length} 个路径点`;
      uni.showToast({ title: "路线规划成功", icon: "success" });
    } else {
      console.error("路线规划失败:", res.data);
      displayText.value = `路线规划失败: ${res.data.info}`;
      uni.showToast({ title: "路线规划失败", icon: "none" });
    }
  } catch (error) {
    uni.hideLoading();
    console.error("请求异常:", error);
    displayText.value = "网络请求失败";
    uni.showToast({ title: "网络请求失败", icon: "none" });
  }
};

// 绘制路线
const drawRouteOnMap = () => {
  if (routePoints.value.length < 2) {
    console.log("路径点不足");
    return;
  }

  const ctx = uni.createCanvasContext("routeCanvas");
  if (!ctx) {
    console.error("Canvas上下文获取失败");
    return;
  }

  ctx.clearRect(0, 0, canvasWidth.value, canvasHeight.value);

  const canvasPoints = routePoints.value.map((point) => {
    const xy = lngLatToXY(point.lng, point.lat);
    return {
      x: (xy.x / 100) * canvasWidth.value,
      y: (xy.y / 100) * canvasHeight.value,
    };
  });

  console.log("画布坐标点数:", canvasPoints.length);
  console.log("画布坐标示例:", canvasPoints.slice(0, 3));

  ctx.beginPath();
  ctx.setStrokeStyle("#4a90e2");
  ctx.setLineWidth(6);
  ctx.setLineCap("round");
  ctx.setLineJoin("round");

  ctx.moveTo(canvasPoints[0].x, canvasPoints[0].y);
  for (let i = 1; i < canvasPoints.length; i++) {
    ctx.lineTo(canvasPoints[i].x, canvasPoints[i].y);
  }
  ctx.stroke();

  ctx.beginPath();
  ctx.setStrokeStyle("rgba(74, 144, 226, 0.3)");
  ctx.setLineWidth(6);
  ctx.moveTo(canvasPoints[0].x, canvasPoints[0].y);
  for (let i = 1; i < canvasPoints.length; i++) {
    ctx.lineTo(canvasPoints[i].x, canvasPoints[i].y);
  }
  ctx.stroke();

  const startPoint = canvasPoints[0];
  ctx.beginPath();
  ctx.setFillStyle("#52c41a");
  ctx.arc(startPoint.x, startPoint.y, 5, 0, Math.PI * 2);
  ctx.fill();
  ctx.beginPath();
  ctx.setFillStyle("#fff");
  ctx.arc(startPoint.x, startPoint.y, 2.5, 0, Math.PI * 2);
  ctx.fill();

  const endPoint = canvasPoints[canvasPoints.length - 1];
  ctx.beginPath();
  ctx.setFillStyle("#ff4d4f");
  ctx.arc(endPoint.x, endPoint.y, 5, 0, Math.PI * 2);
  ctx.fill();
  ctx.beginPath();
  ctx.setFillStyle("#fff");
  ctx.arc(endPoint.x, endPoint.y, 2.5, 0, Math.PI * 2);
  ctx.fill();

  ctx.draw();
  console.log("========== 路线绘制完成 ==========");
};

// 页面跳转
const goToRecommend = () => {
  uni.navigateTo({
    url: "/pages/recommendRoute/recommendRoute",
  });
};

const goToCollection = () => {
  uni.navigateTo({
    url: "/pages/routeCollection/routeCollection",
  });
};

// 生命周期
onMounted(() => {
  console.log("========== CompanionGuide 页面加载 ==========");
  console.log(
    "4个景点:",
    SPOTS.map((s) => `${s.name}: (${s.x}%, ${s.y}%)`),
  );

  const testPoint = lngLatToXY(SPOTS[0].lng, SPOTS[0].lat);
  console.log(
    `测试转换 [${SPOTS[0].name}]: (${SPOTS[0].lng}, ${SPOTS[0].lat}) -> (${testPoint.x.toFixed(
      2,
    )}%, ${testPoint.y.toFixed(2)}%)`,
  );

  // 注册全局函数供数字人 webview 调用
  registerGlobalFunction();

  // 创建数字人 webview
  setTimeout(() => {
    createDigitalHumanWebview();
  }, 100);
});

onShow(() => {
  console.log("CompanionGuide 页面显示");
  // 重新创建数字人webview（因为onHide时销毁了）
  if (!digitalHumanWebview) {
    setTimeout(() => {
      createDigitalHumanWebview();
    }, 100);
  }
});

onHide(() => {
  stopSensor();
  destroyDigitalHumanWebview();
});

onUnmounted(() => {
  stopSensor();
  destroyDigitalHumanWebview();
});
</script>

<style scoped>
/* 自定义导航栏（完全复刻原生） */
.custom-nav-bar {
  width: 100%;
  background: #f8f8f8;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 999;
  border-bottom: 1rpx solid #e8e8e8;
}

.nav-content {
  width: 100%;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: var(--status-bar-height, 44px);
}

.nav-title {
  font-size: 34rpx;
  font-weight: 500;
  color: #333333;
}

.container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
  padding-top: calc(var(--status-bar-height, 44px) + 88rpx);
}

.top-section {
  width: 100%;
  height: 18%;
  display: flex;
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 50%, #a5d6a7 100%);
  padding: 24rpx;
  box-sizing: border-box;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.08);
}

.avatar-section {
  width: 140rpx;
  height: 270rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
  border-radius: 16rpx;
  z-index: 10;
}

.avatar-webview {
  width: 300rpx;
  height: 400rpx;
  border-radius: 16rpx;
  border: 4rpx solid blue;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.15);
  position: relative;
  z-index: 10;
  overflow: hidden;
  clip-path: inset(0 0 0 0 round 16rpx);
  transform-origin: top left;
}

.info-section {
  flex: 0 0 450rpx; /* 固定宽度，变小一点 */
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20rpx;
  padding: 20rpx 24rpx;
  margin: 0 20rpx 0 70rpx; /* 左边距更大，增加与数字人的距离 */
  height: 220rpx; /* 缩短高度 */
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  justify-content: center;
  transition: all 0.2s ease;
}

.info-section:active {
  transform: scale(0.98);
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
}

.info-scroll {
  height: 100%;
  overflow-y: auto;
}

.info-text {
  font-size: 28rpx;
  color: #333;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  font-weight: 400;
}

.route-buttons {
  position: absolute;
  top: 20rpx;
  right: 20rpx;
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: 15rpx;
}

.route-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10rpx 20rpx;
  background: #ffffff;
  border-radius: 8rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
  min-width: 100rpx;
}

.route-btn-text {
  font-size: 22rpx;
  color: #666;
  margin-top: 4rpx;
}

.recommend-btn {
  background: #ffffff;
}

.recommend-btn .route-btn-text {
  color: #b38c4f;
}

.collect-btn {
  background: #ffffff;
}

.collect-btn .route-btn-text {
  color: #b38c4f;
}

.bottom-section {
  width: 100%;
  height: 80%;
  position: relative;
  overflow: hidden;
  z-index: 100;
}

.map-container {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
  overflow: hidden;
  touch-action: none;
  user-select: none;
}

.map-content {
  width: 100%;
  height: 100%;
  position: relative;
  margin: 0 auto;
}

.map-inner {
  width: 100%;
  height: 100%;
  position: relative;
  top: 0;
  left: 0;
  will-change: transform;
}

.map-overlay {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
}

.route-canvas {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 10;
  pointer-events: none;
}

.hand-drawn-map {
  width: 100%;
  height: 100%;
}

.spot-marker {
  position: absolute;
  z-index: 10;
}

.marker-pin {
  width: 12rpx;
  height: 12rpx;
  background: #ff6b6b;
  border-radius: 50%;
  border: 2rpx solid #fff;
  box-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.3);
}

.marker-label {
  font-size: 20rpx;
  color: #333;
  background: rgba(255, 255, 255, 0.9);
  padding: 4rpx 8rpx;
  border-radius: 6rpx;
  position: absolute;
  left: 16rpx;
  top: -14rpx;
  white-space: nowrap;
}

.reverse-marker .marker-label {
  left: auto;
  right: 16rpx;
}

.user-marker {
  position: absolute;
  z-index: 20;
  width: 44rpx;
  height: 44rpx;
  pointer-events: none;
}

.user-arrow {
  width: 100%;
  height: 100%;
  position: relative;
  transition: transform 0.1s linear;
}

.user-pulse {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: rgba(74, 144, 226, 0.3);
  animation: userPulse 2s infinite;
}

@keyframes userPulse {
  0% {
    transform: scale(1);
    opacity: 0.8;
  }
  100% {
    transform: scale(2.5);
    opacity: 0;
  }
}

.user-dot {
  position: absolute;
  width: 24rpx;
  height: 24rpx;
  background: linear-gradient(135deg, #4a90e2 0%, #357abd 100%);
  border-radius: 50%;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.3);
  border: 3rpx solid #fff;
}

.user-direction {
  position: absolute;
  width: 0;
  height: 0;
  border-left: 8rpx solid transparent;
  border-right: 8rpx solid transparent;
  border-bottom: 16rpx solid #4a90e2;
  left: 50%;
  top: 4rpx;
  transform: translateX(-50%);
  filter: drop-shadow(0 1rpx 3rpx rgba(0, 0, 0, 0.2));
}

.top-right-btn {
  position: absolute;
  top: 20rpx;
  right: 150rpx;
  background: rgba(255, 255, 255, 0.95);
  padding: 15rpx 20rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  z-index: 100;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
}

.btn-label {
  font-size: 24rpx;
  color: #333;
}

.bottom-right-btn {
  position: absolute;
  bottom: 50rpx;
  right: 40rpx;
  background: #ffffff;
  padding: 15rpx 20rpx;
  border-radius: 8rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
  z-index: 100;
  min-width: 100rpx;
}

.loc-icon {
  font-size: 40rpx;
  margin-bottom: 4rpx;
  transition: color 0.3s;
}

.loc-icon.active {
  color: #4a90e2;
}

.loc-text {
  font-size: 22rpx;
  color: #333;
}

.zoom-controls {
  position: absolute;
  bottom: 220rpx;
  right: 40rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  z-index: 100;
}

.zoom-btn {
  width: 60rpx;
  height: 60rpx;
  background: #fff;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.15);
  transition: all 0.2s;
}

.walk-sim-btn {
  position: absolute;
  bottom: 40rpx;
  left: 20rpx;
  background: linear-gradient(135deg, #4a90e2 0%, #357abd 100%);
  border-radius: 12rpx;
  padding: 12rpx 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(74, 144, 226, 0.4);
  z-index: 100;
  transition: all 0.2s;
}

.walk-sim-btn:active {
  transform: scale(0.95);
  box-shadow: 0 2rpx 8rpx rgba(74, 144, 226, 0.3);
}

.walk-text {
  color: #fff;
  font-size: 24rpx;
  font-weight: 500;
}

.zoom-btn:active {
  background: #f0f0f0;
  transform: scale(0.95);
}

.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  width: 600rpx;
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
}

.modal-header {
  padding: 30rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1rpx solid #eee;
}

.modal-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.modal-close {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  color: #999;
}

.modal-body {
  padding: 30rpx;
  max-height: 60vh;
}

.modal-image {
  width: 100%;
  max-height: 300rpx;
  margin-bottom: 20rpx;
  border-radius: 12rpx;
}

.modal-desc {
  font-size: 28rpx;
  color: #666;
  line-height: 1.8;
}

/* 调试按钮 */
.debug-btn {
  position: absolute;
  bottom: 200rpx;
  right: 30rpx;
  background: rgba(179, 140, 79, 0.95);
  border-radius: 50rpx;
  padding: 16rpx 24rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
  box-shadow: 0 4rpx 16rpx rgba(179, 140, 79, 0.4);
  z-index: 100;
}

.debug-icon {
  font-size: 32rpx;
}

.debug-text {
  color: #fff;
  font-size: 26rpx;
  font-weight: 500;
}

/* 调试面板 */
.debug-panel-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  z-index: 1000;
}

.debug-panel {
  width: 100%;
  background: #fff;
  border-radius: 30rpx 30rpx 0 0;
  max-height: 80vh;
}

.debug-header {
  padding: 30rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1rpx solid #eee;
}

.debug-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #333;
}

.debug-close {
  font-size: 40rpx;
  color: #999;
  padding: 10rpx;
}

.debug-body {
  padding: 20rpx 30rpx;
  max-height: 60vh;
}

.debug-item {
  margin-bottom: 30rpx;
}

.debug-label {
  font-size: 26rpx;
  color: #666;
  margin-bottom: 10rpx;
}

.debug-value {
  font-size: 28rpx;
  color: #b38c4f;
  font-weight: 500;
  margin-bottom: 10rpx;
}

.debug-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 40rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #eee;
}

.debug-btn-action {
  flex: 1;
  height: 80rpx;
  background: #f5f5f5;
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  color: #666;
}

.debug-btn-action.apply-btn {
  background: linear-gradient(135deg, #b38c4f 0%, #d4a574 100%);
  color: #fff;
}

.debug-divider {
  height: 1rpx;
  background: #eee;
  margin: 30rpx 0;
}

.debug-section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
}

.xy-input-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.xy-input {
  flex: 1;
  height: 72rpx;
  background: #f8f8f8;
  border: 1rpx solid #e0e0e0;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.xy-separator {
  font-size: 28rpx;
  color: #999;
}

.xy-actions {
  display: flex;
  gap: 20rpx;
  margin-bottom: 20rpx;
}

.xy-result {
  background: rgba(179, 140, 79, 0.08);
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
}

.xy-result-label {
  font-size: 26rpx;
  color: #666;
}

.xy-result-value {
  font-size: 28rpx;
  color: #b38c4f;
  font-weight: 600;
}
</style>
