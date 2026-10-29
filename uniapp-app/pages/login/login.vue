<template>
  <view class="login-container">
    <view class="login-content">
      <text class="main-title">登录注册</text>
      <text class="sub-title">未注册的手机号验证后即完成注册</text>

      <view class="form-wrapper">
        <view class="input-row">
          <input
            class="input-item"
            type="number"
            v-model="phone"
            placeholder="请输入手机号码"
            maxlength="11"
          />
        </view>

        <view class="input-row code-row">
          <input
            class="input-item"
            type="text"
            v-model="code"
            placeholder="请输入验证码"
            maxlength="6"
          />
          <view class="inner-code-btn" @tap="getSmsCode">
            <text :class="isCounting ? 'counting' : ''">{{
              isCounting ? timer + "s" : "获取验证码"
            }}</text>
          </view>
        </view>

        <text class="help-link" @tap="toggleHelp">没收到验证码？</text>
      </view>

      <view class="terms-wrapper">
        <checkbox-group @change="handleCheckboxChange">
          <label class="terms-label">
            <checkbox
              value="agree"
              :checked="isAgreed"
              color="#b8976d"
              style="transform: scale(0.7)"
            />
            <text class="terms-text">我已阅读并同意</text>
          </label>
        </checkbox-group>
        <text class="link">《景区服务条款》</text>
      </view>

      <button
        class="submit-btn"
        :style="{ opacity: isAgreed ? 1 : 0.6 }"
        @tap="handleLoginSubmit"
      >
        登录注册
      </button>

      <view class="third-party">
        <view class="divider"><text>其他登录方式</text></view>
        <view class="wechat-btn" @tap="handleWechatLogin">
          <image
            src="/static/wechat.jpg"
            mode="aspectFit"
            class="wechat-img"
          ></image>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";
import request from "../../request.js";
import { setAuth } from "../../auth.js";

const phone = ref("");
const code = ref("");
const isAgreed = ref(false);
const isCounting = ref(false);
const timer = ref(60);
let smsTimer = null;
const keyboardHeight = ref(0);

const phoneRegex = /^1[3-9]\d{9}$/;

const handleKeyboardHeightChange = (e) => {
  if (e && e.detail && typeof e.detail.height === "number") {
    keyboardHeight.value = e.detail.height;
  }
};

const handleCheckboxChange = (e) => {
  isAgreed.value = e.detail.value.length > 0;
};

const toggleHelp = () => {
  uni.showModal({
    title: "未收到验证码？",
    content: "1.检查网络连接\n2.确认手机号无误\n3.检查短信拦截\n4.咨询景区客服",
    showCancel: false,
    confirmColor: "#b8976d",
  });
};

const startCountdown = () => {
  isCounting.value = true;
  timer.value = 60;
  smsTimer = setInterval(() => {
    timer.value -= 1;
    if (timer.value <= 0) {
      clearInterval(smsTimer);
      smsTimer = null;
      isCounting.value = false;
      timer.value = 60;
    }
  }, 1000);
};

const getSmsCode = async () => {
  if (isCounting.value) return;
  if (!phoneRegex.test(phone.value)) {
    uni.showToast({ title: "请输入正确手机号", icon: "none" });
    return;
  }

  // 直接发送验证码
  await sendSmsCode();
};

// 发送验证码
const sendSmsCode = async () => {
  try {
    uni.showLoading({ title: "发送中..." });
    await request.post(
      "/user/auth/sms/send",
      {
        phone: phone.value,
      },
      {
        "Content-Type": "application/x-www-form-urlencoded",
      },
    );
    uni.hideLoading();
    uni.showToast({ title: "验证码已发送", icon: "success" });
    startCountdown();
  } catch (error) {
    uni.hideLoading();
    uni.showToast({
      title: error?.message || "验证码发送失败，请稍后重试",
      icon: "none",
    });
  }
};

