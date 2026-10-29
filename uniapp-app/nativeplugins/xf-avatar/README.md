# xf-avatar UniApp 原生插件

封装讯飞「AI 虚拟人」Android SDK，供 UniApp 游客端调用数字人能力。

## 缺少的第三方 SDK

仓库**不包含**讯飞官方 Android SDK（体积大且受第三方授权约束），运行前需要自行下载并放入指定目录：

```
nativeplugins/xf-avatar/android/libs/
├── AIKit_Android_SDK_2.2.17_rc6.aar
└── avatar-core-v3.2.6.aar
```

获取方式：

1. 登录 [讯飞开放平台](https://console.xfyun.cn/)；
2. 开通「AI 虚拟人」服务，进入控制台下载 Android SDK；
3. 将上述两个 `.aar` 复制到 `android/libs/` 目录。

> 未放置 `.aar` 时，UniApp 可正常编译运行，但数字人页面不可用；其余功能不受影响。

## 使用前提

- 在 `uniapp-app/config/xfyun.js` 中填写自己的 `appId` / `appKey` / `apiSecret`；
- 在 HBuilderX 中制作**自定义基座**或云打包，原生插件才能生效；
- `plugin.json` 已声明插件 ID `xf-avatar`，与 `android/src/main/java/com/xfyun/avatar/XfAvatarModule.java` 一一对应。
