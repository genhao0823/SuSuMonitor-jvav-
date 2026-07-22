package com.susumonitor.server.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Application-level typed configuration for SuSuMonitor.
 */
// 启用配置属性校验，使无效安全配置在应用启动阶段直接失败。
@Validated
// 将 susumonitor 前缀下的配置绑定到当前类型。
@ConfigurationProperties(prefix = "susumonitor")
public class AppProperties {

    // 递归校验 JWT 密钥和有效期配置。
    @Valid
    private final Jwt jwt = new Jwt();

    // 递归校验 AES-GCM 密钥，使缺失的加密配置在应用启动阶段直接失败。
    @Valid
    private final Security security = new Security();

    private final Agent agent = new Agent();

    // 递归校验 SSH 出站访问、超时和并发限制配置。
    @Valid
    private final Ssh ssh = new Ssh();

    // 递归校验 Metrics 保留周期和清理批次配置。
    @Valid
    private final Metrics metrics = new Metrics();

    public Jwt getJwt() {
        return jwt;
    }

    public Security getSecurity() {
        return security;
    }

    public Agent getAgent() {
        return agent;
    }

    public Ssh getSsh() {
        return ssh;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public static class Jwt {

        // JWT 密钥必须通过本机外部配置提供，不能使用空值启动。
        @NotBlank(message = "JWT secret must not be blank")
        private String secret;

        // JWT 有效期必须为正数，默认值为 72 小时（三天）。
        @Positive(message = "JWT expiration hours must be greater than zero")
        private int expireHours = 72;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public int getExpireHours() {
            return expireHours;
        }

        public void setExpireHours(int expireHours) {
            this.expireHours = expireHours;
        }
    }

    public static class Security {

        // AES-GCM 密钥必须通过外部配置提供，空值会在应用启动校验阶段被拒绝。
        @NotBlank(message = "AES-GCM key must not be blank")
        private String aesGcmKey;

        public String getAesGcmKey() {
            return aesGcmKey;
        }

        public void setAesGcmKey(String aesGcmKey) {
            this.aesGcmKey = aesGcmKey;
        }
    }

    public static class Agent {

        private String registerKey;

        public String getRegisterKey() {
            return registerKey;
        }

        public void setRegisterKey(String registerKey) {
            this.registerKey = registerKey;
        }
    }

    public static class Ssh {

        // 限制 TCP 连接超时在可控范围内，避免过短抖动或长时间占用资源。
        @Min(value = 1, message = "SSH connect timeout must be at least one second")
        @Max(value = 60, message = "SSH connect timeout must not exceed 60 seconds")
        private int connectTimeoutSeconds = 10;

        // 限制 SSH 握手和认证期间的 socket 读写超时。
        @Min(value = 1, message = "SSH socket timeout must be at least one second")
        @Max(value = 120, message = "SSH socket timeout must not exceed 120 seconds")
        private int socketTimeoutSeconds = 15;

        // 限制一次 SSH 请求的整体等待预算。
        @Min(value = 1, message = "SSH total timeout must be at least one second")
        @Max(value = 180, message = "SSH total timeout must not exceed 180 seconds")
        private int totalTimeoutSeconds = 30;

        // 限制同时执行的 SSH 握手和认证数量。
        @Min(value = 1, message = "SSH connection limit must be at least one")
        @Max(value = 100, message = "SSH connection limit must not exceed 100")
        private int maxConcurrentConnections = 8;

        // 限制一次 DNS 解析可返回的地址数量，避免异常解析结果放大校验和连接成本。
        @Min(value = 1, message = "SSH resolved address limit must be at least one")
        @Max(value = 32, message = "SSH resolved address limit must not exceed 32")
        private int maxResolvedAddresses = 8;

        // 至少配置一个允许的 SSH 端口，空列表会导致配置校验失败。
        @NotEmpty(message = "SSH allowed ports must not be empty")
        private List<Integer> allowedPorts = new ArrayList<>(List.of(22));

        private List<String> allowedCidrs = new ArrayList<>();

        private int idleTimeoutMinutes = 20;

        public int getConnectTimeoutSeconds() {
            return connectTimeoutSeconds;
        }

        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
        }

        public int getSocketTimeoutSeconds() {
            return socketTimeoutSeconds;
        }

        public void setSocketTimeoutSeconds(int socketTimeoutSeconds) {
            this.socketTimeoutSeconds = socketTimeoutSeconds;
        }

        public int getTotalTimeoutSeconds() {
            return totalTimeoutSeconds;
        }

        public void setTotalTimeoutSeconds(int totalTimeoutSeconds) {
            this.totalTimeoutSeconds = totalTimeoutSeconds;
        }

        public int getMaxConcurrentConnections() {
            return maxConcurrentConnections;
        }

        public void setMaxConcurrentConnections(int maxConcurrentConnections) {
            this.maxConcurrentConnections = maxConcurrentConnections;
        }

        public int getMaxResolvedAddresses() {
            return maxResolvedAddresses;
        }

        public void setMaxResolvedAddresses(int maxResolvedAddresses) {
            this.maxResolvedAddresses = maxResolvedAddresses;
        }

        public List<Integer> getAllowedPorts() {
            return allowedPorts;
        }

        public void setAllowedPorts(List<Integer> allowedPorts) {
            this.allowedPorts = allowedPorts;
        }

        public List<String> getAllowedCidrs() {
            return allowedCidrs;
        }

        public void setAllowedCidrs(List<String> allowedCidrs) {
            this.allowedCidrs = allowedCidrs;
        }

        public int getIdleTimeoutMinutes() {
            return idleTimeoutMinutes;
        }

        public void setIdleTimeoutMinutes(int idleTimeoutMinutes) {
            this.idleTimeoutMinutes = idleTimeoutMinutes;
        }
    }

    public static class Metrics {

        /** Metrics 数据保留天数，必须至少保留一天。 */
        @Min(value = 1, message = "Metrics retention days must be at least one")
        private int retentionDays = 10;

        /** Metrics 清理任务 Cron 表达式。 */
        @NotBlank(message = "Metrics cleanup cron must not be blank")
        private String cleanupCron = "0 0 3 * * ?";

        /** 单次清理 SQL 删除的最大记录数。 */
        @Min(value = 1, message = "Metrics cleanup batch size must be at least one")
        @Max(value = 10000, message = "Metrics cleanup batch size must not exceed 10000")
        private int cleanupBatchSize = 1000;

        /** 单次定时任务允许执行的最大批次数。 */
        @Min(value = 1, message = "Metrics cleanup max batches must be at least one")
        @Max(value = 1000, message = "Metrics cleanup max batches must not exceed 1000")
        private int cleanupMaxBatchesPerRun = 100;

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }

        public String getCleanupCron() {
            return cleanupCron;
        }

        public void setCleanupCron(String cleanupCron) {
            this.cleanupCron = cleanupCron;
        }

        public int getCleanupBatchSize() {
            return cleanupBatchSize;
        }

        public void setCleanupBatchSize(int cleanupBatchSize) {
            this.cleanupBatchSize = cleanupBatchSize;
        }

        public int getCleanupMaxBatchesPerRun() {
            return cleanupMaxBatchesPerRun;
        }

        public void setCleanupMaxBatchesPerRun(int cleanupMaxBatchesPerRun) {
            this.cleanupMaxBatchesPerRun = cleanupMaxBatchesPerRun;
        }
    }
}
