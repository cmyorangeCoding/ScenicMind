<template>
  <view class="container">
    <!-- 自定义导航栏（完全复刻原生） -->
    <view class="custom-nav-bar">
      <view class="nav-content">
        <text class="nav-title">景区导览</text>
        <!-- 导览页播报开关 -->
        <view class="stream-speak-toggle" @click="toggleStreamSpeak">
          <uni-icons
            :type="isStreamSpeakEnabled ? 'sound' : 'sound'"
            :size="24"
            :color="isStreamSpeakEnabled ? '#b38c4f' : '#999'"
          />
        </view>
      </view>
    </view>

    <!-- 顶部模块（数字人 + 文字框） -->
    <view class="top-section">
      <!-- 左侧：数字人模块（矩形） -->
      <view class="avatar-section" @click="handleAvatarClick"></view>

      <!-- 右侧：文字渲染框 -->
      <view class="info-section" @click="handleTextboxClick">
        <scroll-view class="info-scroll" scroll-y>
          <text class="info-text">{{ displayText }}</text>
        </scroll-view>
        <!-- 播报按钮 -->
        <view class="tool-row">
          <view class="tool-btn" @click.stop="playVoice">
            <uni-icons type="sound" size="20" color="#b38c4f" />
            <text>{{ isSpeaking ? "取消播报" : "播报" }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部模块（地图区域） -->
    <view class="bottom-section">
      <!-- 收藏路线和推荐路线按钮（浮在地图上方，不受缩放影响） -->
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

      <!-- 获取位置/开始导览按钮 -->
      <view class="bottom-right-btn" @click="toggleGuide">
        <uni-icons
          :type="isGuiding ? 'close' : 'flag'"
          :size="28"
          :color="isGuiding ? '#ff6b6b' : '#4a90e2'"
        />
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

      <!-- 手绘地图容器 -->
      <view
        class="map-container"
        @touchstart="onTouchStart"
        @touchmove="onTouchMove"
        @touchend="onTouchEnd"
      >
        <view class="map-content">
          <view class="map-inner" :style="getMapInnerStyle()">
            <image
              src="/static/LingShan_map.jpg"
              class="hand-drawn-map"
              mode="aspectFill"
              :style="getMapImageStyle()"
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

            <!-- 景点标记（随地图缩放） -->
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
                <view class="marker-pin"></view
                ><view class="marker-label">{{ spot.name }}</view>
              </view>
            </view>

            <!-- 用户位置标记（随地图缩放但保持固定大小，仅在导览中显示） -->
            <view
              v-if="isGuiding"
              class="user-marker"
              :style="getUserMarkerStyle()"
            >
              <view
                class="user-arrow"
                :style="{
                  transform: `rotate(${currentHeading - 180}deg) scale(${1 / mapScale})`,
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
        <view class="modal-body">
          <!-- 景点图片 -->
          <image
            v-if="currentSpotDetail.image"
            :src="currentSpotDetail.image"
            class="modal-image"
            mode="aspectFit"
          ></image>
          <text class="modal-desc">{{ currentSpotDetail.description }}</text>
          <!-- 播报按钮 -->
          <view class="modal-footer">
            <view class="spot-play-btn" @click="playSpotVoice">
              <uni-icons type="sound" size="20" color="#b38c4f" />
              <text>{{ isSpotSpeaking ? "取消播报" : "播报" }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from "vue";
import { onShow, onHide } from "@dcloudio/uni-app";
import { AMAP_WEB_KEY } from "@/config.js";
import request from "@/request.js";
import storage from "@/utils/storage.js";
import { getUserId } from "@/auth.js";
import {
  getTargetRoute,
  getRouteHash,
  isRouteChanged,
} from "@/utils/appState.js";
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

  // 重置播报按钮状态 - 这会将"取消播报"按钮变回"播报"
  console.log(
    "[播报结束] 重置播报状态: isSpeaking=",
    isSpeaking.value,
    ", isSpotSpeaking=",
    isSpotSpeaking.value,
  );

  // 使用setTimeout确保状态更新后UI能正确响应（在事件循环中异步更新）
  setTimeout(() => {
    isSpeaking.value = false;
    isSpotSpeaking.value = false;
    console.log(
      "[播报结束] 状态已重置: isSpeaking=",
      isSpeaking.value,
      ", isSpotSpeaking=",
      isSpotSpeaking.value,
    );
  }, 50);
};

const destroyDigitalHumanWebview = () => {
  console.log("========== 销毁数字人webview ==========");
  console.log("当前digitalHumanWebview:", digitalHumanWebview);

  if (digitalHumanWebview) {
    try {
      console.log("正在销毁数字人webview:", digitalHumanWebview.id);

      const isPreloaded =
        digitalHumanWebview.id === "preloadDigitalHuman" ||
        digitalHumanWebview.__uuid__ === "preloadDigitalHuman";

      if (isPreloaded) {
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
        if (digitalHumanWebview.hide) {
          console.log("步骤1: 隐藏webview");
          digitalHumanWebview.hide();
        }
        if (digitalHumanWebview.close) {
          console.log("步骤2: 关闭webview");
          digitalHumanWebview.close("none");
        } else if (digitalHumanWebview.parentNode) {
          console.log("步骤2: 移除DOM节点");
          digitalHumanWebview.parentNode.removeChild(digitalHumanWebview);
        }
      }
      digitalHumanWebview = null;
      console.log("✅ 数字人webview已销毁");
    } catch (e) {
      console.error("销毁数字人webview失败:", e);
    }
  }
};

// 数字人webview引用
let digitalHumanWebview = null;

// 商家详情
const merchantDetail = ref({
  name: "灵山胜境",
  introduction: "欢迎来到灵山胜境，我是您的智能导游",
});

// --- 基础配置 ---
const GATE_LOC = { longitude: 120.103637, latitude: 31.420211, x: 37.5, y: 88 }; // 灵山大门（含XY坐标）

// 景点列表（根据手绘地图图片调整的百分比位置 + 真实经纬度）
// x: 水平位置 0-100（从左到右）
// y: 垂直位置 0-100（从上到下）
// lng/lat: 真实GPS坐标
const SPOTS = [
  {
    name: "灵山大照壁",
    x: 38,
    y: 83,
    lng: 120.102499,
    lat: 31.421379,
    range: 0.0005,
    isEntryExit: false,
    image: "/static/spots/lsdzb.jpg",
  },
  {
    name: "五明桥",
    x: 38,
    y: 80,
    lng: 120.102258,
    lat: 31.421751,
    range: 0.0005,
    image: "/static/spots/wmq.jpg",
  },
  {
    name: "佛足坛",
    x: 38,
    y: 73,
    lng: 120.101488,
    lat: 31.422751,
    range: 0.0005,
    image: "/static/spots/fz.jpg",
  },
  {
    name: "五智门",
    x: 38,
    y: 70.5,
    lng: 120.101291,
    lat: 31.423057,
    range: 0.0005,
    image: "/static/spots/wzm.jpg",
  },
  {
    name: "菩提大道",
    x: 38,
    y: 66,
    lng: 120.10114,
    lat: 31.423187,
    range: 0.0005,
    image: "/static/spots/ptdd.jpg",
  },
  {
    name: "九龙灌浴",
    x: 35,
    y: 58,
    lng: 120.099984,
    lat: 31.424602,
    range: 0.0005,
    image: "/static/spots/jl.jpg",
  },
  {
    name: "降魔浮雕",
    x: 38,
    y: 51,
    lng: 120.099565,
    lat: 31.425558,
    range: 0.0005,
    image: "/static/spots/xm.png",
  },
  {
    name: "阿育王柱",
    x: 38,
    y: 47,
    lng: 120.099259,
    lat: 31.426185,
    range: 0.0005,
    image: "/static/spots/a.jpg",
  },
  {
    name: "百子戏弥勒",
    x: 42,
    y: 40,
    lng: 120.098845,
    lat: 31.427191,
    range: 0.0005,
    image: "/static/spots/b.jpg",
  },
  {
    name: "祥符禅寺",
    x: 38,
    y: 33,
    lng: 120.098008,
    lat: 31.42795,
    range: 0.0005,
    image: "/static/spots/xfcs.jpg",
  },
  {
    name: "灵山大佛",
    x: 38,
    y: 17,
    lng: 120.096475,
    lat: 31.4302,
    range: 0.0005,
    image: "/static/spots/lsdf.jpg",
  },
  {
    name: "佛教文化博览馆",
    x: 38,
    y: 20,
    lng: 120.096637,
    lat: 31.430015,
    range: 0.0005,
    image: "/static/spots/fj.jpg",
  },
  {
    name: "灵山梵宫",
    x: 74,
    y: 45,
    lng: 120.102415,
    lat: 31.42822,
    range: 0.0005,
    image: "/static/spots/lsfg.jpg",
  },
  {
    name: "五印坛城",
    x: 61,
    y: 66.5,
    lng: 120.103042,
    lat: 31.424668,
    range: 0.0005,
    image: "/static/spots/wytc.jpg",
  },
  {
    name: "曼飞龙塔",
    x: 81.5,
    y: 62.5,
    lng: 120.104601,
    lat: 31.426068,
    range: 0.0005,
    image: "/static/spots/mlft.jpg",
  },
  {
    name: "无尽意斋",
    x: 32,
    y: 25,
    lng: 120.096984,
    lat: 31.428761,
    range: 0.0005,
    image: "/static/spots/wjyz.jpg",
  },
  {
    name: "出口",
    x: 50,
    y: 74,
    lng: 120.102842,
    lat: 31.423104,
    range: 0.0005,
    isEntryExit: true,
  },
];

// --- 响应式数据 ---
const longitude = ref(GATE_LOC.longitude);
const latitude = ref(GATE_LOC.latitude);
const isGuiding = ref(false);
const displayText = ref(
  '我是您的导览助手，当前页面为景区地图，点击"获取位置"开始导览，点击景点获取景点文化内涵介绍，点击"推荐路线"或"收藏路线"查看路线并导览',
);

// 用户位置（百分比坐标）- 初始为无效位置，开始导览时再设置
const userX = ref(-100); // 初始位置在地图外（不显示）
const userY = ref(-100);

const showSpotModal = ref(false);

// 导览页播报开关
const isStreamSpeakEnabled = ref(true); // 默认开启
const isSpeaking = ref(false); // 是否正在播报（文字框）
const isSpotSpeaking = ref(false); // 是否正在播报（景点弹窗）
const guideStartTime = ref(null); // 导览开始时间（不显示在文字框）
const currentSpotDetail = ref({ name: "", description: "" });
const currentSpot = ref(null);
const visitedSpots = ref([]);

// 景点信息列表（从后端获取）
const attractionsInfo = ref([]);

// Canvas相关
const canvasWidth = ref(750);
const canvasHeight = ref(1200);

// 目标路线（收藏的路线，初始为空）
const targetRoute = ref([]);

// 路线坐标点（高德返回的经纬度数组）
const routePoints = ref([]);

// 经纬度范围（用于映射计算）
const MAP_BOUNDS = {
  minLng: 120.095,
  maxLng: 120.108,
  minLat: 31.418,
  maxLat: 31.433,
};

// 手动调参的坐标变换参数（高德地图 → 手绘地图）
const MAP_TRANSFORM = {
  rotation: 33, // 旋转度数（顺时针）
  centerX: 45.2, // 旋转中心X（百分比）
  centerY: 50, // 旋转中心Y（百分比）
  scaleX: 0.5, // X方向缩放（影响坐标转换，即景点标记位置）
  scaleY: 0.6, // Y方向缩放（影响坐标转换，即景点标记位置）
  offsetX: -22, // X方向平移
  offsetY: -22, // Y方向平移
  mapScaleX: 1, // 地图图片额外的X缩放（只影响地图显示，不影响标记位置）
  mapScaleY: 1.3, // 地图图片额外的Y缩放（只影响地图显示，不影响标记位置）
  mapOffsetX: 0, // 地图图片X方向偏移（像素，只影响地图显示）
  mapOffsetY: 0, // 地图图片Y方向偏移（像素，只影响地图显示）
};

// 角度转弧度
const degToRad = (deg) => (deg * Math.PI) / 180;

// 坐标变换（经纬度 → 手绘地图百分比坐标）
const lngLatToXYTransform = (lng, lat) => {
  // 1. 线性映射（经纬度 → 百分比坐标）
  let x =
    ((lng - MAP_BOUNDS.minLng) / (MAP_BOUNDS.maxLng - MAP_BOUNDS.minLng)) * 100;
  let y =
    100 -
    ((lat - MAP_BOUNDS.minLat) / (MAP_BOUNDS.maxLat - MAP_BOUNDS.minLat)) * 100;

  // 2. 应用仿射变换：平移→缩放→旋转→平移
  const { rotation, centerX, centerY, scaleX, scaleY, offsetX, offsetY } =
    MAP_TRANSFORM;
  const angleRad = degToRad(rotation);
  const cos = Math.cos(angleRad);
  const sin = Math.sin(angleRad);

  // 步骤1: 平移到旋转中心（原点）
  let dx = x - centerX;
  let dy = y - centerY;

  // 步骤2: 应用缩放（在旋转前）
  dx *= scaleX;
  dy *= scaleY;

  // 步骤3: 应用旋转变换
  const rotatedX = dx * cos - dy * sin;
  const rotatedY = dx * sin + dy * cos;

  // 步骤4: 平移回原位置并加上额外偏移
  x = rotatedX + centerX + offsetX;
  y = rotatedY + centerY + offsetY;

  return {
    x: Math.max(-30, Math.min(130, x)),
    y: Math.max(-30, Math.min(130, y)),
  };
};

// XY转经纬度（反向转换，用于调试）
const xyToLngLatTransform = (x, y) => {
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
  const lng =
    (x / 100) * (MAP_BOUNDS.maxLng - MAP_BOUNDS.minLng) + MAP_BOUNDS.minLng;
  const lat =
    MAP_BOUNDS.maxLat - (y / 100) * (MAP_BOUNDS.maxLat - MAP_BOUNDS.minLat);
  return { lng, lat };
};

// 当前路线索引
const currentRouteIndex = ref(0);

// 地图缩放相关
const mapScale = ref(1);
const minScale = 1; // 最小缩放为原始大小（1倍），不能比进来时更小
const maxScale = 3;

// 地图平移相关
const offsetX = ref(0);
const offsetY = ref(0);

// 触摸相关变量
let touchStartX = 0;
let touchStartY = 0;
let lastTouchDistance = 0;
let lastTouchCenterX = 0;
let lastTouchCenterY = 0;
let isPinching = false;
let isPanning = false;

// PDR相关变量
let sensorTimer = null;
let lastAccel = { x: 0, y: 0, z: 0 };
let lastAccelTime = 0;
let stepCount = 0;
let lastStepTime = 0;
const STEP_THRESHOLD = 0.8; // 步检测阈值（降低阈值提高灵敏度）
const STEP_INTERVAL = 250; // 步间隔（毫秒）
const STEP_LENGTH = 0.00005; // 步长（经纬度变化），约5-6米
const MIN_STEP_INTERVAL = 150; // 最小步间隔（防止高频误检测）

const currentHeading = ref(0);
let hasCompass = false;
let lastValidHeading = 0; // 保存最后有效的罗盘方向，用于无罗盘时保持方向
let lastStepHeading = -1; // 上一步的移动方向，用于防止方向突变（-1表示未初始化）
let isTracking = false; // 位置跟踪状态标记

// --- 坐标映射函数 ---

// 经纬度转XY百分比坐标（应用旋转变换）
const lngLatToXY = (lng, lat) => {
  const result = lngLatToXYTransform(lng, lat);
  return result;
};

// XY百分比坐标转经纬度（应用反向旋转变换）
const xyToLngLat = (x, y) => {
  const result = xyToLngLatTransform(x, y);
  return result;
};

// --- 高德地图路线规划 ---

// 延时函数
const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

// 单个路段的路线请求（支持限流重试）
const requestSingleRoute = async (from, to, segmentIndex) => {
  const maxRetries = 1; // 只重试1次
  const retryDelay = 1000; // 重试延迟（毫秒）

  const params = {
    origin: `${from.lng},${from.lat}`,
    destination: `${to.lng},${to.lat}`,
    key: AMAP_WEB_KEY,
    output: "json",
  };

  for (let retry = 0; retry <= maxRetries; retry++) {
    const res = await uni.request({
      url: "https://restapi.amap.com/v3/direction/walking",
      method: "GET",
      data: params,
    });

    if (res.statusCode === 200 && res.data.status === "1") {
      return res.data.route.paths[0];
    }

    const errorInfo = res.data?.info || "";

    // 检查是否是限流错误
    if (errorInfo.includes("CUQPS") || errorInfo.includes("EXCEEDED")) {
      if (retry < maxRetries) {
        console.warn(
          `路段 ${segmentIndex} 遇到限流，等待 ${retryDelay}ms 后重试`,
        );
        await delay(retryDelay);
        continue; // 重试
      } else {
        console.error(`路段 ${segmentIndex} 重试后仍失败，使用直线连接`);
        return null;
      }
    } else {
      // 非限流错误，直接返回失败
      console.error(`路段 ${segmentIndex} 规划失败:`, errorInfo);
      return null;
    }
  }

  return null;
};

// 使用高德地图 Web API 获取步行路线（分段规划确保途经点被经过）
const getRouteFromGaode = async (spots) => {
  // 过滤掉灵山大门和出口，只保留有效景点
  const validSpots = spots.filter(
    (spot) => spot !== "灵山大门" && spot !== "出口",
  );

  if (validSpots.length < 2) {
    uni.showToast({ title: "至少需要 2 个有效景点", icon: "none" });
    return;
  }

  try {
    // 更新 targetRoute 为过滤后的景点列表
    targetRoute.value = validSpots;

    // 获取所有景点的经纬度信息
    const spotCoords = validSpots
      .map((spotName) => {
        const spot = SPOTS.find((s) => s.name === spotName);
        return spot ? { name: spot.name, lng: spot.lng, lat: spot.lat } : null;
      })
      .filter((p) => p !== null);

    // 分段规划路线：每两个相邻景点之间单独请求路线
    // 步行API不支持途经点，必须逐个路段请求才能确保经过所有景点
    const allPoints = [];
    let failedSegments = 0; // 记录失败的路段数

    uni.showLoading({ title: "正在规划路线..." });

    for (let i = 0; i < spotCoords.length - 1; i++) {
      const from = spotCoords[i];
      const to = spotCoords[i + 1];

      console.log(`规划路段 ${i + 1}：${from.name} -> ${to.name}`);

      // 请求路线（带重试机制）
      const path = await requestSingleRoute(from, to, i + 1);

      if (path && path.steps) {
        path.steps.forEach((step) => {
          if (step.polyline) {
            const coordPairs = step.polyline.split(";");
            coordPairs.forEach((pair) => {
              const [lng, lat] = pair.split(",").map(Number);
              // 避免重复添加相邻路段的连接点
              if (
                allPoints.length === 0 ||
                !(
                  allPoints[allPoints.length - 1].lng === lng &&
                  allPoints[allPoints.length - 1].lat === lat
                )
              ) {
                allPoints.push({ lng, lat });
              }
            });
          }
        });
      } else {
        // 路线请求失败，用直线连接
        failedSegments++;
        if (
          allPoints.length === 0 ||
          !(
            allPoints[allPoints.length - 1].lng === from.lng &&
            allPoints[allPoints.length - 1].lat === from.lat
          )
        ) {
          allPoints.push({ lng: from.lng, lat: from.lat });
        }
        allPoints.push({ lng: to.lng, lat: to.lat });
      }

      // 两次请求之间加延时（200ms），避免触发限流
      if (i < spotCoords.length - 2) {
        await delay(200);
      }
    }

    uni.hideLoading();

    if (allPoints.length > 0) {
      routePoints.value = allPoints;

      // 延迟绘制，确保 Canvas 已初始化
      setTimeout(() => {
        drawRouteOnMap();
      }, 100);

      if (failedSegments > 0) {
        uni.showToast({
          title: `路线规划完成（${failedSegments}个路段因限流重试失败，已用直线连接）`,
          icon: "none",
          duration: 3000,
        });
      } else {
        uni.showToast({ title: "路线规划成功", icon: "success" });
      }
    } else {
      uni.showToast({ title: "路线规划失败", icon: "none" });
    }
  } catch (error) {
    uni.hideLoading();
    console.error("路线规划异常:", error);
    uni.showToast({ title: "路线规划异常", icon: "none", duration: 3000 });
  }
};

// ... existing code ...

// 在地图上渲染路线
const drawRouteOnMap = () => {
  if (routePoints.value.length < 2) {
    return;
  }

  const ctx = uni.createCanvasContext("routeCanvas");
  if (!ctx) {
    console.error("[drawRouteOnMap] Canvas上下文获取失败！");
    return;
  }

  // 清空画布
  ctx.clearRect(0, 0, canvasWidth.value, canvasHeight.value);

  // 将坐标转换为画布坐标
  const canvasPoints = routePoints.value.map((point) => {
    let x, y;
    if (point.lng !== undefined && point.lat !== undefined) {
      // 经纬度转百分比坐标（应用仿射变换）
      const xy = lngLatToXY(point.lng, point.lat);
      x = xy.x;
      y = xy.y;
    } else if (point.x !== undefined && point.y !== undefined) {
      // 直接使用百分比坐标
      x = point.x;
      y = point.y;
    } else {
      x = 0;
      y = 0;
    }
    return {
      x: (x / 100) * canvasWidth.value,
      y: (y / 100) * canvasHeight.value,
    };
  });

  // 绘制路线
  ctx.beginPath();
  ctx.setStrokeStyle("#4a90e2");
  ctx.setLineWidth(6);
  ctx.setLineCap("round");
  ctx.setLineJoin("round");

  ctx.moveTo(canvasPoints[0].x, canvasPoints[0].y);

  // 依次连接所有景点坐标点
  for (let i = 1; i < canvasPoints.length; i++) {
    ctx.lineTo(canvasPoints[i].x, canvasPoints[i].y);
  }
  ctx.stroke();

  // 绘制路线光晕效果
  ctx.beginPath();
  ctx.setStrokeStyle("rgba(74, 144, 226, 0.3)");
  ctx.setLineWidth(2);
  ctx.setLineCap("round");
  ctx.setLineJoin("round");
  ctx.moveTo(canvasPoints[0].x, canvasPoints[0].y);
  for (let i = 1; i < canvasPoints.length; i++) {
    ctx.lineTo(canvasPoints[i].x, canvasPoints[i].y);
  }
  ctx.stroke();

  // 绘制起点和终点标记
  // 起点（绿色）
  const startPoint = canvasPoints[0];
  ctx.beginPath();
  ctx.setFillStyle("#52c41a");
  ctx.arc(startPoint.x, startPoint.y, 5, 0, Math.PI * 2);
  ctx.fill();
  ctx.beginPath();
  ctx.setFillStyle("#fff");
  ctx.arc(startPoint.x, startPoint.y, 2.5, 0, Math.PI * 2);
  ctx.fill();

  // 终点（红色）
  const endPoint = canvasPoints[canvasPoints.length - 1];
  ctx.beginPath();
  ctx.setFillStyle("#ff4d4f");
  ctx.arc(endPoint.x, endPoint.y, 5, 0, Math.PI * 2);
  ctx.fill();
  ctx.beginPath();
  ctx.setFillStyle("#fff");
  ctx.arc(endPoint.x, endPoint.y, 2.5, 0, Math.PI * 2);
  ctx.fill();

  // 绘制到画布
  ctx.draw();
  console.log("========== 路线绘制完成 ==========");
};

// 调用高德API获取景点经纬度并打印到控制台
const fetchSpotsLngLat = async () => {
  console.log("========== 获取景点经纬度 ==========");

  // 过滤掉灵山大门和出口
  const filteredSpots = SPOTS.filter(
    (spot) => spot.name !== "灵山大门" && spot.name !== "出口",
  );

  console.log("待获取经纬度的景点数量:", filteredSpots.length);

  filteredSpots.forEach((spot) => {
    console.log(`${spot.name}: lng=${spot.lng}, lat=${spot.lat}`);
  });

  console.log("========== 景点经纬度获取完成 ==========");
};

// 加载路线并获取高德路线
const loadRouteAndGetPath = async (routeSpots) => {
  console.log("========== 加载收藏路线 ==========");
  console.log("路线景点列表:", routeSpots);

  if (routeSpots && routeSpots.length > 0) {
    console.log("开始获取高德路线...");
    await getRouteFromGaode(routeSpots);
  } else {
    console.log("路线景点列表为空");
  }
  console.log("========== 加载收藏路线结束 ==========");
};

// --- 预留初始化函数 ---
const initMarkers = () => {
  // 预留：初始化标记点
};

const initMapping = () => {
  // 预留：初始化坐标映射
};

// 加载持久化状态
const loadSavedState = () => {
  const savedLoc = storage.getCurrentLocation();
  if (savedLoc) {
    longitude.value = savedLoc.longitude;
    latitude.value = savedLoc.latitude;
  }

  isGuiding.value = storage.getIsGuiding();
  if (isGuiding.value) {
    displayText.value = "导览进行中...";
    // 将经纬度转换为地图坐标，显示小蓝点
    const pos = lngLatToXY(longitude.value, latitude.value);
    userX.value = pos.x;
    userY.value = pos.y;
    startLocationTracking();
  }
};

// 获取所有景点信息
const loadAttractionsInfo = async () => {
  try {
    const res = await request({
      url: "/user/attractionsInfo",
      method: "GET",
    });

    if (res.code === 200 && res.data) {
      attractionsInfo.value = res.data;
      console.log(
        "[景点信息] 加载成功，共",
        attractionsInfo.value.length,
        "个景点",
      );
    }
  } catch (e) {
    console.error("加载景点信息失败:", e);
  }
};

// 获取地图内部样式（包含缩放和平移）
const getMapInnerStyle = () => {
  return {
    transform: `translate(${offsetX.value}px, ${offsetY.value}px) scale(${mapScale.value})`,
    transformOrigin: "center center",
    transition: isPanning || isPinching ? "none" : "transform 0.2s ease-out",
  };
};

// 获取地图图片样式（应用额外的缩放和偏移）
const getMapImageStyle = () => {
  return {
    transform: `translate(${MAP_TRANSFORM.mapOffsetX}px, ${MAP_TRANSFORM.mapOffsetY}px) scaleX(${MAP_TRANSFORM.mapScaleX}) scaleY(${MAP_TRANSFORM.mapScaleY})`,
    transformOrigin: "center center",
  };
};

// --- 讯飞数字人Webview创建逻辑 ---
const createDigitalHumanWebview = () => {
  try {
    if (digitalHumanWebview) {
      console.log("⚠️ 数字人webview已存在，跳过创建");
      return;
    }

    if (typeof plus === "undefined" || !plus.webview) {
      createDigitalHumanIframe();
      return;
    }

    setTimeout(() => {
      try {
        // 在setTimeout回调中再次检查plus是否存在（H5环境中plus未定义）
        if (typeof plus === "undefined" || !plus.webview) {
          createDigitalHumanIframe();
          return;
        }

        const pages = getCurrentPages();
        const currentPage = pages[pages.length - 1];

        let currentWebview = null;

        if (currentPage.$page && currentPage.$page.getWebview) {
          currentWebview = currentPage.$page.getWebview();
        }
        if (!currentWebview && plus.webview.currentWebview) {
          currentWebview = plus.webview.currentWebview();
        }

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
          createDigitalHumanIframe();
          return;
        }

        if (currentWebview && plus.webview) {
          uni
            .createSelectorQuery()
            .select(".avatar-section")
            .boundingClientRect((rect) => {
              if (!rect) {
                return;
              }

              const systemInfo = uni.getSystemInfoSync();

              const width = 196;
              const height = 275;
              const left = -85;
              const top = -50; // 向上移动10rpx

              const htmlPath = "_www/static/xfyun-digital-human/web-api.html";
              const preloadedWebview = plus.webview.getWebviewById(
                "preloadDigitalHuman",
              );

              if (preloadedWebview) {
                digitalHumanWebview = preloadedWebview;

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
                  zindex: 1,
                  hardwareAccelerated: true,
                  ignoretouch: false,
                  hitTest: true,
                  pointerEvents: "auto",
                });

                // 切换到半身模式（确保是半身模式，因为首页可能切换成了全身模式）
                setTimeout(() => {
                  digitalHumanWebview.evalJS("window.switchToHalfBody()");
                }, 200);
              } else {
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
                    zindex: 1,
                    popGesture: "none",
                    hardwareAccelerated: true,
                    ignoretouch: false,
                    hitTest: true,
                    pointerEvents: "none",
                  },
                );
              }

              digitalHumanWebview.addEventListener("message", (e) => {
                console.log("[webview 消息] 原始消息:", e);
                console.log("[webview 消息] e.data:", e.data);
                console.log("[webview 消息] e.detail:", e.detail);

                // 处理可能的消息格式：
                // uni-app webview的message事件数据在e.detail.data中（数组格式）
                // 也可能是标准的e.data格式
                let data = null;
                const eventData = e.detail ? e.detail.data : e.data;

                console.log("[webview 消息] eventData:", eventData);

                if (eventData) {
                  if (Array.isArray(eventData) && eventData.length > 0) {
                    data = eventData[0];
                  } else if (typeof eventData === "object" && eventData.type) {
                    // 直接是对象格式
                    data = eventData;
                  } else if (typeof eventData === "string") {
                    // 可能是JSON字符串
                    try {
                      const parsed = JSON.parse(eventData);
                      if (Array.isArray(parsed) && parsed.length > 0) {
                        data = parsed[0];
                      } else if (typeof parsed === "object" && parsed.type) {
                        data = parsed;
                      }
                    } catch (err) {
                      console.log("[webview 消息] JSON解析失败:", err);
                    }
                  }
                }

                console.log("[webview 消息] 解析后data:", data);
                if (data) {
                  console.log("[webview 消息] 收到消息:", data);

                  if (data.type === "ready") {
                    isDigitalHumanLoading.value = false;
                  } else if (data.type === "speakEnd") {
                    console.log("[webview 消息] 收到speakEnd消息");
                  } else if (data.type === "updateText") {
                    displayText.value = data.text;
                  }
                }
              });

              digitalHumanWebview.addEventListener("error", () => {});

              digitalHumanWebview.show();

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

          return;
        }
      } catch (e) {
        console.error("创建数字人webview失败:", e);
      }
    }, 500);
  } catch (e) {
    console.error("创建数字人webview失败:", e);
  }
};

