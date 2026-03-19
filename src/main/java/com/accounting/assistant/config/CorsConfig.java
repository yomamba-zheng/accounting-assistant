package com.accounting.assistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置类
 * 解决前后端分离开发时的跨域问题
 */
@Configuration
public class CorsConfig {

    /**
     * 配置 CORS 过滤器
     * 允许前端应用跨域访问后端接口
     * 
     * @return CORS 过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        // 创建 CORS 配置对象
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许所有来源的跨域请求（生产环境建议指定具体域名）
        config.addAllowedOriginPattern("*");
        
        // 允许所有请求头
        config.addAllowedHeader("*");
        
        // 允许所有 HTTP 方法
        config.addAllowedMethod("*");
        
        // 允许携带认证信息（cookies、authorization headers 等）
        config.setAllowCredentials(true);
        
        // 预检请求的缓存时间（秒）
        config.setMaxAge(3600L);
        
        // 暴露给客户端的响应头
        config.addExposedHeader("*");

        // 创建基于 URL 的 CORS 配置源
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        // 对所有路径应用 CORS 配置
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
