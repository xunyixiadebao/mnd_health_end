package org.example.djiankang;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ServletComponentScan
@MapperScan("org.example.djiankang.db.mapper")
@EnableAsync
@EnableCaching
@EnableScheduling
public class DjiankangApplication {
    public static void main(String[] args) {
        SpringApplication.run(DjiankangApplication.class, args);
    }

}
