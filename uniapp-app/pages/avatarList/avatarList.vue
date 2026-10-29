<script setup>
import { ref, onMounted } from "vue";
import { onBackPress } from "@dcloudio/uni-app";
import request from "@/request.js";

const avatarList = ref([]);
const currentAvatarId = ref("");
const loading = ref(true);
const selectedAvatar = ref(null);

const defaultAvatarConfig = {
  avatarId: "111034002",
  vcn: "x5_lingyouyou_flow_flow",
  speed: 54,
  pitch: 55,
  volume: 59,
};

onMounted(() => {
  loadUserAvatarConfig();
  fetchAvatarList();
});

const loadUserAvatarConfig = () => {
  const savedConfig = uni.getStorageSync("user_avatar_config");
  if (savedConfig && savedConfig.avatarId) {
    currentAvatarId.value = savedConfig.avatarId;
    selectedAvatar.value = null;
    console.log("[数字人配置] 已加载用户保存的配置:", savedConfig);
  } else {
    currentAvatarId.value = defaultAvatarConfig.avatarId;
    console.log("[数字人配置] 使用默认配置:", defaultAvatarConfig);
  }
};

const fetchAvatarList = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/avatar/list");
    console.log("[数字人列表] 后端返回:", res);

    if (res && res.code === 200 && res.data) {
      avatarList.value = res.data;
      console.log("[数字人列表] 成功获取:", avatarList.value.length, "个形象");

      // 检查当前currentAvatarId是否在列表中存在
      const matchedAvatar = avatarList.value.find(
        (a) => a.avatarId === currentAvatarId.value,
      );

      if (!matchedAvatar) {
        // 如果没有匹配项，不自动选择，保持currentAvatarId不变
        // 用户需要手动选择一个数字人
        console.log("[数字人配置] 当前使用的形象不在列表中，请手动选择");
      }
    } else {
      console.error("[数字人列表] 获取失败:", res?.message || "未知错误");
      uni.showToast({ title: "获取列表失败", icon: "none" });
    }
  } catch (e) {
    console.error("[数字人列表] 请求异常:", e);
    uni.showToast({ title: "网络请求失败", icon: "none" });
  } finally {
    loading.value = false;
  }
};

const selectAvatar = (avatar) => {
  if (avatar.status !== 1) {
    uni.showToast({ title: "该形象暂不可用", icon: "none" });
    return;
  }

  selectedAvatar.value = avatar;
  currentAvatarId.value = avatar.avatarId;
  console.log("[数字人选择] 用户选择:", avatar.avatarName, avatar.avatarId);
};

const confirmUseAvatar = () => {
  if (!selectedAvatar.value) {
    uni.showToast({ title: "请先选择一个形象", icon: "none" });
    return;
  }

  // 先弹出弹窗询问用户是否确认更换
  uni.showModal({
    title: "提示",
    content: "确定要更换数字人形象吗？更换后需要重新启动APP才能生效。",
    confirmText: "确定",
    confirmColor: "#b38c4f",
    cancelText: "取消",
    success: (res) => {
      if (res.confirm) {
        // 用户点击确定，才保存配置
        const avatar = selectedAvatar.value;
        const configToSave = {
          avatarId: avatar.avatarId,
          vcn: avatar.vcn || defaultAvatarConfig.vcn,
          speed: avatar.speed || defaultAvatarConfig.speed,
          pitch: avatar.pitch || defaultAvatarConfig.pitch,
          volume: avatar.volume || defaultAvatarConfig.volume,
          avatarName: avatar.avatarName,
          avatarImage: avatar.avatarImage,
        };

        uni.setStorageSync("user_avatar_config", configToSave);
        console.log("[数字人配置] 已保存到本地:", configToSave);

        // 重启APP
        // #ifdef APP-PLUS
        console.log("[数字人配置] 用户选择重启APP");
        plus.runtime.restart();
        // #endif
        // #ifndef APP-PLUS
        uni.showToast({ title: "H5环境请手动刷新", icon: "none" });
        setTimeout(() => {
          uni.navigateBack();
        }, 1500);
        // #endif
      } else {
        // 用户点击取消，什么都不做，弹窗自动关闭
        console.log("[数字人配置] 用户取消更换形象");
      }
    },
  });
};

const getAvatarImage = (avatar) => {
  if (avatar.avatarImage) {
    return avatar.avatarImage;
  }
  return "/static/digitalHuman.jpg";
};

const isSelected = (avatar) => {
  return currentAvatarId.value === avatar.avatarId;
};

const getStatusText = (status) => {
  return status === 1 ? "可用" : "不可用";
};

const getStatusClass = (status) => {
  return status === 1 ? "可用" : "不可用";
};
</script>

