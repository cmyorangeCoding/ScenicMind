<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { onShow } from "@dcloudio/uni-app";
import request from "../../request.js";
import { getDigitalHumanAvatar } from "../../auth.js";

const hotQuestions = ref([]);
const floatingQuestions = ref([]);
const isRow1Scrolling = ref(true);
const isRow2Scrolling = ref(true);
let scrollTimer = null;

// ========== 数字人相关 ==========
const digitalHumanWebview = ref(null);
const digitalHumanAvatar = ref(getDigitalHumanAvatar());

// 更新数字人头像
const updateDigitalHumanAvatar = () => {
  digitalHumanAvatar.value = getDigitalHumanAvatar();
};

const row1Questions = computed(() => {
  const shuffled = [...floatingQuestions.value].sort(() => Math.random() - 0.5);
  return shuffled.slice(0, 6);
});

const row2Questions = computed(() => {
  const shuffled = [...floatingQuestions.value].sort(() => Math.random() - 0.5);
  return shuffled.slice(0, 6);
});

const fetchHotQuestions = async () => {
  try {
    const res = await request.get("/user/ai/hot-questions");
    if (res.code === 200 && res.data) {
      floatingQuestions.value = res.data.slice(0, 15);
    }
  } catch (e) {
    console.log("获取热门问题失败:", e);
  }
};

const goToAiQuestion = (question) => {
  uni.navigateTo({
    url: `/pages/aiQuestion/aiQuestion?question=${encodeURIComponent(question)}`,
  });
};

const stopRow1Scroll = () => {
  isRow1Scrolling.value = false;
};

const startRow1Scroll = () => {
  setTimeout(() => {
    isRow1Scrolling.value = true;
  }, 1500);
};

const stopRow2Scroll = () => {
  isRow2Scrolling.value = false;
};

const startRow2Scroll = () => {
  setTimeout(() => {
    isRow2Scrolling.value = true;
  }, 1500);
};

const handleHomeDhClick = () => {
  uni.navigateTo({
    url: "/pages/aiQuestion/aiQuestion",
  });
};

// 创建数字人 webview（首页使用半身模式，放在四个功能框中间）
const createDigitalHumanWebview = () => {
  console.log("========== 首页创建数字人 webview ==========");

  // H5环境降级方案
  if (typeof plus === "undefined" || !plus.webview) {
    console.log("[数字人] H5环境，跳过webview创建");
    return;
  }

  // 每次都重新获取预加载webview，不管之前是否创建过
  setTimeout(() => {
    // 使用预加载的 webview
    let preloadedWebview = null;
    try {
      preloadedWebview = plus.webview.getWebviewById("preloadDigitalHuman");
      console.log("[数字人] 尝试获取预加载webview，结果:", !!preloadedWebview);
      if (preloadedWebview) {
        console.log("[数字人] 预加载webview ID:", preloadedWebview.id);
        console.log("[数字人] 预加载webview UUID:", preloadedWebview.__uuid__);
      }
    } catch (e) {
      console.warn("[数字人] 获取预加载webview失败:", e);
    }

    if (!preloadedWebview) {
      console.log("[数字人] 未找到预加载webview，创建新的");
      // 用户计算好的位置（嵌入到 hero-card 左上角，高度减45）
      const width = 196;
      const height = 215;
      const left = -68;
      const top = 22;

      digitalHumanWebview.value = plus.webview.create(
        "_www/static/xfyun-digital-human/web-api.html",
        "digitalHumanIndex",
        {
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
          zindex: 20,
          popGesture: "none",
          hardwareAccelerated: true,
          ignoretouch: true,
          hitTest: true,
          pointerEvents: "auto",
        },
      );

      digitalHumanWebview.value.show();
      console.log("[数字人] 新webview已创建并调用 show()");
    } else {
      console.log("[数字人] 找到预加载的 webview:", preloadedWebview.id);
      digitalHumanWebview.value = preloadedWebview;

      // 用户计算好的位置（嵌入到 hero-card 左上角，高度减45）
      const width = 196;
      const height = 215;
      const left = -65;
      const top = 63;

      console.log("[数字人] webview 尺寸:", width, "x", height);
      console.log("[数字人] webview 位置:", left, top);

      // 调整位置和尺寸（嵌入到 hero-card 中）
      digitalHumanWebview.value.setStyle({
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
        zindex: 20,
        popGesture: "none",
        hardwareAccelerated: true,
        ignoretouch: true,
        hitTest: true,
        pointerEvents: "auto",
      });

      // 显式显示webview
      digitalHumanWebview.value.show();
      console.log("[数字人] 预加载webview已显示");

      // 增加延迟，确保RTC流已建立后再切换模式
      setTimeout(() => {
        digitalHumanWebview.value.evalJS("window.delayedSwitchToHalfBody()");
      }, 1500);
    }

    // 监听消息
    digitalHumanWebview.value.addEventListener("message", (e) => {
      const data = e.data && e.data[0];
      if (data) {
        console.log("[首页数字人消息] 收到消息:", data);
        if (data.type === "ready") {
          console.log("[数字人] 数字人准备就绪");
        } else if (data.type === "speakEnd") {
          console.log("[数字人] 播报结束");
        }
      }
    });

    console.log("[数字人] webview 设置完成");
  }, 500);
};