// ===================== 【最新登录逻辑】 =====================
const handleLoginSubmit = async () => {
  if (!isAgreed.value) {
    uni.showToast({ title: "请先勾选服务条款", icon: "none" });
    return;
  }
  if (!phoneRegex.test(phone.value)) {
    uni.showToast({ title: "请输入正确手机号", icon: "none" });
    return;
  }
  if (!code.value || code.value.length !== 6) {
    uni.showToast({ title: "请输入 6 位短信验证码", icon: "none" });
    return;
  }

  try {
    uni.showLoading({ title: "登录中..." });

    const response = await request.post("/user/auth/login", {
      phone: phone.value,
      smsCode: code.value,
    });

    uni.hideLoading();

    if (response && response.data && response.data.token) {
      setAuth({
        token: response.data.token,
        userId: response.data.userId,
        expiresAt: response.data.expireAt,
        nickname: response.data.nickname,
        avatarUrl: response.data.avatarUrl,
        phone: phone.value,
      });

      uni.showToast({ title: "登录成功", icon: "success" });
      uni.redirectTo({ url: "/pages/index/index" });
    } else {
      uni.showToast({
        title: "登录失败：" + (response?.msg || "返回数据异常"),
        icon: "none",
      });
    }
  } catch (error) {
    uni.hideLoading();
    uni.showToast({
      title: error?.message || "登录失败，请稍后重试",
      icon: "none",
    });
  }
};

const handleWechatLogin = () => {
  uni.showToast({ title: "暂不支持微信登录", icon: "none" });
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

onBeforeUnmount(() => {
  if (smsTimer) {
    clearInterval(smsTimer);
    smsTimer = null;
  }
  uni.offKeyboardHeightChange(handleKeyboardHeightChange);
});
</script>

<style scoped>
.login-container {
  margin-top: 200rpx;
  background-color: #f7f7f5;
  min-height: 100vh;
  padding: 60rpx 40rpx;
  overflow: hidden;
  touch-action: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}
.main-title {
  font-size: 52rpx;
  font-weight: bold;
  display: block;
  margin-bottom: 20rpx;
}
.sub-title {
  font-size: 28rpx;
  color: #888;
  display: block;
  margin-bottom: 60rpx;
}

.input-row {
  margin-top: 60rpx;
  background-color: #fff;
  border-radius: 16rpx;
  height: 100rpx;
  margin-bottom: 30rpx;
  padding: 0 30rpx;
  display: flex;
  align-items: center;
  border: 1rpx solid #eee;
}
.input-item {
  flex: 1;
  font-size: 30rpx;
}

.inner-code-btn {
  border-left: 1rpx solid #eee;
  padding-left: 20rpx;
}
.inner-code-btn text {
  font-size: 26rpx;
  color: #b8976d;
}
.counting {
  color: #999 !important;
}

.help-link {
  font-size: 24rpx;
  color: #999;
  text-align: right;
  display: block;
}

.terms-wrapper {
  margin: 40rpx 0;
  display: flex;
  align-items: center;
}
.terms-label {
  display: flex;
  align-items: center;
}
.terms-text {
  font-size: 26rpx;
  color: #666;
}
.link {
  font-size: 26rpx;
  color: #b8976d;
  font-weight: bold;
}

.submit-btn {
  background-color: #2c3e50;
  color: #fff;
  border-radius: 50rpx;
  height: 100rpx;
  line-height: 100rpx;
  font-size: 32rpx;
  margin-top: 40rpx;
}

.third-party {
  margin-top: 100rpx;
  text-align: center;
}
.divider {
  border-top: 1rpx solid #ddd;
  position: relative;
}
.divider text {
  position: absolute;
  top: -20rpx;
  left: 50%;
  transform: translateX(-50%);
  background: #f7f7f5;
  padding: 0 20rpx;
  color: #999;
  font-size: 24rpx;
}
.wechat-btn {
  margin-top: 60rpx;
}
.wechat-img {
  width: 80rpx;
  height: 80rpx;
}
</style>
