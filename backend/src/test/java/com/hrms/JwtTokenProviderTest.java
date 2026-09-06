package com.hrms;

import static org.junit.jupiter.api.Assertions.*;

import com.hrms.config.AppProperties;
import com.hrms.security.JwtTokenProvider;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  private JwtTokenProvider provider() {
    AppProperties props = new AppProperties(
        new AppProperties.Jwt("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            Duration.ofMinutes(15), Duration.ofHours(168)),
        "http://localhost:8080",
        new AppProperties.Cors(List.of("http://localhost:8080")),
        new AppProperties.Mail(true));
    return new JwtTokenProvider(props);
  }

  @Test
  void createsAndParsesAccessToken() {
    JwtTokenProvider p = provider();
    java.util.UUID userId = java.util.UUID.randomUUID();
    String token = p.createAccessToken(userId, "user@hrms.io", List.of("employee"));
    assertTrue(p.isValid(token));
    assertEquals(userId, p.getUserId(token));
    assertEquals(List.of("employee"), p.getRoles(token));
  }

  @Test
  void rejectsTamperedToken() {
    JwtTokenProvider p = provider();
    String token = p.createAccessToken(java.util.UUID.randomUUID(), "user@hrms.io", List.of("employee"));
    String tampered = token.substring(0, token.length() - 3) + "xyz";
    assertFalse(p.isValid(tampered));
    assertFalse(p.isValid("not-a-token"));
  }
}
