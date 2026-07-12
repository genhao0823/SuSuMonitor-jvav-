package com.susumonitor.server.module.system.controller;

import com.susumonitor.server.common.ApiResponse;
import com.susumonitor.server.common.BusinessException;
import com.susumonitor.server.common.ErrorCode;
import com.susumonitor.server.module.system.vo.HealthStatusVo;
import com.susumonitor.server.module.system.vo.ReadyStatusVo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api")
public class SystemController {

    private static final int DATABASE_VALIDATE_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    private final String applicationName;

    public SystemController(DataSource dataSource,
                            @Value("${spring.application.name:susumonitor}") String applicationName) {
        this.dataSource = dataSource;
        this.applicationName = applicationName;
    }

    @GetMapping("/health")
    public ApiResponse<HealthStatusVo> health() {
        return ApiResponse.success(new HealthStatusVo("UP", applicationName, OffsetDateTime.now()));
    }

    @GetMapping("/ready")
    public ApiResponse<ReadyStatusVo> ready() {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(DATABASE_VALIDATE_TIMEOUT_SECONDS)) {
                throw new BusinessException(ErrorCode.DATABASE_ERROR);
            }
            return ApiResponse.success(new ReadyStatusVo("UP", "ok", OffsetDateTime.now()));
        } catch (SQLException exception) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, exception);
        }
    }
}
