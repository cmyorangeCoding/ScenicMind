<script setup>
import { ref, onMounted } from "vue";
import { onBackPress } from "@dcloudio/uni-app";
import { getUserId } from "@/auth.js";
import request from "@/request.js";

const travelNotes = ref([]);
const isLoading = ref(true);

onMounted(() => {
  loadTravelNotes();
});

const loadTravelNotes = () => {
  const userId = getUserId();
  if (!userId) {
    uni.showToast({ title: "请先登录", icon: "none" });
    isLoading.value = false;
    return;
  }

  isLoading.value = true;
  console.log("========== 加载游记列表 ==========");
  console.log("userId:", userId);

  request({
    url: "/user/travel-note/list?userId=" + userId,
    method: "GET",
  })
    .then((res) => {
      console.log("后端返回:", JSON.stringify(res));
      isLoading.value = false;
      if (res.code === 200 && res.data) {
        travelNotes.value = res.data || [];
        console.log("解析到游记数量:", travelNotes.value.length);
      } else {
        console.log("接口返回失败:", res.message);
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
  return year + "-" + month + "-" + day;
};

const formatDuration = (hours) => {
  if (!hours) return "0小时";
  if (hours < 1) return Math.round(hours * 60) + "分钟";
  return hours.toFixed(1) + "小时";
};

const goToNoteDetail = (noteId) => {
  if (!noteId) {
    uni.showToast({ title: "游记ID不存在", icon: "none" });
    return;
  }
  uni.navigateTo({
    url: "/pages/travelNote/travelNote?noteId=" + noteId,
  });
};
</script>

<template>
  <view class="travel-page">
    <view v-if="isLoading" class="loading">
      <text>加载中...</text>
    </view>

    <view v-else-if="travelNotes.length === 0" class="empty">
      <text class="empty-icon">📝</text>
      <text class="empty-text">暂无游记记录</text>
      <text class="empty-hint">完成导览后将自动生成游记</text>
    </view>

    <view v-else class="notes-list">
      <view
        v-for="(note, index) in travelNotes"
        :key="note.id || index"
        class="note-card"
        @click="goToNoteDetail(note.id)"
      >
        <view class="note-header">
          <text class="note-title">{{ note.title || "灵山胜境游记" }}</text>
          <text class="note-date">{{ formatDate(note.createTime) }}</text>
        </view>
        <view class="note-info">
          <view class="info-row">
            <text class="info-label">游览时间</text>
            <text class="info-value"
              >{{ formatDate(note.startTime) }} -
              {{ formatDate(note.endTime) }}</text
            >
          </view>
          <view class="info-row">
            <text class="info-label">游玩时长</text>
            <text class="info-value">{{
              formatDuration(note.durationHours)
            }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.travel-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8f9fa 0%, #f0f2f5 100%);
  padding-bottom: 40rpx;
}

.page-header {
  background: #fff;
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
  margin-bottom: 16rpx;
}

.empty-hint {
  font-size: 24rpx;
  color: #999;
}

.notes-list {
  padding: 30rpx 24rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.note-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx 28rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx dashed #f0f0f0;
}

.note-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  flex: 1;
  margin-right: 20rpx;
}

.note-date {
  font-size: 24rpx;
  color: #999;
  flex-shrink: 0;
}

.note-info {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.info-row {
  display: flex;
  align-items: center;
}

.info-label {
  font-size: 26rpx;
  color: #999;
  width: 140rpx;
  flex-shrink: 0;
}

.info-value {
  font-size: 26rpx;
  color: #666;
  flex: 1;
}
</style>
