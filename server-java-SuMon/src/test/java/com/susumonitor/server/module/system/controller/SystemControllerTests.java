package com.susumonitor.server.module.system.controller;

import com.susumonitor.server.module.auth.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Test
    void healthShouldReturnSuccessAndRequestId() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", not(blankOrNullString())))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.application").value("susumonitor"));
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
