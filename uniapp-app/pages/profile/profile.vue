<script setup>
import { ref, onMounted } from "vue";
import { onShow } from "@dcloudio/uni-app";
import {
  getNickname,
  getAvatarUrl,
  getUserId,
  getToken,
  clearAuth,
} from "@/auth.js";
import request from "@/request.js";
import UniIcons from "@dcloudio/uni-ui/lib/uni-icons/uni-icons.vue";

const nickname = ref("");
const avatarUrl = ref("");
const phone = ref("");

const loadUserInfo = () => {
  nickname.value = getNickname() || "游客朋友";
  avatarUrl.value = getAvatarUrl() || "/static/digitalHuman.jpg";
  const storedPhone = uni.getStorageSync("phone") || "";
  if (storedPhone && storedPhone.length >= 11) {
    phone.value = storedPhone.replace(/(\d{3})\d{4}(\d{4})/, "$1****$2");
  } else {
    phone.value = storedPhone || "暂无手机号";
  }
};

onMounted(() => {
  loadUserInfo();
});

onShow(() => {
  loadUserInfo();
  // 隐藏数字人，避免从子页面返回时显示
  const digitalHuman = plus.webview.getWebviewById("preloadDigitalHuman");
  if (digitalHuman) {
    digitalHuman.hide();
  }
});

const goToCollection = () => {
  uni.navigateTo({ url: "/pages/routeCollection/routeCollection" });
};

const goToTravelJournal = () => {
  uni.navigateTo({ url: "/pages/travelJournal/travelJournal" });
};

const goToProfileEdit = () => {
  uni.navigateTo({ url: "/pages/profileEdit/profileEdit" });
};

const goToFeedbackList = () => {
  uni.navigateTo({ url: "/pages/feedbackList/feedbackList" });
};

const goToAvatarList = () => {
  uni.navigateTo({ url: "/pages/avatarList/avatarList" });
};

const handleLogout = async () => {
  uni.showModal({
    title: "提示",
    content: "确定要退出登录吗？",
    confirmText: "确定",
    confirmColor: "#b8976d",
    cancelText: "取消",
    success: async (res) => {
      if (res.confirm) {
        try {
          const token = getToken();
          if (token) {
            await request({
              url: "/user/auth/logout",
              method: "POST",
            });
          }
        } catch (e) {
          console.log("登出请求失败:", e);
        }
        clearAuth();
        uni.removeStorageSync("phone");
        uni.reLaunch({ url: "/pages/login/login" });
      }
    },
  });
};

const goHome = () => {
  uni.reLaunch({ url: "/pages/index/index" });
};
</script>

<template>
  <view class="profile-page">
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

    <view class="profile-header" @click="goToProfileEdit">
      <image
        class="avatar"
        :src="avatarUrl"
        mode="aspectFill"
        @error="avatarUrl = '/static/digitalHuman.jpg'"
      />
      <view class="profile-text">
        <text class="name">{{ nickname }}</text>
        <text class="phone">{{ phone }}</text>
      </view>
      <uni-icons type="right" size="20" color="#ccc" />
    </view>

    <view class="tool-list">
      <view class="tool-item" @click="goToCollection">
        <view class="tool-left">
          <uni-icons type="star" size="22" color="#b38c4f" />
          <text class="tool-name">收藏路线</text>
        </view>
        <uni-icons type="right" size="18" color="#ccc" />
      </view>
      <view class="tool-item" @click="goToTravelJournal">
        <view class="tool-left">
          <uni-icons type="compose" size="22" color="#b38c4f" />
          <text class="tool-name">游记列表</text>
        </view>
        <uni-icons type="right" size="18" color="#ccc" />
      </view>
      <view class="tool-item" @click="goToProfileEdit">
        <view class="tool-left">
          <uni-icons type="person" size="22" color="#b38c4f" />
          <text class="tool-name">游客信息修改</text>
        </view>
        <uni-icons type="right" size="18" color="#ccc" />
      </view>
      <view class="tool-item" @click="goToAvatarList">
        <view class="tool-left">
          <uni-icons type="camera" size="22" color="#b38c4f" />
          <text class="tool-name">数字人形象管理</text>
        </view>
        <uni-icons type="right" size="18" color="#ccc" />
      </view>
      <view class="tool-item" @click="goToFeedbackList">
        <view class="tool-left">
          <uni-icons type="chat" size="22" color="#b38c4f" />
          <text class="tool-name">反馈列表</text>
        </view>
        <uni-icons type="right" size="18" color="#ccc" />
      </view>
    </view>

    <view class="logout-btn-wrap">
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </view>
  </view>
</template>

<style scoped lang="scss">
$gold: #b38c4f;
$gold-light: rgba(197, 166, 118, 0.1);
$bg-color: #fdfaf2;

.profile-page {
  height: 100vh;
  background-color: $bg-color;
  padding: 30rpx 24rpx 200rpx;
  position: relative;
  overflow: hidden;
  box-sizing: border-box;
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

.profile-header {
  background: #fff;
  border-radius: 24rpx;
  padding: 36rpx 30rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
  margin-bottom: 30rpx;
  margin-top: 30rpx;
  position: relative;
  z-index: 1;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: #eee;
  flex-shrink: 0;
}

.profile-text {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.name {
  font-size: 36rpx;
  font-weight: 700;
  color: #333;
}

.phone {
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #888;
}

.tool-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  margin-bottom: 40rpx;
  margin-top: 60rpx;
  position: relative;
  z-index: 1;
}

.tool-item {
  height: 60rpx;
  margin: 5rpx 0;
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.tool-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.tool-name {
  font-size: 32rpx;
  color: #333;
  font-weight: 500;
}

.logout-btn-wrap {
  margin-top: 60rpx;
  padding: 0 20rpx;
  position: relative;
  z-index: 1;
}

.logout-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #b38c4f 0%, #c5a676 100%);
  border-radius: 44rpx;
  font-size: 30rpx;
  color: #fff;
  font-weight: bold;
  border: none;
  box-shadow: 0 8rpx 20rpx rgba(179, 140, 79, 0.3);
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
  margin-bottom: 30rpx;
}

.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.nav-icon-home {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: #999;
  margin-bottom: 10rpx;
}

.nav-icon-mine {
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
  &.active {
    background: $gold;
    border-color: $gold;
    &::after {
      background: $gold;
      border-color: $gold;
    }
  }
}

.nav-text-group {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.nav-label {
  font-size: 24rpx;
  color: #444;
}

.nav-sub {
  font-size: 18rpx;
  color: #999;
  margin-top: 2rpx;
}

.nav-item.active .nav-label {
  color: $gold;
  font-weight: bold;
}

.nav-sep {
  width: 2rpx;
  height: 40rpx;
  background: #eee;
  margin: 0 30rpx;
}
</style>
