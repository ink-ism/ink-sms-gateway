package com.ink.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * API 服务启动类
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.ink.api", "com.ink.core", "com.ink.common"})
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
