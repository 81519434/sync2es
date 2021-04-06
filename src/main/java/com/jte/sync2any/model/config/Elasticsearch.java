package com.jte.sync2any.model.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("elasticsearch")
public class Elasticsearch {
    private String uri;
    private String username;
    private String password;
}
