package com.gsms.gsms.infra.config;

import com.gsms.gsms.infra.converter.StringToEnumConverterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web配置类
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final CorsProperties corsProperties;
    private final UploadProperties uploadProperties;

    public WebConfig(JwtInterceptor jwtInterceptor, CorsProperties corsProperties,
                     UploadProperties uploadProperties) {
        this.jwtInterceptor = jwtInterceptor;
        this.corsProperties = corsProperties;
        this.uploadProperties = uploadProperties;
    }

    /**
     * 配置跨域资源共享(CORS)
     * 使用 allowedOriginPatterns，支持通配（如局域网 IP），并可与 allowCredentials 同时使用
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(corsProperties.getAllowedOrigins().toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置字符编码过滤器，解决中文乱码问题
     */
    @Bean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        return filter;
    }

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册JWT拦截器，拦截所有/api/开头的请求
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/users/login",      // 登录接口放行
                        "/api/users/register",   // 注册接口放行
                        "/api/captcha",          // 登录验证码放行
                        "/api/captcha/**",
                        "/swagger-ui/**",        // Swagger UI 放行
                        "/v3/api-docs/**",       // OpenAPI 文档放行
                        "/swagger-ui.html"       // Swagger UI 首页放行
                );
    }

    /**
     * 本地上传文件静态访问（头像等，无需 JWT）
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadProperties.getBaseDir()).toAbsolutePath().normalize()
                .toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        String pattern = uploadProperties.getPublicPathPrefix();
        if (!pattern.startsWith("/")) {
            pattern = "/" + pattern;
        }
        if (pattern.endsWith("/")) {
            pattern = pattern.substring(0, pattern.length() - 1);
        }
        registry.addResourceHandler(pattern + "/**")
                .addResourceLocations(location);
    }

    /**
     * 添加自定义转换器，支持 String 到枚举的自动转换
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new StringToEnumConverterFactory());
    }
}
