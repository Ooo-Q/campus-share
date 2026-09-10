package com.campusshare.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@Component
@Configuration
@ConfigurationProperties(prefix = "campusshare.jwt")
public class JwtProperties {

    private String secret;

    private String issuer;

    private long expireMinutes;
}
