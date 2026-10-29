<script setup>
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { getUserId } from "@/auth.js";
import request from "@/request.js";

const noteId = ref("");
const noteDetail = ref(null);
const isLoading = ref(true);

onLoad((options) => {
  noteId.value = options.noteId || "";

  if (noteId.value) {
    loadNoteDetail();
  } else {
    uni.showToast({ title: "游记ID不存在", icon: "none" });
    isLoading.value = false;
  }
});

const loadNoteDetail = () => {
  const userId = getUserId();
  if (!userId) {
    uni.showToast({ title: "请先登录", icon: "none" });
    isLoading.value = false;
    return;
  }

  isLoading.value = true;
  console.log("========== 加载游记详情 ==========");
  console.log("noteId:", noteId.value);
  console.log("userId:", userId);

  request({
    url: `/user/travel-note/${noteId.value}?userId=${userId}`,
    method: "GET",
  })
    .then((res) => {
      console.log("游记详情返回:", JSON.stringify(res));
      isLoading.value = false;
      if (res.code === 200 && res.data) {
        noteDetail.value = res.data;
      } else {
        uni.showToast({ title: res.message || "加载失败", icon: "none" });
      }
    })
    .catch((err) => {
      console.error("请求异常:", err);
      isLoading.value = false;
      uni.showToast({ title: "加载失败", icon: "none" });
    });
};

const formatDate = (dateStr) => {
  if (!dateStr) return "";
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}年${month}月${day}日`;
};

const formatTime = (dateStr) => {
  if (!dateStr) return "";
  const date = new Date(dateStr);
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${hours}:${minutes}`;
};

