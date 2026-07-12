package com.susumonitor.server.module.auth.controller;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.susumonitor.server.common.BusinessException;
import com.susumonitor.server.common.ErrorCode;
import com.susumonitor.server.module.auth.dto.RegisterRequest;
import com.susumonitor.server.module.auth.mapper.UserMapper;
import com.susumonitor.server.module.auth.service.UserService;
import com.susumonitor.server.module.auth.vo.CurrentUserVo;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

// 启用 Spring Boot 测试上下文和 MockMvc，验证注册 Controller 的 HTTP 行为。
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
// 启用 MockMvc，使测试无需启动真实 HTTP 端口即可调用 Controller。
@AutoConfigureMockMvc
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // 使用模拟 UserService，隔离 Controller 测试与数据库和业务实现的依赖。
    @MockitoBean
    private UserService userService;

    // 提供系统就绪检查所需的数据源替身，避免排除数据库自动配置后上下文无法创建。
    @MockitoBean
    private DataSource dataSource;

    // 提供 MyBatis Mapper 替身，避免控制器测试加载真实 Mapper 所需的会话工厂。
    @MockitoBean
    private UserMapper userMapper;

    @Test
    // 验证合法注册请求返回统一成功响应和请求 ID。
    void registerShouldReturnCreatedUser() throws Exception {
        RegisterRequest request = request("admin", "Password123");
        CurrentUserVo user = user("admin", "admin", "approved");
        when(userService.register(any(RegisterRequest.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", not(blankOrNullString())))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.role").value("admin"))
                .andExpect(jsonPath("$.data.reviewStatus").value("approved"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    // 验证 DTO 校验拒绝不符合用户名规则的注册请求。
    void registerShouldRejectInvalidUsername() throws Exception {
        RegisterRequest request = request("bad-name", "Password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40002));
    }

    @Test
    // 验证用户名冲突被转换为统一的 409 资源冲突响应。
    void registerShouldReturnConflictForDuplicateUsername() throws Exception {
        RegisterRequest request = request("admin", "Password123");
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_CONFLICT));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900))
                .andExpect(jsonPath("$.message").value("resource conflict"));
    }

    private RegisterRequest request(String username, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private CurrentUserVo user(String username, String role, String reviewStatus) {
        CurrentUserVo user = new CurrentUserVo();
        user.setId(1L);
        user.setUsername(username);
        user.setRole(role);
        user.setReviewStatus(reviewStatus);
        user.setCreatedAt(OffsetDateTime.now());
        return user;
    }
}
