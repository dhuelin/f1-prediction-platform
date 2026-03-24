package com.f1predict.f1data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class F1DataServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(F1DataServiceApplication.class, args);
    }
}
