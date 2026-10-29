<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { getUserId } from "@/auth.js";
import request from "@/request.js";

const complaintType = ref("");
const content = ref("");
const isSubmitting = ref(false);
const keyboardHeight = ref(0);

const typeOptions = [
  { label: "服务态度", value: "服务态度" },
  { label: "设施问题", value: "设施问题" },
  { label: "环境卫生", value: "环境卫生" },
  { label: "APP体验", value: "app体验" },
  { label: "其他", value: "其他" },
];

const showTypePicker = () => {
  const types = typeOptions.map((t) => t.label);
  uni.showActionSheet({
    itemList: types,
    success: (res) => {
      complaintType.value = typeOptions[res.tapIndex].value;
    },
  });
};

const handleSubmit = async () => {
  const userId = getUserId();
  if (!userId) {
    uni.showToast({ title: "请先登录", icon: "none" });
    return;
  }

  if (!complaintType.value) {
    uni.showToast({ title: "请选择建议类型", icon: "none" });
    return;
  }

  if (!content.value || content.value.trim() === "") {
    uni.showToast({ title: "请输入建议内容", icon: "none" });
    return;
  }

  isSubmitting.value = true;

  const dto = {
    userId: userId,
    complaintType: complaintType.value,
    content: content.value.trim(),
  };

  console.log("========== 提交建议 ==========");
  console.log("DTO:", JSON.stringify(dto));

  try {
    const res = await request({
      url: "/user/complaint/submit",
      method: "POST",
      data: dto,
    });

    console.log("响应:", JSON.stringify(res));

    if (res.code === 200) {
      uni.showToast({ title: "提交成功，感谢您的反馈", icon: "success" });
      setTimeout(() => {
        uni.navigateBack();
      }, 1500);
    } else {
      uni.showToast({ title: res.message || "提交失败", icon: "none" });
    }
  } catch (e) {
    console.error("请求异常:", e);
    uni.showToast({ title: "网络错误，请重试", icon: "none" });
  } finally {
    isSubmitting.value = false;
  }
};

const goBack = () => {
  uni.navigateBack();
};

const handleKeyboardHeightChange = (e) => {
  if (e && e.detail && typeof e.detail.height === "number") {
    keyboardHeight.value = e.detail.height;
  }
};

onMounted(() => {
  // 监听键盘高度变化
  uni.onKeyboardHeightChange(handleKeyboardHeightChange);

  // 隐藏数字人 WebView，避免干扰输入框焦点
  const digitalHuman = plus.webview.getWebviewById("preloadDigitalHuman");
  if (digitalHuman) {
    digitalHuman.hide();
  }
});

onUnmounted(() => {
  uni.offKeyboardHeightChange(handleKeyboardHeightChange);
});
</script>

<template>
  <view class="feedback-page">
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
        <view class="form-item" @click="showTypePicker">
          <view class="form-label">反馈类型</view>
          <view class="form-value-wrap">
            <text :class="['form-value', { placeholder: !complaintType }]">
              {{ complaintType || "请选择反馈类型" }}
            </text>
            <uni-icons type="right" size="18" color="#b38c4f" />
          </view>
        </view>

        <view class="form-divider"></view>

        <view class="form-item content-item">
          <view class="form-label">反馈内容</view>
          <view class="textarea-wrap">
            <textarea
              v-model="content"
              class="form-textarea"
              placeholder="请详细描述您遇到的问题或建议"
              maxlength="500"
              :show-confirm-bar="false"
            />
            <text class="char-count">{{ content.length }}/500</text>
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
          {{ isSubmitting ? "提交中..." : "提交反馈" }}
        </button>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
$gold: #b38c4f;
$gold-light: rgba(197, 166, 118, 0.1);
$bg-color: #fdfaf2;

.feedback-page {
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

.page-header {
  background: #fff;
  padding: 60rpx 30rpx 30rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 10;
  border-bottom: 2rpx solid $gold;
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
  padding: 20rpx 28rpx;
  margin: 20rpx 0rpx;
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

.form-value-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 2rpx solid $gold;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  background: #fff;
}

.form-value {
  font-size: 30rpx;
  color: #333;
}

.form-value.placeholder {
  color: #999;
}

.form-divider {
  height: 1rpx;
  background: $gold-light;
}

.content-item {
  padding-bottom: 40rpx;
}

.textarea-wrap {
  position: relative;
  border: 2rpx solid $gold;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  background: #fff;
  min-height: 300rpx;
}

.form-textarea {
  width: 100%;
  min-height: 280rpx;
  font-size: 30rpx;
  color: #333;
  line-height: 1.6;
  background: transparent;
}

.char-count {
  position: absolute;
  bottom: 16rpx;
  right: 20rpx;
  font-size: 24rpx;
  color: #999;
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