// 让数字人说话
const speak = (text) => {
  if (!isDigitalHumanLoading.value) {
    sendMessageToWebView({ type: "speak", text: text });
    isSpeaking.value = true;
  }
};

// 停止说话
const stopSpeak = () => {
  sendMessageToWebView({ type: "stop" });
};

// 过滤函数：只保留纯文本，用于数字人播报
const filterPureText = (content) => {
  if (!content) return "";

  let text = content;

  // 过滤特殊标记
  text = text.replace(/\[\[.*?\]\]/gi, "");
  text = text.replace(/\[.*?\]/g, "");

  // 过滤多余空白
  text = text.replace(/\s+/g, " ").trim();

  return text;
};

// 手动点击播报按钮
const playVoice = () => {
  // 如果正在播报，点击后取消播报
  if (isSpeaking.value) {
    console.log("[手动播报] 取消播报");
    resetDigitalHuman();
    isSpeaking.value = false;
    uni.showToast({ title: "已取消播报", icon: "none" });
    return;
  }

  console.log("[手动播报] 点击播报按钮，当前文字:", displayText.value);

  if (!digitalHumanWebview) {
    console.error("[手动播报] ❌ 数字人webview不存在");
    uni.showToast({ title: "数字人未就绪", icon: "none" });
    return;
  }

  const pureText = filterPureText(displayText.value);
  console.log("[手动播报] 过滤后内容:", pureText);

  if (!pureText) {
    console.log("[手动播报] 过滤后无内容");
    uni.showToast({ title: "无播报内容", icon: "none" });
    return;
  }

  // 发送完整内容给数字人
  try {
    const escapedText = JSON.stringify(pureText);
    digitalHumanWebview.evalJS(`window.sendTextInteract(${escapedText})`);
    isSpeaking.value = true;
    uni.showToast({ title: "开始播报", icon: "none" });
    console.log("[手动播报] ✅ 完整内容已发送给数字人");
  } catch (e) {
    console.error("[手动播报] ❌ 发送失败:", e);
    uni.showToast({ title: "播报失败", icon: "none" });
  }
};

