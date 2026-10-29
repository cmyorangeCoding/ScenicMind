// 路由表
export const PAGES = {
  home: "/pages/index/index",
  aiQuestion: "/pages/aiQuestion/aiQuestion",
  route: "/pages/routeRecommendation/routeRecommendation",
  companion: "/pages/guideCompanion/guideCompanion",
  feedback: "/pages/feedback/feedback",
  profile: "/pages/profile/profile",
};

export function goPage(name) {
  const url = PAGES[name];
  if (!url) return;

  uni.navigateTo({
    url: url,
    fail: () => {
      uni.showToast({
        title: "页面开发中...",
        icon: "none",
      });
    },
  });
}