<template>
  <view class="avatar-list-page">
    <view v-if="loading" class="loading-wrap">
      <text class="loading-text">加载中...</text>
    </view>

    <view v-else class="avatar-list">
      <view
        v-for="avatar in avatarList"
        :key="avatar.avatarId"
        class="avatar-card"
        :class="{ 'avatar-selected': isSelected(avatar) }"
        @click="selectAvatar(avatar)"
      >
        <view class="avatar-left">
          <image
            class="avatar-image"
            :src="getAvatarImage(avatar)"
            mode="aspectFill"
          />
          <view v-if="isSelected(avatar)" class="selected-badge">
            <text class="selected-icon">✓</text>
          </view>
        </view>

        <view class="avatar-right">
          <view class="avatar-name-wrap">
            <text class="avatar-name">{{
              avatar.avatarName || "未命名形象"
            }}</text>
            <view class="avatar-status" :class="getStatusClass(avatar.status)">
              <text class="status-text">{{
                getStatusText(avatar.status)
              }}</text>
            </view>
          </view>

          <view class="config-info">
            <view class="config-row">
              <view class="config-item">
                <text class="config-label">语速</text>
                <text class="config-value">{{ avatar.speed || 50 }}</text>
              </view>
              <view class="config-item">
                <text class="config-label">语调</text>
                <text class="config-value">{{ avatar.pitch || 50 }}</text>
              </view>
            </view>
            <view class="config-row">
              <view class="config-item">
                <text class="config-label">音量</text>
                <text class="config-value">{{ avatar.volume || 50 }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view v-if="!loading && avatarList.length === 0" class="empty-wrap">
      <text class="empty-text">暂无可用数字人形象</text>
    </view>

    <view class="action-bar">
      <button class="confirm-btn" @click="confirmUseAvatar">确认使用</button>
    </view>
  </view>
</template>

<style scoped lang="scss">
$gold: #b38c4f;
$gold-light: rgba(197, 166, 118, 0.15);
$bg-color: #fdfaf2;

.avatar-list-page {
  min-height: 100vh;
  background-color: $bg-color;
  padding: 30rpx 24rpx 200rpx;
  box-sizing: border-box;
}

.loading-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 100rpx 0;
}

.loading-text {
  font-size: 28rpx;
  color: #888;
}

.avatar-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.avatar-card {
  display: flex;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  border: 3rpx solid transparent;
}

.avatar-selected {
  border-color: $gold;
  box-shadow: 0 8rpx 28rpx rgba(179, 140, 79, 0.18);
  background: linear-gradient(135deg, #fff 0%, #fdf8f0 100%);
}

.avatar-left {
  position: relative;
  width: 160rpx;
  height: 160rpx;
  flex-shrink: 0;
}

.avatar-image {
  width: 100%;
  height: 100%;
  border-radius: 16rpx;
  background: #f5f5f5;
}

.selected-badge {
  position: absolute;
  top: -10rpx;
  right: -10rpx;
  width: 48rpx;
  height: 48rpx;
  background: linear-gradient(135deg, $gold 0%, #c5a676 100%);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 4rpx 12rpx rgba(179, 140, 79, 0.4);
}

.selected-icon {
  color: #fff;
  font-size: 28rpx;
  font-weight: bold;
}

.avatar-right {
  flex: 1;
  margin-left: 24rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.avatar-name-wrap {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.avatar-name {
  font-size: 34rpx;
  font-weight: 600;
  color: #333;
}

.avatar-status {
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
}

.status-enabled {
  background: rgba(76, 175, 80, 0.1);
}

.status-disabled {
  background: rgba(244, 67, 54, 0.1);
}

.status-text {
  font-size: 22rpx;
}

.status-enabled .status-text {
  color: #4caf50;
}

.status-disabled .status-text {
  color: #f44336;
}

.config-info {
  margin-top: 16rpx;
}

.config-row {
  display: flex;
  gap: 24rpx;
}

.config-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.config-label {
  font-size: 24rpx;
  color: #999;
}

.config-value {
  font-size: 24rpx;
  color: #666;
  font-weight: 500;
}

.empty-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 100rpx 0;
}

.empty-text {
  font-size: 28rpx;
  color: #888;
}

.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 30rpx 24rpx;
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.08);
}

.confirm-btn {
  width: 100%;
  height: 92rpx;
  line-height: 92rpx;
  background: linear-gradient(135deg, #b38c4f 0%, #c5a676 100%);
  border-radius: 46rpx;
  font-size: 32rpx;
  color: #fff;
  font-weight: bold;
  border: none;
  box-shadow: 0 10rpx 24rpx rgba(179, 140, 79, 0.35);
}
</style>
