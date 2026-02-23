package com.collabstack.storagenode.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.node")
@Data
public class StorageConfig {
    private String id;       // e.g. "node-1"
    private String dataDir;  // e.g. "./data/chunks"
}