const formatDateTime = (dateStr) => {
  if (!dateStr) return "";
  const date = new Date(dateStr);
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${month}月${day}日 ${hours}:${minutes}`;
};

const formatDuration = (hours) => {
  if (!hours) return "0分钟";
  if (hours < 1) return Math.round(hours * 60) + "分钟";
  const h = Math.floor(hours);
  const m = Math.round((hours - h) * 60);
  if (m > 0) {
    return `${h}小时${m}分钟`;
  }
  return `${h}小时`;
};

const goBack = () => {
  uni.navigateBack();
};
</script>

<template>
  <view class="note-page">
    <view class="cloud-bg">
      <image
        class="cloud-img cloud-1"
        src="/static/cloud_detail.jpg"
        mode="aspectFit"
      />
      <image
        class="cloud-img cloud-2"
        src="/static/cloud_detail.jpg"
        mode="aspectFit"
      />
    </view>

    <view v-if="isLoading" class="loading">
      <text>加载中...</text>
    </view>

    <view v-else-if="!noteDetail" class="empty">
      <text class="empty-icon">📝</text>
      <text class="empty-text">游记不存在</text>
    </view>

    <view v-else class="note-content">
      <view class="note-hero">
        <view class="hero-decoration">
          <view class="decoration-line"></view>
          <view class="decoration-dot"></view>
          <view class="decoration-line"></view>
        </view>
        <text class="hero-title">{{ noteDetail.title || "灵山胜境游记" }}</text>
        <text class="hero-date">{{ formatDate(noteDetail.createTime) }}</text>
      </view>

      <view class="info-card">
        <view class="info-item">
          <view class="info-icon">📅</view>
          <view class="info-content">
            <text class="info-label">游览日期</text>
            <text class="info-value">{{
              formatDate(noteDetail.startTime)
            }}</text>
          </view>
        </view>
        <view class="info-divider"></view>
        <view class="info-item">
          <view class="info-icon">⏱️</view>
          <view class="info-content">
            <text class="info-label">游览时长</text>
            <text class="info-value">{{
              formatDuration(noteDetail.durationHours)
            }}</text>
          </view>
        </view>
      </view>

      <view
        v-if="noteDetail.visitedSpots && noteDetail.visitedSpots.length > 0"
        class="spots-section"
      >
        <view class="section-header">
          <view class="section-line"></view>
          <text class="section-title">游览足迹</text>
          <view class="section-line"></view>
        </view>

        <view class="timeline">
          <view
            v-for="(spot, index) in noteDetail.visitedSpots"
            :key="index"
            class="timeline-item"
          >
            <view class="timeline-time">
              <text class="time-text">{{ formatTime(spot.arrivalTime) }}</text>
            </view>
            <view class="timeline-dot"></view>
            <view class="timeline-content">
              <text class="spot-name">{{ spot.spotName }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="noteDetail.content" class="content-section">
        <view class="section-header">
          <view class="section-line"></view>
          <text class="section-title">游记内容</text>
          <view class="section-line"></view>
        </view>

        <view class="content-card">
          <text class="content-text">{{ noteDetail.content }}</text>
        </view>
      </view>

      <view class="note-footer">
        <text class="footer-text">— 灵山胜境数字人导览 —</text>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
$gold: #b38c4f;
$gold-light: rgba(197, 166, 118, 0.1);
$bg-color: #fdfaf2;

.note-page {
  min-height: 100vh;
  background-color: $bg-color;
  position: relative;
  padding-bottom: 60rpx;
}

.cloud-bg {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
  .cloud-img {
    position: absolute;
    opacity: 0.15;
  }
  .cloud-1 {
    width: 300rpx;
    top: 100rpx;
    right: -50rpx;
  }
  .cloud-2 {
    width: 250rpx;
    bottom: 300rpx;
    left: -80rpx;
  }
}

.page-header {
  background: rgba(255, 255, 255, 0.95);
  padding: 60rpx 30rpx 30rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.back-btn {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-icon {
  font-size: 36rpx;
  color: #666;
}

.page-title {
  font-size: 34rpx;
  font-weight: bold;
  color: #333;
}

.placeholder {
  width: 60rpx;
}

.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 150rpx 0;
  color: #999;
  font-size: 28rpx;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 150rpx 40rpx;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 30rpx;
}

.empty-text {
  font-size: 32rpx;
  color: #666;
  font-weight: 500;
}

.note-content {
  padding: 30rpx 24rpx;
  position: relative;
  z-index: 1;
}

.note-hero {
  background: linear-gradient(135deg, $gold 0%, #c5a676 100%);
  border-radius: 24rpx;
  padding: 50rpx 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 12rpx 32rpx rgba(179, 140, 79, 0.25);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.hero-decoration {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
  width: 100%;
  justify-content: center;
}

.decoration-line {
  width: 60rpx;
  height: 2rpx;
  background: rgba(255, 255, 255, 0.5);
}

.decoration-dot {
  width: 12rpx;
  height: 12rpx;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  margin: 0 16rpx;
}

.hero-title {
  font-size: 40rpx;
  font-weight: bold;
  color: #fff;
  margin-bottom: 16rpx;
  text-align: center;
}

.hero-date {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.9);
}

.info-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx 28rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
}

.info-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.info-icon {
  font-size: 40rpx;
}

.info-content {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.info-label {
  font-size: 24rpx;
  color: #999;
}

.info-value {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

.info-divider {
  width: 2rpx;
  height: 60rpx;
  background: #f0f0f0;
  margin: 0 24rpx;
}

.spots-section {
  margin-bottom: 30rpx;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
  gap: 16rpx;
}

.section-line {
  width: 40rpx;
  height: 2rpx;
  background: $gold;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
}

.timeline {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx 28rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
}

.timeline-item {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  position: relative;

  &:not(:last-child)::after {
    content: "";
    position: absolute;
    left: 118rpx;
    top: 60rpx;
    width: 2rpx;
    height: calc(100% - 40rpx);
    background: linear-gradient(to bottom, $gold, rgba(179, 140, 79, 0.2));
  }
}

.timeline-time {
  width: 100rpx;
  text-align: right;
  margin-right: 16rpx;
}

.time-text {
  font-size: 24rpx;
  color: #999;
}

.timeline-dot {
  width: 16rpx;
  height: 16rpx;
  background: $gold;
  border-radius: 50%;
  margin-right: 16rpx;
  box-shadow: 0 0 0 6rpx rgba(179, 140, 79, 0.2);
  flex-shrink: 0;
}

.timeline-content {
  flex: 1;
  background: rgba(179, 140, 79, 0.05);
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
}

.spot-name {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

.content-section {
  margin-bottom: 30rpx;
}

.content-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx 28rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
}

.content-text {
  font-size: 28rpx;
  color: #555;
  line-height: 48rpx;
  white-space: pre-wrap;
}

.note-footer {
  text-align: center;
  padding: 40rpx 0;
}

.footer-text {
  font-size: 24rpx;
  color: #999;
  letter-spacing: 4rpx;
}
</style>