// 切换流式播报开关
const toggleStreamSpeak = () => {
  const newState = !isStreamSpeakEnabled.value;
  isStreamSpeakEnabled.value = newState;

  // 持久化存储
  storage.setGuideCompanionSpeakEnabled(newState);

  // 如果关闭播报且当前正在播报，重置虚拟人
  if (!newState && isSpeaking.value) {
    console.log("[导览播报] 开关关闭，重置虚拟人");
    resetDigitalHuman();
  }

  console.log("[导览播报] 开关状态:", newState ? "开启" : "关闭");
};

// 重置虚拟人（恢复静默推流状态）
const resetDigitalHuman = () => {
  if (digitalHumanWebview) {
    try {
      digitalHumanWebview.evalJS("window.resetAvatar && window.resetAvatar()");
      console.log("[数字人] 已发送重置指令");
    } catch (e) {
      console.error("[数字人] 重置失败:", e);
    }
  }
  isSpeaking.value = false;
  isSpotSpeaking.value = false;
};

// 播放景点详情播报
const playSpotVoice = () => {
  // 如果正在播报，点击后取消播报
  if (isSpotSpeaking.value) {
    console.log("[景点播报] 取消播报");
    resetDigitalHuman();
    uni.showToast({ title: "已取消播报", icon: "none" });
    return;
  }

  if (!digitalHumanWebview) {
    console.error("[景点播报] ❌ 数字人webview不存在");
    uni.showToast({ title: "数字人未就绪", icon: "none" });
    return;
  }

  // 只传播报文化内涵（description）
  const pureText = filterPureText(currentSpotDetail.value.description);
  console.log("[景点播报] 过滤后内容:", pureText);

  if (!pureText) {
    console.log("[景点播报] 过滤后无内容");
    uni.showToast({ title: "无播报内容", icon: "none" });
    return;
  }

  try {
    const escapedText = JSON.stringify(pureText);
    digitalHumanWebview.evalJS(`window.sendTextInteract(${escapedText})`);
    isSpotSpeaking.value = true;
    uni.showToast({ title: "开始播报", icon: "none" });
    console.log("[景点播报] ✅ 内容已发送给数字人");
  } catch (e) {
    console.error("[景点播报] ❌ 发送失败:", e);
    uni.showToast({ title: "播报失败", icon: "none" });
  }
};

