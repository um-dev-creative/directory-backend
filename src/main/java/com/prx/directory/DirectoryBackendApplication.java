package com.prx.directory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.prx.security.client")
@SpringBootApplication(
        scanBasePackages = {
                "com.prx.commons.services",
                "com.prx.directory",
                "com.prx.security"
        }
)
public class DirectoryBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(DirectoryBackendApplication.class, args);
    }
}
