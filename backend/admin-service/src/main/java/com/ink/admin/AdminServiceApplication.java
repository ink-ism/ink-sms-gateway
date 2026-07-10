package com.ink.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 后台管理服务主启动类
 */
@SpringBootApplication(scanBasePackages = {"com.ink.admin", "com.ink.common"})
@EnableDiscoveryClient
@MapperScan("com.ink.admin.mapper")
public class AdminServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
