package com.susumonitor.server.module.system.controller;

import com.susumonitor.server.module.auth.mapper.AuthBootstrapStateMapper;
import com.susumonitor.server.module.auth.mapper.UserMapper;
import com.susumonitor.server.module.server.mapper.ServerMapper;
import com.susumonitor.server.module.metrics.mapper.MetricsMapper;
import com.susumonitor.server.module.metrics.mapper.MetricsCleanupMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 激活独立测试配置，避免 Controller 测试读取本机敏感配置。
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class SystemControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private Connection connection;

    // 使用模拟 Mapper 替代真实数据库 Mapper，保持 Controller 测试不依赖数据库自动配置。
    @MockitoBean
    private UserMapper userMapper;

    // 提供初始化状态 Mapper 替身，避免加载真实 MyBatis 会话工厂。
    @MockitoBean
    private AuthBootstrapStateMapper authBootstrapStateMapper;

    // 提供服务器 Mapper 替身，避免新增 Mapper 扫描后加载真实 MyBatis 会话工厂。
    @MockitoBean
    private ServerMapper serverMapper;

    @MockitoBean
    private MetricsMapper metricsMapper;

    @MockitoBean
    private MetricsCleanupMapper metricsCleanupMapper;

    @MockitoBean
    private PlatformTransactionManager transactionManager;

    @Test
    void healthShouldReturnSuccessAndRequestId() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", matchesPattern(
                        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.application").value("susumonitor"));
    }

    @Test
    void healthShouldIgnoreClientRequestIdAndReturnValidCorrelationId() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header("X-Request-ID", "client-controlled-id")
                        .header("X-Correlation-ID", "mockmvc-correlation-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", not("client-controlled-id")))
                .andExpect(header().string("X-Correlation-ID", "mockmvc-correlation-1"));
    }

    @Test
    void healthShouldIgnoreInvalidCorrelationId() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header("X-Correlation-ID", "invalid correlation value"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Correlation-ID"));
    }

    @Test
    void readyShouldReturnSuccessWhenDatabaseConnectionIsValid() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        mockMvc.perform(get("/api/ready"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", not(blankOrNullString())))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.database").value("ok"));
    }

    @Test
    void readyShouldReturnDatabaseErrorWhenConnectionFails() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("connection failed"));

        mockMvc.perform(get("/api/ready"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("X-Request-ID", not(blankOrNullString())))
                .andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.message").value("database error"));
    }
}
