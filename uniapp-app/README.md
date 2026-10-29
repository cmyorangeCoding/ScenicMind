# uniapp-app · 游客端

UniApp（Vue 3）跨端应用，包含首页、AI 问答、数字人、智能导览、路线推荐、游记、个人中心等 16 个页面。

## 需要自行配置的内容

| 文件 | 字段 | 用途 |
|---|---|---|
| `config.js` | `BASE_URL` | 后端地址（真机调试填局域网 IP） |
| `config.js` | `AMAP_WEB_KEY` | 高德「Web 服务」Key，步行路径规划 |
| `config/xfyun.js` | `appId` / `appKey` / `apiSecret` | 讯飞数字人 |
| `utils/aliyunAsr.js` | `appKey` | 阿里云智能语音交互 |
| `manifest.json` | 高德 Android / iOS Key | 地图 SDK |

> 仓库内以上字段均已置空，请填写自己的 Key 后运行。

## 运行

1. 用 HBuilderX 导入本目录；
2. 按上表填写配置；
3. 数字人功能需要讯飞 Android SDK（见 `nativeplugins/xf-avatar/README.md`）；
4. 制作自定义基座或云打包运行。

## 主要目录

```
pages/            页面（首页 / AI 问答 / 数字人 / 导览 / 游记 / 个人中心）
utils/            WebSocket、语音识别、存储等工具
nativeplugins/    讯飞数字人原生插件（不含第三方 .aar）
static/           图片、手绘地图、RTCPlayer SDK
request.js        请求封装（携带游客端 JWT）
```