// 销毁数字人 webview
const destroyDigitalHumanWebview = () => {
  console.log("========== 首页销毁数字人webview ==========");

  if (digitalHumanWebview.value) {
    try {
      // 检查是否是预加载的webview
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
          zindex: 1,
          ignoretouch: true,
          hitTest: true,
          pointerEvents: "auto",
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

onMounted(() => {
  fetchHotQuestions();
  createDigitalHumanWebview();
});

onShow(() => {
  console.log("========== Index 页面显示 ==========");
  // 每次显示都尝试重新创建/显示数字人
  createDigitalHumanWebview();
});

onUnmounted(() => {
  if (scrollTimer) {
    clearInterval(scrollTimer);
  }
  destroyDigitalHumanWebview();
});
</script>

<template>
  <view class="container">
    <view class="full-page-bg">
      <image
        class="resort-img"
        src="/static/resort.jpg"
        mode="aspectFill"
      ></image>
      <image
        class="bg-cloud cloud-1"
        src="/static/cloud_detail.jpg"
        mode="aspectFit"
      ></image>
      <image
        class="bg-cloud cloud-2"
        src="/static/cloud_detail.jpg"
        mode="aspectFit"
      ></image>
      <image
        class="bg-cloud cloud-3"
        src="/static/cloud_watermark.jpg"
        mode="aspectFit"
      ></image>
      <view class="bg-mask"></view>
    </view>

    <view class="header-section">
      <view class="status-bar"></view>
      <view class="title-card">
        <text class="main-title">灵山胜境</text>
        <view class="title-sub-row">
          <view class="gold-line"></view>
          <text class="sub-title">数字人智慧导览服务</text>
          <view class="gold-line"></view>
        </view>
      </view>
    </view>

    <view class="hero-card">
      <view class="card-pattern"></view>
      <view class="dh-wrapper"> </view>
      <view class="hero-content">
        <text class="slogan">"穿越时空的神圣遇见"</text>
        <view class="hero-divider"></view>
        <text class="desc-text">灵山胜境官方数字向导</text>
      </view>
      <image
        class="zen-cloud hero-cloud"
        src="/static/cloud_hero.jpg"
        mode="aspectFit"
      ></image>
    </view>

    <!-- 两排横向滚动的热门问题标签 -->
    <view class="hot-questions-container">
      <!-- 第一排 -->
      <scroll-view
        scroll-x
        class="hot-questions-scroll"
        :show-scrollbar="false"
        @touchstart="stopRow1Scroll"
        @touchend="startRow1Scroll"
      >
        <view class="hot-questions-row" :class="{ scrolling: isRow1Scrolling }">
          <view
            v-for="(item, index) in row1Questions"
            :key="'row1-' + index"
            class="hot-question-tag"
            @tap="goToAiQuestion(item.question)"
          >
            <text class="tag-text">{{ item.question }}</text>
          </view>
          <!-- 复制一份实现无缝滚动 -->
          <view
            v-for="(item, index) in row1Questions"
            :key="'row1-copy-' + index"
            class="hot-question-tag"
            @tap="goToAiQuestion(item.question)"
          >
            <text class="tag-text">{{ item.question }}</text>
          </view>
        </view>
      </scroll-view>
      <!-- 第二排 -->
      <scroll-view
        scroll-x
        class="hot-questions-scroll"
        :show-scrollbar="false"
        @touchstart="stopRow2Scroll"
        @touchend="startRow2Scroll"
      >
        <view class="hot-questions-row" :class="{ scrolling: isRow2Scrolling }">
          <view
            v-for="(item, index) in row2Questions"
            :key="'row2-' + index"
            class="hot-question-tag"
            @tap="goToAiQuestion(item.question)"
          >
            <text class="tag-text">{{ item.question }}</text>
          </view>
          <!-- 复制一份实现无缝滚动 -->
          <view
            v-for="(item, index) in row2Questions"
            :key="'row2-copy-' + index"
            class="hot-question-tag"
            @tap="goToAiQuestion(item.question)"
          >
            <text class="tag-text">{{ item.question }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 四个功能框（移除外面的大框） -->
    <view class="grid-container">
      <view class="zen-card shape-tl" @tap="$go('aiQuestion')">
        <view class="card-inner-bg"></view>
        <view class="card-content">
          <view class="card-icon-wrapper">
            <text class="card-icon">💡</text>
          </view>
          <text class="card-title">景区知识</text>
          <text class="card-desc">探索灵山底蕴</text>
        </view>
      </view>

      <view class="zen-card shape-tr" @tap="$go('route')">
        <view class="card-inner-bg"></view>
        <view class="card-content">
          <view class="card-icon-wrapper">
            <text class="card-icon">🚩</text>
          </view>
          <text class="card-title">路线推荐</text>
          <text class="card-desc">官方游玩指南</text>
        </view>
      </view>

      <!-- 中间的index图片 -->
      <view class="center-image-wrapper">
        <image
          class="center-image"
          src="/static/style/index1.jpg"
          mode="aspectFit"
        ></image>
      </view>

      <view class="zen-card shape-bl" @tap="$go('companion')">
        <view class="card-inner-bg"></view>
        <view class="card-content">
          <view class="card-icon-wrapper">
            <text class="card-icon">🎧</text>
          </view>
          <text class="card-title">智能伴游</text>
          <text class="card-desc">随身语音讲解</text>
        </view>
      </view>

      <view class="zen-card shape-br" @tap="$go('feedback')">
        <view class="card-inner-bg"></view>
        <view class="card-content">
          <view class="card-icon-wrapper">
            <text class="card-icon">💬</text>
          </view>
          <text class="card-title">意见反馈</text>
          <text class="card-desc">期待您的建议</text>
        </view>
      </view>
    </view>

    <view class="footer-nav">
      <view class="nav-capsule">
        <view class="nav-item active">
          <view class="nav-circle">
            <image
              class="nav-avatar"
              :src="digitalHumanAvatar"
              mode="aspectFill"
            ></image>
          </view>
          <view class="nav-text-group">
            <text class="nav-label">AI</text>
            <text class="nav-sub">智慧导览</text>
          </view>
        </view>
        <view class="nav-sep"></view>
        <view class="nav-item" @tap="$go('profile')">
          <view class="nav-icon-user"></view>
          <view class="nav-text-group">
            <text class="nav-label">个人中心</text>
            <text class="nav-sub">我的行程</text>
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

.container {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background-color: $bg-color;
  position: relative;
  display: flex;
  flex-direction: column;
}

.full-page-bg {
  position: absolute;
  width: 100%;
  height: 100%;
  z-index: 0;
  .resort-img {
    width: 100%;
    height: 55%;
    opacity: 0.8;
  }
  .bg-mask {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(
      to bottom,
      rgba(253, 250, 242, 0.2) 0%,
      $bg-color 45%,
      $bg-color 100%
    );
  }
  .bg-cloud {
    position: absolute;
    mix-blend-mode: multiply;
    opacity: 0.15;
    &.cloud-1 {
      top: 10%;
      left: -50rpx;
      width: 300rpx;
    }
    &.cloud-2 {
      top: 40%;
      right: -30rpx;
      width: 400rpx;
      transform: rotate(15deg);
    }
    &.cloud-3 {
      bottom: 15%;
      left: 10%;
      width: 500rpx;
    }
  }
}

.header-section {
  margin-top: 40rpx;
  margin-bottom: 10rpx;
  position: relative;
  z-index: 10;
  padding: 60rpx 40rpx 20rpx;
  .title-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    .main-title {
      font-size: 52rpx;
      font-weight: 800;
      color: #2a2a2a;
      letter-spacing: 12rpx;
    }
    .title-sub-row {
      display: flex;
      align-items: center;
      margin-top: 15rpx;
      .gold-line {
        width: 40rpx;
        height: 2rpx;
        background: $gold;
        opacity: 0.6;
      }
      .sub-title {
        font-size: 24rpx;
        color: #554433;
        margin: 0 20rpx;
        letter-spacing: 4rpx;
        font-weight: 500;
      }
    }
  }
}

.hero-card {
  margin: 10rpx 50rpx;
  margin-bottom: 20rpx;
  padding: 40rpx 40rpx 40rpx 200rpx; /* 左边预留数字人空间 */
  background: rgba(255, 255, 255, 0.92);
  border-radius: 35rpx;
  border: 2rpx solid $gold;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  z-index: 5;
  box-shadow: 0 20rpx 50rpx rgba(179, 140, 79, 0.15);
  min-height: 230rpx;
  .card-pattern {
    position: absolute;
    width: 100%;
    height: 100%;
    top: 0;
    left: 0;
    background-image:
      url("/static/cloud_watermark.jpg"), url("/static/cloud_detail.jpg");
    background-size: 200rpx, 150rpx;
    background-position:
      center,
      top right;
    background-repeat: no-repeat, no-repeat;
    opacity: 0.1;
  }
  .dh-wrapper {
    position: relative;
    z-index: 2;
    .dh-img {
      width: 130rpx;
      height: 170rpx;
      border-radius: 20rpx;
      border: 2rpx solid $gold;
    }
  }
  .hero-content {
    margin-left: 35rpx;
    z-index: 2;
    .slogan {
      font-size: 32rpx;
      font-weight: bold;
      color: #222;
    }
    .hero-divider {
      width: 60rpx;
      height: 4rpx;
      background: $gold;
      margin: 15rpx 0;
    }
    .desc-text {
      font-size: 24rpx;
      color: #666;
    }
  }
  .zen-cloud {
    mix-blend-mode: screen;
    position: absolute;
    opacity: 0.6;
  }
  .hero-cloud {
    width: 180rpx;
    height: 180rpx;
    right: -30rpx;
    top: -30rpx;
  }
}

/* 两排横向滚动的热门问题标签 */
.hot-questions-container {
  margin: 10rpx 30rpx;
  padding: 10rpx;
  z-index: 5;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 16rpx;
  border: 2rpx solid rgba(179, 140, 79, 0.3);
  box-shadow: 0 3rpx 12rpx rgba(179, 140, 79, 0.08);
}

.hot-questions-scroll {
  width: 100%;
  overflow-x: auto;
  margin-bottom: 8rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

.hot-questions-row {
  display: inline-flex;
  gap: 16rpx;
  padding: 3rpx 0;
  animation: autoScroll 15s linear infinite;
  animation-play-state: running;

  &:not(.scrolling) {
    animation-play-state: paused;
  }
}

@keyframes autoScroll {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-50%);
  }
}

.hot-question-tag {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  padding: 6rpx 18rpx;
  height: 56rpx;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 56rpx;
  border: 2rpx solid rgba(179, 140, 79, 0.25);
  box-shadow: 0 3rpx 8rpx rgba(179, 140, 79, 0.06);
  transition: all 0.3s;

  &:active {
    transform: scale(0.95);
    background: rgba(179, 140, 79, 0.1);
  }

  .tag-text {
    font-size: 22rpx;
    color: #555;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    line-height: 1.4;
  }
}

/* 四个功能框的网格布局 */
.grid-container {
  padding: 10rpx 60rpx;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-around;
  flex: 1;
  gap: 20rpx;
  position: relative;
}

.zen-card {
  width: 290rpx;
  height: 280rpx;
  margin-top: 20rpx;
  margin-bottom: 20rpx;
  background: #fff;
  position: relative;
  overflow: hidden;
  border: 2rpx solid $gold;
  transition: all 0.3s;
  box-shadow: 0 8rpx 16rpx rgba(0, 0, 0, 0.05);
  z-index: 20;

  &:active {
    transform: scale(0.96);
  }

  .card-inner-bg {
    position: absolute;
    width: 100%;
    height: 100%;
    background: $gold-light;
    background-image: url("/static/cloud_detail.jpg");
    background-size: 100rpx;
    background-position: bottom left;
    background-repeat: no-repeat;
    opacity: 0.3;
  }
  .card-content {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    position: relative;
    z-index: 2;
    .card-icon-wrapper {
      margin-bottom: 8rpx;
    }
    .card-icon {
      font-size: 44rpx;
    }
    .card-title {
      font-size: 30rpx;
      font-weight: bold;
      color: #333;
    }
    .card-desc {
      font-size: 18rpx;
      color: #888;
      margin-top: 6rpx;
    }
  }
}

.shape-tl {
  margin-bottom: 30rpx;
  border-radius: 30rpx;
  clip-path: polygon(0 0, 100% 0, 100% 75%, 75% 100%, 0 100%);
}
.shape-tr {
  border-radius: 30rpx;
  clip-path: polygon(0 0, 100% 0, 100% 100%, 25% 100%, 0 75%);
}
.shape-bl {
  border-radius: 30rpx;
  clip-path: polygon(0 0, 75% 0, 100% 25%, 100% 100%, 0 100%);
}
.shape-br {
  border-radius: 30rpx;
  clip-path: polygon(25% 0, 100% 0, 100% 100%, 0 100%, 0 25%);
}

/* 四个功能框中间的图片 */
.center-image-wrapper {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;
}

.center-image {
  width: 260rpx;
  height: 260rpx;
}

.footer-nav {
  padding-bottom: env(safe-area-inset-bottom);
  margin-top: 30rpx;
  display: flex;
  justify-content: center;
}

.nav-capsule {
  width: 580rpx;
  height: 120rpx;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 100rpx;
  border: 3rpx solid $gold;
  display: flex;
  align-items: center;
  box-shadow: 0 15rpx 40rpx rgba(179, 140, 79, 0.2);
  padding: 0 40rpx;
  .nav-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    &.active {
      .nav-label {
        color: #222;
        font-weight: bold;
      }
      .nav-circle {
        border-color: $gold;
      }
    }
  }
  .nav-circle {
    width: 70rpx;
    height: 70rpx;
    border-radius: 50%;
    border: 3rpx solid #eee;
    overflow: hidden;
    margin-bottom: 10rpx;
    .nav-avatar {
      width: 100%;
      height: 100%;
    }
  }
  .nav-text-group {
    display: flex;
    flex-direction: column;
    .nav-label {
      font-size: 24rpx;
      color: #444;
    }
    .nav-sub {
      font-size: 18rpx;
      color: #999;
      margin-top: 2rpx;
    }
  }
  .nav-sep {
    width: 2rpx;
    height: 40rpx;
    background: #eee;
    margin: 0 30rpx;
  }
}

.nav-icon-user {
  width: 40rpx;
  height: 40rpx;
  border: 3rpx solid #999;
  border-radius: 50% 50% 0 0;
  position: relative;
  margin-bottom: 10rpx;
  &::after {
    content: "";
    position: absolute;
    top: -12rpx;
    left: 10rpx;
    width: 16rpx;
    height: 16rpx;
    border: 3rpx solid #999;
    border-radius: 50%;
  }
}

/* 首页数字人样式 */
.home-dh-container {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 20;
  pointer-events: none; /* 容器不接受点击，点击会穿透到下面的功能模块 */
}
.home-dh-wrapper {
  pointer-events: auto; /* 数字人本身可以接受点击 */
  width: 180rpx;
  height: 240rpx;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 10rpx 30rpx rgba(179, 140, 79, 0.25);
  border: 3rpx solid rgba(179, 140, 79, 0.3);
  background: rgba(255, 255, 255, 0.9);
  transition: transform 0.2s;
  &:active {
    transform: scale(0.95);
  }
}
.home-dh-img {
  width: 100%;
  height: 100%;
}
</style>