// 监听displayText变化，自动播报
watch(displayText, (newText) => {
  if (!newText || !isStreamSpeakEnabled.value) {
    return;
  }

  // 过滤纯文本
  const pureText = filterPureText(newText);
  if (!pureText) {
    return;
  }

  // 如果文字内容包含关键词（到达、继续寻找等），自动播报
  // 另外检测是否包含景点名称（说明是讲解内容）
  const shouldAutoSpeak =
    pureText.includes("到达") ||
    pureText.includes("继续寻找") ||
    SPOTS.some((spot) => pureText.includes(spot.name));

  if (shouldAutoSpeak && digitalHumanWebview) {
    console.log(
      "[导览自动播报] 检测到需要播报的内容:",
      pureText.substring(0, 30) + "...",
    );

    // 立即重置数字人（打断当前播报），然后立即开始新播报
    try {
      // 发送重置指令（会停止当前播报）
      digitalHumanWebview.evalJS("window.resetAvatar && window.resetAvatar()");
      // 立即发送新内容（不需要等待，数字人SDK会处理）
      const escapedText = JSON.stringify(pureText);
      digitalHumanWebview.evalJS(`window.sendTextInteract(${escapedText})`);
      isSpeaking.value = true;
      console.log("[导览自动播报] ✅ 已发送播报指令（打断并开始新播报）");
    } catch (e) {
      console.error("[导览自动播报] ❌ 发送失败:", e);
    }
  }
});

// H5环境创建数字人iframe
const createDigitalHumanIframe = () => {
  try {
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

    window.addEventListener("message", (e) => {
      console.log(
        "[iframe 消息] ==================== 收到消息 ====================",
      );
      console.log("[iframe 消息] 原始事件对象:", e);
      console.log("[iframe 消息] e.data:", e.data);
      console.log("[iframe 消息] e.data类型:", typeof e.data);
      console.log("[iframe 消息] e.origin:", e.origin);

      // 处理可能的消息格式
      let data = null;
      if (e.data) {
        if (Array.isArray(e.data) && e.data.length > 0) {
          data = e.data[0];
          console.log("[iframe 消息] 数组格式，取第一个元素:", data);
        } else if (typeof e.data === "object" && e.data.type) {
          data = e.data;
          console.log("[iframe 消息] 对象格式，直接使用:", data);
        } else if (typeof e.data === "string") {
          console.log("[iframe 消息] 字符串格式，尝试解析:", e.data);
          try {
            const parsed = JSON.parse(e.data);
            if (Array.isArray(parsed) && parsed.length > 0) {
              data = parsed[0];
            } else if (typeof parsed === "object" && parsed.type) {
              data = parsed;
            }
            console.log("[iframe 消息] JSON解析成功:", data);
          } catch (err) {
            console.log("[iframe 消息] JSON解析失败:", err);
          }
        }
      }

      console.log("[iframe 消息] 解析后data:", data);

      if (data) {
        console.log("[iframe 消息] data.type:", data.type);

        if (data.type === "ready") {
          isDigitalHumanLoading.value = false;
          console.log("[iframe 消息] 数字人准备就绪");
        } else if (data.type === "speakEnd") {
          console.log("[iframe 消息] ======== 检测到speakEnd ========");
          console.log(
            "[iframe 消息] 当前isSpotSpeaking值:",
            isSpotSpeaking.value,
          );
          console.log("[iframe 消息] 当前isSpeaking值:", isSpeaking.value);

          // 直接在这里更新状态（不通过handleSpeakEnd）
          isSpotSpeaking.value = false;
          isSpeaking.value = false;

          console.log("[iframe 消息] 状态已直接更新");
          console.log(
            "[iframe 消息] 更新后isSpotSpeaking值:",
            isSpotSpeaking.value,
          );
          console.log("[iframe 消息] 更新后isSpeaking值:", isSpeaking.value);
        } else if (data.type === "updateText") {
          displayText.value = data.text;
        }
      } else {
        console.log("[iframe 消息] data为空，无法处理");
      }
    });

    document.body.appendChild(iframe);
    digitalHumanWebview = iframe;
    console.log("数字人iframe创建成功");
  } catch (e) {
    console.error("创建数字人iframe失败:", e);
  }
};

