package com.gsms.gsms.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

/**
 * CORS 配置属性（值作为 Origin Pattern，支持通配如 http://192.168.*.*:3000）
 */
@Component
@ConfigurationProperties(prefix = "cors")
@Validated
public class CorsProperties {

    @NotEmpty(message = "CORS允许的域名不能为空")
    private List<String> allowedOrigins = new ArrayList<>();

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
