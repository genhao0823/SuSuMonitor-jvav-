package com.susumonitor.server.module.metrics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/** Agent metrics.report 消息外层结构。 */
public class MetricReportMessage {

    private String type;
    @JsonProperty("message_id")
    private String messageId;
    private OffsetDateTime timestamp;
    private MetricsReportPayload payload;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
    public MetricsReportPayload getPayload() { return payload; }
    public void setPayload(MetricsReportPayload payload) { this.payload = payload; }
}
