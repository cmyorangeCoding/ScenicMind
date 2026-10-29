/**
 * 权限管理工具
 */
export default {
    /**
     * 查询权限状态
     * @param {String} scope - 权限类型
     */
    async check(scope) {
        return new Promise((resolve) => {
            // #ifdef MP-WEIXIN || MP-ALIPAY || MP-BAIDU || MP-TOUTIAO
            if (typeof uni.getSetting === 'function') {
                uni.getSetting({
                    success: (res) => {
                        resolve(res.authSetting[scope] === true);
                    },
                    fail: () => resolve(false)
                });
            } else {
                // 在不支持 getSetting 的平台上，默认返回 true
                resolve(true);
            }
            // #endif

            // #ifndef MP-WEIXIN || MP-ALIPAY || MP-BAIDU || MP-TOUTIAO
            // H5 和 App 端不支持 getSetting，直接返回 true
            resolve(true);
            // #endif
        });
    },

    /**
     * 申请权限
     * @param {String} scope - 权限类型
     * @param {Object} options - 配置项
     */
    async request(scope, options = {}) {
        const {
            title = '权限申请',
            content = '需要授权使用此功能',
            showSetting = true
        } = options;

        return new Promise((resolve) => {
            // #ifdef MP-WEIXIN || MP-ALIPAY || MP-BAIDU || MP-TOUTIAO
            if (typeof uni.getSetting !== 'function') {
                // 在不支持 getSetting 的平台上，直接返回 true
                resolve(true);
                return;
            }

            // 1. 检查权限状态
            uni.getSetting({
                success: (res) => {
                    // 已授权
                    if (res.authSetting[scope] === true) {
                        resolve(true);
                        return;
                    }

                    // 已被拒绝
                    if (res.authSetting[scope] === false) {
                        if (showSetting) {
                            uni.showModal({
                                title: '权限已拒绝',
                                content: content + '\n请在设置中开启权限',
                                confirmText: '去设置',
                                cancelText: '取消',
                                success: (modalRes) => {
                                    if (modalRes.confirm) {
                                        if (typeof uni.openSetting === 'function') {
                                            uni.openSetting({
                                                success: (settingRes) => {
                                                    resolve(settingRes.authSetting[scope] === true);
                                                },
                                                fail: () => resolve(false)
                                            });
                                        } else {
                                            resolve(false);
                                        }
                                    } else {
                                        resolve(false);
                                    }
                                }
                            });
                        } else {
                            resolve(false);
                        }
                        return;
                    }

                    // 未申请过，发起授权
                    if (typeof uni.authorize === 'function') {
                        uni.authorize({
                            scope: scope,
                            success: () => resolve(true),
                            fail: () => {
                                // 授权失败，引导去设置
                                if (showSetting) {
                                    uni.showModal({
                                        title: title,
                                        content: content + '\n请开启权限后重试',
                                        confirmText: '去设置',
                                        success: (modalRes) => {
                                            if (modalRes.confirm) {
                                                if (typeof uni.openSetting === 'function') {
                                                    uni.openSetting({
                                                        success: (settingRes) => {
                                                            resolve(settingRes.authSetting[scope] === true);
                                                        },
                                                        fail: () => resolve(false)
                                                    });
                                                } else {
                                                    resolve(false);
                                                }
                                            } else {
                                                resolve(false);
                                            }
                                        }
                                    });
                                } else {
                                    resolve(false);
                                }
                            }
                        });
                    } else {
                        // 在不支持 authorize 的平台上，直接返回 true
                        resolve(true);
                    }
                },
                fail: () => resolve(false)
            });
            // #endif

            // #ifndef MP-WEIXIN || MP-ALIPAY || MP-BAIDU || MP-TOUTIAO
            // H5 和 App 端不支持这些权限 API，直接返回 true
            resolve(true);
            // #endif
        });
    },

    /**
     * 打开设置页
     */
    openSetting() {
        // #ifdef MP-WEIXIN || MP-ALIPAY || MP-BAIDU || MP-TOUTIAO
        if (typeof uni.openSetting === 'function') {
            uni.openSetting({
                success: (res) => {
                    console.log('设置页返回:', res.authSetting);
                }
            });
        } else {
            console.log('当前平台不支持打开设置页');
        }
        // #endif

        // #ifndef MP-WEIXIN || MP-ALIPAY || MP-BAIDU || MP-TOUTIAO
        console.log('当前平台不支持打开设置页');
        // #endif
    }
};
