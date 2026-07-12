package com.susumonitor.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application-level typed configuration for SuSuMonitor.
 */
@ConfigurationProperties(prefix = "susumonitor")
public class AppProperties {

    private final Jwt jwt = new Jwt();

    private final Security security = new Security();

    private final Agent agent = new Agent();

    private final Ssh ssh = new Ssh();

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

        private String secret;

        private int expireHours = 24;

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

        private int connectTimeoutSeconds = 10;

        private int idleTimeoutMinutes = 20;

        public int getConnectTimeoutSeconds() {
            return connectTimeoutSeconds;
        }

        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
        }

        public int getIdleTimeoutMinutes() {
            return idleTimeoutMinutes;
        }

        public void setIdleTimeoutMinutes(int idleTimeoutMinutes) {
            this.idleTimeoutMinutes = idleTimeoutMinutes;
        }
    }

    public static class Metrics {

        private int retentionDays = 10;

        private String cleanupCron = "0 0 3 * * ?";

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
    }
}
