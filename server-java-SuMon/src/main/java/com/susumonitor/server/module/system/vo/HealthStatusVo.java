package com.susumonitor.server.module.system.vo;

import java.time.OffsetDateTime;

public class HealthStatusVo {

    private final String status;

    private final String application;

    private final OffsetDateTime timestamp;

    public HealthStatusVo(String status, String application, OffsetDateTime timestamp) {
        this.status = status;
        this.application = application;
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public String getApplication() {
        return application;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }
}
