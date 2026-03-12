package com.saul.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：
 * - 注册 JWT 拦截器（放行 /api/login）
 * - 配置全局 CORS（支持 Vue 跨端口调用）
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 只拦截业务接口（按你的约定，API 统一走 /api）
               .addPathPatterns("/api/**")
                //先模拟不开启
                //.addPathPatterns("/apii/**")
                // 放行登录接口
                .excludePathPatterns("/api/login");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 允许所有来源（开发阶段常用）；如上线建议改为具体域名白名单
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // 如前端需要读取某些响应头，可在此暴露
                .exposedHeaders("Authorization", "token")
                // 如果你前端需要携带 cookie / 授权信息，这里必须为 true
                .allowCredentials(true)
                .maxAge(3600);
    }
    // ====== 新增：配置本地图片虚拟映射 ======
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将前端请求的 /images/** 映射到本地磁盘的 D:\GraduationProject\Img\ 目录
        // 注意：file: 后面的路径建议使用正斜杠 /，且最后必须带上斜杠 /
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:D:/GraduationProject/Img/");
    }
}
