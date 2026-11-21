package com.umdc.directory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.umdc.directory.client")
@SpringBootApplication(
        scanBasePackages = {
                "com.umdc.commons.services",
                "com.umdc.directory",
                "com.umdc.security"
        }
)
public class DirectoryBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(DirectoryBackendApplication.class, args);
    }
}
