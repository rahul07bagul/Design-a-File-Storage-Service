package com.example.filedrive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.filedrive.repository")
public class FileDriveApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileDriveApplication.class, args);
    }

}
