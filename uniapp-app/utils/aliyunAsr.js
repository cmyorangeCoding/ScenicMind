// utils/aliyunAsr.js - 阿里云语音识别工具（WebSocket 版）

import request from '@/request.js';

// 阿里云智能语音交互 AppKey：请在 https://nls-portal.console.aliyun.com/ 申请后本地填写，
// 或改为从后端接口下发，切勿把真实 AppKey 提交到仓库。
const ALIYUN_CONFIG = {
    appKey: 'YOUR_NLS_APPKEY',
    region: 'cn-shanghai',
    wsUrl: 'wss://nls-gateway-cn-shanghai.aliyuncs.com/ws/v1'
};

let cachedToken = null;
let tokenExpireTime = 0;
let isProcessing = false;

export const getAliyunToken = async () => {
    try {
        if (cachedToken && Date.now() < tokenExpireTime - 3600000) {
            console.log('使用缓存的 Token');
            return cachedToken;
        }

        console.log('从后端获取新的 Token');
        const res = await request.get('/user/ai/getAliyunToken');

        if (res.code === 200 && res.data) {
            cachedToken = res.data.token;
            tokenExpireTime = res.data.expireTime || (Date.now() + 24 * 60 * 60 * 1000);
            console.log('Token 获取成功');
            return cachedToken;
        } else {
            throw new Error(res.message || '获取 Token 失败');
        }
    } catch (e) {
        console.error('获取阿里云 Token 失败', e);
        throw e;
    }
};

// ... existing code ...

export const speechToText = async (audioFilePath) => {
    if (isProcessing) {
        console.warn('⚠️ 正在处理上一次识别，请等待');
        throw new Error('请勿重复触发');
    }

    let wsTask = null;

    try {
        isProcessing = true;
        console.log('========== 开始语音识别（WebSocket） ==========');
        console.log('音频文件路径:', audioFilePath);

        // 检测文件格式
        const ext = audioFilePath.split('.').pop().toLowerCase();
        console.log('音频格式:', ext.toUpperCase());

        const token = await getAliyunToken();
        console.log('Token 获取完成');

        console.log('📖 开始读取音频文件...');
        const audioBase64 = await readFileAsBase64(audioFilePath);
        console.log('✅ 音频文件读取完成');
        console.log('Base64 长度:', audioBase64.length);
        console.log('Base64 解码后大小:', (audioBase64.length * 0.75).toFixed(0), 'bytes');

        const audioData = base64ToArrayBuffer(audioBase64);
        console.log('✅ ArrayBuffer 转换完成');
        console.log('音频数据大小:', audioData.byteLength, 'bytes');

        const result = await new Promise((resolve, reject) => {
            const taskId = generateMessageId();

            console.log('taskId:', taskId);

            const wsUrl = `${ALIYUN_CONFIG.wsUrl}?token=${token}`;
            console.log('连接到:', ALIYUN_CONFIG.wsUrl);

            wsTask = uni.connectSocket({
                url: wsUrl,
                success: () => console.log('WebSocket 连接中...'),
                fail: (err) => reject(new Error('WebSocket 连接失败: ' + err.errMsg))
            });

            wsTask.onOpen(() => {
                if (!isProcessing) {
                    console.warn('⚠️ 处理已取消，关闭连接');
                    wsTask.close();
                    return;
                }

                console.log('WebSocket 已打开（鉴权成功）');

                const startPayload = {
                    header: {
                        message_id: generateMessageId(),
                        task_id: taskId,
                        namespace: 'SpeechRecognizer',
                        name: 'StartRecognition',
                        appkey: ALIYUN_CONFIG.appKey
                    },
                    payload: {
                        format: ext,              // 动态使用检测到的格式
                        sample_rate: 8000,        // 与录音参数一致
                        enable_intermediate_result: false,
                        enable_punctuation_prediction: true,
                        enable_inverse_text_normalization: true,
                        enable_voice_detection: false
                    }
                };

                console.log('发送 StartRecognition，格式:', startPayload.payload.format, '采样率:', startPayload.payload.sample_rate);
                wsTask.send({ data: JSON.stringify(startPayload) });
            });

            let isStarted = false;
            let finalResult = '';

            wsTask.onMessage((res) => {
                if (!isProcessing) {
                    console.warn('⚠️ 处理已取消，忽略消息');
                    return;
                }

                try {
                    const message = JSON.parse(res.data);
                    console.log('收到消息:', message.header?.name);

                    if (message.header.name === 'RecognitionStarted' && !isStarted) {
                        console.log('服务端已确认，开始发送音频数据');
                        console.log('发送音频数据:', audioData.byteLength, 'bytes');
                        isStarted = true;

                        wsTask.send({
                            data: audioData,
                            success: () => {
                                console.log('✅ 音频数据发送成功');

                                setTimeout(() => {
                                    if (!isProcessing) return;

                                    const stopPayload = {
                                        header: {
                                            message_id: generateMessageId(),
                                            task_id: taskId,
                                            namespace: 'SpeechRecognizer',
                                            name: 'StopRecognition',
                                            appkey: ALIYUN_CONFIG.appKey
                                        }
                                    };
                                    console.log('发送 StopRecognition');
                                    wsTask.send({ data: JSON.stringify(stopPayload) });
                                }, 100);
                            },
                            fail: (err) => reject(new Error('音频发送失败: ' + err.errMsg))
                        });
                    }

                    else if (message.header.name === 'RecognitionResultChanged') {
                        console.log('中间结果:', message.payload?.result);
                    }

                    else if (message.header.name === 'RecognitionCompleted') {
                        console.log('识别完成');
                        finalResult = message.payload?.result || '';
                        console.log('最终结果:', finalResult || '(空)');

                        isProcessing = false;
                        wsTask.close();
                        resolve(finalResult);
                    }

                    else if (message.header.name === 'TaskFailed') {
                        console.error('识别失败:', message);
                        const errorMsg = message.header?.status_text || message.payload?.status_text || '识别失败';

                        isProcessing = false;
                        wsTask.close();
                        reject(new Error(errorMsg));
                    }

                } catch (e) {
                    console.error('解析消息失败:', e);
                }
            });

            wsTask.onError((err) => {
                console.error('WebSocket 错误:', err);
                isProcessing = false;
                reject(new Error('WebSocket 错误'));
            });

            wsTask.onClose(() => {
                console.log('WebSocket 已关闭');
                if (isProcessing) isProcessing = false;
            });

            setTimeout(() => {
                if (wsTask && isProcessing) {
                    console.error('识别超时');
                    isProcessing = false;
                    wsTask.close();
                    reject(new Error('识别超时'));
                }
            }, 15000);
        });

        console.log('✅ 语音识别成功:', result);
        console.log('=================================');
        return result;

    } catch (e) {
        console.error('❌ 语音识别失败', e);
        console.error('=================================');
        // 确保出错时也关闭 WebSocket
        if (wsTask) {
            wsTask.close();
            wsTask = null;
        }
        throw e;
    }
    // 注意：WebSocket 会在 RecognitionCompleted、TaskFailed 或超时时自动关闭
    // 不要在 finally 中关闭，否则会导致识别结果还没收到就关闭了
};

