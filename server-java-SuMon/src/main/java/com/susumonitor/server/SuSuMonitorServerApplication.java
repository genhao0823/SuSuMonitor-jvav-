package com.susumonitor.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;

/**
 * SuSuMonitor Java backend application entry point.
 */
// 启用 Spring Scheduling，使后端可以注册定时任务。
@EnableScheduling
// 扫描项目内的 MyBatis Mapper 接口并注册为 Spring Bean。
@MapperScan("com.susumonitor.server.module")
// 扫描 @ConfigurationProperties 类并绑定 application.yml 配置。
@ConfigurationPropertiesScan
// 启用 Spring Boot 组件扫描、自动配置和应用启动能力。
@SpringBootApplication
public class SuSuMonitorServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SuSuMonitorServerApplication.class, args);
    }
}
