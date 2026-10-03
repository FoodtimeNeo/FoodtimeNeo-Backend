package com.FoodtimeNeo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FoodTimeNeoApplication {
    public static void main(String[] args) {
        SpringApplication.run(FoodTimeNeoApplication.class, args);
    }
}
