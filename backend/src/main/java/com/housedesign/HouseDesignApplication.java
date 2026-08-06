package com.housedesign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class HouseDesignApplication {

    public static void main(String[] args) {
        SpringApplication.run(HouseDesignApplication.class, args);
    }
}
