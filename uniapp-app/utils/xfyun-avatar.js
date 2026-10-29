import { XFYUN_CONFIG } from '@/config/xfyun';

let xfAvatar = null;

export function initAvatar() {
  if (typeof uni !== 'undefined') {
    xfAvatar = uni.requireNativePlugin('com.xfyun.avatar');
    if (xfAvatar) {
      const config = XFYUN_CONFIG.virtualHuman;
      xfAvatar.initSDK(config.appId, config.appKey, config.apiSecret || '');
      console.log('讯飞数字人插件初始化成功');
    }
  }
}

export function speak(text) {
  if (xfAvatar && text) {
    xfAvatar.writeText(text);
  }
}

export function startAvatar(avatarId) {
  if (xfAvatar && avatarId) {
    xfAvatar.startAvatar(avatarId);
  }
}

export function stopAvatar() {
  if (xfAvatar) {
    xfAvatar.stopAvatar();
  }
}

export function releaseAvatar() {
  if (xfAvatar) {
    xfAvatar.release();
    xfAvatar = null;
  }
}

export default {
  initAvatar,
  speak,
  startAvatar,
  stopAvatar,
  releaseAvatar
};