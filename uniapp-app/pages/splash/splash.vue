<template>
  <view class="splash-container">
    <!-- 轮播图 -->
    <swiper
      class="splash-swiper"
      :indicator-dots="showDots"
      :autoplay="isAutoPlay && !isLastPage"
      :interval="1000"
      :duration="500"
      :circular="false"
      :current="currentIndex"
      @change="onSwiperChange"
    >
      <swiper-item v-for="(img, index) in images" :key="index">
        <image :src="img" class="splash-image" mode="aspectFill"></image>
      </swiper-item>
    </swiper>

    <!-- 跳过按钮（非最后一页显示） -->
    <view v-if="!isLastPage" class="skip-btn" @tap="goToIndex">
      <text class="skip-text">跳过</text>
    </view>

    <!-- 进入体验按钮（最后一页显示） -->
    <view v-if="isLastPage" class="enter-btn-container">
      <view class="enter-btn" @tap="enterExperience">
        <text class="enter-text">进入体验</text>
      </view>
    </view>

    <!-- 页码指示器 -->
    <view class="page-indicator">
      <view
        v-for="(_, index) in images"
        :key="index"
        :class="['indicator-dot', { active: currentIndex === index }]"
      ></view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from "vue";
import { isTokenValid, getToken } from "../../auth.js";

// 轮播图片列表
const images = [
  "/static/splash/1.jpg",
  "/static/splash/2.jpg",
  "/static/splash/3.jpg",
  "/static/splash/4.jpg",
  "/static/splash/5.jpg",
];

// 当前索引
const currentIndex = ref(0);

// 是否显示指示器
const showDots = ref(false);

// 是否自动播放
const isAutoPlay = ref(true);

// 是否是最后一页
const isLastPage = computed(() => {
  return currentIndex.value === images.length - 1;
});

// 轮播图切换事件
const onSwiperChange = (e) => {
  currentIndex.value = e.detail.current;
};

// 鉴权并跳转
const checkAuthAndNavigate = () => {
  // 1. 先看 token 是否存在
  const token = getToken();
  console.log("当前token：", token);

  // 2. 再看 token 是否有效
  const valid = isTokenValid();
  console.log("token是否有效：", valid);

  // 核心逻辑
  if (token && valid) {
    // 有token + 有效 → 跳主页
    uni.reLaunch({
      url: "/pages/index/index",
    });
  } else {
    // 无token 或 token无效 → 跳登录
    uni.reLaunch({
      url: "/pages/login/login",
    });
  }
};

// 跳过按钮点击
const goToIndex = () => {
  checkAuthAndNavigate();
};

// 进入体验按钮点击
const enterExperience = () => {
  checkAuthAndNavigate();
};
</script>

<style lang="scss">
.splash-container {
  width: 100vw;
  height: 100vh;
  position: relative;
  overflow: hidden;
  background: #fff;
}

.splash-swiper {
  width: 100%;
  height: 100%;
}

.splash-image {
  width: 100%;
  height: 100%;
}

.skip-btn {
  position: fixed;
  top: calc(var(--status-bar-height, 44px) + 20rpx);
  right: 30rpx;
  padding: 12rpx 24rpx;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 30rpx;
  z-index: 100;
}

.skip-text {
  font-size: 26rpx;
  color: #fff;
}

.enter-btn-container {
  position: fixed;
  bottom: 100rpx;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
}

.enter-btn {
  padding: 24rpx 80rpx;
  background: linear-gradient(135deg, #d4af37 0%, #b38c4f 100%);
  border-radius: 40rpx;
  box-shadow: 0 8rpx 24rpx rgba(212, 175, 55, 0.3);
}

.enter-text {
  font-size: 32rpx;
  color: #fff;
  font-weight: 500;
}

.page-indicator {
  position: fixed;
  bottom: 40rpx;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 12rpx;
  z-index: 100;
}

.indicator-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  transition: all 0.3s ease;
}

.indicator-dot.active {
  width: 32rpx;
  border-radius: 6rpx;
  background: #fff;
}
</style>
