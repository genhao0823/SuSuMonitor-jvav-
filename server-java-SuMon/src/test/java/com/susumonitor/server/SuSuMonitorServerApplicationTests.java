package com.susumonitor.server;

import com.susumonitor.server.module.auth.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class SuSuMonitorServerApplicationTests {

    @MockitoBean
    private DataSource dataSource;

    // 使用模拟 Mapper 替代真实数据库 Mapper，保持上下文测试不依赖数据库自动配置。
    @MockitoBean
    private UserMapper userMapper;

    @Test
    void contextLoads() {
        // Verifies that the Spring application context can start.
    }
}