// 发送消息给webview
const sendMessageToWebView = (message) => {
  if (digitalHumanWebview) {
    const script = `window.postMessage(${JSON.stringify(message)});`;
    digitalHumanWebview.evalJS(script);
  }
};

// 向webview发送商家信息
/*const sendMerchantInfoToWebView = () => {
  if (digitalHumanWebview) {
    const msgData = JSON.stringify({
      type: "setMerchantInfo",
      data: {
        merchantName: merchantDetail.value.name,
        introduction: merchantDetail.value.introduction,
      },
    });
    const script = `window.postMessage(${JSON.stringify(msgData)});`;
    digitalHumanWebview.evalJS(script);
  }
};*/

// --- 生命周期 ---
let currentRouteHash = null;

onMounted(() => {
  // 加载导览页播报开关状态
  isStreamSpeakEnabled.value = storage.getGuideCompanionSpeakEnabled();
  console.log(
    "[导览页] 播报开关状态:",
    isStreamSpeakEnabled.value ? "开启" : "关闭",
  );

  loadSavedState();
  initMarkers();
  initMapping();

  // 创建数字人webview
  createDigitalHumanWebview();

  // 调试：打印初始位置
  console.log(
    `[DEBUG] 初始用户位置: userX=${userX.value}, userY=${userY.value}`,
  );
  console.log(`[DEBUG] GATE_LOC: ${JSON.stringify(GATE_LOC)}`);

  // 加载景点信息
  loadAttractionsInfo();

  // 获取景点经纬度并打印到控制台（除灵山大门和出口）
  fetchSpotsLngLat();

  // 从全局状态读取路线
  const savedRoute = getTargetRoute();
  if (savedRoute && savedRoute.length > 0) {
    currentRouteHash = getRouteHash();
    console.log("[导览页] 从全局状态获取路线:", savedRoute);
    loadRouteAndGetPath(savedRoute);
  }
});

onShow(() => {
  console.log("========== 页面显示 ==========");

  // 检查路线是否变化
  const savedRoute = getTargetRoute();
  if (savedRoute && savedRoute.length > 0) {
    if (isRouteChanged(savedRoute)) {
      console.log("[导览页] 检测到新路线，开始加载...");
      currentRouteHash = getRouteHash();
      loadRouteAndGetPath(savedRoute);
    } else {
      console.log("[导览页] 路线未变化，跳过加载");
    }
  }

  // 如果正在导览，恢复位置跟踪
  if (isGuiding.value && !isTracking) {
    console.log("恢复位置跟踪");
    startLocationTracking();
  }

  console.log("========== 页面显示结束 ==========");
});

// 页面隐藏时不停止跟踪（黑屏/小窗时继续检测）
onHide(() => {
  console.log("========== 页面隐藏 ==========");
  console.log(`导览状态: ${isGuiding.value}, 跟踪状态: ${isTracking}`);

  // 退出页面时重置播报状态
  resetDigitalHuman();
  destroyDigitalHumanWebview();
});

onUnmounted(() => {
  // 不停止位置跟踪，继续在后台检测
  console.log("========== 页面卸载 ==========");
  console.log(`导览状态: ${isGuiding.value}, 继续后台跟踪`);
  // 退出页面时重置播报状态
  resetDigitalHuman();
  destroyDigitalHumanWebview();
  // 存储当前位置
  storage.setCurrentLocation({
    longitude: longitude.value,
    latitude: latitude.value,
  });
});

// --- 核心逻辑 ---

// 1. 切换导览状态（开始/取消）
const toggleGuide = () => {
  if (!isGuiding.value) {
    // 开始导览 - 初始化位置为灵山大照壁
    isGuiding.value = true;
    storage.setIsGuiding(true);

    // 获取灵山大照壁的坐标
    const daxiaobi = SPOTS.find((s) => s.name === "灵山大照壁");
    if (daxiaobi) {
      // 使用经纬度转XY的逻辑计算初始位置（带校准）
      longitude.value = daxiaobi.lng;
      latitude.value = daxiaobi.lat;
      const pos = lngLatToXY(longitude.value, latitude.value);
      userX.value = pos.x;
      userY.value = pos.y;

      // 初始化当前所在景点为灵山大照壁
      currentSpot.value = "灵山大照壁";
      displayText.value = `到达灵山大照壁，点击文字框获取讲解`;

      const now = new Date().toISOString();
      storage.addVisitRecord({
        spotName: "灵山大照壁",
        startTime: now,
        endTime: null,
      });
      visitedSpots.value.push({
        spotName: "灵山大照壁",
        startTime: now,
        endTime: null,
      });
    } else {
      // 没有灵山大照壁时使用大门口
      longitude.value = GATE_LOC.longitude;
      latitude.value = GATE_LOC.latitude;
      userX.value = GATE_LOC.x;
      userY.value = GATE_LOC.y;
    }

    storage.setCurrentLocation({
      longitude: longitude.value,
      latitude: latitude.value,
    });

    // 记录灵山大照壁起始时间
    const startTime = new Date().toISOString();
    storage.addVisitRecord({
      spotName: "灵山大照壁",
      startTime,
      endTime: null,
    });
    visitedSpots.value.push({
      spotName: "灵山大照壁",
      startTime,
      endTime: null,
    });

    // 日志记录：开始导览
    console.log("=== 开始导览 ===");
    console.log("时间:", startTime);
    console.log("起点:", "灵山大照壁");
    console.log(
      "经纬度:",
      `${longitude.value.toFixed(6)}, ${latitude.value.toFixed(6)}`,
    );
    console.log("=================");

    // 保存导览开始时间（不显示在文字框）
    guideStartTime.value = startTime;
    displayText.value = `到达灵山大照壁，点击文字框获取讲解`;
    uni.showToast({ title: "开始导览", icon: "success" });

    // 延迟1秒后启动PDR，避免点击按钮的动作被误判为步数
    setTimeout(() => {
      startLocationTracking();
    }, 1000);
  } else {
    // 取消导览 -> 弹窗询问是否生成游记
    stopLocationTracking();
    uni.showModal({
      title: "结束导览",
      content: "是否需要生成游记？",
      confirmText: "生成游记",
      cancelText: "取消",
      success: async (res) => {
        if (res.confirm) {
          await generateTravelogue();
        }
        storage.clearAllGuideData();
        isGuiding.value = false;
        currentSpot.value = null;
        visitedSpots.value = [];
        displayText.value = "导览已结束";
      },
    });
  }
};

// 3. PDR位置追踪（使用加速度传感器模拟）
const startLocationTracking = () => {
  // 初始化PDR变量
  stepCount = 0;
  lastStepTime = 0;
  currentHeading.value = 0;
  isFirstAccel = true; // 重置第一个加速度数据标记
  isTracking = true; // 标记开始跟踪
  hasCompass = false; // 重置罗盘状态
  lastValidHeading = 0; // 重置最后有效方向
  lastStepHeading = -1; // 重置上一步方向（未初始化状态）

  // 尝试获取罗盘方向
  uni.startCompass({
    fail: (err) => {
      console.error("[传感器] 罗盘启动失败:", err);
    },
  });

  uni.onCompassChange((res) => {
    if (res.direction > 0 && res.direction <= 360) {
      // 计算角度差（考虑360度环绕）
      let diff = res.direction - currentHeading.value;
      if (diff > 180) diff -= 360;
      if (diff < -180) diff += 360;

      // 限制单次最大变化角度（防止罗盘噪声导致方向突变，但允许正常转向）
      const maxChange = 30; // 单次最大变化不超过30度
      if (diff > maxChange) diff = maxChange;
      if (diff < -maxChange) diff = -maxChange;

      // 适度的指数平滑（既能过滤噪声，又能响应正常转向）
      currentHeading.value =
        currentHeading.value * 0.7 + (currentHeading.value + diff) * 0.3;

      // 确保在0-360范围内
      currentHeading.value = ((currentHeading.value % 360) + 360) % 360;

      lastValidHeading = currentHeading.value;
      hasCompass = true;
    }
  });

  // 启动加速度传感器
  uni.startAccelerometer({
    interval: "game",
    fail: (err) => {
      console.error("[传感器] 加速度传感器启动失败:", err);
    },
  });

  uni.onAccelerometerChange(onAccelerometerChange);

  uni.showToast({
    title: "PDR追踪已启动，请走动",
    icon: "none",
    duration: 2000,
  });
};

// PDR步检测与位置更新
let isFirstAccel = true; // 标记是否是第一个加速度数据
let lastAccelMag = 0; // 上一次的加速度幅度
let debugCount = 0;
let accelHistory = []; // 存储最近的加速度数据用于步峰检测
const HISTORY_SIZE = 8; // 历史数据大小

