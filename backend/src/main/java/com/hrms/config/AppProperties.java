package com.hrms.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hrms")
public record AppProperties(Jwt jwt, String frontendUrl, Cors cors, Mail mail) {
  public record Jwt(String secret, Duration accessExpiration, Duration refreshExpiration) {}
  public record Cors(List<String> allowedOrigins) {}
  public record Mail(boolean enabled) {}
}
