<template>
  <view class="page">
    <view class="page-header">
      <text class="page-title">推荐路线</text>
      <text class="page-subtitle">精选主题路线，带你深度游玩灵山胜境</text>
    </view>
    <view
      v-for="(route, index) in routes"
      :key="index"
      class="card"
      @click="startGuide(route)"
    >
      <view class="card-content">
        <view class="header">
          <text class="title">{{ route.routeName }}</text>
          <text class="duration">{{ route.totalDuration }}</text>
        </view>
        <view class="route-preview">
          <text class="route-label">路线规划</text>
          <text class="route-text">{{ route.routePath }}</text>
        </view>
        <view class="attractions-box">
          <text class="attractions-label">途经景点</text>
          <text class="attractions">{{ route.attractions }}</text>
        </view>
      </view>
      <view class="btn-group">
        <button class="btn btn-guide">开始导览</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { setTargetRoute } from "@/utils/appState.js";

const routes = [
  {
    routeName: "历史文化爱好者路线",
    totalDuration: "6小时深度游",
    routePath: "灵山大门→灵山大照壁→祥符禅寺→灵山大佛→灵山梵宫→五印坛城→出口",
    attractions:
      "灵山大门、灵山大照壁、祥符禅寺、灵山大佛、灵山梵宫、五印坛城、出口",
  },
  {
    routeName: "自然风光爱好者路线",
    totalDuration: "5小时全景游",
    routePath: "灵山大门→佛足坛→九龙灌浴→菩提大道→灵山大佛→曼飞龙塔→出口",
    attractions:
      "灵山大门、佛足坛、九龙灌浴、菩提大道、灵山大佛、曼飞龙塔、出口",
  },
  {
    routeName: "亲子家庭路线",
    totalDuration: "4小时轻松游",
    routePath: "灵山大门→九龙灌浴→百子戏弥勒→灵山梵宫→五印坛城→出口",
    attractions: "灵山大门、九龙灌浴、百子戏弥勒、灵山梵宫、五印坛城、出口",
  },
];

const startGuide = (route) => {
  console.log("========== 开始推荐路线导览 ==========");
  console.log("路线信息:", JSON.stringify(route));

  if (route.attractions) {
    const attractionList = route.attractions.split("、");
    // 设置全局路线状态（内存存储，退出APP后清除）
    setTargetRoute(attractionList);
    console.log("路线已保存到全局状态:", attractionList);
  }

  uni.redirectTo({ url: "/pages/guideCompanion/guideCompanion" });
};
</script>

<style scoped>
.page {
  padding: 30rpx;
  min-height: 100vh;
  background: linear-gradient(180deg, #f8f9fa 0%, #f0f2f5 100%);
}

.page-header {
  margin-bottom: 40rpx;
}

.page-title {
  display: block;
  font-size: 44rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 12rpx;
}

.page-subtitle {
  display: block;
  font-size: 26rpx;
  color: #999;
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
  font-weight: bold;
  color: #333;
  flex: 1;
}

.duration {
  font-size: 24rpx;
  color: #666;
  background: #f5f5f5;
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
}

.route-preview {
  margin-bottom: 16rpx;
}

.route-label {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-bottom: 8rpx;
}

.route-text {
  display: block;
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
}

.attractions-box {
  margin-top: 12rpx;
}

.attractions-label {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-bottom: 8rpx;
}

.attractions {
  display: block;
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
}

.btn-group {
  display: flex;
  justify-content: flex-end;
}

.btn {
  font-size: 26rpx;
  padding: 16rpx 40rpx;
  border-radius: 40rpx;
  border: none;
}

.btn-guide {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}
</style>