const onAccelerometerChange = (res) => {
  const now = Date.now();

  // 检查时间间隔，避免高频数据处理
  if (lastAccelTime > 0 && now - lastAccelTime < 30) {
    return; // 忽略高频数据
  }
  lastAccelTime = now;

  // 第一次加速度数据用于初始化，不进行步检测
  if (isFirstAccel) {
    lastAccel = { x: res.x, y: res.y, z: res.z };
    lastAccelMag = Math.sqrt(res.x * res.x + res.y * res.y + res.z * res.z);
    isFirstAccel = false;
    console.log(
      `[PDR] 加速度传感器初始化完成，初始幅度: ${lastAccelMag.toFixed(2)}`,
    );
    return;
  }

  // 计算当前加速度幅度
  const currentMag = Math.sqrt(res.x * res.x + res.y * res.y + res.z * res.z);

  // 计算三轴加速度的变化（区分旋转和走路）
  const deltaX = Math.abs(res.x - lastAccel.x);
  const deltaY = Math.abs(res.y - lastAccel.y);
  const deltaZ = Math.abs(res.z - lastAccel.z);

  // 走路时主要是垂直方向（Z轴）的冲击，旋转时主要是水平方向（X/Y轴）的变化
  const verticalRatio = deltaZ / (deltaX + deltaY + deltaZ + 0.001);

  // 更新历史数据
  lastAccelMag = currentMag;
  lastAccel = { x: res.x, y: res.y, z: res.z };

  // 保存到历史记录
  accelHistory.push({ time: now, mag: currentMag, deltaZ: deltaZ });
  if (accelHistory.length > HISTORY_SIZE) {
    accelHistory.shift();
  }

  // 检查时间间隔
  const timeSinceLastStep = now - lastStepTime;

  // 智能步检测：
  // 1. 加速度变化超过阈值
  // 2. 垂直方向变化占比超过50%（主要是Z轴冲击，不是水平旋转）
  // 3. 检测到明显的步峰模式（先加速后减速）
  const hasStepPattern = detectStepPattern();

  if (
    deltaZ > STEP_THRESHOLD * 0.8 &&
    verticalRatio > 0.4 &&
    timeSinceLastStep > MIN_STEP_INTERVAL &&
    hasStepPattern
  ) {
    lastStepTime = now;
    stepCount++;

    console.log(
      `[PDR] 检测到步数: ${stepCount}, deltaZ: ${deltaZ.toFixed(2)}, 方向: ${currentHeading.value.toFixed(1)}°`,
    );

    // 根据朝向计算新位置
    updatePositionByStep();
  }
};

// 检测步峰模式：走路时会有明显的"冲击-恢复"模式
const detectStepPattern = () => {
  if (accelHistory.length < HISTORY_SIZE) {
    return true; // 数据不足时默认认为可能是步
  }

  // 计算最近数据的Z轴变化平均值
  const avgDeltaZ =
    accelHistory.reduce((sum, item) => sum + item.deltaZ, 0) /
    accelHistory.length;

  // 检查是否有明显的峰值模式：先上升后下降
  let hasPeak = false;
  for (let i = 1; i < accelHistory.length - 1; i++) {
    const prev = accelHistory[i - 1].deltaZ;
    const curr = accelHistory[i].deltaZ;
    const next = accelHistory[i + 1].deltaZ;

    // 检测峰值：当前值大于前后值，且超过平均值的1.5倍
    if (curr > prev && curr > next && curr > avgDeltaZ * 1.5) {
      hasPeak = true;
      break;
    }
  }

  return hasPeak;
};

// 根据步长和方向更新位置
const updatePositionByStep = () => {
  // 获取当前方向（使用平滑后的罗盘方向）
  let adjustedHeading = currentHeading.value;

  // 确保方向在0-360范围内
  adjustedHeading = ((adjustedHeading % 360) + 360) % 360;

  // 将方向转换为前进方向（减去180度，因为罗盘指向的是手机朝向，人是向前走）
  let movingHeading = (adjustedHeading - 180 + 360) % 360;

  // 方向稳定性处理：始终以罗盘真实方向为基准，但过滤微小波动
  if (lastStepHeading >= 0) {
    let headingDiff = movingHeading - lastStepHeading;
    // 处理角度环绕
    if (headingDiff > 180) headingDiff -= 360;
    if (headingDiff < -180) headingDiff += 360;

    // 设置方向变化阈值：只有变化超过15度才认为是真实转向
    // 小于15度的变化认为是罗盘噪声，忽略
    const changeThreshold = 15;
    if (Math.abs(headingDiff) <= changeThreshold) {
      // 变化很小，可能是罗盘噪声，保持上一次的方向
      movingHeading = lastStepHeading;
    }
    // 变化大则使用新的罗盘方向
  }
  lastStepHeading = movingHeading;

  // 转换为弧度
  let headingRad = (movingHeading * Math.PI) / 180;

  // 如果没有有效罗盘方向，使用最后保存的方向
  if (!hasCompass && lastValidHeading > 0) {
    movingHeading = (lastValidHeading - 180 + 360) % 360;
    headingRad = (movingHeading * Math.PI) / 180;
  } else if (!hasCompass && lastValidHeading === 0) {
    // 完全没有方向信息，保持当前方向或默认向前
    if (lastStepHeading < 0) {
      headingRad = 0; // 默认向北
    }
  }

  // 计算经纬度变化（考虑地图旋转）
  // 地图旋转角度补偿：地图相对于真实世界顺时针旋转了rotation度
  const rotRad = (MAP_TRANSFORM.rotation * Math.PI) / 180;

  // 计算在真实坐标系中的位移
  let deltaLng = Math.sin(headingRad) * STEP_LENGTH;
  let deltaLat = Math.cos(headingRad) * STEP_LENGTH;

  // 将真实位移转换到地图坐标系（应用旋转补偿，与模拟走路保持一致）
  const rotatedDeltaLng =
    deltaLng * Math.cos(rotRad) - deltaLat * Math.sin(rotRad);
  const rotatedDeltaLat =
    deltaLng * Math.sin(rotRad) + deltaLat * Math.cos(rotRad);

  // 更新经纬度
  longitude.value += rotatedDeltaLng;
  latitude.value += rotatedDeltaLat;

  // 边界检查：确保位置在景区范围内
  longitude.value = Math.max(
    MAP_BOUNDS.minLng,
    Math.min(MAP_BOUNDS.maxLng, longitude.value),
  );
  latitude.value = Math.max(
    MAP_BOUNDS.minLat,
    Math.min(MAP_BOUNDS.maxLat, latitude.value),
  );

  // 使用仿射变换函数转换为XY坐标
  const pos = lngLatToXY(longitude.value, latitude.value);
  userX.value = pos.x;
  userY.value = pos.y;

  // 保存当前位置到存储
  storage.setCurrentLocation({
    longitude: longitude.value,
    latitude: latitude.value,
  });

  // 检查是否到达景点
  checkSpotArrival();
};

const stopLocationTracking = () => {
  uni.stopAccelerometer();
  uni.stopCompass();
  if (sensorTimer) {
    clearInterval(sensorTimer);
    sensorTimer = null;
  }
  isTracking = false; // 标记停止跟踪
};

// 模拟走路（测试用）- 考虑罗盘方向和经纬度计算
const simulateWalk = () => {
  if (!isGuiding.value) {
    uni.showToast({ title: "请先开始导览", icon: "none" });
    return;
  }

  // 每步移动距离（经纬度单位，约0.00001度 ≈ 1米）
  const STEP_LENGTH = 0.00005;

  // 获取当前朝向（罗盘方向）
  let adjustedHeading = (currentHeading.value - 180 + 360) % 360;

  // 防止方向突变：限制每次方向变化不超过45度
  if (lastStepHeading > 0) {
    let headingDiff = adjustedHeading - lastStepHeading;
    if (headingDiff > 180) headingDiff -= 360;
    if (headingDiff < -180) headingDiff += 360;
    if (headingDiff > 45) headingDiff = 45;
    if (headingDiff < -45) headingDiff = -45;
    adjustedHeading = (lastStepHeading + headingDiff + 360) % 360;
  }
  lastStepHeading = adjustedHeading;

  let headingRad = (adjustedHeading * Math.PI) / 180;

  // 如果没有罗盘数据，使用默认方向（向北）
  if (!hasCompass && lastValidHeading > 0) {
    adjustedHeading = (lastValidHeading - 180 + 360) % 360;
    headingRad = (adjustedHeading * Math.PI) / 180;
  } else if (!hasCompass && lastValidHeading === 0) {
    headingRad = 0;
  }

  // 计算经纬度增量（考虑地图旋转补偿）
  // heading: 0=北, 90=东, 180=南, 270=西
  let deltaLng = Math.sin(headingRad) * STEP_LENGTH;
  let deltaLat = Math.cos(headingRad) * STEP_LENGTH;

  // 地图旋转补偿：将地图方向转换为真实方向
  // MAP_TRANSFORM.rotation = 37 表示地图相对于真实世界顺时针旋转了37度
  const rotRad = (MAP_TRANSFORM.rotation * Math.PI) / 180;
  const rotatedDeltaLng =
    deltaLng * Math.cos(rotRad) - deltaLat * Math.sin(rotRad);
  const rotatedDeltaLat =
    deltaLng * Math.sin(rotRad) + deltaLat * Math.cos(rotRad);

  // 更新经纬度
  longitude.value += rotatedDeltaLng;
  latitude.value += rotatedDeltaLat;

  // 使用经纬度转XY逻辑计算用户位置（带校准）
  const pos = lngLatToXY(longitude.value, latitude.value);
  userX.value = Math.max(0, Math.min(100, pos.x));
  userY.value = Math.max(0, Math.min(100, pos.y));

  // 检查是否到达景点
  checkSpotArrival();

  uni.showToast({ title: "已模拟走一步", icon: "none", duration: 500 });
};

