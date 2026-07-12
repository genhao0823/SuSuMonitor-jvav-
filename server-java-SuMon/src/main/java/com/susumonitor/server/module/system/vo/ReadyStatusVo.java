package com.susumonitor.server.module.system.vo;

import java.time.OffsetDateTime;

public class ReadyStatusVo {

    private final String status;

    private final String database;

    private final OffsetDateTime timestamp;

    public ReadyStatusVo(String status, String database, OffsetDateTime timestamp) {
        this.status = status;
        this.database = database;
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public String getDatabase() {
        return database;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }
}
