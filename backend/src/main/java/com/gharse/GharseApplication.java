package com.gharse;

import com.gharse.common.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class GharseApplication {
    public static void main(String[] args) {
        SpringApplication.run(GharseApplication.class, args);
    }
}
