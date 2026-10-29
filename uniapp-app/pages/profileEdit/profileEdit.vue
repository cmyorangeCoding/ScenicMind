<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { onBackPress } from "@dcloudio/uni-app";
import { getUserId, getNickname } from "@/auth.js";
import request from "@/request.js";

const userId = ref("");
const nickname = ref("");
const phone = ref("");
const originalNickname = ref("");
const originalPhone = ref("");
const isSubmitting = ref(false);
const keyboardHeight = ref(0);

onMounted(() => {
  loadUserProfile();

  // 监听键盘高度变化
  uni.onKeyboardHeightChange(handleKeyboardHeightChange);

  // 隐藏数字人 WebView，避免干扰输入框焦点
  const digitalHuman = plus.webview.getWebviewById("preloadDigitalHuman");
  if (digitalHuman) {
    digitalHuman.hide();
  }
});

const handleKeyboardHeightChange = (e) => {
  if (e && e.detail && typeof e.detail.height === "number") {
    keyboardHeight.value = e.detail.height;
  }
};

onUnmounted(() => {
  uni.offKeyboardHeightChange(handleKeyboardHeightChange);
});

const loadUserProfile = () => {
  userId.value = getUserId();
  const storedNickname = getNickname();
  const storedPhone = uni.getStorageSync("phone") || "";

  originalNickname.value = storedNickname;
  originalPhone.value = storedPhone;
  nickname.value = storedNickname;
  phone.value = storedPhone;
};

const validatePhone = (phoneNum) => {
  const phoneRegex = /^1[3-9]\d{9}$/;
  return phoneRegex.test(phoneNum);
};

const handleSubmit = async () => {
  if (!userId.value) {
    uni.showToast({ title: "用户未登录", icon: "none" });
    return;
  }

  if (!nickname.value || nickname.value.trim() === "") {
    uni.showToast({ title: "用户名不能为空", icon: "none" });
    return;
  }

  if (!phone.value || !validatePhone(phone.value)) {
    uni.showToast({ title: "请输入正确的手机号", icon: "none" });
    return;
  }

  const hasNicknameChanged =
    nickname.value.trim() !== originalNickname.value.trim();
  const hasPhoneChanged = phone.value !== originalPhone.value;

  if (!hasNicknameChanged && !hasPhoneChanged) {
    uni.showToast({ title: "没有修改任何信息", icon: "none" });
    return;
  }

  const dto = {
    userId: userId.value,
  };

  if (hasNicknameChanged) {
    dto.nickname = nickname.value.trim();
  }
  if (hasPhoneChanged) {
    dto.phone = phone.value;
  }

  console.log("========== 提交修改 ==========");
  console.log("DTO:", JSON.stringify(dto));

  isSubmitting.value = true;

  try {
    const res = await request({
      url: "/user/profile",
      method: "PUT",
      data: dto,
    });

    console.log("响应:", JSON.stringify(res));

    if (res.code === 200) {
      uni.setStorageSync("nickname", nickname.value.trim());
      uni.setStorageSync("phone", phone.value);
      uni.showToast({ title: "修改成功", icon: "success" });
      setTimeout(() => {
        uni.navigateBack();
      }, 1500);
    } else {
      uni.showToast({ title: res.message || "修改失败", icon: "none" });
    }
  } catch (e) {
    console.error("请求异常:", e);
    uni.showToast({ title: "网络错误，请重试", icon: "none" });
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <view class="edit-page">
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

    <view
      class="form-container"
      :style="{ paddingBottom: `${keyboardHeight + 60}rpx` }"
    >
      <view class="form-card">
        <view class="form-item">
          <view class="form-label">用户名</view>
          <view class="form-input-wrap">
            <input
              v-model="nickname"
              class="form-input"
              placeholder="请输入用户名"
              maxlength="20"
            />
          </view>
        </view>

        <view class="form-divider"></view>

        <view class="form-item">
          <view class="form-label">手机号</view>
          <view class="form-input-wrap">
            <input
              v-model="phone"
              class="form-input"
              type="number"
              placeholder="请输入手机号"
              maxlength="11"
            />
          </view>
        </view>
      </view>

      <view
        class="submit-btn-wrap"
        :style="{ transform: `translateY(-${keyboardHeight}px)` }"
      >
        <button
          class="submit-btn"
          :disabled="isSubmitting"
          @click="handleSubmit"
        >
          {{ isSubmitting ? "提交中..." : "提交修改" }}
        </button>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
$gold: #b38c4f;
$gold-light: rgba(197, 166, 118, 0.1);
$bg-color: #fdfaf2;

.edit-page {
  min-height: 100vh;
  background-color: $bg-color;
  position: relative;
  overflow: hidden;
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
    width: 280rpx;
    top: 80rpx;
    right: -60rpx;
  }
  .cloud-2 {
    width: 220rpx;
    bottom: 200rpx;
    left: -70rpx;
  }
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

.form-container {
  padding: 40rpx 24rpx;
  position: relative;
  z-index: 1;
}

.form-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 0 28rpx;
  box-shadow: 0 8rpx 24rpx rgba(179, 140, 79, 0.1);
}

.form-item {
  padding: 32rpx 0;
}

.form-label {
  font-size: 28rpx;
  color: #888;
  margin-bottom: 16rpx;
}

.form-input-wrap {
  border: 2rpx solid $gold;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  background: #fff;
  transition: all 0.3s;
}

.form-input-wrap:focus-within {
  border-color: $gold;
  box-shadow: 0 0 0 4rpx $gold-light;
}

.form-input {
  font-size: 30rpx;
  color: #333;
  width: 100%;
}

.form-divider {
  height: 1rpx;
  background: $gold-light;
}

.submit-btn-wrap {
  margin-top: 50rpx;
  padding: 0 10rpx;
}

.submit-btn {
  width: 100%;
  height: 96rpx;
  line-height: 96rpx;
  background: linear-gradient(135deg, #b38c4f 0%, #c5a676 100%);
  border-radius: 48rpx;
  font-size: 32rpx;
  font-weight: bold;
  color: #fff;
  border: none;
  box-shadow: 0 12rpx 24rpx rgba(179, 140, 79, 0.3);
}

.submit-btn[disabled] {
  background: #ccc;
  box-shadow: none;
}
</style>
