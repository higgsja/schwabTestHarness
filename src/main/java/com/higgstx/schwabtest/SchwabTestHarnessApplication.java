package com.higgstx.schwabtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class SchwabTestHarnessApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SchwabTestHarnessApplication.class, args);
    }
}