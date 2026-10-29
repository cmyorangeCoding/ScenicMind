package com.cmyhyq.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UserAuthInterceptor userauthInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;

    // 构造注入
    public WebConfig(UserAuthInterceptor userauthInterceptor, AdminAuthInterceptor adminAuthInterceptor) {
        this.userauthInterceptor = userauthInterceptor;
        this.adminAuthInterceptor = adminAuthInterceptor;
    }


//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedOriginPatterns("*")
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
//                .allowedHeaders("*")
//                .exposedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // ====================== 【用户端拦截器】 ======================
        // 只拦截 /user/** 下的接口，不影响管理端！
        registry.addInterceptor(userauthInterceptor)
                .addPathPatterns("/user/**")          // 只拦截用户接口
                .excludePathPatterns(
                        "/user/auth/login",            // 用户登录
                        "/user/auth/sms/send"          // 短信发送
                );

        // ====================== 【管理端拦截器】 ======================
        // 只拦截 /admin/** 下的接口
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**")          // 只拦截管理接口
                .excludePathPatterns(
                        "/admin/account/login",        // 管理员登录
                        "/admin/account/logout"        // 管理员登出
                );
    }
}