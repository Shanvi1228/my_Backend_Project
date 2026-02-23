package com.collabstack.storagenode;

import com.collabstack.storagenode.config.StorageConfig;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@EnableConfigurationProperties(StorageConfig.class)
public class StorageNodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageNodeApplication.class, args);
    }

    @Bean
    CommandLineRunner init(StorageConfig config) {
        return args -> {
            Path dir = Path.of(config.getDataDir());
            if (!Files.exists(dir)) Files.createDirectories(dir);
            System.out.println("Storage node [" + config.getId() + "] ready at: " + dir.toAbsolutePath());
        };
    }
}