// ... existing code ...


const readFileAsBase64 = (filePath) => {
    return new Promise((resolve, reject) => {
        plus.io.resolveLocalFileSystemURL(filePath, (entry) => {
            entry.file((file) => {
                console.log('文件大小:', file.size, 'bytes');  // 新增：显示原始文件大小
                const fileReader = new plus.io.FileReader();

                fileReader.onloadend = (e) => {
                    try {
                        const dataUrl = e.target.result;
                        const base64Index = dataUrl.indexOf('base64,') + 7;
                        const base64 = dataUrl.substring(base64Index);
                        resolve(base64);
                    } catch (err) {
                        reject(new Error('Base64 提取失败: ' + err.message));
                    }
                };

                fileReader.onerror = () => reject(new Error('FileReader 读取失败'));
                fileReader.readAsDataURL(file);
            }, (err) => reject(new Error('读取文件失败: ' + err.message)));
        }, (err) => reject(new Error('解析文件路径失败: ' + err.message)));
    });
};

const base64ToArrayBuffer = (base64) => {
    const binaryString = atob(base64);
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
        bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes.buffer;
};

const generateMessageId = () => {
    const hexDigits = '0123456789abcdef';
    const s = [];
    for (let i = 0; i < 32; i++) {
        s[i] = hexDigits.substr(Math.floor(Math.random() * 16), 1);
    }
    s[14] = '4';
    s[19] = hexDigits.substr((parseInt(s[19], 16) & 0x3) | 0x8, 1);
    return s.join('');
};

export const clearTokenCache = () => {
    cachedToken = null;
    tokenExpireTime = 0;
};
