package com.umdev.directory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.umdev.directory.client")
@SpringBootApplication(
        scanBasePackages = {
                "com.umdev.commons.services",
                "com.umdev.directory",
                "com.umdev.security"
        }
)
public class DirectoryBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(DirectoryBackendApplication.class, args);
    }
}
