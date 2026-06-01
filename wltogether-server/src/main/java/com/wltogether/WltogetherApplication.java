package com.wltogether;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WltogetherApplication {

    public static void main(String[] args) {
        SpringApplication.run(WltogetherApplication.class, args);
    }
}
