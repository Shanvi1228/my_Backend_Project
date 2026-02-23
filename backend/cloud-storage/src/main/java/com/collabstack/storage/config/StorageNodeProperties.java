package com.collabstack.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "storage.nodes")
@Data
public class StorageNodeProperties {

    private List<NodeConfig> nodes = new ArrayList<>();

    @Data
    public static class NodeConfig {
        private String id;
        private String host;
        private int port;
    }
}
