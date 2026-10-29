<script>
import { BASE_URL } from "./config.js";

export default {
  onLaunch: function () {
    console.log("App Launch");

    // 预加载数字人（在APP启动时就开始连接）
    this.preloadDigitalHuman();

    // 监听返回键（安卓）- 注释掉，使用系统默认返回行为
    // this.registerBackButtonListener();
  },

  onShow: function () {
    console.log("App Show");
  },

  onHide: function () {
    console.log("App Hide");
  },

  methods: {
    // 预加载数字人（在APP启动时就开始连接）
    preloadDigitalHuman() {
      // #ifdef APP-PLUS
      try {
        const htmlPath = "_www/static/xfyun-digital-human/web-api.html";

        // 创建一个隐藏的webview来预加载数字人
        const webview = plus.webview.create(htmlPath, "preloadDigitalHuman", {
          width: 1,
          height: 1,
          left: -9999,
          top: -9999,
          opacity: 0,
          show: false,
          scalable: false,
          bounce: "none",
          background: "transparent",
          webkitOverflowScrolling: "touch",
          allowZoom: false,
          showScrollbar: false,
          hardwareAccelerated: true,
        });

        // 等待webview加载完成后，从后端获取配置并设置
        webview.addEventListener("loaded", () => {
          console.log("[数字人] webview加载完成，开始获取配置");
          this.fetchAvatarConfig()
            .then((config) => {
              if (config) {
                console.log("[数字人] 成功获取配置，正在设置:", config);
                webview.evalJS(`window.setConfig(${JSON.stringify(config)})`);
                console.log("[数字人] 配置已设置");
              } else {
                console.warn("[数字人] 获取配置为空，使用默认值");
              }
            })
            .catch((err) => {
              console.error("[数字人] 获取配置失败:", err);
            });
        });

        console.log("✅ 数字人预加载webview创建成功");
      } catch (e) {
        console.log("❌ 数字人预加载失败:", e);
      }
      // #endif
    },

    // 从后端获取数字人配置（用户配置 + 后端敏感配置合并）
    fetchAvatarConfig() {
      return new Promise((resolve, reject) => {
        // #ifdef APP-PLUS
        const baseUrl = BASE_URL;
        // #endif
        // #ifndef APP-PLUS
        // #endif

        uni.request({
          url: baseUrl + "/api/avatar/config",
          method: "GET",
          success: (res) => {
            if (
              res.statusCode === 200 &&
              res.data &&
              res.data.code === 200 &&
              res.data.data
            ) {
              const backendConfig = res.data.data;
              console.log("[数字人配置] 后端配置:", backendConfig);

              // 检查用户是否有保存的配置，如果有则合并
              const userConfig = uni.getStorageSync("user_avatar_config");
              if (userConfig && userConfig.avatarId) {
                console.log("[数字人配置] 合并用户保存的配置:", userConfig);
                resolve({
                  ...backendConfig, // 后端敏感配置
                  avatarId: userConfig.avatarId,
                  vcn: userConfig.vcn || backendConfig.vcn,
                  speed: userConfig.speed,
                  pitch: userConfig.pitch,
                  volume: userConfig.volume,
                });
              } else {
                console.log("[数字人配置] 使用后端默认配置");
                resolve(backendConfig);
              }
            } else {
              reject(
                new Error(
                  "获取配置失败: " + (res.data ? res.data.message : "未知错误"),
                ),
              );
            }
          },
          fail: (err) => {
            reject(err);
          },
        });
      });
    },

    // 注册返回键监听
    registerBackButtonListener() {
      // #ifdef APP-PLUS
      const systemInfo = uni.getSystemInfoSync();
      if (systemInfo.platform === "android") {
        plus.key.addEventListener("backbutton", () => {
          console.log("=== backbutton 事件触发 ===");
          this.handleBackButton();
        });
      }
      // #endif
    },

    // 处理返回键
    handleBackButton() {
      // 获取当前页面栈
      const pages = getCurrentPages();
      console.log("=== 返回键被按下 ===");
      console.log("页面栈长度:", pages.length);
      console.log(
        "页面栈:",
        pages.map((p) => p.route),
      );
      console.log("当前页面:", pages[pages.length - 1]?.route);

      if (pages.length === 1) {
        // 只有一个页面，说明是首页，退出APP前检查是否在导览
        this.checkGuidingAndExit();
      } else {
        // 有多个页面，正常返回
        console.log("执行 uni.navigateBack()");
        uni.navigateBack({
          success: () => {
            console.log("navigateBack 成功");
          },
          fail: (err) => {
            console.log("navigateBack 失败:", err);
          },
        });
      }
    },

    // 销毁预加载的数字人webview
    destroyPreloadedDigitalHuman() {
      // #ifdef APP-PLUS
      try {
        const preloadedWebview = plus.webview.getWebviewById(
          "preloadDigitalHuman",
        );
        if (preloadedWebview) {
          console.log("✅ 销毁预加载数字人webview");
          preloadedWebview.close("none");
        }
      } catch (e) {
        console.log("销毁预加载数字人webview失败:", e);
      }
      // #endif
    },

    // 退出APP
    exitApp() {
      // 先销毁预加载的数字人webview
      this.destroyPreloadedDigitalHuman();

      // #ifdef APP-PLUS
      plus.runtime.quit();
      // #endif
    },

    // 检查导览状态并退出
    checkGuidingAndExit() {
      const isGuiding = uni.getStorageSync("is_guiding") === true;
      if (isGuiding) {
        uni.showModal({
          title: "提示",
          content: "当前正在导览中，退出APP将结束导览，确定要退出吗？",
          confirmText: "确定退出",
          cancelText: "继续导览",
          success: (res) => {
            if (res.confirm) {
              // 退出APP
              this.exitApp();
            }
          },
        });
      } else {
        // 不在导览，直接退出
        this.exitApp();
      }
    },
  },
};
</script>

<style lang="scss">
/*每个页面公共css */
@import "@/uni.scss";
/* #ifndef APP-NVUE */
@import "@/static/customicons.css";
page {
  background-color: #f5f5f5;
}
/* #endif */
.example-info {
  font-size: 14px;
  color: #333;
  padding: 10px;
}
</style>
