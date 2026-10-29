package com.xfyun.avatar;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.common.UniModule;
import android.content.Context;
import android.util.Log;
import android.widget.FrameLayout;
import com.iflytek.avatar.AvatarPlatform;
import com.iflytek.avatar.AvatarPlayController;
import com.iflytek.avatar.params.AvatarParams;
import com.iflytek.avatar.params.TTSParams;
import com.iflytek.avatar.player.StreamPlayer;
import com.iflytek.avatar.player.StreamPlayerFactory;

public class XfAvatarModule extends UniModule {
    
    private static final String TAG = "XfAvatar";
    private Context mContext;
    private AvatarPlayController mController;
    private StreamPlayer mPlayer;
    
    @UniJSMethod(uiThread = false)
    public void initSDK(String appId, String apiKey, String apiSecret) {
        mContext = getUniAppContext();
        try {
            AvatarPlatform.AvatarPlatformConfig config = new AvatarPlatform.AvatarPlatformConfig();
            config.appId = appId;
            config.apiKey = apiKey;
            config.apiSecret = apiSecret;
            config.serverUrl = "wss://avatar.cn-huadong-1.xf-yun.com/v1/interact_lips_flow";
            AvatarPlatform.initialize(mContext, config);
            Log.d(TAG, "SDK初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "SDK初始化失败: " + e.getMessage());
        }
    }
    
    @UniJSMethod(uiThread = true)
    public void startAvatar(String avatarId) {
        try {
            mController = AvatarPlatform.getAvatarPlayController();
            
            AvatarParams avatarParams = new AvatarParams();
            avatarParams.avatarId = avatarId;
            avatarParams.width = 720;
            avatarParams.height = 1280;
            
            TTSParams ttsParams = new TTSParams();
            ttsParams.vcn = "";
            ttsParams.speed = 50;
            ttsParams.pitch = 50;
            ttsParams.volume = 50;
            
            mController.setGlobalParams(avatarParams, ttsParams, null, null, null);
            
            mPlayer = StreamPlayerFactory.createStreamPlayer(mContext, StreamPlayerFactory.PlayerType.LOCAL);
            FrameLayout container = new FrameLayout(mContext);
            mPlayer.setRenderView(container);
            mController.setPlayer(mPlayer);
            
            mController.start();
            Log.d(TAG, "数字人启动成功: " + avatarId);
        } catch (Exception e) {
            Log.e(TAG, "数字人启动失败: " + e.getMessage());
        }
    }
    
    @UniJSMethod(uiThread = false)
    public void writeText(String text) {
        try {
            if (mController != null && text != null && !text.isEmpty()) {
                mController.writeText(text);
                Log.d(TAG, "发送文本成功: " + text);
            }
        } catch (Exception e) {
            Log.e(TAG, "发送文本失败: " + e.getMessage());
        }
    }
    
    @UniJSMethod(uiThread = false)
    public void stopAvatar() {
        try {
            if (mController != null) {
                mController.stop();
                Log.d(TAG, "数字人停止成功");
            }
        } catch (Exception e) {
            Log.e(TAG, "数字人停止失败: " + e.getMessage());
        }
    }
    
    @UniJSMethod(uiThread = false)
    public void release() {
        try {
            if (mPlayer != null) {
                mPlayer.release();
                mPlayer = null;
            }
            if (mController != null) {
                mController.destroy();
                mController = null;
            }
            Log.d(TAG, "资源释放成功");
        } catch (Exception e) {
            Log.e(TAG, "资源释放失败: " + e.getMessage());
        }
    }
}