// 4. 检查是否进入景点范围（使用经纬度计算）
const checkSpotArrival = () => {
  const now = new Date().toISOString();

  // 找到距离最近的景点（使用经纬度计算）
  let closestSpot = null;
  let minDistance = Infinity;

  for (let spot of SPOTS) {
    // 跳过出入口
    if (spot.isEntryExit) {
      continue;
    }

    // 使用经纬度计算距离
    const dx = longitude.value - spot.lng;
    const dy = latitude.value - spot.lat;
    const dist = Math.sqrt(dx * dx + dy * dy);

    // 记录最近的景点
    if (dist < minDistance) {
      minDistance = dist;
      closestSpot = spot;
    }
  }

  // 使用景点定义的范围（SPOTS中的range字段，单位：度）
  if (closestSpot && minDistance < closestSpot.range) {
    if (currentSpot.value !== closestSpot.name) {
      currentSpot.value = closestSpot.name;
      // 到达景点后显示提示信息
      displayText.value = `到达${closestSpot.name}，点击文字框获取讲解`;

      // 更新真实路线记录
      storage.addVisitRecord({
        spotName: closestSpot.name,
        startTime: now,
        endTime: null,
      });
      const existingIndex = visitedSpots.value.findIndex(
        (s) => s.spotName === closestSpot.name,
      );
      if (existingIndex === -1) {
        visitedSpots.value.push({
          spotName: closestSpot.name,
          startTime: now,
          endTime: null,
        });
      }
    }
    return;
  }

  // 出景点范围，更新结束时间
  if (currentSpot.value) {
    const leftSpotName = currentSpot.value;
    storage.updateVisitEndTime(leftSpotName, now);
    const index = visitedSpots.value.findIndex(
      (s) => s.spotName === leftSpotName && !s.endTime,
    );
    if (index !== -1) {
      visitedSpots.value[index].endTime = now;
    }

    displayText.value = `已离开${leftSpotName}，继续寻找下一个景点`;
    currentSpot.value = null;
  }
};

// 计算两点距离（简化版）
const getDistance = (lng1, lat1, lng2, lat2) => {
  const dx = lng1 - lng2;
  const dy = lat1 - lat2;
  return Math.sqrt(dx * dx + dy * dy);
};

// 5. 生成游记（异步，不等待结果）
const generateTravelogue = async () => {
  const records = storage.getVisitRecords();
  if (records.length === 0) {
    uni.showToast({ title: "暂无游览记录", icon: "none" });
    return;
  }

  // 先显示成功提示
  uni.showToast({
    title: "已生成游记",
    icon: "success",
    duration: 2000,
  });

  // 立即发送请求，不等待结果
  try {
    const userId = getUserId();

    // 构建景点访问记录列表（只包含景点名和到达时间）
    const visitedSpots = records.map((record) => ({
      spotName: record.spotName,
      arrivalTime: formatDate(record.startTime), // 使用到达时间，格式化为YYYY-MM-DDTHH:mm:ss
    }));

    // 提取导览开始和结束时间（格式化为YYYY-MM-DDTHH:mm:ss）
    const startTime =
      records.length > 0 ? formatDate(records[0].startTime) : null;
    const endTime =
      records.length > 0
        ? formatDate(records[records.length - 1].endTime) ||
          formatDate(new Date().toISOString())
        : null;

    // 构建请求数据（按照后端要求的格式）
    const requestData = {
      userId,
      startTime,
      endTime,
      visitedSpots,
    };

    // 打印日志记录
    console.log("=== 生成游记参数 ===");
    console.log("userId:", userId);
    console.log("startTime:", startTime);
    console.log("endTime:", endTime);
    console.log("visitedSpots:", JSON.stringify(visitedSpots, null, 2));
    console.log("=====================");

    // 异步发送请求，不等待
    request({
      url: "/user/travel-note/generate",
      method: "POST",
      data: requestData,
    })
      .then((res) => {
        console.log("生成游记成功，返回ID:", res?.data?.data);
      })
      .catch((e) => {
        console.error("生成游记请求失败:", e);
      });
  } catch (e) {
    console.error("生成游记失败:", e);
  } finally {
    // 无论成功失败，都释放游览列表
    visitedSpots.value = [];
    storage.clearVisitRecords();
  }
};

// 辅助函数：从ISO时间字符串提取日期部分
const formatDate = (isoString) => {
  if (!isoString) return null;
  try {
    const date = new Date(isoString);
    // 输出格式：YYYY-MM-DDTHH:mm:ss（去掉毫秒和时区）
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    const seconds = String(date.getSeconds()).padStart(2, "0");
    return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
  } catch {
    return isoString;
  }
};

// 6. 景点交互
// 获取景点标记样式
const getMarkerStyle = (spot) => {
  // 使用经纬度转换后的x/y坐标，然后转换为画布像素坐标
  const pos = lngLatToXY(spot.lng, spot.lat);
  return {
    left: `${(pos.x / 100) * canvasWidth.value}px`,
    top: `${(pos.y / 100) * canvasHeight.value}px`,
    transform: "translate(-50%, -50%)",
  };
};

// 判断是否需要反转标记位置
const isReverseMarker = (spotName) => {
  const reverseSpots = [
    "灵山大照壁",
    "佛足坛",
    "菩提大道",
    "降魔浮雕",
    "祥符禅寺",
  ];
  return reverseSpots.includes(spotName);
};

// 获取用户位置标记样式
const getUserMarkerStyle = () => {
  return {
    left: `${(userX.value / 100) * canvasWidth.value}px`,
    top: `${(userY.value / 100) * canvasHeight.value}px`,
    transform: "translate(-50%, -50%)",
  };
};

// 双击检测相关变量
let lastTapIndex = -1;
let lastTapTime = 0;
let tapTimer = null;

const onMarkerTap = (index) => {
  const spotName = SPOTS[index].name;
  const now = Date.now();

  // 检查是否是双击（300ms内连续点击同一个景点）
  if (index === lastTapIndex && now - lastTapTime < 300) {
    // 双击：获取讲解
    if (tapTimer) {
      clearTimeout(tapTimer);
      tapTimer = null;
    }
    console.log(`[双击] 景点: ${spotName}`);
    handleDoubleClick(spotName);
  } else {
    // 可能是单击，设置延迟检查
    lastTapIndex = index;
    lastTapTime = now;

    if (tapTimer) {
      clearTimeout(tapTimer);
    }

    tapTimer = setTimeout(() => {
      // 单击：显示景点卡片
      console.log(`[单击] 景点: ${spotName}`);
      loadSpotDetail(spotName);
      tapTimer = null;
    }, 350);
  }
};

// 点击文字框处理：获取当前景点讲解
const handleTextboxClick = () => {
  if (currentSpot.value) {
    if (isGuiding.value) {
      // 如果在导览中，获取当前景点讲解
      getSpotGuide(currentSpot.value);
    } else {
      uni.showToast({ title: "请先开始导览", icon: "none" });
    }
  }
};

const loadSpotDetail = async (spotName) => {
  try {
    // 1. 先从SPOTS数组中获取基本信息（包含图片路径）
    const spot = SPOTS.find((s) => s.name === spotName);
    let imagePath = spot?.image || null;

    // 2. 再从已加载的景点信息中获取详细信息
    const attraction = attractionsInfo.value.find(
      (a) => a.attrName === spotName,
    );

    if (attraction) {
      currentSpotDetail.value = {
        name: attraction.attrName || spotName,
        description: attraction.culture || "暂无介绍",
        image: imagePath || attraction.image || null,
      };
    } else {
      // 如果本地没有，尝试从接口获取
      const res = await request({
        url: "/user/attractionsInfo",
        method: "GET",
      });

      if (res.data && Array.isArray(res.data)) {
        const found = res.data.find((a) => a.attrName === spotName);
        currentSpotDetail.value = {
          name: found?.attrName || spotName,
          description: found?.culture || "暂无介绍",
          image: imagePath || found?.image || null,
        };
      } else {
        currentSpotDetail.value = {
          name: spotName,
          description: "暂无介绍",
          image: imagePath || null,
        };
      }
    }

    showSpotModal.value = true;
  } catch (e) {
    console.error("加载景点详情失败:", e);
    uni.showToast({ title: "加载失败", icon: "none" });
  }
};

let isGuideLoading = false; // 讲解请求是否进行中
let currentGuideSpot = null; // 当前正在请求的景点

// 获取景点讲解（用户在导览中且在景点范围内时调用）
const getSpotGuide = async (spotName) => {
  // 防抖：如果正在加载，直接返回
  if (isGuideLoading) {
    console.log(`[讲解] 正在加载中，忽略重复请求`);
    return;
  }
  // 只有当文字框已经显示完整讲解内容时才忽略重复请求
  // 如果显示的是"到达xxx，点击文字框获取讲解"或"xxx讲解生成中..."，则允许请求
  if (
    currentGuideSpot === spotName &&
    displayText.value.includes(spotName) &&
    !displayText.value.includes("讲解生成中") &&
    !displayText.value.includes("点击文字框获取讲解")
  ) {
    console.log(`[讲解] ${spotName} 讲解已加载，忽略重复请求`);
    return;
  }

  isGuideLoading = true;
  currentGuideSpot = spotName;

  try {
    // 在文字框显示"讲解生成中..."
    displayText.value = `${spotName}讲解生成中...`;

    const userId = getUserId();
    const res = await request({
      url: "/user/spot-guide",
      method: "GET",
      data: {
        spotName,
        userId,
      },
    });

    // 只有当前请求的景点仍然是用户所在的景点时才更新
    if (currentSpot.value === spotName) {
      if (res.code === 200 && res.data) {
        if (res.data.explanation) {
          // 把讲解内容渲染到文字框（去掉【讲解】标签）
          displayText.value = `${spotName}\n${res.data.explanation}`;
          // 同时保存到详情中
          if (
            !currentSpotDetail.value.description.includes(res.data.explanation)
          ) {
            currentSpotDetail.value.description +=
              "\n\n" + res.data.explanation;
          }
        } else {
          displayText.value = `${spotName}暂无讲解`;
        }
      } else {
        displayText.value = `${spotName}讲解加载失败`;
      }
    }
  } catch (e) {
    console.error("获取讲解失败:", e);
    if (currentSpot.value === spotName) {
      displayText.value = `${spotName}讲解加载失败`;
    }
  } finally {
    isGuideLoading = false;
  }
};

