<template>
  <view class="page">
    <view v-if="routes.length === 0" class="empty">
      <text>暂无收藏路线</text>
    </view>
    <view
      v-for="(route, index) in routes"
      :key="route.id || index"
      class="card"
    >
      <view class="card-content">
        <view class="header">
          <text class="title">{{ route.routeName }}</text>
          <text class="duration">{{ route.totalDuration }}</text>
        </view>
        <view class="attractions-box">
          <text class="attractions-label">途经景点</text>
          <text class="attractions">{{
            route.attractions?.join("、") || ""
          }}</text>
        </view>
      </view>
      <view class="btn-group">
        <button class="btn btn-guide" @click="startGuide(route)">去导览</button>
        <button class="btn btn-uncollect" @click="uncollectRoute(route.id)">
          取消收藏
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { onShow } from "@dcloudio/uni-app";
import request from "@/request.js";
import { getUserId } from "@/auth.js";
import storage from "@/utils/storage.js";
import { setTargetRoute } from "@/utils/appState.js";

const routes = ref([]);

const loadRoutes = () => {
  const userId = getUserId();
  console.log("========== 加载收藏列表 ==========");
  console.log("userId:", userId);
  console.log("请求地址:", `/user/routes?userId=${userId}&page=1&size=10`);

  if (!userId) {
    uni.showToast({ title: "请先登录", icon: "none" });
    return;
  }

  request({
    url: `/user/routes?userId=${userId}&page=1&size=10`,
    method: "GET",
  })
    .then((res) => {
      console.log("后端返回:", JSON.stringify(res));
      if (res.code === 200 && res.data) {
        routes.value = res.data.list || [];
        console.log("解析到路线数量:", routes.value.length);
        console.log("路线数据:", JSON.stringify(routes.value));
      } else {
        console.log("接口返回失败:", res.message);
        uni.showToast({ title: res.message || "加载失败", icon: "none" });
      }
    })
    .catch((err) => {
      console.error("请求异常:", err);
      uni.showToast({ title: "加载失败", icon: "none" });
    });
};

onMounted(() => {
  loadRoutes();
});

onShow(() => {});

const startGuide = (route) => {
  console.log("========== 去导览开始 ==========");
  console.log("路线信息:", JSON.stringify(route));
  console.log("景点列表:", route.attractions);

  if (route.attractions && route.attractions.length > 0) {
    // 设置全局路线状态（内存存储，退出APP后清除）
    setTargetRoute(route.attractions);
    console.log("路线已保存到全局状态:", route.attractions);
  }

  uni.redirectTo({ url: "/pages/guideCompanion/guideCompanion" });
  console.log("========== 去导览结束 ==========");
};

const uncollectRoute = async (routeId) => {
  const userId = getUserId();
  if (!userId || !routeId) {
    uni.showToast({ title: "参数错误", icon: "none" });
    return;
  }

  try {
    const res = await request({
      url: `/user/route/collect/${routeId}?userId=${userId}`,
      method: "DELETE",
    });

    if (res.code === 200) {
      uni.showToast({ title: "取消收藏成功", icon: "success" });
      // 从列表中移除
      routes.value = routes.value.filter((r) => r.id !== routeId);
      // 更新本地存储中所有相关 chatId 的收藏状态
      const collectedChatIds = storage.getCollectedChatIds();
      console.log("📋 取消收藏前本地存储:", JSON.stringify(collectedChatIds));

      let updated = false;
      collectedChatIds.forEach((item) => {
        console.log(
          "🔍 检查项:",
          "chatId=",
          item.chatId,
          "routeId=",
          item.routeId,
          "isCollected=",
          item.isCollected,
        );
        console.log(
          "🔍 比较:",
          "item.routeId=",
          item.routeId,
          "String(routeId)=",
          String(routeId),
          "相等?",
          String(item.routeId) === String(routeId),
        );
        // 修复：两边都转成字符串进行比较，避免类型不匹配
        if (String(item.routeId) === String(routeId)) {
          console.log("✅ 找到匹配项，更新状态");
          item.isCollected = false;
          item.routeId = null; // 清空 routeId
          updated = true;
        }
      });

      if (updated) {
        storage.setCollectedChatIds(collectedChatIds);
        console.log("✅ 已更新本地存储:", JSON.stringify(collectedChatIds));
      } else {
        console.log("⚠️ 未找到匹配的 routeId，可能已经是未收藏状态");
      }
    } else {
      uni.showToast({ title: res.message || "取消收藏失败", icon: "none" });
    }
  } catch (e) {
    uni.showToast({ title: "请求失败", icon: "none" });
  }
};
</script>

<style scoped>
.page {
  padding: 30rpx;
  min-height: 100vh;
  background: linear-gradient(180deg, #f8f9fa 0%, #f0f2f5 100%);
}
.empty {
  text-align: center;
  padding: 150rpx 40rpx;
  color: #999;
}
.empty text {
  font-size: 28rpx;
}
.card {
  background: #fff;
  padding: 36rpx;
  margin-bottom: 24rpx;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);
  border: 1rpx solid #f0f0f0;
}
.card-content {
  padding-bottom: 24rpx;
  border-bottom: 1rpx dashed #f0f0f0;
  margin-bottom: 24rpx;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}
.title {
  font-size: 34rpx;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 1rpx;
}
.duration {
  font-size: 24rpx;
  color: #4a90e2;
  background: rgba(74, 144, 226, 0.1);
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
}
.attractions-box {
  padding: 16rpx;
  background: #f8f9fa;
  border-radius: 12rpx;
}
.attractions-label {
  font-size: 22rpx;
  color: #999;
  margin-bottom: 8rpx;
  display: block;
}
.attractions {
  font-size: 26rpx;
  color: #555;
  line-height: 1.6;
  word-break: break-all;
}
.btn-group {
  display: flex;
  gap: 20rpx;
  justify-content: flex-end;
}
.btn {
  font-size: 26rpx;
  padding: 18rpx 40rpx;
  border-radius: 36rpx;
  border: none;
  font-weight: 500;
}
.btn-guide {
  background: linear-gradient(135deg, #4a90e2 0%, #357abd 100%);
  color: #fff;
  box-shadow: 0 4rpx 12rpx rgba(74, 144, 226, 0.3);
}
.btn-guide:active {
  opacity: 0.85;
}
.btn-uncollect {
  background: #f5f5f5;
  color: #888;
  border: 1rpx solid #e8e8e8;
}
.btn-uncollect:active {
  background: #eee;
}
</style>
