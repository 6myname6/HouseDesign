package com.housedesign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 应用启动类。{@code @EnableAsync} 开启异步支持（配合 AsyncConfig 的线程池使用）。
 */
@EnableAsync
@SpringBootApplication
public class HouseDesignApplication {

    /** 程序入口：启动 Spring Boot 应用。 */
    public static void main(String[] args) {
        SpringApplication.run(HouseDesignApplication.class, args);
    }
}
