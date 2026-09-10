package com.campusshare;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.campusshare.mapper")
public class CampusShareApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusShareApplication.class, args);
    }
}