const closeSpotModal = () => {
  showSpotModal.value = false;
  // 关闭弹窗时重置景点播报状态
  if (isSpotSpeaking.value) {
    resetDigitalHuman();
  }
};

const goToCollection = () => {
  uni.redirectTo({ url: "/pages/routeCollection/routeCollection" });
};

const goToRecommend = () => {
  uni.redirectTo({ url: "/pages/recommendRoute/recommendRoute" });
};

const zoomIn = () => {
  if (mapScale.value < maxScale) {
    mapScale.value = Math.min(mapScale.value + 0.25, maxScale);
  }
};

const zoomOut = () => {
  if (mapScale.value > minScale) {
    mapScale.value = Math.max(mapScale.value - 0.25, minScale);
  }
};

// 计算触摸点之间的距离
const getTouchDistance = (touch1, touch2) => {
  const dx = touch2.clientX - touch1.clientX;
  const dy = touch2.clientY - touch1.clientY;
  return Math.sqrt(dx * dx + dy * dy);
};

// 计算两点的中心点
const getCenter = (touch1, touch2) => {
  return {
    x: (touch1.clientX + touch2.clientX) / 2,
    y: (touch1.clientY + touch2.clientY) / 2,
  };
};

// 触摸开始
const onTouchStart = (e) => {
  const touches = e.touches;

  if (touches.length === 1) {
    // 单指触摸 - 可能是平移
    touchStartX = touches[0].clientX - offsetX.value;
    touchStartY = touches[0].clientY - offsetY.value;
    isPanning = true;
    isPinching = false;
  } else if (touches.length === 2) {
    // 双指触摸 - 缩放
    lastTouchDistance = getTouchDistance(touches[0], touches[1]);
    const center = getCenter(touches[0], touches[1]);
    lastTouchCenterX = center.x;
    lastTouchCenterY = center.y;
    isPinching = true;
    isPanning = false;
  }
};

// 触摸移动
const onTouchMove = (e) => {
  // 注意：不再调用 e.preventDefault()，因为 touch-action: none 已经禁用了默认触摸行为
  // Chrome 默认将触摸事件设置为 passive 模式，preventDefault 会产生警告
  const touches = e.touches;

  if (touches.length === 1 && isPanning && !isPinching) {
    // 单指平移
    const newX = touches[0].clientX - touchStartX;
    const newY = touches[0].clientY - touchStartY;

    // 限制平移范围
    const maxOffsetX = ((mapScale.value - 1) * canvasWidth.value) / 4;
    const maxOffsetY = ((mapScale.value - 1) * canvasHeight.value) / 4;

    offsetX.value = Math.max(-maxOffsetX, Math.min(maxOffsetX, newX));
    offsetY.value = Math.max(-maxOffsetY, Math.min(maxOffsetY, newY));
  } else if (touches.length === 2 && isPinching) {
    // 双指缩放
    const currentDistance = getTouchDistance(touches[0], touches[1]);
    const center = getCenter(touches[0], touches[1]);

    // 计算缩放比例
    const scaleFactor = currentDistance / lastTouchDistance;
    let newScale = mapScale.value * scaleFactor;
    newScale = Math.max(minScale, Math.min(maxScale, newScale));

    // 计算缩放中心点偏移
    if (newScale !== mapScale.value) {
      const scaleDiff = newScale / mapScale.value;
      const dx = (center.x - lastTouchCenterX) * (1 - scaleDiff);
      const dy = (center.y - lastTouchCenterY) * (1 - scaleDiff);

      offsetX.value = offsetX.value * scaleDiff + dx;
      offsetY.value = offsetY.value * scaleDiff + dy;
      mapScale.value = newScale;
    }

    lastTouchDistance = currentDistance;
    lastTouchCenterX = center.x;
    lastTouchCenterY = center.y;
  }
};

// 触摸结束
const onTouchEnd = () => {
  isPanning = false;
  isPinching = false;

  // 限制最终偏移范围
  const maxOffsetX = ((mapScale.value - 1) * canvasWidth.value) / 4;
  const maxOffsetY = ((mapScale.value - 1) * canvasHeight.value) / 4;

  offsetX.value = Math.max(-maxOffsetX, Math.min(maxOffsetX, offsetX.value));
  offsetY.value = Math.max(-maxOffsetY, Math.min(maxOffsetY, offsetY.value));
};

const handleAvatarClick = () => {
  console.log("数字人被点击，当前文字:", displayText.value);

  if (!digitalHumanWebview) {
    console.error("[数字人点击] ❌ 数字人webview不存在");
    uni.showToast({ title: "数字人未就绪", icon: "none" });
    return;
  }

  const pureText = filterPureText(displayText.value);
  console.log("[数字人点击] 过滤后内容:", pureText);

  if (!pureText) {
    console.log("[数字人点击] 无可播报内容");
    uni.showToast({ title: "无播报内容", icon: "none" });
    return;
  }

  // 发送完整内容给数字人
  try {
    const escapedText = JSON.stringify(pureText);
    digitalHumanWebview.evalJS(`window.sendTextInteract(${escapedText})`);
    isSpeaking.value = true;
    console.log("[数字人点击] ✅ 完整内容已发送给数字人");
  } catch (e) {
    console.error("[数字人点击] ❌ 发送失败:", e);
    uni.showToast({ title: "播报失败", icon: "none" });
  }
};
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
  margin-bottom: 20rpx;
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
  font-weight: 400;
  color: #333333;
}

.container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
  padding-top: calc(var(--status-bar-height, 44px) + 88rpx);
  overflow: hidden;
  touch-action: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

/* 顶部模块（占页面1/5） */
.top-section {
  width: 100%;
  height: 18%;
  display: flex;
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 50%, #a5d6a7 100%);
  padding: 24rpx;
  padding-right: 24rpx;
  box-sizing: border-box;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.08);
}

/* 数字人模块 */
.avatar-section {
  width: 28%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 矩形数字人样式 */
.avatar-rect {
  width: 180rpx;
  height: 240rpx;
  border-radius: 16rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.15);
  object-fit: cover;
}

/* 中间：文字渲染框 */
.info-section {
  flex: 1;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20rpx;
  padding: 10rpx 24rpx;
  margin: 0 20rpx;
  margin-top: -5rpx;
  height: 240rpx;
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

/* 工具按钮行 */
.tool-row {
  display: flex;
  align-items: center;
  padding-top: 8rpx;
  border-top: 1rpx solid #eee;
  margin-top: 8rpx;
}

.tool-btn {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 16rpx;
  font-size: 24rpx;
  color: #b38c4f;
}

.tool-btn:active {
  opacity: 0.7;
}

/* 导航栏播报开关 */
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

/* 路线按钮（浮在地图上方） */
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

/* 底部模块（占页面4/5） */
.bottom-section {
  width: 100%;
  height: 72%;
  position: relative;
  overflow: hidden;
}

/* 地图容器 */
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

/* 缩放内容容器 */
.map-content {
  width: 100%;
  height: 100%;
  position: relative;
}

/* 地图内容包裹 */
.map-inner {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
  will-change: transform;
}

/* 交互层 */
.map-overlay {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
}

/* 路线画布 */
.route-canvas {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 10;
  pointer-events: none;
}

/* 手绘地图背景 */
.hand-drawn-map {
  width: 100%;
  height: 100%;
}

/* 交互层 */
.map-overlay {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
}

/* 景点标记 */
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

/* 反向标记（文字在圆点左边） */

.reverse-marker .marker-label {
  left: auto;
  right: 16rpx;
}

/* 用户位置标记 */
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

/* 外圈脉冲 */
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

/* 蓝色圆点 */
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

/* 方向箭头 */
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

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(74, 144, 226, 0.7);
  }
  70% {
    box-shadow: 0 0 0 12rpx rgba(74, 144, 226, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(74, 144, 226, 0);
  }
}

/* 左上角区域 */
.top-left-area {
  position: absolute;
  top: 40rpx;
  left: 20rpx;
  display: flex;
  align-items: center;
  z-index: 100;
}
.avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  margin-right: 15rpx;
  border: 4rpx solid #fff;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.2);
}
.info-card {
  width: 420rpx;
  min-height: 100rpx;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12rpx;
  padding: 15rpx 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
}
.info-text {
  font-size: 28rpx;
  color: #333;
  line-height: 1.5;
}

/* 右上角按钮 */
.top-right-btn {
  position: absolute;
  top: 40rpx;
  right: 30rpx;
  background: rgba(255, 255, 255, 0.95);
  padding: 15rpx 20rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  z-index: 100;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
}
.btn-icon {
  width: 36rpx;
  height: 36rpx;
  margin-right: 10rpx;
}
.btn-label {
  font-size: 24rpx;
  color: #333;
}

/* 右下角按钮 */
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

/* 地图缩放控制 */
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

/* 模拟走路按钮 */
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

/* 弹窗样式 */
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
  width: 620rpx;
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
}
.modal-header {
  padding: 30rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  border-bottom: 1rpx solid #eee;
  position: relative;
}
.modal-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}
.modal-close {
  position: absolute;
  right: 30rpx;
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  color: #999;
}
.modal-body {
  padding: 20rpx;
}
.modal-image {
  width: 600rpx;
  max-height: 300rpx;
  margin: 0 auto 20rpx;
  display: block;
  border-radius: 12rpx;
}
.modal-desc {
  font-size: 28rpx;
  color: #666;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.modal-footer {
  padding: 5rpx;
  margin-top: 10rpx;
  border-top: 1rpx solid #eee;
  display: flex;
  justify-content: center;
}

.spot-play-btn {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 12rpx 40rpx;
  font-size: 28rpx;
  color: #b38c4f;
}

.spot-play-btn:active {
  opacity: 0.7;
  transform: scale(0.98);
}
</style>
