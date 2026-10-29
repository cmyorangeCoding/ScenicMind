<script setup>
import { ref, onMounted } from "vue";
import { onBackPress } from "@dcloudio/uni-app";
import { getUserId } from "@/auth.js";
import request from "@/request.js";

const complaints = ref([]);
const isLoading = ref(true);

onMounted(() => {
  loadComplaints();
});

const loadComplaints = () => {
  const userId = getUserId();
  if (!userId) {
    uni.showToast({ title: "请先登录", icon: "none" });
    isLoading.value = false;
    return;
  }

  isLoading.value = true;
  console.log("========== 加载投诉列表 ==========");
  console.log("userId:", userId);

  request({
    url: "/user/complaint/list?userId=" + userId,
    method: "GET",
  })
    .then((res) => {
      console.log("后端返回:", JSON.stringify(res));
      isLoading.value = false;
      // 兼容多种成功状态码
      if ((res.code === 200 || res.code === 0) && res.data) {
        complaints.value = res.data || [];
        console.log("解析到投诉数量:", complaints.value.length);
      } else if (res.data && Array.isArray(res.data)) {
        complaints.value = res.data;
        console.log("解析到投诉数量:", complaints.value.length);
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

const getStatusText = (status) => {
  const statusMap = {
    0: "待处理",
    1: "处理中",
    2: "已处理",
  };
  return statusMap[status] || "未知";
};

const getStatusClass = (status) => {
  const classMap = {
    0: "status-pending",
    1: "status-processing",
    2: "status-done",
  };
  return classMap[status] || "status-pending";
};

const formatDate = (dateStr) => {
  if (!dateStr) return "";
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return year + "-" + month + "-" + day;
};
</script>

<template>
  <view class="list-page">
    <view v-if="isLoading" class="loading">
      <text>加载中...</text>
    </view>

    <view v-else-if="complaints.length === 0" class="empty">
      <text class="empty-icon">📋</text>
      <text class="empty-text">暂无反馈记录</text>
      <text class="empty-hint">您提交的反馈将在此处显示</text>
    </view>

    <view v-else class="complaints-list">
      <view
        v-for="(item, index) in complaints"
        :key="item.id || index"
        class="complaint-card"
      >
        <view class="card-header">
          <text class="complaint-type">{{ item.complaintType }}</text>
          <text :class="['complaint-status', getStatusClass(item.status)]">
            {{ getStatusText(item.status) }}
          </text>
        </view>
        <view class="card-content">
          <text class="content-text">{{ item.content }}</text>
        </view>
        <view v-if="item.replyContent" class="card-reply">
          <text class="reply-label">官方回复</text>
          <text class="reply-text">{{ item.replyContent }}</text>
        </view>
        <view class="card-footer">
          <text class="create-time">{{ formatDate(item.createTime) }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.list-page {
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

.complaints-list {
  padding: 30rpx 24rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.complaint-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx 28rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.complaint-type {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
}

.complaint-status {
  font-size: 24rpx;
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
}

.status-pending {
  background: #fff7e6;
  color: #fa8c16;
}

.status-processing {
  background: #e6f7ff;
  color: #1890ff;
}

.status-done {
  background: #f6ffed;
  color: #52c41a;
}

.card-content {
  padding-bottom: 20rpx;
  border-bottom: 1rpx dashed #f0f0f0;
  margin-bottom: 16rpx;
}

.content-text {
  font-size: 28rpx;
  color: #666;
  line-height: 1.6;
}

.card-reply {
  background: #fafafa;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;
}

.reply-label {
  display: block;
  font-size: 24rpx;
  color: #b38c4f;
  margin-bottom: 10rpx;
  font-weight: 500;
}

.reply-text {
  display: block;
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
}

.create-time {
  font-size: 24rpx;
  color: #999;
}
</style>
