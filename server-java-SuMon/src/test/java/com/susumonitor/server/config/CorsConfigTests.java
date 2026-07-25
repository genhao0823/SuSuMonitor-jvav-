package com.susumonitor.server.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

/**
 * 验证 CORS 配置属性绑定和白名单校验。
 *
 * <p>覆盖白名单非空校验、Origin 精确匹配、允许方法和请求头、
 * 以及 CorsConfiguration 对象的构造结果。</p>
 */
class CorsConfigTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /** 默认配置应包含本机开发 Origin。 */
    @Test
    void defaultOriginsShouldIncludeLocalDev() {
        AppProperties.Cors cors = new AppProperties.Cors();
        assertTrue(cors.getAllowedOrigins().contains("http://localhost:5173"));
        assertTrue(cors.getAllowedOrigins().contains("http://127.0.0.1:5173"));
    }

    /** 空白名单应在校验时失败，防止部署时遗漏配置。 */
    @Test
    void emptyOriginsShouldFailValidation() {
        AppProperties.Cors cors = new AppProperties.Cors();
        cors.setAllowedOrigins(List.of());
        Set<ConstraintViolation<AppProperties.Cors>> violations = validator.validate(cors);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("CORS allowed origins must not be empty")));
    }

    /** CorsConfiguration 应正确从 AppProperties 构造，包含白名单 Origin 和方法。 */
    @Test
    void corsConfigurationShouldReflectProperties() {
        AppProperties.Cors corsProps = new AppProperties.Cors();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProps.getAllowedOrigins());
        configuration.setAllowedMethods(corsProps.getAllowedMethods());
        configuration.setAllowedHeaders(corsProps.getAllowedHeaders());
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(corsProps.getMaxAgeSeconds());

        assertNotNull(configuration.getAllowedOrigins());
        assertTrue(configuration.getAllowedOrigins().contains("http://localhost:5173"));
        assertTrue(configuration.getAllowedMethods().contains("GET"));
        assertTrue(configuration.getAllowedMethods().contains("POST"));
        assertTrue(configuration.getAllowedMethods().contains("OPTIONS"));
        assertTrue(configuration.getAllowedHeaders().contains("Authorization"));
        assertTrue(configuration.getAllowedHeaders().contains("Content-Type"));
        assertTrue(configuration.getAllowCredentials());
        assertEquals(3600, configuration.getMaxAge());
    }

    /** 白名单不应包含通配符 "*"，防止意外开放所有来源。 */
    @Test
    void allowedOriginsShouldNotContainWildcard() {
        AppProperties.Cors cors = new AppProperties.Cors();
        assertFalse(cors.getAllowedOrigins().contains("*"));
    }
}
